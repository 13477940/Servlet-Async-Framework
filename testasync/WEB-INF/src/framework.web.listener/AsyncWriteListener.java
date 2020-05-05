package framework.web.listener;

import framework.observer.Bundle;
import framework.observer.Handler;
import framework.observer.Message;
import framework.web.context.AsyncActionContext;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;

/**
 * https://medium.com/@clu1022/%E6%B7%BA%E8%AB%87i-o-model-32da09c619e6
 * https://www.slideshare.net/SimoneBordet/servlet-31-async-io
 * https://openhome.cc/Gossip/ServletJSP/WriteListener.html
 *
 * 當每個獨立輸出的內容希望被非同步輸出時，要封裝於 WriteListener 才能確保完整的輸出，
 * 因為 onWritePossible 和 ServletOutputStream.isReady() 兩者狀態是持續變動的，
 * 例如寫入過長的（例如 2MiB 純文字）文檔或檔案於 onWritePossible 中處理時，
 * 會發現無法完整的輸出並且跳出 while isReady 迴圈，這是因為 onWritePossible 本身的狀態已經被改變了，
 * 所以必須由整體 WriteListener 重複的狀態進行判斷，那麼初始化的動作勢必不能寫在 onWritePossible 之中，
 * 並且判斷一個輸出事件的結束點也必須由 onWritePossible 是否已經寫完所有資料來決定。
 */
public class AsyncWriteListener implements WriteListener {

    private final boolean devMode = false;

    private final AsyncActionContext requestContext;
    private BufferedInputStream inputStream;
    private final Handler handler;

    private AsyncWriteListener(AsyncActionContext asyncActionContext, CharSequence charSequence, Handler handler) {
        this.requestContext = asyncActionContext;
        {
            try {
                this.inputStream = new WeakReference<>( new BufferedInputStream(new ByteArrayInputStream(charSequence.toString().getBytes(StandardCharsets.UTF_8))) ).get();
            } catch (Exception e) {
                if(devMode) { e.printStackTrace(); }
            }
        }
        this.handler = handler;
    }

    private AsyncWriteListener(AsyncActionContext asyncActionContext, File file, Handler handler) {
        this.requestContext = asyncActionContext;
        {
            try {
                this.inputStream = new WeakReference<>( new BufferedInputStream(new FileInputStream(file)) ).get();
            } catch (Exception e) {
                if(devMode) { e.printStackTrace(); }
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
    public void onWritePossible() {
        // 檢查 AsyncContext 是否可用
        if(requestContext.isComplete()) {
            Bundle b = new Bundle();
            b.putString("status", "fail");
            b.putString("msg", "can't output with completed async context");
            Message m = handler.obtainMessage();
            m.setData(b);
            m.sendToTarget();
            {
                try {
                    throw new Exception("can't output with completed async context");
                } catch (Exception e) {
                    if(devMode) { e.printStackTrace(); }
                }
            }
            return;
        }
        // 注意！這個 ServletOutputStream 是共用的，所以不能在此 close 它
        ServletOutputStream out = null;
        try {
            out = new WeakReference<>( requestContext.getAsyncContext().getResponse().getOutputStream() ).get();
        } catch (Exception e) {
            if(devMode) { e.printStackTrace(); }
        }
        // 非同步模式之下將 inputStream 內容讀取並輸出至 ServletOutputStream
        int rLength;
        while ( true ) {
            if(null == out) break;
            if(!out.isReady()) break;
            if(null == inputStream) break;
            try {
                // 當使用 inputStream.available() 會使用到物理記憶體最大值
                // 所以需要特別去注意這部分的調用值，應在之後設定一個極限值
                // 保障系統物理記憶體空間的餘裕與穩定度
                int bSize = inputStream.available();
                int bMaxSize = 1024 * 16;
                if ( bSize > bMaxSize ) bSize = bMaxSize;
                byte[] buffer = new byte[ bSize ];
                rLength = inputStream.read(buffer);
                // if all byte process done
                if( 0 > rLength || 0 == bSize ) {
                    closeStream();
                    {
                        Bundle b = new Bundle();
                        b.putString("status", "done");
                        Message m = handler.obtainMessage();
                        m.setData(b);
                        m.sendToTarget();
                    }
                    break;
                }
                out.write(buffer, 0, rLength);
            } catch (Exception e) {
                if(devMode) { e.printStackTrace(); }
            }
        }
    }

    @Override
    public void onError(Throwable throwable) {
        closeStream();
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

    private void closeStream() {
        try {
            if(null != inputStream) {
                inputStream.close();
            }
        } catch (Exception e) {
            if(devMode) { e.printStackTrace(); }
        }
    }

    public static class Builder {

        private AsyncActionContext requestContext = null;
        private CharSequence charSequence = null;
        private File file = null;
        private Handler handler = null;

        public AsyncWriteListener.Builder setAsyncActionContext(AsyncActionContext asyncActionContext) {
            this.requestContext = new WeakReference<>( asyncActionContext ).get();
            return this;
        }

        public AsyncWriteListener.Builder setCharSequence(CharSequence charSequence) {
            this.charSequence = charSequence;
            return this;
        }

        public AsyncWriteListener.Builder setFile(File file) {
            this.file = file;
            return this;
        }

        public AsyncWriteListener.Builder setHandler(Handler handler) {
            this.handler = new WeakReference<>( handler ).get();
            return this;
        }

        public AsyncWriteListener build() {
            if(null == file) {
                return new AsyncWriteListener(requestContext, charSequence, handler);
            }
            return new AsyncWriteListener(requestContext, file, handler);
        }

    }

}
