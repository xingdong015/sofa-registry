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
package com.alipay.sofa.registry.jdbc.repository.impl;

import static com.alipay.sofa.registry.jdbc.repository.impl.MetadataMetrics.ProvideData.PROVIDE_DATA_QUERY_COUNTER;
import static com.alipay.sofa.registry.jdbc.repository.impl.MetadataMetrics.ProvideData.PROVIDE_DATA_UPDATE_COUNTER;

import com.alipay.sofa.registry.common.model.console.PersistenceData;
import com.alipay.sofa.registry.common.model.console.PersistenceDataBuilder;
import com.alipay.sofa.registry.jdbc.constant.TableEnum;
import com.alipay.sofa.registry.jdbc.convertor.ProvideDataDomainConvertor;
import com.alipay.sofa.registry.jdbc.domain.ProvideDataDomain;
import com.alipay.sofa.registry.jdbc.mapper.ProvideDataMapper;
import com.alipay.sofa.registry.log.Logger;
import com.alipay.sofa.registry.log.LoggerFactory;
import com.alipay.sofa.registry.store.api.config.DefaultCommonConfig;
import com.alipay.sofa.registry.store.api.meta.ProvideDataRepository;
import com.alipay.sofa.registry.store.api.meta.RecoverConfig;
import com.alipay.sofa.registry.store.api.meta.RecoverConfigRepository;
import com.alipay.sofa.registry.util.MathUtils;
import com.google.common.collect.Maps;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.PostConstruct;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author xiaojian.xj
 * @version $Id: ProvideDataJdbcRepository.java, v 0.1 2021年03月13日 19:20 xiaojian.xj Exp $
 */
public class ProvideDataJdbcRepository implements ProvideDataRepository, RecoverConfig {

  private static final Logger LOG = LoggerFactory.getLogger("META-PROVIDEDATA", "[ProvideData]");

  @Autowired protected ProvideDataMapper provideDataMapper;

  @Autowired protected DefaultCommonConfig defaultCommonConfig;

  @Autowired protected RecoverConfigRepository recoverConfigRepository;

  private static final Integer batchQuerySize = 1000;

  @PostConstruct
  public void init() {
    recoverConfigRepository.registerCallback(this);
  }

  @Override
  public boolean put(PersistenceData persistenceData) {
    String dataInfoId = PersistenceDataBuilder.getDataInfoId(persistenceData);
    String clusterId = defaultCommonConfig.getClusterId(tableName(), dataInfoId);

    ProvideDataDomain domain =
        ProvideDataDomainConvertor.convert2ProvideData(persistenceData, clusterId);
    return insertOrUpdate(domain);
  }

  @Override
  public boolean put(PersistenceData persistenceData, long expectVersion) {

    String dataInfoId = PersistenceDataBuilder.getDataInfoId(persistenceData);
    String clusterId = defaultCommonConfig.getClusterId(tableName(), dataInfoId);
    ProvideDataDomain exist = provideDataMapper.query(clusterId, dataInfoId);

    ProvideDataDomain domain =
        ProvideDataDomainConvertor.convert2ProvideData(persistenceData, clusterId);
    if (exist != null && exist.getDataVersion() != expectVersion) {
      LOG.error(
          "save provideData: {}, expectVersion: {}, exist: {}",
          persistenceData,
          expectVersion,
          exist);
      return false;
    }
    return insertOrUpdate(domain, exist);
  }

  protected boolean insertOrUpdate(ProvideDataDomain domain) {
    PROVIDE_DATA_QUERY_COUNTER.inc();

    ProvideDataDomain exist = provideDataMapper.query(domain.getDataCenter(), domain.getDataKey());
    return insertOrUpdate(domain, exist);
  }

  protected boolean insertOrUpdate(ProvideDataDomain domain, ProvideDataDomain exist) {
    int affect;
    try {
      if (exist == null) {
        affect = provideDataMapper.save(domain);
        if (LOG.isInfoEnabled()) {
          LOG.info("save provideData: {}, affect rows: {}", domain, affect);
        }
      } else {
        affect = provideDataMapper.update(domain, exist.getDataVersion());
        if (LOG.isInfoEnabled()) {
          LOG.info(
              "update provideData: {}, expectVersion: {}, affect rows: {}",
              domain,
              exist.getDataVersion(),
              affect);
        }
      }
      PROVIDE_DATA_UPDATE_COUNTER.inc();

      if (affect == 0) {
        PersistenceData query = get(domain.getDataKey());
        LOG.error(
            "put provideData fail, query: {}, update: {}, expectVersion: {}",
            query,
            domain,
            exist.getDataVersion());
      }

    } catch (Throwable t) {
      LOG.error("put provideData: {} error.", domain, t);
      return false;
    }

    return affect > 0;
  }

  @Override
  public PersistenceData get(String key) {
    PROVIDE_DATA_QUERY_COUNTER.inc();
    String clusterId = defaultCommonConfig.getClusterId(tableName(), key);
    return ProvideDataDomainConvertor.convert2PersistenceData(
        provideDataMapper.query(clusterId, key));
  }

  @Override
  public boolean remove(String key, long version) {
    PROVIDE_DATA_UPDATE_COUNTER.inc();
    String clusterId = defaultCommonConfig.getClusterId(tableName(), key);
    int affect = provideDataMapper.remove(clusterId, key, version);
    if (LOG.isInfoEnabled()) {
      LOG.info(
          "remove provideData, dataCenter: {}, key: {}, version: {}, affect rows: {}",
          clusterId,
          key,
          version,
          affect);
    }
    return affect > 0;
  }

  @Override
  public Map<String, PersistenceData> getAll() {

    String clusterId = defaultCommonConfig.getClusterId(tableName());
    Map<String, PersistenceData> responses = getAllByClusterId(clusterId);

    if (defaultCommonConfig.isRecoverCluster()) {
      String recoverClusterId = defaultCommonConfig.getRecoverClusterId();
      Map<String, PersistenceData> recoverConfigMap = getAllByClusterId(recoverClusterId);
      LOG.info(
          "load recover config by recoverClusterId:{}, ret:{}", recoverClusterId, recoverConfigMap);
      Set<String> dataInfoIds = recoverConfigRepository.queryKey(tableName());

      if (CollectionUtils.isNotEmpty(dataInfoIds)) {
        for (String dataInfoId : dataInfoIds) {
          // dependency config
          responses.put(dataInfoId, recoverConfigMap.get(dataInfoId));
        }
      }
    }
    PROVIDE_DATA_QUERY_COUNTER.inc();
    return responses;
  }

  private Map<String, PersistenceData> getAllByClusterId(String clusterId) {
    int total = provideDataMapper.selectTotalCount(clusterId);
    int round = MathUtils.divideCeil(total, batchQuerySize);
    Map<String, PersistenceData> responses = Maps.newHashMapWithExpectedSize(total);
    for (int i = 0; i < round; i++) {
      int start = i * batchQuerySize;
      List<ProvideDataDomain> provideDataDomains =
          provideDataMapper.queryByPage(clusterId, start, batchQuerySize);
      for (ProvideDataDomain provideDataDomain : provideDataDomains) {
        PersistenceData persistenceData =
            ProvideDataDomainConvertor.convert2PersistenceData(provideDataDomain);
        responses.put(PersistenceDataBuilder.getDataInfoId(persistenceData), persistenceData);
      }
    }
    return responses;
  }

  @Override
  public String tableName() {
    return TableEnum.PROVIDE_DATA.getTableName();
  }

  @Override
  public boolean afterConfigSet(String key, String recoverClusterId) {
    if (defaultCommonConfig.isRecoverCluster()) {
      return true;
    }
    String clusterId = defaultCommonConfig.getClusterId(tableName());
    ProvideDataDomain data = provideDataMapper.query(clusterId, key);
    ProvideDataDomain recoverData = provideDataMapper.query(recoverClusterId, key);
    if (data != null && recoverData == null) {
      // copy config
      recoverData =
          new ProvideDataDomain(
              recoverClusterId,
              data.getDataKey(),
              data.getDataValue(),
              PersistenceDataBuilder.nextVersion());
      provideDataMapper.save(recoverData);
      LOG.info("[afterConfigSet]save recover cluster:{}, data:{}", recoverClusterId, recoverData);
    }
    return true;
  }
}
