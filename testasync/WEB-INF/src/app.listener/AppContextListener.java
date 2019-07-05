package app.listener;

import app.handler.FileHandler;
import app.handler.PageHandler;
import app.handler.ResourceFileHandler;
import app.handler.UploadHandler;
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
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {}

}
