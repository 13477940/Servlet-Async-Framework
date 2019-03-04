package framework.web.session.service;

public class SessionServiceStatic {

    static {}

    public static SessionService getInstance() {
        return InstanceHolder.instance;
    }

    static class InstanceHolder {
        static SessionService instance = new SessionServiceStatic.Instance();
    }

    private static class Instance extends SessionService {}

}
