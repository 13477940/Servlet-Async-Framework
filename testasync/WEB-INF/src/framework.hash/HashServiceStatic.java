package framework.hash;

public class HashServiceStatic {

    static {}

    public static HashService getInstance() {
        return InstanceHolder.instance;
    }

    static class InstanceHolder {
        static HashService instance = new HashServiceStatic.Instance();
    }

    private static class Instance extends HashService {}

}
