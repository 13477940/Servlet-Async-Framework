package framework.connector.config;

public abstract class ConnectorConfig {

    /**
     * 取得資料庫採用的 DriverClassName
     */
    protected static String getDriverClassName(String type) {
        String res = null;
        String check = String.valueOf(type).trim().toLowerCase();
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
     * 建立資料庫連接 URI
     */
    protected static String getConnectURI(String DB_Type, String DB_IP, String DB_Port, String DB_Name) {
        StringBuilder sbd = new StringBuilder();
        String check = String.valueOf(DB_Type).trim().toLowerCase();
        String ip = String.valueOf(DB_IP).trim();
        String port = String.valueOf(DB_Port).trim();
        String dbName = String.valueOf(DB_Name).trim();
        switch(check) {
            case "postgresql":
                sbd.append("jdbc:postgresql://");
                sbd.append(ip);
                sbd.append(":");
                sbd.append(port);
                sbd.append("/");
                sbd.append(dbName);
                break;
            case "mssql":
                sbd.append("jdbc:sqlserver://");
                sbd.append(ip);
                sbd.append(":");
                sbd.append(port);
                sbd.append(";databaseName=");
                sbd.append(dbName);
                break;
            case "mysql":
                sbd.append("jdbc:mysql://");
                sbd.append(ip);
                sbd.append(":");
                sbd.append(port);
                sbd.append("/");
                sbd.append(dbName);
                sbd.append("?useUnicode=true&CharacterEncoding=UTF-8");
                break;
            case "mariadb":
                sbd.append("jdbc:mariadb://");
                sbd.append(ip);
                sbd.append(":");
                sbd.append(port);
                sbd.append("/");
                sbd.append(dbName);
                break;
        }
        return sbd.toString();
    }

}
