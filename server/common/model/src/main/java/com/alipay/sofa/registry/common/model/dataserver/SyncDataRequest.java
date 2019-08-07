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
package com.alipay.sofa.registry.common.model.dataserver;

import java.io.Serializable;

/**
 * request to sync data from dataserver in other dataCenter
 *
 * @author qian.lqlq
 * @version $Id: SyncDataRequest.java, v 0.1 2018-03-07 15:22 qian.lqlq Exp $
 */
public class SyncDataRequest implements Serializable {

    private static final long serialVersionUID = 7229539333106031495L;

    private String            dataInfoId;

    private String            dataCenter;

    private String            dataSourceType;

    /**
     * be null when dataInfoId not exist in local datumCache
     */
    private Long              version;

    /**
     * construtor
     * @param dataInfoId
     * @param dataCenter
     * @param version
     * @param dataSourceType
     */
    public SyncDataRequest(String dataInfoId, String dataCenter, Long version, String dataSourceType) {
        this.dataInfoId = dataInfoId;
        this.dataCenter = dataCenter;
        this.version = version;
        this.dataSourceType = dataSourceType;
    }

    /**
     * Getter method for property <tt>dataInfoId</tt>.
     *
     * @return property value of dataInfoId
     */
    public String getDataInfoId() {
        return dataInfoId;
    }

    /**
     * Setter method for property <tt>dataInfoId</tt>.
     *
     * @param dataInfoId  value to be assigned to property dataInfoId
     */
    public void setDataInfoId(String dataInfoId) {
        this.dataInfoId = dataInfoId;
    }

    /**
     * Getter method for property <tt>dataCenter</tt>.
     *
     * @return property value of dataCenter
     */
    public String getDataCenter() {
        return dataCenter;
    }

    /**
     * Setter method for property <tt>dataCenter</tt>.
     *
     * @param dataCenter  value to be assigned to property dataCenter
     */
    public void setDataCenter(String dataCenter) {
        this.dataCenter = dataCenter;
    }

    /**
     * Getter method for property <tt>version</tt>.
     *
     * @return property value of version
     */
    public Long getVersion() {
        return version;
    }

    /**
     * Setter method for property <tt>version</tt>.
     *
     * @param version  value to be assigned to property version
     */
    public void setVersion(Long version) {
        this.version = version;
    }

    /**
     * Getter method for property <tt>dataSourceType</tt>.
     *
     * @return property value of dataSourceType
     */
    public String getDataSourceType() {
        return dataSourceType;
    }

    /**
     * Setter method for property <tt>dataSourceType</tt>.
     *
     * @param dataSourceType  value to be assigned to property dataSourceType
     */
    public void setDataSourceType(String dataSourceType) {
        this.dataSourceType = dataSourceType;
    }

    @Override
    public String toString() {
        return new StringBuilder("[SyncDataRequest] dataInfoId=").append(dataInfoId)
            .append(", dataCenter=").append(dataCenter).append(", version=").append(version)
            .toString();
    }
}