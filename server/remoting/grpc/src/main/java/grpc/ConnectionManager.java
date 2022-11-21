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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author chengzhengzheng
 * @date 2022/11/20
 */
public class ConnectionManager {

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
}
