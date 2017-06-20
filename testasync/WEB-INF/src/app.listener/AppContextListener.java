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
     * WebApp 啟動時
     */
    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        createWorkListDefine();
    }

    /**
     * WebApp 結束時
     */
    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        TomcatDataSource.shutdown();
    }

    // 建立責任鏈實作節點關係
    private void createWorkListDefine() {
        WebAppServicePoolBuilder.build().addHandler(new PageHandler());
        WebAppServicePoolBuilder.build().addHandler(new FileHandler());
        WebAppServicePoolBuilder.build().addHandler(new UploadHandler());
    }

}
