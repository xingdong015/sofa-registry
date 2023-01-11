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
package com.alipay.sofa.registry.server.session.node.service;

import com.alipay.sofa.registry.common.model.store.URL;
import com.alipay.sofa.registry.remoting.CallbackHandler;
import com.alipay.sofa.registry.remoting.exchange.NodeExchanger;
import com.alipay.sofa.registry.remoting.exchange.message.Request;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author shangyu.wh
 * @version $Id: ClientNode.java, v 0.1 2017-12-12 11:56 shangyu.wh Exp $
 */
public class ClientNodeServiceImpl implements ClientNodeService {
  @Autowired private NodeExchanger clientNodeExchanger;

  @Override
  public void pushWithCallback(Object object, URL url, CallbackHandler callbackHandler) {
    pushWithCallback(object,url, URL.ProtocolType.BOLT,callbackHandler);
  }

    @Override
    public void pushWithCallback(Object object, URL url, URL.ProtocolType protocolType, CallbackHandler callbackHandler) {
        Request<Object> request =
                new Request<Object>() {
                    @Override
                    public Object getRequestBody() {
                        return object;
                    }

                    @Override
                    public URL getRequestUrl() {
                        return url;
                    }

                    @Override
                    public CallbackHandler getCallBackHandler() {
                        return callbackHandler;
                    }
                    @Override
                    public URL.ProtocolType getProtocol(){
                        return protocolType;
                    }
                };
        clientNodeExchanger.request(request);
    }
}
