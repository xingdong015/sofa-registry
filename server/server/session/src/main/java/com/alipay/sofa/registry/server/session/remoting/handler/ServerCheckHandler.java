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
package com.alipay.sofa.registry.server.session.remoting.handler;

import com.alibaba.fastjson.JSON;
import com.alipay.sofa.registry.core.grpc.request.ServerCheckRequest;
import com.alipay.sofa.registry.core.grpc.response.ServerCheckResponse;
import com.alipay.sofa.registry.remoting.Channel;
import grpc.GrpcChannel;

/**
 * @author chengzhengzheng
 * @date 2022/11/23
 */
public class ServerCheckHandler extends AbstractClientDataRequestHandler<ServerCheckRequest> {
  @Override
  public Class interest() {
    return ServerCheckRequest.class;
  }

  @Override
  public Object doHandle(Channel channel, ServerCheckRequest request) {
    System.out.println("ServerCheckHandler ...." + JSON.toJSONString(request));
    if (channel instanceof GrpcChannel) {
      return new ServerCheckResponse(((GrpcChannel) channel).getConnection().getConnectionId());
    }
    throw new RuntimeException("不支持的通道协议");
  }
}
