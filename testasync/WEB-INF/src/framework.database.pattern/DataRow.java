package framework.database.pattern;

import com.alibaba.fastjson.JSONObject;

import java.util.*;

/**
 * [data access object]
 * 每個 DataRow 通常代表結果集的一列
 * 一列中會有多個欄位內容 key-value 形式
 */
public class DataRow {

    /**
     * 由 LinkedHashMap 型態保留來自資料庫欄位定義的狀態
     */
    private LinkedHashMap<String, String> instance;

    /**
     * 建立一個空的 DataRow
     */
    public DataRow() {
        this.instance = new LinkedHashMap<>();
    }

    /**
     * 由 HashMap<String, String> 型態轉換為 DataRow
     */
    public DataRow(LinkedHashMap<String, String> row) {
        this.instance = row;
    }

    /**
     * JSONObject to DataRow
     * 前端的空值會在 JavaScript 中轉換為空字串
     */
    public DataRow(JSONObject obj) {
        this.instance = new LinkedHashMap<>();
        for(Map.Entry<String, Object> entry : obj.entrySet()) {
            String key = entry.getKey();
            Object value = Objects.requireNonNullElse(entry.getValue(), "");
            instance.put(key, value.toString());
        }
    }

    /**
     * 由定義的兩鍵值將 DataTable 內容轉換為 DataRow 的格式，降低資料維度
     */
    public DataRow(DataTable dt, String key, String value) {
        this.instance = new LinkedHashMap<>();
        for(DataRow row : dt.prototype()) {
            String _key = row.get(key);
            String _value = Objects.requireNonNullElse(value, "");
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
            String key = entry.getKey().toLowerCase(); // Key - LowerCase
            String value = Objects.requireNonNullElse(entry.getValue(), "");
            obj.put(key, value);
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
    public LinkedHashMap<String, String> prototype() {
        return instance;
    }

}
