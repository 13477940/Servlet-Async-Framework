package framework.database.pattern;

import com.google.gson.JsonObject;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * [data access object]
 * 每個 DataRow 通常代表結果集的一列
 * 一列中會有多個欄位內容 key-value 形式
 */
public class DataRow {

    /**
     * 由 LinkedHashMap 型態保留來自資料庫欄位定義的狀態
     */
    private final LinkedHashMap<String, String> instance;

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
    public DataRow(JsonObject obj) {
        this.instance = new LinkedHashMap<>();
        for(Object keyObj : obj.keySet()) {
            String key = String.valueOf(keyObj);
            Object value = Objects.requireNonNullElse(obj.get(key).getAsString(), "");
            instance.put(String.valueOf(key), value.toString());
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
    public JsonObject toJsonObject() {
        JsonObject obj = new JsonObject();
        if(null == this.instance) return null;
        for(Map.Entry<String, String> entry : prototype().entrySet()) {
            String key = entry.getKey().toLowerCase(); // Key - LowerCase
            String value = Objects.requireNonNullElse(entry.getValue(), "");
            obj.addProperty(key, value);
        }
        return obj;
    }

    /**
     * 原依照 fastjson 規則採用命名，將廢棄
     */
    public JsonObject toJSONObject() {
        JsonObject obj = new JsonObject();
        if(null == this.instance) return null;
        for(Map.Entry<String, String> entry : prototype().entrySet()) {
            String key = entry.getKey().toLowerCase(); // Key - LowerCase
            String value = Objects.requireNonNullElse(entry.getValue(), "");
            obj.addProperty(key, value);
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
