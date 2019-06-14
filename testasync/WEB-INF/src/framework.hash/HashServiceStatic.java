package framework.hash;

public class HashServiceStatic {

    private HashServiceStatic() {}

    static {}

    public static HashService getInstance() {
        return InstanceHolder.instance;
    }

    private static class InstanceHolder {
        static HashService instance = new HashServiceStatic.Instance();
    }

    private static class Instance extends HashService {}

}
