package framework.database.interfaces;

import java.sql.Connection;

public interface ConnectionPool {

    Connection getConnection();

    void shutdown();

}
