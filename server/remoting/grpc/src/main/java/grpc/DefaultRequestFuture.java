package grpc;

import com.alipay.remoting.TimerHolder;
import com.alipay.sofa.registry.core.grpc.Response;
import io.netty.util.Timeout;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author chengzhengzheng
 * @date 2023/1/8
 */
public class DefaultRequestFuture implements RequestFuture {

    private long timeStamp;

    private boolean isSuccess;

    private volatile boolean isDone = false;

    private RequestCallBack requestCallBack;

    private Exception exception;

    private String requestId;

    private String connectionId;

    private Response response;

    private Timeout timeout;

    TimeoutInnerTrigger timeoutInnerTrigger;


    public DefaultRequestFuture(String connectionId, String requestId) {
        this(connectionId, requestId, null, null);
    }

    public DefaultRequestFuture(String connectionId, String requestId, RequestCallBack requestCallBack,
                                TimeoutInnerTrigger timeoutInnerTrigger) {
        this.timeStamp       = System.currentTimeMillis();
        this.requestCallBack = requestCallBack;
        this.requestId       = requestId;
        this.connectionId    = connectionId;
        if (requestCallBack != null) {
            this.timeout = TimerHolder.getTimer().newTimeout(timeout -> timeoutInnerTrigger.triggerOnTimeout(),
                    requestCallBack.getTimeout(),
                    TimeUnit.MILLISECONDS);
        }
        this.timeoutInnerTrigger = timeoutInnerTrigger;
    }

    @Override
    public boolean isDone() {
        return isDone;
    }

    @Override
    public Response get() throws Exception {
        synchronized (this) {
            while (!isDone) {
                wait();
            }
        }
        return response;
    }

    @Override
    public Response get(long timeout) throws Exception {
        if (timeout < 0) {
            synchronized (this) {
                while (!isDone) {
                    wait();
                }
            }
        } else if (timeout > 0) {
            long end = System.currentTimeMillis() + timeout;
            long waitTime = timeout;
            synchronized (this) {
                while (!isDone && waitTime > 0) {
                    wait(waitTime);
                    waitTime = end - System.currentTimeMillis();
                }
            }
        }

        if (isDone) {
            return response;
        } else {
            if (timeoutInnerTrigger != null) {
                timeoutInnerTrigger.triggerOnTimeout();
            }
            throw new TimeoutException("request timeout after " + timeout + " milliseconds, requestId=" + requestId);
        }
    }

    public interface TimeoutInnerTrigger {

        /**
         * triggered on timeout .
         */
        void triggerOnTimeout();

    }

    /**
     * Getter method for property <tt>connectionId</tt>.
     *
     * @return property value of connectionId
     */
    public String getConnectionId() {
        return connectionId;
    }

    public void setResponse(final Response response) {
        isDone = true;
        this.response = response;
        this.isSuccess = response.isSuccess();
        if (this.timeout != null) {
            timeout.cancel();
        }
        synchronized (this) {
            notifyAll();
        }

        callBacInvoke();
    }

    private void callBacInvoke() {
        if (requestCallBack != null) {
            if (requestCallBack.getExecutor() != null) {
                requestCallBack.getExecutor().execute(new CallBackHandler());
            } else {
                new CallBackHandler().run();
            }
        }
    }

    class CallBackHandler implements Runnable {

        @Override
        public void run() {
            if (exception != null) {
                requestCallBack.onException(exception);
            } else {
                requestCallBack.onResponse(response);
            }
        }
    }

    public void setFailResult(Exception e) {
        isDone = true;
        this.exception = e;
        synchronized (this) {
            notifyAll();
        }

        callBacInvoke();
    }
}
