package framework.database;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import framework.database.pattern.DataTable;
import framework.observer.Bundle;
import framework.observer.Handler;
import framework.observer.Message;

import java.lang.ref.WeakReference;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

/**
 * 此 Framework 核心概念中每一次的 SQL 指令請求都代表一個獨立的 DatabaseAction
 */
public class DatabaseAction {

    private Connection conn = null;
    private PreparedStatement preparedStatement = null;
    private boolean autoCommit = false;

    /**
     * SQL 指令字串, Connection, ArrayList（確保參數順序）, AutoCommit Status
     */
    public DatabaseAction(String sql, Connection conn, ArrayList<String> parameters, boolean autoCommit) {
        // check connection status
        if(null == conn) {
            try {
                throw new Exception("資料庫連接為空值");
            } catch (Exception e) {
                e.printStackTrace();
                return;
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
                return;
            }
        }
        // setting autocommit status
        this.autoCommit = autoCommit;
        {
            try {
                if(!this.conn.isClosed()) {
                    this.conn.setAutoCommit(this.autoCommit);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        // init PreparedStatement
        {
            try {
                PreparedStatement preState = new WeakReference<>( conn.prepareStatement(sql) ).get();
                if(null != parameters && parameters.size() > 0) {
                    for(int i = 0, len = parameters.size(); i < len; i++) {
                        String value = parameters.get(i);
                        // PreparedStatement is start at 1 to N
                        assert preState != null;
                        preState.setString(i+1, value);
                    }
                }
                assert preState != null;
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
     * 當查詢具有大型資料回傳時，將藉由 Handler 機制將資料分散為多筆進行片段處理
     * 避免同時間使用大量記憶體造成 out of memory 錯誤
     */
    public void queryOnHandler(Handler handler) {
        Savepoint savepoint = null;
        try {
            if (!autoCommit) savepoint = conn.setSavepoint();
            ResultSet rs = new WeakReference<>( preparedStatement.executeQuery() ).get();
            resultSetToDataRow(rs, handler, savepoint);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * INSERT, UPDATE AND DELETE
     */
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
        ArrayList<String> columns = getAllColumnName(rs);
        try (rs) {
            while(rs.next()) {
                JsonObject row = new JsonObject();
                for (String col : columns) {
                    if(null == col) continue;
                    String key = col.toLowerCase();
                    String value = rs.getString(col);
                    row.addProperty(key, Objects.requireNonNullElse(value, ""));
                }
                {
                    // 過程中每一列的回傳
                    Bundle b = new Bundle();
                    b.putString("organizer", "queryOnHandler");
                    b.putString("status", "data");
                    b.putString("type", "JsonObject");
                    b.putString("data", new Gson().toJson(row));
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
        close();
    }

    // 取得 ResultSet 所有欄位名稱
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
