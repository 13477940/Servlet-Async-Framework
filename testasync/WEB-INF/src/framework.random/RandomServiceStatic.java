package framework.random;

public class RandomServiceStatic {

    private static final RandomService instance;

    private RandomServiceStatic() {}

    static {
        instance = new RandomServiceStatic.Instance();
    }

    public static RandomService getInstance() {
        return instance;
    }

    private static class Instance extends RandomService {}

}
