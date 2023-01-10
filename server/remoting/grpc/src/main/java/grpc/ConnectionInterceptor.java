package grpc;

import io.grpc.*;
import io.grpc.internal.ServerStream;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelHelper;
import io.grpc.netty.shaded.io.netty.channel.Channel;
import io.grpc.internal.ServerStreamHelper;


public class ConnectionInterceptor implements ServerInterceptor {

    @Override
    public <T, S> ServerCall.Listener<T> interceptCall(ServerCall<T, S> call, Metadata headers,
                                                       ServerCallHandler<T, S> next) {
        Context ctx = Context.current().withValue(GrpcServerConstants.CONTEXT_KEY_CONN_ID,
                        call.getAttributes().get(GrpcServerConstants.ATTR_TRANS_KEY_CONN_ID))
                .withValue(GrpcServerConstants.CONTEXT_KEY_CONN_REMOTE_IP, call.getAttributes().get(GrpcServerConstants.ATTR_TRANS_KEY_REMOTE_IP))
                .withValue(GrpcServerConstants.CONTEXT_KEY_CONN_REMOTE_PORT, call.getAttributes().get(GrpcServerConstants.ATTR_TRANS_KEY_REMOTE_PORT))
                .withValue(GrpcServerConstants.CONTEXT_KEY_CONN_LOCAL_PORT, call.getAttributes().get(GrpcServerConstants.ATTR_TRANS_KEY_LOCAL_PORT));
        if (GrpcServerConstants.REQUEST_BI_STREAM_SERVICE_NAME.equals(call.getMethodDescriptor().getServiceName())) {
            Channel internalChannel = getInternalChannel(call);
            ctx = ctx.withValue(GrpcServerConstants.CONTEXT_KEY_CHANNEL, internalChannel);
        }
        return Contexts.interceptCall(ctx, call, headers, next);
    }

    private Channel getInternalChannel(ServerCall serverCall) {
        ServerStream serverStream = ServerStreamHelper.getServerStream(serverCall);
        return NettyChannelHelper.getChannel(serverStream);
    }
}