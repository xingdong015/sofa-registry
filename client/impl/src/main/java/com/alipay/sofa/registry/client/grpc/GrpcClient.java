package com.alipay.sofa.registry.client.grpc;

import com.alipay.remoting.ConnectionEventProcessor;
import com.alipay.remoting.ConnectionEventType;
import com.alipay.remoting.exception.RemotingException;
import com.alipay.remoting.rpc.protocol.UserProcessor;
import com.alipay.sofa.registry.client.api.RegistryClientConfig;
import com.alipay.sofa.registry.client.constants.RpcClientStatus;
import com.alipay.sofa.registry.client.provider.RegisterCache;
import com.alipay.sofa.registry.client.remoting.Client;
import com.alipay.sofa.registry.client.remoting.ServerManager;
import com.alipay.sofa.registry.client.remoting.ServerNode;
import com.alipay.sofa.registry.client.task.Worker;
import com.alipay.sofa.registry.client.task.WorkerThread;
import com.alipay.sofa.registry.common.model.client.pb.BiRequestStreamGrpc;
import com.alipay.sofa.registry.common.model.client.pb.Payload;
import com.alipay.sofa.registry.common.model.client.pb.RequestGrpc;
import com.alipay.sofa.registry.core.model.ConnectionSetupRequest;
import com.google.common.util.concurrent.ListenableFuture;
import io.grpc.*;
import io.grpc.stub.StreamObserver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author chengzhengzheng
 * @date 2022/11/21
 */
public class GrpcClient implements Client {

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
        // 守护线程执行器
        clientEventExecutor = new ScheduledThreadPoolExecutor(2, r -> {
            Thread t = new Thread(r);
            t.setName("com.alipay.sofa.registry.client.remote.worker");
            t.setDaemon(true);
            return t;
        });

        // connect to server, try to connect to server sync RETRY_TIMES times, async starting if failed.
        GrpcConnection connectToServer = null;
        // grpc 状态机，标识连接的不同状态
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
                e.printStackTrace();
            }
        }

        if (connectToServer != null) {
            this.currentConnection = connectToServer;
            rpcClientStatus.set(RpcClientStatus.RUNNING);
            // 连接成功添加ConnectionEvent todo
        }
    }

    private Response serverCheck(String ip, int port, RequestGrpc.RequestFutureStub requestBlockingStub) {
        try {
            if (requestBlockingStub == null) {
                return null;
            }
            ServerCheckRequest        serverCheckRequest = new ServerCheckRequest();
            Payload                   grpcRequest        = GrpcUtils.convert(serverCheckRequest);
            ListenableFuture<Payload> responseFuture     = requestBlockingStub.request(grpcRequest);
            Payload                   response           = responseFuture.get(3000L, TimeUnit.MILLISECONDS);
            //receive connection unregister response here,not check response is success.
            return (Response) GrpcUtils.parse(response);
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
        String                        host               = serverNode.getHost();
        int                           port               = serverNode.getPort();
        ManagedChannel                managedChannel     = createNewManagedChannel(host, port);
        RequestGrpc.RequestFutureStub newChannelStubTemp = createNewChannelStub(managedChannel);
        if (newChannelStubTemp != null) {
            Response response = serverCheck(host, port, newChannelStubTemp);

            BiRequestStreamGrpc.BiRequestStreamStub biRequestStreamStub = BiRequestStreamGrpc.newStub(newChannelStubTemp.getChannel());
            GrpcConnection                          grpcConn            = new GrpcConnection(serverNode);
            grpcConn.setConnectionId(((ServerCheckResponse) response).getConnectionId());

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
    }

    private StreamObserver<Payload> bindRequestStream(BiRequestStreamGrpc.BiRequestStreamStub biRequestStreamStub, GrpcConnection grpcConn) {
        return null;
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
