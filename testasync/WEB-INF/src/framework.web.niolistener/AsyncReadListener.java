package framework.web.niolistener;

import framework.observer.Bundle;
import framework.observer.Handler;
import framework.observer.Message;
import framework.random.RandomServiceStatic;
import framework.web.multipart.FileItemList;
import framework.web.multipart.MultiPartParser;

import javax.servlet.AsyncContext;
import javax.servlet.ReadListener;
import javax.servlet.ServletContext;
import javax.servlet.ServletInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.ref.WeakReference;

/**
 * 當每個獨立的上傳請求希望被非同步處理時，要採用 ReadListener，
 * 當伺服器有空閒的時間可以處理上傳資料時會調用 onDataAvailable，
 * 調用 onDataAvailable 是一個重複性的動作，
 * 直到該請求所有上傳的資料都傳遞完成才會呼叫 onAllDataRead
 * https://openhome.cc/Gossip/ServletJSP/ReadListener.html
 */
public class AsyncReadListener implements ReadListener {

    private final boolean devMode = false;

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
            String fileName = RandomServiceStatic.getInstance().getTimeHash(16).toUpperCase();
            String dirSlash;
            {
                String hostOS = System.getProperty("os.name");
                dirSlash = System.getProperty("file.separator");
                if(hostOS.toLowerCase().contains("windows")) { dirSlash = "\\\\"; }
            }
            try {
                this.targetFile = File.createTempFile(fileName, null, new File(servletContext.getAttribute(ServletContext.TEMPDIR).toString() + dirSlash));
                this.targetFile.deleteOnExit();
            } catch (Exception e) {
                if(devMode) { e.printStackTrace(); }
            }
        }
        {
            try {
                inputStream = new WeakReference<>( asyncContext.getRequest().getInputStream() ).get();
            } catch (Exception e) {
                if(devMode) { e.printStackTrace(); }
            }
            try {
                assert null != this.targetFile;
                outputStream = new WeakReference<>( new BufferedOutputStream( new FileOutputStream( targetFile ) ) ).get();
            } catch (Exception e) {
                if(devMode) { e.printStackTrace(); }
            }
        }
    }

    /**
     * ReadListener 監聽進度將藉由 inputStream 被 read() 到哪部分為主，
     * 需注意若此處不執行 inputStream.read() 則會永遠到不了 onAllDataRead() 狀態之中
     * 2019-06-19 修正 while 處理邏輯及中斷條件
     */
    @Override
    public void onDataAvailable() {
        byte[] buffer = new byte[4096];
        try {
            // onAllDataRead 需要由 inputStream.read 所有 byte 之後才會被觸發
            int length;
            while (true) {
                if(null == inputStream) break;
                if(!inputStream.isReady()) break;
                length = inputStream.read(buffer);
                if(0 > length) break;
                if(null == outputStream) break;
                outputStream.write(buffer, 0, length);
                Thread.onSpinWait();
            }
        } catch (Exception e) {
            if(devMode) { e.printStackTrace(); }
        }
    }

    @Override
    public void onAllDataRead() {
        closeStream();
        FileItemList fileItemList = null;
        {
            if(null != targetFile && targetFile.exists()) {
                fileItemList = new MultiPartParser.Builder()
                        .setFile(targetFile)
                        .setRepository(null)
                        .setServletContext(servletContext)
                        .setAsyncContext(asyncContext)
                        .build()
                        .parse();
            }
        }
        if(null != fileItemList) {
            String key = "list_file_item";
            String type = "ArrayList";
            Bundle b = new Bundle();
            b.putString("status", "done");
            b.putString("key", key);
            b.putString("type", type);
            b.put(key, fileItemList);
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
        closeStream();
        if(devMode) { throwable.printStackTrace(); }
        {
            Bundle b = new Bundle();
            b.putString("status", "fail");
            b.putString("msg", throwable.getMessage());
            Message m = handler.obtainMessage();
            m.setData(b);
            m.sendToTarget();
        }
    }

    private void closeStream() {
        try {
            inputStream.close();
            outputStream.close();
        } catch (Exception e) {
            // e.printStackTrace();
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
