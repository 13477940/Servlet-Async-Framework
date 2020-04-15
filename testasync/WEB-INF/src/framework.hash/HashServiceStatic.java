package framework.hash;

public class HashServiceStatic {

    private static final HashService instance;

    private HashServiceStatic() {}

    static {
        instance = new HashServiceStatic.Instance();
    }

    public static HashService getInstance() {
        return instance;
    }


    private static class Instance extends HashService {}

}
