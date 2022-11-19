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
package com.alipay.sofa.registry.common.model.client.pb;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/** */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.50.2)",
    comments = "Source: PublisherRegisterService.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class BiPublisherRegisterServiceGrpc {

  private BiPublisherRegisterServiceGrpc() {}

  public static final String SERVICE_NAME = "BiPublisherRegisterService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<PublisherRegisterPb, RegisterResponsePb>
      getRequestBiStreamMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "requestBiStream",
      requestType = PublisherRegisterPb.class,
      responseType = RegisterResponsePb.class,
      methodType = io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING)
  public static io.grpc.MethodDescriptor<PublisherRegisterPb, RegisterResponsePb>
      getRequestBiStreamMethod() {
    io.grpc.MethodDescriptor<PublisherRegisterPb, RegisterResponsePb> getRequestBiStreamMethod;
    if ((getRequestBiStreamMethod = BiPublisherRegisterServiceGrpc.getRequestBiStreamMethod)
        == null) {
      synchronized (BiPublisherRegisterServiceGrpc.class) {
        if ((getRequestBiStreamMethod = BiPublisherRegisterServiceGrpc.getRequestBiStreamMethod)
            == null) {
          BiPublisherRegisterServiceGrpc.getRequestBiStreamMethod =
              getRequestBiStreamMethod =
                  io.grpc.MethodDescriptor.<PublisherRegisterPb, RegisterResponsePb>newBuilder()
                      .setType(io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING)
                      .setFullMethodName(generateFullMethodName(SERVICE_NAME, "requestBiStream"))
                      .setSampledToLocalTracing(true)
                      .setRequestMarshaller(
                          io.grpc.protobuf.ProtoUtils.marshaller(
                              PublisherRegisterPb.getDefaultInstance()))
                      .setResponseMarshaller(
                          io.grpc.protobuf.ProtoUtils.marshaller(
                              RegisterResponsePb.getDefaultInstance()))
                      .setSchemaDescriptor(
                          new BiPublisherRegisterServiceMethodDescriptorSupplier("requestBiStream"))
                      .build();
        }
      }
    }
    return getRequestBiStreamMethod;
  }

  /** Creates a new async stub that supports all call types for the service */
  public static BiPublisherRegisterServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<BiPublisherRegisterServiceStub> factory =
        new io.grpc.stub.AbstractStub.StubFactory<BiPublisherRegisterServiceStub>() {
          @Override
          public BiPublisherRegisterServiceStub newStub(
              io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
            return new BiPublisherRegisterServiceStub(channel, callOptions);
          }
        };
    return BiPublisherRegisterServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static BiPublisherRegisterServiceBlockingStub newBlockingStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<BiPublisherRegisterServiceBlockingStub> factory =
        new io.grpc.stub.AbstractStub.StubFactory<BiPublisherRegisterServiceBlockingStub>() {
          @Override
          public BiPublisherRegisterServiceBlockingStub newStub(
              io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
            return new BiPublisherRegisterServiceBlockingStub(channel, callOptions);
          }
        };
    return BiPublisherRegisterServiceBlockingStub.newStub(factory, channel);
  }

  /** Creates a new ListenableFuture-style stub that supports unary calls on the service */
  public static BiPublisherRegisterServiceFutureStub newFutureStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<BiPublisherRegisterServiceFutureStub> factory =
        new io.grpc.stub.AbstractStub.StubFactory<BiPublisherRegisterServiceFutureStub>() {
          @Override
          public BiPublisherRegisterServiceFutureStub newStub(
              io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
            return new BiPublisherRegisterServiceFutureStub(channel, callOptions);
          }
        };
    return BiPublisherRegisterServiceFutureStub.newStub(factory, channel);
  }

  /** */
  public abstract static class BiPublisherRegisterServiceImplBase
      implements io.grpc.BindableService {

    /**
     *
     *
     * <pre>
     * Sends a biStreamRequest
     * </pre>
     */
    public io.grpc.stub.StreamObserver<PublisherRegisterPb> requestBiStream(
        io.grpc.stub.StreamObserver<RegisterResponsePb> responseObserver) {
      return io.grpc.stub.ServerCalls.asyncUnimplementedStreamingCall(
          getRequestBiStreamMethod(), responseObserver);
    }

    @Override
    public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
              getRequestBiStreamMethod(),
              io.grpc.stub.ServerCalls.asyncBidiStreamingCall(
                  new MethodHandlers<PublisherRegisterPb, RegisterResponsePb>(
                      this, METHODID_REQUEST_BI_STREAM)))
          .build();
    }
  }

  /** */
  public static final class BiPublisherRegisterServiceStub
      extends io.grpc.stub.AbstractAsyncStub<BiPublisherRegisterServiceStub> {
    private BiPublisherRegisterServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @Override
    protected BiPublisherRegisterServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new BiPublisherRegisterServiceStub(channel, callOptions);
    }

    /**
     *
     *
     * <pre>
     * Sends a biStreamRequest
     * </pre>
     */
    public io.grpc.stub.StreamObserver<PublisherRegisterPb> requestBiStream(
        io.grpc.stub.StreamObserver<RegisterResponsePb> responseObserver) {
      return io.grpc.stub.ClientCalls.asyncBidiStreamingCall(
          getChannel().newCall(getRequestBiStreamMethod(), getCallOptions()), responseObserver);
    }
  }

  /** */
  public static final class BiPublisherRegisterServiceBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<BiPublisherRegisterServiceBlockingStub> {
    private BiPublisherRegisterServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @Override
    protected BiPublisherRegisterServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new BiPublisherRegisterServiceBlockingStub(channel, callOptions);
    }
  }

  /** */
  public static final class BiPublisherRegisterServiceFutureStub
      extends io.grpc.stub.AbstractFutureStub<BiPublisherRegisterServiceFutureStub> {
    private BiPublisherRegisterServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @Override
    protected BiPublisherRegisterServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new BiPublisherRegisterServiceFutureStub(channel, callOptions);
    }
  }

  private static final int METHODID_REQUEST_BI_STREAM = 0;

  private static final class MethodHandlers<Req, Resp>
      implements io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
          io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
          io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
          io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final BiPublisherRegisterServiceImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(BiPublisherRegisterServiceImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }

    @Override
    @SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_REQUEST_BI_STREAM:
          return (io.grpc.stub.StreamObserver<Req>)
              serviceImpl.requestBiStream(
                  (io.grpc.stub.StreamObserver<RegisterResponsePb>) responseObserver);
        default:
          throw new AssertionError();
      }
    }
  }

  private abstract static class BiPublisherRegisterServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier,
          io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    BiPublisherRegisterServiceBaseDescriptorSupplier() {}

    @Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return PublisherRegisterServiceOuterClass.getDescriptor();
    }

    @Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("BiPublisherRegisterService");
    }
  }

  private static final class BiPublisherRegisterServiceFileDescriptorSupplier
      extends BiPublisherRegisterServiceBaseDescriptorSupplier {
    BiPublisherRegisterServiceFileDescriptorSupplier() {}
  }

  private static final class BiPublisherRegisterServiceMethodDescriptorSupplier
      extends BiPublisherRegisterServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    BiPublisherRegisterServiceMethodDescriptorSupplier(String methodName) {
      this.methodName = methodName;
    }

    @Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (BiPublisherRegisterServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor =
              result =
                  io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
                      .setSchemaDescriptor(new BiPublisherRegisterServiceFileDescriptorSupplier())
                      .addMethod(getRequestBiStreamMethod())
                      .build();
        }
      }
    }
    return result;
  }
}
