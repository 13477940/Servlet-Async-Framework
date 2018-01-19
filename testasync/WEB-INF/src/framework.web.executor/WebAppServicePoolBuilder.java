package framework.web.executor;

public class WebAppServicePoolBuilder {

    private static WebAppServicePool instance;

    static {
        instance = new WebAppServicePool();
    }

    public static WebAppServicePool build() {
        return instance;
    }

}
