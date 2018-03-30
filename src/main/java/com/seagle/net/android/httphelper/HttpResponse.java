package com.seagle.net.android.httphelper;


/**
 * <h1>Http响应</h1>
 * 封装HTTP响应信息
 *
 * @author : xiudong.yuan@midea.com.cn
 * @date : 2016/4/23
 */
public final class HttpResponse<T> {
    /**
     * 服务器返回成功标识
     */
    public static final int SUCCESS = 0;
    /**
     * 请求执行失败
     */
    public static final int ERROR_CODE_REQUEST_FAILED = -100;
    /**
     * 请求超时
     */
    public static final int ERROR_CODE_TIME_OUT = -101;
    /**
     * 结果解析失败
     */
    public static final int ERROR_CODE_RESOLVE_FAILED = -102;
    /**
     * HTTP请求服务器错误.
     */
    public static final int ERROR_CODE_HTTP = -103;
    /**
     * HTTP服务器返回的业务逻辑错误
     */
    public static final int ERROR_CODE_SERVER = -104;

    /**
     * 响应码。除了标识HTTP响应码外，还标识本地错误。
     */
    private final int mCode;

    /**
     * 业务错误码
     */
    private int mResultCode;

    /**
     * 响应消息。除了标识HTT响应消息外，还标识本地错误信息以及服务器的逻辑错误信息。
     */
    private final String mMessage;

    /**
     * 消息响应
     */
    private T mRespResult;

    /**
     * 响应数据。
     */
    private final String mRespRawData;

    public HttpResponse(int code, String message, String respRawData) {
        mCode = code;
        mMessage = message;
        mRespRawData = respRawData;
    }

    public boolean isSuccess() {
        return mCode == SUCCESS;
    }

    /**
     * 获取响应码
     *
     * @return 响应码
     */
    public int getCode() {
        return mCode;
    }

    /**
     * 获取响应的消息
     *
     * @return 响应消息
     */
    public String getMessage() {
        return mMessage;
    }

    /**
     * 获取响应的原始数据
     *
     * @return 原始数据
     */
    public String getRawData() {
        return mRespRawData;
    }

    /**
     * 设置服务器业务错误码.
     * 如果响应错误码不是HTTP_OK，业务错误码无效。
     *
     * @param resultCode 错误码
     */
    void setResultCode(int resultCode) {
        mResultCode = resultCode;
    }

    /**
     * 获取响应的业务错误码.
     *
     * @return 错误码
     */
    public int getResultCode() {
        return mResultCode;
    }

    /**
     * 获取响应结果
     *
     * @return 响应结果
     */
    public T getResult() {
        return mRespResult;
    }

    /**
     * 设置请求结果
     *
     * @param respResult 结果
     */
    void setResult(T respResult) {
        mRespResult = respResult;
    }

    @Override
    public String toString() {
        return "HttpResponse{" +
                "mCode=" + mCode +
                ", mMessage='" + mMessage + '\'' +
                ", mRespRawData='" + mRespRawData + '\'' +
                '}';
    }
}
