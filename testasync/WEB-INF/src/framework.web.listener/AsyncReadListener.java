package framework.web.listener;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import framework.file.FileFinder;
import framework.logs.LoggerService;
import framework.observer.Bundle;
import framework.observer.Handler;
import framework.observer.Message;
import framework.random.RandomServiceStatic;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import org.apache.tomcat.util.http.fileupload.MultipartStream;
import org.apache.tomcat.util.http.fileupload.ParameterParser;

import java.io.*;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * #230426 已採用 ChatGPT 重構並確認效率
 * -
 * https://tomcat.apache.org/tomcat-7.0-doc/api/org/apache/tomcat/util/http/fileupload/MultipartStream.html
 * https://medium.com/@clu1022/%E6%B7%BA%E8%AB%87i-o-model-32da09c619e6
 * https://www.slideshare.net/SimoneBordet/servlet-31-async-io
 * https://openhome.cc/Gossip/ServletJSP/ReadListener.html
 * -
 * 當每個獨立的上傳請求希望被非同步處理時，要採用 ReadListener，
 * 當伺服器有空閒的時間可以處理上傳資料時會調用 onDataAvailable，
 * 調用 onDataAvailable 是一個重複性的動作，
 * 直到該請求所有上傳的資料都傳遞完成才會呼叫 onAllDataRead
 */
public class AsyncReadListener implements ReadListener {

    ServletInputStream inputStream;
    String boundary_string;
    Handler handler = null;

    ReadableByteChannel inputChannel;
    FileOutputStream outputStream;
    FileChannel outputChannel;

    Path temp_file_dir;
    String dir_slash = System.getProperty("file.separator");
    File multi_part_file;

    public AsyncReadListener(ServletInputStream servletInputStream, String boundary_string, Handler handler) {
        this.inputStream = new WeakReference<>( servletInputStream ).get();
        if(null != this.inputStream) {
            this.inputChannel = Channels.newChannel(this.inputStream);
        }
        if(null != boundary_string) {
            this.boundary_string = boundary_string;
        }
        if(null != handler) this.handler = handler;
        {
            File app_dir = new FileFinder.Builder().build().find("WEB-INF").getParentFile();
            String app_name = app_dir.getName();
            File app_temp_dir = new FileFinder.Builder().build().find("WebAppFiles");
            if(null == app_temp_dir) {
                LoggerService.logERROR("尚未建立 WebAppFiles 資料夾");
                System.err.println("尚未建立 WebAppFiles 資料夾");
            } else {
                this.temp_file_dir = Paths.get(app_temp_dir + dir_slash + app_name + dir_slash + "temp");
                if(!this.temp_file_dir.toFile().exists()) {
                    this.temp_file_dir.toFile().mkdirs();
                }
            }
        }
        // 設定輸出檔案
        try {
            String file_name = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS")) + "_" + RandomServiceStatic.getInstance().getLowerCaseRandomString(4);
            this.multi_part_file = Paths.get(temp_file_dir + dir_slash + file_name).toFile();
            this.multi_part_file.deleteOnExit(); // temp file delete setting
            this.outputStream = new FileOutputStream(this.multi_part_file);
            this.outputChannel = outputStream.getChannel();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDataAvailable() throws IOException {
        ByteBuffer byteBuffer = ByteBuffer.allocate(1024 * 4);
        while (inputStream.isReady() && !inputStream.isFinished()) {
            byteBuffer.clear();
            int bytesRead = inputChannel.read(byteBuffer);
            byteBuffer.flip();
            if (bytesRead > 0) {
                outputChannel.write(byteBuffer);
            }
        }
    }

    @Override
    public void onAllDataRead() throws IOException {
        JsonArray data_arr = new JsonArray();

        BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(this.multi_part_file));
        FileOutputStream up_output; // output file temp

        byte[] boundaryBytes = (boundary_string).getBytes(StandardCharsets.UTF_8);
        MultipartStream multipartStream = new MultipartStream(inputStream, boundaryBytes, null);
        boolean nextPart = multipartStream.skipPreamble();
        StringBuilder sbd = new StringBuilder();
        while (nextPart) {
            String header = multipartStream.readHeaders();
            {
                header = header.replaceAll("\r", ";");
                header = header.replaceAll("\n", ";");
                header = header.replaceAll("\r\n", ";");
                header = header.replaceAll(":", "=");
                sbd.append(header);
            }
            sbd.append(header);
            String fieldName = getFieldName(header);
            if (fieldName == null) {
                // Skip this part, not a form field
                multipartStream.discardBodyData();
            } else {
                String file_name = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS")) + "_" + RandomServiceStatic.getInstance().getLowerCaseRandomString(4);
                Path output_file_path = Paths.get(this.temp_file_dir + this.dir_slash + file_name);
                File file_upload_temp = output_file_path.toFile();
                file_upload_temp.deleteOnExit();
                up_output = new FileOutputStream(file_upload_temp);
                multipartStream.readBodyData(up_output);
                up_output.flush();
                up_output.close();
                {
                    ParameterParser parameterParser = new ParameterParser();
                    parameterParser.setLowerCaseNames(true);
                    Map<String, String> params = parameterParser.parse(sbd.toString(), new char[] {';', ','});
                    params.put("file_path", output_file_path.toString());
                    sbd.delete(0, sbd.length()); // reset string
                    {
                        JsonObject obj = new JsonObject();
                        for(Map.Entry<String, String> entry : params.entrySet()) {
                            obj.addProperty(entry.getKey(), entry.getValue());
                        }
                        data_arr.add(obj);
                    }
                }
            }
            nextPart = multipartStream.readBoundary();
        }

        closeStream();
        if(null != handler) {
            Bundle b = new Bundle();
            b.putString("status", "done");
            b.putString("data", new Gson().toJson(data_arr));
            Message m = handler.obtainMessage();
            m.setData(b);
            m.sendToTarget();
        }
    }

    @Override
    public void onError(Throwable throwable) {
        throwable.printStackTrace();
        if(null != handler) {
            Bundle b = new Bundle();
            b.putString("status", "fail");
            Message m = handler.obtainMessage();
            m.setData(b);
            m.sendToTarget();
        }
    }

    private String getFieldName(String header) {
        Pattern pattern = Pattern.compile("name=\"(.+?)\"");
        Matcher matcher = pattern.matcher(header);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private void closeStream() {
        try {
            inputChannel.close();
            inputStream.close();
            outputChannel.close();
            outputStream.close();
        } catch (Exception e) {
            // e.printStackTrace();
        }
    }

}
