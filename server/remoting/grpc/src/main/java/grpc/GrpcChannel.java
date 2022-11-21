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

import com.alipay.sofa.registry.exception.SofaRegistryRuntimeException;
import com.alipay.sofa.registry.net.NetUtil;
import com.alipay.sofa.registry.remoting.Channel;
import com.alipay.sofa.registry.util.StringFormatter;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.client.WebTarget;

/**
 * @author chengzhengzheng
 * @date 2022/11/20
 */
public class GrpcChannel implements Channel {

  private final Connection connection;

  private Map<String, Object> attributes;

  public GrpcChannel(Connection conn) {
    if (conn == null) {
      throw new SofaRegistryRuntimeException("conn is null.");
    }
    this.connection = conn;
  }

  @Override
  public InetSocketAddress getRemoteAddress() {
    return connection.getRemoteAddress();
  }

  @Override
  public InetSocketAddress getLocalAddress() {
    return NetUtil.getLocalSocketAddress();
  }

  @Override
  public boolean isConnected() {
    return connection.isConnected();
  }

  @Override
  public void setConnAttribute(String key, Object value) {
    // nothing
  }

  @Override
  public Object getConnAttribute(String key) {
    return connection.getAttribute(key);
  }

  @Override
  public synchronized Object getAttribute(String key) {
    return attributes == null ? null : attributes.get(key);
  }

  @Override
  public synchronized void setAttribute(String key, Object value) {
    if (attributes == null) {
      attributes = new HashMap<>();
    }
    if (value == null) {
      attributes.remove(key);
    } else {
      attributes.put(key, value);
    }
  }

  @Override
  public WebTarget getWebTarget() {
    return null;
  }

  @Override
  public void close() {
    this.connection.close();
  }

  @Override
  public String toString() {
    return StringFormatter.format(
        "connected={},remote={},local={}", isConnected(), getRemoteAddress(), getLocalAddress());
  }
}
