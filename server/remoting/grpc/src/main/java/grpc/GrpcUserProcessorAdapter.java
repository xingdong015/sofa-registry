package grpc;

import com.alipay.sofa.registry.remoting.ChannelHandler;
import grpc.Connection;
import grpc.GrpcChannel;

/**
 * @author chengzhengzheng
 * @date 2023/1/4
 */
public class GrpcUserProcessorAdapter {

    private final ChannelHandler userProcessorHandler;

    public GrpcUserProcessorAdapter(ChannelHandler userProcessorHandler) {
        this.userProcessorHandler = userProcessorHandler;
    }

    public Object handleRequest(Connection connection, Object request) {
        GrpcChannel grpcChannel = new GrpcChannel(connection);
        return userProcessorHandler.reply(grpcChannel, request);
    }
}
