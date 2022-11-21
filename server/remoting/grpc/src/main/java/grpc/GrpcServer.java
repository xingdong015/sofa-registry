/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package grpc;

import com.alipay.sofa.registry.common.model.client.pb.Payload;
import com.alipay.sofa.registry.common.model.store.URL;
import com.alipay.sofa.registry.remoting.CallbackHandler;
import com.alipay.sofa.registry.remoting.Channel;
import com.alipay.sofa.registry.remoting.ChannelHandler;
import com.alipay.sofa.registry.remoting.Server;
import io.grpc.*;
import io.grpc.internal.ServerStream;
import io.grpc.protobuf.ProtoUtils;
import io.grpc.stub.ServerCalls;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author chengzhengzheng
 * @date 2022/11/6
 */
public class GrpcServer implements Server {

    private io.grpc.Server grpcServer;

    private static final String REQUEST_BI_STREAM_SERVICE_NAME = "BiRequestStream";

    private static final String REQUEST_BI_STREAM_METHOD_NAME = "requestBiStream";

    private static final String REQUEST_SERVICE_NAME = "Request";

    private static final String REQUEST_METHOD_NAME = "request";

    private final AtomicBoolean isStarted = new AtomicBoolean(false);

    static final Attributes.Key<String> TRANS_KEY_CONN_ID = Attributes.Key.create("conn_id");

    static final Attributes.Key<String> TRANS_KEY_REMOTE_IP = Attributes.Key.create("remote_ip");

    static final Attributes.Key<Integer> TRANS_KEY_REMOTE_PORT = Attributes.Key.create("remote_port");

    static final Attributes.Key<Integer> TRANS_KEY_LOCAL_PORT = Attributes.Key.create("local_port");

    static final Context.Key<String> CONTEXT_KEY_CONN_ID = Context.key("conn_id");

    static final Context.Key<String> CONTEXT_KEY_CONN_REMOTE_IP = Context.key("remote_ip");

    static final Context.Key<Integer> CONTEXT_KEY_CONN_REMOTE_PORT = Context.key("remote_port");

    static final Context.Key<Integer> CONTEXT_KEY_CONN_LOCAL_PORT = Context.key("local_port");

    static final    Context.Key<io.grpc.netty.shaded.io.netty.channel.Channel> CONTEXT_KEY_CHANNEL = Context.key("ctx_channel");
    /**
     * accoding server port can not be null
     */
    protected final URL                                                        url;

    private final List<ChannelHandler> handlers;

    private final RequestHandlerRegistry requestHandlerRegistry;

    private final ConnectionManager connectionManager;

    public GrpcServer(URL url, List<ChannelHandler> grpcDefinitions) {
        this.url                    = url;
        this.handlers               = grpcDefinitions;
        this.grpcServer             = newGrpcServer();
        this.requestHandlerRegistry = new RequestHandlerRegistry();
        this.connectionManager      = new ConnectionManager();
    }

    private io.grpc.Server newGrpcServer() {
        ServerBuilder<?> serverBuilder = ServerBuilder.forPort(url.getPort()).executor(GrpcUtils.grpcServerExecutor).maxInboundMessageSize(10 * 1024 * 1024).compressorRegistry(CompressorRegistry.getDefaultInstance()).decompressorRegistry(DecompressorRegistry.getDefaultInstance()).addTransportFilter(new ServerTransportFilter() {
            @Override
            public Attributes transportReady(Attributes transportAttrs) {
                InetSocketAddress remoteAddress = (InetSocketAddress) transportAttrs.get(Grpc.TRANSPORT_ATTR_REMOTE_ADDR);
                InetSocketAddress localAddress  = (InetSocketAddress) transportAttrs.get(Grpc.TRANSPORT_ATTR_LOCAL_ADDR);
                int               remotePort    = remoteAddress.getPort();
                int               localPort     = localAddress.getPort();
                String            remoteIp      = remoteAddress.getAddress().getHostAddress();
                return transportAttrs.toBuilder().
                        set(TRANS_KEY_CONN_ID, System.currentTimeMillis() + "_" + remoteIp + "_" + remotePort).
                        set(TRANS_KEY_REMOTE_IP, remoteIp).
                        set(TRANS_KEY_REMOTE_PORT, remotePort).
                        set(TRANS_KEY_LOCAL_PORT, localPort).build();
            }

            @Override
            public void transportTerminated(Attributes transportAttrs) {
                System.out.println("transportTerminated.......");
            }
        });

        ServerInterceptor serverInterceptor = new ServerInterceptor() {
            @Override
            public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
                // 请求的前置设置
                Context ctx = Context.current().
                        withValue(CONTEXT_KEY_CONN_ID, call.getAttributes().get(TRANS_KEY_CONN_ID)).
                        withValue(CONTEXT_KEY_CONN_REMOTE_IP, call.getAttributes().get(TRANS_KEY_REMOTE_IP)).
                        withValue(CONTEXT_KEY_CONN_REMOTE_PORT, call.getAttributes().get(TRANS_KEY_REMOTE_PORT)).
                        withValue(CONTEXT_KEY_CONN_LOCAL_PORT, call.getAttributes().get(TRANS_KEY_LOCAL_PORT));
                if (REQUEST_BI_STREAM_SERVICE_NAME.equals(call.getMethodDescriptor().getServiceName())) {
                    io.grpc.netty.shaded.io.netty.channel.Channel internalChannel = getInternalChannel(call);
                    ctx = ctx.withValue(CONTEXT_KEY_CHANNEL, internalChannel);
                }
                return Contexts.interceptCall(ctx, call, headers, next);
            }
        };

        return addServices(serverBuilder, serverInterceptor).build();
    }

    private ServerBuilder addServices(ServerBuilder serverBuilder, ServerInterceptor... serverInterceptor) {
        // unary common call register.
        final MethodDescriptor<Payload, Payload> unaryPayloadMethod = MethodDescriptor.<Payload, Payload>newBuilder().setType(MethodDescriptor.MethodType.UNARY).setFullMethodName(MethodDescriptor.generateFullMethodName(REQUEST_SERVICE_NAME, REQUEST_METHOD_NAME)).setRequestMarshaller(ProtoUtils.marshaller(Payload.getDefaultInstance())).setResponseMarshaller(ProtoUtils.marshaller(Payload.getDefaultInstance())).build();

        final ServerCallHandler<Payload, Payload> payloadHandler = ServerCalls.asyncUnaryCall((request, responseObserver) -> new GrpcCommonRequestAcceptor(requestHandlerRegistry, connectionManager).request(request, responseObserver));

        final ServerServiceDefinition serviceDefOfUnaryPayload = ServerServiceDefinition.builder(REQUEST_SERVICE_NAME).addMethod(unaryPayloadMethod, payloadHandler).build();
        serverBuilder.addService(ServerInterceptors.intercept(serviceDefOfUnaryPayload, serverInterceptor));

        // bi stream register.
        final ServerCallHandler<Payload, Payload> biStreamHandler = ServerCalls.asyncBidiStreamingCall((responseObserver) -> new GrpcBiStreamRequestAcceptor(requestHandlerRegistry, connectionManager).requestBiStream(responseObserver));

        final MethodDescriptor<Payload, Payload> biStreamMethod = MethodDescriptor.<Payload, Payload>newBuilder().setType(MethodDescriptor.MethodType.BIDI_STREAMING).setFullMethodName(MethodDescriptor.generateFullMethodName(REQUEST_BI_STREAM_SERVICE_NAME, REQUEST_BI_STREAM_METHOD_NAME)).setRequestMarshaller(ProtoUtils.marshaller(Payload.newBuilder().build())).setResponseMarshaller(ProtoUtils.marshaller(Payload.getDefaultInstance())).build();

        final ServerServiceDefinition serviceDefOfBiStream = ServerServiceDefinition.builder(REQUEST_BI_STREAM_SERVICE_NAME).addMethod(biStreamMethod, biStreamHandler).build();
        serverBuilder.addService(ServerInterceptors.intercept(serviceDefOfBiStream, serverInterceptor));

        return serverBuilder;
    }

    @Override
    public InetSocketAddress getLocalAddress() {
        return null;
    }

    @Override
    public void close() {
    }

    @Override
    public boolean isClosed() {
        return false;
    }

    @Override
    public boolean isOpen() {
        return false;
    }

    @Override
    public List<Channel> getChannels() {
        return null;
    }

    @Override
    public Map<String, Channel> selectAvailableChannelsForHostAddress() {
        return null;
    }

    @Override
    public Map<String, List<Channel>> selectAllAvailableChannelsForHostAddress() {
        return null;
    }

    @Override
    public Channel getChannel(InetSocketAddress remoteAddress) {
        return null;
    }

    @Override
    public Channel getChannel(URL url) {
        return null;
    }

    @Override
    public void close(Channel channel) {
    }

    @Override
    public int getChannelCount() {
        return 0;
    }

    @Override
    public void sendCallback(Channel channel, Object message, CallbackHandler callbackHandler, int timeoutMillis) {
    }

    @Override
    public Object sendSync(Channel channel, Object message, int timeoutMillis) {
        return null;
    }

    public void startServer() {
        if (isStarted.compareAndSet(false, true)) {
            try {
                initHandlerRegistry();
                grpcServer.start();
            } catch (Exception e) {
                isStarted.set(false);
                throw new RuntimeException("Start bolt server error!", e);
            }
        }
    }

    private void initHandlerRegistry() {
        for (ChannelHandler handler : handlers) {
            Class<?> clazz  = handler.getClass();
            Class    tClass = (Class) ((ParameterizedType) clazz.getGenericSuperclass()).getActualTypeArguments()[0];
            requestHandlerRegistry.registryHandler(tClass.getSimpleName(), handler);
        }
    }

    private static io.grpc.netty.shaded.io.netty.channel.Channel getInternalChannel(ServerCall serverCall) {
        ServerStream serverStream = (ServerStream) getFieldValue(serverCall, "stream");
        return (io.grpc.netty.shaded.io.netty.channel.Channel) getFieldValue(serverStream, "channel");
    }

    /**
     * get filed value of obj.
     *
     * @param obj       obj.
     * @param fieldName file name to get value.
     * @return field value.
     */
    public static Object getFieldValue(Object obj, String fieldName) {
        try {
            Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * get filed value of obj.
     *
     * @param obj       obj.
     * @param fieldName file name to get value.
     * @return field value.
     */
    public static Object getFieldValue(Object obj, String fieldName, Object defaultValue) {
        try {
            Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(obj);
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
