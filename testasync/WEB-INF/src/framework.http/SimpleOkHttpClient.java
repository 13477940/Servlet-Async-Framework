package framework.http;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import framework.observer.Bundle;
import framework.observer.Handler;
import framework.observer.Message;
import framework.random.RandomServiceStatic;
import okhttp3.*;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.*;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * [非 JDK 11 環境的 HttpClient 替代方案]
 * https://mvnrepository.com/artifact/org.jetbrains.kotlin/kotlin-stdlib
 * https://mvnrepository.com/artifact/com.squareup.okhttp3/okhttp
 * https://mvnrepository.com/artifact/com.squareup.okio/okio
 *
 * get(only_url_params),
 * post(x-www-form-urlencoded)
 * post(application/json)
 * post(multipart/form-data)
 * -> response(text/file)
 */
public class SimpleOkHttpClient {

    private final String url;
    private final HashMap<String, String> headers;
    private final LinkedHashMap<String, String> parameters;
    private final LinkedHashMap<String, File> files;

    private final boolean alwaysDownload;
    private String tempDirPath;
    private final boolean tempFileDeleteOnExit;
    private final Duration conn_timeout;
    private final boolean insecure_https;

    private String resp_encoding = StandardCharsets.UTF_8.name();

    // init OkHttpClient
    private SimpleOkHttpClient(
            String url,
            LinkedHashMap<String, String> headers,
            LinkedHashMap<String, String> parameters,
            LinkedHashMap<String, File> files,
            Boolean insecure_https,
            Boolean alwaysDownload,
            Boolean tempFileDeleteOnExit,
            String tempDirPath,
            String tempDirName,
            Duration conn_timeout,
            String resp_encoding
    ) {
        this.url = url;
        {
            // 是否具有 UrlSearchParams
            if(url.contains("?")) {
                String[] tmp = url.split("\\?");
                url = tmp[0];
                String getParamsStr = tmp[1];
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
        }
        this.headers = headers;
        this.parameters = parameters;
        this.files = files;
        this.insecure_https = Objects.requireNonNullElse(insecure_https, false);
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

    /**
     * GET application/x-www-form-urlencoded
     */
    public void get(Handler handler) {
        OkHttpClient client;
        if(insecure_https) {
            client = get_insecure_http_client();
        } else {
            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            if(null != conn_timeout) builder.connectTimeout(conn_timeout);
            client = builder.build();
        }
        // 設定 Parameters
        LinkedHashMap<String, String> _params;
        {
            _params = parse_url_parameter(this.url);
            if(null != this.parameters && this.parameters.size() > 0) {
                // 若有重複的 key 以 params map 值為主
                for (Map.Entry<String, String> params : this.parameters.entrySet()) {
                    _params.put(params.getKey(), params.getValue());
                }
            }
        }
        HttpUrl.Builder urlBuilder = Objects.requireNonNull(HttpUrl.parse(parse_url_domain(this.url))).newBuilder();
        {
            if(null != _params && _params.size() > 0) {
                for (Map.Entry<String, String> entry : _params.entrySet()) {
                    urlBuilder.addQueryParameter(entry.getKey(), entry.getValue());
                }
            }
        }
        Request.Builder requestBuilder = new Request.Builder();
        // request header
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
        OkHttpClient client;
        if(insecure_https) {
            client = get_insecure_http_client();
        } else {
            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            if(null != conn_timeout) builder.connectTimeout(conn_timeout);
            client = builder.build();
        }
        // 設定 Parameters
        LinkedHashMap<String, String> _params;
        {
            _params = parse_url_parameter(this.url);
            if(null != this.parameters && this.parameters.size() > 0) {
                // 若有重複的 key 以 params map 值為主
                for (Map.Entry<String, String> params : this.parameters.entrySet()) {
                    _params.put(params.getKey(), params.getValue());
                }
            }
        }
        FormBody.Builder formBodyBuilder = new FormBody.Builder();
        {
            if(null != _params && _params.size() > 0) {
                for ( Map.Entry<String, String> entry : _params.entrySet() ) {
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
        requestBuilder.url(parse_url_domain(this.url));
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
        OkHttpClient client;
        if(insecure_https) {
            client = get_insecure_http_client();
        } else {
            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            if(null != conn_timeout) builder.connectTimeout(conn_timeout);
            client = builder.build();
        }
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
        requestBuilder.url(parse_url_domain(this.url));
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(new Gson().toJson(obj), JSON);
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
        OkHttpClient client;
        if(insecure_https) {
            client = get_insecure_http_client();
        } else {
            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            if(null != conn_timeout) builder.connectTimeout(conn_timeout);
            client = builder.build();
        }
        // proc multipart body
        MultipartBody.Builder bodyBuilder = new MultipartBody.Builder().setType(MultipartBody.FORM);
        {
            LinkedHashMap<String, String> params = parse_url_parameter(this.url);
            // url params
            for(Map.Entry<String, String> entry : params.entrySet()) {
                bodyBuilder.addFormDataPart(entry.getKey(), entry.getValue());
            }
            // set params
            for(Map.Entry<String, String> entry : this.parameters.entrySet()) {
                bodyBuilder.addFormDataPart(entry.getKey(), entry.getValue());
            }
            // set files
            for(Map.Entry<String, File> entry : this.files.entrySet()) {
                String extName;
                {
                    String fileName = entry.getValue().getName();
                    // 直接用反轉字串並取第一個看見的點號位置
                    String revStr = reverseString(fileName);
                    int index = revStr.indexOf(".");
                    if(-1 == index) {
                        extName = "";
                    } else {
                        extName = fileName.substring(fileName.length() - index);
                    }
                }
                MediaType mediaType = MediaType.parse(extName);
                RequestBody fileBody = RequestBody.create(entry.getValue(), mediaType);
                bodyBuilder.addFormDataPart(entry.getKey(), entry.getValue().getName(), fileBody);
            }
        }
        Request.Builder requestBuilder = new Request.Builder();
        requestBuilder.url(parse_url_domain(this.url));
        // set headers
        {
            if(null != this.headers && this.headers.size() > 0) {
                for (Map.Entry<String, String> entry : this.headers.entrySet()) {
                    requestBuilder.addHeader(entry.getKey(), entry.getValue());
                }
            }
        }
        requestBuilder.post(bodyBuilder.build()); // post multipart
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
                processTextResponse(headers, response.body().byteStream(), handler);
            } else {
                if ( content_type.contains("text/") || content_type.contains("application/json") || content_type.contains("application/x-msdownload") ) {
                    if ( alwaysDownload ) {
                        processFileResponse(headers, response.body().byteStream(), handler);
                    } else {
                        processTextResponse(headers, response.body().byteStream(), handler);
                    }
                } else {
                    processFileResponse(headers, response.body().byteStream(), handler);
                }
            }
        } catch (Exception e) {
            processTextResponse(headers, null, handler);
            e.printStackTrace();
        }
    }

    private void processTextResponse(LinkedHashMap<String, String> resp_header, InputStream inputStream, Handler handler) {
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
        if(resp_header.containsKey("content_type")) b.put("content_type", resp_header.get("content_type"));
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
            if(resp_header.containsKey("content_type")) b.put("content_type", resp_header.get("content_type"));
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

    /**
     * 解析網址夾帶的參數內容（正規的 URL 參數內容要是 URL encoding 格式）
     * https://developer.mozilla.org/en-US/docs/Glossary/percent-encoding
     */
    private LinkedHashMap<String, String> parse_url_parameter(String url) {
        if(null == url || url.length() == 0) return null; // is error url
        int start_index = url.indexOf("?");
        if( -1 == start_index ) return new LinkedHashMap<>(); // is empty map
        String param_str = url.substring(start_index + 1); // +1 skip '?'
        String mode = "key"; // 'key' or 'value' mode, default is key
        LinkedHashMap<String, String> params = new LinkedHashMap<>();
        // proc request content value
        String now_key = null;
        {
            StringBuilder sbd = new StringBuilder(); // char pool
            for(int i = 0, len = param_str.length(); i < len; i++) {
                String str = String.valueOf(param_str.charAt(i)); // by word
                switch (str) {
                    // value
                    case "=": {
                        // if duplicate
                        if("value".equals(mode)) continue;
                        // key is over
                        {
                            now_key = URLDecoder.decode(sbd.toString(), StandardCharsets.UTF_8);
                            params.put(now_key, "");
                        }
                        // next
                        mode = "value";
                        sbd.delete(0, sbd.length());
                    } break;
                    // key
                    case "&": {
                        // if duplicate
                        if("key".equals(mode)) continue;
                        // value is over
                        {
                            params.put(now_key, URLDecoder.decode(sbd.toString(), StandardCharsets.UTF_8));
                        }
                        // next
                        mode = "key";
                        sbd.delete(0, sbd.length());
                    } break;
                    default: {
                        switch (mode) {
                            case "key":
                            case "value": {
                                sbd.append(str);
                            } break;
                        }
                    } break;
                }
            }
            // flush
            {
                switch (mode) {
                    case "key": {
                        now_key = URLDecoder.decode(sbd.toString(), StandardCharsets.UTF_8);
                        params.put(now_key, "");
                    } break;
                    case "value": {
                        params.put(now_key, URLDecoder.decode(sbd.toString(), StandardCharsets.UTF_8));
                    } break;
                }
            }
        }
        return params;
    }

    /**
     * 解析網址為僅網域的格式，去除後面的參數夾帶
     */
    private String parse_url_domain(String url) {
        String domain;
        {
            int index = url.indexOf("?");
            if (-1 < index) {
                domain = url.substring(0, index);
            } else {
                domain = url;
            }
            // set default scheme
            if(!domain.contains("http://") && !domain.contains("https://")) {
                domain = "http://" + domain;
            }
        }
        return domain;
    }

    private OkHttpClient get_insecure_http_client() {
        OkHttpClient okHttpClient = null;
        try {
            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            builder.sslSocketFactory(get_insecure_ssl_context(), (X509TrustManager) get_trust_all_certs()[0]);
            builder.hostnameVerifier((hostname, session) -> true);
            if(null != conn_timeout) builder.connectTimeout(conn_timeout);
            okHttpClient = builder.build();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return okHttpClient;
    }

    private SSLSocketFactory get_insecure_ssl_context() {
        // Create a trust manager that does not validate certificate chains
        SSLSocketFactory sslSocketFactory = null;
        try {
            // Install the all-trusting trust manager
            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, get_trust_all_certs(), new java.security.SecureRandom());
            // Create an ssl socket factory with our all-trusting manager
            sslSocketFactory = sslContext.getSocketFactory();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sslSocketFactory;
    }

    private TrustManager[] get_trust_all_certs() {
        return new TrustManager[] {
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {}

                    @Override
                    public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {}

                    @Override
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return new java.security.cert.X509Certificate[]{};
                    }
                }
        };
    }

    private String reverseString(String str) {
        StringBuilder sbd = new StringBuilder();
        for(int i = str.length() - 1, len = 0; i >= len; i--) {
            sbd.append(str.charAt(i));
        }
        return sbd.toString();
    }

    public static class Builder {

        private String url = null;
        private LinkedHashMap<String, String> headers = null;
        private LinkedHashMap<String, String> parameters = null;
        private LinkedHashMap<String, File> files = null;
        private Boolean alwaysDownload = false; // default
        private Boolean tempFileDeleteOnExit = true; // default
        private String tempDirPath = null;
        private String tempDirName = null;
        private Duration conn_timeout = null; // default
        private Boolean insecure_https = false;
        private String resp_encoding = StandardCharsets.UTF_8.name();

        public SimpleOkHttpClient.Builder setUrl(String url) {
            this.url = url;
            return this;
        }

        public SimpleOkHttpClient.Builder setHeaders(LinkedHashMap<String, String> headers) {
            LinkedHashMap<String, String> map = new LinkedHashMap<>();
            for(Map.Entry<String, String> entry : headers.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if(null == value) value = "";
                map.put(key, value);
            }
            if(map.size() > 0) this.headers = map;
            return this;
        }

        public SimpleOkHttpClient.Builder setHeaders(HashMap<String, String> headers) {
            LinkedHashMap<String, String> map = new LinkedHashMap<>();
            for(Map.Entry<String, String> entry : headers.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if(null == value) value = "";
                map.put(key, value);
            }
            if(map.size() > 0) this.headers = map;
            return this;
        }

        public SimpleOkHttpClient.Builder setHeaders(JsonObject headers) {
            LinkedHashMap<String, String> map = new LinkedHashMap<>();
            for(Object keyObj : headers.keySet()) {
                String key = String.valueOf(keyObj);
                String value = headers.get(key).getAsString();
                if(null == value) value = "";
                map.put(key, value);
            }
            if(map.size() > 0) this.headers = map;
            return this;
        }

        public SimpleOkHttpClient.Builder setParameters(LinkedHashMap<String, String> parameters) {
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
        public SimpleOkHttpClient.Builder setParameters(HashMap<String, String> parameters) {
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

        public SimpleOkHttpClient.Builder setParameters(JsonObject parameters) {
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

        public SimpleOkHttpClient.Builder setFiles(LinkedHashMap<String, File> files) {
            LinkedHashMap<String, File> map = new LinkedHashMap<>();
            for(Map.Entry<String, File> entry : files.entrySet()) {
                String key = entry.getKey();
                File value = entry.getValue();
                map.put(key, value);
            }
            if(map.size() > 0) this.files = map;
            return this;
        }

        public SimpleOkHttpClient.Builder setFiles(HashMap<String, File> files) {
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
        public SimpleOkHttpClient.Builder setAlwaysDownload(Boolean alwaysDownload) {
            this.alwaysDownload = alwaysDownload;
            return this;
        }

        /**
         * 設定由 HttpClient 下載的暫存檔案長期存放原則，
         * 若為 false 則表示主程式結束時仍持續保留暫存檔案
         */
        public SimpleOkHttpClient.Builder setTempFileDelete(Boolean deleteOnExit) {
            if(null != deleteOnExit) this.tempFileDeleteOnExit = deleteOnExit;
            return this;
        }

        /**
         * 設定下載檔案的暫存資料夾路徑
         */
        public SimpleOkHttpClient.Builder setTempDirPath(String tempDirPath) {
            this.tempDirPath = tempDirPath;
            return this;
        }

        /**
         * 設定下載檔案的暫存資料夾名稱
         */
        public SimpleOkHttpClient.Builder setTempDirName(String tempDirName) {
            this.tempDirName = tempDirName;
            return this;
        }

        public SimpleOkHttpClient.Builder setConnectionTimeout(Duration duration) {
            this.conn_timeout = duration;
            return this;
        }

        public SimpleOkHttpClient.Builder setResponseEncoding(String charsets_name) {
            this.resp_encoding = charsets_name;
            return this;
        }

        /**
         * 設定 true 表示不檢查 HTTPS 證書正確性，預設為 false
         */
        public SimpleOkHttpClient.Builder setInsecureHttps(Boolean insecure_https) {
            this.insecure_https = insecure_https;
            return this;
        }

        public SimpleOkHttpClient build() {
            return new SimpleOkHttpClient(
                    this.url,
                    this.headers,
                    this.parameters,
                    this.files,
                    this.insecure_https,
                    this.alwaysDownload,
                    this.tempFileDeleteOnExit,
                    this.tempDirPath,
                    this.tempDirName,
                    this.conn_timeout,
                    this.resp_encoding
            );
        }

    }

}
