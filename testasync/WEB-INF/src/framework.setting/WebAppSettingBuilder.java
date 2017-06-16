package framework.setting;

public class WebAppSettingBuilder {

    private static WebAppSetting instance = null;

    static {
        instance = new WebAppSetting();
    }

    public static WebAppSetting build() {
        return instance;
    }

}
