package framework.time;

public class TimeServiceStatic {

    private static TimeServiceStatic.Instance instance = null;

    static {}

    public static TimeService getInstance() {
        if(null == instance) instance = new TimeServiceStatic.Instance();
        return instance;
    }

    private static class Instance extends TimeService {}

}
