package framework.web.multipart;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class FileItem {

    private File file;
    private String name;
    private String fieldName;
    private String contentType;
    private Long size;
    private Boolean isFormField;

    private FileItem(File file, String name, String fieldName, String contentType, Long size, Boolean isFormField) {
        this.file = file;
        this.name = name;
        this.fieldName = fieldName;
        this.contentType = contentType;
        this.size = size;
        this.isFormField = isFormField;
    }

    public void setFile(File file) { this.file = file; }
    public File getFile() {
        return this.file;
    }

    public void setName(String name) { this.name = name; }
    public String getName() {
        return this.name;
    }

    public void setFieldName(String fieldName) { this.fieldName = fieldName; }
    public String getFieldName() {
        return this.fieldName;
    }

    public void setContentType(String contentType) { this.contentType = contentType; }
    public String getContentType() {
        return this.contentType;
    }

    public void setSize(Long size) { this.size = size; }
    public Long getSize() {
        return this.size;
    }

    public void setIsFormField(Boolean isFormField) { this.isFormField = isFormField; }
    public Boolean isFormField() {
        return this.isFormField;
    }

    public String getContent() {
        StringBuilder sbd = new StringBuilder();
        try {
            for(String line : Files.readAllLines(this.file.toPath(), StandardCharsets.UTF_8)) {
                sbd.append(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sbd.toString();
    }

    public InputStream getInputStream() {
        InputStream inputStream = null;
        try {
            inputStream = new BufferedInputStream(new FileInputStream(this.file));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new WeakReference<>( inputStream ).get();
    }

    public static class Builder {

        private File file = null;
        private String name = null;
        private String fieldName = null;
        private String contentType = null;
        private Long size = null;
        private Boolean isFormField = null;

        public FileItem.Builder setFile(File file) {
            this.file = file;
            return this;
        }

        public FileItem.Builder setName(String name) {
            this.name = name;
            return this;
        }

        public FileItem.Builder setFieldName(String fieldName) {
            this.fieldName = fieldName;
            return this;
        }

        public FileItem.Builder setContentType(String contentType) {
            this.contentType = contentType;
            return this;
        }

        public FileItem.Builder setSize(Long size) {
            this.size = size;
            return this;
        }

        public FileItem.Builder setIsFormField(Boolean isFormField) {
            this.isFormField = isFormField;
            return this;
        }

        public FileItem build() {
            return new FileItem(file, name, fieldName, contentType, size, isFormField);
        }

    }

}
