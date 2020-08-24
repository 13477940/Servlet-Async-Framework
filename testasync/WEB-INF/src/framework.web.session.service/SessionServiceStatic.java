package framework.web.session.service;

public class SessionServiceStatic {

    private static final SessionService instance;

    private SessionServiceStatic () {}

    static {
        instance = new SessionServiceStatic.Instance();
    }

    public static SessionService getInstance() {
        return instance;
    }

    private static class Instance extends SessionService {}

}
