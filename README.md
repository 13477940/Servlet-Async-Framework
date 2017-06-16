# Servlet-Async-Framework
It's a tomcat webapp for processing asynchronous http request task simple framework instance.

# 必備需求（Demand）
[1] Java JDK 8+<br/>
[2] Apache Tomcat 8.5x

# Startup
放置檔案內容到 tomcat/webapp 之中，並進行編譯，編譯完成後，即可啟動 Tomcat。
complier src/*/*.java after put source code file to tomcat/webapp.<br/>
if compiler done to startup tomcat.

# 編譯方式（Compiler Command）
cd tomcat/webapp/testasync/WEB-INF

＊Like Unix OS:<br/>
javac -cp ../../../lib/*:./lib/*:./classes/:. -d ./classes/. -encoding utf-8 src/*/*.java

＊Windows OS:<br/>
javac -cp ../../../lib/*;./lib/*;./classes/;. -d ./classes/. -encoding utf-8 src/*/*.java
