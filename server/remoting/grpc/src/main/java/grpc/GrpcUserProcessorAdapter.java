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

import com.alipay.sofa.registry.remoting.ChannelHandler;

/**
 * @author chengzhengzheng
 * @date 2023/1/4
 */
public class GrpcUserProcessorAdapter {

  private final ChannelHandler userProcessorHandler;

  public GrpcUserProcessorAdapter(ChannelHandler userProcessorHandler) {
    this.userProcessorHandler = userProcessorHandler;
  }

  public Object handleRequest(Connection connection, Object request) {
    GrpcChannel grpcChannel = new GrpcChannel(connection);
    return userProcessorHandler.reply(grpcChannel, request);
  }
}
