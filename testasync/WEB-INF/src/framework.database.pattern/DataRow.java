package framework.database.pattern;

import com.alibaba.fastjson.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class DataRow {

    private HashMap<String, String> instance = null;

    /**
     * 建立一個空的 DataRow
     */
    public DataRow() {
        this.instance = new HashMap<>();
    }

    /**
     * 由 HashMap<String, String> 型態轉換為 DataRow
     */
    public DataRow(HashMap<String, String> row) {
        this.instance = row;
    }

    /**
     * JSONObject to DataRow
     * 前端的空值會在 JavaScript 中轉換為空字串
     */
    public DataRow(JSONObject obj) {
        this.instance = new HashMap<>();
        for(Map.Entry<String, Object> entry : obj.entrySet()) {
            String key = entry.getKey();
            String value;
            Object tmp = entry.getValue();
            // 預防空值處理
            if(null != tmp) {
                value = tmp.toString();
            } else {
                value = "";
            }
            instance.put(key, value);
        }
    }

    /**
     * 由定義的兩鍵值將 DataTable 內容轉換為 DataRow 的格式，降低資料維度
     */
    public DataRow(DataTable dt, String key, String value) {
        this.instance = new HashMap<>();
        for(DataRow row : dt.prototype()) {
            String _key = row.get(key);
            String _value = row.get(value);
            instance.put(_key, _value);
        }
    }

    public void put(String key, String value) {
        instance.put(key, value);
    }

    public String get(String key) {
        return instance.get(key);
    }

    public String toString() {
        return instance.toString();
    }

    public int size() {
        return instance.size();
    }

    public Set<String> keySet() {
        return instance.keySet();
    }

    public boolean containsKey(String key) {
        return instance.containsKey(key);
    }

    /**
     * DataRow 轉換為 JSONObject 型態
     */
    public JSONObject toJSONObject() {
        JSONObject obj = new JSONObject();
        if(null == prototype()) return null;
        for(Map.Entry<String, String> entry : prototype().entrySet()) {
            String key = entry.getKey().toLowerCase(); // column key, always lowercase
            String value = entry.getValue(); // column value
            if(null != value) {
                obj.put(key, value);
            } else {
                obj.put(key, ""); // 空值處理
            }
        }
        return obj;
    }

    /**
     * Instance Map Entry Set
     */
    public Set<Map.Entry<String, String>> entrySet() {
        return instance.entrySet();
    }

    /**
     * 取得實作原型
     */
    public HashMap<String, String> prototype() {
        return instance;
    }

}
