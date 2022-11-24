package com.alipay.sofa.registry.client.grpc;

import com.alipay.remoting.exception.RemotingException;
import com.alipay.sofa.registry.client.api.RegistryClientConfig;
import com.alipay.sofa.registry.client.constants.RpcClientStatus;
import com.alipay.sofa.registry.client.log.LoggerFactory;
import com.alipay.sofa.registry.client.provider.RegisterCache;
import com.alipay.sofa.registry.client.remoting.Client;
import com.alipay.sofa.registry.client.remoting.ServerManager;
import com.alipay.sofa.registry.client.remoting.ServerNode;
import com.alipay.sofa.registry.client.task.Worker;
import com.alipay.sofa.registry.client.task.WorkerThread;
import com.alipay.sofa.registry.common.model.client.pb.BiRequestStreamGrpc;
import com.alipay.sofa.registry.common.model.client.pb.Payload;
import com.alipay.sofa.registry.common.model.client.pb.RequestGrpc;
import com.alipay.sofa.registry.core.grpc.PayloadRegistry;
import com.alipay.sofa.registry.core.grpc.ServerCheckRequest;
import com.alipay.sofa.registry.core.grpc.ServerCheckResponse;
import com.alipay.sofa.registry.core.grpc.ConnectionSetupRequest;
import com.google.common.util.concurrent.ListenableFuture;
import io.grpc.CompressorRegistry;
import io.grpc.DecompressorRegistry;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author chengzhengzheng
 * @date 2022/11/21
 */
public class GrpcClient implements Client {

    private static final Logger LOGGER = LoggerFactory.getLogger(GrpcClient.class);

    private              ServerManager                    serverManager;
    private              RegistryClientConfig             config;
    private              RegisterCache                    registerCache;
    protected volatile   GrpcConnection                   currentConnection;
    private              Worker                           worker;
    private              ScheduledExecutorService         clientEventExecutor;
    private              ServerNode                       serverNode;
    private static final int                              RETRY_TIMES                      = 3;
    protected volatile   AtomicReference<RpcClientStatus> rpcClientStatus                  = new AtomicReference<>(RpcClientStatus.WAIT_INIT);
    private static final long                             DEFAULT_MAX_INBOUND_MESSAGE_SIZE = 10 * 1024 * 1024L;

    private static final long DEFAULT_KEEP_ALIVE_TIME = 6 * 60 * 1000;

    /**
     * handlers to process server push request.
     */
    protected Map<Class,ServerRequestHandler> serverRequestHandlers = new HashMap<>();


    /**
     * Instantiates a new Client connection.
     *
     * @param serverManager the server manager
     * @param config        the config
     */
    public GrpcClient(ServerManager serverManager,
                      RegisterCache registerCache,
                      RegistryClientConfig config) {
        this.serverManager = serverManager;
        this.registerCache = registerCache;
        this.config        = config;
    }

    @Override
    public void init() {
        // 将Client状态由INITIALIZED变更为STARTING
        boolean success = rpcClientStatus.compareAndSet(RpcClientStatus.INITIALIZED, RpcClientStatus.STARTING);
        if (!success) {
            return;
        }

        GrpcConnection connectToServer = null;
        rpcClientStatus.set(RpcClientStatus.STARTING);
        int startUpRetryTimes = RETRY_TIMES;
        while (startUpRetryTimes > 0 && connectToServer == null) {
            try {
                startUpRetryTimes--;
                List<ServerNode> serverNodes = new ArrayList<>(serverManager.getServerList());
                // shuffle server list to make server connections as discrete as possible
                Collections.shuffle(serverNodes);

                for (ServerNode serverNode : serverNodes) {
                    connectToServer = connectToServer(serverNode);
                    if (null != connectToServer && connectToServer.isFine()) {
                        break;
                    }
                }
            } catch (Throwable e) {
                LOGGER.warn("[GrpcConnect] Failed trying connect to, error message = {}, init retry times left: {}",
                        e.getMessage(), startUpRetryTimes, e);
            }
        }

        if (connectToServer != null) {
            this.currentConnection = connectToServer;
            rpcClientStatus.set(RpcClientStatus.RUNNING);
        }
    }

    private ServerCheckResponse serverCheck(String ip, int port, RequestGrpc.RequestFutureStub requestBlockingStub) {
        try {
            if (requestBlockingStub == null) {
                return null;
            }
            ServerCheckRequest        serverCheckRequest = new ServerCheckRequest();
            Payload                   grpcRequest        = GrpcUtils.convert(serverCheckRequest);
            ListenableFuture<Payload> responseFuture     = requestBlockingStub.request(grpcRequest);
            Payload                   response           = responseFuture.get(3000L, TimeUnit.MILLISECONDS);
            return GrpcUtils.parse(response, ServerCheckResponse.class);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Create a stub using a channel.
     *
     * @param managedChannelTemp channel.
     * @return if server check success,return a non-null stub.
     */
    private RequestGrpc.RequestFutureStub createNewChannelStub(ManagedChannel managedChannelTemp) {
        return RequestGrpc.newFutureStub(managedChannelTemp);
    }

    /**
     * create a new channel with specific server address.
     *
     * @param serverIp   serverIp.
     * @param serverPort serverPort.
     * @return if server check success,return a non-null channel.
     */
    private ManagedChannel createNewManagedChannel(String serverIp, int serverPort) {
        ManagedChannelBuilder<?> managedChannelBuilder = ManagedChannelBuilder.forAddress(serverIp, serverPort).
                compressorRegistry(CompressorRegistry.getDefaultInstance()).
                decompressorRegistry(DecompressorRegistry.getDefaultInstance()).
                usePlaintext();
        return managedChannelBuilder.build();
    }


    private GrpcConnection connectToServer(ServerNode serverNode) throws InterruptedException {
        String host = serverNode.getHost();
        int    port = serverNode.getPort();
        try {
            ManagedChannel                managedChannel     = createNewManagedChannel(host, port);
            RequestGrpc.RequestFutureStub newChannelStubTemp = createNewChannelStub(managedChannel);
            if (newChannelStubTemp != null) {
                ServerCheckResponse response = serverCheck(host, port, newChannelStubTemp);

                if (response == null) {
                    shuntDownChannel(managedChannel);
                    return null;
                }

                BiRequestStreamGrpc.BiRequestStreamStub biRequestStreamStub = BiRequestStreamGrpc.newStub(newChannelStubTemp.getChannel());
                GrpcConnection                          grpcConn            = new GrpcConnection(serverNode);
                grpcConn.setConnectionId(response.getConnectionId());

                //create stream request and bind connection event to this connection.
                StreamObserver<Payload> payloadStreamObserver = bindRequestStream(biRequestStreamStub, grpcConn);

                // stream observer to send response to server
                grpcConn.setPayloadStreamObserver(payloadStreamObserver);
                grpcConn.setGrpcFutureServiceStub(newChannelStubTemp);
                grpcConn.setChannel(managedChannel);
                //send a  setup request.
                ConnectionSetupRequest conSetupRequest = new ConnectionSetupRequest();
                grpcConn.sendRequest(conSetupRequest);
                //wait to register connection setup
                Thread.sleep(100L);
                return grpcConn;
            }
            return null;
        } catch (Exception e) {
            LOGGER.error("[GrpcClient]Fail to connect to server!,error={}", e);
        }
        return null;
    }

    private void shuntDownChannel(ManagedChannel managedChannel) {
        if (managedChannel != null && !managedChannel.isShutdown()) {
            managedChannel.shutdownNow();
        }
    }

    private StreamObserver<Payload> bindRequestStream(BiRequestStreamGrpc.BiRequestStreamStub streamStub, GrpcConnection grpcConn) {
        return streamStub.requestBiStream(new StreamObserver<Payload>() {

            @Override
            public void onNext(Payload payload) {
                // 这里是来自于服务端的的请求。 对端的数据流，对端也就是服务端
                LOGGER.debug("[{}]Stream server request receive, original info: {}", grpcConn.getConnectionId(), payload.toString());
                Object parseBody = GrpcUtils.parse(payload);

                Class      classType  = PayloadRegistry.getClassByType(payload.getMetadata().getType());
                Object response = serverRequestHandlers.get(classType).requestReply(parseBody);
                if (response != null) {
                    sendResponse(response);
                }
            }

            @Override
            public void onError(Throwable throwable) {

            }

            @Override
            public void onCompleted() {
            }
        });
    }
    private void sendResponse(Object response) {
        try {
            this.currentConnection.sendResponse(response);
        } catch (Exception e) {
            LOGGER.error("[{}]Error to send ack response, ackId->{}", this.currentConnection.getConnectionId(),
                    response);
        }
    }
    @Override
    public boolean isConnected() {
        return false;
    }

    @Override
    public void ensureConnected() throws InterruptedException {

    }

    @Override
    public Object invokeSync(Object request) throws RemotingException, InterruptedException {
        return null;
    }

    public void setWorker(WorkerThread worker) {
        this.worker = worker;
    }

}
