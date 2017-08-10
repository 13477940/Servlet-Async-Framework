package framework.session.service;

public class SessionBuilder {

    private static SessionService instance = null;

    static {
        instance = new SessionService();
    }

    public static SessionService build() {
        return instance;
    }

}
