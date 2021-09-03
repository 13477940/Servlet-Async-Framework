package framework.database.pattern;

import com.google.gson.JsonArray;

import java.lang.ref.WeakReference;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.Objects;

/**
 * [data access object]
 * 每個 DataTable 通常代表一次 Query 的結果集
 * DataTable 中通常會具有多個 DataRow 對應於資料庫的內容狀態
 */
public class DataTable {

    /**
     * 由 ArrayList 型態保留來自資料庫定義的資料列順序
     */
    private ArrayList<DataRow> instance = null;

    /**
     * 建立一個空的 DataTable
     */
    public DataTable() {
        this.instance = new ArrayList<>();
    }

    /**
     * 由 ArrayList<DataRow> 型態轉換為 DataTable
     */
    public DataTable(ArrayList<DataRow> rows) {
        this.instance = rows;
    }

    /**
     * ResultSet To DataTable
     */
    public DataTable(ResultSet rs) {
        ResultSetToDatainstance(new WeakReference<>(rs).get());
    }

    public void addRow(DataRow row) {
        instance.add(row);
    }

    public DataRow getRow(int index) {
        return instance.get(index);
    }

    public String toString() {
        return instance.toString();
    }

    /**
     * DataTable 轉換為 JSONArray 型態
     */
    public JsonArray toJsonArray() {
        if(null == this.instance) return null;
        JsonArray arr = new JsonArray();
        for(DataRow row : prototype()) {
            arr.add(row.toJsonObject());
        }
        return arr;
    }

    /**
     * 原依照 fastjson 規則採用命名，將進行廢棄
     */
    @Deprecated public JsonArray toJSONArray() {
        return toJsonArray();
    }

    /**
     * 取得所有列的數量
     */
    public int size() {
        return instance.size();
    }

    /**
     * 取得實作原型
     */
    public ArrayList<DataRow> prototype() {
        return instance;
    }

    // 由 ResultSet 格式轉換為 Data Object
    private void ResultSetToDatainstance(ResultSet rs) {
        instance = new ArrayList<>();
        ArrayList<String> cols = getAllColumnName(rs);
        try(rs) {
            while(rs.next()) {
                DataRow row = new DataRow();
                for(String col : cols) {
                    if(null == col || col.length() == 0) continue;
                    String key = col.toLowerCase();
                    String value = rs.getString(col);
                    row.put(key, Objects.requireNonNullElse(value, ""));
                }
                instance.add(row);
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    // 取得所有欄位名稱
    private ArrayList<String> getAllColumnName(ResultSet rs) {
        ArrayList<String> columnNameList = new ArrayList<>();
        try {
            ResultSetMetaData metaData = rs.getMetaData();
            for(int i = 1, len = metaData.getColumnCount(); i <= len; i++) {
                columnNameList.add(metaData.getColumnLabel(i));
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        return columnNameList;
    }

}
