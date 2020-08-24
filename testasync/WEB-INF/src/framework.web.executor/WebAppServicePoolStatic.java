package framework.web.executor;

public class WebAppServicePoolStatic {

    private static final WebAppServicePool instance;

    private WebAppServicePoolStatic() {}

    static {
        instance = new WebAppServicePoolStatic.Instance();
    }

    public static WebAppServicePool getInstance() {
        return instance;
    }

    private static class Instance extends WebAppServicePool {}

}
