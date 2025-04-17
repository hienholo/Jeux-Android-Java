// Ce fichier est le début du jeu en mode multijoueur Bluetooth
// Nous allons le structurer en deux parties :
// 1. Activité Bluetooth principale (choix d'un appareil, connexion, etc.)
// 2. JeuxCap1 adapté pour envoyer/recevoir les positions

// Partie 1: Activité Bluetooth Multijoueur
// BluetoothGameActivity.java
package com.iot.jeux_mobile.capteur;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class BluetoothGameActivity extends AppCompatActivity {

    private static final String APP_NAME = "JeuxBluetooth";
    private static final UUID MY_UUID = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket socket;
    private InputStream inputStream;
    private OutputStream outputStream;
    private boolean isHost = false;
    private Thread listenThread;
    private JeuxCap1 gameActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Intent intent = getIntent();
        isHost = intent.getBooleanExtra("isHost", false);

        if (isHost) {
            startServer();
        } else {
            String address = intent.getStringExtra("deviceAddress");
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
            connectToDevice(device);
        }
    }

    private void startServer() {
        new Thread(() -> {
            try {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                BluetoothServerSocket serverSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord(APP_NAME, MY_UUID);
                socket = serverSocket.accept();
                serverSocket.close();
                manageConnection(socket);
            } catch (IOException e) {
                e.printStackTrace();
                showToast("Erreur serveur Bluetooth");
            }
        }).start();
    }

    private void connectToDevice(BluetoothDevice device) {
        new Thread(() -> {
            try {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                socket = device.createRfcommSocketToServiceRecord(MY_UUID);
                bluetoothAdapter.cancelDiscovery();
                socket.connect();
                manageConnection(socket);
            } catch (IOException e) {
                e.printStackTrace();
                showToast("Erreur de connexion Bluetooth");
            }
        }).start();
    }

    private void manageConnection(BluetoothSocket socket) {
        try {
            inputStream = socket.getInputStream();
            outputStream = socket.getOutputStream();
            showToast("Connecté avec succès !");
            startListening();
            // Ici on peut lancer le jeu après connexion
            runOnUiThread(() -> {
                Intent i = new Intent(this, JeuxCap1.class);
                i.putExtra("isMultiplayer", true);
                startActivity(i);
                finish();
            });
        } catch (IOException e) {
            e.printStackTrace();
            showToast("Erreur de gestion de la connexion");
        }
    }

    private void startListening() {
        listenThread = new Thread(() -> {
            byte[] buffer = new byte[1024];
            int bytes;
            while (true) {
                try {
                    bytes = inputStream.read(buffer);
                    String received = new String(buffer, 0, bytes);
                    Log.d("RECU", received);
                    // Ici, transmettre à l'activité de jeu pour mise à jour
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            }
        });
        listenThread.start();
    }

    public void sendPosition(String data) {
        if (outputStream != null) {
            try {
                outputStream.write(data.getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void showToast(String msg) {
        new Handler(Looper.getMainLooper()).post(() ->
                Toast.makeText(BluetoothGameActivity.this, msg, Toast.LENGTH_SHORT).show()
        );
    }
}
