package com.alipay.sofa.registry.server.session.remoting.handler;

import com.alibaba.fastjson.JSON;
import com.alipay.sofa.registry.core.grpc.ServerCheckRequest;
import com.alipay.sofa.registry.core.grpc.ServerCheckResponse;
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
        System.out.println("ServerCheckHandler ...." + JSON.toJSONString(request));
        if (channel instanceof GrpcChannel) {
            return new ServerCheckResponse(((GrpcChannel) channel).getConnection().getConnectionId());
        }
        throw new RuntimeException("不支持的通道协议");
    }
}
