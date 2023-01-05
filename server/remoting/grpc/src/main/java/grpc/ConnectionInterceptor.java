package grpc;

import io.grpc.*;

public class ConnectionInterceptor implements ServerInterceptor {

    @Override
    public <T, S> ServerCall.Listener<T> interceptCall(ServerCall<T, S> call, Metadata headers,
                                                       ServerCallHandler<T, S> next) {
        Context ctx = Context.current().withValue(GrpcServerConstants.CONTEXT_KEY_CONN_ID,
                        call.getAttributes().get(GrpcServerConstants.ATTR_TRANS_KEY_CONN_ID))
                .withValue(GrpcServerConstants.CONTEXT_KEY_CONN_REMOTE_IP,
                        call.getAttributes().get(GrpcServerConstants.ATTR_TRANS_KEY_REMOTE_IP))
                .withValue(GrpcServerConstants.CONTEXT_KEY_CONN_REMOTE_PORT,
                        call.getAttributes().get(GrpcServerConstants.ATTR_TRANS_KEY_REMOTE_PORT))
                .withValue(GrpcServerConstants.CONTEXT_KEY_CONN_LOCAL_PORT,
                        call.getAttributes().get(GrpcServerConstants.ATTR_TRANS_KEY_LOCAL_PORT));

        return Contexts.interceptCall(ctx, call, headers, next);
    }
}