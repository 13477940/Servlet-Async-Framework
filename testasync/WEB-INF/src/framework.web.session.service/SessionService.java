package framework.web.session.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import framework.web.session.context.UserContext;
import framework.web.session.pattern.UserMap;

import javax.servlet.http.HttpSession;
import java.util.Map;

public abstract class SessionService {

    private final UserMap userMap;
    private final String userContextTag = "user_context";

    /**
     * 一定要於此處初始化的原因在於不一定都從 addUser 開始，
     * 也有可能只是查詢 userMap 的內容為起點
     */
    SessionService() {
        this.userMap = new UserMapInstance();
    }

    public void addUser(UserContext userContext) {
        addUser(userContext.getSession(), userContext);
    }

    public void addUser(HttpSession session, UserContext userContext) {
        if(null == session) {
            try {
                throw new Exception("必須帶入有效的 session 物件");
            } catch (Exception e) {
                e.printStackTrace();
            }
            return;
        }
        if(null == userContext) {
            try {
                throw new Exception("必須帶入有效的 UserContext 物件");
            } catch (Exception e) {
                e.printStackTrace();
            }
            return;
        }
        userMap.put(session.getId(), userContext);
        setUserContext(session, userContext);
    }

    public void removeUser(HttpSession session) {
        if(userMap.size() == 0) return;
        String sessionID = session.getId();
        if(userMap.prototype().containsKey(sessionID)) {
            userMap.remove(sessionID);
        }
    }

    public UserMap getUserMap() {
        return userMap;
    }

    public void setUserContext(HttpSession session, UserContext userContext) {
        if(null == userContext) return;
        session.setAttribute(userContextTag, new Gson().toJson(userContext.toJSONObject()));
    }

    public UserContext getUserContext(HttpSession session) {
        if(null == session.getAttribute(userContextTag)) return null;
        String tmp = session.getAttribute(userContextTag).toString();
        if(null == tmp || tmp.length() == 0) return null;
        JsonObject obj = new Gson().fromJson(tmp, JsonObject.class);
        return new UserContext.Builder()
                .setHttpSession(session)
                .setDataID(obj.get("data_id").getAsString())
                .setAccount(obj.get("account").getAsString())
                .setName(obj.get("name").getAsString())
                .setNickName(obj.get("nickname").getAsString())
                .setCategory(obj.get("category").getAsString())
                .setRemoteIP(obj.get("remote_ip").getAsString())
                .setExtenObj(obj.get("exten_obj").getAsJsonObject())
                .build();
    }

    /**
     * 由於 Session 過期並不是即時的，
     * 有按登出的使用者會直接於列表中刪除，
     * 但非登出而離開的使用者則須等待至過期才會被刪除，
     * 藉由此方法快速清除所有已暫存的使用者列表，
     * 如果是活躍的使用者將會在短時間內被恢復登記於使用者列表中。
     */
    public void clearUserMap() {
        userMap.prototype().clear();
    }

    /**
     * 清空所有 Session 使用者目前的 Session 內容
     */
    public void logoutAllUser() {
        for(Map.Entry<String, UserContext> uc : userMap.prototype().entrySet()) {
            removeUser(uc.getValue().getSession());
        }
        clearUserMap();
    }

    private static class UserMapInstance extends UserMap {}

}
