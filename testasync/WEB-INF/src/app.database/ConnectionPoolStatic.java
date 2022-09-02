package app.database;

import framework.database.datasource.TomcatDataSource;
import framework.database.interfaces.ConnectionPool;

/**
 * 此處是資料庫連結資源作為常駐服務的一個範例，
 * 使每次使用資料庫的時候都有統一調用連結資源的集中點
 */
public class ConnectionPoolStatic {

    private static ConnectionPool instance = null;

    public static ConnectionPool getInstance() {
        if(null == instance) {
            try {
                // you should insert database login info right here.
                instance = new TomcatDataSource.Builder()
                        .setIP("your_db_ip")
                        .setPort("your_db_port")
                        .setDatabaseType("your_db_type") // mariadb, mysql, postgresql, mssql or etc.
                        .setAccount("your_db_acc") // account
                        .setPassword("your_db_pwd") // password
                        .setDatabaseName("your_db_name") // target database name
                        .build();
            } catch (Exception e) {
                e.printStackTrace();
                instance = null;
            }
        }
        return instance;
    }

}
