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
	 * ����url����ͼƬ��ָ�����ļ�
	 *
	 * @param urlStr Զ��ͼƬURL
	 * @param file �����ļ���
	 * @return �Ƿ�����ɹ�
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
	 * ����url����ͼƬ����ָ���Ŀؼ���ʾ
	 * @param urlStr ͼƬURL��ַ
	 * @param imageView ��ʾͼƬ�Ŀؼ�
	 * @return �����������ص�ͼƬ
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

			//��ȡimageView��Ҫ��ʾ�Ŀ�͸�
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
