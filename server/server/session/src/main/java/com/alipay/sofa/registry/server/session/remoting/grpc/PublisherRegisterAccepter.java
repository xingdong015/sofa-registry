package com.alipay.sofa.registry.server.session.remoting.grpc;

import com.alipay.sofa.registry.common.model.client.pb.PublisherRegisterPb;
import com.alipay.sofa.registry.common.model.client.pb.PublisherRegisterServiceGrpc;
import com.alipay.sofa.registry.common.model.client.pb.RegisterResponsePb;
import io.grpc.stub.StreamObserver;

/**
 * @author chengzhengzheng
 * @date 2022/11/13
 */
public class PublisherRegisterAccepter extends PublisherRegisterServiceGrpc.PublisherRegisterServiceImplBase {
    @Override
    public void request(PublisherRegisterPb request, StreamObserver<RegisterResponsePb> responseObserver) {
    }
}
