package com.alipay.sofa.registry.core.grpc;

import com.alipay.sofa.registry.core.model.PublisherRegister;

import java.util.HashMap;
import java.util.Map;

/**
 * @author chengzhengzheng
 * @date 2022/11/24
 */
public class PayloadRegistry {

    private static final Map<String, Class<?>> REGISTRY_REQUEST = new HashMap<>();

    public static Class getClassByType(String type) {
        return REGISTRY_REQUEST.get(type);
    }

    static {
        init();
    }

    private static void init() {
        REGISTRY_REQUEST.put(PublisherRegister.class.getSimpleName(),PublisherRegister.class);
        REGISTRY_REQUEST.put(ServerCheckRequest.class.getSimpleName(),ServerCheckRequest.class);
        REGISTRY_REQUEST.put(ConnectionSetupRequest.class.getSimpleName(),ConnectionSetupRequest.class);
        REGISTRY_REQUEST.put(ServerCheckResponse.class.getSimpleName(),ServerCheckResponse.class);

    }
}
