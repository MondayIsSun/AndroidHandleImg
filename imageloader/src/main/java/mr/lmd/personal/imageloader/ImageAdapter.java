package mr.lmd.personal.imageloader;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * GridView adapter
 * Created by LinMingDao on 2015/11/25.
 */
public class ImageAdapter extends BaseAdapter {

    private static Set<String> mSelectImg = new HashSet<>();

    private String mDirPath;
    private List<String> mImgPaths;
    private LayoutInflater mInflater;

    ImageAdapter(Context context, List<String> mData, String dirPath) {
        this.mDirPath = dirPath;
        this.mImgPaths = mData;
        this.mInflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return mImgPaths.size();
    }

    @Override
    public Object getItem(int position) {
        return mImgPaths.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final ViewHolder viewHolder;
        if (null == convertView) {
            convertView = mInflater.inflate(R.layout.item_gridview, parent, false);

            viewHolder = new ViewHolder();
            viewHolder.mImg = (ImageView) convertView.findViewById(R.id.id_item_image);
            viewHolder.mSelect = (ImageButton) convertView.findViewById(R.id.id_item_select);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        //重置状态
        viewHolder.mImg.setImageResource(R.drawable.pictures_no);
        viewHolder.mSelect.setImageResource(R.drawable.picture_unselected);
        viewHolder.mImg.setColorFilter(null);

        ImageLoader.getInstance(3, ImageLoader.Type.LIFO).loadImage(mDirPath + "/" + mImgPaths.get(position), viewHolder.mImg,false);
        final String filePath = mDirPath + "/" + mImgPaths.get(position);
        viewHolder.mImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (mSelectImg.contains(filePath)) {
                    //已经被选择
                    mSelectImg.remove(filePath);
                    //notifyDataSetChanged();//该方式会闪屏
                    viewHolder.mImg.setColorFilter(null);
                    viewHolder.mSelect.setImageResource(R.drawable.picture_unselected);
                } else {
                    //未被选择
                    mSelectImg.add(filePath);
                    //notifyDataSetChanged();//该方式会闪屏
                    viewHolder.mImg.setColorFilter(Color.parseColor("#77000000"));
                    viewHolder.mSelect.setImageResource(R.drawable.picture_selected);
                }
            }
        });

        if (mSelectImg.contains(filePath)) {
            viewHolder.mImg.setColorFilter(Color.parseColor("#77000000"));
            viewHolder.mSelect.setImageResource(R.drawable.picture_selected);
        }

        return convertView;
    }

    private class ViewHolder {
        ImageView mImg;
        ImageButton mSelect;
    }
}
