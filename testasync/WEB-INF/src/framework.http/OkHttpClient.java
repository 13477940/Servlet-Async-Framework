package framework.http;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import framework.observer.Bundle;
import framework.observer.Handler;
import framework.observer.Message;
import framework.random.RandomServiceStatic;
import okhttp3.*;

import javax.net.ssl.*;
import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.cert.CertificateException;
import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 替代 JDK HttpClient 方案
 *
 * https://mvnrepository.com/artifact/org.jetbrains.kotlin/kotlin-stdlib
 * https://mvnrepository.com/artifact/com.squareup.okhttp3/okhttp
 * https://mvnrepository.com/artifact/com.squareup.okio/okio
 */
public class OkHttpClient {

    private String url;
    private String getParamsStr;
    private final HashMap<String, String> headers;
    private final LinkedHashMap<String, String> parameters;
    private final LinkedHashMap<String, File> files;
    private final boolean alwaysDownload;
    private String tempDirPath; // 自定義下載暫存資料夾路徑
    // private String tempDirName = "http_client_temp";
    private final boolean tempFileDeleteOnExit;
    private final Duration conn_timeout;

    private String resp_encoding = StandardCharsets.UTF_8.name();

    // init OkHttpClient
    private OkHttpClient(
            String url,
            HashMap<String, String> headers,
            LinkedHashMap<String, String> parameters,
            LinkedHashMap<String, File> files,
            Boolean alwaysDownload,
            Boolean tempFileDeleteOnExit,
            String tempDirPath,
            String tempDirName,
            Boolean keepUrlSearchParams,
            Duration conn_timeout,
            String resp_encoding
    ) {
        this.url = null;
        {
            // 是否具有 UrlSearchParams
            if(url.contains("?")) {
                String[] tmp = url.split("\\?");
                url = tmp[0];
                getParamsStr = tmp[1];
            }
            // 是否為正式的 Url Path
            StringBuilder sbd = new StringBuilder();
            if(url.contains("://")) {
                String[] tmpSch = url.split("://");
                sbd.append(tmpSch[0]);
                sbd.append("://");
                String[] tmp = tmpSch[1].split("/");
                boolean isDomain = true;
                for(String str : tmp) {
                    if(isDomain) {
                        // example http://localhost:8080/testasync/index
                        if(str.contains(":")) {
                            String[] pStr = str.split(":");
                            String pDomain = pStr[0];
                            String pNumber = pStr[1];
                            sbd.append(URLEncoder.encode(pDomain, StandardCharsets.UTF_8));
                            sbd.append(":").append(pNumber);
                        } else {
                            sbd.append(URLEncoder.encode(str, StandardCharsets.UTF_8));
                        }
                        isDomain = false;
                    } else {
                        sbd.append("/").append(URLEncoder.encode(str, StandardCharsets.UTF_8));
                    }
                }
            } else {
                sbd.append("http://"); // default scheme
                String[] tmp = url.split("/");
                boolean isDomain = true;
                for(String str : tmp) {
                    if(isDomain) {
                        sbd.append(URLEncoder.encode(str, StandardCharsets.UTF_8));
                        isDomain = false;
                    } else {
                        sbd.append("/").append(URLEncoder.encode(str, StandardCharsets.UTF_8));
                    }
                }
            }
            // proc getParamsStr
            if(Objects.requireNonNullElse(keepUrlSearchParams, false)) {
                if(null != getParamsStr && getParamsStr.length() > 0) {
                    this.url = sbd.toString() + "?" + getParamsStr;
                } else {
                    this.url = sbd.toString();
                }
            } else {
                this.url = sbd.toString();
            }
        }
        this.headers = headers;
        this.parameters = parameters;
        this.files = files;
        this.alwaysDownload = Objects.requireNonNullElse(alwaysDownload, false);
        this.tempFileDeleteOnExit = Objects.requireNonNullElse(tempFileDeleteOnExit, true);
        this.conn_timeout = conn_timeout;
        // 下載暫存資料夾
        {
            this.tempDirPath = tempDirPath;
            if(null != tempDirName) {
                this.tempDirPath = this.tempDirPath + tempDirName;
            }
        }
        // resp_encoding
        {
            if(null != resp_encoding) {
                this.resp_encoding = resp_encoding;
            }
        }
    }

    private static okhttp3.OkHttpClient getUnsafeOkHttpClient() {
        try {
            // Create a trust manager that does not validate certificate chains
            final TrustManager[] trustAllCerts = new TrustManager[] {
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                        }

                        @Override
                        public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                        }

                        @Override
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return new java.security.cert.X509Certificate[]{};
                        }
                    }
            };

            // Install the all-trusting trust manager
            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            // Create an ssl socket factory with our all-trusting manager
            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            okhttp3.OkHttpClient.Builder builder = new okhttp3.OkHttpClient.Builder();
            builder.sslSocketFactory(sslSocketFactory, (X509TrustManager)trustAllCerts[0]);
            builder.hostnameVerifier((hostname, session) -> true);

            okhttp3.OkHttpClient okHttpClient = builder.build();
            return okHttpClient;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void get_unsafe(Handler handler) {
        okhttp3.OkHttpClient client = getUnsafeOkHttpClient();
        HttpUrl.Builder urlBuilder = Objects.requireNonNull(HttpUrl.parse(this.url)).newBuilder();
        {
            if(null != this.parameters && this.parameters.size() > 0) {
                for (Map.Entry<String, String> entry : this.parameters.entrySet()) {
                    urlBuilder.addQueryParameter(entry.getKey(), entry.getValue());
                }
            }
        }
        Request.Builder requestBuilder = new Request.Builder();
        // set headers
        {
            if(null != this.headers && this.headers.size() > 0) {
                for (Map.Entry<String, String> entry : this.headers.entrySet()) {
                    requestBuilder.addHeader(entry.getKey(), entry.getValue());
                }
            }
        }
        requestBuilder.url(urlBuilder.build());
        Request request = requestBuilder.build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if(null != handler) {
                    Bundle b = new Bundle();
                    b.put("status", "fail");
                    b.put("msg", e.getMessage());
                    Message m = handler.obtainMessage();
                    m.setData(b);
                    m.sendToTarget();
                }
            }
            @Override
            public void onResponse(Call call, Response response) {
                processResponse(response, handler);
            }
        });
    }

    /**
     * GET application/x-www-form-urlencoded
     */
    public void get(Handler handler) {
        okhttp3.OkHttpClient client = new okhttp3.OkHttpClient();
        HttpUrl.Builder urlBuilder = Objects.requireNonNull(HttpUrl.parse(this.url)).newBuilder();
        {
            if(null != this.parameters && this.parameters.size() > 0) {
                for (Map.Entry<String, String> entry : this.parameters.entrySet()) {
                    urlBuilder.addQueryParameter(entry.getKey(), entry.getValue());
                }
            }
        }
        Request.Builder requestBuilder = new Request.Builder();
        // set headers
        {
            if(null != this.headers && this.headers.size() > 0) {
                for (Map.Entry<String, String> entry : this.headers.entrySet()) {
                    requestBuilder.addHeader(entry.getKey(), entry.getValue());
                }
            }
        }
        requestBuilder.url(urlBuilder.build());
        Request request = requestBuilder.build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if(null != handler) {
                    Bundle b = new Bundle();
                    b.put("status", "fail");
                    b.put("msg", e.getMessage());
                    Message m = handler.obtainMessage();
                    m.setData(b);
                    m.sendToTarget();
                }
            }
            @Override
            public void onResponse(Call call, Response response) {
                processResponse(response, handler);
            }
        });
    }

    /**
     * POST application/x-www-form-urlencoded
     */
    public void post(Handler handler) {
        okhttp3.OkHttpClient client = new okhttp3.OkHttpClient();
        FormBody.Builder formBodyBuilder = new FormBody.Builder();
        {
            if(null != this.parameters && this.parameters.size() > 0) {
                for ( Map.Entry<String, String> entry : this.parameters.entrySet() ) {
                    formBodyBuilder.add(entry.getKey(), entry.getValue());
                }
            }
        }
        Request.Builder requestBuilder = new Request.Builder();
        // set headers
        {
            if(null != this.headers && this.headers.size() > 0) {
                for ( Map.Entry<String, String> entry : this.headers.entrySet() ) {
                    requestBuilder.addHeader(entry.getKey(), entry.getValue());
                }
            }
        }
        requestBuilder.url(this.url);
        requestBuilder.post(formBodyBuilder.build());
        Request request = requestBuilder.build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if(null != handler) {
                    Bundle b = new Bundle();
                    b.put("status", "fail");
                    b.put("msg", e.getMessage());
                    Message m = handler.obtainMessage();
                    m.setData(b);
                    m.sendToTarget();
                }
            }
            @Override
            public void onResponse(Call call, Response response) {
                processResponse(response, handler);
            }
        });
    }

    /**
     * POST application/json
     */
    public void postJSON(JsonObject obj, Handler handler) {
        okhttp3.OkHttpClient client = new okhttp3.OkHttpClient();
        Request.Builder requestBuilder = new Request.Builder();
        // set headers
        {
            if(null != this.headers && this.headers.size() > 0) {
                for ( Map.Entry<String, String> entry : this.headers.entrySet() ) {
                    requestBuilder.addHeader(entry.getKey(), entry.getValue());
                }
                requestBuilder.addHeader("Content-Type","application/json;charset=utf-8");
            }
        }
        requestBuilder.url(this.url);
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(new Gson().toJson(obj), JSON);
        // requestBuilder.post(formBodyBuilder.build());
        requestBuilder.post(body);
        Request request = requestBuilder.build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if(null != handler) {
                    Bundle b = new Bundle();
                    b.put("status", "fail");
                    b.put("msg", e.getMessage());
                    Message m = handler.obtainMessage();
                    m.setData(b);
                    m.sendToTarget();
                }
            }
            @Override
            public void onResponse(Call call, Response response) {
                processResponse(response, handler);
            }
        });
    }

    /**
     * POST form-data
     */
    public void postFormData(final Handler handler) {
        okhttp3.OkHttpClient client = new okhttp3.OkHttpClient();
        FormBody.Builder formBodyBuilder = new FormBody.Builder();
        {
            if(null != this.parameters && this.parameters.size() > 0) {
                for (Map.Entry<String, String> entry : parameters.entrySet()) {
                    formBodyBuilder.add(entry.getKey(), entry.getValue());
                }
            }
        }
        FormBody formBody = formBodyBuilder.build();
        Request.Builder requestBuilder = new Request.Builder();
        requestBuilder.url(this.url);
        // set headers
        {
            if(null != this.headers && this.headers.size() > 0) {
                for (Map.Entry<String, String> entry : this.headers.entrySet()) {
                    requestBuilder.addHeader(entry.getKey(), entry.getValue());
                }
            }
        }
        requestBuilder.post(formBody);
        Request request = requestBuilder.build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if(null != handler) {
                    Bundle b = new Bundle();
                    b.put("status", "fail");
                    b.put("msg", e.getMessage());
                    Message m = handler.obtainMessage();
                    m.setData(b);
                    m.sendToTarget();
                }
            }
            @Override
            public void onResponse(Call call, Response response) {
                processResponse(response, handler);
            }
        });
    }

    private void processResponse(Response response, Handler handler) {
        LinkedHashMap<String, String> headers = new LinkedHashMap<>();
        {
            for(String key : response.headers().names()) {
                String value = response.headers().get(key);
                headers.put(key, value);
            }
        }
        // #200807 避免因 exception 而造成 HttpClient 無 handler 回應的問題
        try {
            String content_type = response.body().contentType().toString();
            if (content_type.length() == 0) {
                processTextResponse(response.body().byteStream(), handler);
            } else {
                if ( content_type.contains("text/") || content_type.contains("application/json") || content_type.contains("application/x-msdownload") ) {
                    if ( alwaysDownload ) {
                        processFileResponse(headers, response.body().byteStream(), handler);
                    } else {
                        processTextResponse(response.body().byteStream(), handler);
                    }
                } else {
                    processFileResponse(headers, response.body().byteStream(), handler);
                }
            }
        } catch (Exception e) {
            processTextResponse(null, handler);
            e.printStackTrace();
        }
    }

    private void processTextResponse(InputStream inputStream, Handler handler) {
        if(null == inputStream) {
            Bundle b = new Bundle();
            b.put("status", "done");
            b.put("resp_type", "text");
            b.put("data", "");
            b.put("msg_zht", "因例外錯誤而中止，請查看 Exception 訊息");
            Message m = handler.obtainMessage();
            m.setData(b);
            m.sendToTarget();
            return;
        }
        final int bufferSize = 2048;
        final char[] buffer = new char[bufferSize];
        final StringBuilder out = new StringBuilder();
        // response proc InputStream Encoding Here
        try( InputStreamReader in = new InputStreamReader(inputStream, resp_encoding) ) {
            for (;;) {
                int rsz = in.read(buffer, 0, buffer.length);
                if (rsz < 0) break;
                out.append(buffer, 0, rsz);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        String res = out.toString();
        Bundle b = new Bundle();
        b.put("status", "done");
        b.put("resp_type", "text");
        b.put("data", res);
        Message m = handler.obtainMessage();
        m.setData(b);
        m.sendToTarget();
    }

    private void processFileResponse(LinkedHashMap<String, String> resp_header, InputStream resp_body, Handler handler) {
        boolean download_status = false;
        StringBuilder sbd = new StringBuilder();
        {
            sbd.append("http_client_download_");
            sbd.append(System.currentTimeMillis());
            sbd.append("_");
            sbd.append(RandomServiceStatic.getInstance().getLowerCaseRandomString(8));
            sbd.append(".tmp");
        }
        try {
            if(null == tempDirPath) {
                throw new Exception("請設定 HttpClient 檔案下載的路徑");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        // 如果沒有暫存路徑則取消下載處理
        if(null == tempDirPath) {
            Bundle b = new Bundle();
            b.put("status", "fail");
            b.put("resp_type", "file");
            b.put("msg_zht", "請設定 HttpClient 檔案下載的路徑");
            Message m = handler.obtainMessage();
            m.setData(b);
            m.sendToTarget();
            return;
        }
        // 由 InputStream 寫入至指定的檔案位置
        File targetFile = null;
        try {
            String fileName = sbd.toString();
            targetFile = new File(tempDirPath+fileName);
            if(tempFileDeleteOnExit) targetFile.deleteOnExit();
            // 當非檔案操作時可使用 inputStream.transferTo()，
            // 而有牽涉到檔案輸出入時要採用 Files.copy() 確保程式效率
            Files.copy(
                    new BufferedInputStream( resp_body ),
                    targetFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING
            );
            download_status = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        Bundle b = new Bundle();
        if(null != targetFile && download_status) {
            b.put("status", "done");
            b.put("resp_type", "file");
            // http response header
            {
                JsonObject obj = new JsonObject();
                for(Map.Entry<String, String> entry : resp_header.entrySet()) {
                    obj.addProperty(entry.getKey(), entry.getValue());
                }
                b.put("headers", new Gson().toJson(obj));
                if (resp_header.containsKey("content_type")) b.put("content_type", resp_header.get("content_type"));
                if (resp_header.containsKey("content_disposition")) b.put("content_disposition", resp_header.get("content_disposition"));
            }
            // 本地端暫存回傳值
            {
                b.put("name", targetFile.getName());
                b.put("size", targetFile.length());
                b.put("path", targetFile.getPath());
            }
            Message m = handler.obtainMessage();
            m.setData(b);
            m.sendToTarget();
        } else {
            b.put("status", "fail");
            b.put("resp_type", "file");
            Message m = handler.obtainMessage();
            m.setData(b);
            m.sendToTarget();
        }
    }

    public static class Builder {

        private String url = null;
        private HashMap<String, String> headers = null;
        private LinkedHashMap<String, String> parameters = null;
        private LinkedHashMap<String, File> files = null;
        private Boolean alwaysDownload = false; // default
        private Boolean tempFileDeleteOnExit = true; // default

        private String tempDirPath = null;
        private String tempDirName = null;

        private Boolean keepUrlSearchParams = null;

        private Duration conn_timeout = null; // default

        private String resp_encoding = StandardCharsets.UTF_8.name();

        public OkHttpClient.Builder setUrl(String url) {
            this.url = url;
            return this;
        }

        public OkHttpClient.Builder setHeaders(LinkedHashMap<String, String> headers) {
            HashMap<String, String> map = new HashMap<>();
            for(Map.Entry<String, String> entry : headers.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if(null == value) value = "";
                map.put(key, value);
            }
            if(map.size() > 0) this.headers = map;
            return this;
        }

        public OkHttpClient.Builder setHeaders(HashMap<String, String> headers) {
            HashMap<String, String> map = new HashMap<>();
            for(Map.Entry<String, String> entry : headers.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if(null == value) value = "";
                map.put(key, value);
            }
            if(map.size() > 0) this.headers = map;
            return this;
        }

        public OkHttpClient.Builder setHeaders(JsonObject headers) {
            HashMap<String, String> map = new HashMap<>();
            for(Object keyObj : headers.keySet()) {
                String key = String.valueOf(keyObj);
                String value = headers.get(key).getAsString();
                if(null == value) value = "";
                map.put(key, value);
            }
            if(map.size() > 0) this.headers = map;
            return this;
        }

        public OkHttpClient.Builder setParameters(LinkedHashMap<String, String> parameters) {
            LinkedHashMap<String, String> map = new LinkedHashMap<>();
            for(Map.Entry<String, String> entry : parameters.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if(null == value) value = "";
                map.put(key, value);
            }
            if(map.size() > 0) this.parameters = map;
            return this;
        }

        /**
         * from HashMap 將不保證參數順序
         */
        public OkHttpClient.Builder setParameters(HashMap<String, String> parameters) {
            LinkedHashMap<String, String> map = new LinkedHashMap<>();
            for(Map.Entry<String, String> entry : parameters.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if(null == value) value = "";
                map.put(key, value);
            }
            if(map.size() > 0) this.parameters = map;
            return this;
        }

        public OkHttpClient.Builder setParameters(JsonObject parameters) {
            LinkedHashMap<String, String> map = new LinkedHashMap<>();
            for(Object keyObj : parameters.keySet()) {
                String key = String.valueOf(keyObj);
                String value = parameters.get(key).getAsString();
                if(null == value) value = "";
                map.put(key, value);
            }
            if(map.size() > 0) this.parameters = map;
            return this;
        }

        public OkHttpClient.Builder setFiles(LinkedHashMap<String, File> files) {
            LinkedHashMap<String, File> map = new LinkedHashMap<>();
            for(Map.Entry<String, File> entry : files.entrySet()) {
                String key = entry.getKey();
                File value = entry.getValue();
                map.put(key, value);
            }
            if(map.size() > 0) this.files = map;
            return this;
        }

        public OkHttpClient.Builder setFiles(HashMap<String, File> files) {
            LinkedHashMap<String, File> map = new LinkedHashMap<>();
            for(Map.Entry<String, File> entry : files.entrySet()) {
                String key = entry.getKey();
                File value = entry.getValue();
                map.put(key, value);
            }
            if(map.size() > 0) this.files = map;
            return this;
        }

        /**
         * 是否將 Response 作為檔案內容下載，並以檔案格式進行操作，
         * 預設為 false，因為大部分的 HTTP 請求會被由純文字內容回應
         */
        public OkHttpClient.Builder setAlwaysDownload(Boolean alwaysDownload) {
            this.alwaysDownload = alwaysDownload;
            return this;
        }

        /**
         * 設定由 HttpClient 下載的暫存檔案長期存放原則，
         * 若為 false 則表示主程式結束時仍持續保留暫存檔案
         */
        public OkHttpClient.Builder setTempFileDelete(Boolean deleteOnExit) {
            if(null != deleteOnExit) this.tempFileDeleteOnExit = deleteOnExit;
            return this;
        }

        /**
         * 設定下載檔案的暫存資料夾路徑
         */
        public OkHttpClient.Builder setTempDirPath(String tempDirPath) {
            this.tempDirPath = tempDirPath;
            return this;
        }

        /**
         * 設定下載檔案的暫存資料夾名稱
         */
        public OkHttpClient.Builder setTempDirName(String tempDirName) {
            this.tempDirName = tempDirName;
            return this;
        }

        /**
         * https://developer.mozilla.org/en-US/docs/Web/API/URLSearchParams
         * 是否維持網址夾帶 URLSearchParams
         */
        public OkHttpClient.Builder setKeepUrlSearchParams(boolean keepUrlSearchParams) {
            this.keepUrlSearchParams = keepUrlSearchParams;
            return this;
        }

        public OkHttpClient.Builder setConnectionTimeout(Duration duration) {
            this.conn_timeout = duration;
            return this;
        }

        public OkHttpClient.Builder setResponseEncoding(String charsets_name) {
            this.resp_encoding = charsets_name;
            return this;
        }

        public OkHttpClient build() {
            return new OkHttpClient(
                    this.url,
                    this.headers,
                    this.parameters,
                    this.files,
                    this.alwaysDownload,
                    this.tempFileDeleteOnExit,
                    this.tempDirPath,
                    this.tempDirName,
                    this.keepUrlSearchParams,
                    this.conn_timeout,
                    this.resp_encoding
            );
        }

    }

}
