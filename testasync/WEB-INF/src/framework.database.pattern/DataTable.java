package framework.database.pattern;

import com.alibaba.fastjson.JSONArray;

import java.lang.ref.WeakReference;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.Objects;

public class DataTable {

    /**
     * ArrayList 實例，丟入的內容會依照丟入先後的順序
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
    public JSONArray toJSONArray() {
        JSONArray arr = new JSONArray();
        for(DataRow row : prototype()) {
            arr.add(row.toJSONObject());
        }
        return arr;
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
                    if(null == col) continue; // if key is null
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
