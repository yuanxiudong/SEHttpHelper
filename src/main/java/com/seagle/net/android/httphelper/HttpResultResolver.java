package com.seagle.net.android.httphelper;

/**
 * <h1>请求解析</h1>
 * <p>将请求返回的字符串数据解析成用户数据对象。</P>
 *
 * @author : yuanxiudong66@sina.com
 */
public abstract class HttpResultResolver<Result> {
    /**
     * 数据解析错误码
     */
    protected int errorCode;

    /**
     * 错误消息
     */
    protected String errorMsg;

    /**
     * 结果的类型
     */
    protected final Class<Result> mResultClass;

    public HttpResultResolver(Class<Result> resultClass) {
        mResultClass = resultClass;
    }

    /**
     * 处理HTTP响应数据.
     *
     * @param httpRespData 响应数据
     * @hide
     */
    protected abstract Result resolverHttpRespData(String httpRespData) throws Exception;

    /**
     * 返回错误码.
     *
     * @return 错误码
     */
    public int getErrorCode() {
        return errorCode;
    }

    /**
     * 返回错误消息.
     *
     * @return 错误消息
     */
    public String getErrorMsg() {
        return errorMsg;
    }

    /**
     * 是否成功.
     *
     * @return 请求成功
     */
    public abstract boolean isSuccess();
}
