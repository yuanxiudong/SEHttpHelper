package com.seagle.net.android.httphelper;

import android.os.AsyncTask;
import android.os.Bundle;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * <h1>HTTP请求协助类.</h1>
 * HttpHelper提供了一套HTTP请求的接口，包括HTTP和HTTPS以及GET和POST提交方式。
 * HttpHelper同时支持同步和异步。每一次提交返回一个HttpSession。
 * 同时每个提交的接口也有一个callback接口。
 * 同步调用：HttpSession.getResponse(),可以将callback置为空。
 * 异步调用：通过callback的回调得到请求的响应
 *
 * @author : yuanxiudong66@sina.com
 */
public final class HttpHelper {

    /**
     * HTTPS相关验证类.
     * 主要是证书验证和域名验证。
     */
    private volatile HttpsSSLConfig mHttpsSSLConfig;

    /**
     * 请求执行线程池.
     */
    private final Executor mExecutor;

    public HttpHelper() {
        mExecutor = Executors.newCachedThreadPool(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r, "HttpThread");
                thread.setDaemon(true);
                return thread;
            }
        });
    }

    public HttpHelper(Executor executor) {
        mExecutor = executor;
    }

    /**
     * 提交HTTP POST请求.
     *
     * @param <T>      响应类型
     * @param request  http请求
     * @param resolver 响应数据解析器
     * @param callback 回调，可以为空
     * @return HTTP响应
     */
    public <T> HttpSession<T> doHttpPost(HttpRequest request, HttpResultResolver<T> resolver, HttpCallback<T> callback) {
        request.setRequestMethod(HttpRequest.HTTP_POST);
        return doHttpRequest(request, resolver, callback, false);
    }

    /**
     * 提交HTTP GET请求.
     *
     * @param <T>      响应类型
     * @param request  http请求
     * @param resolver 响应数据解析器
     * @param callback 回调，可以为空
     * @return HTTP响应
     */
    public <T> HttpSession<T> doHttpGet(HttpRequest request, HttpResultResolver<T> resolver, HttpCallback<T> callback) {
        request.setRequestMethod(HttpRequest.HTTP_GET);
        return doHttpRequest(request, resolver, callback, false);
    }

    /**
     * 提交HTTPS POST请求.
     *
     * @param <T>      响应类型
     * @param request  http请求
     * @param resolver 响应数据解析器
     * @param callback 回调，可以为空
     * @return HTTP响应
     */
    public <T> HttpSession<T> doHttpsPost(HttpRequest request, HttpResultResolver<T> resolver, HttpCallback<T> callback) {
        request.setRequestMethod(HttpRequest.HTTP_POST);
        return doHttpRequest(request, resolver, callback, true);
    }

    /**
     * 提交HTTPS GET请求.
     *
     * @param <T>      响应类型
     * @param request  http请求
     * @param resolver 响应数据解析器
     * @param callback 回调，可以为空
     * @return HTTP响应
     */
    public <T> HttpSession<T> doHttpsGet(HttpRequest request, HttpResultResolver<T> resolver, HttpCallback<T> callback) {
        request.setRequestMethod(HttpRequest.HTTP_GET);
        return doHttpRequest(request, resolver, callback, true);
    }

    /**
     * 设置HTTPS的配置.
     * 这个配置用于配置HTTPS的证书验证和域名验证。
     *
     * @param httpsSSLConfig 配置
     */
    public void setHttpsSSLConfig(HttpsSSLConfig httpsSSLConfig) {
        mHttpsSSLConfig = httpsSSLConfig;
    }

    /**
     * 执行HTTP请求.
     *
     * @param request  请求对象
     * @param resolver 结果解析器
     * @param callback 回调
     * @param https    是否走HTTPS
     * @param <T>      请求结果
     * @return 请求会话
     */
    private <T> HttpSession<T> doHttpRequest(HttpRequest request, HttpResultResolver<T> resolver, HttpCallback<T> callback, boolean https) {
        final HttpRequestTask<T> requestTask = new HttpRequestTask<>(request, resolver, https);
        requestTask.setSSLConfig(mHttpsSSLConfig);
        final HttpSession<T> httpSession = new HttpSession<>();
        RequestSession<T> requestSession = new RequestSession<>(requestTask, httpSession);
        httpSession.setCallback(callback);
        httpSession.setAsyncTask(requestSession);
        requestSession.executeOnExecutor(mExecutor);
        return httpSession;
    }


    /**
     * 异步任务类构建请求的会话.
     *
     * @param <T>
     */
    private static class RequestSession<T> extends AsyncTask<Void, Bundle, HttpResponse<T>> {
        private final HttpRequestTask<T> mRequestTask;
        private final HttpSession<T> mHttpSession;

        private RequestSession(HttpRequestTask<T> requestTask, HttpSession<T> httpSession) {
            mRequestTask = requestTask;
            mHttpSession = httpSession;
        }

        @Override
        protected HttpResponse<T> doInBackground(Void... voids) {
            HttpResponse<T> httpResponse;
            try {
                httpResponse = mRequestTask.call();
            } catch (Exception e) {
                e.printStackTrace();
                httpResponse = new HttpResponse<>(HttpResponse.ERROR_CODE_REQUEST_FAILED, e.getMessage(), null);
            }
            return httpResponse;
        }

        @Override
        protected void onPostExecute(HttpResponse<T> result) {
            if (result != null) {
                mHttpSession.callOnRequestComplete(result);
            }
        }

        @Override
        protected final void onProgressUpdate(Bundle... values) {
            if (values != null && values.length > 0) {
                mHttpSession.callOnProgressUpdate(values[0]);
            }
        }

        @Override
        protected void onCancelled() {
            mHttpSession.callOnCancelled();
        }
    }
}
