package framework.web.multipart;

import java.util.ArrayList;
import java.util.Iterator;

public class FileItemList {

    private final ArrayList<FileItem> list;

    public FileItemList() {
        this.list = new ArrayList<>();
    }

    public void add(FileItem fileItem) {
        list.add(fileItem);
    }

    public FileItem get(int index) {
        return list.get(index);
    }

    public Iterator<FileItem> iterator() {
        return list.iterator();
    }

    public int size() {
        return list.size();
    }

    public boolean isEmpty() {
        return list.isEmpty();
    }

    public ArrayList<FileItem> prototype() {
        return list;
    }

}
