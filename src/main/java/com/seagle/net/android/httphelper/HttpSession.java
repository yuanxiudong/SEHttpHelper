package com.seagle.net.android.httphelper;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * <h1>一句话功能说明</h1>
 * <p>详细功能描述</P>
 *
 * @author : xiudong.yuan@midea.com.cn
 * @date : 2016/6/27
 */
@SuppressWarnings("unused")
public final class HttpSession<T> {

    private static final String TAG = "HttpHelper";

    /**
     * 执行HTTP请求的任务
     */
    private AsyncTask<Void, Bundle, HttpResponse<T>> mAsyncTask;
    /**
     * 回调
     */
    private HttpCallback<T> mCallback;
    /**
     * 任务是否完成
     */
    private volatile boolean mCompleted;

    HttpSession() {
    }

    /**
     * 获取HTTP请求响应。
     * 在请求完成之前，调用这个接口会阻塞
     *
     * @return 响应
     */
    public HttpResponse<T> getResponse() {
        try {
            HttpResponse<T> httpResponse = mAsyncTask.get();
            mCompleted = true;
            return httpResponse;
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            HttpResponse<T> httpResponse = new HttpResponse<>(HttpResponse.ERROR_CODE_REQUEST_FAILED, e.getMessage(), null);
            mCompleted = true;
            return httpResponse;
        }
    }

    /**
     * 获取HTTP请求响应。
     * 在请求完成之前，调用这个接口会阻塞。
     * 如果获取响应超时不取消任务，还可以继续调用这个接口，回调还能正常收到回调。
     * 注意：就算取消了任务，仅仅代表任务不会在返回response对象了，对服务器提交的修改可能已经生效。
     *
     * @param timeout       超时时间
     * @param unit          超时时间单位
     * @param cancelRequest 获取响应结果超时是否取消任务
     * @return 响应
     */
    public HttpResponse<T> getResponse(int timeout, TimeUnit unit, boolean cancelRequest) {
        try {
            HttpResponse<T> httpResponse = mAsyncTask.get(timeout, TimeUnit.MILLISECONDS);
            mCompleted = true;
            return httpResponse;
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            HttpResponse<T> httpResponse = new HttpResponse<>(HttpResponse.ERROR_CODE_REQUEST_FAILED, e.getMessage(), null);
            mCompleted = true;
            return httpResponse;
        } catch (TimeoutException e) {
            e.printStackTrace();
            HttpResponse<T> httpResponse = new HttpResponse<>(HttpResponse.ERROR_CODE_TIME_OUT, e.getMessage(), null);
            if (cancelRequest) {
                mCompleted = true;
                mAsyncTask.cancel(true);
            }
            return httpResponse;
        }
    }

    /**
     * 取消任务
     * 注意：就算取消了任务，仅仅代表任务不会在返回response对象了，对服务器提交的修改可能已经生效。
     */
    @SuppressWarnings("unused")
    public final void cancelTask() {
        if (mAsyncTask != null && !mAsyncTask.isCancelled() && !mCompleted) {
            mAsyncTask.cancel(true);
        }
    }

    /**
     * 任务是否完成
     *
     * @return true or false
     */
    @SuppressWarnings("unused")
    public final boolean isCompleted() {
        return mCompleted;
    }

    /**
     * 判断任务是否取消
     *
     * @return true or false
     */
    public final boolean isCancelled() {
        return (mAsyncTask == null || mAsyncTask.isCancelled());
    }

    /**
     * 设置执行任务
     *
     * @param asyncTask 执行任务
     */
    final void setAsyncTask(AsyncTask<Void, Bundle, HttpResponse<T>> asyncTask) {
        mAsyncTask = asyncTask;
    }

    /**
     * 设置回调
     *
     * @param callback 回调
     */
    final void setCallback(HttpCallback<T> callback) {
        mCallback = callback;
    }

    /**
     * 请求完成
     *
     * @param response 响应
     */
    final void callOnRequestComplete(HttpResponse<T> response) {
        if (!mCompleted && mAsyncTask != null && !mAsyncTask.isCancelled() && response != null) {
            mCompleted = true;
            if (mCallback != null) {
                if (response.isSuccess()) {
                    mCallback.onResponseSuccess(response);
                } else {
                    mCallback.onResponseFailure(response);
                }
            }
        }
    }

    /**
     * 通知进度更新。
     * 运行在主线程
     *
     * @param progress 进度
     */
    final void callOnProgressUpdate(Bundle progress) {
        if (!mCompleted && mCallback != null) {
            Log.i(TAG, "callOnProgressUpdate");
            mCallback.onProgressUpdate(progress);
        }
    }

    /**
     * 通知任务取消。
     * 运行在主线程
     */
    final void callOnCancelled() {
        if (!mCompleted) {
            mCompleted = true;
            Log.i(TAG, "callOnCancelled");
        }
    }
}
