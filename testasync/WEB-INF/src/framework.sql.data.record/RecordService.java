package framework.sql.data.record;

import framework.sql.context.SQLContext;
import framework.sql.data.DataService;
import framework.sql.data.pattern.DataRow;
import framework.sql.data.pattern.DataTable;

import java.sql.PreparedStatement;

public class RecordService extends DataService {

    private final String resultKey = "result"; // Create, Update, Delete 結果鍵值

    /**
     * 執行 INSERT, UPDATE, DELETE 語法
     */
    public DataTable executeRecord(SQLContext sqx) {
        PreparedStatement pst = sqx.getPreparedStatement();
        DataTable dt = new DataTable();
        try {
            // executeUpdate 只會回傳 row count 或是 0 表示無錯誤
            int res = pst.executeUpdate();
            dt.addRow(resToDataRow(res));
        } catch(Exception e) {
            e.printStackTrace();
        }
        return dt;
    }

    /**
     * 單次請求執行多筆 INSERT, UPDATE, DELETE 語法，
     * 當進行大量增刪改時，可採用此方法較有效率，並減少頻繁索取連線數，
     * 此方法限制同一類的 SQL 語法但進行多筆請求的封裝，
     * 像是對同一張表新增、修改、刪除幾千筆資料，
     * 使用時在 PreparedStatement 中必須先使用 addBatch 加入該次所有請求。
     */
    public DataTable executeRecordTable(SQLContext sqx) {
        PreparedStatement pst = sqx.getPreparedStatement();
        DataTable dt = new DataTable();
        try {
            int[] res = pst.executeBatch();
            dt = resToDataTable(res);
        } catch(Exception e) {
            e.printStackTrace();
        }
        return dt;
    }

    /**
     * 取得結果陣列的數值鍵
     */
    public String getResultKey() { return this.resultKey; }

    // 多個回傳值時（基於單個回傳值基礎）
    private DataTable resToDataTable(int[] res) {
        DataTable dt = new DataTable();
        for (int tmp : res) {
            DataRow row = resToDataRow(tmp);
            dt.addRow(row);
        }
        return dt;
    }

    // 單個回傳值
    private DataRow resToDataRow(int res) {
        DataRow row = new DataRow();
        String tmp = String.valueOf(res);
        row.put(resultKey, tmp);
        return row;
    }

}
