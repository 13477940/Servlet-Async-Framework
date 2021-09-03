package framework.web.session.service;

import javax.servlet.http.HttpSession;
import java.util.HashMap;

public class SessionCounter {

    private static HashMap<String, HttpSession> onlineCount = null;

    public static void addCount(HttpSession session) {
        if(null == onlineCount) onlineCount = new HashMap<>();
        onlineCount.put(session.getId(), session);
    }

    public static void delCount(HttpSession session) {
        if(null == onlineCount) onlineCount = new HashMap<>();
        onlineCount.remove(session.getId());
    }

    /**
     * 所有存活中的 Session 使用者數量，包含未登入狀態的訪客
     * 如果要計算真實人數請使用 WebSocket 較為準確
     */
    public static int userCount() {
        return onlineCount.size();
    }

}
