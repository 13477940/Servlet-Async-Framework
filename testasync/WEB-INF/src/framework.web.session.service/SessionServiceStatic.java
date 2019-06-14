package framework.web.session.service;

public class SessionServiceStatic {

    private SessionServiceStatic () {}

    static {}

    public static SessionService getInstance() {
        return InstanceHolder.instance;
    }

    private static class InstanceHolder {
        static SessionService instance = new SessionServiceStatic.Instance();
    }

    private static class Instance extends SessionService {}

}
