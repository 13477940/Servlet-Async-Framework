package framework.hash;

public class HashBuilder {

    private static HashService instance;

    static {
        instance = new HashService();
    }

    public static HashService build() {
        return instance;
    }

}
