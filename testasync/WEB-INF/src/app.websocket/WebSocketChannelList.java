package app.websocket;

import java.lang.ref.WeakReference;
import java.util.HashMap;

/**
 * 廣域 Socket Session 頻道成員表
 * - 使用於跨 Socket Session 溝通用
 * - 每一筆 Socket Session 代表每一位 Socket 在線的使用者
 * - Channel 概念用於區分該則訊息將發送給哪些 Socket 端
 */
public class WebSocketChannelList {

    private static final HashMap<String, WebSocketChannel> channels;

    private WebSocketChannelList() {}

    static {
        channels = new HashMap<>();
    }

    public static void addChannel(String channel_label, WebSocketChannel webSocketChannel) {
        channels.put( channel_label, new WeakReference<>(webSocketChannel).get() );
    }

    public static WebSocketChannel getChannel(String key) {
        return channels.get( key );
    }

    public static void closeChannel(String channel_label) {
        channels.remove(channel_label);
    }

    public static HashMap<String, WebSocketChannel> prototype() {
        return channels;
    }

}
