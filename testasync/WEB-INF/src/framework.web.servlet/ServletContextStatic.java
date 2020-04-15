package framework.web.servlet;

import javax.servlet.ServletContext;
import java.lang.ref.WeakReference;

/**
 * https://tomcat.apache.org/tomcat-9.0-doc/servletapi/javax/servlet/ServletContext.html
 * https://openhome.cc/Gossip/ServletJSP/ServletContext.html
 * https://www.zhihu.com/question/38481443
 *
 * 2020-02-25 修改適用於 ServletContextListener 之中，解決 AppSetting 類別建立問題
 * 2020-04-14 修改初始化流程
 */
public class ServletContextStatic {

    private static ServletContext _servletContext;

    private ServletContextStatic() {}

    static {
        _servletContext = null;
    }

    public static ServletContext getInstance() {
        return _servletContext;
    }

    public static void setInstance(ServletContext servletContext) {
        _servletContext = new WeakReference<>( servletContext ).get();
    }

}
