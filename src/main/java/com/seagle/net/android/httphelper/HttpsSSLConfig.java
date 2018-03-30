package com.seagle.net.android.httphelper;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

/**
 * <h1>一句话功能说明.</h1>
 * <p>详细功能描述
 *
 * @author : xiudong.yuan@midea.com.cn
 * @date : 2016/7/14
 */
public abstract class HttpsSSLConfig implements HostnameVerifier {

    /**
     * 获取SSL工厂
     *
     * @return SSLSocketFactory
     */
    public abstract SSLSocketFactory getSSLSocketFactory();

    /**
     * 支持指定load-der.crt证书验证
     *
     * @return SSLContext
     */
    protected final SSLContext getSSLContext(String certificateName, Certificate certificate, TrustManager[] trustManagers) {
        SSLContext sslContext;
        try {
            sslContext = SSLContext.getInstance("TLS");
            if (trustManagers == null) {
                KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
                keyStore.load(null, null);
                keyStore.setCertificateEntry(certificateName, certificate);
                String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
                TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(tmfAlgorithm);
                trustManagerFactory.init(keyStore);
                sslContext.init(null, trustManagerFactory.getTrustManagers(), null);
            } else {
                sslContext.init(null, trustManagers, null);
            }
            return sslContext;
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
