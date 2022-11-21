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

import static grpc.GrpcServer.CONTEXT_KEY_CONN_ID;

import com.alipay.sofa.registry.common.model.client.pb.Payload;
import com.alipay.sofa.registry.common.model.client.pb.RequestGrpc;
import com.alipay.sofa.registry.remoting.Channel;
import com.alipay.sofa.registry.server.shared.remoting.AbstractServerHandler;
import io.grpc.stub.StreamObserver;

/**
 * @author chengzhengzheng
 * @date 2022/11/20
 */
public class GrpcCommonRequestAcceptor extends RequestGrpc.RequestImplBase {

  private final RequestHandlerRegistry requestHandlerRegistry;

  private final ConnectionManager connectionManager;

  public GrpcCommonRequestAcceptor(
      RequestHandlerRegistry requestHandlerRegistry, ConnectionManager connectionManager) {
    this.requestHandlerRegistry = requestHandlerRegistry;
    this.connectionManager = connectionManager;
  }

  @Override
  public void request(Payload grpcRequest, StreamObserver<Payload> responseObserver) {
    String connectionId = CONTEXT_KEY_CONN_ID.get();

    // 从 pb 协议中解析出实际的数据对象
    Object parseObj = GrpcUtils.parse(grpcRequest);
    String requestType = grpcRequest.getMetadata().getType();
    AbstractServerHandler requestHandler =
        (AbstractServerHandler) requestHandlerRegistry.getByRequestType(requestType);
    Connection connection = connectionManager.getConnection(connectionId);
    connectionManager.refreshActiveTime(CONTEXT_KEY_CONN_ID.get());
    Channel ch = new GrpcChannel(connection);
    // 模仿 AsyncUserProcessorAdapter.java 自己创建 GrpcChannel 添加封装
    requestHandler.doHandle(ch, parseObj);
  }
}
