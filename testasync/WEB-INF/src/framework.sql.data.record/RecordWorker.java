package framework.sql.data.record;

import framework.sql.context.SQLContext;
import framework.sql.data.pattern.DataRow;
import framework.sql.data.pattern.DataTable;

import java.util.ArrayList;

public abstract class RecordWorker {

    private RecordService recordService = null;
    protected String creator = null;

    public RecordWorker() { recordService = new RecordService(); }

    /**
     * 執行 INSERT, UPDATE, DELETE 語法
     */
    protected ArrayList<Integer> executeRecord(SQLContext sqx) {
        return getRecordResult(recordService.executeRecord(sqx));
    }

    /**
     * 單次請求執行多筆 INSERT, UPDATE, DELETE 語法，
     * 當進行大量增刪改時，可採用此方法較有效率，並減少頻繁索取連線數，
     * 此方法限制同一類的 SQL 語法但進行多筆請求的封裝，
     * 像是對同一張表新增、修改、刪除幾千筆資料，
     * 使用時在 PreparedStatement 中必須先使用 addBatch 加入該次所有請求。
     */
    protected ArrayList<Integer> executeRecordBatch(SQLContext sqx) {
        return getRecordResult(recordService.executeRecordBatch(sqx));
    }

    /**
     * 確認是否該次所有寫入指令執行成功
     */
    protected boolean isAllSuccess(ArrayList<Integer> result) {
        boolean isAS = false;
        int iSum = 0;
        for (Integer aResult : result) {
            int res = aResult;
            if (1 == res) iSum++;
        }
        if(iSum == result.size() - 1) isAS = true;
        return isAS;
    }

    /**
     * 取出 Record Result
     */
    private int getRecordResult(DataRow row) {
        String key = recordService.getResultKey();
        return Integer.valueOf(row.get(key));
    }

    /**
     * 取出 add Batch 的 Record Result
     */
    private ArrayList<Integer> getRecordResult(DataTable dt) {
        String key = recordService.getResultKey();
        ArrayList<Integer> res = new ArrayList<>();
        for(int i = 0, len = dt.size(); i < len; i++) {
            DataRow row = dt.getRow(i);
            int value = Integer.valueOf(row.get(key));
            res.add(value);
        }
        return res;
    }

}
