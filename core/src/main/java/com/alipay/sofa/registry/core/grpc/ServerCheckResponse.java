package com.alipay.sofa.registry.core.grpc;

/**
 * @author chengzhengzheng
 * @date 2022/11/23
 */
public class ServerCheckResponse {
    private String connectionId;

    public ServerCheckResponse() {
    }

    public ServerCheckResponse(String connectionId) {
        this.connectionId = connectionId;
    }

    public String getConnectionId() {
        return connectionId;
    }

    public void setConnectionId(String connectionId) {
        this.connectionId = connectionId;
    }
}
