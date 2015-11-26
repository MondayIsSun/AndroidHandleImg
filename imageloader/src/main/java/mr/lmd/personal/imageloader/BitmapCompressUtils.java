package mr.lmd.personal.imageloader;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.lang.reflect.Field;

/**
 * Bitmap图片压缩工具类
 * Created by LinMingDao on 2015/11/26.
 */
public class BitmapCompressUtils {

    private static BitmapCompressUtils mInstance;

    public static BitmapCompressUtils getInstance() {
        if (null == mInstance) {
            synchronized (BitmapCompressUtils.class) {
                if (null == mInstance) {
                    mInstance = new BitmapCompressUtils();
                }
            }
        }
        return mInstance;
    }

    private BitmapCompressUtils() {

    }

    public Bitmap doCompress(View destView, String bmpPath) {
        ViewSize viewSize = calculateImageViewSize(destView);//1、计算控件的尺寸
        Bitmap compressedBmp = decodeSampledBitmapFromPath(bmpPath, viewSize.width, viewSize.height);//2、根据控件大小压缩图片
        return compressedBmp;
    }

    private class ViewSize {
        int width;
        int height;
    }

    /**
     * 根据控件(ImageView)的大小获取适当的压缩宽和高
     *
     * @param imageView 要显示图片的控件
     * @return 图片压缩的宽和高
     */
    private ViewSize calculateImageViewSize(View imageView) {
        DisplayMetrics displayMetrics = imageView.getContext().getResources().getDisplayMetrics();
        ViewSize viewSize = new ViewSize();

        ViewGroup.LayoutParams lp = imageView.getLayoutParams();
        int width = imageView.getWidth();//获取imageView的实际宽度
        //(lp.width == ViewGroup.LayoutParams.WRAP_CONTENT) ? 0 : imageView.getWidth();

        //无论如何都要进行压缩
        //width的值会有很多情况下是小于0的，比如这个view还没被加入到父控件中，或者没有设定固定值（设置了wrap_content or fill_parent）
        if (width <= 0) {
            width = lp.width;//获取imageView在Layout中声明的宽度
        }
        if (width <= 0) {//wrap_content（-1） or fill_parent（-2）
            //width = imageView.getMaxWidth();//检查最大值，@SuppressLint("NewApi")，学会如何处理API兼容问题——>反射机制
            width = getImageViewFieldValue(imageView, "mMaxWidth");
        }
        if (width <= 0) {//最坏的情况：屏幕的宽度了
            width = displayMetrics.widthPixels;
        }

        //同样处理高度
        int height = imageView.getHeight();
        if (height <= 0) {
            height = lp.height;
        }
        if (height <= 0) {
            //height = imageView.getMaxHeight();
            height = getImageViewFieldValue(imageView, "mMaxHeight");
        }
        if (height <= 0) {
            height = displayMetrics.heightPixels;
        }

        viewSize.width = width;
        viewSize.height = height;

        return viewSize;
    }

    /**
     * 通过反射机制处理兼容性问题
     * 通过反射机制获取ImageView的某个属性值
     *
     * @param object
     * @param fieldName
     * @return
     */
    private static int getImageViewFieldValue(Object object, String fieldName) {
        int value = 0;
        try {
            Field field = ImageView.class.getField(fieldName);
            field.setAccessible(true);
            int fieldValue = field.getInt(object);
            if (fieldValue > 0 && fieldValue < Integer.MAX_VALUE) {
                value = fieldValue;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return value;
    }

    /**
     * 根据需求的宽和高以及图片实际的宽和高计算SampleSize
     *
     * @param options   options参数
     * @param reqWidth  需求的宽度
     * @param reqHeight 需求的高度
     * @return 计算出的Sample值
     */
    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int width = options.outWidth;
        int height = options.outHeight;
        int sampleSize = 1;
        if (width > reqWidth || height > reqHeight) {
            int widthRadio = Math.round(width * 1.0f / reqWidth);
            int heightRadio = Math.round(height * 1.0f / reqHeight);
            //不失真min，节省内存max
            sampleSize = Math.max(widthRadio, heightRadio);//300*500 ——> 100*100,300*500 ——> 30*50
        }
        return sampleSize;
    }

    /**
     * 根据图片需要显示的宽个高对图片进行压缩
     *
     * @param path
     * @param width
     * @param height
     * @return
     */
    private Bitmap decodeSampledBitmapFromPath(String path, int width, int height) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;//获取图片实际的宽和高，但是并不把图片加载到内存中
        BitmapFactory.decodeFile(path, options);
        options.inSampleSize = calculateInSampleSize(options, width, height);
        //使用获取到的inSampleSize再次解析图片
        options.inJustDecodeBounds = false;
        Bitmap bitmap = BitmapFactory.decodeFile(path, options);
        return bitmap;
    }
}
