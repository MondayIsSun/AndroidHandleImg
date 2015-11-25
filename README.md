# AndroidHandleImg
tell you how to deal with the follow instances:
1、deal with loading thousands of img;
2、deal with loading HD img.

## 数量巨大的图片加载策略

加载数量巨大的图片更多的是涉及到如何避免OOM的同时还加载到流畅

核心技术点：

内存缓存策略——>LruCache

异步加载图片策略——>线程池（尽量避免使用线程异步加载）,Android消息框架，队列

涉及到多线程操作需要注意多线程的锁机制以及同步机制（锁使用synchronized关键字处理，同步使用Java提供的信号量机制（Semaphore）处理，而非原始的wait()、notify()处理同步）,还需要注意Java的内存可见性

图片压缩策略——>Options

反射机制处理API兼容性



## 处理高清巨图策略

http://android.jobbole.com/81938/
