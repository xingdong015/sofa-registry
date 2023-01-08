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
import com.alipay.sofa.registry.core.utils.GrpcUtils;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.ServerCallStreamObserver;

import java.util.Map;

/**
 * @author chengzhengzheng
 * @date 2022/11/20
 */
@JsonPOJOBuilder
public class GrpcConnection extends Connection {

    private final ServerCallStreamObserver streamObserver;

    public GrpcConnection(
            String connectionId,
            String clientIp,
            int localPort,
            String remoteIp,
            int remotePort,
            String version,
            Map<String, String> attributes,
            ServerCallStreamObserver streamObserver) {
        super(connectionId, clientIp, localPort, remoteIp, remotePort, version, attributes);
        this.streamObserver = streamObserver;
    }

    @Override
    public boolean isConnected() {
        return streamObserver.isReady();
    }

    @Override
    public void close() {
        try {
            closeBiStream();
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
    protected void sendRequest(Object request)  {
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
