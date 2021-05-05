package framework.web.session.context;

import com.google.gson.JsonObject;
import framework.time.TimeService;
import framework.web.context.AsyncActionContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.lang.ref.WeakReference;
import java.time.LocalDateTime;

/**
 * UserContext 是依據 HTTP Session 存在而封裝
 * 記錄該請求的使用者資訊，使用者登入成功時建立，
 * 並於每次發起請求時由此物件進行授權，
 * 若該 Session 尚未建立 UserContext 則表示尚未登入
 */
public class UserContext {

    private AsyncActionContext requestContext = null;
    private HttpSession session = null;
    private String sessionID = null;
    private String dataID = null; // 資料表主鍵值（自定義）
    private String account = null; // 使用者帳號（自定義）
    private String name = null; // 使用者名稱（自定義）
    private String nickName = null; // 使用者暱稱（自定義）
    private String category = null; // 使用者權限（自定義）
    private String remoteIP = null;
    private JsonObject extenObj = null; // 延伸資訊用（自定義）
    private String createDate = null;
    private String createTime = null;

    private UserContext(AsyncActionContext requestContext, String dataID, String account, String name, String nickName, String category, String remoteIP, JsonObject extenObj) {
        this.requestContext = requestContext;
        initUserContext(requestContext.getHttpSession(), dataID, account, name, nickName, category, remoteIP, extenObj);
    }

    private UserContext(HttpSession session, String dataID, String account, String name, String nickName, String category, String remoteIP, JsonObject extenObj) {
        initUserContext(session, dataID, account, name, nickName, category, remoteIP, extenObj);
    }

    private void initUserContext(HttpSession session, String dataID, String account, String name, String nickName, String category, String remoteIP, JsonObject extenObj) {
        this.session = session;
        this.sessionID = session.getId();
        this.dataID = dataID;
        this.account = account;
        this.name = name;
        this.nickName = nickName;
        this.category = category;
        {
            if(null == remoteIP) {
                this.remoteIP = getIpFromRequest();
            } else {
                this.remoteIP = remoteIP;
            }
        }
        this.extenObj = extenObj;
        {
            TimeService now = new TimeService.Builder().setLocalDateTime(LocalDateTime.now()).build();
            this.createDate = now.getDate();
            this.createTime = now.getTime();
        }
    }

    public HttpSession getSession() {
        return session;
    }
    public String getSessionID() {
        return sessionID;
    }
    public String getDataID() {
        return dataID;
    }
    public String getAccount() {
        return account;
    }
    public String getName() {
        return name;
    }
    public String getNickName() {
        return nickName;
    }
    public String getCategory() {
        return category;
    }
    public String getRemoteIP() {
        return remoteIP;
    }
    public String getCreateDate() {
        return createDate;
    }
    public String getCreateTime() {
        return createTime;
    }
    public JsonObject getExtenObj() {
        return extenObj;
    }

    public JsonObject toJSONObject() {
        JsonObject obj = new JsonObject();
        obj.addProperty("session_id", sessionID);
        obj.addProperty("data_id", dataID);
        obj.addProperty("account", account);
        obj.addProperty("name", name);
        obj.addProperty("nickname", nickName);
        obj.addProperty("category", category);
        obj.addProperty("remote_ip", remoteIP);
        obj.addProperty("create_date", createDate);
        obj.addProperty("create_time", createTime);
        obj.add("exten_obj", extenObj);
        return obj;
    }

    // 由 HttpServletRequest 判斷遠端 IP
    private String getIpFromRequest() {
        HttpServletRequest request = requestContext.getHttpRequest();
        String ip = request.getHeader("X-Real-IP");
        if (null == ip || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Forwarded-For");
        }
        if (null == ip || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (null == ip || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (null == ip || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    public static class Builder {
        private AsyncActionContext requestContext = null;
        private HttpSession session = null;
        private String dataID = null;
        private String account = null;
        private String name = null;
        private String nickName = null;
        private String category = null;
        private String remoteIP = null;
        private JsonObject extenObj = null;

        public UserContext.Builder setAsyncActionContext(AsyncActionContext requestContext) {
            this.requestContext = requestContext;
            return this;
        }

        public UserContext.Builder setHttpSession(HttpSession session) {
            this.session = new WeakReference<>(session).get();
            return this;
        }

        public UserContext.Builder setDataID(String dataID) {
            this.dataID = dataID;
            return this;
        }

        public UserContext.Builder setAccount(String account) {
            this.account = account;
            return this;
        }

        public UserContext.Builder setName(String name) {
            this.name = name;
            return this;
        }

        public UserContext.Builder setNickName(String nickName) {
            this.nickName = nickName;
            return this;
        }

        public UserContext.Builder setCategory(String category) {
            this.category = category;
            return this;
        }

        public UserContext.Builder setRemoteIP(String remoteIP) {
            this.remoteIP = remoteIP;
            return this;
        }

        public UserContext.Builder setExtenObj(JsonObject extenObj) {
            this.extenObj = extenObj;
            return this;
        }

        public UserContext build() {
            if(null == requestContext && null == session) {
                try {
                    throw new Exception("建立 UserContext 時，Session 資訊為必要的參數");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
            if(null == requestContext) {
                return new UserContext(session, dataID, account, name, nickName, category, remoteIP, extenObj);
            } else {
                return new UserContext(requestContext, dataID, account, name, nickName, category, remoteIP, extenObj);
            }
        }
    }

}
