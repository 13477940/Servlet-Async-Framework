package framework.http;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import framework.observer.Bundle;
import framework.observer.Handler;
import framework.observer.Message;
import framework.random.RandomServiceStatic;
import framework.thread.ThreadPoolStatic;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.*;

/**
 * [required JDK 11+]
 * https://docs.oracle.com/en/java/javase/11/docs/api/java.net.http/java/net/http/HttpClient.html
 * http://openjdk.java.net/groups/net/httpclient/intro.html
 * https://golb.hplar.ch/2019/01/java-11-http-client.html
 *
 * get(only_url_params),
 * post(x-www-form-urlencoded)
 * post(application/json)
 * post(multipart/form-data)
 * -> response(text/file)
 */
public class JdkHttpClient {

    private final String url;
    private final LinkedHashMap<String, String> headers;
    private final LinkedHashMap<String, String> parameters;
    private final LinkedHashMap<String, File> files;

    private final boolean alwaysDownload;
    private String tempDirPath; // 自定義下載暫存資料夾路徑
    private final boolean tempFileDeleteOnExit;
    private final Duration conn_timeout;
    private final boolean insecure_https;

    private final HttpClient.Version httpVersion = HttpClient.Version.HTTP_2; // default
    private final HttpClient.Redirect httpRedirect = HttpClient.Redirect.NORMAL; // default

    private String resp_encoding = StandardCharsets.UTF_8.name();

    // init
    private JdkHttpClient(
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
     * GET 不具有 body，僅具有 Authorization 及 Header 夾帶能力，
     * 其他資料由網址後的參數夾帶進行附加，要注意 URI Encoding 的使用範圍
     */
    public void get(Handler handler) {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder();
        {
            // 設定 request header
            if (null != headers && headers.size() > 0) {
                for (Map.Entry<String, String> header : headers.entrySet()) {
                    requestBuilder.setHeader(header.getKey(), header.getValue());
                }
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
            // 建立正規化 parameter string 於請求網址後
            StringBuilder sbd = new StringBuilder();
            {
                sbd.append(parse_url_domain(this.url));
                if (null != _params && _params.size() > 0) {
                    boolean isFirst = true;
                    for (Map.Entry<String, String> params : _params.entrySet()) {
                        if (isFirst) {
                            sbd.append("?");
                            isFirst = false;
                        } else {
                            sbd.append("&");
                        }
                        sbd.append(URLEncoder.encode(params.getKey(), StandardCharsets.UTF_8));
                        sbd.append("=");
                        sbd.append(URLEncoder.encode(params.getValue(), StandardCharsets.UTF_8));
                    }
                }
            }
            // method GET
            requestBuilder.GET();
            // 設定請求 URI
            requestBuilder.uri(URI.create(sbd.toString()));
        }
        HttpClient.Builder clientBuilder = HttpClient.newBuilder();
        {
            clientBuilder.version(httpVersion);
            clientBuilder.followRedirects(httpRedirect);
            clientBuilder.executor(ThreadPoolStatic.getInstance());
            if(null != conn_timeout) clientBuilder.connectTimeout(conn_timeout);
            if(insecure_https) clientBuilder.sslContext(get_insecure_ssl_context());
        }
        // build & run
        HttpRequest request = requestBuilder.build();
        HttpClient client = new WeakReference<>( clientBuilder.build() ).get();
        assert null != client;
        asyncRequest(client, request, handler);
    }

    /**
     * POST application/x-www-form-urlencoded
     */
    public void post(Handler handler) {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder();
        {
            // request header
            if (null != headers && headers.size() > 0) {
                for (Map.Entry<String, String> header : headers.entrySet()) {
                    requestBuilder.setHeader(header.getKey(), header.getValue());
                }
            }
            // 採用 post 預設方法必須是 application/x-www-form-urlencoded 的方式夾帶參數
            requestBuilder.setHeader("Content-Type","application/x-www-form-urlencoded;charset=utf-8");
        }
        {
            // request parameter
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
            if (null != _params && _params.size() > 0) {
                StringBuilder sbd = new StringBuilder();
                boolean isFirst = true;
                for (Map.Entry<String, String> params : _params.entrySet()) {
                    if(isFirst) { isFirst = false; } else { sbd.append("&"); }
                    sbd.append(URLEncoder.encode(params.getKey(), StandardCharsets.UTF_8));
                    sbd.append("=");
                    sbd.append(URLEncoder.encode(params.getValue(), StandardCharsets.UTF_8));
                }
                requestBuilder.POST(HttpRequest.BodyPublishers.ofString(sbd.toString(), StandardCharsets.UTF_8));
            } else {
                requestBuilder.POST(HttpRequest.BodyPublishers.noBody());
            }
            requestBuilder.uri(URI.create(parse_url_domain(this.url)));
        }
        HttpClient.Builder clientBuilder = HttpClient.newBuilder();
        {
            clientBuilder.version(httpVersion);
            clientBuilder.followRedirects(httpRedirect);
            clientBuilder.executor(ThreadPoolStatic.getInstance());
            if(null != conn_timeout) clientBuilder.connectTimeout(conn_timeout);
            if(insecure_https) clientBuilder.sslContext(get_insecure_ssl_context());
        }
        // build & run
        HttpRequest request = requestBuilder.build();
        HttpClient client = new WeakReference<>( clientBuilder.build() ).get();
        assert null != client;
        asyncRequest(client, request, handler);
    }

    /**
     * POST application/json
     */
    public void postJSON(JsonObject obj, Handler handler) {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder();
        {
            // request header
            if (null != headers && headers.size() > 0) {
                for (Map.Entry<String, String> header : headers.entrySet()) {
                    requestBuilder.setHeader(header.getKey(), header.getValue());
                }
            }
            // 如果採用 post 預設方法必須是 application/x-www-form-urlencoded 的方式夾帶參數
            // 此方法則採用 raw 內容方式夾帶 JSON 字串
            requestBuilder.setHeader("Content-Type","application/json;charset=utf-8");
            if (null != obj) {
                requestBuilder.POST(HttpRequest.BodyPublishers.ofString(new Gson().toJson(obj), StandardCharsets.UTF_8));
            } else {
                requestBuilder.POST(HttpRequest.BodyPublishers.noBody());
            }
            requestBuilder.uri(URI.create(parse_url_domain(this.url)));
        }
        HttpClient.Builder clientBuilder = HttpClient.newBuilder();
        {
            clientBuilder.version(httpVersion);
            clientBuilder.followRedirects(httpRedirect);
            clientBuilder.executor(ThreadPoolStatic.getInstance());
            if(null != conn_timeout) clientBuilder.connectTimeout(conn_timeout);
            if(insecure_https) clientBuilder.sslContext(get_insecure_ssl_context());
        }
        // build & run
        HttpRequest request = requestBuilder.build();
        HttpClient client = new WeakReference<>( clientBuilder.build() ).get();
        assert null != client;
        asyncRequest(client, request, handler);
    }

    /**
     * POST multipart/form-data
     */
    public void postFormData(Handler handler) {
        String boundary = "jdk11hc_"+System.currentTimeMillis()+"_"+ RandomServiceStatic.getInstance().getLowerCaseRandomString(8);
        LinkedHashMap<String, Object> bMap = new LinkedHashMap<>();
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
        if(null != _params && _params.size() > 0) {
            for (Map.Entry<String, String> param : _params.entrySet()) {
                bMap.put(param.getKey(), param.getValue());
            }
        }
        // 設定 Files
        if(null != files && files.size() > 0) {
            for (Map.Entry<String, File> file : files.entrySet()) {
                bMap.put(file.getKey(), file.getValue());
            }
        }
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder();
        {
            // request header
            if (null != headers && headers.size() > 0) {
                for (Map.Entry<String, String> header : headers.entrySet()) {
                    requestBuilder.setHeader(header.getKey(), header.getValue());
                }
            }
        }
        {
            requestBuilder.uri(URI.create(parse_url_domain(this.url)));
            requestBuilder.header("Content-Type", "multipart/form-data;boundary=" + boundary);
            requestBuilder.POST(ofMimeMultipartData(bMap, boundary));
        }
        HttpClient.Builder clientBuilder = HttpClient.newBuilder();
        {
            clientBuilder.version(httpVersion);
            clientBuilder.followRedirects(httpRedirect);
            clientBuilder.executor(ThreadPoolStatic.getInstance());
            if(null != conn_timeout) clientBuilder.connectTimeout(conn_timeout);
            if(insecure_https) clientBuilder.sslContext(get_insecure_ssl_context());
        }
        // build & run
        HttpRequest request = requestBuilder.build();
        HttpClient client = new WeakReference<>( clientBuilder.build() ).get();
        assert client != null;
        asyncRequest(client, request, handler);
    }

    /**
     * 藉由非同步方式處理 Response
     */
    private void asyncRequest(HttpClient client, HttpRequest request, Handler handler) {
        Handler tmp_handler;
        {
            LinkedHashMap<String, String> resp_header = new LinkedHashMap<>();
            tmp_handler = new Handler() {
                @Override
                public void handleMessage(Message m) {
                    super.handleMessage(m);
                    m.getData().get("response");
                    String status = m.getData().getString("status");
                    if("header".equals(status)) {
                        resp_header.put("status_code", m.getData().getString("status_code"));
                        if(m.getData().containsKey("content_type")) {
                            resp_header.put("content_type", m.getData().getString("content_type"));
                        }
                        if(m.getData().containsKey("content_disposition")) {
                            resp_header.put("content_disposition", m.getData().getString("content_disposition"));
                        }
                    }
                    if("body".equals(status)) {
                        InputStream resp_body = (InputStream) m.getData().get("input_stream");
                        String contentType = resp_header.get("content_type");
                        if(null == contentType || contentType.length() == 0) {
                            processTextResponse(resp_header, resp_body, handler);
                        } else {
                            if(contentType.contains("text/") || contentType.contains("application/json") || contentType.contains("application/x-msdownload")) {
                                if(alwaysDownload) {
                                    processFileResponse(resp_header, resp_body, handler);
                                } else {
                                    processTextResponse(resp_header, resp_body, handler);
                                }
                            } else {
                                processFileResponse(resp_header, resp_body, handler);
                            }
                        }
                    }
                    if("exception".equals(status)) {
                        handler.sendMessage(m);
                    }
                }
            };
        }
        {
            client.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream())
                    .whenCompleteAsync((resp_input_stream, throw_opt) -> {
                        if(null != throw_opt) {
                            throw_opt.printStackTrace();
                            {
                                Bundle b = new Bundle();
                                b.put("status", "exception");
                                b.put("throwable", throw_opt);
                                Message m = tmp_handler.obtainMessage();
                                m.setData(b);
                                m.sendToTarget();
                            }
                        } else {
                            // proc resp header
                            {
                                LinkedHashMap<String, String> headers = new LinkedHashMap<>();
                                for (Map.Entry<String, List<String>> entry : resp_input_stream.headers().map().entrySet()) {
                                    String key = entry.getKey();
                                    List<String> value = entry.getValue();
                                    if (value.size() > 1) {
                                        int indx = 0;
                                        for (String str : entry.getValue()) {
                                            if (0 == indx) {
                                                headers.put(key, str);
                                            } else {
                                                headers.put(key + "_" + indx, str);
                                            }
                                            indx++;
                                        }
                                    } else {
                                        headers.put(key, value.get(0));
                                    }
                                }
                                {
                                    Bundle b = new Bundle();
                                    b.put("status", "header");
                                    b.put("status_code", String.valueOf(resp_input_stream.statusCode()));
                                    b.put("headers", headers);
                                    if(headers.containsKey("content-type")) b.put("content_type", headers.get("content-type"));
                                    if(headers.containsKey("content-disposition")) b.put("content_disposition", headers.get("content-disposition"));
                                    Message m = tmp_handler.obtainMessage();
                                    m.setData(b);
                                    m.sendToTarget();
                                }
                            }
                            // proc resp body
                            {
                                Bundle b = new Bundle();
                                b.put("status", "body");
                                b.put("input_stream", resp_input_stream.body());
                                Message m = tmp_handler.obtainMessage();
                                m.setData(b);
                                m.sendToTarget();
                            }
                        }
                    });
        }
    }

    private void processTextResponse(LinkedHashMap<String, String> resp_header, InputStream resp_body, Handler handler) {
        Bundle b = new Bundle();
        b.put("status", "done");
        if(resp_header.containsKey("content_type")) b.put("content_type", resp_header.get("content_type"));
        b.put("resp_type", "text");
        try( BufferedInputStream bis = new BufferedInputStream( resp_body ) ) {
            b.put("data", new String(bis.readAllBytes(), resp_encoding));
        } catch (Exception e) {
            b.put("status", "fail");
            e.printStackTrace();
        }
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

    /**
     * https://golb.hplar.ch/2019/01/java-11-http-client.html
     * https://github.com/ralscha/blog2019/blob/master/java11httpclient/client/src/main/java/ch/rasc/httpclient/File.java
     */
    private static HttpRequest.BodyPublisher ofMimeMultipartData(Map<String, Object> data, String boundary) {
        var byteArrays = new ArrayList<byte[]>();
        byte[] separator = ("--" + boundary + "\r\nContent-Disposition: form-data; name=").getBytes(StandardCharsets.UTF_8);
        for ( Map.Entry<String, Object> entry : data.entrySet() ) {
            byteArrays.add(separator);
            if ( entry.getValue() instanceof File ) {
                var path = ((File) entry.getValue()).toPath();
                String mimeType = null;
                try {
                    mimeType = Files.probeContentType(path);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                byteArrays.add(("\"" + entry.getKey() + "\"; filename=\"" + path.getFileName()+ "\"\r\nContent-Type: " + mimeType + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
                try {
                    byteArrays.add(Files.readAllBytes(path));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                byteArrays.add("\r\n".getBytes(StandardCharsets.UTF_8));
            } else {
                byteArrays.add(("\"" + entry.getKey() + "\"\r\n\r\n" + entry.getValue() + "\r\n").getBytes(StandardCharsets.UTF_8));
            }
        }
        byteArrays.add(("--" + boundary + "--").getBytes(StandardCharsets.UTF_8));
        return HttpRequest.BodyPublishers.ofByteArrays(byteArrays);
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
     * JDK HttpClient 忽略 HTTPS 證書驗證實作
     * https://stackoverflow.com/a/5308658
     */
    private SSLContext get_insecure_ssl_context() {
        SSLContext sslContext = null;
        try {
            sslContext = SSLContext.getInstance("TLS");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        {
            try {
                assert sslContext != null;
                sslContext.init(null, new TrustManager[]{
                        new X509TrustManager() {
                            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                                return null;
                            }

                            public void checkClientTrusted(
                                    java.security.cert.X509Certificate[] certs, String authType) {
                            }

                            public void checkServerTrusted(
                                    java.security.cert.X509Certificate[] certs, String authType) {
                            }
                        }
                }, new SecureRandom());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return sslContext;
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

    public static class Builder {

        private String url = null;
        private LinkedHashMap<String, String> headers = null;
        private LinkedHashMap<String, String> parameters = null;
        private LinkedHashMap<String, File> files = null;
        private Boolean alwaysDownload = false;
        private Boolean tempFileDeleteOnExit = true;
        private String tempDirPath = null;
        private String tempDirName = null;
        private Duration conn_timeout = null;
        private Boolean insecure_https = false;
        private String resp_encoding = StandardCharsets.UTF_8.name();

        public JdkHttpClient.Builder setUrl(String url) {
            this.url = url;
            return this;
        }

        public JdkHttpClient.Builder setHeaders(LinkedHashMap<String, String> headers) {
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

        public JdkHttpClient.Builder setHeaders(HashMap<String, String> headers) {
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

        public JdkHttpClient.Builder setHeaders(JsonObject headers) {
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

        public JdkHttpClient.Builder setParameters(LinkedHashMap<String, String> parameters) {
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

        public JdkHttpClient.Builder setParameters(HashMap<String, String> parameters) {
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

        public JdkHttpClient.Builder setParameters(JsonObject parameters) {
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

        public JdkHttpClient.Builder setFiles(LinkedHashMap<String, File> files) {
            LinkedHashMap<String, File> map = new LinkedHashMap<>();
            for(Map.Entry<String, File> entry : files.entrySet()) {
                String key = entry.getKey();
                File value = entry.getValue();
                map.put(key, value);
            }
            if(map.size() > 0) this.files = map;
            return this;
        }

        public JdkHttpClient.Builder setFiles(HashMap<String, File> files) {
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
        public JdkHttpClient.Builder setAlwaysDownload(Boolean alwaysDownload) {
            this.alwaysDownload = alwaysDownload;
            return this;
        }

        /**
         * 設定由 HttpClient 下載的暫存檔案長期存放原則，
         * 若為 false 則表示主程式結束時仍持續保留暫存檔案
         */
        public JdkHttpClient.Builder setTempFileDelete(Boolean deleteOnExit) {
            if(null != deleteOnExit) this.tempFileDeleteOnExit = deleteOnExit;
            return this;
        }

        /**
         * 設定下載檔案的暫存資料夾路徑
         */
        public JdkHttpClient.Builder setTempDirPath(String tempDirPath) {
            this.tempDirPath = tempDirPath;
            return this;
        }

        /**
         * 設定下載檔案的暫存資料夾名稱
         */
        public JdkHttpClient.Builder setTempDirName(String tempDirName) {
            this.tempDirName = tempDirName;
            return this;
        }

        public JdkHttpClient.Builder setConnectionTimeout(Duration duration) {
            this.conn_timeout = duration;
            return this;
        }

        /**
         * 設定 true 表示不檢查 HTTPS 證書正確性，預設為 false
         */
        public JdkHttpClient.Builder setInsecureHttps(Boolean insecure_https) {
            this.insecure_https = insecure_https;
            return this;
        }

        /**
         * 設定回傳內容的編碼（串接傳統非 UTF-8 環境）
         */
        public JdkHttpClient.Builder setResponseEncoding(String charsets_name) {
            this.resp_encoding = charsets_name;
            return this;
        }

        public JdkHttpClient build() {
            return new JdkHttpClient(
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
