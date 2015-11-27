package mr.lmd.personal.imageloader;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.ImageView;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class DownloadImgUtils {

	/**
	 * 根据url下载图片到指定的文件
	 *
	 * @param urlStr 远程图片URL
	 * @param file 本地文件夹
	 * @return 是否操作成功
	 */
	public static boolean downloadImgByUrl(String urlStr, File file) {
		FileOutputStream fos = null;
		InputStream is = null;
		try {
			URL url = new URL(urlStr);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();

			is = conn.getInputStream();
			fos = new FileOutputStream(file);
			byte[] buf = new byte[512];
			int len;
			while ((len = is.read(buf)) != -1) {
				fos.write(buf, 0, len);
			}
			fos.flush();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (is != null)
					is.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				if (fos != null)
					fos.close();
			} catch (IOException e) {
			}
		}
		return false;
	}

	/**
	 * 根据url下载图片并给指定的控件显示
	 * @param urlStr 图片URL地址
	 * @param imageView 显示图片的控件
	 * @return 返回网络下载的图片
	 */
	public static Bitmap downloadImgByUrl(String urlStr, ImageView imageView) {
		FileOutputStream fos = null;
		InputStream is = null;
		try {
			URL url = new URL(urlStr);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			is = new BufferedInputStream(conn.getInputStream());
			is.mark(is.available());

			BitmapFactory.Options opts = new BitmapFactory.Options();
			opts.inJustDecodeBounds = true;
			//Bitmap bitmap = BitmapFactory.decodeStream(is, null, opts);

			//获取imageView想要显示的宽和高
			BitmapCompressUtils.ViewSize imageViewSize = BitmapCompressUtils.getInstance().calculateImageViewSize(imageView);
			opts.inSampleSize = BitmapCompressUtils.getInstance().calculateInSampleSize(opts, imageViewSize.width, imageViewSize.height);

			opts.inJustDecodeBounds = false;
			is.reset();
			Bitmap bitmap = BitmapFactory.decodeStream(is, null, opts);

			conn.disconnect();
			return bitmap;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (is != null)
					is.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				if (fos != null)
					fos.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return null;
	}
}
