package framework.database.connection;

/**
 * 關於 Connection Pool 基礎設定及預設值封裝
 */
public class ConnectContext {

    private String DB_Type = null;
    private String DB_IP = null;
    private String DB_Port = null;
    private String DB_Name = null;
    private String DB_ACC = null;
    private String DB_PWD = null;

    private int DB_Max_Age = 0;
    private int DB_Max_Active = 0;
    private int DB_Max_Idle = 0;
    private int DB_InitialSize = 0;
    private int DB_Min_Idle = 0;
    private int DB_Max_Wait = 0;
    private int DB_RemoveAbandonedTimeout = 0;

    public String getDB_Type() {
        return DB_Type;
    }

    public void setDB_Type(String DB_Type) {
        this.DB_Type = DB_Type;
    }

    public String getDB_IP() {
        return DB_IP;
    }

    public void setDB_IP(String DB_IP) {
        this.DB_IP = DB_IP;
    }

    public String getDB_Port() {
        return DB_Port;
    }

    public void setDB_Port(String DB_Port) {
        this.DB_Port = DB_Port;
    }

    public String getDB_Name() {
        return DB_Name;
    }

    public void setDB_Name(String DB_Name) {
        this.DB_Name = DB_Name;
    }

    public String getDB_ACC() {
        return DB_ACC;
    }

    public void setDB_ACC(String DB_ACC) {
        this.DB_ACC = DB_ACC;
    }

    public String getDB_PWD() {
        return DB_PWD;
    }

    public void setDB_PWD(String DB_PWD) {
        this.DB_PWD = DB_PWD;
    }

    public int getDB_Max_Age() {
        if(0 == this.DB_Max_Age) {
            return 0;
        }
        return DB_Max_Age;
    }

    public void setDB_Max_Age(int DB_Max_Age) {
        this.DB_Max_Age = DB_Max_Age;
    }

    public int getDB_Max_Active() {
        if(0 == this.DB_Max_Active) {
            return 128;
        }
        return DB_Max_Active;
    }

    public void setDB_Max_Active(int DB_Max_Active) {
        this.DB_Max_Active = DB_Max_Active;
    }

    public int getDB_Max_Idle() {
        if(0 == this.DB_Max_Idle) {
            // should be like DB_Max_Active
            return 128;
        }
        return DB_Max_Idle;
    }

    public void setDB_Max_Idle(int DB_Max_Idle) {
        this.DB_Max_Idle = DB_Max_Idle;
    }

    public int getDB_InitialSize() {
        if(0 == DB_InitialSize) {
            return 10;
        }
        return DB_InitialSize;
    }

    public void setDB_InitialSize(int DB_InitialSize) {
        this.DB_InitialSize = DB_InitialSize;
    }

    public int getDB_Min_Idle() {
        if(0 == DB_Min_Idle) {
            // should be like DB_InitialSize
            return 10;
        }
        return DB_Min_Idle;
    }

    public void setDB_Min_Idle(int DB_Min_Idel) {
        this.DB_Min_Idle = DB_Min_Idel;
    }

    public int getDB_Max_Wait() {
        if(0 == DB_Max_Wait) {
            return 20000; // ms
        }
        return DB_Max_Wait;
    }

    public void setDB_Max_Wait(int DB_Max_Wait) {
        this.DB_Max_Wait = DB_Max_Wait;
    }

    public int getDB_RemoveAbandonedTimeout() {
        if(0 == DB_RemoveAbandonedTimeout) {
            return 600; // sec
        }
        return DB_RemoveAbandonedTimeout;
    }

    public void setDB_RemoveAbandonedTimeout(int DB_RemoveAbandonedTimeout) {
        this.DB_RemoveAbandonedTimeout = DB_RemoveAbandonedTimeout;
    }

}
