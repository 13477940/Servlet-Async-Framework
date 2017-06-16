package framework.time;

public class TimeBuilder {

    private static TimeService instance;

    static {
        instance = new TimeService();
    }

    public static TimeService build() {
        return instance;
    }

}
