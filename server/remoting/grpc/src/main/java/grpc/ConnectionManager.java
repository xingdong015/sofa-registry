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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author chengzhengzheng
 * @date 2022/11/20
 */
public class ConnectionManager {

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
    connections.forEach((s, connection) -> {
      InetSocketAddress remoteAddress = connection.getRemoteAddress();
      InetAddress       address       = remoteAddress.getAddress();
      String            hostAddress   = address.getHostAddress();
      int port = remoteAddress.getPort();
      Url key  = new Url(hostAddress, port);
      if (key.getUniqueKey().equals(uniqueKey)){
        connectionList.add(connection);
      }
    });

    if (CollectionUtils.isEmpty(connectionList)){
      return null;
    }
    List<Connection> snapshot = new ArrayList<Connection>(connectionList);

    return snapshot.get(0);


  }

  /**
   * regresh connection active time.
   *
   * @param connectionId connectionId.
   */
  public void refreshActiveTime(String connectionId) {
    Connection connection = connections.get(connectionId);
    if (connection != null) {
      connection.freshActiveTime();
    }
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
