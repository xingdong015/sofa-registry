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

import com.alipay.sofa.registry.core.grpc.auto.Payload;
import com.alipay.sofa.registry.core.grpc.request.Request;
import com.alipay.sofa.registry.core.grpc.response.Response;
import com.alipay.sofa.registry.core.utils.GrpcUtils;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import io.grpc.StatusRuntimeException;
import io.grpc.netty.shaded.io.netty.channel.Channel;
import io.grpc.stub.ServerCallStreamObserver;

import java.util.Map;

/**
 * @author chengzhengzheng
 * @date 2022/11/20
 */
@JsonPOJOBuilder
public class GrpcConnection extends Connection {

    private final ServerCallStreamObserver streamObserver;

    private Channel channel;

    public GrpcConnection(
            String connectionId,
            String clientIp,
            int localPort,
            String remoteIp,
            int remotePort,
            String version,
            Map<String, String> attributes,
            ServerCallStreamObserver streamObserver,
            Channel channel) {
        super(connectionId, clientIp, localPort, remoteIp, remotePort, version, attributes);
        this.streamObserver = streamObserver;
        this.channel        = channel;
    }

    @Override
    public boolean isConnected() {
        return channel != null && channel.isOpen() && channel.isActive();
    }

    @Override
    public void close() {
        try {
            closeBiStream();

            channel.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void closeBiStream() {
        if (streamObserver.isCancelled()) {
            return;
        }
        streamObserver.onCompleted();
    }


    @Override
    public RequestFuture requestFuture(Request request) {
        return sendRequestInner(request, null);
    }

    @Override
    public Response request(Request request, long timeoutMills) {
        DefaultRequestFuture pushFuture = sendRequestInner(request, null);
        try {
            return pushFuture.get(timeoutMills);
        } catch (Exception e) {
            throw new RuntimeException();
        } finally {
            RpcAckCallbackSynchronizer.clearFuture(getConnectionId(), pushFuture.getRequestId());
        }
    }

    @Override
    public void asyncRequest(Request request, RequestCallBack requestCallBack) {
        sendRequestInner(request, requestCallBack);
    }

    private DefaultRequestFuture sendRequestInner(Request request, RequestCallBack callBack) {
        final String requestId = String.valueOf(PushAckIdGenerator.getNextId());
        request.setRequestId(requestId);

        DefaultRequestFuture defaultPushFuture = new DefaultRequestFuture(getConnectionId(), requestId,
                callBack, () -> RpcAckCallbackSynchronizer.clearFuture(getConnectionId(), requestId));

        RpcAckCallbackSynchronizer.syncCallback(getConnectionId(), requestId, defaultPushFuture);

        sendRequestNoAck(request);
        return defaultPushFuture;
    }

    private void sendRequestNoAck(Object request) {
        try {
            //StreamObserver#onNext() is not thread-safe,synchronized is required to avoid direct memory leak.
            synchronized (streamObserver) {

                Payload payload = GrpcUtils.convert(request);
                streamObserver.onNext(payload);
            }
        } catch (Exception e) {
            if (e instanceof StatusRuntimeException) {
                throw new RuntimeException(e);
            }
            throw e;
        }
    }


}
