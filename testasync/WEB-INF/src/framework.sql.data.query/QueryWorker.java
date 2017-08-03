package framework.sql.data.query;

import com.alibaba.fastjson.JSONObject;
import framework.hash.HashBuilder;
import framework.observer.Bundle;
import framework.observer.Handler;
import framework.observer.Message;
import framework.random.RandomBuilder;
import framework.setting.WebAppSettingBuilder;
import framework.sql.context.SQLContext;
import framework.sql.data.pattern.DataTable;
import framework.text.TextFileWriter;

import java.io.File;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;

public abstract class QueryWorker {

    private QueryService queryService = null;

    public QueryWorker() { queryService = new QueryService(); }

    /**
     * 預設的查詢語句結果會存放於記憶體之中，此為無參數帶入的 SQL 指令查詢指令
     */
    protected DataTable simpleQuery(String sql) {
        SQLContext sqx = queryService.getSQLContext(sql);
        DataTable dt = queryService.executeQuery(sqx);
        queryService.close(sqx);
        return dt;
    }

    /**
     * 藉由硬碟空間進行查詢結果緩存，建議於 Http Response 結束之後將檔案刪除，
     * 藉由 requestContext.outputFileToResponse(file.getPath(), file.getName(), "text/html", false); 輸出 JSON 內容，
     * 如果 ArrayKey 為 null 則直接回傳 JSONArray 格式
     */
    protected File onDiskQuery(SQLContext sqx) {
        return onDiskQuery(sqx, null);
    }
    protected File onDiskQuery(SQLContext sqx, String arrayKey) {
        File file = null;
        PreparedStatement pst = sqx.getPreparedStatement();
        try {
            pst.setEscapeProcessing(true);
            ResultSet rs = pst.executeQuery();
            file = onDiskQueryFn(rs, arrayKey);
        } catch(Exception e) {
            e.printStackTrace();
        }
        return file;
    }

    // 於硬碟上儲存 ResultSet 實作
    private File onDiskQueryFn(ResultSet rs, String arrayKey) {
        String tempDir = WebAppSettingBuilder.build().getPathContext().getTempDirPath();
        String fName = HashBuilder.build().stringToSHA1(RandomBuilder.build().getTimeHash());
        File file = new File(tempDir + fName + ".json");
        { // 建立 TextFileWriter 進行暫存文檔寫入
            TextFileWriter writer = new TextFileWriter(file);
            writer.build();
            // 如果需要在包覆一層 JSONObject 的話，要加入 arrKey 的參數
            if (null != arrayKey) {
                writer.write("{\"" + arrayKey + "\":");
            }
            writer.write("[");
            { // 藉由 ResultSet 開始製作 JSONArray 的內容
                String[] cols = getAllColumnName(rs);
                int index = 0;
                try {
                    while (rs.next()) {
                        JSONObject row = new JSONObject();
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
                        if (index > 0) {
                            writer.write(",");
                        }
                        writer.write(row.toJSONString());
                        index++;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            writer.write("]");
            if (null != arrayKey) {
                writer.write("}");
            }
            writer.close();
        }
        return file;
    }

    /**
     * 大型資料查詢 SQL 指令處理，調用此查詢通常會將結果檔案化，並回傳該檔案的 uuid，
     * 為了確保程式在短時間內儲存整個查詢結果，採用非同步模式的架構回傳給調用者，
     * 調用者可藉由 Handler 自定義每一列查詢結果回傳後的處理方式
     */
    protected void onHandlerQuery(SQLContext sqx, Handler handler) {
        PreparedStatement pst = sqx.getPreparedStatement();
        try {
            pst.setEscapeProcessing(true);
            ResultSet rs = pst.executeQuery();
            resultSetToDataRow(rs, handler);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    // 由 ResultSet Row 轉換為 JSONObject 格式並將每一列傳至 Handler，
    // 要注意「空值(null)」由於資料型態的關係所以會轉換為「空字串("")」，
    // 這樣才可以確保資料鍵完整傳達到調用者那方
    private void resultSetToDataRow(ResultSet rs, Handler handler) {
        String[] cols = getAllColumnName(rs);
        try {
            while(rs.next()) {
                JSONObject row = new JSONObject();
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
                // 傳送至處理者
                Bundle b = new Bundle();
                b.putString("organizer", "onHandlerQuery");
                b.putString("status", "data");
                b.putString("type", "JSONObject");
                b.putString("data", row.toJSONString());
                Message m = handler.obtainMessage();
                m.setData(b);
                m.sendToTarget();
            }
            // 傳送至處理者
            Bundle b = new Bundle();
            b.putString("organizer", "onHandlerQuery");
            b.putString("status", "done");
            Message m = handler.obtainMessage();
            m.setData(b);
            m.sendToTarget();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    // 取得 ResultSet 所有欄位名稱
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
