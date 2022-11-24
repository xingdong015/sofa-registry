package com.alipay.sofa.registry.client.grpc;

import com.alipay.sofa.registry.core.grpc.ConnectResetResponse;
import com.alipay.sofa.registry.core.grpc.ConnectionSetupRequest;

/**
 * @author chengzhengzheng
 * @date 2022/11/24
 */
public class ConnectionRestRequestHandler implements ServerRequestHandler<ConnectionSetupRequest, ConnectResetResponse> {

    @Override
    public ConnectResetResponse requestReply(ConnectionSetupRequest request) {
        return null;
    }
}
