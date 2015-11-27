package mr.lmd.personal.loadhdimage.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.io.IOException;
import java.io.InputStream;

/**
 * 自定义局部显示HDImg的控件
 * Created by LinMingDao on 15/5/16.
 */
public class LargeImageView extends View {

    private BitmapRegionDecoder mRegionDecoder;
    private int mImageWidth, mImageHeight;//图片的宽度和高度
    private volatile Rect mRect = new Rect();//绘制的区域

    private MoveGestureDetector mGestureDetector;

    private static final BitmapFactory.Options options = new BitmapFactory.Options();

    static {
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        /*
        android中的大图片一般都要经过压缩才显示，不然容易发生oom，一般我们压缩的时候都只关注其尺寸方面的大小，
        其实除了尺寸之外，影响一个图片占用空间的还有其色彩细节。
        打开Android.graphics.Bitmap类里有一个内部类Bitmap.Config类，
        在Bitmap类里createBitmap(int width, int height, Bitmap.Config config)方法里会用到，打开个这个类一看
        枚举变量
        public static final Bitmap.Config ALPHA_8
        public static final Bitmap.Config ARGB_4444
        public static final Bitmap.Config ARGB_8888
        public static final Bitmap.Config RGB_565
        一看，有点蒙了，ALPHA_8, ARGB_4444,ARGB_8888,RGB_565 到底是什么呢？
        其实这都是色彩的存储方法：我们知道ARGB指的是一种色彩模式，里面A代表Alpha，R表示red，G表示green，B表示blue，其实所有的可见色都是右红绿蓝组成的，
        所以红绿蓝又称为三原色，每个原色都存储着所表示颜色的信息值
        说白了就ALPHA_8就是Alpha由8位组成
        ARGB_4444就是由4个4位组成即16位，
        ARGB_8888就是由4个8位组成即32位，
        RGB_565就是R为5位，G为6位，B为5位共16位
        由此可见：
        ALPHA_8 代表8位Alpha位图
        ARGB_4444 代表16位ARGB位图
        ARGB_8888 代表32位ARGB位图
        RGB_565 代表8位RGB位图
        位图位数越高代表其可以存储的颜色信息越多，当然图像也就越逼真。
        用法：
        在压缩之前将option的值设置一下：
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        */
    }

    public void setInputStream(InputStream is) {
        try {
            mRegionDecoder = BitmapRegionDecoder.newInstance(is, false);
            BitmapFactory.Options tmpOptions = new BitmapFactory.Options();
            tmpOptions.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(is, null, tmpOptions);
            mImageWidth = tmpOptions.outWidth;
            mImageHeight = tmpOptions.outHeight;

            Log.d("ELSeed", "mImageWidth = " + mImageWidth + " mImageHeight = " + mImageHeight);

            requestLayout();//View.requestLayout() 请求重新布局,重新调用：onMeasure，onLayout，onDraw,View.invalidate()刷新视图，相当于调用View.onDraw()方法
            invalidate();//必须在主线程中调用,而postInvalidate();非主线程中调用
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (is != null) is.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void init() {
        mGestureDetector = new MoveGestureDetector(getContext(), new MoveGestureDetector.SimpleMoveGestureDetector() {
            @Override
            public boolean onMove(MoveGestureDetector detector) {
                int moveX = (int) detector.getMoveX();
                int moveY = (int) detector.getMoveY();
                Log.d("ELSeed", "mImageWidth = " + mImageWidth + " getWidth = " + getWidth() + " getHeight = " + getHeight());
                if (mImageWidth > getWidth()) {
                    mRect.offset(-moveX, 0);
                    checkWidth();
                    invalidate();
                }
                if (mImageHeight > getHeight()) {
                    mRect.offset(0, -moveY);
                    checkHeight();
                    invalidate();
                }
                return true;
            }
        });
    }

    /**
     * 边界判定
     */
    private void checkWidth() {
        Rect rect = mRect;
        int imageWidth = mImageWidth;
        int imageHeight = mImageHeight;
        if (rect.right > imageWidth) {
            rect.right = imageWidth;
            rect.left = imageWidth - getWidth();
        }
        if (rect.left < 0) {
            rect.left = 0;
            rect.right = getWidth();
        }
    }

    private void checkHeight() {
        Rect rect = mRect;
        int imageWidth = mImageWidth;
        int imageHeight = mImageHeight;
        if (rect.bottom > imageHeight) {
            rect.bottom = imageHeight;
            rect.top = imageHeight - getHeight();
        }
        if (rect.top < 0) {
            rect.top = 0;
            rect.bottom = getHeight();
        }
    }

    public LargeImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mGestureDetector.onTouchEvent(event);
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Bitmap bm = mRegionDecoder.decodeRegion(mRect, options);
        canvas.drawBitmap(bm, 0, 0, null);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int width = getMeasuredWidth();
        int height = getMeasuredHeight();

        int imageWidth = mImageWidth;
        int imageHeight = mImageHeight;

        mRect.left = imageWidth / 2 - width / 2;
        mRect.top = imageHeight / 2 - height / 2;
        mRect.right = mRect.left + width;
        mRect.bottom = mRect.top + height;
    }
}
