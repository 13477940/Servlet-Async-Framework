package framework.web.session.pattern;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import framework.web.session.context.UserContext;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

public abstract class UserMap {

    private final LinkedHashMap<String, UserContext> map = new LinkedHashMap<>();

    public UserMap() {}

    /**
     * 由 sessionID 增加使用者記錄
     */
    public void put(String sessionID, UserContext obj) {
        map.put(sessionID, obj);
    }

    /**
     * 由 sessionID 取得資訊
     */
    public UserContext get(String sessionID) {
        return map.get(sessionID);
    }

    /**
     * 剔除該使用者登入資訊
     */
    public void remove(String sessionID) {
        map.remove(sessionID);
    }

    /**
     * 目前登入人數
     */
    public int size() {
        return map.size();
    }

    /**
     * 轉型為 JSONArray
     */
    public JsonArray toJSONArray() {
        JsonArray arr = new JsonArray();
        for (Entry<String, UserContext> entry : map.entrySet()) {
            JsonObject obj = entry.getValue().toJSONObject();
            arr.add(obj);
        }
        return arr;
    }

    /**
     * 轉型為 ArrayList<UserObject>
     */
    public ArrayList<UserContext> toArrayList() {
        ArrayList<UserContext> list = new ArrayList<>();
        for (Entry<String, UserContext> entry : map.entrySet()) {
            list.add(entry.getValue());
        }
        return list;
    }

    public LinkedHashMap<String, UserContext> prototype() {
        return map;
    }

}
