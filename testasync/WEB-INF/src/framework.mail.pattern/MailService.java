package framework.mail.pattern;

import framework.mail.context.MailContext;
import framework.observer.Handler;

import java.util.Properties;

public interface MailService {

    abstract Properties getMailProperties();

    /**
     * 設定 Mail 服務的登入帳戶 Session 為何
     */
    abstract void setMailSession(String account, String password);

    /**
     * 自定義發送信件的實作，每次的發送都要自定義一個 Handler 進行結果回傳
     */
    abstract void sendMail(MailContext mailContext, Handler handler);

}
