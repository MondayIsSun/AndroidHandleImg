package mr.lmd.personal.imageloader;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.GridView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import mr.lmd.personal.imageloader.bean.FolderBean;

public class MainActivity extends AppCompatActivity {

    private GridView mGridView;
    private List<String> mImgs;//存储当前文件夹下图片的名字
    private ImageAdapter mAdapter;

    private RelativeLayout mBottomLy;
    private TextView mDirName;
    private TextView mDirCount;

    private File mCurrentDir;
    private int mMaxCount;

    private List<FolderBean> mFolderBeanList = new ArrayList<>();

    private ProgressDialog mProgressDialog;

    private final int DATA_LOADED = 0x110;

    private ListImgDirPopupWindow mDirPopupWindow;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == DATA_LOADED) {
                mProgressDialog.dismiss();

                //绑定数据到GridView中
                data2View();

                initDirPopupWindow();
            }
        }
    };

    private void initDirPopupWindow() {
        mDirPopupWindow = new ListImgDirPopupWindow(this, mFolderBeanList);
        mDirPopupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                //将内容区域变亮
                lightOn();
            }
        });
        mDirPopupWindow.setOnDriSelectedListener(new ListImgDirPopupWindow.OnDriSelectedListener() {
            @Override
            public void onSelected(FolderBean folderBean) {
                mCurrentDir = new File(folderBean.getDir());
                mImgs = Arrays.asList(mCurrentDir.list(new FilenameFilter() {//过滤不是图片的文件
                    @Override
                    public boolean accept(File dir, String filename) {
                        return filename.endsWith(".jpg") || filename.endsWith(".jpeg") || filename.endsWith(".png");
                    }
                }));

                //当然notifyChange();
                mAdapter = new ImageAdapter(MainActivity.this, mImgs, mCurrentDir.getAbsolutePath());
                mGridView.setAdapter(mAdapter);

                mDirCount.setText(mImgs.size() + "");
                mDirName.setText(folderBean.getName());

                mDirPopupWindow.dismiss();
            }
        });
    }

    private void data2View() {
        if (null == mCurrentDir) {
            Toast.makeText(this, "未扫描到任何图片", Toast.LENGTH_SHORT).show();
            return;
        }
        mImgs = Arrays.asList(mCurrentDir.list());

        //构造适配器
        mAdapter = new ImageAdapter(this, mImgs, mCurrentDir.getAbsolutePath());
        mGridView.setAdapter(mAdapter);

        mDirCount.setText(mMaxCount + "");
        mDirName.setText(mCurrentDir.getName() + "");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initData();
        initEvent();
    }

    private void initView() {
        mGridView = (GridView) findViewById(R.id.id_gridView);
        mBottomLy = (RelativeLayout) findViewById(R.id.id_bottom_ly);
        mDirName = (TextView) findViewById(R.id.id_dir_name);
        mDirCount = (TextView) findViewById(R.id.id_dir_count);
    }

    /**
     * 使用ContentProvider扫描手机中所有的图片
     */
    private void initData() {
        //判断外部有无存储卡
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            Toast.makeText(this, "当前存储卡不能用", Toast.LENGTH_SHORT).show();
            return;
        }

        //显示进度对话框
        mProgressDialog = ProgressDialog.show(this, null, "正在加载...");

        //开启一个线程扫描手机存储卡中的所有图片
        new Thread() {
            @Override
            public void run() {
                Uri mImgUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                ContentResolver cr = MainActivity.this.getContentResolver();
                Cursor cursor = cr.query(
                        mImgUri,
                        null,
                        MediaStore.Images.Media.MIME_TYPE + "=? or " + MediaStore.Images.Media.MIME_TYPE + "=?",
                        new String[]{"image/jpeg", "image/png"},
                        MediaStore.Images.Media.DATE_MODIFIED);

                if (null != cursor) {
                    //防止重复遍历
                    Set<String> mDirPaths = new HashSet<>();
                    //遍历游标
                    while (cursor.moveToNext()) {
                        String path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
                        File parentFile = new File(path).getParentFile();
                        if (null == parentFile) {//有些图片并没有所在的文件夹
                            continue;
                        }

                        String dirPath = parentFile.getAbsolutePath();

                        FolderBean folderBean;
                        if (mDirPaths.contains(dirPath)) {//该文件夹遍历过了
                            continue;
                        } else {//该文件夹没有遍历过
                            mDirPaths.add(dirPath);
                            folderBean = new FolderBean();
                            folderBean.setDir(dirPath);
                            folderBean.setFirstImgPath(path);
                        }

                        if (parentFile.list() == null) {
                            continue;
                        }

                        int picSize = parentFile.list(
                                new FilenameFilter() {//过滤不是图片的文件
                                    @Override
                                    public boolean accept(File dir, String filename) {
                                        if (filename.endsWith(".jpg") || filename.endsWith(".jpeg") || filename.endsWith(".png"))
                                            return true;
                                        return false;
                                    }
                                }
                        ).length;

                        folderBean.setCount(picSize);

                        mFolderBeanList.add(folderBean);

                        if (picSize > mMaxCount) {
                            mMaxCount = picSize;
                            mCurrentDir = parentFile;
                        }
                    }
                    cursor.close();

                    //扫描完成，释放临时变量的内存——>这个地方不用这样操作，因为这个是在方法内部的变量，方法执行完后会自动回收
                    //mDirPaths = null;

                    //通知Handler扫描图片完成
                    mHandler.sendEmptyMessage(DATA_LOADED);
                }
            }
        }.start();
    }

    private void initEvent() {
        mBottomLy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //设置popupWindow出现的动画
                mDirPopupWindow.setAnimationStyle(R.style.dir_popupWindow_anim);
                mDirPopupWindow.showAsDropDown(mBottomLy, 0, 0);
                lightOff();
            }
        });
    }

    /**
     * 内容区域边亮
     */
    private void lightOn() {
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.alpha = 1.0f;//其实默认就是1.0f
        getWindow().setAttributes(lp);
    }

    /**
     * 内容区域变暗
     */
    private void lightOff() {
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.alpha = 0.3f;//其实默认就是1.0f
        getWindow().setAttributes(lp);
    }

}
