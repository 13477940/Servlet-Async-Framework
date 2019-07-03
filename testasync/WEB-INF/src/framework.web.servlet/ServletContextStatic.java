package framework.web.servlet;

import javax.servlet.ServletContext;

/**
 * https://tomcat.apache.org/tomcat-9.0-doc/servletapi/javax/servlet/ServletContext.html
 * https://openhome.cc/Gossip/ServletJSP/ServletContext.html
 * https://www.zhihu.com/question/38481443
 */
public class ServletContextStatic {

    private ServletContextStatic() {}

    static {}

    public static ServletContext getInstance() {
        return ServletContextStatic.InstanceHolder.instance;
    }

    static class InstanceHolder {
        static ServletContext instance = null;
    }

}
