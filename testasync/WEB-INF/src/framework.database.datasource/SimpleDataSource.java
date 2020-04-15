package framework.database.datasource;

import framework.database.connection.ConnectContext;
import framework.database.connection.ConnectorConfig;
import framework.database.interfaces.ConnectionPool;
import framework.thread.ThreadPoolStatic;

import java.lang.ref.WeakReference;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * [connection template]
 * 基礎資料庫連線方式，此類別為無 Connection Pool 技術可採用時才選擇的方案
 * 因為不具有 ConnectionPool 管理會造成整體效率較差，不建議直接將此功能使用於生產環境中
 */
public class SimpleDataSource extends ConnectorConfig implements ConnectionPool {

    private ConnectContext dbContext = null;
    private ArrayList<HashMap<String, Object>> pool = null;
    private ExecutorService worker = null;
    private Boolean runTag = false;
    private long maxActiveSecond = (1000*1800); // 連結最長的存活時間(ms)限制，預設值

    private SimpleDataSource(ConnectContext dbContext) {
        if(null == dbContext) {
            try {
                throw new Exception("沒有資料庫連接定義");
            } catch(Exception e) {
                e.printStackTrace();
            }
        } else {
            this.dbContext = dbContext;
        }
    }

    @Override
    public Connection getConnection() {
        initPool();
        Connection conn = null;
        try {
            Class.forName(getDriverClassName(dbContext.getDB_Type()));
            String connectURI = getConnectURI(
                    dbContext.getDB_Type(),
                    dbContext.getDB_IP(),
                    dbContext.getDB_Port(),
                    dbContext.getDB_Name()
            );
            conn = new WeakReference<>(
                    DriverManager.getConnection(connectURI, dbContext.getDB_ACC(), dbContext.getDB_PWD())
            ).get();
            if(null != conn) {
                conn.setAutoCommit(false); // AutoCommit
                HashMap<String, Object> connObj = new HashMap<>();
                connObj.put("time", String.valueOf(System.currentTimeMillis()));
                connObj.put("connection", conn);
                pool.add(connObj);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return conn;
    }

    @Override
    public void shutdown() {
        {
            if (null == pool || pool.size() == 0) return;
            for (HashMap<String, Object> connObj : pool) {
                try {
                    Connection conn = (Connection) connObj.get("connection");
                    if (!conn.isClosed()) {
                        conn.close();
                    }
                } catch (Exception e) {
                    // e.printStackTrace();
                }
            }
            runTag = false;
            pool.clear();
        }
        {
            worker.shutdown();
            try {
                worker.shutdownNow();
            } catch (Exception e) {
                // e.printStackTrace();
            }
        }
    }

    public void setMaxActiveSecond(int second) {
        if(second <= 0) {
            System.err.println("設定的連結持續時間必須是一個大於 0 的整數");
            return;
        }
        this.maxActiveSecond = second * 1000;
    }

    // 初始化基礎資料庫連結管理
    private void initPool() {
        if(null == pool) {
            runTag = true;
            pool = new ArrayList<>();
            setupSimpleManager();
        }
    }

    // 簡易連接池回收管理
    private void setupSimpleManager() {
        if(null == worker) worker = ThreadPoolStatic.getInstance();
        worker.submit(() -> {
            while(runTag) {
                for (HashMap<String, Object> connObj : pool) {
                    // 檢查是否有逾時的 Connection 未被關閉
                    try {
                        long nowTime = System.currentTimeMillis();
                        long connTime = Long.parseLong(String.valueOf(connObj.get("time")));
                        Connection conn = (Connection) connObj.get("connection");
                        if( ( nowTime - connTime ) > maxActiveSecond ) {
                            if(!conn.isClosed()) conn.close();
                        }
                        if(conn.isClosed()) {
                            pool.remove(connObj);
                        }
                    } catch (SQLException e) {
                        // e.printStackTrace();
                    }
                }
                try {
                    TimeUnit.MINUTES.sleep(10);
                    // Thread.sleep(1000 * 60 * 10);
                } catch (Exception e) {
                    // e.printStackTrace();
                }
            }
        });
    }

    /**
     * 利用 Builder 模式，導引使用者採用 ConnectContext 並提供適用的設定接口，
     * 可以減少使用者學習 ConnectContext 所有內容的成本
     */
    public static class Builder {
        private ConnectContext dbContext = new SimpleConnContext();

        public SimpleDataSource.Builder setAccount(String account) {
            this.dbContext.setDB_ACC(account);
            return this;
        }

        public SimpleDataSource.Builder setPassword(String password) {
            this.dbContext.setDB_PWD(password);
            return this;
        }

        public SimpleDataSource.Builder setIP(String ip) {
            this.dbContext.setDB_IP(ip);
            return this;
        }

        public SimpleDataSource.Builder setPort(String port) {
            this.dbContext.setDB_Port(port);
            return this;
        }

        public SimpleDataSource.Builder setDatabaseName(String dbName) {
            this.dbContext.setDB_Name(dbName);
            return this;
        }

        public SimpleDataSource.Builder setDatabaseType(String dbType) {
            this.dbContext.setDB_Type(dbType);
            return this;
        }

        public SimpleDataSource build() {
            return new SimpleDataSource(this.dbContext);
        }
    }

    private static class SimpleConnContext extends ConnectContext {}

}
