# Servlet-Async-Framework
It's a tomcat webapp for processing asynchronous http request task simple framework instance.

# 已具備功能（Features）
＊非同步處理每個連結至 Servlet 的 HttpRequest（AsyncHttpServlet）<br/>
＊可自定義擴充伺服器功能的 RequestHandler 與責任鏈模式架構（Chain Of Responsibility Pattern）<br/>
＊檔案上傳處理（File Upload）<br/>
＊伺服器端檔案瀏覽及下載（File Explore & Download）<br/>
＊HTTP Parameters 封裝處理，方便調用（Easy to use HTTP Parameters）<br/>
＊實作類似於 Android Handler 功能處理後端非同步任務（Android Handler Instance for Servlet AsyncTask）

# 必備需求（Demand）
[1] Java JDK 8+<br/>
[2] Apache Tomcat 8.5x<br/>
[3] put "testasync" files to "tomcat/webapps/testasync" folder<br/>
<!-- [4] tomcat/webapps/testasync/WEB-INF/lib/ -> put <a href="https://github.com/alibaba/fastjson">alibaba/fastjson</a> lib -->

# 選配套件（Optional）
<a href="https://stackoverflow.com/questions/6981564/why-must-the-jdbc-driver-be-put-in-tomcat-home-lib-folder/7198049#7198049">Why must the JDBC driver be put in TOMCAT_HOME/lib folder?</a><br/>
＊tomcat/lib/ -> put supported JDBC lib for access database<br/>
<a href="https://www.microsoft.com/en-us/download/details.aspx?id=11774">MSSQL</a><br/>
<a href="https://dev.mysql.com/downloads/connector/j/">MySQL</a><br/>
<a href="https://mariadb.com/kb/en/mariadb/about-mariadb-connector-j/">MariaDB</a><br/>
<a href="https://jdbc.postgresql.org/">PostgreSQL</a>

# 如何使用（Startup）
放置檔案內容到 tomcat/webapps/testasync 之中，並進行編譯，編譯完成後，即可啟動 Tomcat。<br/>
complie src/java after put source code file to tomcat/webapps/testasync.<br/>
if compile done to startup tomcat.<br/>
<br/>
＊Index(Form-Data Request Test):
<pre><code>http://ip:port/testasync/</code></pre>
＊Page Request(Parameters Test):
<pre><code>http://ip:port/testasync/service?page=test</code></pre>
＊File Request(File Explore & Download):
<pre><code>http://ip:port/testasync/service?file=value</code></pre>

# 編譯方式（Compile Command）
command mode to folder：<br/>
<pre><code>cd tomcat/webapps/testasync/WEB-INF/</code></pre>
or Windows:
<pre><code>cd tomcat\webapps\testasync\WEB-INF\</code></pre>

＊For Unix/Linux/Mac OS:<br/>
<pre><code>javac -cp ../../../lib/*:./lib/*:./classes/:. -d ./classes/. -encoding utf-8 ./src/*/*.java</code></pre>

＊For Windows OS:<br/>
<pre><code>javac -cp ..\..\..\lib\*;.\lib\*;.\classes\;. -d .\classes\. -encoding utf-8 .\src\*\*.java</code></pre>

# Database Functions Example
＊For "Read(Select or Query)" Database Table:<br/>
<pre><code>public class MyDatabaseQuery extends QueryWorker {
    public DataTable queryTable() {
        String sql = "SELECT * FROM MyDBTable;";
        DataTable res = simpleQuery(sql);
        return res;
    }
}</code></pre>

＊For "Create/Update/Delete" Database Table:<br/>
<pre><code>public class MyDatabaseRecord extends RecordWorker {
    public DataTable recordTable() {
        String sql = "INSERT INTO MyDBTable VALUES(...);";
        SQLContext sqx = getRecordService().getSQLContext(sql);
        DataTable res = getRecordService().executeRecord(sqx);
        return res;
    }
}</code></pre>

＊if need with further operating, can get PreparedStatement from SQLContext
<pre><code>String sql = "...";
SQLContext sqx = getQueryService().getSQLContext(sql); // for Read
try {
    PreparedStatement ps = sqx.getPreparedStatement();
} catch (Exception e) {
    e.printStackTrace();
}
</code></pre>
<pre><code>String sql = "...";
SQLContext sqx = getRecordService().getSQLContext(sql); // for CUD
try {
    PreparedStatement ps = sqx.getPreparedStatement();
} catch (Exception e) {
    e.printStackTrace();
}
</code></pre>
