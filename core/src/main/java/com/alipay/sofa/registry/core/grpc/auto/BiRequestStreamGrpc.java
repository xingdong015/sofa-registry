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
package com.alipay.sofa.registry.core.grpc.auto;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/** */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.50.2)",
    comments = "Source: registry_grpc_service.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class BiRequestStreamGrpc {

  private BiRequestStreamGrpc() {}

  public static final String SERVICE_NAME = "BiRequestStream";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<Payload, Payload> getRequestBiStreamMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "requestBiStream",
      requestType = Payload.class,
      responseType = Payload.class,
      methodType = io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING)
  public static io.grpc.MethodDescriptor<Payload, Payload> getRequestBiStreamMethod() {
    io.grpc.MethodDescriptor<Payload, Payload> getRequestBiStreamMethod;
    if ((getRequestBiStreamMethod = BiRequestStreamGrpc.getRequestBiStreamMethod) == null) {
      synchronized (BiRequestStreamGrpc.class) {
        if ((getRequestBiStreamMethod = BiRequestStreamGrpc.getRequestBiStreamMethod) == null) {
          BiRequestStreamGrpc.getRequestBiStreamMethod =
              getRequestBiStreamMethod =
                  io.grpc.MethodDescriptor.<Payload, Payload>newBuilder()
                      .setType(io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING)
                      .setFullMethodName(generateFullMethodName(SERVICE_NAME, "requestBiStream"))
                      .setSampledToLocalTracing(true)
                      .setRequestMarshaller(
                          io.grpc.protobuf.ProtoUtils.marshaller(Payload.getDefaultInstance()))
                      .setResponseMarshaller(
                          io.grpc.protobuf.ProtoUtils.marshaller(Payload.getDefaultInstance()))
                      .setSchemaDescriptor(
                          new BiRequestStreamMethodDescriptorSupplier("requestBiStream"))
                      .build();
        }
      }
    }
    return getRequestBiStreamMethod;
  }

  /** Creates a new async stub that supports all call types for the service */
  public static BiRequestStreamStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<BiRequestStreamStub> factory =
        new io.grpc.stub.AbstractStub.StubFactory<BiRequestStreamStub>() {
          @Override
          public BiRequestStreamStub newStub(
              io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
            return new BiRequestStreamStub(channel, callOptions);
          }
        };
    return BiRequestStreamStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static BiRequestStreamBlockingStub newBlockingStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<BiRequestStreamBlockingStub> factory =
        new io.grpc.stub.AbstractStub.StubFactory<BiRequestStreamBlockingStub>() {
          @Override
          public BiRequestStreamBlockingStub newStub(
              io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
            return new BiRequestStreamBlockingStub(channel, callOptions);
          }
        };
    return BiRequestStreamBlockingStub.newStub(factory, channel);
  }

  /** Creates a new ListenableFuture-style stub that supports unary calls on the service */
  public static BiRequestStreamFutureStub newFutureStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<BiRequestStreamFutureStub> factory =
        new io.grpc.stub.AbstractStub.StubFactory<BiRequestStreamFutureStub>() {
          @Override
          public BiRequestStreamFutureStub newStub(
              io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
            return new BiRequestStreamFutureStub(channel, callOptions);
          }
        };
    return BiRequestStreamFutureStub.newStub(factory, channel);
  }

  /** */
  public abstract static class BiRequestStreamImplBase implements io.grpc.BindableService {

    /**
     *
     *
     * <pre>
     * Sends a biStreamRequest
     * </pre>
     */
    public io.grpc.stub.StreamObserver<Payload> requestBiStream(
        io.grpc.stub.StreamObserver<Payload> responseObserver) {
      return io.grpc.stub.ServerCalls.asyncUnimplementedStreamingCall(
          getRequestBiStreamMethod(), responseObserver);
    }

    @Override
    public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
              getRequestBiStreamMethod(),
              io.grpc.stub.ServerCalls.asyncBidiStreamingCall(
                  new MethodHandlers<Payload, Payload>(this, METHODID_REQUEST_BI_STREAM)))
          .build();
    }
  }

  /** */
  public static final class BiRequestStreamStub
      extends io.grpc.stub.AbstractAsyncStub<BiRequestStreamStub> {
    private BiRequestStreamStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @Override
    protected BiRequestStreamStub build(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new BiRequestStreamStub(channel, callOptions);
    }

    /**
     *
     *
     * <pre>
     * Sends a biStreamRequest
     * </pre>
     */
    public io.grpc.stub.StreamObserver<Payload> requestBiStream(
        io.grpc.stub.StreamObserver<Payload> responseObserver) {
      return io.grpc.stub.ClientCalls.asyncBidiStreamingCall(
          getChannel().newCall(getRequestBiStreamMethod(), getCallOptions()), responseObserver);
    }
  }

  /** */
  public static final class BiRequestStreamBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<BiRequestStreamBlockingStub> {
    private BiRequestStreamBlockingStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @Override
    protected BiRequestStreamBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new BiRequestStreamBlockingStub(channel, callOptions);
    }
  }

  /** */
  public static final class BiRequestStreamFutureStub
      extends io.grpc.stub.AbstractFutureStub<BiRequestStreamFutureStub> {
    private BiRequestStreamFutureStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @Override
    protected BiRequestStreamFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new BiRequestStreamFutureStub(channel, callOptions);
    }
  }

  private static final int METHODID_REQUEST_BI_STREAM = 0;

  private static final class MethodHandlers<Req, Resp>
      implements io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
          io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
          io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
          io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final BiRequestStreamImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(BiRequestStreamImplBase serviceImpl, int methodId) {
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
              serviceImpl.requestBiStream((io.grpc.stub.StreamObserver<Payload>) responseObserver);
        default:
          throw new AssertionError();
      }
    }
  }

  private abstract static class BiRequestStreamBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier,
          io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    BiRequestStreamBaseDescriptorSupplier() {}

    @Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return RegistryGrpcService.getDescriptor();
    }

    @Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("BiRequestStream");
    }
  }

  private static final class BiRequestStreamFileDescriptorSupplier
      extends BiRequestStreamBaseDescriptorSupplier {
    BiRequestStreamFileDescriptorSupplier() {}
  }

  private static final class BiRequestStreamMethodDescriptorSupplier
      extends BiRequestStreamBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    BiRequestStreamMethodDescriptorSupplier(String methodName) {
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
      synchronized (BiRequestStreamGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor =
              result =
                  io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
                      .setSchemaDescriptor(new BiRequestStreamFileDescriptorSupplier())
                      .addMethod(getRequestBiStreamMethod())
                      .build();
        }
      }
    }
    return result;
  }
}
