package framework.connector.datasource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import framework.connector.config.ConnectorConfig;
import framework.setting.WebAppSettingBuilder;

import java.sql.Connection;

/**
 * https://github.com/brettwooldridge/HikariCP
 * 實作基於 HikariCP 套件的資料庫連接池，
 * 只有對外 Connection 才需要統一型態即可，
 * 每個 ConnectionPool 差異化的細節應藉由封裝減少使用者學習成本，
 * DataSource 遵照自己套件的型態才能準確的控制 close 事件
 */
public class HikariCPDataSource extends ConnectorConfig {

    private static HikariDataSource dataSource = null; // 採用自己的 DataSource 實例為型態

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
                    shutdown();
                    initDataSource();
                    lastestErrorTime = System.currentTimeMillis();
                    errorCount++;
                    return getConnection();
                } else {
                    // 超過指定時間未重置時，執行重置計數器流程
                    if(currentErrorTime - lastestErrorTime > 3600000) {
                        shutdown();
                        initDataSource();
                        lastestErrorTime = System.currentTimeMillis();
                        errorCount = 0;
                        return getConnection();
                    } else {
                        // 未超過指定時間，但錯誤計數器已到達最大數目時（可能為無法修復的情況時）
                        if(errorCount >= maxErrorCount) {
                            System.err.println("Connection Pool 短時間內嚴重錯誤次數過多，已介入防止無窮初始化，請檢查資料庫操作程式碼中是否具有尚未正確關閉連結的部分。");
                            return null;
                        } else {
                            // 未超過指定時間，且錯誤次數未超過時
                            shutdown();
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
                if(!dataSource.isClosed()) {
                    dataSource.close();
                }
            }
        } catch (Exception e) {
            // e.printStackTrace();
        }
    }

    // 初始化資料庫連接池
    private static void initDataSource() {
        HikariConfig config = new HikariConfig();
        String type = WebAppSettingBuilder.build().getWebAppProperties().getProperty("DB_Type");
        String ip = WebAppSettingBuilder.build().getWebAppProperties().getProperty("DB_IP");
        String port = WebAppSettingBuilder.build().getWebAppProperties().getProperty("DB_Port");
        String name = WebAppSettingBuilder.build().getWebAppProperties().getProperty("DB_Name");
        String uri = getConnectURI(type, ip, port, name);

        String acc = WebAppSettingBuilder.build().getWebAppProperties().getProperty("DB_ACC");
        String pwd = WebAppSettingBuilder.build().getWebAppProperties().getProperty("DB_PWD");

        config.setDriverClassName(getDriverClassName(type));
        config.setJdbcUrl(uri);
        config.setUsername(acc);
        config.setPassword(pwd);

        config.setAutoCommit(true);
        config.setReadOnly(false);
        config.setConnectionTestQuery("SELECT 1;");

        config.setMaximumPoolSize(128);
        dataSource = new HikariDataSource(config);
    }

}
