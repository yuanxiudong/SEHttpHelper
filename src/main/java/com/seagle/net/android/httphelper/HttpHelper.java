package com.seagle.net.android.httphelper;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.TextUtils;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.GeneralSecurityException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * <h1>HTTP请求协助类.</h1>
 * HttpHelper提供了一套HTTP请求的接口，包括HTTP和HTTPS以及GET和POST提交方式。
 * HttpHelper同时支持同步和异步。每一次提交返回一个HttpSession。
 * 同时每个提交的接口也有一个callback接口。
 * 同步调用：HttpSession.getResponse(),可以将callback置为空。
 * 异步调用：通过callback的回调得到请求的响应
 *
 * @author : xiudong.yuan@midea.com.cn
 * @date : 2016/4/23
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
     * @param request  http请求
     * @param resolver 响应数据
     * @return HTTP相应
     */
    public <T> HttpSession<T> doHttpPost(HttpRequest request, HttpResultResolver<T> resolver, HttpCallback<T> callback) {
        request.setRequestMethod(HttpRequest.HTTP_POST);
        return doHttpRequest(request, resolver, callback, false);
    }

    /**
     * 提交HTTP GET请求.
     *
     * @param request http请求
     * @param <T>     响应类型
     * @return HTTP相应
     */
    public <T> HttpSession<T> doHttpGet(HttpRequest request, HttpResultResolver<T> resolver, HttpCallback<T> callback) {
        request.setRequestMethod(HttpRequest.HTTP_GET);
        return doHttpRequest(request, resolver, callback, false);
    }

    /**
     * 提交HTTPS POST请求.
     *
     * @param request http请求
     * @param <T>     响应类型
     * @return HTTP相应
     */
    public <T> HttpSession<T> doHttpsPost(HttpRequest request, HttpResultResolver<T> resolver, HttpCallback<T> callback) {
        request.setRequestMethod(HttpRequest.HTTP_POST);
        return doHttpRequest(request, resolver, callback, true);
    }

    /**
     * 提交HTTPS GET请求.
     *
     * @param request http请求
     * @param <T>     响应类型
     * @return HTTP相应
     */
    public <T> HttpSession<T> doHttpsGet(HttpRequest request, HttpResultResolver<T> resolver, HttpCallback<T> callback) {
        request.setRequestMethod(HttpRequest.HTTP_GET);
        return doHttpRequest(request, resolver, callback, true);
    }

    /**
     * 设置默认用户交互
     *
     * @param allows true
     */
    @SuppressWarnings("unused")
    public void setDefaultAllowUserInteraction(boolean allows) {
        HttpURLConnection.setDefaultAllowUserInteraction(allows);
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
        final HttpSession<T> httpSession = new HttpSession<>();
        AsyncTask<Void, Bundle, HttpResponse<T>> asyncTask = new AsyncTask<Void, Bundle, HttpResponse<T>>() {
            @Override
            protected HttpResponse<T> doInBackground(Void... params) {
                HttpResponse<T> httpResponse;
                try {
                    httpResponse = requestTask.call();
                } catch (Exception e) {
                    e.printStackTrace();
                    httpResponse = new HttpResponse<>(HttpResponse.ERROR_CODE_REQUEST_FAILED, e.getMessage(), null);
                }
                return isCancelled() ? null : httpResponse;
            }

            @Override
            protected void onPostExecute(HttpResponse<T> result) {
                if (result != null) {
                    httpSession.callOnRequestComplete(result);
                }
            }

            @Override
            protected final void onProgressUpdate(Bundle... values) {
                if (values != null && values.length > 0) {
                    httpSession.callOnProgressUpdate(values[0]);
                }
            }

            @Override
            protected void onCancelled() {
                httpSession.callOnCancelled();
            }
        };
        httpSession.setAsyncTask(asyncTask);
        httpSession.setCallback(callback);
        asyncTask.executeOnExecutor(mExecutor);
        return httpSession;
    }


    /**
     * 发送请求以及解析响应消息
     */
    private class HttpRequestTask<T> implements Callable<HttpResponse<T>> {

        /**
         * 请求
         */
        private final HttpRequest mRequest;

        /**
         * HTTP连接
         */
        private HttpURLConnection mUrlConnection;

        /**
         * 数据处理类
         */
        private HttpResultResolver<T> mResolver;

        /**
         * 是否是HTTPS请求
         */
        private final boolean mHttps;

        /**
         * 协议头
         */
        private final String mHttpScheme;

        /**
         * 请求地址
         */
        private final String mRequestUrl;

        /**
         * 请求是否包含文件上传
         */
        private boolean mUploadFile;

        /**
         * request头和上传文件内容的分隔符
         */
        private final String mBoundary = "----" + SystemClock.uptimeMillis();

        HttpRequestTask(HttpRequest request, HttpResultResolver<T> resolver, boolean https) {
            mRequest = request;
            mResolver = resolver;
            mHttps = https;
            mHttpScheme = https ? "https" : "http";
            mUploadFile = false;
            if (TextUtils.isEmpty(mRequest.getRequestPath())) {
                mRequestUrl = String.format("%s://%s", mHttpScheme, mRequest.getRequestHost());
            } else {
                mRequestUrl = String.format("%s://%s%s", mHttpScheme, mRequest.getRequestHost(), mRequest.getRequestPath());
            }
        }

        @Override
        public HttpResponse<T> call() throws Exception {
            if (HttpRequest.HTTP_GET.equalsIgnoreCase(mRequest.getRequestMethod())) {
                return doGetRequest();
            } else if (HttpRequest.HTTP_POST.equalsIgnoreCase(mRequest.getRequestMethod())) {
                return doPostRequest();
            } else {
                throw new IllegalArgumentException("Not support http method!");
            }
        }

        /**
         * 处理HTTP POST请求
         *
         * @return HTTP响应
         * @throws Exception
         */
        private HttpResponse<T> doPostRequest() throws Exception {
            URL url = new URL(mRequestUrl);
            mUrlConnection = (HttpURLConnection) url.openConnection();
            mUrlConnection.setRequestMethod(HttpRequest.HTTP_POST);
            mUrlConnection.setChunkedStreamingMode(0);
            mUrlConnection.setDoOutput(true);
            mUrlConnection.setDoInput(true);
            mUrlConnection.setReadTimeout(mRequest.getRequestTimeout());
            mUrlConnection.setConnectTimeout(mRequest.getRequestTimeout());
            prepareConnectionSettings();
            prepareRequestHeaders();

            //处理HTTPS请求
            if (mHttps) {
                if (mHttpsSSLConfig == null) {
                    mHttpsSSLConfig = DEFAULT_HTTPS_CONFIG;
                }
                SSLSocketFactory sslSocketFactory = mHttpsSSLConfig.getSSLSocketFactory();
                if (sslSocketFactory != null) {
                    ((HttpsURLConnection) mUrlConnection).setSSLSocketFactory(sslSocketFactory);
                    ((HttpsURLConnection) mUrlConnection).setHostnameVerifier(mHttpsSSLConfig);
                }
            }

            try {
                mUrlConnection.connect();
                DataOutputStream outStream = new DataOutputStream(mUrlConnection.getOutputStream());
                try {
                    if (mUploadFile) {
                        writeFileParams(outStream);
                    } else {
                        writePostParams(outStream);
                    }
                } finally {
                    try {
                        outStream.flush();
                        outStream.close();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }

                //获取响应
                int code = mUrlConnection.getResponseCode();
                String message = mUrlConnection.getResponseMessage();
                String respRawData = null;
                if (HttpURLConnection.HTTP_OK == code) {
                    respRawData = readHttpData();
                }
                return buildResponse(code, message, respRawData);
            } finally {
                mUrlConnection.disconnect();
            }
        }

        /**
         * 处理HTTP GET请求
         *
         * @return HTTP响应
         * @throws Exception
         */
        private HttpResponse<T> doGetRequest() throws Exception {
            String dataParams = prepareParams();
            String requestUrl;
            if (!TextUtils.isEmpty(dataParams)) {
                requestUrl = String.format("%s?%s", mRequestUrl, dataParams);
            } else {
                requestUrl = mRequestUrl;
            }

            URL url = new URL(URLEncoder.encode(requestUrl, "UTF-8"));
            mUrlConnection = (HttpURLConnection) url.openConnection();
            mUrlConnection.setRequestMethod(HttpRequest.HTTP_GET);
            mUrlConnection.setChunkedStreamingMode(0);
            mUrlConnection.setDoOutput(false);
            mUrlConnection.setDoInput(true);
            prepareConnectionSettings();
            prepareRequestHeaders();

            //处理HTTPS请求
            if (mHttps) {
                if (mHttpsSSLConfig == null) {
                    mHttpsSSLConfig = DEFAULT_HTTPS_CONFIG;
                }
                SSLSocketFactory sslSocketFactory = mHttpsSSLConfig.getSSLSocketFactory();
                if (sslSocketFactory != null) {
                    ((HttpsURLConnection) mUrlConnection).setSSLSocketFactory(sslSocketFactory);
                    ((HttpsURLConnection) mUrlConnection).setHostnameVerifier(mHttpsSSLConfig);
                }
            }

            mUrlConnection.connect();
            try {
                int code = mUrlConnection.getResponseCode();
                String message = mUrlConnection.getResponseMessage();
                mUrlConnection.getContent();
                String respRawData = null;
                if (HttpsURLConnection.HTTP_OK == code) {
                    respRawData = readHttpData();
                }
                return buildResponse(code, message, respRawData);
            } finally {
                mUrlConnection.disconnect();
            }
        }

        /**
         * 读取响应数据
         *
         * @return 数据
         * @throws IOException
         */
        private String readHttpData() throws IOException {
            StringBuilder respRawDataBuild = new StringBuilder();
            BufferedReader reader;
            try {
                reader = new BufferedReader(new InputStreamReader(mUrlConnection.getInputStream()));
            } catch (Exception ex) {
                reader = new BufferedReader(new InputStreamReader(mUrlConnection.getErrorStream()));
            }
            String tmpStr;
            while ((tmpStr = reader.readLine()) != null) {
                respRawDataBuild.append(tmpStr);
            }
            return respRawDataBuild.toString();
        }


        /**
         * 构建响应对象
         *
         * @param code        HTTP响应吗
         * @param message     响应消息
         * @param respRawData 响应数据
         * @return 响应对象
         */
        private HttpResponse<T> buildResponse(int code, String message, String respRawData) {
            if (!TextUtils.isEmpty(respRawData) && mResolver != null) {
                try {
                    T result = mResolver.resolverHttpRespData(respRawData);
                    int errCode = mResolver.isSuccess() ? HttpResponse.SUCCESS : HttpResponse.ERROR_CODE_SERVER;
                    HttpResponse<T> response = new HttpResponse<>(errCode, mResolver.getErrorMsg(), respRawData);
                    response.setResultCode(mResolver.getErrorCode());
                    if (result != null) {
                        response.setResult(result);
                    }
                    return response;
                } catch (Exception ex) {
                    return new HttpResponse<>(HttpResponse.ERROR_CODE_RESOLVE_FAILED, ex.getMessage(), respRawData);
                }
            } else {
                if (HttpsURLConnection.HTTP_OK == code) {
                    return new HttpResponse<>(HttpResponse.SUCCESS, message, respRawData);
                } else {
                    HttpResponse<T> response = new HttpResponse<>(HttpResponse.ERROR_CODE_HTTP, message, respRawData);
                    response.setResultCode(code);
                    return response;
                }
            }
        }

        /**
         * 准备连接参数配置
         */
        private void prepareConnectionSettings() {
            Map<String, String> settings = mRequest.getConnectSettings();
            if (settings.containsKey(HttpRequest.HttpSettings.ALLOW_USER_INTERACTION)) {
                boolean newValue = Boolean.parseBoolean(settings.get(HttpRequest.HttpSettings.ALLOW_USER_INTERACTION));
                mUrlConnection.setAllowUserInteraction(newValue);
            }

            if (settings.containsKey(HttpRequest.HttpSettings.DO_INPUT)) {
                boolean newValue = Boolean.parseBoolean(settings.get(HttpRequest.HttpSettings.DO_INPUT));
                mUrlConnection.setDoInput(newValue);
            }

            if (settings.containsKey(HttpRequest.HttpSettings.DO_OUTPUT)) {
                boolean newValue = Boolean.parseBoolean(settings.get(HttpRequest.HttpSettings.DO_OUTPUT));
                mUrlConnection.setDoOutput(newValue);
            }

            if (settings.containsKey(HttpRequest.HttpSettings.IF_MODIFIED_SINCE)) {
                long newValue = Long.parseLong(settings.get(HttpRequest.HttpSettings.IF_MODIFIED_SINCE));
                mUrlConnection.setIfModifiedSince(newValue);
            }

            if (settings.containsKey(HttpRequest.HttpSettings.USE_CACHES)) {
                boolean newValue = Boolean.parseBoolean(settings.get(HttpRequest.HttpSettings.USE_CACHES));
                mUrlConnection.setUseCaches(newValue);
            }

            if (settings.containsKey(HttpRequest.HttpSettings.REQUEST_TIME_OUT)) {
                int newValue = Integer.parseInt(settings.get(HttpRequest.HttpSettings.REQUEST_TIME_OUT));
                mUrlConnection.setConnectTimeout(newValue);
                mUrlConnection.setReadTimeout(newValue);
            }
        }

        /**
         * 准备请求头部
         */
        private void prepareRequestHeaders() {
            Map<String, String> headers = mRequest.getRequestHeaders();
            for (String key : headers.keySet()) {
                mUrlConnection.setRequestProperty(key, headers.get(key));
            }
            //解决OkHttp的EOFException异常
            if (Build.VERSION.SDK_INT > 14 && Build.VERSION.SDK_INT < 19) {
                mUrlConnection.setRequestProperty("Connection", "close");
            } else {
                mUrlConnection.setRequestProperty("Connection", "Keep-Alive");
            }

            //设置文件上传参数,仅限于POST请求
            Map<String, File> fileParams = mRequest.getRequestFileParams();
            mUploadFile = fileParams != null && !fileParams.isEmpty() && HttpRequest.HTTP_POST.equalsIgnoreCase(mRequest.getRequestMethod());
            if (mUploadFile) {
                mUrlConnection.setRequestProperty("Connection", "Keep-Alive");
                mUrlConnection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + mBoundary);
            }
        }

        /**
         * 准备请求参数
         *
         * @return 参数格式化串
         */
        private String prepareParams() throws UnsupportedEncodingException {
            Map<String, String> params = mRequest.getRequestParams();
            if (params == null || params.isEmpty()) {
                return null;
            }
            StringBuilder paramsStrBuilder = new StringBuilder();
            for (String key : params.keySet()) {
                String encodeKey = URLEncoder.encode(key, "UTF-8");
                String value = URLEncoder.encode(params.get(key), "UTF-8");
                paramsStrBuilder.append(encodeKey).append("=").append(value).append("&");
            }
            String paramsStr = paramsStrBuilder.toString();
            return paramsStr.substring(0, paramsStrBuilder.length() - 1);
        }

        /**
         * 提交POST普通请求参数.
         *
         * @param outStream 流
         * @throws IOException 输出异常
         */
        private void writePostParams(DataOutputStream outStream) throws IOException {
            String dataParams = prepareParams();
            if (!TextUtils.isEmpty(dataParams)) {
                outStream.write(dataParams.getBytes());
                outStream.flush();
            }
        }

        /**
         * 提交POST文件上传请求参数.
         * POST上传文件有两个步骤，先提交参数，然后上传文件.<br>
         * 如果是文件上传，则每一个参数和每一个文件之间要有间隔。
         *
         * @param outStream 流
         * @throws IOException 输出异常
         */
        private void writeFileParams(DataOutputStream outStream) throws IOException {

            //写参数
            Map<String, String> params = mRequest.getRequestParams();
            if (params != null && !params.isEmpty()) {
                for (String key : params.keySet()) {
                    String value = params.get(key);
                    StringBuilder strBuilder = new StringBuilder();
                    strBuilder.append("--").append(mBoundary).append("\r\n");
                    strBuilder.append("Content-Disposition: form-data; name=\"").append(key).append("\"\r\n");
                    strBuilder.append("Content-Type:text/plain;charset=UTF-8\r\n\r\n");
                    strBuilder.append(value).append("\r\n");
                    outStream.write(strBuilder.toString().getBytes());
                    outStream.flush();
                }
            }

            //写文件
            Map<String, File> fileParams = mRequest.getRequestFileParams();
            if (fileParams != null && !fileParams.isEmpty()) {
                for (String key : fileParams.keySet()) {
                    File file = fileParams.get(key);

                    //写文件头部信息
                    StringBuilder strBuilder = new StringBuilder();
                    String filename = file.getName();
                    String contentType = getContextType(filename);
                    strBuilder.append("--").append(mBoundary).append("\r\n");
                    strBuilder.append("Content-Disposition: form-data; name=\"").append(key).append("\"; filename=\"").append(filename).append("\"\r\n");
                    strBuilder.append("Content-Type:" + contentType + "\r\n");
                    strBuilder.append("Content-Transfer-Encoding: binary" + "\r\n\r\n");
                    outStream.write(strBuilder.toString().getBytes());
                    outStream.flush();

                    //写文件
                    DataInputStream in = new DataInputStream(new FileInputStream(file));
                    int bytes;
                    byte[] bufferOut = new byte[1024];
                    while ((bytes = in.read(bufferOut)) != -1) {
                        outStream.write(bufferOut, 0, bytes);
                    }
                    in.close();
                }
            }

            //写尾部
            outStream.flush();
            outStream.write(("\r\n--" + mBoundary + "--\r\n").getBytes());
            outStream.flush();
        }

        /**
         * 获取文件的类型
         *
         * @param fileName 文件名称
         * @return 类型
         */
        private String getContextType(String fileName) {
            if (fileName.endsWith("jpg")) {
                return "image/jpeg";
            } else if (fileName.endsWith("jpeg")) {
                return "image/x-jpg";
            } else if (fileName.endsWith("png")) {
                return "image/x-png";
            }
            return "application/octet-stream";
        }
    }


    /**
     * 默认HTTPS配置.
     * 信任所有主机-对于任何证书都不做检查.<br>
     * 不验证服务器名称.
     */
    private static final HttpsSSLConfig DEFAULT_HTTPS_CONFIG = new HttpsSSLConfig() {

        @Override
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }

        @Override
        public SSLSocketFactory getSSLSocketFactory() {
            SSLContext sslContext = getDefaultSSLContext();
            if (sslContext != null) {
                return sslContext.getSocketFactory();
            }
            return null;
        }

        /**
         * 默认信任所有主机。
         *
         * @return SSLContext
         */
        private SSLContext getDefaultSSLContext() {
            SSLContext sslContext = null;
            try {
                sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, new TrustManager[]{TRUST_ALL_X509_Manager}, null);
            } catch (GeneralSecurityException e) {
                e.printStackTrace();
            }
            return sslContext;
        }


        /**
         * 信任所有主机-对于任何证书都不做检查
         */
        @SuppressLint("TrustAllX509TrustManager")
        private final TrustManager TRUST_ALL_X509_Manager = new X509TrustManager() {

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[]{};
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType)
                    throws CertificateException {
            }

            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType)
                    throws CertificateException {
            }
        };
    };
}
