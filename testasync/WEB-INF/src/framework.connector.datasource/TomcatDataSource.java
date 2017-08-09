package framework.connector.datasource;

import framework.connector.config.ConnectorConfig;
import framework.setting.WebAppSettingBuilder;
import org.apache.tomcat.jdbc.pool.DataSource;
import org.apache.tomcat.jdbc.pool.PoolProperties;

import java.sql.Connection;
import java.util.Properties;

/**
 * base Tomcat JDBC Connection Pool
 * https://tomcat.apache.org/tomcat-8.5-doc/jdbc-pool.html
 */
public class TomcatDataSource extends ConnectorConfig {

    private static DataSource dataSource = null; // 採用自己的 DataSource 實例為型態

    private static int errorCount = 0; // 累計錯誤次數
    private static final int maxErrorCount = 50; // 最大錯誤次數，防止無窮循環
    private static long lastestErrorTime = 0; // 最近一次發生錯誤的時機點

    static {}

    public static Connection getConnection() {
        if(null == dataSource) {
            initDataSource();
        }
        Connection conn = null;
        try {
            conn = dataSource.getConnection();
            if(!conn.isValid(3000)) {
                // 持久型服務重置處理判斷，當時間計數器超過指定時間後，將觸發流程執行重置
                long currentErrorTime = System.currentTimeMillis();
                // 開啟伺服器後第一次發生時
                if(lastestErrorTime == 0) {
                    dataSource.close(true);
                    initDataSource();
                    lastestErrorTime = System.currentTimeMillis();
                    errorCount++;
                    return getConnection();
                } else {
                    // 超過指定時間未重置時，執行重置計數器流程
                    if(currentErrorTime - lastestErrorTime > 3600000) {
                        dataSource.close(true);
                        initDataSource();
                        lastestErrorTime = System.currentTimeMillis();
                        errorCount = 0;
                        return getConnection();
                    } else {
                        // 未超過指定時間，但錯誤計數器已到達最大數目時（可能為無法修復的情況時）
                        if(errorCount >= maxErrorCount) {
                            System.err.println("Connection Pool 短時間內嚴重錯誤次數過多，已介入防止無窮初始化，請檢查程式碼或是資料庫關閉連結的時間配置。");
                            return null;
                        } else {
                            // 未超過指定時間，且錯誤次數未超過時
                            dataSource.close(true);
                            initDataSource();
                            lastestErrorTime = System.currentTimeMillis();
                            errorCount++;
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

    /**
     * 關閉整個 Connection Pool
     */
    public static void shutdown() {
        try {
            if(null != dataSource) {
                dataSource.close(true);
            }
        } catch (Exception e) {
            // e.printStackTrace();
        }
    }

    // 初始化資料庫連接池
    private static void initDataSource() {
        Properties prop = WebAppSettingBuilder.build().getWebAppProperties();
        String connectURI = getConnectURI(prop.getProperty("DB_Type"), prop.getProperty("DB_IP"), prop.getProperty("DB_Port"), prop.getProperty("DB_Name"));

        PoolProperties poolConfig = new PoolProperties();
        poolConfig.setUrl(connectURI);
        poolConfig.setDriverClassName(getDriverClassName(prop.getProperty("DB_Type")));
        poolConfig.setUsername(prop.getProperty("DB_ACC"));
        poolConfig.setPassword(prop.getProperty("DB_PWD"));

        poolConfig.setJmxEnabled(true);

        // 連接池限制設定
        poolConfig.setDefaultAutoCommit(true);
        poolConfig.setDefaultReadOnly(false);
        poolConfig.setMaxAge(0); // 連接保持時間（ms），預設為 0 表示始終為開放連接狀態
        poolConfig.setMaxActive(128); // 預備連接數最大數量（整數）
        poolConfig.setMaxIdle(128); // 最大預備連接數量，不大於 MaxActive（整數）
        poolConfig.setInitialSize(10); // 初始連接池大小（整數）
        poolConfig.setMinIdle(10); // 最小預備連接數量，與 InitialSize 相同即可（整數）
        poolConfig.setMaxWait(20000); // 連接池滿載時等候時間限制（ms）
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
        poolConfig.setRemoveAbandonedTimeout(600); // 應該設定成資料庫最長操作的時間限制（second）
        poolConfig.setLogAbandoned(false); // 如果逐筆記錄連接消滅的訊息會造成資源浪費

        String sbd = "org.apache.tomcat.jdbc.pool.interceptor.ConnectionState;" +
                "org.apache.tomcat.jdbc.pool.interceptor.StatementFinalizer";
        poolConfig.setJdbcInterceptors(sbd);
        dataSource = new DataSource();
        dataSource.setPoolProperties(poolConfig);
    }

}
