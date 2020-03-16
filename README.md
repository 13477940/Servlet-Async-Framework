# Servlet-Async-Framework
It's a tomcat webapp for processing asynchronous http request task simple framework instance.

# Features
＊非同步處理每個連結至 Servlet 的 HttpRequest（AsyncHttpServlet）<br/>
＊可自定義擴充伺服器功能的 RequestHandler 與責任鏈模式架構（Chain Of Responsibility Pattern）<br/>
＊檔案上傳處理（Easy File Upload）<br/>
＊伺服器端檔案瀏覽及下載（File Explore & Download）<br/>
＊HTTP Parameters 封裝處理，方便調用（Easy to use HTTP Parameters）<br/>
＊實作類似於 Android Handler 功能處理後端非同步任務（Android Handler Instance for Servlet AsyncTask）

# Require
[1] Java OpenJDK 8+(recommend use latest version)<br/>
[2] Apache Tomcat 8.5+ or Jetty(attention JDBC use)<br/>
[3] <a href="https://github.com/alibaba/fastjson">FastJSON</a><br/>
[4] put "testasync" files to "tomcat/webapps/testasync" folder<br/>

# Optional
<a href="https://eclipse-ee4j.github.io/mail/">Jakarta Mail</a><br/>
<a href="https://eclipse-ee4j.github.io/jaf/">Jakarta Activation</a><br/>
<br/>
<a href="https://stackoverflow.com/questions/6981564/why-must-the-jdbc-driver-be-put-in-tomcat-home-lib-folder/7198049#7198049">Why must the JDBC driver be put in TOMCAT_HOME/lib folder?</a><br/>
＊tomcat/lib/ -> put supported JDBC lib for access database<br/>
<a href="https://github.com/Microsoft/mssql-jdbc">MSSQL</a><br/>
<a href="https://github.com/mysql/mysql-connector-j">MySQL</a><br/>
<a href="https://github.com/MariaDB/mariadb-connector-j">MariaDB</a><br/>
<a href="https://github.com/pgjdbc/pgjdbc">PostgreSQL</a>

# Getting Started
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

# Compile Command
command mode to folder：<br/>
<pre><code>cd tomcat/webapps/testasync/WEB-INF/</code></pre>
or Windows:
<pre><code>cd tomcat\webapps\testasync\WEB-INF\</code></pre>

＊For Unix/Linux/Mac OS:<br/>
<pre><code>javac -cp ../../../lib/*:./lib/*:./classes/:. -d ./classes/. -encoding utf-8 ./src/*/*.java</code></pre>

＊For Windows OS:<br/>
<pre><code>javac -cp ..\..\..\lib\*;.\lib\*;.\classes\;. -d .\classes\. -encoding utf-8 .\src\*\*.java</code></pre>

# How To Use Database Functions
<pre><code>ConnectionPool connectionPool = new TomcatDataSource.Builder()
        .setIP("your_db_ip")
        .setPort("your_db_port")
        .setAcc("your_db_user")
        .setPassword("your_db_password")
        .setDatabaseType("your_db_type") // mssql, postgresql, mysql or mariadb
        .setDatabaseName("your_db_name")
        .build();

DatabaseAction databaseAction = new DatabaseAction.Builder()
        .setSQL("your_sql_command")
        .setConnection(connectionPool.getConnection())
        .setParameters(ArrayList<String> params) // not necessary
        .build();

// databaseAction.query(), databaseAction.update() ... etc
databaseAction.execute();

// close ConnectionPool
connectionPool.shutdown();</code></pre>
