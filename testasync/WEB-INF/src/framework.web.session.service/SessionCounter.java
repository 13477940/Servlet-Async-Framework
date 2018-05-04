package framework.web.session.service;

import javax.servlet.http.HttpSession;
import java.util.HashMap;

public class SessionCounter {

    private static HashMap<String, HttpSession> onlineCount = null;

    static {}

    public static void addCount(HttpSession session) {
        if(null == onlineCount) onlineCount = new HashMap<>();
        onlineCount.put(session.getId(), session);
    }

    public static void delCount(HttpSession session) {
        if(null == onlineCount) onlineCount = new HashMap<>();
        onlineCount.remove(session.getId());
    }

    /**
     * 所有活躍中的使用者數量，包含未登入狀態的訪客
     */
    public static int userCount() {
        return onlineCount.size();
    }

}
