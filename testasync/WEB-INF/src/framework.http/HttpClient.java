package framework.http;

import com.alibaba.fastjson.JSONObject;
import framework.observer.Bundle;
import framework.observer.Handler;
import framework.observer.Message;
import framework.random.RandomServiceStatic;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.*;

/**
 * [required openJDK 11+]
 * https://docs.oracle.com/en/java/javase/11/docs/api/java.net.http/java/net/http/HttpClient.html
 * http://openjdk.java.net/groups/net/httpclient/intro.html
 * https://golb.hplar.ch/2019/01/java-11-http-client.html
 *
 * implement by UrielTech.com TomLi
 * 基於 java.net.http.HttpClient 的功能實作封裝
 * 並模擬類似 OkHttp 方式處理
 *
 * for old jdk version use: https://github.com/square/okhttp
 * https://mvnrepository.com/artifact/com.squareup.okhttp3/okhttp
 *
 * 2019-12-18 修正檔案下載處理的行為與回傳值可自定義處理的內容
 * 2020-02-25 刪除 AppSetting 引用，下載檔案時請自行指定暫存路徑
 * 2020-03-16 更換 LinkedHashMap 為基礎，維持使用者傳入的參數順序
 * 2020-03-24 增加 keepUrlSearchParams 參數，判斷是否需保留完整的參數網址
 * 2020-04-16 修正 post form-data 無法正常關閉連線的問題
 * 2020-04-17 修正語法細節及預設方法的使用方式
 */
public class HttpClient {

    private String url;
    private String getParamsStr;
    private final HashMap<String, String> headers;
    private final LinkedHashMap<String, String> parameters;
    private final LinkedHashMap<String, File> files;
    private final boolean alwaysDownload;
    private String tempDirPath; // 自定義下載暫存資料夾路徑
    // private String tempDirName = "http_client_temp";
    private final boolean tempFileDeleteOnExit;

    private final java.net.http.HttpClient.Version httpVersion = java.net.http.HttpClient.Version.HTTP_2; // default
    private final java.net.http.HttpClient.Redirect httpRedirect = java.net.http.HttpClient.Redirect.NORMAL; // default

    private final Duration conn_timeout;

    // 建構子
    private HttpClient(
            String url,
            HashMap<String, String> headers,
            LinkedHashMap<String, String> parameters,
            LinkedHashMap<String, File> files,
            Boolean alwaysDownload,
            Boolean tempFileDeleteOnExit,
            String tempDirPath,
            String tempDirName,
            Boolean keepUrlSearchParams,
            Duration conn_timeout
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
                            sbd.append(URLEncoder.encode(pStr[0], StandardCharsets.UTF_8));
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
            // 是否需保留原始網址（非正規情況時，可能只處理網址內容）
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
    }

    /**
     * GET application/x-www-form-urlencoded
     * GET 僅具有 Authorization 及 Header 夾帶能力，
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
            StringBuilder sbd = new StringBuilder();
            sbd.append(url);
            if (null != parameters && parameters.size() > 0) {
                boolean isFirst = true;
                for (Map.Entry<String, String> params : parameters.entrySet()) {
                    if(isFirst) { sbd.append("?"); isFirst = false;} else { sbd.append("&"); }
                    sbd.append(URLEncoder.encode(params.getKey(), StandardCharsets.UTF_8));
                    sbd.append("=");
                    sbd.append(URLEncoder.encode(params.getValue(), StandardCharsets.UTF_8));
                }
            } else {
                boolean mode = true; // t = key, f = value;
                LinkedHashMap<String, String> map = new LinkedHashMap<>();
                StringBuilder tmp = new StringBuilder();
                String prevKey = null;
                if(null != getParamsStr && getParamsStr.length() > 0) {
                    for (int i = 0, len = getParamsStr.length(); i < len; i++) {
                        String s = String.valueOf(getParamsStr.charAt(i));
                        if (mode && s.equals("=")) {
                            prevKey = tmp.toString();
                            map.put(prevKey, "");
                            tmp.delete(0, tmp.length());
                            mode = false;
                            continue;
                        }
                        if (!mode && s.equals("&")) {
                            map.put(prevKey, tmp.toString());
                            tmp.delete(0, tmp.length());
                            mode = true;
                            continue;
                        }
                        tmp.append(s);
                        if (i == getParamsStr.length() - 1) {
                            map.put(prevKey, tmp.toString());
                            tmp.delete(0, tmp.length());
                        }
                    }
                }
                if(map.size() > 0) {
                    boolean isFirst = true;
                    for (Map.Entry<String, String> params : map.entrySet()) {
                        if(isFirst) {
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
        java.net.http.HttpClient.Builder clientBuilder = java.net.http.HttpClient.newBuilder();
        {
            clientBuilder.version(httpVersion);
            clientBuilder.followRedirects(httpRedirect);
            // clientBuilder.executor(ThreadPoolStatic.getInstance());
            if(null != conn_timeout) clientBuilder.connectTimeout(conn_timeout);
        }
        // build & run
        HttpRequest request = new WeakReference<>( requestBuilder.build() ).get();
        java.net.http.HttpClient client = new WeakReference<>( clientBuilder.build() ).get();
        assert null != client;
        asyncRequest(client, request, handler);
    }

    /**
     * POST application/x-www-form-urlencoded
     */
    public void post(Handler handler) {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder();
        {
            // 設定 request header
            if (null != headers && headers.size() > 0) {
                for (Map.Entry<String, String> header : headers.entrySet()) {
                    requestBuilder.setHeader(header.getKey(), header.getValue());
                }
            }
            // 採用 post 預設方法必須是 application/x-www-form-urlencoded 的方式夾帶參數
            requestBuilder.setHeader("Content-Type","application/x-www-form-urlencoded;charset=utf-8");
            // 設定 request parameters
            if (null != parameters && parameters.size() > 0) {
                StringBuilder sbd = new StringBuilder();
                boolean isFirst = true;
                for (Map.Entry<String, String> params : parameters.entrySet()) {
                    if(isFirst) { isFirst = false; } else { sbd.append("&"); }
                    sbd.append(URLEncoder.encode(params.getKey(), StandardCharsets.UTF_8));
                    sbd.append("=");
                    sbd.append(URLEncoder.encode(params.getValue(), StandardCharsets.UTF_8));
                }
                // method POST
                requestBuilder.POST(HttpRequest.BodyPublishers.ofString(sbd.toString(), StandardCharsets.UTF_8));
            } else {
                // method POST
                requestBuilder.POST(HttpRequest.BodyPublishers.noBody());
            }
            // 設定請求 URI
            requestBuilder.uri(URI.create(this.url));
        }
        java.net.http.HttpClient.Builder clientBuilder = java.net.http.HttpClient.newBuilder();
        {
            clientBuilder.version(httpVersion);
            clientBuilder.followRedirects(httpRedirect);
            // clientBuilder.executor(ThreadPoolStatic.getInstance());
            if(null != conn_timeout) clientBuilder.connectTimeout(conn_timeout);
        }
        // build & run
        HttpRequest request = new WeakReference<>( requestBuilder.build() ).get();
        java.net.http.HttpClient client = new WeakReference<>( clientBuilder.build() ).get();
        assert null != client;
        asyncRequest(client, request, handler);
    }

    /**
     * POST application/json
     */
    public void postJSON(JSONObject obj, Handler handler) {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder();
        {
            // 設定 request header
            if (null != headers && headers.size() > 0) {
                for (Map.Entry<String, String> header : headers.entrySet()) {
                    requestBuilder.setHeader(header.getKey(), header.getValue());
                }
            }
            // 如果採用 post 預設方法必須是 application/x-www-form-urlencoded 的方式夾帶參數
            // 此方法則採用 raw 內容方式夾帶 JSON 字串
            requestBuilder.setHeader("Content-Type","application/json;charset=utf-8");
            // 設定 request parameters
            if (null != obj) {
                // method POST
                requestBuilder.POST(HttpRequest.BodyPublishers.ofString(obj.toJSONString(), StandardCharsets.UTF_8));
            } else {
                // method POST
                requestBuilder.POST(HttpRequest.BodyPublishers.noBody());
            }
            // 設定請求 URI
            requestBuilder.uri(URI.create(this.url));
        }
        java.net.http.HttpClient.Builder clientBuilder = java.net.http.HttpClient.newBuilder();
        {
            clientBuilder.version(httpVersion);
            clientBuilder.followRedirects(httpRedirect);
            // clientBuilder.executor(ThreadPoolStatic.getInstance());
            if(null != conn_timeout) clientBuilder.connectTimeout(conn_timeout);
        }
        // build & run
        HttpRequest request = new WeakReference<>( requestBuilder.build() ).get();
        java.net.http.HttpClient client = new WeakReference<>( clientBuilder.build() ).get();
        assert null != client;
        asyncRequest(client, request, handler);
    }

    /**
     * POST multipart/form-data
     *
     * 2020-04-16 修正檔案上傳效能及非正常關閉的問題
     */
    public void postFormData(Handler handler) {
        String boundary = "jdk11hc_"+System.currentTimeMillis()+"_"+ RandomServiceStatic.getInstance().getLowerCaseRandomString(8);
        System.out.println(boundary);
        LinkedHashMap<String, Object> bMap = new LinkedHashMap<>();
        if(null != parameters && parameters.size() > 0) {
            for (Map.Entry<String, String> param : parameters.entrySet()) {
                bMap.put(param.getKey(), param.getValue());
            }
        }
        if(null != files && files.size() > 0) {
            for (Map.Entry<String, File> file : files.entrySet()) {
                bMap.put(file.getKey(), file.getValue());
            }
        }
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder();
        {
            requestBuilder.uri(URI.create(this.url));
            requestBuilder.header("Content-Type", "multipart/form-data;boundary=" + boundary);
            requestBuilder.POST(ofMimeMultipartData(bMap, boundary));
        }
        java.net.http.HttpClient.Builder clientBuilder = java.net.http.HttpClient.newBuilder();
        {
            clientBuilder.version(httpVersion);
            clientBuilder.followRedirects(httpRedirect);
            // clientBuilder.executor(ThreadPoolStatic.getInstance());
            if(null != conn_timeout) clientBuilder.connectTimeout(conn_timeout);
        }
        // build & run
        HttpRequest request = new WeakReference<>( requestBuilder.build() ).get();
        java.net.http.HttpClient client = new WeakReference<>( clientBuilder.build() ).get();
        assert client != null;
        asyncRequest(client, request, handler);
    }

    /**
     * https://golb.hplar.ch/2019/01/java-11-http-client.html
     * https://github.com/ralscha/blog2019/blob/master/java11httpclient/client/src/main/java/ch/rasc/httpclient/File.java
     *
     * 2020-04-16 modify by UrielTech.com TomLi
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
     * 藉由非同步方式處理 Response
     */
    private void asyncRequest(java.net.http.HttpClient client, HttpRequest request, Handler handler) {
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
                            if(contentType.contains("text/") || contentType.contains("application/json")) {
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
        // https://github.com/biezhi/java11-examples/blob/master/src/main/java/io/github/biezhi/java11/http/Example.java#L108
        client.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream())
                .whenCompleteAsync((resp, throwable) -> {
                    if(null == resp) {
                        // throwable.printStackTrace();
                        {
                            Bundle b = new Bundle();
                            b.put("status", "exception");
                            b.put("throwable", throwable);
                            Message m = tmp_handler.obtainMessage();
                            m.setData(b);
                            m.sendToTarget();
                        }
                        return;
                    }
                    // response header
                    {
                        LinkedHashMap<String, String> headers = new LinkedHashMap<>();
                        for(Map.Entry<String, List<String>> entry : resp.headers().map().entrySet()) {
                            String key = entry.getKey();
                            List<String> value = entry.getValue();
                            if(value.size() > 1) {
                                int indx = 0;
                                for(String str : entry.getValue()) {
                                    if(0 == indx) {
                                        headers.put(key, str);
                                    } else {
                                        headers.put(key+"_"+indx, str);
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
                            b.put("status_code", String.valueOf(resp.statusCode()));
                            b.put("headers", new WeakReference<>( headers ).get());
                            if(headers.containsKey("content-type")) b.put("content_type", headers.get("content-type"));
                            if(headers.containsKey("content-disposition")) b.put("content_disposition", headers.get("content-disposition"));
                            Message m = tmp_handler.obtainMessage();
                            m.setData(b);
                            m.sendToTarget();
                        }
                    }
                    // response body
                    {
                        Bundle b = new Bundle();
                        b.put("status", "body");
                        b.put("input_stream", resp.body());
                        Message m = tmp_handler.obtainMessage();
                        m.setData(b);
                        m.sendToTarget();
                    }
                })
                .join();
    }

    private void processTextResponse(LinkedHashMap<String, String> resp_header, InputStream resp_body, Handler handler) {
        Bundle b = new Bundle();
        b.put("status", "done");
        if(resp_header.containsKey("content_type")) b.put("content_type", resp_header.get("content_type"));
        b.put("resp_type", "text");
        try( BufferedInputStream bis = new BufferedInputStream( resp_body ) ) {
            b.put("data", new String(bis.readAllBytes(), StandardCharsets.UTF_8));
        } catch (Exception e) {
            b.put("status", "fail");
            e.printStackTrace();
        }
        Message m = handler.obtainMessage();
        m.setData(b);
        m.sendToTarget();
    }

    private void processFileResponse(LinkedHashMap<String, String> resp_header, InputStream resp_body, Handler handler) {
        File tmpFile = null;
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
        try {
            String fileName = sbd.toString();
            tmpFile = new File(tempDirPath+fileName);
            if(tempFileDeleteOnExit) tmpFile.deleteOnExit();
            // 當非檔案操作時可使用 inputStream.transferTo()，
            // 而有牽涉到檔案輸出入時要採用 Files.copy() 確保程式效率
            Files.copy(
                    new BufferedInputStream( resp_body ),
                    tmpFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING
            );
            download_status = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        if(null != tmpFile && download_status) {
            Bundle b = new Bundle();
            b.put("status", "done");
            b.put("resp_type", "file");
            // http response header
            {
                JSONObject obj = new JSONObject();
                for(Map.Entry<String, String> entry : resp_header.entrySet()) {
                    obj.put(entry.getKey(), entry.getValue());
                }
                b.put("headers", obj.toJSONString());
                if (resp_header.containsKey("content_type")) b.put("content_type", resp_header.get("content_type"));
                if (resp_header.containsKey("content_disposition")) b.put("content_disposition", resp_header.get("content_disposition"));
            }
            // 本地端暫存回傳值
            {
                b.put("name", tmpFile.getName());
                b.put("size", tmpFile.length());
                b.put("path", tmpFile.getPath());
            }
            Message m = handler.obtainMessage();
            m.setData(b);
            m.sendToTarget();
        } else {
            Bundle b = new Bundle();
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

        public HttpClient.Builder setUrl(String url) {
            this.url = url;
            return this;
        }

        public HttpClient.Builder setHeaders(LinkedHashMap<String, String> headers) {
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

        public HttpClient.Builder setHeaders(HashMap<String, String> headers) {
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

        public HttpClient.Builder setHeaders(JSONObject headers) {
            HashMap<String, String> map = new HashMap<>();
            for(Map.Entry<String, Object> entry : headers.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue().toString();
                if(null == value) value = "";
                map.put(key, value);
            }
            if(map.size() > 0) this.headers = map;
            return this;
        }

        public HttpClient.Builder setParameters(LinkedHashMap<String, String> parameters) {
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
        public HttpClient.Builder setParameters(HashMap<String, String> parameters) {
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

        public HttpClient.Builder setParameters(JSONObject parameters) {
            LinkedHashMap<String, String> map = new LinkedHashMap<>();
            for(Map.Entry<String, Object> entry : parameters.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue().toString();
                if(null == value) value = "";
                map.put(key, value);
            }
            if(map.size() > 0) this.parameters = map;
            return this;
        }

        public HttpClient.Builder setFiles(LinkedHashMap<String, File> files) {
            LinkedHashMap<String, File> map = new LinkedHashMap<>();
            for(Map.Entry<String, File> entry : files.entrySet()) {
                String key = entry.getKey();
                File value = entry.getValue();
                map.put(key, value);
            }
            if(map.size() > 0) this.files = map;
            return this;
        }

        public HttpClient.Builder setFiles(HashMap<String, File> files) {
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
        public HttpClient.Builder setAlwaysDownload(Boolean alwaysDownload) {
            this.alwaysDownload = alwaysDownload;
            return this;
        }

        /**
         * 設定由 HttpClient 下載的暫存檔案長期存放原則，
         * 若為 false 則表示主程式結束時仍持續保留暫存檔案
         */
        public HttpClient.Builder setTempFileDelete(Boolean deleteOnExit) {
            if(null != deleteOnExit) this.tempFileDeleteOnExit = deleteOnExit;
            return this;
        }

        /**
         * 設定下載檔案的暫存資料夾路徑
         */
        public HttpClient.Builder setTempDirPath(String tempDirPath) {
            this.tempDirPath = tempDirPath;
            return this;
        }

        /**
         * 設定下載檔案的暫存資料夾名稱
         */
        public HttpClient.Builder setTempDirName(String tempDirName) {
            this.tempDirName = tempDirName;
            return this;
        }

        /**
         * https://developer.mozilla.org/en-US/docs/Web/API/URLSearchParams
         * 是否維持網址夾帶 URLSearchParams
         */
        public HttpClient.Builder setKeepUrlSearchParams(boolean keepUrlSearchParams) {
            this.keepUrlSearchParams = keepUrlSearchParams;
            return this;
        }

        /**
         * 設定下載檔案的暫存資料夾名稱
         */
        public HttpClient.Builder setConnectionTimeout(Duration duration) {
            this.conn_timeout = duration;
            return this;
        }

        public HttpClient build() {
            return new HttpClient(
                    this.url,
                    this.headers,
                    this.parameters,
                    this.files,
                    this.alwaysDownload,
                    this.tempFileDeleteOnExit,
                    this.tempDirPath,
                    this.tempDirName,
                    this.keepUrlSearchParams,
                    this.conn_timeout
            );
        }

    }

}
