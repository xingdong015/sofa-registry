package com.alipay.sofa.registry.client.grpc;

import com.alipay.sofa.registry.client.remoting.ServerNode;
import com.alipay.sofa.registry.common.model.client.pb.Payload;
import com.alipay.sofa.registry.common.model.client.pb.RequestGrpc;
import com.alipay.sofa.registry.core.model.ConnectionSetupRequest;
import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;

/**
 * @author chengzhengzheng
 * @date 2022/11/23
 */
public class GrpcConnection {
    private ServerNode serverNode;
    private String     connectionId;
    protected ManagedChannel channel;

    public GrpcConnection(ServerNode serverNode) {
        this.serverNode = serverNode;
    }

    public void setConnectionId(String connectionId) {
        this.connectionId = connectionId;
    }

    public void setPayloadStreamObserver(StreamObserver<Payload> payloadStreamObserver) {

    }

    public void setGrpcFutureServiceStub(RequestGrpc.RequestFutureStub newChannelStubTemp) {

    }

    public void setChannel(ManagedChannel managedChannel) {
        this.channel = managedChannel;
    }

    public void sendRequest(ConnectionSetupRequest conSetupRequest) {

    }

    public boolean isFine() {
        return channel != null && !channel.isShutdown();
    }
}
