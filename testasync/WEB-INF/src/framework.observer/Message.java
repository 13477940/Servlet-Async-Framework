package framework.observer;

import java.lang.ref.WeakReference;
import java.util.Objects;

public class Message {

    private String messageName;
    private Handler handler;
    private Bundle bundle;

    public Message() {}

    public Message(Handler handler) {
        this.handler = new WeakReference<>(handler).get();
    }

    public void setData(Bundle bundle) {
        this.bundle = new WeakReference<>(bundle).get();
    }

    public Bundle getData() {
        return this.bundle;
    }

    public void setTarget(Handler handler) {
        this.handler = handler;
    }

    public Handler getTarget() {
        return this.handler;
    }

    public void sendToTarget() {
        this.handler.sendMessage(this);
    }

    public void setMessageName(String messageName) {
        this.messageName = messageName;
    }

    public String getMessageName() {
        return this.messageName;
    }

    public boolean isEmpty() {
        return ( null == this.bundle );
    }

    @Override
    public String toString() {
        return Objects.requireNonNullElse(this.bundle.prototype().toString(), null);
    }

}
