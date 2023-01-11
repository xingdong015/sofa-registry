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
package grpc.exchange;

import com.alipay.sofa.registry.common.model.store.URL;
import com.alipay.sofa.registry.remoting.ChannelHandler;
import com.alipay.sofa.registry.remoting.Client;
import com.alipay.sofa.registry.remoting.Server;
import com.alipay.sofa.registry.remoting.exchange.Exchange;
import grpc.GrpcServer;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author chengzhengzheng
 * @date 2022/11/4
 */
public class GrpcExchange implements Exchange<ChannelHandler> {

  private final ConcurrentHashMap<Integer, Server> serverMap = new ConcurrentHashMap<>();

  @Override
  public Client connect(String serverType, URL serverUrl, ChannelHandler... definitions) {
    return null;
  }

  @Override
  public Client connect(
      String serverType, int connNum, URL serverUrl, ChannelHandler... channelHandlers) {
    return null;
  }

  @Override
  public Server open(URL url, ChannelHandler... definitions) {
    GrpcServer server = createServer(url, definitions);
    server.startServer();
    return server;
  }

  private GrpcServer createServer(URL url, ChannelHandler[] definitions) {
    if (definitions == null) {
      throw new IllegalArgumentException("channelHandlers cannot be null!");
    }
    GrpcServer server = createGrpcServer(url, definitions);
    setServer(server, url);
    return server;
  }

  private void setServer(GrpcServer server, URL url) {
    serverMap.putIfAbsent(url.getPort(), server);
  }

  private GrpcServer createGrpcServer(URL url, ChannelHandler[] definitions) {
    return new GrpcServer(url, Arrays.asList(definitions));
  }

  @Override
  public Server open(URL url, int lowWaterMark, int highWaterMark, ChannelHandler... definitions) {
    return null;
  }

  @Override
  public Client getClient(String serverType) {
    return null;
  }

  @Override
  public Server getServer(Integer port) {
    return serverMap.get(port);
  }

}
