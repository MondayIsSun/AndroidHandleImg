package mr.lmd.personal.disklrucachedemo.example.disklrucachetest;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import mr.lmd.personal.disklrucachedemo.R;
import mr.lmd.personal.disklrucachedemo.jakewharton.disklrucache.DiskLruCache;


public class MainActivity extends Activity {

    private TextView tv;
    private ImageView im;
    private DiskLruCache mDiskLruCache;
    private String IMG_IP = "http://f.hiphotos.baidu.com/image/pic/item/58ee3d6d55fbb2fbfe951a134d4a20a44623dc71.jpg";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tv = (TextView) this.findViewById(R.id.tv);
        im = (ImageView) this.findViewById(R.id.show_img);
        initDiskLruCache();
    }

    private void initDiskLruCache() {
        mDiskLruCache = null;
        try {
            //初始化缓存路径
            File cacheDir = getDiskCacheDir(this, "bitmap");
            if (!cacheDir.exists()) {
                cacheDir.mkdirs();
            }
            //初始化DiskLruCache
            mDiskLruCache = DiskLruCache.open(cacheDir, getAppVersion(this), 1, 10 * 1024 * 1024);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.getImg:
                cacheImg();
                break;
            case R.id.showImgbtn:
                showImg();
                break;
            case R.id.clearCache:
                clearCache();
                break;
            case R.id.getSize:
                getCacheSize();
                break;
            case R.id.deleteAll:
                deleteAll();
                break;
            default:
                break;
        }
    }

    private void deleteAll() {
        /**
         * 这个方法用于将所有的缓存数据全部删除，比如说网易新闻中的那个手动清理缓存功能，
         * 其实只需要调用一下DiskLruCache的delete()方法就可以实现了。
         * 会删除包括日志文件在内的所有文件
         */
        try {
            mDiskLruCache.delete();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void getCacheSize() {
        tv.setText(mDiskLruCache.size() + "");
    }

    private void clearCache() {
        String key = MD5Util.md5(IMG_IP);
        try {
            mDiskLruCache.remove(key);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showImg() {
        String key = MD5Util.md5(IMG_IP);
        try {
            DiskLruCache.Snapshot snapShot = mDiskLruCache.get(key);
            if (snapShot != null) {
                InputStream is = snapShot.getInputStream(0);
                Bitmap bitmap = BitmapFactory.decodeStream(is);
                im.setImageBitmap(bitmap);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void cacheImg() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String key = MD5Util.md5(IMG_IP);
                try {
                    DiskLruCache.Editor editor = mDiskLruCache.edit(key);
                    if (editor != null) {
                        OutputStream out = editor.newOutputStream(0);
                        if (downloadImg(IMG_IP, out)) {//加载网络图片
                            //提交
                            editor.commit();
                        } else {
                            //撤销操作
                            editor.abort();
                        }
                    }
                    /**
                     * 这个方法用于将内存中的操作记录同步到日志文件（也就是journal文件）当中。
                     * 这个方法非常重要，因为DiskLruCache能够正常工作的前提就是要依赖于journal文件中的内容。
                     * 并不是每次写入缓存都要调用一次flush()方法的，频繁地调用并不会带来任何好处，
                     * 只会额外增加同步journal文件的时间。
                     * 比较标准的做法就是在Activity的onPause()方法中去调用一次flush()方法就可以了
                     */
                    mDiskLruCache.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            mDiskLruCache.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean downloadImg(final String urlStr, final OutputStream outputStream) {
        HttpURLConnection conn = null;
        BufferedOutputStream out = null;
        BufferedInputStream in = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            in = new BufferedInputStream(conn.getInputStream(), 8 * 1024);
            out = new BufferedOutputStream(outputStream, 8 * 1024);
            int len = 0;
            while ((len = in.read()) != -1) {
                out.write(len);
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (conn != null)
                conn.disconnect();
            try {
                if (out != null)
                    out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                if (in != null)
                    in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    private File getDiskCacheDir(Context context, String uniqueName) {
        String cachePath;
        //如果sd卡存在并且没有被移除
        if (Environment.MEDIA_MOUNTED.equals(Environment
                .getExternalStorageState())
                || !Environment.isExternalStorageRemovable()) {
            cachePath = context.getExternalCacheDir().getPath();
        } else {
            cachePath = context.getCacheDir().getPath();
        }
        return new File(cachePath + File.separator + uniqueName);
    }

    private int getAppVersion(Context context) {
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(
                    context.getPackageName(), 0);
            return info.versionCode;
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
        return 1;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        /**
         * 这个方法用于将DiskLruCache关闭掉，是和open()方法对应的一个方法。
         * 关闭掉了之后就不能再调用DiskLruCache中任何操作缓存数据的方法，
         * 通常只应该在Activity的onDestroy()方法中去调用close()方法。
         */
        try {
            mDiskLruCache.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
