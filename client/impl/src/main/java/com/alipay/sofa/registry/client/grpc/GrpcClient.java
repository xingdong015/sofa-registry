package com.alipay.sofa.registry.client.grpc;

import com.alipay.sofa.registry.client.api.RegistryClientConfig;
import com.alipay.sofa.registry.client.constants.RpcClientStatus;
import com.alipay.sofa.registry.client.log.LoggerFactory;
import com.alipay.sofa.registry.client.provider.RegisterCache;
import com.alipay.sofa.registry.client.remoting.Client;
import com.alipay.sofa.registry.client.remoting.ServerManager;
import com.alipay.sofa.registry.client.remoting.ServerNode;
import com.alipay.sofa.registry.client.task.Worker;
import com.alipay.sofa.registry.client.task.WorkerThread;
import com.alipay.sofa.registry.core.grpc.*;
import com.alipay.sofa.registry.core.model.RegisterResponse;
import com.alipay.sofa.registry.core.utils.GrpcUtils;
import io.grpc.CompressorRegistry;
import io.grpc.DecompressorRegistry;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
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
    protected Map<Class, ServerRequestHandler> serverRequestHandlers = new HashMap<>();


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
        boolean success = rpcClientStatus.compareAndSet(RpcClientStatus.WAIT_INIT, RpcClientStatus.STARTING);
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
                if (CollectionUtils.isEmpty(serverNodes)){
                    break;
                }
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

    private ServerCheckResponse serverCheck(String ip, int port, RequestGrpc.RequestBlockingStub requestBlockingStub) {
        try {
            if (requestBlockingStub == null) {
                return null;
            }
            ServerCheckRequest        serverCheckRequest = new ServerCheckRequest();
            Payload grpcRequest = GrpcUtils.convert(serverCheckRequest);
            Payload response    = requestBlockingStub.request(grpcRequest);
            return GrpcUtils.parse(response, ServerCheckResponse.class);
        } catch (Exception e) {
            LOGGER.error("Server check fail, please check server {} ,port {} is available , error ={}", ip, port, e);
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

    /**
     * 连接 connect
     *
     * @return
     */
    public boolean connect() {
        List<ServerNode> serverNodes = new ArrayList<>(serverManager.getServerList());
        for (ServerNode serverNode : serverNodes) {
            GrpcConnection connectToServer = connectToServer(serverNode);
            if (null != connectToServer && connectToServer.isFine()) {
                this.currentConnection = connectToServer;
                return true;
            }
        }
        return false;
    }

    private GrpcConnection connectToServer(ServerNode serverNode) {
        String host = serverNode.getHost();
        int    port = serverNode.getPort();
        try {
            ManagedChannel                managedChannel     = createNewManagedChannel(host, port);
            RequestGrpc.RequestFutureStub newChannelStubTemp = createNewChannelStub(managedChannel);
            RequestGrpc.RequestBlockingStub requestBlockingStub = RequestGrpc.newBlockingStub(managedChannel);

            if (newChannelStubTemp != null) {
                ServerCheckResponse response = serverCheck(host, port, requestBlockingStub);

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
                LOGGER.debug("[{}]Stream server request receive, original info: {}", grpcConn.getConnectionId(), payload.toString());
            }

            @Override
            public void onError(Throwable throwable) {
                LOGGER.debug("[{}]Stream server Error receive, original info: {}", grpcConn.getConnectionId());
            }

            @Override
            public void onCompleted() {
                LOGGER.debug("[{}]Stream server Completed receive, original info: {}", grpcConn.getConnectionId());
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
        return currentConnection != null && currentConnection.isFine();
    }

    @Override
    public void ensureConnected() throws InterruptedException {
        if (isConnected()) {
            return;
        }
        while (!connect()) {
            Thread.sleep(GrpcConnection.RECONNECTING_DELAY);
        }
    }

    @Override
    public Object invokeSync(Object request) {
        Payload          payload = currentConnection.request(request, 100);
        RegisterResponse response = new RegisterResponse();
        response.setSuccess(true);
        return response;
    }

    public void setWorker(WorkerThread worker) {
        this.worker = worker;
    }

}
