package framework.hash;

public class HashServiceStatic {

    private static HashService instance = null;

    static {}

    public static HashService build() {
        if(null == instance) instance = new HashServiceStatic.Instance();
        return instance;
    }

    private static class Instance extends HashService {}

}
