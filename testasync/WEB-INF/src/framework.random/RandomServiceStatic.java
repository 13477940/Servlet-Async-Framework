package framework.random;

public class RandomServiceStatic {

    private static RandomService instance = null;

    static {}

    public static RandomService getInstance() {
        if(null == instance) instance = new RandomServiceStatic.Instance();
        return instance;
    }

    private static class Instance extends RandomService {}

}
