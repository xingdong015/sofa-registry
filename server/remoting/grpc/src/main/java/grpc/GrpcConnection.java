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

import io.grpc.netty.shaded.io.netty.channel.Channel;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import java.util.Map;

/**
 * @author chengzhengzheng
 * @date 2022/11/20
 */
public class GrpcConnection extends Connection {

  private StreamObserver streamObserver;

  private Channel channel;

  public GrpcConnection(
      String connectionId,
      String clientIp,
      int localPort,
      String remoteIp,
      int remotePort,
      String version,
      Map<String, String> attributes,
      StreamObserver streamObserver,
      Channel channel) {
    super(connectionId, clientIp, localPort, remoteIp, remotePort, version, attributes);
    this.streamObserver = streamObserver;
    this.channel = channel;
  }

  @Override
  public boolean isConnected() {
    return channel != null && channel.isActive();
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
    if (streamObserver instanceof ServerCallStreamObserver) {
      ServerCallStreamObserver serverCallStreamObserver =
          ((ServerCallStreamObserver) streamObserver);
      if (!serverCallStreamObserver.isCancelled()) {
        serverCallStreamObserver.onCompleted();
      }
    }
  }
}
