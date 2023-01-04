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
package grpc.handler;

import static grpc.GrpcServer.*;

import com.alipay.sofa.registry.core.grpc.BiRequestStreamGrpc;
import com.alipay.sofa.registry.core.grpc.ConnectionSetupRequest;
import com.alipay.sofa.registry.core.grpc.Payload;
import com.alipay.sofa.registry.core.utils.GrpcUtils;
import grpc.Connection;
import grpc.ConnectionManager;
import grpc.GrpcConnection;
import grpc.RequestHandlerRegistry;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author chengzhengzheng
 * @date 2022/11/20
 */
public class GrpcBiStreamRequestAcceptor extends BiRequestStreamGrpc.BiRequestStreamImplBase {
    public static final Logger                 LOGGER = LoggerFactory.getLogger(GrpcBiStreamRequestAcceptor.class);

    private final RequestHandlerRegistry requestHandlerRegistry;

    private final ConnectionManager connectionManager;

    public GrpcBiStreamRequestAcceptor(
            RequestHandlerRegistry requestHandlerRegistry, ConnectionManager connectionManager) {
        this.requestHandlerRegistry = requestHandlerRegistry;
        this.connectionManager      = connectionManager;
    }

    @Override
    public StreamObserver<Payload> requestBiStream(StreamObserver<Payload> responseObserver) {

        return new StreamObserver<Payload>() {

            final String connectionId = CONTEXT_KEY_CONN_ID.get();

            final Integer localPort = CONTEXT_KEY_CONN_LOCAL_PORT.get();

            final int remotePort = CONTEXT_KEY_CONN_REMOTE_PORT.get();

            String remoteIp = CONTEXT_KEY_CONN_REMOTE_IP.get();

            String clientIp = "";

            @Override
            public void onNext(Payload payload) {
                clientIp = payload.getMetadata().getClientIp();
                Object parseObj;
                try {
                    parseObj = GrpcUtils.parse(payload);
                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                    return;
                }
                if (parseObj instanceof ConnectionSetupRequest) {
                    ConnectionSetupRequest setUpRequest = (ConnectionSetupRequest) parseObj;

                    Connection connection = new GrpcConnection(
                            connectionId,
                            payload.getMetadata().getClientIp(),
                            localPort,
                            remoteIp,
                            remotePort,
                            setUpRequest.getClientVersion(),
                            setUpRequest.getAttributes(),
                            responseObserver);

                    if (!connectionManager.register(connectionId, connection)) {
                        LOGGER.error("register to connection manager error {}", connectionId);
                    }
                }
            }

            @Override
            public void onError(Throwable t) {
                // End the response stream if the client presents an error.
                LOGGER.error("requestBiStream error",t);
                responseObserver.onCompleted();
            }

            @Override
            public void onCompleted() {
                // Signal the end of work when the client ends the request stream.
                if (responseObserver instanceof ServerCallStreamObserver) {
                    ServerCallStreamObserver serverCallStreamObserver = ((ServerCallStreamObserver) responseObserver);
                    if (serverCallStreamObserver.isCancelled()) {
                        // client close the stream.
                        serverCallStreamObserver.onCompleted();
                    } else {
                        try {
                            // 结束服务器端的调用
                            serverCallStreamObserver.onCompleted();
                        } catch (Throwable throwable) {
                            // ignore
                            serverCallStreamObserver.onError(throwable);
                        }
                    }
                }
            }
        };
    }
}
