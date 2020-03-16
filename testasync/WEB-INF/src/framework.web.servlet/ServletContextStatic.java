package framework.web.servlet;

import javax.servlet.ServletContext;
import java.lang.ref.WeakReference;

/**
 * https://tomcat.apache.org/tomcat-9.0-doc/servletapi/javax/servlet/ServletContext.html
 * https://openhome.cc/Gossip/ServletJSP/ServletContext.html
 * https://www.zhihu.com/question/38481443
 *
 * #200225 修改適用於 ServletContextListener 之中，解決 AppSetting 類別建立問題
 */
public class ServletContextStatic {

    private ServletContextStatic() {}

    static {}

    public static ServletContext getInstance() {
        return ServletContextStatic.InstanceHolder.instance;
    }

    public static void setInstance(ServletContext servletContext) {
        InstanceHolder.instance = new WeakReference<>( servletContext ).get();
    }

    private static class InstanceHolder {
        private static ServletContext instance = null;
    }

}
