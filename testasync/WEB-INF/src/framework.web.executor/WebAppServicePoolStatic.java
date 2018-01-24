package framework.web.executor;

public class WebAppServicePoolStatic {

    private static WebAppServicePool instance = null;

    static {}

    public static WebAppServicePool getInstance() {
        if(null == instance) instance = new WebAppServicePoolStatic.Instance();
        return instance;
    }

    private static class Instance extends WebAppServicePool {}

}
