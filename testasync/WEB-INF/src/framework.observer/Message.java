package framework.observer;

public class Message {

    private String messageName;
    private Handler handler;
    private Bundle bundle;

    public Message() {}

    public Message(Handler handler) {
        this.handler = handler;
    }

    public void setData(Bundle bundle) {
        this.bundle = bundle;
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
        return null == this.bundle;
    }

    @Override
    public String toString() {
        if(null == this.bundle) {
            return null;
        } else {
            return this.bundle.prototype().toString();
        }
    }

}
