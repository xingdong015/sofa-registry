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
package com.alipay.sofa.registry.core.utils;

import com.alipay.sofa.registry.core.grpc.*;
import com.fasterxml.jackson.databind.util.ByteBufferBackedInputStream;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.UnsafeByteOperations;
import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author chengzhengzheng
 * @date 2022/11/23
 */
public class GrpcUtils {

  public static final ThreadPoolExecutor grpcServerExecutor =
      new ThreadPoolExecutor(10, 10, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());

  public static Object parse(Payload payload) {
    Class classType = PayloadRegistry.getClassByType(payload.getMetadata().getType());
    if (classType != null) {
      ByteString byteString = payload.getBody().getValue();
      ByteBuffer byteBuffer = byteString.asReadOnlyByteBuffer();
      return JacksonUtils.read(new ByteBufferBackedInputStream(byteBuffer), classType);
    }
    throw new RuntimeException(" classType error");
  }

  public static <T> Payload convert(T request) {
    Metadata newMeta =
        Metadata.newBuilder()
            .setType(request.getClass().getSimpleName())
            .setClientIp(NetUtils.localIP())
            .build();
    byte[] jsonBytes = JacksonUtils.toJsonBytes(request);

    Payload.Builder builder = Payload.newBuilder();

    return builder
        .setBody(Any.newBuilder().setValue(UnsafeByteOperations.unsafeWrap(jsonBytes)))
        .setMetadata(newMeta)
        .build();
  }

  public static <T> T parse(Payload payload, Class<T> classType) {
    ByteString byteString = payload.getBody().getValue();
    ByteBuffer byteBuffer = byteString.asReadOnlyByteBuffer();
    return JacksonUtils.toObj(new ByteBufferBackedInputStream(byteBuffer), classType);
  }

  public static void main(String[] args) {
    Payload convert = GrpcUtils.convert(new ServerCheckResponse("-1"));
    System.out.println(convert);
  }
}
