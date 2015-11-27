package mr.lmd.personal.imageloader;

import android.support.v4.app.Fragment;

/**
 * Created by Administrator on 2015/11/27.
 */
public class NetMainActivity extends AbsSingleFragmentActivity {

    @Override
    protected Fragment createFragment() {
        return new ListImgsFragment();
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_single_fragment;
    }
}
