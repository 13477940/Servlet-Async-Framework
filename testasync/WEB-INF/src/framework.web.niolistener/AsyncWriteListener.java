package framework.web.niolistener;

import framework.observer.Bundle;
import framework.observer.Handler;
import framework.observer.Message;
import org.apache.tomcat.util.http.fileupload.disk.DiskFileItemFactory;

import javax.servlet.AsyncContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * 當每個獨立輸出的內容希望被非同步輸出時，要封裝於 WriteListener 才能確保完整的輸出，
 * 因為 onWritePossible 和 ServletOutputStream.isReady() 兩者狀態是持續變動的，
 * 這意味著寫入過長的（例如 2MiB 純文字）文檔或檔案於 onWritePossible 中處理時，
 * 會發現無法完整的輸出並且跳出 while isReady 迴圈，這是因為 onWritePossible 本身的狀態已經被改變了，
 * 所以必須由整體 WriteListener 重複的狀態進行判斷，那麼初始化的動作勢必不能寫在 onWritePossible 之中，
 * 並且判斷一個輸出事件的結束點也必須由 onWritePossible 是否已經寫完所有資料來決定。
 */
public class AsyncWriteListener implements WriteListener {

    private AsyncContext asyncContext;
    private BufferedInputStream inputStream;
    private Handler handler;

    private AsyncWriteListener(AsyncContext asyncContext, CharSequence charSequence, Handler handler) {
        this.asyncContext = asyncContext;
        {
            try {
                this.inputStream = new BufferedInputStream(new ByteArrayInputStream(charSequence.toString().getBytes(StandardCharsets.UTF_8)));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        this.handler = handler;
    }

    private AsyncWriteListener(AsyncContext asyncContext, File file, Handler handler) {
        this.asyncContext = asyncContext;
        {
            try {
                this.inputStream = new BufferedInputStream(new FileInputStream(file));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        this.handler = handler;
    }

    @Override
    public void onWritePossible() {
        ServletOutputStream out = null;
        try {
            out = asyncContext.getResponse().getOutputStream();
        } catch (Exception e) {
            e.printStackTrace();
        }
        assert null != out;
        byte[] buffer = new byte[DiskFileItemFactory.DEFAULT_SIZE_THRESHOLD];
        while (out.isReady()) {
            try {
                int len = inputStream.read(buffer);
                if (len != -1) {
                    out.write(buffer, 0, len);
                } else {
                    // all byte process done
                    closeInputSteam();
                    out.flush();
                    {
                        Bundle b = new Bundle();
                        b.putString("status", "done");
                        Message m = handler.obtainMessage();
                        m.setData(b);
                        m.sendToTarget();
                    }
                    break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            Thread.onSpinWait();
        }
    }

    @Override
    public void onError(Throwable throwable) {
        throwable.printStackTrace();
        closeInputSteam();
        {
            Bundle b = new Bundle();
            b.putString("status", "fail");
            b.putString("msg", throwable.getMessage());
            Message m = handler.obtainMessage();
            m.setData(b);
            m.sendToTarget();
        }
    }

    private void closeInputSteam() {
        try {
            if(null != inputStream) {
                inputStream.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static class Builder {

        private AsyncContext asyncContext = null;
        private CharSequence charSequence = null;
        private File file = null;
        private Handler handler = null;

        public AsyncWriteListener.Builder setAsyncContext(AsyncContext asyncContext) {
            this.asyncContext = asyncContext;
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
            this.handler = handler;
            return this;
        }

        public AsyncWriteListener build() {
            if(null == file) { return new AsyncWriteListener(asyncContext, charSequence, handler); }
            return new AsyncWriteListener(asyncContext, file, handler);
        }

    }

}
