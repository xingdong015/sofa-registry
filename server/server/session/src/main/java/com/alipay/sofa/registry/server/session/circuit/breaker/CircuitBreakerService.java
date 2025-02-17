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
package com.alipay.sofa.registry.server.session.circuit.breaker;

import com.alipay.sofa.registry.common.model.store.CircuitBreakerStatistic;
import com.alipay.sofa.registry.common.model.store.Subscriber;

/**
 * @author xiaojian.xj
 * @version $Id: CircuitBreakerService.java, v 0.1 2021年06月19日 13:14 xiaojian.xj Exp $
 */
public interface CircuitBreakerService {

  /**
   * @param statistic
   * @param hasPushed
   * @return
   */
  boolean pushCircuitBreaker(CircuitBreakerStatistic statistic, boolean hasPushed);

  /**
   * statistic when push success
   *
   * @param dataCenter
   * @param pushVersion
   * @param subscriber
   * @return
   */
  boolean onPushSuccess(String dataCenter, long pushVersion, int pushNum, Subscriber subscriber);

  /**
   * statistic when push fail
   *
   * @param dataCenter
   * @param pushVersion
   * @param subscriber
   * @return
   */
  boolean onPushFail(String dataCenter, long pushVersion, Subscriber subscriber);
}
