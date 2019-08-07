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
package com.alipay.sofa.registry.server.meta.repository.service;

import com.alipay.sofa.registry.common.model.metaserver.DataOperator;
import com.alipay.sofa.registry.common.model.metaserver.SessionNode;
import com.alipay.sofa.registry.jraft.processor.AbstractSnapshotProcess;
import com.alipay.sofa.registry.jraft.processor.SnapshotProcess;
import com.alipay.sofa.registry.log.Logger;
import com.alipay.sofa.registry.log.LoggerFactory;
import com.alipay.sofa.registry.server.meta.node.NodeOperator;
import com.alipay.sofa.registry.server.meta.repository.NodeConfirmStatusService;
import com.alipay.sofa.registry.store.api.annotation.RaftService;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author shangyu.wh
 * @version $Id: DataConfirmStatusService.java, v 0.1 2018-05-15 17:53 shangyu.wh Exp $
 */
@RaftService(uniqueId = "sessionServer")
public class SessionConfirmStatusService extends AbstractSnapshotProcess
                                                                        implements
                                                                        NodeConfirmStatusService<SessionNode> {

    private static final Logger                                                             LOGGER                  = LoggerFactory
                                                                                                                        .getLogger(SessionConfirmStatusService.class);

    /**
     * store new data node status,The status can be removed after confirm by all registered other node
     */
    private ConcurrentHashMap<SessionNode/*node*/, Map<String/*ipAddress*/, SessionNode>> expectNodes             = new ConcurrentHashMap<>();
    private BlockingQueue<NodeOperator>                                                     expectNodesOrders       = new LinkedBlockingQueue();

    private Set<String>                                                                     snapShotFileNames       = new HashSet<>();

    private static final String                                                             NODE_EXTEND_NAME        = "expectNodes";

    private static final String                                                             NODE_ORDERS_EXTEND_NAME = "expectNodesOrders";

    /**
     * constructor
     */
    public SessionConfirmStatusService() {
    }

    /**
     * constructor
     * @param expectNodes
     * @param expectNodesOrders
     */
    public SessionConfirmStatusService(ConcurrentHashMap<SessionNode, Map<String, SessionNode>> expectNodes,
                                       BlockingQueue<NodeOperator> expectNodesOrders) {
        this.expectNodes = expectNodes;
        this.expectNodesOrders = expectNodesOrders;
    }

    @Override
    public SnapshotProcess copy() {
        return new SessionConfirmStatusService(new ConcurrentHashMap<>(expectNodes),
            new LinkedBlockingQueue<>(expectNodesOrders));
    }

    @Override
    public void putConfirmNode(SessionNode node, DataOperator nodeOperate) {
        try {
            expectNodesOrders.put(new NodeOperator(node, nodeOperate));
            LOGGER.info("Put operate:{} node:{} expect be confirm.", nodeOperate, node);
        } catch (InterruptedException e) {
            LOGGER.error("Put expect status list interrupt!", e);
        }
    }

    @Override
    public NodeOperator<SessionNode> peekConfirmNode() {
        return expectNodesOrders.peek();
    }

    @Override
    public NodeOperator<SessionNode> pollConfirmNode() throws InterruptedException {
        return expectNodesOrders.poll(1, TimeUnit.SECONDS);
    }

    @Override
    public Queue<NodeOperator> getAllConfirmNodes() {
        return expectNodesOrders;
    }

    @Override
    public Map<String, SessionNode> putExpectNodes(SessionNode confirmNode,
                                                   Map<String, SessionNode> addNodes) {
        expectNodes.put(confirmNode, addNodes);
        LOGGER.info("Put ExpectNodes:{} node:{} expect be confirm.expectNodes all {}", addNodes,
            confirmNode, expectNodes);
        return addNodes;
    }

    @Override
    public Map<String, SessionNode> getExpectNodes(SessionNode confirmNode) {
        Map<String, SessionNode> map = expectNodes.get(confirmNode);
        LOGGER.info("Get ExpectNodes:{} node:{} expect be confirm. expectNodes all {}", map,
            confirmNode, expectNodes);
        return map;
    }

    @Override
    public Map<String, SessionNode> removeExpectNodes(SessionNode confirmNode) {
        return expectNodes.remove(confirmNode);
    }

    @Override
    public Map<String, SessionNode> removeExpectConfirmNodes(SessionNode confirmNode, Collection<String> ips) {

        Map<String, SessionNode> map = expectNodes.get(confirmNode);
        if (map != null) {
            if (ips != null && !ips.isEmpty()) {
                ips.forEach(ipAddress -> {
                    SessionNode old = map.remove(ipAddress);
                    if (old == null) {
                        LOGGER.warn("Get Expect confirmNode ip {} not existed!", ipAddress);
                    }
                });
            }
        } else {
            LOGGER.warn("Get Expect confirmNode {} not existed!", confirmNode);
        }
        LOGGER.info("Remove expect confirmNode:{} ips:{}. return all {}", confirmNode, ips, map);
        return map;
    }

    @Override
    public boolean save(String path) {
        if (path == null) {
            throw new IllegalArgumentException("Input path can't be null!");
        }

        if (path.endsWith(NODE_ORDERS_EXTEND_NAME)) {
            return save(path, expectNodesOrders);
        } else {
            return save(path, expectNodes);
        }

    }

    @Override
    public synchronized boolean load(String path) {
        try {
            if (path == null) {
                throw new IllegalArgumentException("Input path can't be null!");
            }

            if (path.endsWith(NODE_ORDERS_EXTEND_NAME)) {
                BlockingQueue<NodeOperator> queue = load(path, expectNodesOrders.getClass());
                expectNodesOrders.clear();
                expectNodesOrders.addAll(queue);

            } else {
                ConcurrentHashMap<SessionNode, Map<String, SessionNode>> map = load(path,
                    expectNodes.getClass());
                expectNodes.clear();
                expectNodes.putAll(map);
            }
            return true;
        } catch (IOException e) {
            LOGGER.error("Load confirm expect Nodes data error!", e);
            return false;
        }
    }

    @Override
    public Set<String> getSnapshotFileNames() {
        if (!snapShotFileNames.isEmpty()) {
            return snapShotFileNames;
        }
        snapShotFileNames.add(getExtPath(this.getClass().getSimpleName(), NODE_EXTEND_NAME));
        snapShotFileNames.add(getExtPath(this.getClass().getSimpleName(), NODE_ORDERS_EXTEND_NAME));
        return snapShotFileNames;
    }

    private String getExtPath(String path, String extentName) {
        return path + "_" + extentName;
    }
}