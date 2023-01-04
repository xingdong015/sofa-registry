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

import com.alipay.remoting.Url;
import com.alipay.sofa.registry.log.Logger;
import com.alipay.sofa.registry.log.LoggerFactory;
import org.apache.commons.collections.CollectionUtils;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author chengzhengzheng
 * @date 2022/11/20
 */
public class ConnectionManager {
  private static final int    MAX_TIMES = 5;
  private final        Random random    = new Random();

  private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionManager.class);

  private Map<String, AtomicInteger> connectionForClientIp = new ConcurrentHashMap<>(16);

  Map<String, Connection> connections = new ConcurrentHashMap<>();

  /**
   * check connection id is valid.
   *
   * @param connectionId connectionId to be check.
   * @return is valid or not.
   */
  public boolean checkValid(String connectionId) {
    return connections.containsKey(connectionId);
  }

  /**
   * register a new connect.
   *
   * @param connectionId connectionId
   * @param connection connection
   */
  public synchronized boolean register(String connectionId, Connection connection) {
    if (connection.isConnected()) {
      if (connections.containsKey(connectionId)) {
        return true;
      }
      connections.put(connectionId, connection);
      connectionForClientIp.get(connection.clientIp).getAndIncrement();
      return true;
    }
    return false;
  }

  /**
   * get by connection id.
   *
   * @param connectionId connection id.
   * @return connection of the id.
   */
  public Connection getConnection(String connectionId) {
    return connections.get(connectionId);
  }

  public Connection getConnectionByKey(String uniqueKey){
    final List<Connection> connectionList = new ArrayList<>();
    for (Map.Entry<String, Connection> entry : connections.entrySet()) {
      Connection        connection    = entry.getValue();
      InetSocketAddress remoteAddress = connection.getRemoteAddress();
      InetAddress       address       = remoteAddress.getAddress();
      String            hostAddress   = address.getHostAddress();
      int               port          = remoteAddress.getPort();
      Url               key           = new Url(hostAddress, port);
      if (key.getUniqueKey().equals(uniqueKey)) {
        connectionList.add(connection);
      }
    }

    if (null == connectionList || connectionList.isEmpty()) {
      return null;
    }
    int size = connections.size();
    int tries = 0;
    Connection result = null;
    while ((result == null || !result.isConnected()) && tries++ < MAX_TIMES) {
      result = connectionList.get(this.random.nextInt(size));
    }

    if (result != null && !result.isConnected()) {
      result = null;
    }
    return result;
  }

  /**
   * connection un register
   * @param connectionId
   */
  public void unregister(String connectionId) {
    Connection remove = this.connections.remove(connectionId);
    if (remove != null) {
      String clientIp = remove.clientIp;
      AtomicInteger atomicInteger = connectionForClientIp.get(clientIp);
      if (atomicInteger != null) {
        int count = atomicInteger.decrementAndGet();
        if (count <= 0) {
          connectionForClientIp.remove(clientIp);
        }
      }
      remove.close();
      LOGGER.info("[{}]Connection unregistered successfully. ", connectionId);
    }
  }
}
