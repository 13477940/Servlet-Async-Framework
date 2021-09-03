package framework.handler;

import framework.observer.Bundle;
import framework.observer.Handler;
import framework.observer.Message;

/**
 * 基礎形式的 Observer Design Pattern 應用
 * 僅使用 String 內容作為判斷工作責任歸屬的責任鏈節點
 * 需要附帶另外的參數可藉由繼承此類別
 * 通常用來簡化 switch、多層 if-else 判斷處理結構
 */
public abstract class StringHandler {

    private StringHandler nextHandler;
    private Handler nonMatchedExceptionHandler;

    /**
     * Handler 處理事件起始點，通常直接呼叫 checkIsMyJob()
     */
    public abstract void startup(String str);

    /**
     * Handler 確認是否為自身的工作
     */
    protected abstract boolean checkIsMyJob(String str);

    /**
     * 設定下一位 Handler
     */
    public void setNextHandler(StringHandler handler) {
        this.nextHandler = handler;
        if(null != this.nonMatchedExceptionHandler) {
            this.nextHandler.setNonMatchedExceptionHandler(this.nonMatchedExceptionHandler);
        }
    }

    /**
     * 如果該項工作不屬於這位 Handler 轉交給下一個 Handler
     */
    protected void passToNext(String str) {
        if(null != nextHandler) {
            this.nextHandler.startup(str);
        } else {
            // 如果是持續遞交到沒有下一個 handler 表示為無效的請求
            if(null != this.nonMatchedExceptionHandler) {
                Bundle b = new Bundle();
                b.putString("status", "fail");
                b.putString("msg", "non_matched");
                b.putString("msg_zht", "沒有符合條件的 handler 進行處理");
                Message m = this.nonMatchedExceptionHandler.obtainMessage();
                m.setData(b);
                this.nonMatchedExceptionHandler.sendMessage(m);
            } else {
                try {
                    throw new Exception("沒有符合條件的 handler 進行處理");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 取得下一位 Handler
     */
    public StringHandler getNextHandler() {
        return this.nextHandler;
    }

    /**
     * 設定沒有被處理的事件例外錯誤回傳處理
     */
    public void setNonMatchedExceptionHandler(Handler handler) {
        setNonMatchedExceptionHandler(handler, false);
    }
    public void setNonMatchedExceptionHandler(Handler handler, boolean overwrite) {
        if(null == this.nonMatchedExceptionHandler) {
            this.nonMatchedExceptionHandler = handler;
        } else {
            if(overwrite) {
                this.nonMatchedExceptionHandler = handler;
            } else {
                try {
                    throw new Exception("已有 handler 時，需附帶 overwrite = true 才能進行覆蓋 handler");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
