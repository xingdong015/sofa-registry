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

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * ConnectionMetaInfo.
 *
 * @author liuzunfei
 * @version $Id: ConnectionMetaInfo.java, v 0.1 2020年07月13日 7:28 PM liuzunfei Exp $
 */
public class ConnectionMeta {
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

  /** lastActiveTime. */
  long lastActiveTime;

  protected Map<String, String> labels = new HashMap<>();

  public String getLabel(String labelKey) {
    return labels.get(labelKey);
  }

  public ConnectionMeta(
      String connectionId,
      String clientIp,
      int localPort,
      String remoteIp,
      int remotePort,
      String version,
      Map<String, String> labels) {
    this.connectionId = connectionId;
    this.clientIp = clientIp;
    this.version = version;
    this.remoteIp = remoteIp;
    this.remotePort = remotePort;
    this.localPort = localPort;
    this.createTime = new Date();
    this.lastActiveTime = System.currentTimeMillis();
    this.labels.putAll(labels);
  }

  /**
   * Getter method for property <tt>labels</tt>.
   *
   * @return property value of labels
   */
  public Map<String, String> getLabels() {
    return labels;
  }

  /**
   * Setter method for property <tt>labels</tt>.
   *
   * @param labels value to be assigned to property labels
   */
  public void setLabels(Map<String, String> labels) {
    this.labels = labels;
  }

  /**
   * Getter method for property <tt>clientIp</tt>.
   *
   * @return property value of clientIp
   */
  public String getClientIp() {
    return clientIp;
  }

  /**
   * Setter method for property <tt>clientIp</tt>.
   *
   * @param clientIp value to be assigned to property clientIp
   */
  public void setClientIp(String clientIp) {
    this.clientIp = clientIp;
  }

  /**
   * Getter method for property <tt>connectionId</tt>.
   *
   * @return property value of connectionId
   */
  public String getConnectionId() {
    return connectionId;
  }

  /**
   * Setter method for property <tt>connectionId</tt>.
   *
   * @param connectionId value to be assigned to property connectionId
   */
  public void setConnectionId(String connectionId) {
    this.connectionId = connectionId;
  }

  /**
   * Getter method for property <tt>createTime</tt>.
   *
   * @return property value of createTime
   */
  public Date getCreateTime() {
    return createTime;
  }

  /**
   * Setter method for property <tt>createTime</tt>.
   *
   * @param createTime value to be assigned to property createTime
   */
  public void setCreateTime(Date createTime) {
    this.createTime = createTime;
  }

  /**
   * Getter method for property <tt>lastActiveTime</tt>.
   *
   * @return property value of lastActiveTime
   */
  public long getLastActiveTime() {
    return lastActiveTime;
  }

  /**
   * Setter method for property <tt>lastActiveTime</tt>.
   *
   * @param lastActiveTime value to be assigned to property lastActiveTime
   */
  public void setLastActiveTime(long lastActiveTime) {
    this.lastActiveTime = lastActiveTime;
  }

  /**
   * Getter method for property <tt>version</tt>.
   *
   * @return property value of version
   */
  public String getVersion() {
    return version;
  }

  /**
   * Setter method for property <tt>version</tt>.
   *
   * @param version value to be assigned to property version
   */
  public void setVersion(String version) {
    this.version = version;
  }

  public String getRemoteIp() {
    return remoteIp;
  }

  public int getRemotePort() {
    return remotePort;
  }

  public int getLocalPort() {
    return localPort;
  }

  public void setRemoteIp(String remoteIp) {
    this.remoteIp = remoteIp;
  }

  public void setRemotePort(int remotePort) {
    this.remotePort = remotePort;
  }

  public void setLocalPort(int localPort) {
    this.localPort = localPort;
  }

  @Override
  public String toString() {
    return "ConnectionMeta{"
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
        + ", lastActiveTime="
        + lastActiveTime
        + ", labels="
        + labels
        + '}';
  }
}
