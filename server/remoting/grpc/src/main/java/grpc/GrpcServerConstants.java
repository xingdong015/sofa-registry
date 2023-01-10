package grpc;

import io.grpc.Attributes;
import io.grpc.Context;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import io.grpc.netty.shaded.io.netty.channel.Channel;

/**
 * @author chengzhengzheng
 * @date 2023/1/4
 */
public class GrpcServerConstants {
    public static final Attributes.Key<String> ATTR_TRANS_KEY_CONN_ID = Attributes.Key.create("conn_id");

    public static final Attributes.Key<String> ATTR_TRANS_KEY_REMOTE_IP = Attributes.Key.create("remote_ip");

    public static final Attributes.Key<Integer> ATTR_TRANS_KEY_REMOTE_PORT = Attributes.Key.create("remote_port");

    public static final Attributes.Key<Integer> ATTR_TRANS_KEY_LOCAL_PORT = Attributes.Key.create("local_port");

    public static final Context.Key<Channel> CONTEXT_KEY_CHANNEL = Context.key("ctx_channel");

    public static final Context.Key<String> CONTEXT_KEY_CONN_ID = Context.key("conn_id");

    public static final Context.Key<String> CONTEXT_KEY_CONN_REMOTE_IP = Context.key("remote_ip");

    public static final Context.Key<Integer> CONTEXT_KEY_CONN_REMOTE_PORT = Context.key("remote_port");

    public static final Context.Key<Integer> CONTEXT_KEY_CONN_LOCAL_PORT = Context.key("local_port");

    public static final String REQUEST_BI_STREAM_SERVICE_NAME = "BiRequestStream";

    public static final String REQUEST_BI_STREAM_METHOD_NAME = "requestBiStream";

    public static final String REQUEST_SERVICE_NAME = "Request";

    public static final String REQUEST_METHOD_NAME = "request";

    static class GrpcConfig {

        public static final  ThreadPoolExecutor GRPC_SERVER_THREAD_POOL_EXECUTOR =
                new ThreadPoolExecutor(10, 10, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());

        private static final String             REGISTRY_REMOTE_SERVER_GRPC_PREFIX = "registry.remote.server.grpc.";

        public static final String MAX_INBOUND_MSG_SIZE_PROPERTY =
                REGISTRY_REMOTE_SERVER_GRPC_PREFIX + "max-inbound-message-size";

        public static final int DEFAULT_GRPC_MAX_INBOUND_MSG_SIZE = 10 * 1024 * 1024;

        public static final long DEFAULT_GRPC_KEEP_ALIVE_TIME = TimeUnit.MINUTES.toMillis(6L);

        public static final long DEFAULT_GRPC_KEEP_ALIVE_TIMEOUT = TimeUnit.MINUTES.toMillis(18L);

        public static final long DEFAULT_GRPC_MAX_CONNECTION_IDLE = TimeUnit.SECONDS.toMillis(30L);

        public static final long DEFAULT_GRPC_PERMIT_KEEP_ALIVE_TIME = TimeUnit.MINUTES.toMillis(10L);
    }
}
