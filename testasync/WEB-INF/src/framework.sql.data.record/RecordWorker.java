package framework.sql.data.record;

import framework.sql.data.pattern.DataRow;
import framework.sql.data.pattern.DataTable;

import java.util.ArrayList;

public abstract class RecordWorker {

    protected RecordService recordService = null;
    protected String creator = null;

    public RecordWorker() { recordService = new RecordService(); }

    /**
     * 取出 Record Result
     */
    protected int getRecordResult(DataRow row) {
        String key = recordService.getResultKey();
        return Integer.valueOf(row.get(key));
    }

    /**
     * 取出 add Batch 的 Record Result
     */
    protected ArrayList<Integer> getRecordResult(DataTable dt) {
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
