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
package com.alipay.sofa.registry.core.grpc.response;

import com.alipay.sofa.registry.core.grpc.Payload;
import com.alipay.sofa.registry.core.grpc.ResponseCode;

/**
 * @author chengzhengzheng
 * @date 2022/11/23
 */
public class Response implements Payload {

    int resultCode = ResponseCode.SUCCESS.getCode();

    int errorCode;

    String message;

    String requestId;

    /**
     * Getter method for property <tt>requestId</tt>.
     *
     * @return property value of requestId
     */
    public String getRequestId() {
        return requestId;
    }

    /**
     * Setter method for property <tt>requestId</tt>.
     *
     * @param requestId value to be assigned to property requestId
     */
    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    /**
     * Check Response  is Successed.
     *
     * @return success or not.
     */
    public boolean isSuccess() {
        return this.resultCode == ResponseCode.SUCCESS.getCode();
    }

    /**
     * Getter method for property <tt>resultCode</tt>.
     *
     * @return property value of resultCode
     */
    public int getResultCode() {
        return resultCode;
    }

    /**
     * Setter method for property <tt>resultCode</tt>.
     *
     * @param resultCode value to be assigned to property resultCode
     */
    public void setResultCode(int resultCode) {
        this.resultCode = resultCode;
    }

    /**
     * Getter method for property <tt>message</tt>.
     *
     * @return property value of message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Setter method for property <tt>message</tt>.
     *
     * @param message value to be assigned to property message
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Getter method for property <tt>errorCode</tt>.
     *
     * @return property value of errorCode
     */
    public int getErrorCode() {
        return errorCode;
    }

    /**
     * Setter method for property <tt>errorCode</tt>.
     *
     * @param errorCode value to be assigned to property errorCode
     */
    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    public void setErrorInfo(int errorCode, String errorMsg) {
        this.resultCode = ResponseCode.FAIL.getCode();
        this.errorCode = errorCode;
        this.message = errorMsg;
    }

    @Override
    public String toString() {
        return "Response{" + "resultCode=" + resultCode + ", errorCode=" + errorCode + ", message='" + message + '\''
                + ", requestId='" + requestId + '\'' + '}';
    }

}
