package com.alipay.sofa.registry.client.grpc;

import com.alipay.sofa.registry.client.util.JacksonUtils;
import com.alipay.sofa.registry.client.util.NetUtils;
import com.alipay.sofa.registry.common.model.client.pb.Metadata;
import com.alipay.sofa.registry.common.model.client.pb.Payload;
import com.alipay.sofa.registry.common.model.client.pb.Request;
import com.alipay.sofa.registry.core.grpc.ServerCheckRequest;
import com.fasterxml.jackson.databind.util.ByteBufferBackedInputStream;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.UnsafeByteOperations;

import java.nio.ByteBuffer;

/**
 * @author chengzhengzheng
 * @date 2022/11/23
 */
public class GrpcUtils {

    public static Payload convert(ServerCheckRequest request) {
        Metadata newMeta = Metadata.newBuilder().setType(request.getClass().getSimpleName())
                .setClientIp(NetUtils.localIP()).build();
        byte[] jsonBytes = JacksonUtils.toJsonBytes(request);

        Payload.Builder builder = Payload.newBuilder();

        return builder
                .setBody(Any.newBuilder().setValue(UnsafeByteOperations.unsafeWrap(jsonBytes)))
                .setMetadata(newMeta).build();
    }

    public static <T> T parse(Payload payload,Class<T> classType) {
        ByteString byteString = payload.getBody().getValue();
        ByteBuffer byteBuffer = byteString.asReadOnlyByteBuffer();
        return JacksonUtils.toObj(new ByteBufferBackedInputStream(byteBuffer), classType);
    }
}
