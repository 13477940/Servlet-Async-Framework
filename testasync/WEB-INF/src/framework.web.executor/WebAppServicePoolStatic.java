package framework.web.executor;

public class WebAppServicePoolStatic {

    static {}

    public static WebAppServicePool getInstance() {
        return InstanceHolder.instance;
    }

    static class InstanceHolder {
        static WebAppServicePool instance = new WebAppServicePoolStatic.Instance();
    }

    private static class Instance extends WebAppServicePool {}

}
