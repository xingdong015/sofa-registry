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
package com.alipay.sofa.registry.client.demo;

import com.alipay.sofa.registry.client.api.Publisher;
import com.alipay.sofa.registry.client.api.RegistryClientConfig;
import com.alipay.sofa.registry.client.api.Subscriber;
import com.alipay.sofa.registry.client.api.SubscriberDataObserver;
import com.alipay.sofa.registry.client.api.model.UserData;
import com.alipay.sofa.registry.client.api.registration.PublisherRegistration;
import com.alipay.sofa.registry.client.api.registration.SubscriberRegistration;
import com.alipay.sofa.registry.client.constants.ConnectionType;
import com.alipay.sofa.registry.client.provider.DefaultRegistryClient;
import com.alipay.sofa.registry.client.provider.DefaultRegistryClientConfigBuilder;
import com.alipay.sofa.registry.core.model.ScopeEnum;
import java.io.IOException;

/**
 * @author chengzhengzheng
 * @date 2022/3/12
 */
public class RegistryDemo {
  public static void main(String[] args) throws IOException {
    RegistryClientConfig config =
        DefaultRegistryClientConfigBuilder.start()
            .setRegistryEndpoint("127.0.0.1")
            .setRegistryEndpointPort(9603)
            .build();
    DefaultRegistryClient registryClient = new DefaultRegistryClient(config);
    registryClient.init(ConnectionType.GRPC);

    // 构造发布者注册表
    PublisherRegistration registration =
        new PublisherRegistration("com.alipay.test.demo.service:1.0@DEFAULT");
    registration.setGroup("TEST_GROUP");
    registration.setAppName("TEST_APP");

    // 将注册表注册进客户端并发布数据
//    Publisher publisher = registryClient.register(registration, "10.10.1.1:12200?xx=yy");

    // 如需覆盖上次发布的数据可以使用发布者模型重新发布数据
//    publisher.republish("10.10.1.1:12200?xx=zz");

    // 创建 SubscriberDataObserver
    SubscriberDataObserver subscriberDataObserver =
        new SubscriberDataObserver() {
          @Override
          public void handleData(String dataId, UserData userData) {
            System.out.println("receive data success, dataId: " + dataId + ", data: " + userData);
          }
        };

    // 构造订阅者注册表，设置订阅维度，ScopeEnum 共有三种级别 zone, dataCenter, global
    String dataId = "com.alipay.test.demo.service:1.0@DEFAULT";
    SubscriberRegistration subscriberRegistration =
        new SubscriberRegistration(dataId, subscriberDataObserver);
    subscriberRegistration.setGroup("TEST_GROUP");
    subscriberRegistration.setAppName("TEST_APP");
    subscriberRegistration.setScopeEnum(ScopeEnum.global);

    // 将注册表注册进客户端并订阅数据，订阅到的数据会以回调的方式通知 SubscriberDataObserver
//    Subscriber subscriber = registryClient.register(subscriberRegistration);

    System.in.read();
  }
}
