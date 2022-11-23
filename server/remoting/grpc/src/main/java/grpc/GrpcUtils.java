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
package grpc;

import com.alipay.sofa.registry.common.model.client.pb.Payload;
import com.alipay.sofa.registry.exception.SofaRegistryRuntimeException;
import com.alipay.sofa.registry.util.JsonUtils;
import com.fasterxml.jackson.databind.util.ByteBufferBackedInputStream;
import com.google.protobuf.ByteString;
import java.nio.ByteBuffer;
import java.rmi.RemoteException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author chengzhengzheng
 * @date 2022/11/20
 */
public class GrpcUtils {

  public static final ThreadPoolExecutor grpcServerExecutor =
      new ThreadPoolExecutor(
          10,
          10,
          60L,
          TimeUnit.SECONDS,
          new LinkedBlockingQueue<>(2000),
          new ThreadFactoryBuilder().daemon(true).nameFormat("grpc-server-executor-%d").build());

  public static Object parse(Payload payload) {
    Class classType = PayloadRegistry.getClassByType(payload.getMetadata().getType());
    if (classType != null) {
      ByteString byteString = payload.getBody().getValue();
      ByteBuffer byteBuffer = byteString.asReadOnlyByteBuffer();
      return JsonUtils.read(new ByteBufferBackedInputStream(byteBuffer), classType);
    }
    throw new SofaRegistryRuntimeException(" classType error");
  }

  public static Payload convert(Object payload) {
   return null;
  }
}
