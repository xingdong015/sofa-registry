package grpc;

import com.alipay.hessian.clhm.ConcurrentLinkedHashMap;
import com.alipay.sofa.registry.core.grpc.response.Response;
import com.alipay.sofa.registry.remoting.RemotingException;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * @author chengzhengzheng
 * @date 2023/1/8
 */
public class RpcAckCallbackSynchronizer {
    public static final Map<String, Map<String, DefaultRequestFuture>> CALLBACK_CONTEXT = new ConcurrentLinkedHashMap.Builder<String, Map<String, DefaultRequestFuture>>()
            .maximumWeightedCapacity(1000000)
            .listener((s, pushCallBack) -> pushCallBack.entrySet().forEach(
                    stringDefaultPushFutureEntry -> stringDefaultPushFutureEntry.getValue().setFailResult(new TimeoutException()))).build();

    /**
     * notify  ack.
     */
    public static void ackNotify(String connectionId, Response response) {

        Map<String, DefaultRequestFuture> stringDefaultPushFutureMap = CALLBACK_CONTEXT.get(connectionId);
        if (stringDefaultPushFutureMap == null) {
            return;
        }

        DefaultRequestFuture currentCallback = stringDefaultPushFutureMap.remove(response.getRequestId());
        if (currentCallback == null) {
            return;
        }

        if (response.isSuccess()) {
            currentCallback.setResponse(response);
        } else {
            currentCallback.setFailResult(new RemotingException(response.getMessage()));
        }
    }

    /**
     * notify  ackid.
     */
    public static void syncCallback(String connectionId, String requestId, DefaultRequestFuture defaultPushFuture) {

        Map<String, DefaultRequestFuture> stringDefaultPushFutureMap = initContextIfNecessary(connectionId);

        if (!stringDefaultPushFutureMap.containsKey(requestId)) {
            DefaultRequestFuture pushCallBackPrev = stringDefaultPushFutureMap
                    .putIfAbsent(requestId, defaultPushFuture);
            if (pushCallBackPrev == null) {
                return;
            }
        }
        throw new RuntimeException();
    }

    /**
     * clear context of connectionId.
     *
     * @param connectionId connectionId
     */
    public static void clearContext(String connectionId) {
        CALLBACK_CONTEXT.remove(connectionId);
    }

    /**
     * clear context of connectionId.
     *
     * @param connectionId connectionId
     */
    public static Map<String, DefaultRequestFuture> initContextIfNecessary(String connectionId) {
        if (!CALLBACK_CONTEXT.containsKey(connectionId)) {
            Map<String, DefaultRequestFuture> context = new HashMap<>(128);
            Map<String, DefaultRequestFuture> stringDefaultRequestFutureMap = CALLBACK_CONTEXT
                    .putIfAbsent(connectionId, context);
            return stringDefaultRequestFutureMap == null ? context : stringDefaultRequestFutureMap;
        } else {
            return CALLBACK_CONTEXT.get(connectionId);
        }
    }

    /**
     * clear context of connectionId.
     *
     * @param connectionId connectionId
     */
    public static void clearFuture(String connectionId, String requestId) {
        Map<String, DefaultRequestFuture> stringDefaultPushFutureMap = CALLBACK_CONTEXT.get(connectionId);

        if (stringDefaultPushFutureMap == null || !stringDefaultPushFutureMap.containsKey(requestId)) {
            return;
        }
        stringDefaultPushFutureMap.remove(requestId);
    }

}
