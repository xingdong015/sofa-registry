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
package com.alipay.sofa.registry.server.meta.slot.balance;

import static com.alipay.sofa.registry.server.meta.slot.balance.LeaderOnlyBalancer.TRIGGER_THESHOLD;

import com.alipay.sofa.registry.common.model.Triple;
import com.alipay.sofa.registry.common.model.Tuple;
import com.alipay.sofa.registry.common.model.slot.DataNodeSlot;
import com.alipay.sofa.registry.common.model.slot.SlotTable;
import com.alipay.sofa.registry.exception.SofaRegistryRuntimeException;
import com.alipay.sofa.registry.log.Logger;
import com.alipay.sofa.registry.log.LoggerFactory;
import com.alipay.sofa.registry.server.meta.monitor.Metrics;
import com.alipay.sofa.registry.server.meta.slot.SlotBalancer;
import com.alipay.sofa.registry.server.meta.slot.util.builder.SlotTableBuilder;
import com.alipay.sofa.registry.server.meta.slot.util.comparator.Comparators;
import com.alipay.sofa.registry.util.MathUtils;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.*;
import org.apache.commons.lang.StringUtils;

/**
 * @author chen.zhu
 *     <p>Jan 15, 2021
 */
public class DefaultSlotBalancer implements SlotBalancer {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultSlotBalancer.class);

  private final Set<String> currentDataServers;

  protected final SlotTableBuilder slotTableBuilder;
  private final BalancePolicy balancePolicy = new NaiveBalancePolicy();
  private final int slotNum;
  private final int slotReplicas;

  public DefaultSlotBalancer(
      SlotTableBuilder slotTableBuilder, Collection<String> currentDataServers) {
    this.currentDataServers = Collections.unmodifiableSet(Sets.newTreeSet(currentDataServers));
    this.slotTableBuilder = slotTableBuilder;
    this.slotNum = slotTableBuilder.getSlotNums();
    this.slotReplicas = slotTableBuilder.getSlotReplicas();
  }

  @Override
  public SlotTable balance() {
    if (currentDataServers.isEmpty()) {
      LOGGER.error("[no available data-servers] quit");
      throw new SofaRegistryRuntimeException(
          "no available data-servers for slot-table reassignment");
    }
    if (slotReplicas < TRIGGER_THESHOLD) {
      LOGGER.warn(
          "[balance] slot replica[{}] means no followers, balance leader only",
          slotTableBuilder.getSlotReplicas());
      return new LeaderOnlyBalancer(slotTableBuilder, currentDataServers).balance();
    }
    if (balanceLeaderSlots()) {
      LOGGER.info("[balanceLeaderSlots] end");
      slotTableBuilder.incrEpoch();
      return slotTableBuilder.build();
    }
    if (balanceHighFollowerSlots()) {
      LOGGER.info("[balanceHighFollowerSlots] end");
      slotTableBuilder.incrEpoch();
      return slotTableBuilder.build();
    }
    if (balanceLowFollowerSlots()) {
      LOGGER.info("[balanceLowFollowerSlots] end");
      slotTableBuilder.incrEpoch();
      return slotTableBuilder.build();
    }
    // check the low watermark leader, the follower has balanced
    // just upgrade the followers in low data server
    if (balanceLowLeaders()) {
      LOGGER.info("[balanceLowLeaders] end");
      slotTableBuilder.incrEpoch();
      return slotTableBuilder.build();
    }
    LOGGER.info("[balance] do nothing");
    return null;
  }

  private boolean balanceLeaderSlots() {
    // ceil avg, find the high water mark   找到高水位线。假如有 32 个节点、那么 leaderCeilAvg = 256 / 32 = 8
    //这里就是找到每一个节点 dataServer 作为leader的 slot 个数的最大天花板值----> 容易想到的方案肯定是平均方式、一共有 slotNum 个slot、
    // 将这些slot的leader归属平均分配给 currentDataServer
    final int leaderCeilAvg = MathUtils.divideCeil(slotNum, currentDataServers.size());
    if (upgradeHighLeaders(leaderCeilAvg)) {
      //如果有替换过 leader、那么就直接返回、不用进行 migrateHighLeaders 操作
      return true;
    }
    if (migrateHighLeaders(leaderCeilAvg)) {
      //经过上面的 upgradeHighLeaders 操作，两种情况下进行 migrateHighLeaders 操作
      //1. 所有的leader节点都很平均。
      //2. 不能找到 follow 进行迁移、因为所有的follow也都很忙、在 exclude 当中、所以没法找到一个follow进行迁移。那么我们尝试迁移 follow。

      //因为 highLeader 的所有 follower 都是比较忙、所以需要将这些忙的节点进行迁移、期待给这些 highLeader 所负责的 slot 替换一些比较清闲的 follow
      return true;
    }
    return false;
  }

  private boolean upgradeHighLeaders(int ceilAvg) {
    // smoothly, find the dataNode which owners the target slot's follower
    // and upgrade the follower to leader   "如果一个节点作为slot的leader节点的slot的个数大于指定的阈值、那么就会采用替换leader的方式、来降低这个节点的负载"  最多移动 maxMove次数
    final int maxMove = balancePolicy.getMaxMoveLeaderSlots(); //默认是 6
    //理解来说这块可以直接将 leader个数大于 ceilAvg 的 dataServer 所负责的 slot用另一些follow节点处理就可以了、为什么还要再次向上取整呢? 回答如下:
    // 主要是防止slotTable出现抖动，所以设定了触发变更的上下阈值   这里向上取整、是不是作为一个不平衡阈值来使用、就是只针对于不平衡多少(这个多少可以控制)的进行再平衡处理
    final int threshold = balancePolicy.getHighWaterMarkSlotLeaderNums(ceilAvg);
    int balanced = 0;
    Set<String> notSatisfies = Sets.newHashSet();

    while (balanced < maxMove) {
      int last = balanced;
      // 1. find the dataNode which has leaders more than high water mark
      //    and sorted by leaders.num desc
      //找到 这些节点中、那些节点作为 某一些slot的 leaders 个数 超过 threshold 9、并且按照超过的从大到小排列。
      final List<String> highDataServers = findDataServersLeaderHighWaterMark(threshold);
      if (highDataServers.isEmpty()) {
        break;
      }
      // could not found any follower to upgrade
      if (notSatisfies.containsAll(highDataServers)) {
        LOGGER.info(
            "[upgradeHighLeaders] could not found followers to upgrade for {}", highDataServers);
        break;
      }
      // 2. find the dataNode which could own a new leader
      // exclude the high
      final Set<String> excludes = Sets.newHashSet(highDataServers);
      // exclude the dataNode which could not add any leader  找到 作为Slot 的leaders 的个数大于8的
      excludes.addAll(findDataServersLeaderHighWaterMark(threshold - 1));
      for (String highDataServer : highDataServers) {
        if (notSatisfies.contains(highDataServer)) {
          continue;
        }
        //这里其实就是给那些 dataNode 什么样的dataNode 呢 (自己已经成为 大于9个 slot 的 dataNode)
        //所负责的 slot 中的某一个 重新找一个leader,但是不包含在 excludes 之中的节点

        //其实 做法是从 highDataServer 所负责的所有 slot 中找到某一个 slot、这个slot满足一个条件就是
        //该 slot 的 follow 节点中有一个最闲(也就是被作为leader的次数最少)
        //找到这个 slot、我们只需要替换该 slot 的leader为找到的follow
        //其实站在宏观的角度来说就是将 highDataServer 所负责的 所有slot的follow节点进行按照闲忙成都进行排序、
        //找到那个最闲的、然后让他当leader。这样就替换了 highDataServer 当leader了
        Tuple<String, Integer> selected = selectFollower4LeaderUpgradeOut(highDataServer, excludes);
        if (selected == null) {
          notSatisfies.add(highDataServer);
          continue;
        }
        final int slotId = selected.o2;
        final String newLeaderDataServer = selected.o1;
        slotTableBuilder.replaceLeader(slotId, newLeaderDataServer);
        LOGGER.info(
            "[upgradeHighLeaders] slotId={} leader balance from {} to {}",
            slotId,
            highDataServer,
            newLeaderDataServer);
        Metrics.SlotBalance.onLeaderUpgrade(highDataServer, newLeaderDataServer, slotId);
        balanced++;
      }
      if (last == balanced) break;
    }
    return balanced != 0;
  }

  private boolean migrateHighLeaders(int ceilAvg) {
    // could not found the follower to upgrade, migrate follower first
    final int maxMove = balancePolicy.getMaxMoveFollowerSlots();
    final int threshold = balancePolicy.getHighWaterMarkSlotLeaderNums(ceilAvg);

    int balanced = 0;
    while (balanced < maxMove) {
      int last = balanced;
      // 1. find the dataNode which has leaders more than high water mark
      //    and sorted by leaders.num desc
      final List<String> highDataServers = findDataServersLeaderHighWaterMark(threshold);
      if (highDataServers.isEmpty()) {
        return false;
      }
      // 2. find the dataNode which could own a new leader
      // exclude the high
      final Set<String> excludes = Sets.newHashSet(highDataServers);
      // exclude the dataNode which could not add any leader
      excludes.addAll(findDataServersLeaderHighWaterMark(threshold - 1));
      final Set<String> newFollowerDataServers = Sets.newHashSet();
      // only balance highDataServer once at one round, avoid the follower moves multi times
      for (String highDataServer : highDataServers) {
        Triple<String, Integer, String> selected =
            selectFollower4LeaderMigrate(highDataServer, excludes, newFollowerDataServers);
        if (selected == null) {
          LOGGER.warn(
              "[migrateHighLeaders] could not find dataServer to migrate follower for {}",
              highDataServer);
          continue;
        }
        final String oldFollower = selected.getFirst();
        final int slotId = selected.getMiddle();
        final String newFollower = selected.getLast();
        slotTableBuilder.removeFollower(slotId, oldFollower);
        slotTableBuilder.addFollower(slotId, newFollower);
        newFollowerDataServers.add(newFollower);
        LOGGER.info(
            "[migrateHighLeaders] slotId={}, follower balance from {} to {}",
            slotId,
            oldFollower,
            newFollower);
        Metrics.SlotBalance.onLeaderMigrate(oldFollower, newFollower, slotId);
        balanced++;
      }
      if (last == balanced) break;
    }
    return balanced != 0;
  }

  private boolean balanceLowLeaders() {
    final int leaderFloorAvg = Math.floorDiv(slotNum, currentDataServers.size());
    final int maxMove = balancePolicy.getMaxMoveLeaderSlots();
    final int threshold = balancePolicy.getLowWaterMarkSlotLeaderNums(leaderFloorAvg);
    int balanced = 0;
    Set<String> notSatisfies = Sets.newHashSet();

    while (balanced < maxMove) {
      int last = balanced;
      // 1. find the dataNode which has leaders less than low water mark
      //    and sorted by leaders.num asc
      final List<String> lowDataServers = findDataServersLeaderLowWaterMark(threshold);
      if (lowDataServers.isEmpty()) {
        break;
      }
      // could not found any follower to upgrade
      if (notSatisfies.containsAll(lowDataServers)) {
        LOGGER.info(
            "[upgradeLowLeaders] could not found followers to upgrade for {}", lowDataServers);
        break;
      }
      // 2. find the dataNode which could not remove a leader exclude the low
      final Set<String> excludes = Sets.newHashSet(lowDataServers);
      // exclude the dataNode which could not remove any leader
      excludes.addAll(findDataServersLeaderLowWaterMark(threshold + 1));
      for (String lowDataServer : lowDataServers) {
        if (notSatisfies.contains(lowDataServer)) {
          continue;
        }
        //实际上可以思考一下、这里肯定是给 lowDataServer 找到一个负载较高的 leader、将他所负责的 slot中的某一个的 leader权限
        //转移给这个 lowDataServer
        Tuple<String, Integer> selected = selectFollower4LeaderUpgradeIn(lowDataServer, excludes);
        if (selected == null) {
          notSatisfies.add(lowDataServer);
          continue;
        }
        final int slotId = selected.o2;
        final String oldLeaderDataServer = selected.o1;
        //将当前节点提升为 leader 节点的时候同时要把它从 follow 节点中删除 内部进行的操作
        final String replaceLeader = slotTableBuilder.replaceLeader(slotId, lowDataServer);
        if (!StringUtils.equals(oldLeaderDataServer, replaceLeader)) {
          LOGGER.error(
              "[upgradeLowLeaders] conflict leader, slotId={} leader balance from {}/{} to {}",
              slotId,
              oldLeaderDataServer,
              replaceLeader,
              lowDataServer);
          throw new SofaRegistryRuntimeException(
              String.format(
                  "upgradeLowLeaders, conflict leader=%d of %s and %s",
                  slotId, oldLeaderDataServer, replaceLeader));
        }
        Metrics.SlotBalance.onLowLeaderReplace(oldLeaderDataServer, replaceLeader, slotId);
        LOGGER.info(
            "[upgradeLowLeaders] slotId={} leader balance from {} to {}",
            slotId,
            oldLeaderDataServer,
            lowDataServer);
        balanced++;
      }
      if (last == balanced) break;
    }
    return balanced != 0;
  }

  /**
   * 此方法的目的是 给 leaderDataServer (上一步中 作为leader比较高的的 dataServer)
   * 负责的 slot、找到负责这些 slot 的 follow 节点、按照忙闲(作为 follow 的次数)给他们重新找一个新的节点来替换他们
   * @param leaderDataServer
   * @param excludes
   * @param newFollowerDataServers
   * @return
   */
  private Triple<String, Integer, String> selectFollower4LeaderMigrate(
      String leaderDataServer, Set<String> excludes, Set<String> newFollowerDataServers) {
    final DataNodeSlot dataNodeSlot = slotTableBuilder.getDataNodeSlot(leaderDataServer);
    Set<Integer> leaderSlots = dataNodeSlot.getLeaders();
    Map<String, List<Integer>> dataServersWithFollowers = Maps.newHashMap();
    for (int slot : leaderSlots) {
      List<String> followerDataServers = slotTableBuilder.getDataServersOwnsFollower(slot);
      for (String followerDataServer : followerDataServers) {
        if (newFollowerDataServers.contains(followerDataServer)) {
          // the followerDataServer contains move in follower, could not be move out candidates
          continue;
        }
        List<Integer> followerSlots =
            dataServersWithFollowers.computeIfAbsent(followerDataServer, k -> Lists.newArrayList());
        followerSlots.add(slot);
      }
    }
    LOGGER.info(
        "[selectFollower4LeaderMigrate] {} owns leader slots={}, slotIds={}, migrate candidates {}, newFollowers={}",
        leaderDataServer,
        leaderSlots.size(),
        leaderSlots,
        dataServersWithFollowers,
        newFollowerDataServers);
    // sort the dataServer by follower.num desc
    List<String> migrateDataServers = Lists.newArrayList(dataServersWithFollowers.keySet());
    migrateDataServers.sort(Comparators.mostFollowersFirst(slotTableBuilder));
    //这些 follow 节点按照忙闲排序、依次给重新分配
    for (String migrateDataServer : migrateDataServers) {
      final List<Integer> selectedFollowers = dataServersWithFollowers.get(migrateDataServer);
      for (Integer selectedFollower : selectedFollowers) {
        // chose the dataServer which own least leaders

        //选择一个最闲(作为 leader 的次数最少的)的节点进行替换
        List<String> candidates =
            getCandidateDataServers(
                excludes, Comparators.leastLeadersFirst(slotTableBuilder), currentDataServers);
        for (String candidate : candidates) {
          if (candidate.equals(migrateDataServer)) {
            // the same, skip
            continue;
          }
          DataNodeSlot candidateDataNodeSlot = slotTableBuilder.getDataNodeSlot(candidate);
          if (candidateDataNodeSlot.containsFollower(selectedFollower)) {
            LOGGER.error(
                "[selectFollower4LeaderMigrate] slotId={}, follower conflict with migrate from {} to {}",
                selectedFollower,
                migrateDataServer,
                candidateDataNodeSlot);
            continue;
          }
          return Triple.from(migrateDataServer, selectedFollower, candidate);
        }
      }
    }
    return null;
  }

  private Tuple<String, Integer> selectFollower4LeaderUpgradeOut(
      String leaderDataServer, Set<String> excludes) {
    //excludes 包含那些 dataNode 作为leader的个数大于8的

    //找出当前 leaderDataServer 所负责 DataNodeSlot 也就是 slot信息
    final DataNodeSlot dataNodeSlot = slotTableBuilder.getDataNodeSlot(leaderDataServer);

    Set<Integer> leaderSlots = dataNodeSlot.getLeaders();
    Map<String, List<Integer>> dataServers2Followers = Maps.newHashMap();
    //给 当前dataNode 所负责的 leader slot 重新选择一个leader
    for (int slot : leaderSlots) {
      //找出当前 slot 的follow
      List<String> followerDataServers = slotTableBuilder.getDataServersOwnsFollower(slot);
      //扣除excludes 之后找到一个满足的follow
      followerDataServers = getCandidateDataServers(excludes, null, followerDataServers);
      for (String followerDataServer : followerDataServers) {
        List<Integer> followerSlots =
            dataServers2Followers.computeIfAbsent(followerDataServer, k -> Lists.newArrayList());
        followerSlots.add(slot);
      }
    }
    if (dataServers2Followers.isEmpty()) {
      //当 leaderDataServer 锁负责的slot的follow都是 excludes 中的成员时候、那么就有可能是空的。
      LOGGER.info(
          "[LeaderUpgradeOut] {} owns leader slots={}, no dataServers could be upgrade, slotId={}",
          leaderDataServer,
          leaderSlots.size(),
          leaderSlots);
      return null;
    } else {
      LOGGER.info(
          "[LeaderUpgradeOut] {} owns leader slots={}, slotIds={}, upgrade candidates {}",
          leaderDataServer,
          leaderSlots.size(),
          leaderSlots,
          dataServers2Followers);
    }
    // sort the dataServer by leaders.num asc
    List<String> dataServers = Lists.newArrayList(dataServers2Followers.keySet());
    //按照 leader的个数升序排序、也就是也就是找到那个最不忙的
    dataServers.sort(Comparators.leastLeadersFirst(slotTableBuilder));
    final String selectedDataServer = dataServers.get(0);
    List<Integer> followers = dataServers2Followers.get(selectedDataServer);
    return Tuple.of(selectedDataServer, followers.get(0));
  }

  /**
   * 这里是由于 followerDataServer 负责的 leader 过少、所以需要将部分slot的leader提升为它、
   * 注意这里提升为leader的时候、要提升他作为 follow的slot、这样不用复制数据。
   * @param followerDataServer
   * @param excludes
   * @return
   */
  private Tuple<String, Integer> selectFollower4LeaderUpgradeIn(
      String followerDataServer, Set<String> excludes) {
    final DataNodeSlot dataNodeSlot = slotTableBuilder.getDataNodeSlot(followerDataServer);
    Set<Integer> followerSlots = dataNodeSlot.getFollowers();
    LOGGER.info(
        "[LeaderUpgradeIn] {} find follower to upgrade, {}/{}",
        followerDataServer,
        followerSlots.size(),
        followerSlots);
    Map<String, List<Integer>> dataServers2Leaders = Maps.newHashMap();
    for (int slot : followerSlots) {
      final String leaderDataServer = slotTableBuilder.getDataServersOwnsLeader(slot);
      if (StringUtils.isBlank(leaderDataServer)) {
        // no leader, should not happen
        LOGGER.error("[LeaderUpgradeIn] no leader for slotId={} in {}", slot, followerDataServer);
        continue;
      }
      if (excludes.contains(leaderDataServer)) {
        final DataNodeSlot leaderDataNodeSlot = slotTableBuilder.getDataNodeSlot(leaderDataServer);
        LOGGER.info(
            "[LeaderUpgradeIn] {} not owns enough leader to downgrade slotId={} for {}, leaderSize={}",
            leaderDataServer,
            slot,
            followerDataServer,
            leaderDataNodeSlot.getLeaders().size());
        continue;
      }

      List<Integer> leaders =
          dataServers2Leaders.computeIfAbsent(leaderDataServer, k -> Lists.newArrayList());
      leaders.add(slot);
    }
    if (dataServers2Leaders.isEmpty()) {
      LOGGER.info(
          "[LeaderUpgradeIn] {} owns followerSize={}, no dataServers could be downgrade, slotId={}",
          followerDataServer,
          followerSlots.size(),
          followerSlots);
      return null;
    } else {
      LOGGER.info(
          "[LeaderUpgradeIn] {} owns followerSize={}, slotIds={}, downgrade candidates {}",
          followerDataServer,
          followerSlots.size(),
          followerSlots,
          dataServers2Leaders);
    }
    // sort the dataServer by leaders.num asc
    List<String> dataServers = Lists.newArrayList(dataServers2Leaders.keySet());
    dataServers.sort(Comparators.mostLeadersFirst(slotTableBuilder));
    final String selectedDataServer = dataServers.get(0);
    List<Integer> leaders = dataServers2Leaders.get(selectedDataServer);
    return Tuple.of(selectedDataServer, leaders.get(0));
  }

  private List<String> findDataServersLeaderHighWaterMark(int threshold) {
    List<DataNodeSlot> dataNodeSlots = slotTableBuilder.getDataNodeSlotsLeaderBeyond(threshold);
    List<String> dataServers = DataNodeSlot.collectDataNodes(dataNodeSlots);
    dataServers.sort(Comparators.mostLeadersFirst(slotTableBuilder));
    LOGGER.info(
        "[LeaderHighWaterMark] threshold={}, dataServers={}/{}",
        threshold,
        dataServers.size(),
        dataServers);
    return dataServers;
  }

  private List<String> findDataServersLeaderLowWaterMark(int threshold) {
    List<DataNodeSlot> dataNodeSlots = slotTableBuilder.getDataNodeSlotsLeaderBelow(threshold);
    List<String> dataServers = DataNodeSlot.collectDataNodes(dataNodeSlots);
    dataServers.sort(Comparators.leastLeadersFirst(slotTableBuilder));
    LOGGER.info(
        "[LeaderLowWaterMark] threshold={}, dataServers={}/{}",
        threshold,
        dataServers.size(),
        dataServers);
    return dataServers;
  }

  private List<String> findDataServersFollowerHighWaterMark(int threshold) {
    List<DataNodeSlot> dataNodeSlots = slotTableBuilder.getDataNodeSlotsFollowerBeyond(threshold);
    List<String> dataServers = DataNodeSlot.collectDataNodes(dataNodeSlots);
    dataServers.sort(Comparators.mostFollowersFirst(slotTableBuilder));
    LOGGER.info(
        "[FollowerHighWaterMark] threshold={}, dataServers={}/{}",
        threshold,
        dataServers.size(),
        dataServers);
    return dataServers;
  }

  private List<String> findDataServersFollowerLowWaterMark(int threshold) {
    List<DataNodeSlot> dataNodeSlots = slotTableBuilder.getDataNodeSlotsFollowerBelow(threshold);
    List<String> dataServers = DataNodeSlot.collectDataNodes(dataNodeSlots);
    dataServers.sort(Comparators.leastFollowersFirst(slotTableBuilder));
    LOGGER.info(
        "[FollowerLowWaterMark] threshold={}, dataServers={}/{}",
        threshold,
        dataServers.size(),
        dataServers);
    return dataServers;
  }

  private boolean balanceHighFollowerSlots() {
    final int followerCeilAvg =
        MathUtils.divideCeil(slotNum * (slotReplicas - 1), currentDataServers.size());
    final int maxMove = balancePolicy.getMaxMoveFollowerSlots();
    final int threshold = balancePolicy.getHighWaterMarkSlotFollowerNums(followerCeilAvg);
    int balanced = 0, prevBalanced = -1;
    while (balanced < maxMove) {
      final List<String> highDataServers = findDataServersFollowerHighWaterMark(threshold);
      if (highDataServers.isEmpty()) {
        break;
      }

      Set<String> excludes = Sets.newHashSet(highDataServers);
      excludes.addAll(findDataServersFollowerHighWaterMark(threshold - 1));

      prevBalanced = balanced;
      for (String highDataServer : highDataServers) {
        Tuple<String, Integer> selected = selectFollower4BalanceOut(highDataServer, excludes);
        if (selected == null) {
          LOGGER.warn(
              "[balanceHighFollowerSlots] could not find follower slot to balance: {}",
              highDataServer);
          continue;
        }
        final int followerSlot = selected.o2;
        final String newFollowerDataServer = selected.o1;
        slotTableBuilder.removeFollower(followerSlot, highDataServer);
        slotTableBuilder.addFollower(followerSlot, newFollowerDataServer);
        LOGGER.info(
            "[balanceHighFollowerSlots] balance follower slotId={} from {} to {}",
            followerSlot,
            highDataServer,
            newFollowerDataServer);
        Metrics.SlotBalance.onHighFollowerMigrate(
            highDataServer, newFollowerDataServer, followerSlot);
        balanced++;
        break;
      }
      /**
       * avoid for infinity loop once the "prev-balanced == balanced", it means that we can't find
       * any suitable candidate for migrate stop before we case an infinity loop
       */
      if (prevBalanced == balanced) {
        LOGGER.warn("[balanceHighFollowerSlots][prevBlanced == balanced]no more balance available");
        break;
      }
    }
    return balanced != 0;
  }

  private boolean balanceLowFollowerSlots() {
    final int followerFloorAvg =
        Math.floorDiv(slotNum * (slotReplicas - 1), currentDataServers.size());
    final int maxMove = balancePolicy.getMaxMoveFollowerSlots();
    final int threshold = balancePolicy.getLowWaterMarkSlotFollowerNums(followerFloorAvg);
    int balanced = 0, prevBalanced = -1;
    while (balanced < maxMove) {
      final List<String> lowDataServers = findDataServersFollowerLowWaterMark(threshold);
      if (lowDataServers.isEmpty()) {
        break;
      }

      Set<String> excludes = Sets.newHashSet(lowDataServers);
      excludes.addAll(findDataServersFollowerLowWaterMark(threshold + 1));

      prevBalanced = balanced;
      for (String lowDataServer : lowDataServers) {
        Tuple<String, Integer> selected = selectFollower4BalanceIn(lowDataServer, excludes);
        if (selected == null) {
          LOGGER.warn(
              "[balanceLowFollowerSlots] could not find follower slot to balance: {}",
              lowDataServer);
          continue;
        }
        final int followerSlot = selected.o2;
        final String oldFollowerDataServer = selected.o1;
        slotTableBuilder.removeFollower(followerSlot, oldFollowerDataServer);
        slotTableBuilder.addFollower(followerSlot, lowDataServer);
        LOGGER.info(
            "[balanceLowFollowerSlots] balance follower slotId={} from {} to {}",
            followerSlot,
            oldFollowerDataServer,
            lowDataServer);
        Metrics.SlotBalance.onLowFollowerMigrate(
            oldFollowerDataServer, lowDataServer, followerSlot);
        balanced++;
        break;
      }
      if (prevBalanced == balanced) {
        LOGGER.warn("[balanceLowFollowerSlots][prevBlanced == balanced]no more balance available");
        break;
      }
    }
    return balanced != 0;
  }

  /**
   * 此方法的目的是将 followDataServer 比较闲的节点、给他找到一个相对来说比较忙的节点、将这个忙的节点负责的、
   * 一部分slot分配给 这个 followDataServer
   * @param followerDataServer
   * @param excludes
   * @return
   */
  private Tuple<String, Integer> selectFollower4BalanceIn(
      String followerDataServer, Set<String> excludes) {
    final DataNodeSlot dataNodeSlot = slotTableBuilder.getDataNodeSlot(followerDataServer);
    List<String> candidates =
        getCandidateDataServers(
            excludes, Comparators.mostFollowersFirst(slotTableBuilder), currentDataServers);
    LOGGER.info(
        "[selectFollower4BalanceIn] target={}, followerSize={}, candidates={}",
        followerDataServer,
        dataNodeSlot.getFollowers().size(),
        candidates);
    for (String candidate : candidates) {
      DataNodeSlot candidateDataNodeSlot = slotTableBuilder.getDataNodeSlot(candidate);
      Set<Integer> candidateFollowerSlots = candidateDataNodeSlot.getFollowers();
      for (int candidateFollowerSlot : candidateFollowerSlots) {
        if (dataNodeSlot.containsFollower(candidateFollowerSlot)) {
          LOGGER.info(
              "[selectFollower4BalanceIn] skip, target {} contains follower {}, candidate={}",
              followerDataServer,
              candidateFollowerSlot,
              candidate);
          continue;
        }
        if (dataNodeSlot.containsLeader(candidateFollowerSlot)) {
          LOGGER.info(
              "[selectFollower4BalanceIn] skip, target {} contains leader {}, candidate={}",
              followerDataServer,
              candidateFollowerSlot,
              candidate);
          continue;
        }
        return Tuple.of(candidate, candidateFollowerSlot);
      }
    }
    return null;
  }

  private Tuple<String, Integer> selectFollower4BalanceOut(
      String followerDataServer, Set<String> excludes) {
    final DataNodeSlot dataNodeSlot = slotTableBuilder.getDataNodeSlot(followerDataServer);
    Set<Integer> followerSlots = dataNodeSlot.getFollowers();
    List<String> candidates =
        getCandidateDataServers(
            excludes, Comparators.leastFollowersFirst(slotTableBuilder), currentDataServers);
    LOGGER.info(
        "[selectFollower4BalanceOut] target={}, followerSize={}, candidates={}",
        followerDataServer,
        followerSlots.size(),
        candidates);
    for (Integer followerSlot : followerSlots) {
      for (String candidate : candidates) {
        DataNodeSlot candidateDataNodeSlot = slotTableBuilder.getDataNodeSlot(candidate);
        if (candidateDataNodeSlot.containsLeader(followerSlot)) {
          LOGGER.info(
              "[selectFollower4BalanceOut] skip, conflict leader, target={}, follower={}, candidate={}",
              followerDataServer,
              followerSlot,
              candidate);
          continue;
        }
        if (candidateDataNodeSlot.containsFollower(followerSlot)) {
          LOGGER.info(
              "[selectFollower4BalanceOut] skip, conflict follower, target={}, follower={}, candidate={}",
              followerDataServer,
              followerSlot,
              candidate);
          continue;
        }
        return Tuple.of(candidate, followerSlot);
      }
    }
    return null;
  }

  public SlotTableBuilder getSlotTableBuilder() {
    return slotTableBuilder;
  }

  private List<String> getCandidateDataServers(
      Collection<String> excludes,
      Comparator<String> comp,
      Collection<String> candidateDataServers) {
    Set<String> candidates = Sets.newHashSet(candidateDataServers);
    candidates.removeAll(excludes);
    List<String> ret = Lists.newArrayList(candidates);
    if (comp != null) {
      ret.sort(comp);
    }
    return ret;
  }
}
