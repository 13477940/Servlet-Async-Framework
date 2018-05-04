package framework.database.connection;

public abstract class ConnectorConfig {

    /**
     * 取得資料庫採用的 DriverClassName
     */
    protected static String getDriverClassName(String databaseType) {
        String res = null;
        String check = String.valueOf(databaseType).trim().toLowerCase();
        switch(check) {
            case "postgresql":
                res = "org.postgresql.Driver";
                break;
            case "mssql":
                res = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
                break;
            case "mysql":
                res = "com.mysql.jdbc.Driver";
                break;
            case "mariadb":
                res = "org.mariadb.jdbc.Driver";
                break;
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
        String check = String.valueOf(DB_Type).trim().toLowerCase();
        String ip = String.valueOf(DB_IP).trim();
        String port = String.valueOf(DB_Port).trim();
        String dbName = String.valueOf(DB_Name).trim();
        switch(check) {
            case "mssql":
                sbd.append("jdbc:sqlserver://");
                sbd.append(ip);
                sbd.append(":");
                sbd.append(port);
                sbd.append(";databaseName=");
                sbd.append(dbName);
                break;
            case "postgresql":
                sbd.append("jdbc:postgresql://");
                sbd.append(ip);
                sbd.append(":");
                sbd.append(port);
                sbd.append("/");
                sbd.append(dbName);
                break;
            case "mysql":
                sbd.append("jdbc:mysql://");
                sbd.append(ip);
                sbd.append(":");
                sbd.append(port);
                sbd.append("/");
                sbd.append(dbName);
                // MySQL 體系要採用 utf8mb4 才能正常支援所有 Unicode 字集
                // 新增 NULL 值處理原則，zeroDateTimeBehavior=convertToNull
                if(!useSecurity) {
                    sbd.append("?useSSL=false&useUnicode=true&CharacterEncoding=utf8mb4&zeroDateTimeBehavior=convertToNull");
                } else {
                    sbd.append("?useSSL=true&verifyServerCertificate=false&useUnicode=true&CharacterEncoding=utf8mb4&zeroDateTimeBehavior=convertToNull");
                }
                break;
            case "mariadb":
                sbd.append("jdbc:mariadb://");
                sbd.append(ip);
                sbd.append(":");
                sbd.append(port);
                sbd.append("/");
                sbd.append(dbName);
                // MySQL 體系要採用 utf8mb4 才能正常支援所有 Unicode 字集
                if(!useSecurity) {
                    sbd.append("?useSSL=false&useUnicode=true&CharacterEncoding=utf8mb4");
                } else {
                    sbd.append("?useSSL=true&verifyServerCertificate=false&useUnicode=true&CharacterEncoding=utf8mb4");
                }
                break;
        }
        return sbd.toString();
    }

}
