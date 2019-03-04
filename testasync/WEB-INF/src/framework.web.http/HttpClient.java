package framework.web.http;

import com.alibaba.fastjson.JSONObject;
import framework.hash.HashServiceStatic;
import framework.observer.Bundle;
import framework.observer.Handler;
import framework.observer.Message;
import framework.random.RandomServiceStatic;
import framework.setting.AppSetting;

import java.io.*;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * implement by UrielTech.com TomLi.
 * 基於 java.net.http.HttpClient 的功能實作封裝
 * 並模擬類似 OkHttp 方式處理
 * required JDK 11+
 */
public class HttpClient {

    private String url;
    private String getParamsStr;
    private HashMap<String, String> headers;
    private HashMap<String, String> parameters;
    private HashMap<String, File> files;
    private boolean alwaysDownload;
    private final String tempDirName = "[temp]";

    private HttpClient(String url, HashMap<String, String> headers, HashMap<String, String> parameters, HashMap<String, File> files, Boolean alwaysDownload) {
        this.url = null;
        {
            if(url.contains("?")) {
                String[] tmp = url.split("\\?");
                url = tmp[0];
                getParamsStr = tmp[1];
            }
            StringBuilder sbd = new StringBuilder();
            if(url.contains("://")) {
                String[] tmpSch = url.split("://");
                sbd.append(tmpSch[0]);
                sbd.append("://");
                String[] tmp = tmpSch[1].split("/");
                boolean isDomain = true;
                for(String str : tmp) {
                    if(isDomain) {
                        sbd.append(URLEncoder.encode(str, StandardCharsets.UTF_8));
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
            this.url = sbd.toString();
        }
        this.headers = headers;
        this.parameters = parameters;
        this.files = files;
        this.alwaysDownload = Objects.requireNonNullElse(alwaysDownload, false);
    }

    /**
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
                HashMap<String, String> map = new HashMap<>();
                StringBuilder tmp = new StringBuilder();
                String prevKey = null;
                for(int i = 0, len = getParamsStr.length(); i < len; i++) {
                    String s = String.valueOf(getParamsStr.charAt(i));
                    if(mode && s.equals("=")) {
                        prevKey = tmp.toString();
                        map.put(prevKey, "");
                        tmp.delete(0, tmp.length());
                        mode = false;
                        continue;
                    }
                    if(!mode && s.equals("&")) {
                        map.put(prevKey, tmp.toString());
                        tmp.delete(0, tmp.length());
                        mode = true;
                        continue;
                    }
                    tmp.append(s);
                    if(i == getParamsStr.length() - 1) {
                        map.put(prevKey, tmp.toString());
                        tmp.delete(0, tmp.length());
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
            // 設定 Method
            requestBuilder.GET();
            // 設定請求 URI
            requestBuilder.uri(URI.create(sbd.toString()));
        }
        java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                .version(java.net.http.HttpClient.Version.HTTP_2)
                .followRedirects(java.net.http.HttpClient.Redirect.NORMAL)
                .build();
        HttpRequest request = requestBuilder.build();
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
                requestBuilder.POST(HttpRequest.BodyPublishers.ofString(sbd.toString(), StandardCharsets.UTF_8));
            } else {
                requestBuilder.POST(HttpRequest.BodyPublishers.noBody());
            }
            // 設定請求 URI
            requestBuilder.uri(URI.create(this.url));
        }
        java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                .version(java.net.http.HttpClient.Version.HTTP_2)
                .followRedirects(java.net.http.HttpClient.Redirect.NORMAL)
                .build();
        HttpRequest request = requestBuilder.build();
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
                requestBuilder.POST(HttpRequest.BodyPublishers.ofString(obj.toJSONString(), StandardCharsets.UTF_8));
            } else {
                requestBuilder.POST(HttpRequest.BodyPublishers.noBody());
            }
            // 設定請求 URI
            requestBuilder.uri(URI.create(this.url));
        }
        java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                .version(java.net.http.HttpClient.Version.HTTP_2)
                .followRedirects(java.net.http.HttpClient.Redirect.NORMAL)
                .build();
        HttpRequest request = requestBuilder.build();
        asyncRequest(client, request, handler);
    }

    /**
     * POST multipart/form-data
     */
    public void postFormData(Handler handler) {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder();
        {
            // 設定 request header
            if (null != headers && headers.size() > 0) {
                for (Map.Entry<String, String> header : headers.entrySet()) {
                    requestBuilder.setHeader(header.getKey(), header.getValue());
                }
            }
            // 採用 post 預設方法必須是 application/x-www-form-urlencoded 的方式夾帶參數
            final String boundary = "----" + HashServiceStatic.getInstance().stringToSHA256(RandomServiceStatic.getInstance().getTimeHash(6));
            requestBuilder.setHeader("Content-Type","multipart/form-data;boundary="+boundary);
            // 製作暫存檔案 -> 要被轉換為上傳的 InputStream 內容
            File tmpFile = null;
            {
                try {
                    tmpFile = File.createTempFile(
                            RandomServiceStatic.getInstance().getTimeHash(6),
                            null,
                            new File(new AppSetting.Builder().setAppName(tempDirName).build().getPathContext().getTempDirPath()));
                    tmpFile.deleteOnExit();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            // FileOutput
            BufferedOutputStream outputStream = null;
            {
                assert null != tmpFile;
                try {
                    outputStream = new BufferedOutputStream(new FileOutputStream(tmpFile, true));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            {
                assert null != outputStream;
                byte[] eol_byte = { 13, 10 }; // 換行符號
                try {
                    // 設定 request parameters
                    if (null != parameters && parameters.size() > 0) {
                        for (Map.Entry<String, String> param : parameters.entrySet()) {
                            outputStream.write(("--" + boundary).getBytes(StandardCharsets.UTF_8));
                            outputStream.write(eol_byte);
                            String key = param.getKey();
                            outputStream.write(("Content-Disposition: form-data; name=\""+key+"\"").getBytes(StandardCharsets.UTF_8));
                            outputStream.write(eol_byte);
                            outputStream.write(eol_byte);
                            String value = param.getValue();
                            outputStream.write(value.getBytes(StandardCharsets.UTF_8));
                            outputStream.write(eol_byte);
                        }
                    }
                    // 設定 upload files
                    if (null != files && files.size() > 0) {
                        for (Map.Entry<String, File> fileOpt : files.entrySet()) {
                            outputStream.write(("--" + boundary).getBytes(StandardCharsets.UTF_8));
                            outputStream.write(eol_byte);
                            String key = fileOpt.getKey();
                            outputStream.write(("Content-Disposition: form-data; name=\""+key+"\"").getBytes(StandardCharsets.UTF_8));
                            outputStream.write(eol_byte);
                            outputStream.write(eol_byte);
                            File file = fileOpt.getValue();
                            Files.copy(file.toPath(), outputStream);
                            outputStream.write(eol_byte);
                        }
                    }
                    // multipart 結尾符號
                    outputStream.write(("--" + boundary + "--").getBytes(StandardCharsets.UTF_8));
                    outputStream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            // 設定上傳的 MultiPart 內容 by InputStream
            {
                final File fTmpFile = tmpFile;
                requestBuilder.POST(HttpRequest.BodyPublishers.ofInputStream(() -> {
                    InputStream inputStream = null;
                    try {
                        inputStream = new BufferedInputStream(new FileInputStream(fTmpFile));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return inputStream;
                }));
            }
            // 設定請求 URI
            requestBuilder.uri(URI.create(this.url));
        }
        java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                .version(java.net.http.HttpClient.Version.HTTP_2)
                .followRedirects(java.net.http.HttpClient.Redirect.NORMAL)
                .build();
        HttpRequest request = requestBuilder.build();
        asyncRequest(client, request, handler);
    }

    /**
     * 藉由非同步方式處理 Response
     */
    private void asyncRequest(java.net.http.HttpClient client, HttpRequest request, Handler handler) {
        Handler tmp_handler;
        {
            HashMap<String, String> info = new HashMap<>();
            tmp_handler = new Handler() {
                @Override
                public void handleMessage(Message m) {
                    super.handleMessage(m);
                    String status = m.getData().getString("status");
                    if("info".equals(status)) {
                        info.put("status_code", m.getData().getString("status_code"));
                        info.put("content_type", m.getData().getString("content_type"));
                    }
                    if("body".equals(status)) {
                        InputStream inputStream = (InputStream) m.getData().get("input_stream");
                        String contentType = info.get("content_type");
                        if(null == contentType || contentType.length() == 0) {
                            processTextResponse(inputStream, handler);
                        } else {
                            if (contentType.contains("text/") || contentType.contains("application/json")) {
                                if (alwaysDownload) {
                                    processFileResponse(inputStream, handler);
                                } else {
                                    processTextResponse(inputStream, handler);
                                }
                            } else {
                                processFileResponse(inputStream, handler);
                            }
                        }
                    }
                }
            };
        }
        // 藉由 Future 接收非同步回傳結果
        CompletableFuture<InputStream> responseFuture = client
                .sendAsync(request, HttpResponse.BodyHandlers.ofInputStream())
                .thenApply(response -> {
                    String statusCode = String.valueOf(response.statusCode());
                    String contentType = null;
                    if(response.headers().map().containsKey("content-type")) {
                        contentType = String.valueOf(response.headers().map().get("content-type").get(0));
                    }
                    // 第一次回傳 response header 內容
                    {
                        Bundle b = new Bundle();
                        b.put("status", "info");
                        b.put("status_code", statusCode);
                        b.put("content_type", contentType);
                        Message m = tmp_handler.obtainMessage();
                        m.setData(b);
                        m.sendToTarget();
                    }
                    return response;
                })
                .thenApply(HttpResponse::body); // 第二次回傳 response body
        try {
            Bundle b = new Bundle();
            b.put("status", "body");
            // 最後一次 handler 提交一定要由 future.get() 之後送出，要不然會有 InterruptedException 卡死的問題
            b.put("input_stream", responseFuture.get());
            Message m = tmp_handler.obtainMessage();
            m.setData(b);
            m.sendToTarget();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void processTextResponse(InputStream inputStream, Handler handler) {
        BufferedInputStream bis = new BufferedInputStream(inputStream);
        {
            Bundle b = new Bundle();
            b.put("status", "done");
            b.put("resp_type", "text");
            try {
                b.put("data", new String(bis.readAllBytes(), StandardCharsets.UTF_8));
                bis.close(); // flush and close input stream
            } catch (Exception e) {
                e.printStackTrace();
            }
            Message m = handler.obtainMessage();
            m.setData(b);
            m.sendToTarget();
        }
    }

    private void processFileResponse(InputStream inputStream, Handler handler) {
        File tmpFile = null;
        boolean download_status = false;
        try {
            tmpFile = File.createTempFile(
                    RandomServiceStatic.getInstance().getTimeHash(8),
                    null,
                    new File(new AppSetting.Builder().setAppName(tempDirName).build().getPathContext().getTempDirPath()));
            tmpFile.deleteOnExit();
            // 當非檔案操作時可使用 inputStream.transferTo()，而有牽涉到檔案輸出入時要採用 Files.copy() 確保程式效率
            Files.copy(new BufferedInputStream(inputStream), tmpFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            download_status = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        if(null != tmpFile && download_status) {
            Bundle b = new Bundle();
            b.put("status", "done");
            b.put("resp_type", "file");
            b.put("name", tmpFile.getName());
            b.put("size", tmpFile.length());
            b.put("path", tmpFile.getPath());
            b.put("download_status", "true");
            Message m = handler.obtainMessage();
            m.setData(b);
            m.sendToTarget();
        } else {
            Bundle b = new Bundle();
            b.put("status", "fail");
            b.put("resp_type", "file");
            b.put("download_status", "false");
            Message m = handler.obtainMessage();
            m.setData(b);
            m.sendToTarget();
        }
    }

    public static class Builder {

        private String url = null;
        private HashMap<String, String> headers = null;
        private HashMap<String, String> parameters = null;
        private HashMap<String, File> files = null;
        private Boolean alwaysDownload = false;

        public HttpClient.Builder setUrl(String url) {
            this.url = url;
            return this;
        }

        public HttpClient.Builder setHeaders(Map<String, String> headers) {
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

        public HttpClient.Builder setParameters(Map<String, String> parameters) {
            HashMap<String, String> map = new HashMap<>();
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
            HashMap<String, String> map = new HashMap<>();
            for(Map.Entry<String, Object> entry : parameters.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue().toString();
                if(null == value) value = "";
                map.put(key, value);
            }
            if(map.size() > 0) this.parameters = map;
            return this;
        }

        public HttpClient.Builder setFiles(Map<String, File> files) {
            HashMap<String, File> map = new HashMap<>();
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

        public HttpClient build() {
            return new HttpClient(this.url, this.headers, this.parameters, this.files, this.alwaysDownload);
        }

    }

}
