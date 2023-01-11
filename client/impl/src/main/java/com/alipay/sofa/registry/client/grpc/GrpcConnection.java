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
package com.alipay.sofa.registry.client.grpc;

import com.alipay.sofa.registry.client.remoting.ServerNode;
import com.alipay.sofa.registry.core.grpc.auto.BiRequestStreamGrpc;
import com.alipay.sofa.registry.core.grpc.auto.Payload;
import com.alipay.sofa.registry.core.grpc.auto.RequestGrpc;
import com.alipay.sofa.registry.core.utils.GrpcUtils;
import com.google.common.util.concurrent.ListenableFuture;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;
import java.util.concurrent.TimeUnit;

/**
 * @author chengzhengzheng
 * @date 2022/11/23
 */
public class GrpcConnection {
  private ServerNode serverNode;
  private String connectionId;
  protected ManagedChannel channel;

  protected StreamObserver<Payload> payloadStreamObserver;
  public static final int RECONNECTING_DELAY = 5000;
  /** stub to send request. */
  protected RequestGrpc.RequestFutureStub grpcFutureServiceStub;
  /** stub to send blocking request **/
  private RequestGrpc.RequestBlockingStub requestBlockingStub;

  public GrpcConnection(ServerNode serverNode) {
    this.serverNode = serverNode;
  }

  public String getConnectionId() {
    return connectionId;
  }

  public void setConnectionId(String connectionId) {
    this.connectionId = connectionId;
  }

  public void setPayloadStreamObserver(StreamObserver<Payload> payloadStreamObserver) {
    this.payloadStreamObserver = payloadStreamObserver;
  }
  /**
   * Setter method for property <tt>grpcFutureServiceStub</tt>.
   *
   * @param grpcFutureServiceStub value to be assigned to property grpcFutureServiceStub
   */
  public void setGrpcFutureServiceStub(RequestGrpc.RequestFutureStub grpcFutureServiceStub) {
    this.grpcFutureServiceStub = grpcFutureServiceStub;
  }

  public void setChannel(ManagedChannel managedChannel) {
    this.channel = managedChannel;
  }

  public <T> void sendRequest(T request) {
    Payload convert = GrpcUtils.convert(request);
    payloadStreamObserver.onNext(convert);
  }

  public <T> Payload request(T request, long timeouts) {
    Payload grpcRequest = GrpcUtils.convert(request);
    ListenableFuture<Payload> requestFuture = grpcFutureServiceStub.request(grpcRequest);
    try {
      return requestFuture.get(1,TimeUnit.DAYS);
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  public <T> Payload requestSync(T request) {
    Payload grpcRequest = GrpcUtils.convert(request);
    return requestBlockingStub.request(grpcRequest);
  }

  public boolean isFine() {
    return channel != null && !channel.isShutdown();
  }

  public void sendResponse(Object response) {
    Payload convert = GrpcUtils.convert(response);
    payloadStreamObserver.onNext(convert);
  }

  public void close() {
    if (this.payloadStreamObserver != null) {
      try {
        payloadStreamObserver.onCompleted();
      } catch (Throwable throwable) {
        // ignore.
      }
    }

    if (this.channel != null && !channel.isShutdown()) {
      try {
        this.channel.shutdownNow();
      } catch (Throwable throwable) {
        // ignore.
      }
    }
  }

  public void setGrpcBlockingStub(RequestGrpc.RequestBlockingStub requestBlockingStub) {
    this.requestBlockingStub = requestBlockingStub;
  }
}
