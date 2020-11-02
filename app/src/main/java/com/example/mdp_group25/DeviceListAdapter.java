package com.example.mdp_group25;

import android.content.Context;
import java.util.ArrayList;
import android.bluetooth.BluetoothDevice;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;

public class DeviceListAdapter extends ArrayAdapter<BluetoothDevice> {
    private LayoutInflater mLayoutInflater;
    private ArrayList<BluetoothDevice> mBTDevices;
    private int mViewResourceId;
    TextView deviceName, deviceAdress;

    public DeviceListAdapter(Context ctx, int resourceId, ArrayList<BluetoothDevice> btDevices) {
        super(ctx, resourceId, btDevices);
        this.mBTDevices = btDevices;
        mLayoutInflater = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mViewResourceId = resourceId;
    }

    public View getView(int pos, View ctView, ViewGroup parent) {
        ctView = mLayoutInflater.inflate(mViewResourceId, null);
        BluetoothDevice btDevice = mBTDevices.get(pos);
        if (btDevice != null) {
            deviceName = (TextView) ctView.findViewById(R.id.tvDeviceName);
            deviceAdress = (TextView) ctView.findViewById(R.id.tvDeviceAddress);
            if (deviceName != null) {
                deviceName.setText(btDevice.getName());
            }
            if (deviceAdress != null) {
                deviceAdress.setText(btDevice.getAddress());
            }
        }
        return ctView;
    }

}
