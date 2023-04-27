package app.listener;

import app.handler.*;
import framework.web.executor.WebAppServicePoolStatic;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

/**
 * webapp main program entry point
 * modularization load request handler class
 * you can add request handlers to increase functionality
 */
public class AppContextListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContextListener.super.contextInitialized(sce);
        {
            WebAppServicePoolStatic.getInstance().addHandler(new PageHandler());
            WebAppServicePoolStatic.getInstance().addHandler(new ResourceFileHandler());
            WebAppServicePoolStatic.getInstance().addHandler(new FileHandler());
            WebAppServicePoolStatic.getInstance().addHandler(new UploadHandler());
            WebAppServicePoolStatic.getInstance().addHandler(new ParameterHandler());
            WebAppServicePoolStatic.getInstance().addHandler(new SessionHandler());
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        ServletContextListener.super.contextDestroyed(sce);
    }

}
