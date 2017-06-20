package framework.connector.datasource;

import framework.connector.config.ConnectorConfig;
import framework.setting.WebAppSettingBuilder;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

/**
 * 基礎資料庫連線方式，此類別為無 Connection Pool 技術可採用時才選擇的方案
 */
public class SimpleDataSource extends ConnectorConfig {

    static {}

    /**
     * 取得單一持久資料庫連線，需自行處理 Connection 關閉與回收時機
     */
    public static Connection getConnection() {
        Connection conn = null;
        try {
            Properties prop = WebAppSettingBuilder.build().getWebAppProperties();
            Class.forName(getDriverClassName(prop.getProperty("DB_Type")));
            String connectURI = getConnectURI(
                    prop.getProperty("DB_Type"),
                    prop.getProperty("DB_IP"),
                    prop.getProperty("DB_Port"),
                    prop.getProperty("DB_Name")
            );
            conn = DriverManager.getConnection(connectURI, prop.getProperty("DB_ACC"), prop.getProperty("DB_PWD"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return conn;
    }

}
