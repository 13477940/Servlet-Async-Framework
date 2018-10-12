package framework.web.http;

import com.alibaba.fastjson.JSONObject;
import framework.observer.Bundle;
import framework.observer.Handler;
import framework.observer.Message;
import framework.setting.AppSetting;
import okhttp3.*;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.apache.tomcat.util.http.fileupload.disk.DiskFileItemFactory;

import javax.activation.MimetypesFileTypeMap;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HttpClient {

    private String url;
    private HashMap<String, String> headers;
    private HashMap<String, String> parameters;
    private ArrayList<File> files;
    private Boolean alwaysDownload; // 確認是否總是當作下載檔案使用

    private HttpClient(String url, HashMap<String, String> headers, HashMap<String, String> parameters, ArrayList<File> files, Boolean alwaysDownload) {
        this.url = url;
        this.headers = headers;
        this.parameters = parameters;
        this.files = files;
        this.alwaysDownload = alwaysDownload;
    }

    public void get(Handler handler) {
        useGet(this.url, this.headers, this.parameters, handler);
    }

    public void post(Handler handler) {
        usePostUrlEncoded(this.url, this.headers, this.parameters, handler);
    }

    public void postFormData(Handler handler) {
        usePost(this.url, this.headers, this.parameters, this.files, handler);
    }

    public void postJSON(JSONObject jsonObject, Handler handler) {
        usePostJSON(this.url, this.headers, jsonObject, handler);
    }

    // HTTP GET - x-www-form-urlencoded
    private void useGet(String url, HashMap<String, String> headers, HashMap<String, String> parameters, Handler handler) {
        OkHttpClient client = OkHttpClientStatic.getInstance();
        // 檢查 URL 解析
        HttpUrl httpUrl = HttpUrl.parse(url);
        if(null == httpUrl) {
            try {
                throw new Exception("解析 HTTP URL 發生錯誤");
            } catch (Exception e) {
                e.printStackTrace();
            }
            return; // 中斷
        }
        HttpUrl.Builder urlBuilder = httpUrl.newBuilder();
        {
            // 如果為沒有帶參數的 GET 請求網址，但有附帶參數資訊時，幫忙處理參數代入
            if(null != parameters && parameters.size() > 0) {
                for(Map.Entry<String, String> entry : parameters.entrySet()) {
                    try {
                        urlBuilder.addQueryParameter(entry.getKey(), entry.getValue());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        String _url = urlBuilder.build().toString();
        {
            Request.Builder requestBuilder = new Request.Builder();
            {
                if(null != headers) {
                    for (Map.Entry<String, String> header : headers.entrySet()) {
                        requestBuilder.header(header.getKey(), header.getValue());
                    }
                }
            }
            Request request = requestBuilder.url(_url).build();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    processFailResponse(e, handler);
                }
                @Override
                public void onResponse(Call call, Response response) {
                    processResponse(response, handler);
                }
            });
        }
    }

    // HTTP POST - application/json
    private void usePostJSON(String url, HashMap<String, String> headers, JSONObject obj, Handler handler) {
        OkHttpClient client = OkHttpClientStatic.getInstance();
        String charset = StandardCharsets.UTF_8.name();
        RequestBody body = RequestBody.create(MediaType.parse("application/json; charset="+charset), obj.toJSONString());
        Request.Builder requestBuilder = new Request.Builder();
        {
            if(null != headers) {
                for (Map.Entry<String, String> header : headers.entrySet()) {
                    requestBuilder.header(header.getKey(), header.getValue());
                }
            }
        }
        Request request = requestBuilder.url(url).post(body).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                processFailResponse(e, handler);
            }
            @Override
            public void onResponse(Call call, Response response) {
                processResponse(response, handler);
            }
        });
    }

    // HTTP POST - x-www-form-urlencoded
    private void usePostUrlEncoded(String url, HashMap<String, String> headers, HashMap<String, String> parameters, Handler handler) {
        OkHttpClient client = OkHttpClientStatic.getInstance();
        FormBody.Builder formBodyBuilder = new FormBody.Builder();
        {
            if(null != parameters && parameters.size() > 0) {
                for (Map.Entry<String, String> entry : parameters.entrySet()) {
                    formBodyBuilder.add(entry.getKey(), entry.getValue());
                }
            }
        }
        {
            Request.Builder requestBuilder = new Request.Builder();
            {
                if(null != headers) {
                    for (Map.Entry<String, String> header : headers.entrySet()) {
                        requestBuilder.header(header.getKey(), header.getValue());
                    }
                }
            }
            Request request = requestBuilder.url(url).post(formBodyBuilder.build()).build();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    processFailResponse(e, handler);
                }
                @Override
                public void onResponse(Call call, Response response) {
                    processResponse(response, handler);
                }
            });
        }
    }

    // HTTP POST - multipart/form-data
    private void usePost(String url, HashMap<String, String> headers, HashMap<String, String> parameters, ArrayList<File> files, Handler handler) {
        OkHttpClient client = OkHttpClientStatic.getInstance();
        MultipartBody.Builder fileBodyBuilder = new MultipartBody.Builder();
        {
            if(null != files) {
                for(File file : files) {
                    MediaType mediaType = MediaType.parse(parseFileMimeString(file));
                    String fSSID = String.valueOf(System.currentTimeMillis());
                    fileBodyBuilder.addFormDataPart(fSSID, file.getName(), RequestBody.create(mediaType, file));
                }
            }
        }
        {
            if(null != parameters) {
                for(Map.Entry<String, String> entry : parameters.entrySet()) {
                    fileBodyBuilder.addFormDataPart(entry.getKey(), entry.getValue());
                }
            }
        }
        {
            RequestBody requestBody = fileBodyBuilder.build();
            Request.Builder requestBuilder = new Request.Builder();
            {
                if(null != headers) {
                    for (Map.Entry<String, String> header : headers.entrySet()) {
                        requestBuilder.header(header.getKey(), header.getValue());
                    }
                }
            }
            Request request = requestBuilder.url(url).post(requestBody).build();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    processFailResponse(e, handler);
                }
                @Override
                public void onResponse(Call call, Response response) {
                    processResponse(response, handler);
                }
            });
        }
    }

    // 在此處判斷回傳值類型為何種，檔案 or 純文字
    private void processResponse(Response response, Handler handler) {
        ResponseBody respBody = response.body();
        if(null != respBody) {
            MediaType mediaType = respBody.contentType();
            if(null != mediaType) {
                String type = mediaType.toString();
                // 如果是 txt 或 json 檔案要檢查是否仍需要當作檔案被下載成二進位內容
                if(type.contains("text/") || type.contains("application/json")) {
                    if(alwaysDownload) {
                        processFileRespData(response, respBody, handler);
                    } else {
                        processTextRespData(response, respBody, handler);
                    }
                } else {
                    processFileRespData(response, respBody, handler);
                }
            } else {
                processTextRespData(response, respBody, handler);
            }
        } else {
            Bundle b = new Bundle();
            if (200 == response.code()) {
                b.put("status", "done");
            } else {
                b.put("status", "fail");
            }
            b.put("code", response.code());
            b.put("data", new JSONObject());
            b.put("response", response);
            Message m = handler.obtainMessage();
            m.setData(b);
            m.sendToTarget();
        }
    }

    // 處理純文字類型的回傳內容
    private void processTextRespData(Response response, ResponseBody respBody, Handler handler) {
        Bundle b = new Bundle();
        if (200 == response.code()) {
            b.put("status", "done");
        } else {
            b.put("status", "fail");
        }
        b.put("code", response.code());
        b.put("data", inputSteamToString(respBody.byteStream()));
        b.put("response", response);
        Message m = handler.obtainMessage();
        m.setData(b);
        m.sendToTarget();
        // clear response
        {
            respBody.close();
            response.close();
        }
    }

    // 處理檔案類型的回傳內容
    private void processFileRespData(Response response, ResponseBody respBody, Handler handler) {
        InputStream inStream = respBody.byteStream();
        File tmpFile = null;
        boolean download_status = false;
        try {
            tmpFile = new File(new AppSetting.Builder().build().getPathContext().getTempDirPath()+"temp_"+System.currentTimeMillis());
            OutputStream outStream = new FileOutputStream(tmpFile);
            {
                byte[] buffer = new byte[8 * 1024];
                int bytesRead;
                while ((bytesRead = inStream.read(buffer)) != -1) {
                    outStream.write(buffer, 0, bytesRead);
                }
                outStream.flush();
                IOUtils.closeQuietly(inStream);
                IOUtils.closeQuietly(outStream);
            }
            download_status = true;
        } catch (Exception e) {
            e.printStackTrace();
            // download_status = false;
            // clear response
            {
                respBody.close();
                response.close();
            }
        }
        {
            Bundle b = new Bundle();
            if (200 == response.code()) {
                b.put("status", "done");
            } else {
                b.put("status", "fail");
            }
            b.put("download_status", download_status); // 確認是否下載完成
            b.put("code", response.code());
            if(null != tmpFile && tmpFile.exists()) {
                JSONObject resObj = new JSONObject();
                resObj.put("name", tmpFile.getName());
                resObj.put("size", tmpFile.length());
                resObj.put("path", tmpFile.getPath());
                b.put("data", resObj.toJSONString());
            } else {
                b.put("data", new JSONObject());
            }
            b.put("response", response);
            Message m = handler.obtainMessage();
            m.setData(b);
            m.sendToTarget();
        }
        // clear response
        {
            respBody.close();
            response.close();
        }
    }

    private void processFailResponse(IOException e, Handler handler) {
        if (null == handler) {
            e.printStackTrace();
        } else {
            e.printStackTrace();
            Bundle b = new Bundle();
            b.put("status", "fail");
            b.put("IOException", e);
            Message m = handler.obtainMessage();
            m.setData(b);
            m.sendToTarget();
        }
    }

    private String parseFileMimeString(File file) {
        String res = null;
        try {
            // fileMIME = URLConnection.guessContentTypeFromName(file.getName());
            // JDK 7+
            res = Files.probeContentType(Paths.get(file.getPath()));
            // MimetypesFileTypeMap need javax.activation.jar
            if (null == res) res = new MimetypesFileTypeMap().getContentType(file);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return res;
    }

    private String inputSteamToString(InputStream inputStream) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[DiskFileItemFactory.DEFAULT_SIZE_THRESHOLD];
        int length;
        String res = null;
        try {
            while ((length = inputStream.read(buffer)) != -1) {
                out.write(buffer, 0 , length);
            }
            out.flush();
            res = out.toString(StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
        }
        IOUtils.closeQuietly(inputStream);
        IOUtils.closeQuietly(out);
        return res;
    }

    public static class Builder {

        private String url = null;
        private HashMap<String, String> headers = null;
        private HashMap<String, String> parameters = null;
        private ArrayList<File> files = null;
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

        public HttpClient.Builder setFiles(List<File> files) {
            ArrayList<File> _files = new ArrayList<>(files);
            if(_files.size() > 0) this.files = _files;
            return this;
        }

        /**
         * 因應純文字內容可能會被當作一般訊息回傳至 Handler，
         * 若是要採用檔案操作則需要強制啟用下載模式把純文字檔下載為檔案型態
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
