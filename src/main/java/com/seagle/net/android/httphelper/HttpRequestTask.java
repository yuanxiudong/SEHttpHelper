package com.seagle.net.android.httphelper;

import android.os.Build;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;

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
import java.util.Map;
import java.util.concurrent.Callable;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

/**
 * Http request task.
 * Created by seagle on 2018/4/10.
 */

class HttpRequestTask<T> implements Callable<HttpResponse<T>> {

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
     * 请求地址
     */
    private final String mRequestUrl;

    /**
     * 请求是否包含文件上传
     */
    private boolean mUploadFile;

    private HttpsSSLConfig mSSLConfig;

    /**
     * request头和上传文件内容的分隔符
     */
    private final String mBoundary = "----" + SystemClock.uptimeMillis();

    HttpRequestTask(HttpRequest request, HttpResultResolver<T> resolver, boolean https) {
        mRequest = request;
        mResolver = resolver;
        mUploadFile = false;
        mHttps = https;
        String httpScheme = https ? "https" : "http";
        if (TextUtils.isEmpty(mRequest.getRequestPath())) {
            mRequestUrl = String.format("%s://%s", httpScheme, mRequest.getRequestHost());
        } else {
            mRequestUrl = String.format("%s://%s%s", httpScheme, mRequest.getRequestHost(), mRequest.getRequestPath());
        }
    }

    public void setSSLConfig(HttpsSSLConfig SSLConfig) {
        mSSLConfig = SSLConfig;
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
     * @throws Exception exception
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
        if (mHttps && mSSLConfig != null) {
            SSLSocketFactory sslSocketFactory = mSSLConfig.getSSLSocketFactory();
            if (sslSocketFactory != null) {
                ((HttpsURLConnection) mUrlConnection).setSSLSocketFactory(sslSocketFactory);
                ((HttpsURLConnection) mUrlConnection).setHostnameVerifier(mSSLConfig);
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

        URL url = new URL(requestUrl);
        mUrlConnection = (HttpURLConnection) url.openConnection();
        mUrlConnection.setRequestMethod(HttpRequest.HTTP_GET);
        mUrlConnection.setChunkedStreamingMode(0);
        mUrlConnection.setDoOutput(false);
        mUrlConnection.setDoInput(true);
        prepareConnectionSettings();
        prepareRequestHeaders();

        //处理HTTPS请求
        if (mHttps && mSSLConfig != null) {
            SSLSocketFactory sslSocketFactory = mSSLConfig.getSSLSocketFactory();
            if (sslSocketFactory != null) {
                ((HttpsURLConnection) mUrlConnection).setSSLSocketFactory(sslSocketFactory);
                ((HttpsURLConnection) mUrlConnection).setHostnameVerifier(mSSLConfig);
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
        if (settings.containsKey("requestTimeout")) {
            int newValue = Integer.parseInt(settings.get("requestTimeout"));
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
