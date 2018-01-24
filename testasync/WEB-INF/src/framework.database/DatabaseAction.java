package framework.database;

import com.alibaba.fastjson.JSONObject;
import framework.database.pattern.DataTable;
import framework.observer.Bundle;
import framework.observer.Handler;
import framework.observer.Message;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * 每個 SQL 指令請求都代表一個獨立的 DatabaseAction
 */
public class DatabaseAction {

    private Connection conn = null;
    private PreparedStatement preparedStatement = null;
    private boolean autoCommit = false;

    public DatabaseAction(String sql, Connection conn, ArrayList<String> parameters, boolean autoCommit) {
        if(null == conn) {
            try {
                throw new Exception("資料庫連接為空值");
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            try {
                if (conn.isClosed()) {
                    throw new Exception("資料庫連接已被關閉");
                } else {
                    this.conn = conn;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if(null == sql || sql.length() == 0) {
            try {
                throw new Exception("未輸入正確的 SQL 語法指令");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        this.autoCommit = autoCommit;
        // 設定 PreparedStatement
        {
            try {
                PreparedStatement preState = conn.prepareStatement(sql);
                if(null != parameters && parameters.size() > 0) {
                    int i = 1; // start from 1 to n
                    for (String value : parameters) {
                        preState.setString(i, value);
                        i++;
                    }
                }
                preState.setEscapeProcessing(true);
                preparedStatement = preState; // 可用時
            } catch (Exception e) {
                e.printStackTrace();
                preparedStatement = null;
            }
        }
    }

    public Boolean execute() {
        Boolean res = null;
        Savepoint savepoint = null;
        try {
            if(!autoCommit) savepoint = conn.setSavepoint();
            res = preparedStatement.execute();
            if(!autoCommit) conn.commit();
        } catch (Exception e) {
            e.printStackTrace();
            try {
                if (!autoCommit) conn.rollback(savepoint);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        close();
        return res;
    }

    public DataTable query() {
        DataTable res = null;
        Savepoint savepoint = null;
        try {
            if(!autoCommit) savepoint = conn.setSavepoint();
            ResultSet rs = preparedStatement.executeQuery();
            res = new DataTable(rs);
            if(!autoCommit) conn.commit();
        } catch (Exception e) {
            e.printStackTrace();
            try {
                if (!autoCommit) conn.rollback(savepoint);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        close();
        return res;
    }

    /**
     * 當查詢具有大型資料回傳時，請藉由 Handler 將資料分散為多筆進行片段處理
     */
    public void queryOnHandler(Handler handler) {
        Savepoint savepoint = null;
        try {
            if (!autoCommit) savepoint = conn.setSavepoint();
            ResultSet rs = preparedStatement.executeQuery();
            resultSetToDataRow(rs, handler, savepoint);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Integer update() {
        Integer res = null;
        Savepoint savepoint = null;
        try {
            if(!autoCommit) savepoint = conn.setSavepoint();
            res = preparedStatement.executeUpdate();
            if(!autoCommit) conn.commit();
        } catch (Exception e) {
            e.printStackTrace();
            try {
                if (!autoCommit) conn.rollback(savepoint);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        close();
        return res;
    }

    public Long largeUpdate() {
        Long res = null;
        Savepoint savepoint = null;
        try {
            if(!autoCommit) savepoint = conn.setSavepoint();
            res = preparedStatement.executeLargeUpdate();
            if(!autoCommit) conn.commit();
        } catch (Exception e) {
            e.printStackTrace();
            try {
                if (!autoCommit) conn.rollback(savepoint);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        close();
        return res;
    }

    public Integer[] updateBatch(PreparedStatement preparedStatement) {
        Integer[] res = null;
        Savepoint savepoint = null;
        try {
            if(!autoCommit) savepoint = conn.setSavepoint();
            res = Arrays.stream( preparedStatement.executeBatch() ).boxed().toArray( Integer[]::new );
            if(!autoCommit) conn.commit();
        } catch (Exception e) {
            e.printStackTrace();
            try {
                if (!autoCommit) conn.rollback(savepoint);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        close();
        return res;
    }

    public Long[] largeUpdateBatch(PreparedStatement preparedStatement) {
        Long[] res = null;
        Savepoint savepoint = null;
        try {
            if(!autoCommit) savepoint = conn.setSavepoint();
            res = Arrays.stream( preparedStatement.executeLargeBatch() ).boxed().toArray( Long[]::new );
            if(!autoCommit) conn.commit();
        } catch (Exception e) {
            e.printStackTrace();
            try {
                if (!autoCommit) conn.rollback(savepoint);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        close();
        return res;
    }

    public static class Builder {

        private Connection conn = null;
        private String sql = null;
        private ArrayList<String> parameters = null;
        private boolean autoCommit = false;

        public DatabaseAction.Builder setConnection(Connection connection) {
            this.conn = connection;
            return this;
        }

        public DatabaseAction.Builder setSQL(String sql) {
            this.sql = sql;
            return this;
        }

        public DatabaseAction.Builder setParameters(ArrayList<String> parameters) {
            this.parameters = parameters;
            return this;
        }

        public DatabaseAction.Builder setAutoCommit(boolean autoCommit) {
            this.autoCommit = autoCommit;
            return this;
        }

        public DatabaseAction build() {
            return new DatabaseAction(this.sql, this.conn, this.parameters, this.autoCommit);
        }

    }

    // 資料庫操作要回收：ResultSet, PreparedStatement, Connection
    private void close() {
        try {
            if(null != preparedStatement && !preparedStatement.isClosed()) {
                preparedStatement.close();
            }
            if(null != conn && !conn.isClosed()) {
                conn.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // used for queryOnHandler
    private void resultSetToDataRow(ResultSet rs, Handler handler, Savepoint savepoint) {
        String[] columns = getAllColumnName(rs);
        try {
            while(rs.next()) {
                JSONObject row = new JSONObject();
                for (String col : columns) {
                    // 欄位名稱一律轉為小寫
                    String key = String.valueOf(col).toLowerCase();
                    String value = rs.getString(col);
                    if(null != value) {
                        row.put(key, value);
                    } else {
                        row.put(key, "");
                    }
                }
                {
                    // 過程中每一列的回傳
                    Bundle b = new Bundle();
                    b.putString("organizer", "queryOnHandler");
                    b.putString("status", "data");
                    b.putString("type", "JSONObject");
                    b.putString("data", row.toJSONString());
                    Message m = handler.obtainMessage();
                    m.setData(b);
                    m.sendToTarget();
                }
            }
            if(!autoCommit) conn.commit(); // commit
            {
                // 傳送處理成功的資訊
                Bundle b = new Bundle();
                b.putString("organizer", "queryOnHandler");
                b.putString("status", "done");
                Message m = handler.obtainMessage();
                m.setData(b);
                m.sendToTarget();
            }
        } catch (Exception e) {
            e.printStackTrace();
            try {
                if(!autoCommit) conn.rollback(savepoint);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            {
                // 傳送處理失敗的資訊
                Bundle b = new Bundle();
                b.putString("organizer", "queryOnHandler");
                b.putString("status", "fail");
                Message m = handler.obtainMessage();
                m.setData(b);
                m.sendToTarget();
            }
        }
        // resultSet close
        try {
            rs.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        close();
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
