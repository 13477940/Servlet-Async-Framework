# Servlet-Async-Framework
It's a tomcat webapp for processing asynchronous http request task simple framework instance.

# 必備需求（Demand）
[1] Java JDK 8+<br/>
[2] Apache Tomcat 8.5x<br/>
[3] put files to "tomcat/webapp/testasync" folder

# Startup
放置檔案內容到 tomcat/webapp/testasync 之中，並進行編譯，編譯完成後，即可啟動 Tomcat。<br/>
complier src/java after put source code file to tomcat/webapp/testasync.<br/>
if compiler done to startup tomcat.

# 編譯方式（Compiler Command）
command mode to folder：<br/>
<pre><code>cd tomcat/webapp/testasync/WEB-INF</code></pre>

＊Like Unix/Linux/Mac OS:<br/>
<pre><code>javac -cp ../../../lib/*:./lib/*:./classes/:. -d ./classes/. -encoding utf-8 src/*/*.java</code></pre>


＊Windows OS:<br/>
<pre><code>javac -cp ../../../lib/*;./lib/*;./classes/;. -d ./classes/. -encoding utf-8 src/*/*.java</code></pre>
