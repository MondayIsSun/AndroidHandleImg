package mr.lmd.personal.imageloader.bean;

/**
 * 文件夹Module
 * Created by LinMingDao on 2015/11/25.
 */
public class FolderBean {

    private String dir;//当前文件夹的路径
    private String firstImgPath;
    private String name;
    private int count;

    public String getDir() {
        return dir;
    }

    public void setDir(String dir) {
        this.dir = dir;
        int lastIndexOf = this.dir.lastIndexOf("/") + 1;
        this.name = this.dir.substring(lastIndexOf);
    }

    public String getFirstImgPath() {
        return firstImgPath;
    }

    public void setFirstImgPath(String firstImgPath) {
        this.firstImgPath = firstImgPath;
    }

    public String getName() {
        return name;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
