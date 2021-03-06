package com.seagle.net.android.httphelper;

import android.text.TextUtils;

import java.io.File;
import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <h1>HTTP请求参数</h1>
 * 封装了HTTP请求的相关配置与参数。
 *
 * @author : yuanxiudong66@sina.com
 */
public final class HttpRequest implements Serializable {

    /**
     * POST请求.
     */
    static final String HTTP_POST = "POST";

    /**
     * GET请求.
     */
    static final String HTTP_GET = "GET";

    /**
     * 服务器地址，包括端口.
     */
    private final String mHost;

    /**
     * 请求路径.
     */
    private String mPath;

    /**
     * HTTP提交模式.
     */
    private String mMethod;

    /**
     * 连接参数配置.
     */
    private final Map<String, String> mConnectSettingsMap;

    /**
     * Http请求头部配置.
     */
    private final Map<String, String> mHeadersMap;

    /**
     * HTTP请求参数.
     */
    private final Map<String, String> mParamsMap;

    /**
     * 文件上传列表.
     */
    private final Map<String, File> mParamsFileMap;

    public HttpRequest(String host, String path) {
        if (TextUtils.isEmpty(host)) {
            throw new IllegalArgumentException("Host should not be null!");
        }
        mHost = host;
        mPath = path;
        mConnectSettingsMap = new ConcurrentHashMap<>();
        mHeadersMap = new ConcurrentHashMap<>();
        mParamsMap = new ConcurrentHashMap<>();
        mParamsFileMap = new ConcurrentHashMap<>();
    }

    public String getPath() {
        return mPath;
    }

    public void setPath(String path) {
        mPath = path;
    }

    /**
     * 增加HTTP请求头参数。
     *
     * @param key   名称
     * @param value 值
     */
    public void addRequestHeaderProperty(String key, String value) {
        if (!TextUtils.isEmpty(key) && !TextUtils.isEmpty(value)) {
            mHeadersMap.put(key, value);
        }
    }

    /**
     * 获取HTTP请求头参数.
     *
     * @param key 名称
     * @return 参数值
     */
    public String getRequestHeaderProperty(String key) {
        if (!TextUtils.isEmpty(key)) {
            return mHeadersMap.get(key);
        }
        return null;
    }

    /**
     * 获取全部HTTP请求头参数
     *
     * @return HTTP请求头
     */
    public Map<String, String> getRequestHeaders() {
        return Collections.unmodifiableMap(mHeadersMap);
    }

    /**
     * 配置连接参数。
     *
     * @param key   名称
     * @param value 值
     */
    public void setConnectSetting(String key, String value) {
        if (!TextUtils.isEmpty(key) && !TextUtils.isEmpty(value)) {
            mConnectSettingsMap.put(key, value);
        }
    }

    /**
     * 获取连接配置参数
     *
     * @param key 明
     * @return 值
     */
    public String getConnectSetting(String key) {
        if (!TextUtils.isEmpty(key)) {
            return mConnectSettingsMap.get(key);
        }
        return null;
    }

    /**
     * 获取全部连接参数配置。
     *
     * @return 连接参数配置
     */
    public Map<String, String> getConnectSettings() {
        return Collections.unmodifiableMap(mConnectSettingsMap);
    }

    /**
     * 获取请求参数
     *
     * @return 请求参数
     */
    public Map<String, String> getRequestParams() {
        return mParamsMap;
    }

    /**
     * 获取文件列表。
     *
     * @return 文件列表
     */
    public Map<String, File> getRequestFileParams() {
        return mParamsFileMap;
    }

    /**
     * 设置请求参数
     *
     * @param paramsMap 请求数据
     */
    public void setRequestParams(Map<String, String> paramsMap) {
        if (null != paramsMap) {
            mParamsMap.clear();
            mParamsMap.putAll(paramsMap);
        }
    }

    /**
     * 增加请求参数
     *
     * @param key   参数
     * @param value 值
     */
    public void addRequestParam(String key, String value) {
        if (!TextUtils.isEmpty(key) && null != value) {
            mParamsMap.put(key, value);
        }
    }

    /**
     * 增加待上传的文件
     *
     * @param key   文件名称
     * @param value 文件
     */
    public void addRequestFileParam(String key, File value) {
        if (!TextUtils.isEmpty(key) && null != value) {
            mParamsFileMap.put(key, value);
        }
    }

    /**
     * 批量增加请求参数
     *
     * @param paramsMap 请求参数
     */
    public void addRequestParams(Map<String, String> paramsMap) {
        if (null != paramsMap) {
            for (String key : paramsMap.keySet()) {
                addRequestParam(key, paramsMap.get(key));
            }
        }
    }

    /**
     * 设置请求方法
     *
     * @param method GET or POST
     */
    void setRequestMethod(String method) {
        mMethod = method;
    }

    /**
     * 设置请求超时时间，单位：ms
     *
     * @param timeoutMillis 超时时间
     */
    public void setRequestTimeout(int timeoutMillis) {
        setConnectSetting("requestTimeout", Integer.toString(timeoutMillis));
    }

    /**
     * 获取请求超时时间，单位：MS
     *
     * @return 超时时间
     */
    public int getRequestTimeout() {
        if (mConnectSettingsMap.containsKey("requestTimeout")) {
            return Integer.parseInt(mConnectSettingsMap.get("requestTimeout"));
        }
        return 10000;
    }

    /**
     * 获取服务器地址
     *
     * @return 服务器地址
     */
    public String getRequestHost() {
        return mHost;
    }

    /**
     * 获取请求地址
     *
     * @return 地址
     */
    public String getRequestPath() {
        return mPath;
    }

    /**
     * 获取提交方法
     *
     * @return 提交方法
     */
    public String getRequestMethod() {
        return mMethod;
    }
}
