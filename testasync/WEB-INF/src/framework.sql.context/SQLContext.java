package framework.sql.context;

import framework.hash.HashBuilder;
import framework.random.RandomBuilder;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class SQLContext {

    private Connection conn = null;
    private String sql = null;
    private PreparedStatement pst = null;
    private String ssid = null;

    public SQLContext(Connection conn, PreparedStatement pst, String sql) {
        this.ssid = HashBuilder.build().stringToSHA1(RandomBuilder.build().getTimeHash());
        this.conn = conn;
        this.pst = pst;
        this.sql = sql;
    }

    public String getSSID() {
        return ssid;
    }

    public PreparedStatement getPreparedStatement() {
        return pst;
    }
    public void setPreparedStatement(PreparedStatement pst) {
        this.pst = pst;
    }

    public String getSQL() {
        return sql;
    }
    public void setSQL(String sql) {
        this.sql = sql;
    }

    public Connection getConnection() {
        return conn;
    }
    public void setConnection(Connection conn) {
        this.conn = conn;
    }

}
