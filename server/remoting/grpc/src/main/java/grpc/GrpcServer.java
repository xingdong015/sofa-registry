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

import com.alipay.remoting.Url;
import com.alipay.sofa.registry.common.model.store.URL;
import com.alipay.sofa.registry.core.grpc.auto.Payload;
import com.alipay.sofa.registry.log.Logger;
import com.alipay.sofa.registry.log.LoggerFactory;
import com.alipay.sofa.registry.remoting.CallbackHandler;
import com.alipay.sofa.registry.remoting.Channel;
import com.alipay.sofa.registry.remoting.ChannelHandler;
import com.alipay.sofa.registry.remoting.Server;
import com.google.common.collect.Lists;
import grpc.handler.GrpcBiStreamRequestAcceptor;
import grpc.handler.GrpcCommonRequestAcceptor;
import io.grpc.*;
import io.grpc.protobuf.ProtoUtils;
import io.grpc.stub.ServerCalls;
import io.grpc.util.MutableHandlerRegistry;

import java.lang.reflect.ParameterizedType;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author chengzhengzheng
 * @date 2022/11/6
 */
public class GrpcServer implements Server {

    private static final Logger LOGGER = LoggerFactory.getLogger(GrpcServer.class);

    private io.grpc.Server grpcServer;

    private final AtomicBoolean isStarted = new AtomicBoolean(false);

    protected final URL url;

    private final List<ChannelHandler> handlers;

    private final RequestHandlerRegistry requestHandlerRegistry;

    private final ConnectionManager connectionManager;

    public GrpcServer(URL url, List<ChannelHandler> grpcDefinitions) {
        this.url                    = url;
        this.handlers               = grpcDefinitions;
        this.requestHandlerRegistry = new RequestHandlerRegistry();
        this.connectionManager      = new ConnectionManager();
        this.grpcServer             = newGrpcServer();
    }

    private io.grpc.Server newGrpcServer() {
        final MutableHandlerRegistry handlerRegistry = new MutableHandlerRegistry();
        addServices(handlerRegistry, new ConnectionInterceptor());
        // todo 参考 https://github.com/grpc/proposal/blob/master/A9-server-side-conn-mgt.md
        return ServerBuilder.forPort(url.getPort())
                .executor(GrpcServerConstants.GrpcConfig.GRPC_SERVER_THREAD_POOL_EXECUTOR)
                .maxInboundMessageSize(getMaxInboundMessageSize()).fallbackHandlerRegistry(handlerRegistry)
                .compressorRegistry(CompressorRegistry.getDefaultInstance())
                .decompressorRegistry(DecompressorRegistry.getDefaultInstance())
                .addTransportFilter(new AddressTransportFilter(connectionManager))
                .maxConnectionIdle(getMaxConnectionIdle(), TimeUnit.MILLISECONDS)
                .keepAliveTime(getKeepAliveTime(), TimeUnit.MILLISECONDS)
                .keepAliveTimeout(getKeepAliveTimeout(), TimeUnit.MILLISECONDS)
                .permitKeepAliveTime(getPermitKeepAliveTime(), TimeUnit.MILLISECONDS)
                .build();
    }

    private void addServices(
            MutableHandlerRegistry handlerRegistry, ServerInterceptor serverInterceptor) {
        // unary common call register.
        final MethodDescriptor<Payload, Payload> unaryPayloadMethod =
                MethodDescriptor.<Payload, Payload>newBuilder()
                        .setType(MethodDescriptor.MethodType.UNARY).setFullMethodName(
                                MethodDescriptor.generateFullMethodName(GrpcServerConstants.REQUEST_SERVICE_NAME,
                                        GrpcServerConstants.REQUEST_METHOD_NAME))
                        .setRequestMarshaller(ProtoUtils.marshaller(Payload.getDefaultInstance()))
                        .setResponseMarshaller(ProtoUtils.marshaller(Payload.getDefaultInstance()))
                        .build();


        final ServerCallHandler<Payload, Payload> payloadHandler = ServerCalls.asyncUnaryCall(
                (request, responseObserver) ->
                        new GrpcCommonRequestAcceptor(requestHandlerRegistry, connectionManager).request(request, responseObserver));

        final ServerServiceDefinition serviceDefOfUnaryPayload =
                ServerServiceDefinition.builder(GrpcServerConstants.REQUEST_SERVICE_NAME)
                        .addMethod(unaryPayloadMethod, payloadHandler)
                        .build();
        handlerRegistry.addService(
                ServerInterceptors.intercept(serviceDefOfUnaryPayload, serverInterceptor));

        // bi stream register.
        final ServerCallHandler<Payload, Payload> biStreamHandler =
                ServerCalls.asyncBidiStreamingCall(
                        (responseObserver) ->
                                new GrpcBiStreamRequestAcceptor(connectionManager)
                                        .requestBiStream(responseObserver));

        final MethodDescriptor<Payload, Payload> biStreamMethod =
                MethodDescriptor.<Payload, Payload>newBuilder()
                        .setType(MethodDescriptor.MethodType.BIDI_STREAMING).setFullMethodName(
                                MethodDescriptor.generateFullMethodName(GrpcServerConstants.REQUEST_BI_STREAM_SERVICE_NAME,
                                        GrpcServerConstants.REQUEST_BI_STREAM_METHOD_NAME))
                        .setRequestMarshaller(ProtoUtils.marshaller(Payload.newBuilder().build()))
                        .setResponseMarshaller(ProtoUtils.marshaller(Payload.getDefaultInstance()))
                        .build();

        final ServerServiceDefinition serviceDefOfBiStream =
                ServerServiceDefinition.builder(GrpcServerConstants.REQUEST_BI_STREAM_SERVICE_NAME)
                        .addMethod(biStreamMethod, biStreamHandler)
                        .build();
        handlerRegistry.addService(
                ServerInterceptors.intercept(serviceDefOfBiStream, serverInterceptor));
    }

    @Override
    public InetSocketAddress getLocalAddress() {
        return new InetSocketAddress(url.getPort());
    }

    @Override
    public void close() {
        stopServer();
    }

    private void stopServer() {
        if (grpcServer != null && isStarted.get()) {
            try {
                grpcServer.shutdownNow();
            } catch (Exception e) {
                LOGGER.error("Stop grpc server error!", e);
                throw new RuntimeException("Stop grpc server error!", e);
            }
        }
    }

    @Override
    public boolean isClosed() {
        return !isStarted.get();
    }

    @Override
    public boolean isOpen() {
        return isStarted.get();
    }

    @Override
    public List<Channel> getChannels() {
        Map<String, Connection>                           conns   = connectionManager.getAll();
        if (conns.isEmpty()) {
            return Collections.emptyList();
        }
        List<Channel> ret = Lists.newArrayListWithCapacity(128);
        for (Map.Entry<String, Connection> entry : conns.entrySet()) {
            Connection conn = entry.getValue();
            if (conn.isConnected()) {
                GrpcChannel boltChannel = new GrpcChannel(conn);
                ret.add(boltChannel);
            }
        }
        return ret;
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
        URL url = new URL(remoteAddress.getAddress().getHostAddress(), remoteAddress.getPort());
        return getChannel(url);
    }

    @Override
    public Channel getChannel(URL url) {
        Url        key        = new Url(url.getIpAddress(), url.getPort());
        Connection connection = connectionManager.getConnection(key.getUniqueKey());
        if (Objects.isNull(connection)) {
            return null;
        }
        return new GrpcChannel(connection);
    }

    @Override
    public void close(Channel channel) {
        if (channel == null) {
            return;
        }
        GrpcChannel                    boltChannel = (GrpcChannel) channel;
        Connection                     connection  = boltChannel.getConnection();
        if (connection.isConnected()) {
            connection.close();
        }
    }

    @Override
    public int getChannelCount() {
        Map<String, Connection>                                conns   = connectionManager.getAll();
        int                                                    count = 0;
        for (Connection conn : conns.values()) {
            if (conn.isConnected()){
                count++;
            }
        }
        return count;
    }

    @Override
    public void sendCallback(
            Channel channel, Object message, CallbackHandler callbackHandler, int timeoutMillis) {
        Url        key        = new Url(url.getIpAddress(), url.getPort());
        Connection connection = connectionManager.getConnection(key.getUniqueKey());
        connection.sendRequest(message);
    }

    @Override
    public Object sendSync(Channel channel, Object message, int timeoutMillis) {
        Connection connection = connectionManager.getConnection("1");
        if (connection != null){
            connection.sendRequest(message);
        }
    }

    public void startServer() {
        if (isStarted.compareAndSet(false, true)) {
            try {
                initHandle();
                grpcServer.start();

                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    try {
                        GrpcServer.this.stopServer();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }));
            } catch (Exception e) {
                isStarted.set(false);
                throw new RuntimeException("Start bolt server error!", e);
            }
        }
    }

    private void initHandle() {
        for (ChannelHandler channelHandler : handlers) {
            if (ChannelHandler.HandlerType.PROCESSER.equals(channelHandler.getType())) {
                Class<?> clazz = channelHandler.getClass();
                Class tClass = (Class) ((ParameterizedType) clazz.getGenericSuperclass()).getActualTypeArguments()[0];
                requestHandlerRegistry.registryHandler(tClass.getSimpleName(), newSyncUserProcessorAdapter(channelHandler));
            }
        }
    }

    protected GrpcUserProcessorAdapter newSyncUserProcessorAdapter(ChannelHandler channelHandler) {
        return new GrpcUserProcessorAdapter(channelHandler);
    }


    protected long getPermitKeepAliveTime() {
        return GrpcServerConstants.GrpcConfig.DEFAULT_GRPC_PERMIT_KEEP_ALIVE_TIME;
    }

    protected long getKeepAliveTime() {
        return GrpcServerConstants.GrpcConfig.DEFAULT_GRPC_KEEP_ALIVE_TIME;
    }

    protected long getKeepAliveTimeout() {
        return GrpcServerConstants.GrpcConfig.DEFAULT_GRPC_KEEP_ALIVE_TIMEOUT;
    }

    protected long getMaxConnectionIdle() {
        return GrpcServerConstants.GrpcConfig.DEFAULT_GRPC_MAX_CONNECTION_IDLE;
    }

    protected int getMaxInboundMessageSize() {
        String propertyStr = System.getProperty(GrpcServerConstants.GrpcConfig.MAX_INBOUND_MSG_SIZE_PROPERTY);
        if (propertyStr != null) {
            return Integer.parseInt(propertyStr);
        }
        return GrpcServerConstants.GrpcConfig.DEFAULT_GRPC_MAX_INBOUND_MSG_SIZE;
    }
}
