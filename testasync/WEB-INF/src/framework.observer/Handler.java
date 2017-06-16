package framework.observer;

import java.util.ArrayList;

public class Handler {

    private ArrayList<Message> msgList;

    /**
     * 非同步回傳處理方案，藉由觀察者模式（Observer Pattern）實作，
     * 核心概念模擬來自 Android Handler 實作原始碼，
     * 觀察者模式階層關係：Handler > Message > Bundle
     */
    public Handler() {
        this.msgList = new ArrayList<>();
    }

    public void handleMessage(Message m) {
        msgList.remove(m);
    }

    public Message obtainMessage() {
        Message m = new Message(this);
        msgList.add(m);
        return m;
    }

    public void sendMessage(Message m) {
        handleMessage(m);
    }

    public boolean isEmpty() {
        return (null == msgList || msgList.size() == 0);
    }

}
