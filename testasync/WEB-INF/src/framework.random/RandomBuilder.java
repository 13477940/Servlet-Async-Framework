package framework.random;

public class RandomBuilder {

    private static RandomService instance;

    static {
        instance = new RandomService();
    }

    public static RandomService build() {
        return instance;
    }

}
