package com.iot.jeux_mobile.multi;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class BluetoothDiscoveryReceiver extends BroadcastReceiver {
    private final DeviceFoundListener listener;

    public interface DeviceFoundListener {
        void onDeviceFound(BluetoothDevice device);
    }

    public BluetoothDiscoveryReceiver(DeviceFoundListener listener) {
        this.listener = listener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (BluetoothDevice.ACTION_FOUND.equals(action)) {
            BluetoothDevice device;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice.class);
            } else {
                device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            }
            if (device != null && listener != null) {
                listener.onDeviceFound(device);
            }
        }
    }
}