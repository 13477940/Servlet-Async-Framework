package framework.database.connection;

import java.util.Objects;

public abstract class ConnectorConfig {

    /**
     * 取得資料庫採用的 DriverClassName
     */
    protected static String getDriverClassName(String databaseType) {
        String res = null;
        String dbType = Objects.requireNonNullElse(databaseType, "").trim().toLowerCase();
        switch(dbType) {
            // https://dev.mysql.com/doc/connector-j/5.1/en/connector-j-usagenotes-connect-drivermanager.html
            // https://dev.mysql.com/doc/connector-j/8.0/en/connector-j-usagenotes-connect-drivermanager.html
            case "mysql": {
                // res = "com.mysql.jdbc.Driver"; // MySQL 5.x
                res = "com.mysql.cj.jdbc.Driver"; // MySQL 8.0+
            } break;
            // https://mariadb.com/kb/en/about-mariadb-connector-j/
            case "mariadb": {
                res = "org.mariadb.jdbc.Driver";
            } break;
            // https://jdbc.postgresql.org/documentation/81/load.html
            case "postgresql": {
                res = "org.postgresql.Driver";
            } break;
            // https://docs.microsoft.com/zh-tw/sql/connect/jdbc/using-the-jdbc-driver?view=sql-server-ver15#making-a-simple-connection-to-a-database
            case "mssql": {
                res = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
            } break;
            default: {
                try {
                    throw new Exception("錯誤的資料庫類型，應為：sqlserver, postgresql, mysql, mariadb");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } break;
        }
        return res;
    }

    /**
     * 建立資料庫連接 URI，預設為不使用加密連線
     */
    protected static String getConnectURI(String DB_Type, String DB_IP, String DB_Port, String DB_Name) {
        return getConnectURI(DB_Type, DB_IP, DB_Port, DB_Name, false);
    }

    /**
     * 建立資料庫連接 URI
     */
    protected static String getConnectURI(String DB_Type, String DB_IP, String DB_Port, String DB_Name, Boolean useSecurity) {
        StringBuilder sbd = new StringBuilder();
        String dbType = Objects.requireNonNullElse(DB_Type, "").trim().toLowerCase();
        String ip = Objects.requireNonNullElse(DB_IP, "").trim();
        String port = Objects.requireNonNullElse(DB_Port, "").trim();
        String dbName = Objects.requireNonNullElse(DB_Name, "").trim();
        switch(dbType) {
            case "sqlserver": {
                sbd.append("jdbc:sqlserver://");
                sbd.append(ip);
                sbd.append(":");
                sbd.append(port);
                sbd.append(";databaseName=");
                sbd.append(dbName);
            } break;
            case "postgresql": {
                sbd.append("jdbc:postgresql://");
                sbd.append(ip);
                sbd.append(":");
                sbd.append(port);
                sbd.append("/");
                sbd.append(dbName);
            } break;
            case "mysql": {
                sbd.append("jdbc:mysql://");
                sbd.append(ip);
                sbd.append(":");
                sbd.append(port);
                sbd.append("/");
                sbd.append(dbName);
                // MySQL 體系要採用 utf8mb4/utf8mb4_0900_ai_ci 才能正常支援所有 Unicode 字集
                // 新增 NULL 值處理原則，zeroDateTimeBehavior=convertToNull
                // 更新到 MySQL 8.0 以及 JDBC 版本的需求更改 zeroDateTimeBehavior=CONVERT_TO_NULL
                if (!useSecurity) {
                    sbd.append("?useSSL=false");
                } else {
                    sbd.append("?useSSL=true");
                    sbd.append("&verifyServerCertificate=false");
                }
                sbd.append("&useUnicode=true");
                sbd.append("&CharacterEncoding=utf8mb4");
                // sbd.append("&zeroDateTimeBehavior=convertToNull");
                sbd.append("&zeroDateTimeBehavior=CONVERT_TO_NULL");
                sbd.append("&serverTimezone=CST");
                sbd.append("&useServerPrepStmts=true");
            } break;
            case "mariadb": {
                sbd.append("jdbc:mariadb://");
                sbd.append(ip);
                sbd.append(":");
                sbd.append(port);
                sbd.append("/");
                sbd.append(dbName);
                if (!useSecurity) {
                    sbd.append("?useSSL=false");
                } else {
                    sbd.append("?useSSL=true");
                    sbd.append("&verifyServerCertificate=false");
                }
                sbd.append("&useUnicode=true");
                sbd.append("&CharacterEncoding=utf8mb4");
            } break;
            default: {
                try {
                    throw new Exception("錯誤的資料庫類型，應為：sqlserver, postgresql, mysql, mariadb");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } break;
        }
        return sbd.toString();
    }

}
