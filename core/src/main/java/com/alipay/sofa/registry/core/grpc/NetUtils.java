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
package com.alipay.sofa.registry.core.grpc;

import io.grpc.netty.shaded.io.netty.util.internal.StringUtil;
import java.io.IOException;
import java.net.*;
import java.util.Enumeration;

/**
 * Net utils.
 *
 * @author xuanyin.zy
 */
public class NetUtils {

  @Deprecated
  private static final String CLIENT_NAMING_LOCAL_IP_PROPERTY =
      "com.alibaba.registry.client.naming.local.ip";

  private static final String CLIENT_LOCAL_IP_PROPERTY = "com.alibaba.registry.client.local.ip";

  private static final String CLIENT_LOCAL_PREFER_HOSTNAME_PROPERTY =
      "com.alibaba.registry.client.local.preferHostname";

  private static final String LEGAL_LOCAL_IP_PROPERTY = "java.net.preferIPv6Addresses";

  private static final String DEFAULT_SOLVE_FAILED_RETURN = "resolve_failed";

  private static String localIp;

  /**
   * Get local ip.
   *
   * @return local ip
   */
  public static String localIP() {
    if (!StringUtil.isNullOrEmpty(localIp)) {
      return localIp;
    }

    if (System.getProperties().containsKey(CLIENT_LOCAL_IP_PROPERTY)) {
      return localIp = System.getProperty(CLIENT_LOCAL_IP_PROPERTY, getAddress());
    }

    String ip = System.getProperty(CLIENT_NAMING_LOCAL_IP_PROPERTY, getAddress());

    return localIp = ip;
  }

  private static String getAddress() {
    InetAddress inetAddress = findFirstNonLoopbackAddress();
    if (inetAddress == null) {
      return DEFAULT_SOLVE_FAILED_RETURN;
    }

    boolean preferHost =
        Boolean.parseBoolean(System.getProperty(CLIENT_LOCAL_PREFER_HOSTNAME_PROPERTY));
    return preferHost ? inetAddress.getHostName() : inetAddress.getHostAddress();
  }

  private static InetAddress findFirstNonLoopbackAddress() {
    InetAddress result = null;

    try {
      int lowest = Integer.MAX_VALUE;
      for (Enumeration<NetworkInterface> nics = NetworkInterface.getNetworkInterfaces();
          nics.hasMoreElements(); ) {
        NetworkInterface ifc = nics.nextElement();
        if (ifc.isUp()) {
          if (ifc.getIndex() < lowest || result == null) {
            lowest = ifc.getIndex();
          } else {
            continue;
          }

          for (Enumeration<InetAddress> addrs = ifc.getInetAddresses(); addrs.hasMoreElements(); ) {
            InetAddress address = addrs.nextElement();
            boolean isLegalIpVersion =
                Boolean.parseBoolean(System.getProperty(LEGAL_LOCAL_IP_PROPERTY))
                    ? address instanceof Inet6Address
                    : address instanceof Inet4Address;
            if (isLegalIpVersion && !address.isLoopbackAddress()) {
              result = address;
            }
          }
        }
      }
    } catch (IOException ex) {
      // ignore
    }

    if (result != null) {
      return result;
    }

    try {
      return InetAddress.getLocalHost();
    } catch (UnknownHostException e) {
      // ignore
    }

    return null;
  }
}
