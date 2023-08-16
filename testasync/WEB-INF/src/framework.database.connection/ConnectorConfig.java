package framework.database.connection;

import java.util.Locale;
import java.util.Objects;

public abstract class ConnectorConfig {

    /**
     * 取得資料庫採用的 DriverClassName
     */
    protected static String getDriverClassName(String databaseType) {
        String res = null;
        String dbType = Objects.requireNonNullElse(databaseType, "").trim().toLowerCase(Locale.ENGLISH);
        switch (dbType) {
            // https://dev.mysql.com/doc/connector-j/5.1/en/connector-j-usagenotes-connect-drivermanager.html
            // https://dev.mysql.com/doc/connector-j/8.0/en/connector-j-usagenotes-connect-drivermanager.html
            case "mysql" -> {
                // res = "com.mysql.jdbc.Driver"; // MySQL 5.x
                res = "com.mysql.cj.jdbc.Driver"; // MySQL 8.0+
            }

            // https://mariadb.com/kb/en/about-mariadb-connector-j/
            case "mariadb" -> {
                res = "org.mariadb.jdbc.Driver";
            }

            // https://jdbc.postgresql.org/documentation/81/load.html
            case "postgres", "postgresql" -> {
                res = "org.postgresql.Driver";
            }

            // https://docs.microsoft.com/zh-tw/sql/connect/jdbc/using-the-jdbc-driver?view=sql-server-ver15#making-a-simple-connection-to-a-database
            case "mssql", "sqlserver" -> {
                res = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
            }
            default -> {
                try {
                    throw new Exception("錯誤的資料庫類型(step1)，可用的類型：mysql, mariadb, postgresql, mssql, sqlserver");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
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
        String dbType = Objects.requireNonNullElse(DB_Type, "").trim().toLowerCase(Locale.ENGLISH);
        String ip = Objects.requireNonNullElse(DB_IP, "").trim();
        String port = Objects.requireNonNullElse(DB_Port, "").trim();
        String dbName = Objects.requireNonNullElse(DB_Name, "").trim();
        switch (dbType) {
            case "mysql" -> {
                sbd.append("jdbc:mysql://");
                sbd.append(ip);
                sbd.append(":");
                sbd.append(port);
                sbd.append("/");
                sbd.append(dbName);
                // MySQL 體系要採用 utf8mb4/utf8mb4_0900_ai_ci 才能正常支援所有 Unicode 字集
                // 新增 NULL 值處理原則，zeroDateTimeBehavior=convertToNull
                // 更新到 MySQL 8.0 以及 JDBC 版本的需求更改 zeroDateTimeBehavior=CONVERT_TO_NULL
                sbd.append("?useUnicode=true");
                sbd.append("&CharacterEncoding=utf8mb4");
                if (!useSecurity) {
                    sbd.append("&useSSL=false");
                } else {
                    sbd.append("&useSSL=true");
                    sbd.append("&verifyServerCertificate=false");
                }
                // sbd.append("&zeroDateTimeBehavior=convertToNull");
                sbd.append("&zeroDateTimeBehavior=CONVERT_TO_NULL");
                // fix for jdbc 8.0.25 timezone setting
                // https://ithelp.ithome.com.tw/articles/10217691
                // sbd.append("&serverTimezone=CST");
                // sbd.append("&serverTimezone=").append(URLEncoder.encode("UTC+8", StandardCharsets.UTF_8));
                sbd.append("&serverTimezone=UTC");
                sbd.append("&useServerPrepStmts=true");
                // https://segmentfault.com/a/1190000021870318
                sbd.append("&allowPublicKeyRetrieval=true");
            }
            case "mariadb" -> {
                sbd.append("jdbc:mariadb://");
                sbd.append(ip);
                sbd.append(":");
                sbd.append(port);
                sbd.append("/");
                sbd.append(dbName);
                sbd.append("?useUnicode=true");
                // 指定使用完整版的 unicode 編碼：utf8mb4
                sbd.append("&CharacterEncoding=utf8mb4");
                if (!useSecurity) {
                    sbd.append("&useSSL=false");
                } else {
                    sbd.append("&useSSL=true");
                    // 若需要使用 server 端的 rsa public key 則要為 false
                    // 但設定為 false 需要注意是否可能具有中間人攻擊的機率（未經過 VPN 時）
                    sbd.append("&verifyServerCertificate=false");
                }
            }
            case "postgres", "postgresql" -> {
                sbd.append("jdbc:postgresql://");
                sbd.append(ip);
                sbd.append(":");
                sbd.append(port);
                sbd.append("/");
                sbd.append(dbName);
            }
            case "mssql", "sqlserver" -> {
                sbd.append("jdbc:sqlserver://");
                sbd.append(ip);
                sbd.append(":");
                sbd.append(port);
                sbd.append(";databaseName=");
                sbd.append(dbName);
                if (!useSecurity) {
                    sbd.append(";encrypt=false");
                    sbd.append(";authentication=NotSpecified");
                } else {
                    sbd.append(";encrypt=true");
                    sbd.append(";integratedSecurity=true;trustServerCertificate=true");
                }
            }
            default -> {
                try {
                    throw new Exception("錯誤的資料庫類型(step2)，可用的類型：mysql, mariadb, postgres, postgresql, mssql, sqlserver");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return sbd.toString();
    }

}
