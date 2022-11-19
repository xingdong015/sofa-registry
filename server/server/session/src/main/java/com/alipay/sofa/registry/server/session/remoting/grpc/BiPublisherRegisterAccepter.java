package com.alipay.sofa.registry.server.session.remoting.grpc;

import com.alipay.sofa.registry.common.model.client.pb.BiPublisherRegisterServiceGrpc;
import com.alipay.sofa.registry.common.model.client.pb.PublisherRegisterPb;
import com.alipay.sofa.registry.common.model.client.pb.RegisterResponsePb;
import io.grpc.stub.StreamObserver;

/**
 * @author chengzhengzheng
 * @date 2022/11/19
 */
public class BiPublisherRegisterAccepter extends BiPublisherRegisterServiceGrpc.BiPublisherRegisterServiceImplBase {
    @Override
    public StreamObserver<PublisherRegisterPb> requestBiStream(StreamObserver<RegisterResponsePb> responseObserver) {
        return super.requestBiStream(responseObserver);
    }
}
