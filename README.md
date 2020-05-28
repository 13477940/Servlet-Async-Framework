# Servlet-Async-Framework
It's a tomcat(or jetty) webapp for processing asynchronous http request task simple framework instance.

# Features
＊非同步處理每個連結至 Servlet 的 HttpRequest（AsyncHttpServlet）<br>
＊可自定義擴充伺服器功能的 RequestHandler 與責任鏈模式架構（Chain Of Responsibility Pattern）<br>
＊檔案上傳處理（Easy File Upload）<br>
＊伺服器端檔案瀏覽及下載（File Explore & Download）<br>
＊HTTP Parameters 封裝處理，方便調用（Easy to use HTTP Parameters）<br>
＊實作類似於 Android Handler 功能處理後端非同步任務（Android Handler Instance for Servlet AsyncTask）<br>
＊簡易型的 JDK HttpClient 功能（Simplified JDK HttpClient）

# Require
* Java OpenJDK 8+(recommend use LTS version or Latest version)
  * <a href="https://adoptopenjdk.net">AdoptOpenJDK</a>
* Apache Tomcat 8.5+ or Jetty ( attention JDBC use )
  * <a href="http://tomcat.apache.org">Tomcat</a>
  * <a href="https://www.eclipse.org/jetty">Jetty (recommend)</a>
* Google Gson
  * <a href="https://github.com/google/gson">google/gson</a>
* Elopteryx/upload-parser
  * <a href="https://github.com/Elopteryx/upload-parser">Elopteryx/upload-parser</a>

# Optional
<a href="https://stackoverflow.com/questions/6981564/why-must-the-jdbc-driver-be-put-in-tomcat-home-lib-folder/7198049#7198049">Why must the JDBC driver be put in TOMCAT_HOME/lib folder?</a><br>

* tomcat/lib/ -> put supported JDBC lib for access database
  * <a href="https://github.com/MariaDB/mariadb-connector-j">MariaDB</a>
  * <a href="https://github.com/mysql/mysql-connector-j">MySQL</a>
  * <a href="https://github.com/pgjdbc/pgjdbc">PostgreSQL</a>
  * <a href="https://github.com/Microsoft/mssql-jdbc">MSSQL</a>

# Getting Started
放置檔案內容到 tomcat/webapps/testasync 之中，並進行編譯，編譯完成後，即可啟動 Tomcat。<br>
complie src/java after put source code file to tomcat/webapps/testasync.<br>
if compile done to startup tomcat.<br>
<br>
＊Index(Form-Data Request Test):
```
http://ip:port/testasync/
```
＊Page Request(Parameters Test):
```
http://ip:port/testasync/service?page=test
```
＊File Request(File Explore & Download):
```
http://ip:port/testasync/service?file=value
```

# Compile Command
Windows OS:
```
cd tomcat\webapps\testasync\WEB-INF\
javac -cp ..\..\..\tomcat\lib\*;.\lib\*;.\classes\; -d .\classes -encoding utf-8 .\src\*\*.java
```

Unix / Linux / Mac OS:
```
cd tomcat/webapps/testasync/WEB-INF/
javac -cp ../../../tomcat/lib/*:./lib/*:./classes/: -d ./classes -encoding utf-8 ./src/*/*.java
```
# RequestHandler Example
A RequestHandler can handle a http request task or a type of task.<br>
Create a complete website MicroService or API with multiple RequestHandlers.
```java
package app.handler;

import framework.file.FileFinder;
import framework.observer.Bundle;
import framework.observer.Handler;
import framework.observer.Message;
import framework.setting.AppSetting;
import framework.web.context.AsyncActionContext;
import framework.web.handler.RequestHandler;

import javax.servlet.http.HttpServletResponse;
import java.io.File;

public class PageHandler extends RequestHandler {

    private AsyncActionContext requestContext;

    @Override
    public void startup(AsyncActionContext asyncActionContext) {
        if(checkIsMyJob(asyncActionContext)) {
            this.requestContext = asyncActionContext;
            processRequest();
        } else {
            this.passToNext(asyncActionContext);
        }
    }

    @Override
    protected boolean checkIsMyJob(AsyncActionContext asyncActionContext) {
        if(asyncActionContext.isFileAction()) return false;
        if(null != asyncActionContext.getResourceExtension()) return false; // 排除資源類請求
        if("page".equals(asyncActionContext.getParameters().get("page"))) return true;
        return asyncActionContext.getParameters().size() == 0;
    }

    private void processRequest() {
        String dirSlash = new AppSetting.Builder().build().getDirSlash();
        switch (requestContext.getUrlPath()) {
            case "/":
            case "/index": {
                outputPage(dirSlash + "index.html");
            } break;
            default: {
                response404();
            } break;
        }
    }

    // 指定檔案路徑後輸出該頁面檔案，如果仍然不具有檔案則回傳 404
    private void outputPage(String path) {
        outputPage(path, new Handler(){
            @Override
            public void handleMessage(Message m) {
                super.handleMessage(m);
                requestContext.complete();
            }
        });
    }
    private void outputPage(String path, Handler handler) {
        File file = getPageFile(path);
        if(null != file && file.exists()) {
            requestContext.outputFileToResponse(file, file.getName(), "text/html", false, handler);
        } else {
            response404(handler);
        }
    }

    private File getPageFile(String path) {
        File appDir = new FileFinder.Builder().build().find("WEB-INF").getParentFile();
        File res = new File(appDir.getPath() + path);
        if(!res.exists()) return null;
        return res;
    }

    private void response404() {
        response404(new Handler(){
            @Override
            public void handleMessage(Message m) {
                super.handleMessage(m);
                requestContext.complete();
            }
        });
    }

    private void response404(Handler handler) {
        try {
            requestContext.getHttpResponse().sendError(HttpServletResponse.SC_NOT_FOUND);
            if(null != handler) {
                Bundle b = new Bundle();
                b.putString("status", "fail");
                b.putString("error_code", "404");
                b.putString("msg_zht", "沒有正確的指定頁面網址");
                Message m = handler.obtainMessage();
                m.setData(b);
                m.sendToTarget();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
```
# Use WebAppServicePool build a Chain Of Responsibility
```java
package app.listener;

import app.handler.*;
import framework.web.executor.WebAppServicePoolStatic;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class AppContextListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        WebAppServicePoolStatic.getInstance().addHandler(new PageHandler());
        WebAppServicePoolStatic.getInstance().addHandler(new ResourceFileHandler());
        WebAppServicePoolStatic.getInstance().addHandler(new FileHandler());
        WebAppServicePoolStatic.getInstance().addHandler(new UploadHandler());
        WebAppServicePoolStatic.getInstance().addHandler(new ParameterHandler());
        // etc...
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {}

}
```
# Build a database connection pool
```java
package app.database;

import framework.database.datasource.TomcatDataSource;
import framework.database.interfaces.ConnectionPool;

public class MyConnectionPoolStatic {

    private static ConnectionPool instance = null;

    public static ConnectionPool getInstance() {
        if(null == instance) {
            try {
                instance = new TomcatDataSource.Builder()
                        .setIP("db_ip")
                        .setPort("db_port")
                        .setDatabaseType("mysql") // mariadb, mysql, postgresql or mssql
                        .setAccount("db_acc")
                        .setPassword("db_pwd")
                        .setDatabaseName("db_name")
                        .setUseSSL(true)
                        .build();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return instance;
    }

}
```
# How To Use Database Functions
```java
// build a database action
DatabaseAction dbAct = new DatabaseAction.Builder()
        .setConnection(MyConnectionPoolStatic.getInstance().getConnection())
        .setSQL("your_sql_command")
        .setParameters(ArrayList<String> params) // not necessary
        .build();

// dbAct.query(), dbAct.update() ... etc
dbAct.execute();

// close ConnectionPool
MyConnectionPoolStatic.getInstance().shutdown();
```
