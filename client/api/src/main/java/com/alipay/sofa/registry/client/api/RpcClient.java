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
package com.alipay.sofa.registry.client.api;

/**
 * @author chengzhengzheng
 * @date 2022/11/6
 */
public abstract class RpcClient {

  public static class ServerInfo {

    protected String serverIp;

    protected int serverPort;

    public ServerInfo() {}

    public ServerInfo(String serverIp, int serverPort) {
      this.serverPort = serverPort;
      this.serverIp = serverIp;
    }

    public String getAddress() {
      return serverIp + ":" + serverPort;
    }

    public void setServerIp(String serverIp) {
      this.serverIp = serverIp;
    }

    public void setServerPort(int serverPort) {
      this.serverPort = serverPort;
    }

    public String getServerIp() {
      return serverIp;
    }

    public int getServerPort() {
      return serverPort;
    }

    @Override
    public String toString() {
      return "{serverIp = '" + serverIp + '\'' + ", server main port = " + serverPort + '}';
    }
  }
}
