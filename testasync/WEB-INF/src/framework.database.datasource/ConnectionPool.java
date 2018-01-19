package framework.database.datasource;

import java.sql.Connection;

public interface ConnectionPool {

    public Connection getConnection();

    public void shutdown();

}
