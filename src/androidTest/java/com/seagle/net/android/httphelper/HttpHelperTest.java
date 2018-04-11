package com.seagle.net.android.httphelper;


import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.*;

/**
 * HttpHelper test unit.
 * Created by seagle on 2018/4/10.
 */
@RunWith(AndroidJUnit4.class)
public class HttpHelperTest {

    @Test
    public void doHttpGet() throws Exception {
        long time = System.currentTimeMillis();
        HttpRequest request = new HttpRequest("ip.taobao.com", "/service/getIpInfo.php");
        request.addRequestParam("ip", "210.21.220.218");
        HttpHelper httpHelper = new HttpHelper();
        HttpSession session = httpHelper.doHttpGet(request, null, null);
        Log.i("HttpHelperTest",session.getResponse().getMessage());
        HttpResponse response = session.getResponse();
        Log.i("HttpHelperTest","Coast time: "+(System.currentTimeMillis() - time));
        assertTrue(response.isSuccess());
    }

    @Test
    public void doHttpPost() throws Exception {
        HttpRequest request = new HttpRequest("ip.taobao.com", "/service/getIpInfo.php");
        request.addRequestParam("ip", "210.21.220.218");
        HttpHelper httpHelper = new HttpHelper();
        HttpSession session = httpHelper.doHttpPost(request, null, null);
        System.out.println(session.getResponse().getMessage());
        HttpResponse response = session.getResponse();
        assertTrue(response.isSuccess());
    }

    @Test
    public void doHttpsGet() throws Exception {
        HttpRequest request = new HttpRequest("ip.taobao.com", "/service/getIpInfo.php");
        request.addRequestParam("ip", "210.21.220.218");
        HttpHelper httpHelper = new HttpHelper();
        HttpSession session = httpHelper.doHttpGet(request, null, null);
        System.out.println(session.getResponse().getMessage());
        HttpResponse response = session.getResponse();
        assertTrue(response.isSuccess());
    }

    @Test
    public void doHttpsPost() throws Exception {
        HttpRequest request = new HttpRequest("ip.taobao.com", "/service/getIpInfo.php");
        request.addRequestParam("ip", "210.21.220.218");
        HttpHelper httpHelper = new HttpHelper();
        HttpSession session = httpHelper.doHttpGet(request, null, null);
        System.out.println(session.getResponse().getMessage());
        HttpResponse response = session.getResponse();
        assertTrue(response.isSuccess());
    }

    @Test
    public void doAsyncHttpGet() throws Exception {
        HttpRequest request = new HttpRequest("ip.taobao.com", "/service/getIpInfo.php");
        request.addRequestParam("ip", "210.21.220.218");
        HttpHelper httpHelper = new HttpHelper();
        final CountDownLatch latch = new CountDownLatch(1);
        HttpSession session = httpHelper.doHttpGet(request, null, new HttpCallback() {
            @Override
            protected void onResponseSuccess(HttpResponse response) {
                assertTrue(response.isSuccess());
                Log.i("HttpHelperTest",response.getMessage());
                latch.countDown();
            }

            @Override
            protected void onResponseFailure(HttpResponse response) {
                assertTrue(response.isSuccess());
                Log.i("HttpHelperTest",response.getMessage());
                latch.countDown();
            }
        });
        latch.await();
    }
}