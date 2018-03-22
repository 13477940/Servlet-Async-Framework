package framework.hash;

public class HashServiceStatic {

    private static HashService instance = null;

    static {}

    public static HashService getInstance() {
        if(null == instance) instance = new HashServiceStatic.Instance();
        return instance;
    }

    private static class Instance extends HashService {}

}
