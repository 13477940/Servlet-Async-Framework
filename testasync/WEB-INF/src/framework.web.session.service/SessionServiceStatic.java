package framework.web.session.service;

public class SessionServiceStatic {

    private static SessionService instance = null;

    static {}

    public static SessionService getInstance() {
        if(null == instance) instance = new SessionServiceStatic.Instance();
        return instance;
    }

    private static class Instance extends SessionService {}

}
