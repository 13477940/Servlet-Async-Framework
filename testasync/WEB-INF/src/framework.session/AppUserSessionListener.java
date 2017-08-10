package framework.session;

import framework.session.service.SessionBuilder;

import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

public class AppUserSessionListener implements HttpSessionListener {

    @Override
    public void sessionCreated(HttpSessionEvent event) {
        // session create
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent event) {
        String sessionID = event.getSession().getId();
        SessionBuilder.build().removeUser(sessionID);
    }

}
