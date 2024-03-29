package app.websocket;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import framework.observer.Handler;
import framework.observer.Message;
import framework.thread.ThreadPoolStatic;
import jakarta.websocket.Session;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class WebSocketService {

    private WebSocketService() {}

    static {
        autoPingAllSession();
    }

    /**
     * 所有 Socket 傳輸的內容由此開始處理
     */
    public static void processMessage(Session webSocketSession, String message) {
        JsonObject msgObj = null;
        {
            try {
                msgObj = new Gson().fromJson(message, JsonObject.class);
            } catch (Exception e) {
                // e.printStackTrace();
            }
        }
        String sessionID = webSocketSession.getId();
        if(null == msgObj) {
            System.out.println(sessionID + " say: " + message);
            return;
        }
        // if JSON data
        {
            if(msgObj.has("cmd")) {
                String cmd = msgObj.get("cmd").getAsString();
                switch (cmd) {
                    case "viewer_reg": {
                        if(null == WebSocketChannelList.getChannel("_viewer")) WebSocketChannelList.addChannel("_viewer", new WebSocketChannel());
                        WebSocketChannelList.getChannel("_viewer").addSession(sessionID, new WeakReference<>( webSocketSession ).get());
                    } break;
                    default: {
                        JsonObject obj = new JsonObject();
                        obj.addProperty("status", "fail");
                        obj.addProperty("msg_zht", "請輸入正確的指令");
                        webSocketSession.getAsyncRemote().sendText( new Gson().toJson(obj) );
                    } break;
                }
            } else {
                // send to viewer
                System.out.println(sessionID + " say: " + message);
                for(Map.Entry<String, Session> entry : WebSocketChannelList.getChannel("_viewer").prototype().entrySet()) {
                    entry.getValue().getAsyncRemote().sendText(message);
                }
            }
        }
    }

    /**
     * 由於 caddy, nginx, tomcat servlet container 機制，
     * 會因 timeout 自動切斷 WebSocket 連接，
     * 所以要有 ping 機制去解決頻繁斷線的問題
     */
    private static void autoPingAllSession() {
        ThreadPoolStatic.execute(() -> autoPingAllSessionFn(new Handler(){
            @Override
            public void handleMessage(Message m) {
                super.handleMessage(m);
                autoPingAllSession();
            }
        }));
    }

    private static void autoPingAllSessionFn(Handler handler) {
        JsonObject obj = new JsonObject();
        {
            obj.addProperty("status", "ping");
        }
        if(null == WebSocketChannelList.getChannel("_all")) WebSocketChannelList.addChannel("_all", new WebSocketChannel());
        for(Map.Entry<String, Session> entry : WebSocketChannelList.getChannel("_all").prototype().entrySet()) {
            Session wsSession = entry.getValue();
            // 確認是使用中的 socket session
            try {
                if (null != wsSession && wsSession.isOpen()) {
                    wsSession.getAsyncRemote().sendText(new Gson().toJson(obj));
                }
            } catch (Exception e) {
                // e.printStackTrace();
            }
        }
        try {
            // 間隔多久發送 ping 包（用於維持連線狀態）
            TimeUnit.SECONDS.sleep(20);
        } catch (Exception e) {
            // e.printStackTrace();
        }
        if(null != handler) {
            handler.obtainMessage().sendToTarget();
        }
    }

}
