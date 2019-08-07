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
package com.alipay.sofa.registry.server.meta.test;

import com.alipay.sofa.registry.jraft.command.ProcessResponse;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author shangyu.wh
 * @version $Id: ProcessResponseTest.java, v 0.1 2018-08-14 22:20 shangyu.wh Exp $
 */
public class ProcessResponseTest {

    @Test
    public void testProcessResponse() throws InterruptedException {

        int number = 500;

        AtomicInteger count = new AtomicInteger();
        List<Runnable> list = new ArrayList();
        for (int i = 0; i < number; i++) {
            if (i % 2 == 0) {
                list.add(() -> {
                    ProcessResponse response = ProcessResponse.fail("Can not find service %s from process!").build();
                    if (response != null) {
                        if (!response.getSuccess()) {
                            count.incrementAndGet();
                        }
                    }
                });
            } else {
                list.add(() -> {
                    ProcessResponse response = ProcessResponse.ok("Can not find service %s from process!").build();
                    if (response != null) {
                        if (response.getSuccess()) {
                            count.incrementAndGet();
                        }
                    }
                });
            }

        }
        ConcurrentTestUtil.assertConcurrent("多线程build序列化", list, 10);
    }
}