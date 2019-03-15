package framework.mail.context;

import java.io.File;
import java.util.ArrayList;

public class MailContext {

    private String sender = null; // 寄件人
    private ArrayList<String> recipients = null; // 收件人
    private String subject = null; // 主旨
    private String content = null; // 內容
    private ArrayList<File> files = null; // 大部分限制為 10~30MiB，盡量減少傳檔的動作

    private MailContext() {}

    public void setSender(String sender) { this.sender = sender; }
    public String getSender() {
        return this.sender;
    }

    public void setRecipients(ArrayList<String> recipients) {
        this.recipients = recipients;
    }
    public ArrayList<String> getRecipients() {
        return this.recipients;
    }

    public void addRecipient(String mailAddress) {
        if(null == recipients) {
            recipients = new ArrayList<>();
        }
        recipients.add(mailAddress);
    }

    public void clearRecipients() {
        if(null != recipients) recipients.clear();
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }
    public String getSubject() {
        return this.subject;
    }

    public void setContent(String content) {
        this.content = content;
    }
    public String getContent() {
        return this.content;
    }

    public void setFiles(ArrayList<File> files) {
        this.files = files;
    }
    public ArrayList<File> getFiles() {
        return this.files;
    }

    public void addFile(File file) {
        if(null == files) {
            files = new ArrayList<>();
        }
        files.add(file);
    }

    public void clearFiles() {
        if(null != files) files.clear();
    }

    public static class Builder {

        private String sender = null; // 寄件人
        private ArrayList<String> recipients = null; // 收件人
        private String subject = null; // 主旨
        private String content = null; // 內容
        private ArrayList<File> files = null; // 大部分限制為 10~30MiB，盡量減少傳檔的動作

        public MailContext.Builder setSender(String sender) {
            this.sender = sender;
            return this;
        }

        public MailContext.Builder setRecipients(ArrayList<String> recipients) {
            this.recipients = recipients;
            return this;
        }

        public MailContext.Builder setSubject(String subject) {
            this.subject = subject;
            return this;
        }

        public MailContext.Builder setContent(String content) {
            this.content = content;
            return this;
        }

        public MailContext.Builder setFiles(ArrayList<File> files) {
            this.files = files;
            return this;
        }

        public MailContext build() {
            if(null == sender) {
                try {
                    throw new Exception("缺少寄件者");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if(null == recipients || recipients.size() == 0) {
                try {
                    throw new Exception("缺少收件者");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if(null == subject) { subject = ""; }
            if(null == content) { content = ""; }
            // build
            MailContext instance = new MailContext();
            instance.setSender(sender);
            instance.setRecipients(recipients);
            instance.setSubject(subject);
            instance.setContent(content);
            instance.setFiles(files);
            return instance;
        }

    }

}
