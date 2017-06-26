package framework.sql.data.pattern;

import com.alibaba.fastjson.JSONObject;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;

public class DataRow {

    protected HashMap<String, String> instance = null;

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
        for (Entry<String, Object> t : obj.entrySet()) {
            String key = String.valueOf(t.getKey());
            String value = String.valueOf(t.getValue());
            instance.put(key, value);
        }
    }

    /**
     * 由定義的兩鍵值將 DataTable 內容轉換為 DataRow 的格式，降低資料維度
     */
    public DataRow(DataTable dt, String key, String value) {
        this.instance = new HashMap<>();
        for(int i = 0, len = dt.size(); i < len; i++) {
            DataRow row = dt.getRow(i);
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
     * 轉換為 JSONObject 型態
     */
    public JSONObject toJSONObject() {
        JSONObject obj = new JSONObject();
        if(instance == null) return obj;
        for (Entry<String, String> t : instance.entrySet()) {
            String key = String.valueOf(t.getKey()).toLowerCase();
            String value = t.getValue();
            if(null != value) {
                obj.put(key, value);
            } else {
                obj.put(key, "");
            }
        }
        return obj;
    }

    /**
     * 設定多個欄位
     */
    public void setCells(String[] cols, String[] values) {
        for(int i = 0, len = cols.length; i < len; i++) {
            String key = cols[i];
            String value = values[i];
            instance.put(key, value);
        }
    }

    /**
     * Instance Map Entry Set
     */
    public Set<Entry<String, String>> entrySet() {
        return instance.entrySet();
    }

    /**
     * 取得實作原型
     */
    public HashMap<String, String> prototype() {
        return instance;
    }

}
