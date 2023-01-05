package app.websocket;

import jakarta.websocket.Session;

import java.util.LinkedHashMap;

public class WebSocketChannel {

    private final LinkedHashMap<String, Session> map = new LinkedHashMap<>();

    public void addSession(String key, Session value) {
        this.map.put(key, value);
    }

    public Session getSession(String key) {
        return this.map.get(key);
    }

    public void closeSession(String key) {
        this.map.remove(key);
    }

    public LinkedHashMap<String, Session> prototype() {
        return this.map;
    }

}
