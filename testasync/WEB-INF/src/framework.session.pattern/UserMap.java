package framework.session.pattern;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import framework.session.context.UserContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

public class UserMap {

    private HashMap<String, UserContext> map = null;

    public UserMap() {
        map = new HashMap<>();
    }

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
        if(null == map) { return 0; }
        return map.size();
    }

    /**
     * 轉型為 JSONArray
     */
    public JSONArray toJSONArray() {
        JSONArray arr = new JSONArray();
        for (Entry<String, UserContext> entry : map.entrySet()) {
            JSONObject obj = entry.getValue().getJSONObject();
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

    public HashMap<String, UserContext> prototype() {
        return map;
    }

}
