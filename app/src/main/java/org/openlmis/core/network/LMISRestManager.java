/*
 * This program is part of the OpenLMIS logistics management information
 * system platform software.
 *
 * Copyright © 2015 ThoughtWorks, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version. This program is distributed in the
 * hope that it will be useful, but WITHOUT ANY WARRANTY; without even the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details. You should
 * have received a copy of the GNU Affero General Public License along with
 * this program. If not, see http://www.gnu.org/licenses. For additional
 * information contact info@OpenLMIS.org
 */

package org.openlmis.core.network;


import android.support.annotation.NonNull;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonSerializer;
import com.squareup.okhttp.Credentials;
import com.squareup.okhttp.OkHttpClient;

import org.openlmis.core.LMISApp;
import org.openlmis.core.R;
import org.openlmis.core.exceptions.LMISException;
import org.openlmis.core.exceptions.NetWorkException;
import org.openlmis.core.exceptions.SyncServerException;
import org.openlmis.core.exceptions.UnauthorizedException;
import org.openlmis.core.manager.UserInfoMgr;
import org.openlmis.core.model.Product;
import org.openlmis.core.model.RnRForm;
import org.openlmis.core.model.StockCard;
import org.openlmis.core.model.User;
import org.openlmis.core.network.adapter.ProductAdapter;
import org.openlmis.core.network.adapter.RnrFormAdapter;
import org.openlmis.core.network.adapter.StockCardAdapter;
import org.openlmis.core.network.model.DataErrorResponse;

import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManagerFactory;

import lombok.Data;
import retrofit.ErrorHandler;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.OkClient;
import retrofit.client.Response;
import retrofit.converter.GsonConverter;

import static javax.net.ssl.HttpsURLConnection.getDefaultHostnameVerifier;

public class LMISRestManager {

    public String END_POINT;

    protected RestAdapter restAdapter;
    protected LMISRestApi lmisRestApi;
    private String hostName;

    public LMISRestManager() {
        END_POINT = LMISApp.getContext().getResources().getString(R.string.server_base_url);
        try {
            hostName = new URL(END_POINT).getHost();
        } catch (Exception e) {
            new LMISException(e).reportToFabric();
        }

        RequestInterceptor requestInterceptor = new RequestInterceptor() {
            @Override
            public void intercept(RequestFacade request) {
                User user = UserInfoMgr.getInstance().getUser();
                if (user != null) {
                    String basic = Credentials.basic(user.getUsername(), user.getPassword());
                    request.addHeader("Authorization", basic);
                }
            }
        };

        restAdapter = new RestAdapter.Builder()
                .setEndpoint(END_POINT)
                .setErrorHandler(new APIErrorHandler())
                .setLogLevel(RestAdapter.LogLevel.FULL)
                .setRequestInterceptor(requestInterceptor)
                .setConverter(registerTypeAdapter())
                .setClient(new OkClient(getSSLClient()))
                .build();

        lmisRestApi = restAdapter.create(LMISRestApi.class);
    }

    public OkHttpClient getSSLClient() {
        OkHttpClient client = getOkHttpClient();

        // loading CAs from an InputStream
        try {
            SSLContext sslContext = getSslContext(getCertificate());
            client.setSslSocketFactory(sslContext.getSocketFactory());
            client.setHostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    if (hostname.equals(hostName)) {
                        return true;
                    }
                    return getDefaultHostnameVerifier().verify(hostname, session);
                }
            });
        } catch (Exception e) {
            new LMISException(e).reportToFabric();
        }

        return client;
    }

    private SSLContext getSslContext(Certificate ca) throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException, KeyManagementException {
        // creating a KeyStore containing our trusted CAs
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, null);
        keyStore.setCertificateEntry("ca", ca);

        // creating a TrustManager that trusts the CAs in our KeyStore
        String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
        tmf.init(keyStore);

        // creating an SSLSocketFactory that uses our TrustManager
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, tmf.getTrustManagers(), null);
        return sslContext;
    }

    private Certificate getCertificate() throws CertificateException, IOException {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        InputStream cert = LMISApp.getContext().getResources().openRawResource(R.raw.ssl_cert);
        Certificate ca = null;
        try {
            ca = cf.generateCertificate(cert);
            System.out.println("ca=" + ((X509Certificate) ca).getSubjectDN());
        } catch (Exception e) {
            new LMISException(e).reportToFabric();
        } finally {
            cert.close();
        }
        return ca;
    }

    @NonNull
    private OkHttpClient getOkHttpClient() {
        OkHttpClient client = new OkHttpClient();

        //set timeout to 1 minutes
        client.setReadTimeout(1, TimeUnit.MINUTES);
        client.setConnectTimeout(15, TimeUnit.SECONDS);
        client.setWriteTimeout(30, TimeUnit.SECONDS);
        return client;
    }


    public LMISRestApi getLmisRestApi() {
        return lmisRestApi;
    }

    private GsonConverter registerTypeAdapter() {
        return new GsonConverter(new GsonBuilder()
                .registerTypeAdapter(RnRForm.class, getTypeAdapter())
                .registerTypeAdapter(Product.class, new ProductAdapter())
                .registerTypeAdapter(StockCard.class, new StockCardAdapter())
                .create());
    }

    @NonNull
    private JsonSerializer getTypeAdapter() {
        return new RnrFormAdapter();
    }

    @Data
    public static class UserResponse {
        User userInformation;
    }

    class APIErrorHandler implements ErrorHandler {
        @Override
        public Throwable handleError(RetrofitError cause) {
            Response r = cause.getResponse();
            if (r != null && r.getStatus() == 401) {
                return new UnauthorizedException(cause);
            }

            if (r != null && r.getStatus() == 400) {
                return new SyncServerException(((DataErrorResponse) cause.getBodyAs(DataErrorResponse.class)).getError());
            }
            if (r != null && r.getStatus() == 500) {
                return new SyncServerException(LMISApp.getContext().getString(R.string.sync_server_error));
            }
            if (r == null && cause.getCause() instanceof ConnectException) {
                return new NetWorkException();
            }
            return cause;
        }
    }

}
