package framework.observer;

import java.util.ArrayList;

/**
 * https://stackoverflow.com/questions/9558779/is-there-any-class-in-java-similar-to-android-os-handler-in-android
 * 非同步回傳處理方案之一，藉由觀察者模式（Observer Pattern）思維實作，
 * 核心概念模擬來自 Android Handler 實作原始碼，
 * 觀察者模式階層關係：Handler > Message > Bundle
 */
public class Handler {

    private ArrayList<Message> msgList;

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
