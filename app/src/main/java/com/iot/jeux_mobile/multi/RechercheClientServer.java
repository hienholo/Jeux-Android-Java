package com.iot.jeux_mobile.multi;

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
import androidx.annotation.Nullable;
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
    private static final int NAME_INPUT_REQUEST = 1001;

    private BluetoothAdapter bluetoothAdapter;
    private final List<BluetoothDevice> discoveredDevices = new ArrayList<>();
    private ArrayAdapter<String> deviceAdapter;
    private BluetoothDiscoveryReceiver discoveryReceiver;
    private String playerName;
    private boolean isHost = false;

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
        hostButton.setOnClickListener(v -> {
            isHost = true;
            makeDeviceDiscoverable();
        });
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
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) return;

        if (bluetoothAdapter.isDiscovering()) bluetoothAdapter.cancelDiscovery();

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

                runOnUiThread(() ->
                        startActivityForResult(new Intent(this, PodsStartMulti.class), NAME_INPUT_REQUEST));

            } catch (IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(this, "Échec de la connexion", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == NAME_INPUT_REQUEST && resultCode == RESULT_OK && data != null) {
            playerName = data.getStringExtra("PLAYER_NAME");
            BluetoothConnectionManager.getInstance().sendMessage("NAME|" + playerName);
            launchGameActivity(playerName, "Adversaire");
        }
    }

    private void hostGame() {
        new Thread(() -> {
            try {
                BluetoothSocket socket = bluetoothAdapter.listenUsingRfcommWithServiceRecord("PodsGame", APP_UUID).accept();
                BluetoothConnectionManager.getInstance().init(socket);

                BluetoothConnectionManager.getInstance().listen(message -> {
                    if (message.startsWith("NAME|")) {
                        String opponentName = message.substring(5);
                        runOnUiThread(() -> launchGameActivity("Hôte", opponentName));
                    }
                    else if (message.startsWith("DURATION|")) {
                        int duration = Integer.parseInt(message.substring(9));
                        runOnUiThread(() -> launchGameActivity("Hôte", "Adversaire", duration));
                    }
                });
            } catch (IOException e) {
                Log.e("HostError", "Erreur d'hébergement", e);
            }
        }).start();
    }

    private void launchGameActivity(String playerName, String opponentName) {
        Intent intent = new Intent(this, PodsActivity.class);
        intent.putExtra("PLAYER_NAME", playerName);
        intent.putExtra("OPPONENT_NAME", opponentName);
        intent.putExtra("role", isHost ? "host" : "client");
        startActivity(intent);
    }

    private void launchGameActivity(String playerName, String opponentName, int duration) {
        Intent intent = new Intent(this, PodsActivity.class);
        intent.putExtra("PLAYER_NAME", playerName);
        intent.putExtra("OPPONENT_NAME", opponentName);
        intent.putExtra("GAME_DURATION", duration);
        intent.putExtra("role", "host");
        startActivity(intent);
    }

    private void showErrorAndFinish(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (discoveryReceiver != null) unregisterReceiver(discoveryReceiver);
        } catch (Exception e) {
            Log.w("LobbyActivity", "Récepteur non enregistré", e);
        }
        BluetoothConnectionManager.getInstance().close();
    }
}