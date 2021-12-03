package framework.database.datasource;

import com.zaxxer.hikari.HikariConfig;
import framework.database.connection.ConnectContext;
import framework.database.connection.ConnectorConfig;
import framework.database.interfaces.ConnectionPool;

import javax.sql.DataSource;
import java.lang.ref.WeakReference;
import java.sql.Connection;

/**
 * https://github.com/brettwooldridge/HikariCP
 * --- required jar file ---
 * https://mvnrepository.com/artifact/com.zaxxer/HikariCP
 * https://mvnrepository.com/artifact/org.slf4j/slf4j-api
 * https://mvnrepository.com/artifact/org.slf4j/slf4j-simple
 */
public class HikariDataSource extends ConnectorConfig implements ConnectionPool {

    private final ConnectContext dbContext = null;
    private DataSource dataSource = null;

    private HikariDataSource(ConnectContext dbContext) {
        if(null == dbContext) {
            try {
                throw new Exception("沒有資料庫連接定義");
            } catch(Exception e) {
                e.printStackTrace();
            }
        } else {
            initDataSource(dbContext);
        }
    }

    private void initDataSource(ConnectContext dbContext) {
        String connectURI = getConnectURI(dbContext.getDB_Type(), dbContext.getDB_IP(), dbContext.getDB_Port(), dbContext.getDB_Name(), dbContext.getUseSSL());
        {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(connectURI);
            config.setDriverClassName(getDriverClassName(dbContext.getDB_Type())); // webapp required
            config.setUsername(dbContext.getDB_ACC());
            config.setPassword(dbContext.getDB_PWD());
            config.addDataSourceProperty("cachePrepStmts", "true"); // 當為 true 時下方三個參數才有效
            config.addDataSourceProperty("prepStmtCacheSize", "250"); // 連接池大小預設 25，推薦值是 200~250
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048"); // 單個指令快取是 256，推薦值是 2048
            config.addDataSourceProperty("useServerPrepStmts", "true"); // 新版本 MySQL 支援服務器端快取，提升處理效能
            config.addDataSourceProperty("useLocalSessionState", "true");
            config.addDataSourceProperty("useLocalTransactionState", "true");
            config.addDataSourceProperty("rewriteBatchedStatements", "true");
            config.addDataSourceProperty("cacheResultSetMetadata", "true");
            config.addDataSourceProperty("cacheServerConfiguration", "true");
            config.addDataSourceProperty("elideSetAutoCommits", "true");
            config.addDataSourceProperty("maintainTimeStats", "false");
            dataSource = new com.zaxxer.hikari.HikariDataSource(config);
        }
    }

    @Override
    public Connection getConnection() {
        if(null == dataSource) {
            assert false;
            initDataSource(dbContext);
        }
        Connection conn = null;
        try {
            conn = dataSource.getConnection();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new WeakReference<>( conn ).get();
    }

    @Override
    public void shutdown() {
        try {
            com.zaxxer.hikari.HikariDataSource _dataSource = (com.zaxxer.hikari.HikariDataSource) this.dataSource;
            if(null != _dataSource && !_dataSource.isClosed()) {
                _dataSource.close();
            }
        } catch (Exception e) {
            // e.printStackTrace();
        }
    }

    public static class Builder {
        private final ConnectContext dbContext = new HikariDataSource.HikariCpPoolContext();

        public HikariDataSource.Builder setAccount(String account) {
            this.dbContext.setDB_ACC(account);
            return this;
        }

        public HikariDataSource.Builder setPassword(String password) {
            this.dbContext.setDB_PWD(password);
            return this;
        }

        public HikariDataSource.Builder setIP(String ip) {
            this.dbContext.setDB_IP(ip);
            return this;
        }

        public HikariDataSource.Builder setPort(String port) {
            this.dbContext.setDB_Port(port);
            return this;
        }

        public HikariDataSource.Builder setDatabaseName(String dbName) {
            this.dbContext.setDB_Name(dbName);
            return this;
        }

        public HikariDataSource.Builder setDatabaseType(String dbType) {
            this.dbContext.setDB_Type(dbType);
            return this;
        }

        public HikariDataSource.Builder setMaxAge(int maxAge) {
            this.dbContext.setDB_Max_Age(maxAge);
            return this;
        }

        public HikariDataSource.Builder setMaxActive(int maxActive) {
            this.dbContext.setDB_Max_Active(maxActive);
            return this;
        }

        public HikariDataSource.Builder setMaxIdle(int maxIdle) {
            this.dbContext.setDB_Max_Idle(maxIdle);
            return this;
        }

        public HikariDataSource.Builder setInitialSize(int initialSize) {
            this.dbContext.setDB_InitialSize(initialSize);
            return this;
        }

        public HikariDataSource.Builder setMinIdle(int minIdle) {
            this.dbContext.setDB_Min_Idle(minIdle);
            return this;
        }

        public HikariDataSource.Builder setMaxWait(int maxWait) {
            this.dbContext.setDB_Max_Wait(maxWait);
            return this;
        }

        public HikariDataSource.Builder setUseSSL(boolean useSSL) {
            this.dbContext.setUseSSL(useSSL);
            return this;
        }

        public HikariDataSource build() {
            return new HikariDataSource(this.dbContext);
        }
    }

    private static class HikariCpPoolContext extends ConnectContext {}

}
