package framework.web.session.context;

import com.alibaba.fastjson.JSONObject;
import framework.time.TimeService;
import framework.web.context.AsyncActionContext;
import framework.time.TimeServiceStatic;
import framework.time.pattern.TimeContext;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * 基於 Http Session 機制封裝使用者資訊
 */
public class UserContext {

    private AsyncActionContext requestContext = null;
    private HttpSession session = null;
    private String sessionID = null;
    private String account = null;
    private String name = null;
    private String nickName = null;
    private String category = null; // 系統權限級別
    private String remoteIP = null;
    private String createDate = null;
    private String createTime = null;

    /**
     * 初始化建立使用者資訊
     */
    public UserContext(AsyncActionContext requestContext, String account, String name, String nickName, String category) {
        this.requestContext = requestContext;
        this.session = requestContext.getHttpSession();
        this.sessionID = this.session.getId();
        this.account = account;
        this.name = name;
        this.nickName = nickName;
        this.category = category;
        this.remoteIP = getIpFromRequest();
        getCreateDT();
    }

    /**
     * 讀取 UserInfo 封裝後的 JSONObject 的內容恢復物件型態，
     * 只擷取使用者資訊內容，Session 部分覆寫為當前狀態
     */
    public UserContext(AsyncActionContext requestContext, JSONObject obj) {
        this.requestContext = requestContext;
        this.session = requestContext.getHttpSession();
        this.sessionID = this.session.getId();
        this.account = obj.getString("account");
        this.name = obj.getString("name");
        this.nickName = obj.getString("nickname");
        this.category = obj.getString("category");
        this.remoteIP = getIpFromRequest();
        getCreateDT();
    }

    /**
     * 從 Session 取出並回復成 UserContext 型態時
     */
    public UserContext(HttpSession session, JSONObject obj) {
        this.session = session;
        this.requestContext = null;
        this.sessionID = this.session.getId();
        this.account = obj.getString("account");
        this.name = obj.getString("name");
        this.nickName = obj.getString("nickname");
        this.category = obj.getString("category");
        this.remoteIP = obj.getString("ip");
        this.createDate = obj.getString("create_date");
        this.createTime = obj.getString("create_time");
    }

    public HttpSession getSession() {
        return session;
    }
    public String getSessionID() {
        return sessionID;
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

    public JSONObject getJSONObject() {
        JSONObject obj = new JSONObject();
        obj.put("session_id", sessionID);
        obj.put("account", account);
        obj.put("name", name);
        obj.put("nickname", nickName);
        obj.put("category", category);
        obj.put("ip", remoteIP);
        obj.put("create_date", createDate);
        obj.put("create_time", createTime);
        return obj;
    }

    // 記錄建立時間點
    private void getCreateDT() {
        TimeContext timeContext = TimeServiceStatic.getInstance().getTimeContext();
        this.createDate = timeContext.getDate();
        this.createTime = timeContext.getTime();
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
        private String account = null;
        private String name = null;
        private String nickName = null;
        private String category = null; // 系統權限級別

        public UserContext.Builder setAsyncActionContext(AsyncActionContext requestContext) {
            this.requestContext = requestContext;
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

        public UserContext build() {
            return new UserContext(requestContext, account, name, nickName, category);
        }

        public UserContext build(JSONObject obj) {
            return new UserContext(requestContext, obj);
        }
    }

}
