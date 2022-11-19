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

import com.alipay.sofa.registry.client.api.RpcClient;
import io.grpc.CompressorRegistry;
import io.grpc.DecompressorRegistry;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author chengzhengzheng
 * @date 2022/11/6
 */
public class GrpcClient extends RpcClient {

  public void request(ServerInfo serverInfo) throws InterruptedException, ExecutionException {

    ManagedChannel managedChannel =
        createNewManagedChannel(serverInfo.getServerIp(), serverInfo.getServerPort());
    //    RequestGrpc.RequestBlockingStub blockingStub =
    // RequestGrpc.newBlockingStub(managedChannel);
    //    Payload grpcRequest = GrpcUtils.convert(new ServerCheckRequest());
    //    Payload payloadResp = blockingStub.request(grpcRequest);
    //
    //    // receive connection unregister response here,not check response is success.
    //    Response response = (Response) GrpcUtils.parse(payloadResp);

    //    GrpcConnection grpcConn = new GrpcConnection(serverInfo, Executors.newCachedThreadPool());

    // create stream request and bind connection event to this connection.
    //    BiRequestStreamGrpc.BiRequestStreamStub biRequestStreamStub =
    //        BiRequestStreamGrpc.newStub(newChannelStubTemp.getChannel());
    //    StreamObserver<Payload> payloadStreamObserver =
    //        bindRequestStream(biRequestStreamStub, grpcConn);

    // stream observer to send response to server
    //    grpcConn.setPayloadStreamObserver(payloadStreamObserver);
    //    grpcConn.setGrpcFutureServiceStub(newChannelStubTemp);
    //    grpcConn.setChannel(managedChannel);
    //    // send a  setup request.
    //    ConnectionSetupRequest conSetupRequest = new ConnectionSetupRequest();
    //    grpcConn.sendRequest(conSetupRequest);
    //    // wait to register connection setup
    //    Thread.sleep(100L);
  }

  /**
   * create a new channel with specific server address.
   *
   * @param serverIp serverIp.
   * @param serverPort serverPort.
   * @return if server check success,return a non-null channel.
   */
  private ManagedChannel createNewManagedChannel(String serverIp, int serverPort) {
    ManagedChannelBuilder<?> managedChannelBuilder =
        ManagedChannelBuilder.forAddress(serverIp, serverPort)
            .executor(Executors.newCachedThreadPool())
            .compressorRegistry(CompressorRegistry.getDefaultInstance())
            .decompressorRegistry(DecompressorRegistry.getDefaultInstance())
            .maxInboundMessageSize(10 * 1024 * 1024)
            .keepAliveTime(6 * 60 * 1000, TimeUnit.MILLISECONDS)
            .usePlaintext();
    return managedChannelBuilder.build();
  }

  //  private StreamObserver<Payload> bindRequestStream(
  //      final BiRequestStreamGrpc.BiRequestStreamStub streamStub, final GrpcConnection grpcConn) {
  //
  //    return streamStub.requestBiStream(
  //        new StreamObserver<Payload>() {
  //
  //          @Override
  //          public void onNext(Payload payload) {
  //
  //            try {
  //              Object parseBody = GrpcUtils.parse(payload);
  //              final Request request = (Request) parseBody;
  //              if (request != null) {
  //
  //                try {
  //                  //                            Response response =
  // handleServerRequest(request);
  //                  //                            if (response != null) {
  //                  //
  // response.setRequestId(request.getRequestId());
  //                  //                                sendResponse(response);
  //                  //                            }
  //
  //                } catch (Exception e) {
  //                  e.printStackTrace();
  //                }
  //              }
  //
  //            } catch (Exception e) {
  //              e.printStackTrace();
  //            }
  //          }
  //
  //          @Override
  //          public void onError(Throwable throwable) {}
  //
  //          @Override
  //          public void onCompleted() {}
  //        });
  //  }
}
