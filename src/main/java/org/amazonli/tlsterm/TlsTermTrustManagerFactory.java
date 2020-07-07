package org.amazonli.tlsterm;

import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.ssl.util.SimpleTrustManagerFactory;
import io.netty.util.internal.EmptyArrays;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import javax.net.ssl.ManagerFactoryParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.security.KeyStore;
import java.security.cert.X509Certificate;

public class TlsTermTrustManagerFactory extends SimpleTrustManagerFactory {
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(InsecureTrustManagerFactory.class);
    public static final TrustManagerFactory INSTANCE = new TlsTermTrustManagerFactory();
    private static final TrustManager tm = new X509TrustManager() {
        public void checkClientTrusted(X509Certificate[] chain, String s) {
            TlsTermTrustManagerFactory.logger.info("Accepting a client certificate: " + chain[0].getSubjectDN());
        }

        public void checkServerTrusted(X509Certificate[] chain, String s) {
            throw new NotImplementedException();
        }

        public X509Certificate[] getAcceptedIssuers() {
            return EmptyArrays.EMPTY_X509_CERTIFICATES;
        }
    };

    private TlsTermTrustManagerFactory() {
    }

    @Override
    protected void engineInit(KeyStore keyStore) throws Exception {
    }

    @Override
    protected void engineInit(ManagerFactoryParameters managerFactoryParameters) throws Exception {
    }

    @Override
    protected TrustManager[] engineGetTrustManagers() {
        return new TrustManager[]{tm};
    }
}