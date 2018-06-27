package framework.mail;

import framework.mail.context.MailContext;
import framework.mail.interfaces.MailService;
import framework.observer.Bundle;
import framework.observer.Handler;

import javax.mail.*;
import javax.mail.internet.*;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Properties;

/**
 * only for google mail service used.
 * 如果要連接其他信箱服務，請由 MailService 介面實作
 */
public class GoogleMailService implements MailService {

    private Session session = null;

    @Override
    public Properties getMailProperties() {
        Properties props = System.getProperties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.socketFactory.port", "465");
        props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        props.put("mail.smtp.auth", true);
        props.put("mail.smtp.port", 465);
        return props;
    }

    @Override
    public void setMailSession(String account, String password) {
        Authenticator auth = new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(account, password);
            }
        };
        this.session = Session.getDefaultInstance(getMailProperties(), auth);
    }

    @Override
    public void sendMail(MailContext mailContext, Handler handler) {
        MimeMessage message = new MimeMessage(session);

        // 設定寄件者
        try {
            message.setFrom(new InternetAddress(mailContext.getSender()));
        } catch (Exception e) {
            e.printStackTrace();
            {
                Bundle b = new Bundle();
                b.putString("status", "fail");
                b.putString("msg", "sender_error");
                b.putString("msg_zht", "設定寄件者時錯誤");
                b.put("exception", e);
                sendMsgToHandler(b, handler);
            }
            return;
        }

        // 設定收件者及副本設定等
        try {
            Address[] list = new Address[mailContext.getRecipients().size()];
            for(int i = 0, len = list.length; i < len; i++) {
                String recipient = mailContext.getRecipients().get(i);
                Address addr = new InternetAddress(recipient);
                list[i] = addr;
            }
            message.setRecipients(javax.mail.Message.RecipientType.TO, list);
            // message.setRecipients(javax.mail.Message.RecipientType.CC, list);
            // message.setRecipients(javax.mail.Message.RecipientType.BCC, list);
        } catch (Exception e) {
            e.printStackTrace();
            {
                Bundle b = new Bundle();
                b.putString("status", "fail");
                b.putString("msg", "recipient_error");
                b.putString("msg_zht", "設定收件者時錯誤");
                b.put("exception", e);
                sendMsgToHandler(b, handler);
            }
            return;
        }

        // 寄件主旨及時間點
        try {
            message.setSubject(mailContext.getSubject(), StandardCharsets.UTF_8.name());
            message.setSentDate(new Date());
            // message.setHeader("X-Mailer", "");
        } catch (Exception e) {
            e.printStackTrace();
            {
                Bundle b = new Bundle();
                b.putString("status", "fail");
                b.putString("msg", "subject_error");
                b.putString("msg_zht", "設定寄件主旨錯誤");
                b.put("exception", e);
                sendMsgToHandler(b, handler);
            }
            return;
        }

        // 處理信件內容
        try {
            // 具有檔案時
            if(null != mailContext.getFiles() && mailContext.getFiles().size() > 0) {
                Multipart multipart = new MimeMultipart();

                // 處理檔案
                for(File file : mailContext.getFiles()) {
                    MimeBodyPart filePart = new MimeBodyPart();

                    String fileName = file.getName();
                    filePart.attachFile(file);
                    filePart.setHeader("Content-Type", "application/octet-stream; charset="+StandardCharsets.UTF_8.name());

                    filePart.setFileName(MimeUtility.encodeText(fileName, StandardCharsets.UTF_8.name(), "B"));
                    if(file.exists()) {
                        multipart.addBodyPart(filePart);
                    } else {
                        System.err.println("該檔案不存在："+fileName);
                    }
                }

                BodyPart msg = new MimeBodyPart();
                // 如果要讀取 HTML 格式不能用 setText 要用此方法
                msg.setContent(mailContext.getContent(), "text/html;charset="+StandardCharsets.UTF_8.name());
                multipart.addBodyPart(msg); // file content
                // 放入信件內容
                message.setContent(multipart);
            } else {
                // 放入信件內容
                message.setContent(mailContext.getContent(), "text/html;charset="+StandardCharsets.UTF_8.name());
            }
        } catch (Exception e) {
            e.printStackTrace();
            {
                Bundle b = new Bundle();
                b.putString("status", "fail");
                b.putString("msg", "content_error");
                b.putString("msg_zht", "處理信件內容出錯");
                b.put("exception", e);
                sendMsgToHandler(b, handler);
            }
            return;
        }

        try {
            Transport.send(message);
            {
                Bundle b = new Bundle();
                b.putString("status", "done");
                b.putString("msg", "complete");
                b.putString("msg_zht", "寄送信件成功");
                sendMsgToHandler(b, handler);
            }
        } catch (Exception e) {
            e.printStackTrace();
            {
                Bundle b = new Bundle();
                b.putString("status", "fail");
                b.putString("msg", "transport_error");
                b.putString("msg_zht", "寄送信件出錯");
                b.put("exception", e);
                sendMsgToHandler(b, handler);
            }
        }
    }

    private void sendMsgToHandler(Bundle b, Handler handler) {
        if(null == handler) {
            // 如果沒有指定 handler 接收則直接列印至 log
            System.out.println(b.prototype());
        }
        framework.observer.Message m = handler.obtainMessage();
        m.setData(b);
        m.sendToTarget();
    }

}
