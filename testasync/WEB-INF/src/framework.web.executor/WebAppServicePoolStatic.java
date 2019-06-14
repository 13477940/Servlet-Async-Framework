package framework.web.executor;

public class WebAppServicePoolStatic {

    private WebAppServicePoolStatic() {}

    static {}

    public static WebAppServicePool getInstance() {
        return InstanceHolder.instance;
    }

    private static class InstanceHolder {
        static WebAppServicePool instance = new WebAppServicePoolStatic.Instance();
    }

    private static class Instance extends WebAppServicePool {}

}
