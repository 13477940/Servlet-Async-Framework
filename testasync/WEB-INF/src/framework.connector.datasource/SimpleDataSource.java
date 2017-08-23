package framework.connector.datasource;

import framework.connector.config.ConnectorConfig;
import framework.setting.WebAppSettingBuilder;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 基礎資料庫連線方式，此類別為無 Connection Pool 技術可採用時才選擇的方案
 */
public class SimpleDataSource extends ConnectorConfig {

    private static ArrayList<HashMap<String, Object>> pool = null;
    private static ExecutorService worker = null;
    private static boolean runTag = false;
    private static long maxActiveSecond = 1000*1800; // 連結最長的存活時間限制，預設值

    static {}

    /**
     * 取得單一持久資料庫連線，需自行處理 Connection 關閉與回收時機
     */
    public static Connection getConnection() {
        initPool();
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

            HashMap<String, Object> connObj = new HashMap<>();
            connObj.put("time", String.valueOf(System.currentTimeMillis()));
            connObj.put("connection", conn);
            pool.add(connObj);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return conn;
    }

    /**
     * 回收非連接池的連結實作
     */
    public static void shutdown() {
        if(null == pool || pool.size() == 0) return;
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
        worker.shutdown();
    }

    public static void setMaxActiveSecond(int second) {
        if(second <= 0) {
            System.err.println("設定的連結持續時間必須是一個大於 0 的整數");
            return;
        }
        maxActiveSecond = second * 1000;
    }

    /**
     * 初始化基礎資料庫連結管理
     */
    private static void initPool() {
        if(null == pool) {
            runTag = true;
            pool = new ArrayList<>();
            setupSimpleManager();
        }
    }

    private static void setupSimpleManager() {
        worker = Executors.newCachedThreadPool();
        worker.submit(() -> {
            while(runTag) {
                for (HashMap<String, Object> connObj : pool) {
                    try {
                        long nowTime = System.currentTimeMillis();
                        long connTime = Long.valueOf(String.valueOf(connObj.get("time")));
                        Connection conn = (Connection) connObj.get("connection");
                        if( ( nowTime - connTime ) > maxActiveSecond ) {
                            conn.close();
                        }
                        if(conn.isClosed()) {
                            pool.remove(connObj);
                        }
                    } catch (SQLException e) {
                        // e.printStackTrace();
                    }
                }
                try {
                    Thread.sleep(1000 * 60 * 10);
                } catch (Exception e) {
                    // e.printStackTrace();
                }
            }
        });
    }

}
