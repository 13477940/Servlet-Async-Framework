package app.websocket;

import javax.websocket.Session;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
 * 廣域 Socket Session 頻道成員表
 * - 使用於跨 Socket Session 溝通用
 * - 每一筆 Socket Session 代表每一位 Socket 在線的使用者
 * - Channel 概念用於區分該則訊息將發送給哪些 Socket 端
 */
public class WebSocketChannel {

    private static final ArrayList<LinkedHashMap<String, Session>> channels;
    private static final LinkedHashMap<String, Session> allSessionMap; // 所有頻道
    private static final LinkedHashMap<String, Session> viewerMap; // UI 端觀察者頻道

    private WebSocketChannel() {}

    static {
        {
            LinkedHashMap<String, Session> map = new LinkedHashMap<>();
            allSessionMap = new WeakReference<>(map).get();
        }
        {
            LinkedHashMap<String, Session> map = new LinkedHashMap<>();
            viewerMap = new WeakReference<>(map).get();
        }
        {
            ArrayList<LinkedHashMap<String, Session>> list = new ArrayList<>();
            channels = new WeakReference<>(list).get();
            {
                assert channels != null;
                channels.add(allSessionMap);
                channels.add(viewerMap);
            }
        }
    }

    public static LinkedHashMap<String, Session> getAllSessionMap() {
        return allSessionMap;
    }

    public static LinkedHashMap<String, Session> getViewerMap() {
        return viewerMap;
    }

    public static void exitChannels(String sessionId) {
        for(LinkedHashMap<String, Session> list : channels) {
            list.remove(sessionId);
        }
    }

}
