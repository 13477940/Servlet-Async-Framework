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
[4] tomcat/webapps/testasync/WEB-INF/lib/ -> put <a href="https://github.com/alibaba/fastjson">alibaba/fastjson</a> lib</br>

# 選配套件（Optional）
[1] tomcat/webapps/testasync/WEB-INF/lib/ -> put <a href="https://github.com/brettwooldridge/HikariCP">brettwooldridge/HikariCP</a> lib</br>
[2] tomcat/webapps/testasync/WEB-INF/lib/ -> put supported JDBC lib for access database<br/>
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
