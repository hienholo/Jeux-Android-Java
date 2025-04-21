package com.iot.jeux_mobile.screen.multi;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import com.iot.jeux_mobile.R;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RechercheClientServer extends AppCompatActivity {
    private static final UUID APP_UUID = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");
    private static final int DISCOVERABLE_DURATION = 300;
    private static final int REQUEST_ENABLE_BT = 1;

    private BluetoothAdapter bluetoothAdapter;
    private final List<BluetoothDevice> discoveredDevices = new ArrayList<>();
    private ArrayAdapter<String> deviceAdapter;
    private BluetoothDiscoveryReceiver discoveryReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lobby);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            showErrorAndFinish("Bluetooth non supporté");
            return;
        }

        setupUI();
        checkPermissions();
    }

    private void setupUI() {
        ListView listView = findViewById(R.id.device_list);
        deviceAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        listView.setAdapter(deviceAdapter);

        Button discoverButton = findViewById(R.id.discover_button);
        Button hostButton = findViewById(R.id.host_button);

        discoverButton.setOnClickListener(v -> startDiscovery());
        hostButton.setOnClickListener(v -> makeDeviceDiscoverable());
        listView.setOnItemClickListener((parent, view, position, id) ->
                connectToDevice(discoveredDevices.get(position)));
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{
                                Manifest.permission.BLUETOOTH_CONNECT,
                                Manifest.permission.BLUETOOTH_SCAN,
                                Manifest.permission.ACCESS_FINE_LOCATION
                        }, REQUEST_ENABLE_BT);
            }
        } else {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_ENABLE_BT);
            }
        }
    }

    private void startDiscovery() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
        discoveredDevices.clear();
        deviceAdapter.clear();

        discoveryReceiver = new BluetoothDiscoveryReceiver(device -> {
            runOnUiThread(() -> {
                String deviceName = device.getName() != null ? device.getName() : "Inconnu";
                String deviceInfo = deviceName + "\n" + device.getAddress();
                if (!discoveredDevices.contains(device)) {
                    discoveredDevices.add(device);
                    deviceAdapter.add(deviceInfo);
                }
            });
        });
        registerReceiver(discoveryReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
        bluetoothAdapter.startDiscovery();
    }

    private void makeDeviceDiscoverable() {
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, DISCOVERABLE_DURATION);
        startActivity(discoverableIntent);
        hostGame();
    }

    private void connectToDevice(BluetoothDevice device) {
        new Thread(() -> {
            try {
                BluetoothSocket socket = device.createRfcommSocketToServiceRecord(APP_UUID);
                socket.connect();
                BluetoothConnectionManager.getInstance().init(socket);

                runOnUiThread(() -> {
                    Intent intent = new Intent(this, PodsStartMulti.class);
                    intent.putExtra("role", "client");
                    startActivity(intent);
                });

            } catch (IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(this, "Échec de la connexion", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void hostGame() {
        new Thread(() -> {
            try {
                BluetoothSocket socket = bluetoothAdapter.listenUsingRfcommWithServiceRecord(
                        "PodsGame", APP_UUID).accept();
                BluetoothConnectionManager.getInstance().init(socket);

                BluetoothConnectionManager.getInstance().listen(message -> {
                    if (message.startsWith("DURATION|")) {
                        int duration = Integer.parseInt(message.substring(9));
                        runOnUiThread(() -> {
                            Intent intent = new Intent(this, PodsActivity.class);
                            intent.putExtra("role", "host"); // Rôle forcé
                            intent.putExtra("GAME_DURATION", duration);
                            startActivity(intent);
                        });
                    }
                });
            } catch (IOException e) {
                Log.e("HostError", "Erreur d'hébergement", e);
            }
        }).start();
    }
    private void showErrorAndFinish(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (discoveryReceiver != null) {
                unregisterReceiver(discoveryReceiver);
            }
        } catch (Exception e) {
            Log.w("LobbyActivity", "Récepteur non enregistré", e);
        }
        BluetoothConnectionManager.getInstance().close();
    }
}