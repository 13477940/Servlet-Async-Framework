package app.listener;

import app.handler.FileHandler;
import app.handler.PageHandler;
import app.handler.UploadHandler;
import framework.connector.datasource.TomcatDataSource;
import framework.executor.WebAppServicePoolBuilder;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class AppContextListener implements ServletContextListener {

    /**
     * WebApp 載入時，請於此處建立自定義 Handler 列表
     */
    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        WebAppServicePoolBuilder.build().addHandler(new PageHandler());
        WebAppServicePoolBuilder.build().addHandler(new FileHandler());
        WebAppServicePoolBuilder.build().addHandler(new UploadHandler());
    }

    /**
     * WebApp 結束時，可於此處回收 ConnectionPool 等等的系統資源
     */
    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        TomcatDataSource.shutdown();
    }

}
