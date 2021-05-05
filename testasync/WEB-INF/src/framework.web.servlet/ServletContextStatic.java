package framework.web.servlet;

import jakarta.servlet.ServletContext;

import java.lang.ref.WeakReference;

/**
 * https://tomcat.apache.org/tomcat-9.0-doc/servletapi/javax/servlet/ServletContext.html
 * https://openhome.cc/Gossip/ServletJSP/ServletContext.html
 * https://www.zhihu.com/question/38481443
 *
 * 2021-01-13 原先 AppSetting 會在執行中消失的問題已解決，屬於 ThreadPool 自建發生的問題
 * 此問題解決已更改 ThreadPool 建立時採用預設的 Executors.newCachedThreadPool()。
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
