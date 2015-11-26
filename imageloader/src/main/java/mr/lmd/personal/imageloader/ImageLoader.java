package mr.lmd.personal.imageloader;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.LruCache;
import android.widget.ImageView;

import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

/**
 * 还未实现：网络加载图片 以及 硬盘缓存
 * 图片加载类功能：
 * 1、单例获取实例
 * 2、线程池技术 + 任务队列 异步加载图片 ——> 多线程需要做同步处理
 * 3、LruCache内存缓存图片（本地图片的获取直接从LruCache中获取，而LruCache会去缓存没有缓存过的图片信息）
 * Created by LinMingDao on 2015/11/23.
 */
public class ImageLoader {

    private LruCache<String, Bitmap> mLruCache;//避免OOM，内存缓存Bitmap的核心类

    private Type mType = Type.LIFO;//图片加载策略，默认采用后进先出

    public enum Type {
        FIFO, LIFO
    }

    private ExecutorService mThreadPool;//采用线程池去处理远程下载任务，而不是使用线程（提高线程的使用效率）
    private static final int DEFAULT_THREAD_COUNT = 1;//线程池的线程数量

    private LinkedList<Runnable> mTaskQueue;//任务队列
    private Handler mPoolThreadHandler;
    private Thread mPoolThread;//后台轮询线程,该线程绑定mPoolThreadHandler，这个Handler对象用于发消息给后台轮询线程执行异步任务

    private Handler mUIHandler;//mUIHandler,该Handler用于通知线程更新View

    //Semaphore可以控制某个资源可被同时访问的个数
    //通过 acquire() 获取一个许可，如果没有就等待，而 release() 释放一个许可。
    private Semaphore mSemaphorePoolThreadHandler = new Semaphore(0);//Java信号量机制控制多线程同步问题
    private Semaphore mSemaphoreThreadPool;

    private static ImageLoader mImageLoader;//单例获取该类实例

    private ImageLoader(int threadCount, Type type) {
        init(threadCount, type);
    }

    public static ImageLoader getInstance() {
        if (mImageLoader == null) {
            synchronized (ImageLoader.class) {
                if (mImageLoader == null) {
                    mImageLoader = new ImageLoader(DEFAULT_THREAD_COUNT, Type.LIFO);
                }
            }
        }
        return mImageLoader;
    }

    public static ImageLoader getInstance(int threadCount, Type type) {
        if (mImageLoader == null) {
            synchronized (ImageLoader.class) {
                if (mImageLoader == null) {
                    mImageLoader = new ImageLoader(threadCount, type);
                }
            }
        }
        return mImageLoader;
    }

    private void init(int threadCount, Type type) {

        //1、初始化线程池
        mThreadPool = Executors.newFixedThreadPool(threadCount);
        mSemaphoreThreadPool = new Semaphore(threadCount);
        mTaskQueue = new LinkedList<>();
        mType = type;

        /*********************************************************************************************************/
        //2、初始化后台轮询线程，要点：
        //#、准备自己的消息队列,Looper.prepare()；
        //#、不断地轮询自己的消息队列,Looper.loop()；
        //#、外部可以给自己的消息队列发送消息,Handler
        mPoolThread = new Thread() {//当LruCache里面没有缓存Bitmap对象时候需要后台轮询线程去执行异步加载任务
            @Override
            public void run() {
                Looper.prepare();//准备该Thread的消息队列，sThreadLocal
                //注意：需要对mPoolThreadHandler对象进行同步处理
                mPoolThreadHandler = new Handler() {//这个Handler必须在此处初始化，这样才能绑定该子线程的消息队列
                    @Override
                    public void handleMessage(Message msg) {//接收到mPoolThreadHandler发送过来的空消息，线程池取出人任务去执行
                        Runnable r = getTask();
                        if (null != r) {
                            mThreadPool.execute(r);
                            try {
                                mSemaphoreThreadPool.acquire();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                };
                //释放一个信号量
                mSemaphorePoolThreadHandler.release();
                Looper.loop();//while(true),在后台不断轮询
            }
        };
        //在后台不断轮询
        mPoolThread.start();
        /*********************************************************************************************************/

        //3、初始化LruCache
        int maxMemory = (int) Runtime.getRuntime().maxMemory();
        int cacheMemory = maxMemory / 8;
        mLruCache = new LruCache<String, Bitmap>(cacheMemory) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getRowBytes() * value.getHeight();
            }
        };
    }

    private class ImageBeanHolder {
        Bitmap bitmap;
        ImageView imageView;
        String path;
    }

    /**
     * 根据path为ImageView设置图片
     *
     * @param path      图片获取到路径
     * @param imageView 要设置的控件
     */
    public void loadImage(final String path, final ImageView imageView) {

        //为控件设置一个tag
        imageView.setTag(path);

        //1、初始化mUIHandler
        if (mUIHandler == null) {
            mUIHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    //获取到图片，为ImageView回调设置图片
                    ImageBeanHolder holder = (ImageBeanHolder) msg.obj;
                    Bitmap bm = holder.bitmap;
                    ImageView imageView = holder.imageView;
                    String path = holder.path;

                    if (imageView.getTag().toString().equals(path)) {
                        imageView.setImageBitmap(bm);
                    }
                }
            };
        }

        //2、根据path在LruCache当中获取Bitmap
        Bitmap bm = getBitmapFromCache(path);

        //3、为ImageView设置图片
        if (null != bm) {
            refreshBitmap(path, imageView, bm);
        } else {
            addTask(new Runnable() {//没有缓存图片，需要异步加载图片
                @Override
                public void run() {
                    Bitmap bm = BitmapCompressUtils.getInstance().doCompress(imageView, path);//1、图片压缩
                    addBitmapToCache(path, bm);//2、把图片加入到LruCache进行缓存
                    refreshBitmap(path, imageView, bm);//3、为ImageView设置图片
                    mSemaphoreThreadPool.release();
                }
            });
        }
    }

    private void refreshBitmap(String path, ImageView imageView, Bitmap bm) {
        Message message = Message.obtain();
        ImageBeanHolder holder = new ImageBeanHolder();
        holder.bitmap = bm;
        holder.path = path;
        holder.imageView = imageView;
        message.obj = holder;
        mUIHandler.sendMessage(message);
    }

    private synchronized void addTask(Runnable runnable) {
        //把异步任务加入到任务队列中
        mTaskQueue.add(runnable);

        //信号量机制实现线程同步
        //以后要特别注意：当你的类里面有两个线程，当一个线程当中使用了另一个线程当中某一个变量的时候，对这个变量一定要做现场同步处理
        try {
            if (null == mPoolThreadHandler) {
                mSemaphorePoolThreadHandler.acquire();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //发送消息，告知任务队列有异步任务等待处理
        //if mPoolThreadHandler == null wait();
        //mPoolThreadHandler初始化完毕以后notify();
        //对线程的同步处理如果使用wait()和notify()的话比较麻烦，所以Java还提供了处理线程同步问题的类
        mPoolThreadHandler.sendEmptyMessage(0x110);
    }

    /**
     * 从任务队列取出一个任务
     *
     * @return 待执行的task
     */
    private Runnable getTask() {
        if (!mTaskQueue.isEmpty()) {
            if (mType == Type.FIFO) {
                return mTaskQueue.removeFirst();
            } else if (mType == Type.LIFO) {
                return mTaskQueue.removeLast();
            }
            return null;
        }
        return null;
    }

    /**
     * 从缓存中获取Bitmap对象
     *
     * @param path 从LruCache当中查询的key
     * @return 返回到Bitmap对象（需要判空处理）
     */

    private Bitmap getBitmapFromCache(String path) {
        return mLruCache.get(path);
    }

    /**
     * 把图片加入到LruCache缓存起来
     *
     * @param path 图片的key
     * @param bm   图片本身
     */
    private void addBitmapToCache(String path, Bitmap bm) {
        if (null == getBitmapFromCache(path)) {
            if (null != bm) {
                mLruCache.put(path, bm);
            }
        }
    }
}
