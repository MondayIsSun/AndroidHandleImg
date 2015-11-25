package mr.lmd.personal.imageloader;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

import java.util.List;

import mr.lmd.personal.imageloader.bean.FolderBean;

/**
 * BasePopupWindow
 * BaseActivity
 * Created by LinMingDao on 2015/11/25.
 */
public class ListImgDirPopupWindow extends PopupWindow {

    private int mWidth;
    private int mHeight;
    private View mConvertView;
    private ListView mListView;

    private List<FolderBean> mData;

    ListImgDirPopupWindow(Context context, List<FolderBean> data) {

        //计算宽和高
        calWidthAndHeight(context);

        mConvertView = LayoutInflater.from(context).inflate(R.layout.popup_main, null);
        mData = data;

        setContentView(mConvertView);
        setWidth(mWidth);
        setHeight(mHeight);

        setFocusable(true);
        setTouchable(true);
        setOutsideTouchable(true);

        setBackgroundDrawable(new BitmapDrawable());

        setTouchInterceptor(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_OUTSIDE) {
                    dismiss();
                    return true;
                }
                return false;
            }
        });

        initView(context);
        initEvent();

    }

    /**
     * 接口回调
     */
    public interface OnDriSelectedListener {
        void onSelected(FolderBean folderBean);
    }

    private OnDriSelectedListener mListener;

    public void setOnDriSelectedListener(OnDriSelectedListener mListener) {
        this.mListener = mListener;
    }

    private void initEvent() {
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (null != mListener) {
                    mListener.onSelected(mData.get(position));
                }
            }
        });
    }

    private void initView(Context context) {
        mListView = (ListView) mConvertView.findViewById(R.id.id_list_dir);
        mListView.setAdapter(new ListDirAdapter(context, mData));

    }

    private void calWidthAndHeight(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(outMetrics);

        mWidth = outMetrics.widthPixels;
        mHeight = (int) (outMetrics.heightPixels * 0.7);
    }

    private class ViewHolder {
        ImageView mImg;
        TextView mDirName;
        TextView mDirCount;
    }

    private class ListDirAdapter extends ArrayAdapter<FolderBean> {

        private LayoutInflater mInflater;
        private List<FolderBean> mData;

        public ListDirAdapter(Context context, List<FolderBean> objects) {
            super(context, 0, objects);
            mInflater = LayoutInflater.from(context);
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            final ViewHolder viewHolder;
            if (null == convertView) {
                convertView = mInflater.inflate(R.layout.item_popup_main, parent, false);

                viewHolder = new ViewHolder();
                viewHolder.mImg = (ImageView) convertView.findViewById(R.id.id_dir_item_image);
                viewHolder.mDirName = (TextView) convertView.findViewById(R.id.id_dir_item_name);
                viewHolder.mDirCount = (TextView) convertView.findViewById(R.id.id_dir_item_count);

                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }

            FolderBean folderBean = getItem(position);

            //重置
            viewHolder.mImg.setImageResource(R.drawable.pictures_no);

            //复用View时刷新View的数据
            ImageLoader.getInstance().loadImage(folderBean.getFirstImgPath(), viewHolder.mImg);
            viewHolder.mDirCount.setText(folderBean.getCount() + "");
            viewHolder.mDirName.setText(folderBean.getName());

            return convertView;
        }
    }
}
