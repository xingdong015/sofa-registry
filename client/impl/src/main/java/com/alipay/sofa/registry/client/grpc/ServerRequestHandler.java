package com.alipay.sofa.registry.client.grpc;

/**
 * @author chengzhengzheng
 * @date 2022/11/24
 */
public interface ServerRequestHandler<T, R> {
    /**
     * Handle request from server.
     *
     * @param request  request
     * @param grpcConn
     * @return response.
     */
    R requestReply(T request, GrpcConnection grpcConn);
}
