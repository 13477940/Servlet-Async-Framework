package framework.sql.data;

import framework.connector.datasource.TomcatDataSource;
import framework.setting.WebAppSettingBuilder;
import framework.sql.context.SQLContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.HashMap;
import java.util.Map;

public abstract class DataService {

    private HashMap<String, SQLContext> commandPool = null;

    public DataService() { commandPool = new HashMap<>(); }

    /**
     * 建立連線並預載 SQL 語法
     * 此作法採用單一連線單一功能的原則，減少耦合
     * 預設不回傳資料庫訊息（減少多個回傳的 JDBC 錯誤問題及節省遠端頻寬）
     */
    public SQLContext getSQLContext(String sql) {
        return createSQLContext(sql, true);
    }

    /**
     * 建立連線並預載 SQL 語法
     * 此作法採用單一連線單一功能的原則，減少耦合
     * 第二參數決定是否需要回傳資料庫端資訊，需要回傳值的時候應設定成"false"
     */
    public SQLContext getSQLContext(String sql, boolean NOCOUNT) {
        return createSQLContext(sql, NOCOUNT);
    }

    // 建立 SQLContext 實作
    private SQLContext createSQLContext(String sql, boolean NOCOUNT) {
        // 取得現在存取的資料庫類型
        String dbType = WebAppSettingBuilder.build().getWebAppProperties().getProperty("DB_Type").toLowerCase();
        // 加工 SQL 字串語法為前端用途
        StringBuilder preFix = new StringBuilder();
        // MSSQL 才有支援 SET NOCOUNT 語法
        {
            if("mssql".equals(dbType)) {
                if (NOCOUNT) {
                    // 回傳時除去影響筆數資訊
                    preFix.append("SET NOCOUNT ON;");
                } else {
                    // 回傳時附帶被影響的筆數資訊
                    preFix.append("SET NOCOUNT OFF;");
                }
            }
        }
        // MySQL 連線設定語法（MySQL 架構一律採用 utf8mb4 確保最高的字集相容度）
        {
            if("mysql".equals(dbType)) {
                preFix.append("SET names utf8mb4;");
            }
        }
        // 補上此次請求的 SQL 語法
        preFix.append(sql);
        // 連線狀態封裝
        SQLContext sqx = null;
        try {
            /*
             * TODO 如果要更改資料庫 Connection 取得方式，藉由此處即可
             */
            Connection conn = null;
            {
                // conn = SimpleDataSource.getConnection();
                // conn = HikariCPDataSource.getConnection();
                conn = TomcatDataSource.getConnection();
            }
            PreparedStatement pst = conn.prepareStatement(preFix.toString());
            sqx = new SQLContext(conn, pst, sql);
            commandPool.put(sqx.getSSID(), sqx);
        } catch(Exception e) {
            e.printStackTrace();
        }
        return sqx;
    }

    /**
     * 單純執行語法只回傳 boolean
     */
    public boolean executeCommand(SQLContext sqx) {
        PreparedStatement pst = sqx.getPreparedStatement();
        boolean res = false;
        try {
            res = pst.execute();
        } catch(Exception e) {
            e.printStackTrace();
        }
        return res;
    }

    /**
     * 針對單一指令關閉連線
     */
    public void close(SQLContext sqx) {
        String key = sqx.getSSID();
        SQLContext tmp = commandPool.get(key);
        closeConnection(tmp);
    }

    /**
     * 關閉所有連線
     */
    public void close() {
        for (Map.Entry<String, SQLContext> entry : commandPool.entrySet()) {
            closeConnection(entry.getValue());
        }
    }

    /**
     * 關閉連線實作
     */
    private void closeConnection(SQLContext sqx) {
        try {
            Connection conn = sqx.getConnection();
            if(conn != null) conn.close();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

}
