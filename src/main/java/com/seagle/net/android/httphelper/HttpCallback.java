package com.seagle.net.android.httphelper;

import android.os.Bundle;

/**
 * HTTP请求回调.
 *
 * @param <T> 结果类型
 * @author : xiudong.yuan@midea.com.cn
 * @date : 2016/6/27
 */
public abstract class HttpCallback<T> {
    /**
     * 请求响应成功.
     *
     * @param response 响应
     */
    protected abstract void onResponseSuccess(HttpResponse<T> response);

    /**
     * 请求响应失败.
     *
     * @param response 响应
     */
    protected void onResponseFailure(HttpResponse<T> response) {

    }

    /**
     * 进度更新.
     *
     * @param progress 进度
     */
    @SuppressWarnings("unused")
    protected void onProgressUpdate(Bundle progress) {

    }
}
