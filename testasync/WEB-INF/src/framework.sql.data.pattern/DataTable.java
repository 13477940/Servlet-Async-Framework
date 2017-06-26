package framework.sql.data.pattern;

import com.alibaba.fastjson.JSONArray;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;

public class DataTable {

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
        ResultSetToDatainstance(rs);
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
     * 轉換為 JSONArray 型態
     */
    public JSONArray toJSONArray() {
        JSONArray arr = new JSONArray();
        for (DataRow row : instance) {
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
     * 取得所有列
     */
    public ArrayList<DataRow> instance() {
        return instance;
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
        String[] cols = getAllColumnName(rs);
        try {
            while(rs.next()) {
                DataRow row = new DataRow();
                for (String col : cols) {
                    // 欄位名稱一律轉為小寫
                    String key = String.valueOf(col).toLowerCase();
                    String value = rs.getString(col);
                    if(null != value) {
                        row.put(key, value);
                    } else {
                        row.put(key, "");
                    }
                }
                instance.add(row);
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    // 取得所有欄位名稱
    private String[] getAllColumnName(ResultSet rs) {
        String[] columnName = null;
        try {
            ResultSetMetaData metaData = rs.getMetaData();
            int count = metaData.getColumnCount();
            columnName = new String[count];
            for(int i = 0; i < count; i++) {
                columnName[i] = metaData.getColumnLabel(i+1);
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        return columnName;
    }

}
