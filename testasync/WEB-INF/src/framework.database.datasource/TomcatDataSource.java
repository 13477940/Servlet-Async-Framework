package framework.database.datasource;

import framework.database.connection.ConnectContext;
import framework.database.connection.ConnectorConfig;
import org.apache.tomcat.jdbc.pool.DataSource;
import org.apache.tomcat.jdbc.pool.PoolProperties;

import java.sql.Connection;

/**
 * base Tomcat JDBC Connection Pool
 * https://tomcat.apache.org/tomcat-8.5-doc/jdbc-pool.html
 * 版本會跟著 Apache Tomcat 步進，相容性 JDK 則依照其需求即可
 * tomcat/bin/tomcat-juli.jar
 * tomcat/lib/tomcat-jdbc.jar
 */
public class TomcatDataSource extends ConnectorConfig implements ConnectionPool {

    private ConnectContext dbContext = null;
    private DataSource dataSource = null;

    private int errorCount = 0; // 累計錯誤次數
    private final int maxErrorCount = 50; // 最大錯誤次數，防止無窮循環
    private long lastestErrorTime = 0; // 最近一次發生錯誤的時機點

    public TomcatDataSource(ConnectContext dbContext) {
        initDataSource(dbContext);
    }

    @Override
    public Connection getConnection() {
        if(null == dataSource) {
            initDataSource(dbContext);
        }
        Connection conn = null;
        try {
            conn = dataSource.getConnection();
            if(!conn.isValid(3000)) {
                // 持久型服務重置處理判斷，當時間計數器超過指定時間後，將觸發流程執行重置
                long currentErrorTime = System.currentTimeMillis();
                // 開啟伺服器後第一次發生時
                if(lastestErrorTime == 0) {
                    resetDataSource(false, dbContext);
                    return getConnection();
                } else {
                    // 超過指定時間未重置時，執行重置計數器流程
                    if(currentErrorTime - lastestErrorTime > 3600000) {
                        resetDataSource(false, dbContext);
                        return getConnection();
                    } else {
                        // 未超過指定時間，但錯誤計數器已到達最大數目時（可能為無法修復的情況時）
                        if(errorCount >= maxErrorCount) {
                            System.err.println("Connection Pool 短時間內嚴重錯誤次數過多，已介入防止無窮初始化，請檢查資料庫操作程式碼中是否具有尚未正確關閉連結的部分。");
                            return null;
                        } else {
                            // 未超過指定時間，且錯誤次數未超過時
                            resetDataSource(false, dbContext);
                            return getConnection();
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return conn;
    }

    @Override
    public void shutdown() {
        try {
            if(null != this.dataSource) {
                this.dataSource.close(true);
            }
        } catch (Exception e) {
            // e.printStackTrace();
        }
    }

    private void initDataSource(ConnectContext dbContext) {
        String connectURI = getConnectURI(dbContext.getDB_Type(), dbContext.getDB_IP(), dbContext.getDB_Port(), dbContext.getDB_Name());

        PoolProperties poolConfig = new PoolProperties();
        poolConfig.setUrl(connectURI);
        poolConfig.setDriverClassName(getDriverClassName(dbContext.getDB_Type()));
        poolConfig.setUsername(dbContext.getDB_ACC());
        poolConfig.setPassword(dbContext.getDB_PWD());

        poolConfig.setJmxEnabled(true);

        // 連接池限制設定
        poolConfig.setDefaultAutoCommit(false); // AutoCommit
        poolConfig.setDefaultReadOnly(false);
        poolConfig.setMaxAge(dbContext.getDB_Max_Age()); // 連接保持時間（ms），預設為 0 表示該連結始終為連接中的狀態
        poolConfig.setMaxActive(dbContext.getDB_Max_Active()); // 預備連接數最大數量（整數）
        poolConfig.setMaxIdle(dbContext.getDB_Max_Idle()); // 最大預備連接數量，不大於 MaxActive（整數）
        poolConfig.setInitialSize(dbContext.getDB_InitialSize()); // 初始連接池大小（整數）
        poolConfig.setMinIdle(dbContext.getDB_Min_Idle()); // 最小預備連接數量，與 InitialSize 相同即可（整數）
        poolConfig.setMaxWait(dbContext.getDB_Max_Wait()); // 連接池滿載時等候時間限制（ms）
        poolConfig.setTimeBetweenEvictionRunsMillis(10000); // 檢查空閑或廢棄的連接頻率（ms），不可小於 1 秒
        poolConfig.setMinEvictableIdleTimeMillis(20000); // 空閒連接在池中保留最短的時間（ms）

        // 測試連接是否持續為可使用的
        poolConfig.setTestWhileIdle(true); // 空閒時週期性驗證連接
        poolConfig.setTestOnBorrow(false); // 由池中取出連接時執行驗證
        poolConfig.setTestOnReturn(false); // 連接回收時是否執行驗證
        poolConfig.setValidationQuery("SELECT 1;"); // 驗證連接 SQL 語法
        poolConfig.setValidationQueryTimeout(10); // 單位為秒數，零或負值表示無限大（second）
        poolConfig.setValidationInterval(20000); // 驗證連接指令頻率，過度密集會造成資源都花在驗證上面（ms）

        poolConfig.setRemoveAbandoned(true); // 連接時間大於 RemoveAbandonedTimeout 是否當作可廢棄的連接
        poolConfig.setRemoveAbandonedTimeout(dbContext.getDB_RemoveAbandonedTimeout()); // 應該設定成資料庫最長操作的時間限制（second）
        poolConfig.setLogAbandoned(false); // 如果逐筆記錄連接消滅的訊息會造成資源浪費

        StringBuilder sbd;
        sbd = new StringBuilder();
        sbd.append("org.apache.tomcat.jdbc.pool.interceptor.ConnectionState;");
        sbd.append("org.apache.tomcat.jdbc.pool.interceptor.StatementFinalizer");
        poolConfig.setJdbcInterceptors(sbd.toString());
        dataSource = new DataSource();
        dataSource.setPoolProperties(poolConfig);
    }

    // 重新建立資料庫連接池
    private void resetDataSource(boolean timerReset, ConnectContext dbContext) {
        this.dataSource.close(true);
        initDataSource(dbContext);
        lastestErrorTime = System.currentTimeMillis();
        if(timerReset) {
            errorCount = 0;
        } else {
            errorCount++;
        }
    }

    public static class Builder {
        private ConnectContext dbContext = new ConnectContext();

        public TomcatDataSource.Builder setAccount(String account) {
            this.dbContext.setDB_ACC(account);
            return this;
        }

        public TomcatDataSource.Builder setPassword(String password) {
            this.dbContext.setDB_PWD(password);
            return this;
        }

        public TomcatDataSource.Builder setIP(String ip) {
            this.dbContext.setDB_IP(ip);
            return this;
        }

        public TomcatDataSource.Builder setPort(String port) {
            this.dbContext.setDB_Port(port);
            return this;
        }

        public TomcatDataSource.Builder setDatabaseName(String dbName) {
            this.dbContext.setDB_Name(dbName);
            return this;
        }

        public TomcatDataSource.Builder setDatabaseType(String dbType) {
            this.dbContext.setDB_Type(dbType);
            return this;
        }

        public TomcatDataSource.Builder setMaxAge(int maxAge) {
            this.dbContext.setDB_Max_Age(maxAge);
            return this;
        }

        public TomcatDataSource.Builder setMaxActive(int maxActive) {
            this.dbContext.setDB_Max_Active(maxActive);
            return this;
        }

        public TomcatDataSource.Builder setMaxIdle(int maxIdle) {
            this.dbContext.setDB_Max_Idle(maxIdle);
            return this;
        }

        public TomcatDataSource.Builder setInitialSize(int initialSize) {
            this.dbContext.setDB_InitialSize(initialSize);
            return this;
        }

        public TomcatDataSource.Builder setMinIdle(int minIdle) {
            this.dbContext.setDB_Min_Idle(minIdle);
            return this;
        }

        public TomcatDataSource.Builder setMaxWait(int maxWait) {
            this.dbContext.setDB_Max_Wait(maxWait);
            return this;
        }

        public TomcatDataSource build() {
            return new TomcatDataSource(this.dbContext);
        }
    }

}
