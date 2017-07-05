package com.jiteng.intelligentgatetest.ui.adapter;

import android.bluetooth.BluetoothDevice;
import android.content.Context;

import com.jiteng.intelligentgatetest.R;
import com.zhy.adapter.recyclerview.CommonAdapter;
import com.zhy.adapter.recyclerview.base.ViewHolder;

import java.util.List;

/**
 * Created by zwj on 2017/3/26.
 */

public class DeviceAdapter extends CommonAdapter<BluetoothDevice> {

    public DeviceAdapter(Context context, List<BluetoothDevice> datas) {
        super(context, R.layout.item_devcie, datas);
    }

    @Override
    protected void convert(ViewHolder viewHolder, BluetoothDevice device, int postion) {
        viewHolder.setText(R.id.tv_name, device.getName());
        viewHolder.setText(R.id.tv_address, device.getAddress());

//        FrameLayout flItem = (FrameLayout) viewHolder.getConvertView();
//        RecyclerView.LayoutParams lps = (RecyclerView.LayoutParams) flItem.getLayoutParams();
//        if (postion == mDatas.size() - 1) {
//            lps.bottomMargin = AutoUtils.getPercentHeightSize(44);
//        } else {
//            lps.bottomMargin = 0;
//        }

    }
}
