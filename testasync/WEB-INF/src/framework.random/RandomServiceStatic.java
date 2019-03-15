package framework.random;

public class RandomServiceStatic {

    private RandomServiceStatic() {}

    static {}

    public static RandomService getInstance() {
        return InstanceHolder.instance;
    }

    static class InstanceHolder {
        static RandomService instance = new RandomServiceStatic.Instance();
    }

    private static class Instance extends RandomService {}

}
