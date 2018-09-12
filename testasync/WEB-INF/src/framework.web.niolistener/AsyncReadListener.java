package framework.web.niolistener;

import framework.observer.Bundle;
import framework.observer.Handler;
import framework.observer.Message;
import org.apache.tomcat.util.http.fileupload.disk.DiskFileItemFactory;

import javax.servlet.AsyncContext;
import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;

/**
 * TODO 此實作尚未加入應用，因為還沒有 JDK 原生的 Async MultiPart Parser 方案
 * 當每個獨立的上傳請求希望被非同步處理時，要採用 ReadListener，
 * 當伺服器有空閒的時間可以處理上傳資料時會調用 onDataAvailable，
 * 調用 onDataAvailable 是一個重複性的動作，
 * 直到該請求所有上傳的資料都傳遞完成才會呼叫 onAllDataRead
 * https://openhome.cc/Gossip/ServletJSP/ReadListener.html
 */
public class AsyncReadListener implements ReadListener {

    private AsyncContext asyncContext;
    private Handler handler;

    private AsyncReadListener(AsyncContext asyncContext, Handler handler) {
        this.asyncContext = asyncContext;
        this.handler = handler;
    }

    @Override
    public void onDataAvailable() {
        ServletInputStream sInput = null;
        try {
            sInput = asyncContext.getRequest().getInputStream();
        } catch (Exception e) {
            e.printStackTrace();
        }
        byte[] buffer = new byte[DiskFileItemFactory.DEFAULT_SIZE_THRESHOLD];
        assert sInput != null;
        while (sInput.isReady()) {
            if(sInput.isFinished()) break;
            try {
                int len = sInput.read(buffer);
                if(len == -1) { break; }
            } catch (Exception e) {
                e.printStackTrace();
            }
            Thread.onSpinWait();
        }
    }

    @Override
    public void onAllDataRead() {
        Bundle b = new Bundle();
        b.putString("status", "done");
        Message m = handler.obtainMessage();
        m.setData(b);
        m.sendToTarget();
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

        private AsyncContext asyncContext;
        private Handler handler;

        public AsyncReadListener.Builder setAsyncContext(AsyncContext asyncContext) {
            this.asyncContext = asyncContext;
            return this;
        }

        public AsyncReadListener.Builder setHandler(Handler handler) {
            this.handler = handler;
            return this;
        }

        public AsyncReadListener build() {
            return new AsyncReadListener(asyncContext, handler);
        }

    }

}
