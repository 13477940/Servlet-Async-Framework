package framework.sql.data.query;

import framework.sql.context.SQLContext;
import framework.sql.data.DataService;
import framework.sql.data.pattern.DataTable;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class QueryService extends DataService {

    public DataTable executeQuery(SQLContext sqx) {
        PreparedStatement pst = sqx.getPreparedStatement();
        DataTable dt = new DataTable();
        try {
            pst.setEscapeProcessing(true);
            ResultSet rs = pst.executeQuery();
            dt = new DataTable(rs);
        } catch(Exception e) {
            e.printStackTrace();
        }
        return dt;
    }

}
