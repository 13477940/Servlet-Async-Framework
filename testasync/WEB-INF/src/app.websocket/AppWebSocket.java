package app.websocket;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.lang.ref.WeakReference;

/**
 * https://www.pegaxchange.com/2018/01/28/websocket-server-java/
 *
 * 要注意，此類別內的參數域是每筆 Connection 獨立的，
 * 所以如果要跨 Session 操作通訊則需要另外建立廣域的 Socket Session 佇列，
 * 無法再此類別中直接去呼叫別的 Socket Session
 *
 * ！要注意 @ServerEndPoint 的參數值內容格式
 */
@ServerEndpoint(value = "/websocket")
public class AppWebSocket {

    @OnMessage
    public void onReciveMessage(String message, Session session) {
        WebSocketService.processMessage(new WeakReference<>( session ).get(), message);
    }

    @OnError
    public void onSocketError(Throwable error) {
        error.printStackTrace();
    }

    @OnOpen
    public void onSocketOpen(Session session) {
        String sessionID = session.getId();
        // all session map
        { WebSocketChannel.getAllSessionMap().put(sessionID, new WeakReference<>( session ).get()); }
        // server welcome message
        {
            JsonObject obj = new JsonObject();
            obj.addProperty("status", "connect_success");
            obj.addProperty("msg_zht", "連接成功");
            session.getAsyncRemote().sendText( new Gson().toJson(obj) );
        }
    }

    @OnClose
    public void onSocketClose(Session session) {
        String sessionId = session.getId();
        WebSocketChannel.exitChannels(sessionId);
    }

}
