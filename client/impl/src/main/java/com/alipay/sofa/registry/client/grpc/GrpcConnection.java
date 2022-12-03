package com.alipay.sofa.registry.client.grpc;

import com.alipay.sofa.registry.client.remoting.ServerNode;
import com.alipay.sofa.registry.core.grpc.Payload;
import com.alipay.sofa.registry.core.grpc.RequestGrpc;
import com.alipay.sofa.registry.core.utils.GrpcUtils;
import com.google.common.util.concurrent.ListenableFuture;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;

import java.util.concurrent.TimeUnit;

/**
 * @author chengzhengzheng
 * @date 2022/11/23
 */
public class GrpcConnection {
    private   ServerNode     serverNode;
    private   String         connectionId;
    protected ManagedChannel channel;

    protected           StreamObserver<Payload>       payloadStreamObserver;
    public static final int                           RECONNECTING_DELAY = 5000;
    /**
     * stub to send request.
     */
    protected           RequestGrpc.RequestFutureStub grpcFutureServiceStub;

    public GrpcConnection(ServerNode serverNode) {
        this.serverNode = serverNode;
    }

    public String getConnectionId() {
        return connectionId;
    }

    public void setConnectionId(String connectionId) {
        this.connectionId = connectionId;
    }

    public void setPayloadStreamObserver(StreamObserver<Payload> payloadStreamObserver) {
        this.payloadStreamObserver = payloadStreamObserver;
    }

    /**
     * Getter method for property <tt>grpcFutureServiceStub</tt>.
     *
     * @return property value of grpcFutureServiceStub
     */
    public RequestGrpc.RequestFutureStub getGrpcFutureServiceStub() {
        return grpcFutureServiceStub;
    }

    /**
     * Setter method for property <tt>grpcFutureServiceStub</tt>.
     *
     * @param grpcFutureServiceStub value to be assigned to property grpcFutureServiceStub
     */
    public void setGrpcFutureServiceStub(RequestGrpc.RequestFutureStub grpcFutureServiceStub) {
        this.grpcFutureServiceStub = grpcFutureServiceStub;
    }

    /**
     * Getter method for property <tt>payloadStreamObserver</tt>.
     *
     * @return property value of payloadStreamObserver
     */
    public StreamObserver<Payload> getPayloadStreamObserver() {
        return payloadStreamObserver;
    }

    public void setChannel(ManagedChannel managedChannel) {
        this.channel = managedChannel;
    }

    public <T> void sendRequest(T request) {
        Payload convert = GrpcUtils.convert(request);
        payloadStreamObserver.onNext(convert);
    }

    public <T> Payload request(T request, long timeouts) {
        Payload                   grpcRequest   = GrpcUtils.convert(request);
        ListenableFuture<Payload> requestFuture = grpcFutureServiceStub.request(grpcRequest);
        try {
            return requestFuture.get(timeouts, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public boolean isFine() {
        return channel != null && !channel.isShutdown();
    }

    public ServerNode getServerNode() {
        return serverNode;
    }

    public ManagedChannel getChannel() {
        return channel;
    }

    public void sendResponse(Object response) {
        Payload convert = GrpcUtils.convert(response);
        payloadStreamObserver.onNext(convert);
    }

    public void close() {
        if (this.payloadStreamObserver != null) {
            try {
                payloadStreamObserver.onCompleted();
            } catch (Throwable throwable) {
                //ignore.
            }
        }

        if (this.channel != null && !channel.isShutdown()) {
            try {
                this.channel.shutdownNow();
            } catch (Throwable throwable) {
                //ignore.
            }
        }
    }
}
