package com.alipay.sofa.registry.core.grpc;

/**
 * @author chengzhengzheng
 * @date 2023/1/9
 */
public enum ResponseCode {

    /**
     * Request success.
     */
    SUCCESS(200, "Response ok"),

    /**
     * Request failed.
     */
    FAIL(500, "Response fail");

    int code;

    String desc;

    ResponseCode(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * Getter method for property <tt>code</tt>.
     *
     * @return property value of code
     */
    public int getCode() {
        return code;
    }

    /**
     * Setter method for property <tt>code</tt>.
     *
     * @param code value to be assigned to property code
     */
    public void setCode(int code) {
        this.code = code;
    }

    /**
     * Getter method for property <tt>desc</tt>.
     *
     * @return property value of desc
     */
    public String getDesc() {
        return desc;
    }

    /**
     * Setter method for property <tt>desc</tt>.
     *
     * @param desc value to be assigned to property desc
     */
    public void setDesc(String desc) {
        this.desc = desc;
    }
}
