package framework.database.pattern;

import com.alibaba.fastjson.JSONArray;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;

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
    public DataTable(ResultSet rs) { ResultSetToDatainstance(rs); }

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
        try {
            while(rs.next()) {
                DataRow row = new DataRow();
                for(String col : cols) {
                    String key = String.valueOf(col).toLowerCase(); // column key, always lowercase
                    String value = rs.getString(col); // column value
                    // 如果欄位值為 NULL 時
                    if(null == value) {
                        row.put(key, "");
                        continue;
                    }
                    // 如果欄位值為空字串時
                    if(value.length() == 0) {
                        // row.put(key, "");
                        continue;
                    }
                    // 如果欄位值不為空值
                    row.put(key, value);
                }
                instance.add(row);
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        // 回收 ResultSet 資源
        try {
            if(null != rs && !rs.isClosed()) rs.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 取得所有欄位名稱
    private ArrayList<String> getAllColumnName(ResultSet rs) {
        ArrayList<String> columnNameList = new ArrayList<>();
        try {
            ResultSetMetaData metaData = rs.getMetaData();
            for(int i = 0; i < metaData.getColumnCount(); i++) {
                columnNameList.add(metaData.getColumnLabel(i+1));
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        return columnNameList;
    }

}
