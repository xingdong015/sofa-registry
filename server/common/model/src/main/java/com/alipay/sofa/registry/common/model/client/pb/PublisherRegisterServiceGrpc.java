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
public final class PublisherRegisterServiceGrpc {

  private PublisherRegisterServiceGrpc() {}

  public static final String SERVICE_NAME = "PublisherRegisterService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<PublisherRegisterPb, RegisterResponsePb>
      getRequestMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "request",
      requestType = PublisherRegisterPb.class,
      responseType = RegisterResponsePb.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<PublisherRegisterPb, RegisterResponsePb>
      getRequestMethod() {
    io.grpc.MethodDescriptor<PublisherRegisterPb, RegisterResponsePb> getRequestMethod;
    if ((getRequestMethod = PublisherRegisterServiceGrpc.getRequestMethod) == null) {
      synchronized (PublisherRegisterServiceGrpc.class) {
        if ((getRequestMethod = PublisherRegisterServiceGrpc.getRequestMethod) == null) {
          PublisherRegisterServiceGrpc.getRequestMethod =
              getRequestMethod =
                  io.grpc.MethodDescriptor.<PublisherRegisterPb, RegisterResponsePb>newBuilder()
                      .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                      .setFullMethodName(generateFullMethodName(SERVICE_NAME, "request"))
                      .setSampledToLocalTracing(true)
                      .setRequestMarshaller(
                          io.grpc.protobuf.ProtoUtils.marshaller(
                              PublisherRegisterPb.getDefaultInstance()))
                      .setResponseMarshaller(
                          io.grpc.protobuf.ProtoUtils.marshaller(
                              RegisterResponsePb.getDefaultInstance()))
                      .setSchemaDescriptor(
                          new PublisherRegisterServiceMethodDescriptorSupplier("request"))
                      .build();
        }
      }
    }
    return getRequestMethod;
  }

  /** Creates a new async stub that supports all call types for the service */
  public static PublisherRegisterServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<PublisherRegisterServiceStub> factory =
        new io.grpc.stub.AbstractStub.StubFactory<PublisherRegisterServiceStub>() {
          @Override
          public PublisherRegisterServiceStub newStub(
              io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
            return new PublisherRegisterServiceStub(channel, callOptions);
          }
        };
    return PublisherRegisterServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static PublisherRegisterServiceBlockingStub newBlockingStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<PublisherRegisterServiceBlockingStub> factory =
        new io.grpc.stub.AbstractStub.StubFactory<PublisherRegisterServiceBlockingStub>() {
          @Override
          public PublisherRegisterServiceBlockingStub newStub(
              io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
            return new PublisherRegisterServiceBlockingStub(channel, callOptions);
          }
        };
    return PublisherRegisterServiceBlockingStub.newStub(factory, channel);
  }

  /** Creates a new ListenableFuture-style stub that supports unary calls on the service */
  public static PublisherRegisterServiceFutureStub newFutureStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<PublisherRegisterServiceFutureStub> factory =
        new io.grpc.stub.AbstractStub.StubFactory<PublisherRegisterServiceFutureStub>() {
          @Override
          public PublisherRegisterServiceFutureStub newStub(
              io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
            return new PublisherRegisterServiceFutureStub(channel, callOptions);
          }
        };
    return PublisherRegisterServiceFutureStub.newStub(factory, channel);
  }

  /** */
  public abstract static class PublisherRegisterServiceImplBase implements io.grpc.BindableService {

    /**
     *
     *
     * <pre>
     * Sends a commonRequest
     * </pre>
     */
    public void request(
        PublisherRegisterPb request,
        io.grpc.stub.StreamObserver<RegisterResponsePb> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getRequestMethod(), responseObserver);
    }

    @Override
    public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
              getRequestMethod(),
              io.grpc.stub.ServerCalls.asyncUnaryCall(
                  new MethodHandlers<PublisherRegisterPb, RegisterResponsePb>(
                      this, METHODID_REQUEST)))
          .build();
    }
  }

  /** */
  public static final class PublisherRegisterServiceStub
      extends io.grpc.stub.AbstractAsyncStub<PublisherRegisterServiceStub> {
    private PublisherRegisterServiceStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @Override
    protected PublisherRegisterServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new PublisherRegisterServiceStub(channel, callOptions);
    }

    /**
     *
     *
     * <pre>
     * Sends a commonRequest
     * </pre>
     */
    public void request(
        PublisherRegisterPb request,
        io.grpc.stub.StreamObserver<RegisterResponsePb> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getRequestMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /** */
  public static final class PublisherRegisterServiceBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<PublisherRegisterServiceBlockingStub> {
    private PublisherRegisterServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @Override
    protected PublisherRegisterServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new PublisherRegisterServiceBlockingStub(channel, callOptions);
    }

    /**
     *
     *
     * <pre>
     * Sends a commonRequest
     * </pre>
     */
    public RegisterResponsePb request(PublisherRegisterPb request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getRequestMethod(), getCallOptions(), request);
    }
  }

  /** */
  public static final class PublisherRegisterServiceFutureStub
      extends io.grpc.stub.AbstractFutureStub<PublisherRegisterServiceFutureStub> {
    private PublisherRegisterServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @Override
    protected PublisherRegisterServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new PublisherRegisterServiceFutureStub(channel, callOptions);
    }

    /**
     *
     *
     * <pre>
     * Sends a commonRequest
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<RegisterResponsePb> request(
        PublisherRegisterPb request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getRequestMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_REQUEST = 0;

  private static final class MethodHandlers<Req, Resp>
      implements io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
          io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
          io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
          io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final PublisherRegisterServiceImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(PublisherRegisterServiceImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_REQUEST:
          serviceImpl.request(
              (PublisherRegisterPb) request,
              (io.grpc.stub.StreamObserver<RegisterResponsePb>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @Override
    @SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }
  }

  private abstract static class PublisherRegisterServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier,
          io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    PublisherRegisterServiceBaseDescriptorSupplier() {}

    @Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return PublisherRegisterServiceOuterClass.getDescriptor();
    }

    @Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("PublisherRegisterService");
    }
  }

  private static final class PublisherRegisterServiceFileDescriptorSupplier
      extends PublisherRegisterServiceBaseDescriptorSupplier {
    PublisherRegisterServiceFileDescriptorSupplier() {}
  }

  private static final class PublisherRegisterServiceMethodDescriptorSupplier
      extends PublisherRegisterServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    PublisherRegisterServiceMethodDescriptorSupplier(String methodName) {
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
      synchronized (PublisherRegisterServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor =
              result =
                  io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
                      .setSchemaDescriptor(new PublisherRegisterServiceFileDescriptorSupplier())
                      .addMethod(getRequestMethod())
                      .build();
        }
      }
    }
    return result;
  }
}
