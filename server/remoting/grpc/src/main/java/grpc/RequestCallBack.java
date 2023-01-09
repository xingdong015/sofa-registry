package grpc;

import com.alipay.sofa.registry.core.grpc.response.Response;

import java.util.concurrent.Executor;

/**
 * @author chengzhengzheng
 * @date 2023/1/8
 */
public interface RequestCallBack<T extends Response> {
    /**
     * get executor on callback.
     *
     * @return executor.
     */
    Executor getExecutor();

    /**
     * get timeout mills.
     *
     * @return timeouts.
     */
    long getTimeout();

    /**
     * called on success.
     *
     * @param response response received.
     */
    void onResponse(T response);

    /**
     * called on failed.
     *
     * @param e exception throwed.
     */
    void onException(Throwable e);
}
