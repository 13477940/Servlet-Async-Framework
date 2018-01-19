package framework.web.session;

import framework.web.session.service.SessionBuilder;
import framework.web.session.service.SessionCounter;

import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

public class AppUserSessionListener implements HttpSessionListener {

    @Override
    public void sessionCreated(HttpSessionEvent event) {
        SessionCounter.addCount(event.getSession());
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent event) {
        SessionCounter.delCount(event.getSession());
        SessionBuilder.build().removeUser(event.getSession());
    }

}
