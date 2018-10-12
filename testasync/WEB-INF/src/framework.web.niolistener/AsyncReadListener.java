package framework.web.niolistener;

import framework.observer.Bundle;
import framework.observer.Handler;
import framework.observer.Message;
import framework.random.RandomServiceStatic;
import framework.web.multipart.FileItem;
import framework.web.multipart.MultiPartParser;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.apache.tomcat.util.http.fileupload.disk.DiskFileItemFactory;

import javax.servlet.AsyncContext;
import javax.servlet.ReadListener;
import javax.servlet.ServletContext;
import javax.servlet.ServletInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;

/**
 * 當每個獨立的上傳請求希望被非同步處理時，要採用 ReadListener，
 * 當伺服器有空閒的時間可以處理上傳資料時會調用 onDataAvailable，
 * 調用 onDataAvailable 是一個重複性的動作，
 * 直到該請求所有上傳的資料都傳遞完成才會呼叫 onAllDataRead
 * https://openhome.cc/Gossip/ServletJSP/ReadListener.html
 */
public class AsyncReadListener implements ReadListener {

    private ServletContext servletContext;
    private AsyncContext asyncContext;
    private Handler handler;

    private File targetFile;
    private ServletInputStream inputStream = null;
    private BufferedOutputStream outputStream = null;

    private AsyncReadListener(ServletContext servletContext, AsyncContext asyncContext, Handler handler) {
        this.servletContext = servletContext;
        this.asyncContext = asyncContext;
        this.handler = handler;
        {
            String fileName = RandomServiceStatic.getInstance().getTimeHash(8);
            String dirSlash = System.getProperty("file.separator");
            try {
                this.targetFile = File.createTempFile(fileName, null, new File(servletContext.getAttribute(ServletContext.TEMPDIR).toString() + dirSlash));
                this.targetFile.deleteOnExit();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        {
            try {
                inputStream = asyncContext.getRequest().getInputStream();
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                assert null != this.targetFile;
                outputStream = new BufferedOutputStream(new FileOutputStream(targetFile));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * ReadListener 監聽進度將藉由 inputStream 被 read() 到哪部分為主，
     * 若不在此處執行 inputStream.read() 則永遠到不了 onAllDataRead() 之中
     */
    @Override
    public void onDataAvailable() {
        byte[] buffer = new byte[DiskFileItemFactory.DEFAULT_SIZE_THRESHOLD];
        int length;
        try {
            // onAllDataRead 需要由 inputStream.read 所有 byte 之後才會被觸發
            while (inputStream.isReady() && (length = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, length);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onAllDataRead() {
        IOUtils.closeQuietly(outputStream);
        ArrayList<FileItem> fileItems = null;
        {
            if(null != targetFile && targetFile.exists()) {
                fileItems = new MultiPartParser.Builder()
                        .setFile(targetFile)
                        .setRepository(null)
                        .setServletContext(servletContext)
                        .setAsyncContext(asyncContext)
                        .build()
                        .parse();
            }
        }
        if(null != fileItems) {
            String key = "list_file_item";
            String type = "ArrayList";
            Bundle b = new Bundle();
            b.putString("status", "done");
            b.putString("key", key);
            b.putString("type", type);
            b.put(key, fileItems);
            Message m = handler.obtainMessage();
            m.setData(b);
            m.sendToTarget();
        } else {
            Bundle b = new Bundle();
            b.putString("status", "fail");
            Message m = handler.obtainMessage();
            m.setData(b);
            m.sendToTarget();
        }
    }

    @Override
    public void onError(Throwable throwable) {
        throwable.printStackTrace();
        {
            Bundle b = new Bundle();
            b.putString("status", "fail");
            b.putString("msg", throwable.getMessage());
            Message m = handler.obtainMessage();
            m.setData(b);
            m.sendToTarget();
        }
    }

    public static class Builder {

        private ServletContext servletContext;
        private AsyncContext asyncContext;
        private Handler handler;

        public AsyncReadListener.Builder setServletContext(ServletContext servletContext) {
            this.servletContext = servletContext;
            return this;
        }

        public AsyncReadListener.Builder setAsyncContext(AsyncContext asyncContext) {
            this.asyncContext = asyncContext;
            return this;
        }

        public AsyncReadListener.Builder setHandler(Handler handler) {
            this.handler = handler;
            return this;
        }

        public AsyncReadListener build() {
            return new AsyncReadListener(servletContext, asyncContext, handler);
        }

    }

}
