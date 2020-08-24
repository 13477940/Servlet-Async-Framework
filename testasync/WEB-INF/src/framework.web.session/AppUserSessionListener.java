package framework.web.session;

import framework.web.session.service.SessionCounter;
import framework.web.session.service.SessionServiceStatic;

import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

/**
 * 此類別使用於 WEB-INF/web.xml -> <listener></listener> 定義之中
 *
 * https://openhome.cc/Gossip/ServletJSP/BehindHttpSession.html
 */
public class AppUserSessionListener implements HttpSessionListener {

    @Override
    public void sessionCreated(HttpSessionEvent event) {
        SessionCounter.addCount(event.getSession());
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent event) {
        SessionCounter.delCount(event.getSession());
        SessionServiceStatic.getInstance().removeUser(event.getSession());
    }

}
