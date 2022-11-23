package com.alipay.sofa.registry.server.session.remoting.handler;

import com.alipay.sofa.registry.core.model.PublisherRegister;
import com.alipay.sofa.registry.core.model.ServerCheckRequest;
import com.alipay.sofa.registry.core.model.ServerCheckResponse;
import com.alipay.sofa.registry.remoting.Channel;
import grpc.GrpcChannel;

/**
 * @author chengzhengzheng
 * @date 2022/11/23
 */
public class ServerCheckHandler extends AbstractClientDataRequestHandler<ServerCheckRequest> {
    @Override
    public Class interest() {
        return ServerCheckRequest.class;
    }

    @Override
    public Object doHandle(Channel channel, ServerCheckRequest request) {
        return new ServerCheckResponse(((GrpcChannel)channel).getConnection().getConnectionId());
    }
}
