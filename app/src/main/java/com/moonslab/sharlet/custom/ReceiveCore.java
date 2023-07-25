package com.moonslab.sharlet.custom;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ReceiveCore {

    private final OkHttpClient client;
    private final Handler mainHandler;

    public ReceiveCore() {
        client = createOkHttpClient();
        mainHandler = new Handler(Looper.getMainLooper());
    }

    public OkHttpClient getClient(){
        return client;
    }

    private OkHttpClient createOkHttpClient() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        // Disable certificate validation
        try {
            TrustManager[] trustAllCerts = createTrustAllCerts();
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
            builder.sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0]);
            builder.hostnameVerifier((hostname, session) -> true);
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            e.printStackTrace();
        }
        return builder.build();
    }

    @SuppressLint("CustomX509TrustManager")
    private TrustManager[] createTrustAllCerts() {
        return new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }

                    @SuppressLint("TrustAllX509TrustManager")
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    }

                    @SuppressLint("TrustAllX509TrustManager")
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    }
                }
        };
    }

    public OkHttpClient downloadFile(String url, String savePath, String parameterP, final DownloadCallback callback) {
        RequestBody requestBody = new FormBody.Builder()
                .add("p", parameterP)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> callback.onFailure(e));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    runOnUiThread(() -> callback.onFailure(new IOException("Unexpected response: " + response)));
                    return;
                }
                try {
                    assert response.body() != null;
                    try (InputStream inputStream = response.body().byteStream();
                         OutputStream outputStream = Files.newOutputStream(Paths.get(savePath))) {
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        long totalBytesRead = 0;
                        long totalSize = response.body().contentLength();

                        while ((bytesRead = inputStream.read(buffer)) != -1) {
                            outputStream.write(buffer, 0, bytesRead);
                            totalBytesRead += bytesRead;

                            int progress = (int) (totalBytesRead * 100 / totalSize);
                            long finalTotalBytesRead = totalBytesRead;
                            runOnUiThread(() -> callback.onProgressUpdate(progress, finalTotalBytesRead, totalSize));
                        }

                        outputStream.flush();
                        runOnUiThread(callback::onSuccess);
                    }
                } catch (IOException e) {
                    runOnUiThread(() -> callback.onFailure(e));
                }
            }
        });
        return client;
    }

    private void runOnUiThread(Runnable runnable) {
        mainHandler.post(runnable);
    }

    public interface DownloadCallback {
        void onProgressUpdate(int progress, long totalBytesRead, long totalSize);

        void onSuccess();

        void onFailure(Exception e);
    }
}
