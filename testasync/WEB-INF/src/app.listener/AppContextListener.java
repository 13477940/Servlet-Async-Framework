package app.listener;

import app.handler.FileHandler;
import app.handler.PageHandler;
import app.handler.UploadHandler;
import framework.web.executor.WebAppServicePool;
import framework.web.executor.WebAppServicePoolBuilder;

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
    public void contextDestroyed(ServletContextEvent servletContextEvent) {}

    // 建立責任鏈實作節點關係
    private void createWorkListDefine() {
        WebAppServicePool servicePool = WebAppServicePoolBuilder.build();
        servicePool.addHandler(new PageHandler());
        servicePool.addHandler(new FileHandler());
        servicePool.addHandler(new UploadHandler());
    }

}
