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
package com.alipay.sofa.registry.client.grpc;

import com.alipay.sofa.registry.client.api.Configurator;
import com.alipay.sofa.registry.client.api.Publisher;
import com.alipay.sofa.registry.client.api.RegistryClientConfig;
import com.alipay.sofa.registry.client.api.Subscriber;
import com.alipay.sofa.registry.client.constants.RpcClientStatus;
import com.alipay.sofa.registry.client.log.LoggerFactory;
import com.alipay.sofa.registry.client.provider.RegisterCache;
import com.alipay.sofa.registry.client.remoting.Client;
import com.alipay.sofa.registry.client.remoting.ServerManager;
import com.alipay.sofa.registry.client.remoting.ServerNode;
import com.alipay.sofa.registry.client.task.TaskEvent;
import com.alipay.sofa.registry.client.task.Worker;
import com.alipay.sofa.registry.client.task.WorkerThread;
import com.alipay.sofa.registry.core.grpc.auto.BiRequestStreamGrpc;
import com.alipay.sofa.registry.core.grpc.auto.Payload;
import com.alipay.sofa.registry.core.grpc.auto.RequestGrpc;
import com.alipay.sofa.registry.core.grpc.request.ConnectionSetupRequest;
import com.alipay.sofa.registry.core.grpc.request.ServerCheckRequest;
import com.alipay.sofa.registry.core.grpc.response.ServerCheckResponse;
import com.alipay.sofa.registry.core.utils.GrpcUtils;
import io.grpc.*;
import io.grpc.stub.StreamObserver;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;

/**
 * @author chengzhengzheng
 * @date 2022/11/21
 */
public class GrpcClient implements Client {

  private static final Logger LOGGER = LoggerFactory.getLogger(GrpcClient.class);

  private static final int RECONNECTING_DELAY = 5000;

  private static final long DEFAULT_KEEP_ALIVE_TIME = 6 * 60 * 1000;

  private ServerManager serverManager;
  private RegistryClientConfig config;
  private RegisterCache registerCache;
  protected volatile GrpcConnection currentConnection;
  private Worker worker;
  private static final int RETRY_TIMES = 3;
  protected volatile AtomicReference<RpcClientStatus> rpcClientStatus =
      new AtomicReference<>(RpcClientStatus.WAIT_INIT);

  /**
   * Instantiates a new Client connection.
   *
   * @param serverManager the server manager
   * @param config the config
   */
  public GrpcClient(
      ServerManager serverManager, RegisterCache registerCache, RegistryClientConfig config) {
    this.serverManager = serverManager;
    this.registerCache = registerCache;
    this.config = config;
  }

  @Override
  public void init() {
    // 将Client状态由INITIALIZED变更为STARTING
    boolean success =
        rpcClientStatus.compareAndSet(RpcClientStatus.WAIT_INIT, RpcClientStatus.STARTING);
    if (!success) {
      return;
    }

    GrpcConnection connectToServer = null;
    rpcClientStatus.set(RpcClientStatus.STARTING);
    int startUpRetryTimes = RETRY_TIMES;
    while (startUpRetryTimes > 0 && connectToServer == null) {
      try {
        startUpRetryTimes--;
        //todo 从meta节点获取grpc服务节点。 心跳，session节点上报心跳的时候、需要上报协议、以及提供服务的端口信息。
        List<ServerNode> serverNodes = new ArrayList<>(serverManager.getServerList());
        if (CollectionUtils.isEmpty(serverNodes)) {
          break;
        }
        // shuffle server list to make server connections as discrete as possible
        Collections.shuffle(serverNodes);

        for (ServerNode serverNode : serverNodes) {
          connectToServer = connectToServer(serverNode);
          if (null != connectToServer && connectToServer.isFine()) {
            break;
          }
        }
      } catch (Throwable e) {
        LOGGER.warn(
            "[GrpcConnect] Failed trying connect to, error message = {}, init retry times left: {}",
            e.getMessage(),
            startUpRetryTimes,
            e);
      }
    }

    if (connectToServer != null) {
      this.currentConnection = connectToServer;
      rpcClientStatus.set(RpcClientStatus.RUNNING);
    }
  }

  private ServerCheckResponse serverCheck(
      String ip, int port, RequestGrpc.RequestBlockingStub requestBlockingStub) {
    try {
      if (requestBlockingStub == null) {
        return null;
      }
      ServerCheckRequest serverCheckRequest = new ServerCheckRequest();
      Payload            grpcRequest        = GrpcUtils.convert(serverCheckRequest);
      Payload            response           = requestBlockingStub.request(grpcRequest);
      return GrpcUtils.parse(response, ServerCheckResponse.class);
    } catch (Exception e) {
      LOGGER.error(
          "Server check fail, please check server {} ,port {} is available , error ={}",
          ip,
          port,
          e);
      return null;
    }
  }

  /**
   * Create a stub using a channel.
   *
   * @param managedChannelTemp channel.
   * @return if server check success,return a non-null stub.
   */
  private RequestGrpc.RequestFutureStub createNewChannelStub(ManagedChannel managedChannelTemp) {
    return RequestGrpc.newFutureStub(managedChannelTemp);
  }

  /**
   * create a new channel with specific server address.
   *
   * @param serverIp serverIp.
   * @param serverPort serverPort.
   * @return if server check success,return a non-null channel.
   */
  private ManagedChannel createNewManagedChannel(String serverIp, int serverPort) {
    ManagedChannelBuilder<?> managedChannelBuilder =
        ManagedChannelBuilder.forAddress(serverIp, serverPort)
            .compressorRegistry(CompressorRegistry.getDefaultInstance())
            .keepAliveTime(keepAliveTimeMillis(), TimeUnit.MILLISECONDS)
            .decompressorRegistry(DecompressorRegistry.getDefaultInstance())
            // todo https://github.com/grpc/proposal/blob/master/A8-client-side-keepalive.md
            // keepAliveTimeout(keepAliveTim).
                //todo 连接是否池话 https://stackoverflow.com/questions/68229848/grpc-java-multiple-channel-management
            .usePlaintext();
    return managedChannelBuilder.build();
  }

  private long keepAliveTimeMillis() {
    String keepAliveTimeMillis =
        System.getProperty(
            "sofa.registry.client.grpc.keep.alive.millis", String.valueOf(DEFAULT_KEEP_ALIVE_TIME));
    return Integer.parseInt(keepAliveTimeMillis);
  }

  /**
   * 连接 connect
   *
   * @return
   */
  public boolean connect() {
    Random random = new Random();
    GrpcConnection connection = null;
    List<ServerNode> serverNodes = new ArrayList<>(serverManager.getServerList());
    // shuffle server list to make server connections as discrete as possible
    //todo lb 策略。 https://github.com/grpc/grpc/blob/master/doc/load-balancing.md 参考 grpc的lb策略
    // nacos 的策略很简单
    // 重新连接的时候 指数退避策略 https://github.com/grpc/grpc/blob/master/doc/connection-backoff.md
    Collections.shuffle(serverNodes);
    for (ServerNode serverNode : serverNodes) {
      try {
        connection = connectToServer(serverNode);
        if (null != connection && connection.isFine()) {
          resetRegister();
          LOGGER.info("[Grpc Connect] Successfully connected to server: {}", serverNode);
          break;
        } else if (connection != null) {
          // recycle connection
          connection.close();
        }
        Thread.sleep(random.nextInt(RECONNECTING_DELAY));
      } catch (Exception e) {
        LOGGER.error("[Grpc Connect] Failed trying connect to {}", serverNode, e);
      }
    }
    if (null != connection && connection.isFine()) {
      currentConnection = connection;
      return true;
    }
    return false;
  }

  private void resetRegister() {
    try {
      List<TaskEvent> eventList = new ArrayList<>();

      Collection<Publisher> publishers = registerCache.getAllPublishers();
      for (Publisher publisher : publishers) {
        try {
          publisher.reset();
          eventList.add(new TaskEvent(publisher));
        } catch (Exception e) {
          LOGGER.error("[connection] Publisher reset error, {}", publisher, e);
        }
      }

      Collection<Subscriber> subscribers = registerCache.getAllSubscribers();
      for (Subscriber subscriber : subscribers) {
        try {
          subscriber.reset();
          eventList.add(new TaskEvent(subscriber));
        } catch (Exception e) {
          LOGGER.error("[connection] Subscriber reset error, {}", subscriber, e);
        }
      }

      Collection<Configurator> configurators = registerCache.getAllConfigurator();
      for (Configurator configurator : configurators) {
        try {
          configurator.reset();
          eventList.add(new TaskEvent(configurator));
        } catch (Exception e) {
          LOGGER.error("[connection] Configurator reset error, {}", configurator, e);
        }
      }

      worker.schedule(eventList);
      LOGGER.info(
          "[reset] {} publishers and {} subscribers has been reset",
          publishers.size(),
          subscribers.size());
    } catch (Exception e) {
      LOGGER.error("[reset] Reset register after reconnect error", e);
    }
  }

  private GrpcConnection connectToServer(ServerNode serverNode) {
    String host = serverNode.getHost();
    int port = serverNode.getPort();
    try {
      ManagedChannel managedChannel = createNewManagedChannel(host, port);
      // 是否需要初始化 managedChannel 连接服务端
      // https://stackoverflow.com/questions/43284217/getting-connection-state-for-grpc
      // 参考 nacos 的健康检测
      RequestGrpc.RequestFutureStub newChannelStubTemp = createNewChannelStub(managedChannel);
      RequestGrpc.RequestBlockingStub requestBlockingStub =
          RequestGrpc.newBlockingStub(managedChannel);

      if (newChannelStubTemp != null) {
        ServerCheckResponse response = serverCheck(host, port, requestBlockingStub);

        if (response == null) {
          shuntDownChannel(managedChannel);
          return null;
        }

        BiRequestStreamGrpc.BiRequestStreamStub biRequestStreamStub =
            BiRequestStreamGrpc.newStub(newChannelStubTemp.getChannel());
        GrpcConnection grpcConn = new GrpcConnection(serverNode);
        grpcConn.setConnectionId(response.getConnectionId());

        // create stream request and bind connection event to this connection.
        StreamObserver<Payload> payloadStreamObserver =
            bindRequestStream(biRequestStreamStub, grpcConn);

        // stream observer to send response to server
        grpcConn.setPayloadStreamObserver(payloadStreamObserver);
        grpcConn.setGrpcFutureServiceStub(newChannelStubTemp);
        grpcConn.setChannel(managedChannel);
        // send a  setup request.
        ConnectionSetupRequest conSetupRequest = new ConnectionSetupRequest();
        grpcConn.sendRequest(conSetupRequest);
        // wait to register connection setup
        Thread.sleep(100L);
        return grpcConn;
      }
    } catch (Exception e) {
      LOGGER.error("[GrpcClient]Fail to connect to server!,error={}", e);
    }
    return null;
  }

  private void shuntDownChannel(ManagedChannel managedChannel) {
    if (managedChannel != null && !managedChannel.isShutdown()) {
      managedChannel.shutdownNow();
    }
  }

  private StreamObserver<Payload> bindRequestStream(
      BiRequestStreamGrpc.BiRequestStreamStub streamStub, GrpcConnection grpcConn) {
    return streamStub.requestBiStream(
        new StreamObserver<Payload>() {

          @Override
          public void onNext(Payload payload) {
            LOGGER.debug(
                "[{}]Stream server request receive, original info: {}",
                grpcConn.getConnectionId(),
                payload.toString());
          }

          @Override
          public void onError(Throwable throwable) {
            LOGGER.error(
                "[{}]Stream server Error receive, original info: {}",
                grpcConn.getConnectionId(),
                throwable);
          }

          @Override
          public void onCompleted() {
            LOGGER.debug(
                "[{}]Stream server Completed receive, original info: {}",
                grpcConn.getConnectionId());
          }
        });
  }

  private void sendResponse(Object response) {
    try {
      this.currentConnection.sendResponse(response);
    } catch (Exception e) {
      LOGGER.error(
          "[{}]Error to send ack response, ackId->{}",
          this.currentConnection.getConnectionId(),
          response);
    }
  }

  @Override
  public boolean isConnected() {
    return currentConnection != null && currentConnection.isFine();
  }

  @Override
  public void ensureConnected() throws InterruptedException {
    if (isConnected()) {
      return;
    }
    while (!connect()) {
      Thread.sleep(GrpcConnection.RECONNECTING_DELAY);
    }
  }

  @Override
  public Object invokeSync(Object request) {
    Payload payload = currentConnection.request(request, 100);
    return GrpcUtils.parse(payload);
  }

  public void setWorker(WorkerThread worker) {
    this.worker = worker;
  }
}
