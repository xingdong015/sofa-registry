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

import java.net.InetSocketAddress;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author chengzhengzheng
 * @date 2022/11/20
 */
public abstract class Connection {

  /** Client IP Address. */
  String clientIp;

  /** Remote IP Address. */
  String remoteIp;

  /** Remote IP Port. */
  int remotePort;

  /** Local Ip Port. */
  int localPort;
  /** Client version. */
  String version;

  /** Identify Unique connectionId. */
  String connectionId;

  /** create time. */
  Date createTime;


  protected Map<String, String> attributes = new HashMap<>();

  public String getAttribute(String key) {
    return attributes.get(key);
  }

  public void putAttribute(String key, String value) {
    this.attributes.put(key, value);
  }

  public Connection(
      String connectionId,
      String clientIp,
      int localPort,
      String remoteIp,
      int remotePort,
      String version,
      Map<String, String> attributes) {
    this.connectionId = connectionId;
    this.clientIp = clientIp;
    this.version = version;
    this.remoteIp = remoteIp;
    this.remotePort = remotePort;
    this.localPort = localPort;
    this.createTime = new Date();
    this.attributes.putAll(attributes);
  }

  /**
   * check is connected.
   *
   * @return if connection or not,check the inner connection is active.
   */
  public abstract boolean isConnected();

  /**
   * 关闭连接
   *
   * @return
   */
  public abstract void close();

  public InetSocketAddress getRemoteAddress() {
    return new InetSocketAddress(remoteIp, remotePort);
  }

  public String getConnectionId() {
    return connectionId;
  }

  @Override
  public String toString() {
    return "Connection{"
        + "clientIp='"
        + clientIp
        + '\''
        + ", remoteIp='"
        + remoteIp
        + '\''
        + ", remotePort="
        + remotePort
        + ", localPort="
        + localPort
        + ", version='"
        + version
        + '\''
        + ", connectionId='"
        + connectionId
        + '\''
        + ", createTime="
        + createTime
        + ", attributes="
        + attributes
        + '}';
  }
}
