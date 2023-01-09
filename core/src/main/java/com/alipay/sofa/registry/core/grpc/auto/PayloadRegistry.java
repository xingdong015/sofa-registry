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
package com.alipay.sofa.registry.core.grpc.auto;

import com.alipay.sofa.registry.core.grpc.request.ConnectionSetupRequest;
import com.alipay.sofa.registry.core.grpc.request.ServerCheckRequest;
import com.alipay.sofa.registry.core.grpc.response.ServerCheckResponse;
import com.alipay.sofa.registry.core.model.*;
import java.util.HashMap;
import java.util.Map;

/**
 * @author chengzhengzheng
 * @date 2022/11/24
 */
public class PayloadRegistry {

  private static final Map<String, Class<?>> REGISTRY_REQUEST = new HashMap<>();

  public static Class getClassByType(String type) {
    return REGISTRY_REQUEST.get(type);
  }

  static {
    init();
  }

  private static void init() {
    REGISTRY_REQUEST.put(PublisherRegister.class.getSimpleName(), PublisherRegister.class);
    REGISTRY_REQUEST.put(RegisterResponse.class.getSimpleName(), RegisterResponse.class);
    REGISTRY_REQUEST.put(SubscriberRegister.class.getSimpleName(), SubscriberRegister.class);
    REGISTRY_REQUEST.put(SyncConfigRequest.class.getSimpleName(), SyncConfigRequest.class);
    REGISTRY_REQUEST.put(SyncConfigResponse.class.getSimpleName(), SyncConfigResponse.class);
    REGISTRY_REQUEST.put(ServerCheckRequest.class.getSimpleName(), ServerCheckRequest.class);
    REGISTRY_REQUEST.put(
        ConnectionSetupRequest.class.getSimpleName(), ConnectionSetupRequest.class);
    REGISTRY_REQUEST.put(ServerCheckResponse.class.getSimpleName(), ServerCheckResponse.class);
  }
}
