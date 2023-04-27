package framework.web.listener;

import framework.observer.Bundle;
import framework.observer.Handler;
import framework.observer.Message;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;

import java.io.*;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;

/**
 * https://medium.com/@clu1022/%E6%B7%BA%E8%AB%87i-o-model-32da09c619e6
 * https://www.slideshare.net/SimoneBordet/servlet-31-async-io
 * https://openhome.cc/Gossip/ServletJSP/WriteListener.html
 * -
 * 當每個獨立輸出的內容希望被非同步輸出時，要封裝於 WriteListener 才能確保完整的輸出，
 * 因為 onWritePossible 和 ServletOutputStream.isReady() 兩者狀態是持續變動的，
 * 例如寫入過長的（例如 2MiB 純文字）文檔或檔案於 onWritePossible 中處理時，
 * 會發現無法完整的輸出並且跳出 while isReady 迴圈，這是因為 onWritePossible 本身的狀態已經被改變了，
 * 所以必須由整體 WriteListener 重複的狀態進行判斷，那麼初始化的動作勢必不能寫在 onWritePossible 之中，
 * 並且判斷一個輸出事件的結束點也必須由 onWritePossible 是否已經寫完所有資料來決定。
 * -
 * #230427 採用 ChatGPT 重構並確認效率，應該確保此實作為最簡化才是正確的實作方式，複雜的判斷應實作在應用端
 */
public class AsyncWriteListener implements WriteListener {

    private ServletOutputStream servletOutputStream;
    private InputStream inputStream;
    private final Handler handler;

    // from InputStream
    private AsyncWriteListener(ServletOutputStream servletOutputStream, InputStream inputStream, Handler handler) {
        this.servletOutputStream = servletOutputStream;
        {
            try {
                this.inputStream = new WeakReference<>( new BufferedInputStream( inputStream )).get();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        this.handler = handler;
    }

    // from String
    private AsyncWriteListener(ServletOutputStream servletOutputStream, CharSequence charSequence, Handler handler) {
        this.servletOutputStream = servletOutputStream;
        {
            try {
                byte[] bytes = charSequence.toString().getBytes(StandardCharsets.UTF_8);
                this.inputStream = new WeakReference<>( new ByteArrayInputStream(bytes) ).get();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        this.handler = handler;
    }

    // from File
    private AsyncWriteListener(ServletOutputStream servletOutputStream, File file, Handler handler) {
        this.servletOutputStream = servletOutputStream;
        {
            try {
                this.inputStream = new WeakReference<>( new BufferedInputStream(new FileInputStream(file)) ).get();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        this.handler = handler;
    }

    /**
     * 非同步輸出實作
     * 2019-06-17 修改 while 處理邏輯及中斷條件，解決 CPU 高使用率的問題
     * 2019-12-31 取消自旋鎖
     * 2020-04-13 修正 Buffer Size 為 inputStream.available() 自動取值
     * 2020-05-05 修正 inputStream.available() 會達到物理記憶體最大值造成溢位的問題
     */
    @Override
    public void onWritePossible() throws IOException {
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ( servletOutputStream.isReady() && (bytesRead = inputStream.read(buffer)) != -1 ) {
            // 在 response 對象可寫入數據時，持續向輸出流寫入數據
            servletOutputStream.write(buffer, 0, bytesRead);
            servletOutputStream.flush();
        }
        if (servletOutputStream.isReady()) {
            close();
            // 寫入完成後，通知容器該操作已完成
            if(null != handler) {
                Bundle b = new Bundle();
                b.putString("status", "done");
                Message m = handler.obtainMessage();
                m.setData(b);
                m.sendToTarget();
            }
        }
    }

    @Override
    public void onError(Throwable throwable) {
        close();
        throwable.printStackTrace();
        if(null != handler) {
            Bundle b = new Bundle();
            b.putString("status", "fail");
            Message m = handler.obtainMessage();
            m.setData(b);
            m.sendToTarget();
        }
    }

    private void close() {
        try {
            inputStream.close();
            servletOutputStream.flush();
        } catch (Exception e) {
            // e.printStackTrace();
        }
    }

    public static class Builder {

        private ServletOutputStream servletOutputStream;

        private InputStream inputStream = null;
        private File file = null;
        private CharSequence charSequence = null;

        private Handler handler = null;

        public AsyncWriteListener.Builder setServletOutputStream(ServletOutputStream servletOutputStream) {
            this.servletOutputStream = new WeakReference<>( servletOutputStream ).get();
            return this;
        }

        public AsyncWriteListener.Builder setInputStream(InputStream inputStream) {
            this.inputStream = inputStream;
            return this;
        }

        public AsyncWriteListener.Builder setFile(File file) {
            this.file = file;
            return this;
        }

        public AsyncWriteListener.Builder setCharSequence(CharSequence charSequence) {
            this.charSequence = charSequence;
            return this;
        }

        public AsyncWriteListener.Builder setHandler(Handler handler) {
            this.handler = new WeakReference<>( handler ).get();
            return this;
        }

        public AsyncWriteListener build() {
            if(null != inputStream) {
                return new AsyncWriteListener(servletOutputStream, inputStream, handler);
            }
            if(null != file) {
                return new AsyncWriteListener(servletOutputStream, file, handler);
            }
            return new AsyncWriteListener(servletOutputStream, charSequence, handler);
        }

    }

}
