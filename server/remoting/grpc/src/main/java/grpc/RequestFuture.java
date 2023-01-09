package grpc;


import com.alipay.sofa.registry.core.grpc.response.Response;

/**
 * @author chengzhengzheng
 * @date 2023/1/8
 */
public interface RequestFuture {

    /**
     * check that it is done or not..
     *
     * @return is done .
     */
    boolean isDone();

    /**
     * get response without timeouts.
     *
     * @return return response if done.
     * @throws Exception exception throws .
     */
    Response get() throws Exception;

    /**
     * get response with a given timeouts.
     *
     * @param timeout timeout milliseconds.
     * @return return response if done.
     * @throws Exception exception throws .
     */
    Response get(long timeout) throws Exception;
}
