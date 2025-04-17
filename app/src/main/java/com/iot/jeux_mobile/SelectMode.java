package com.iot.jeux_mobile;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.iot.jeux_mobile.capteur.JeuxCapMultiplayer;

import java.util.ArrayList;
import java.util.Set;

public class SelectMode extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 2;

    private BluetoothAdapter bluetoothAdapter;
    private ArrayAdapter<String> devicesArrayAdapter;
    private ArrayList<BluetoothDevice> devicesList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.select_mode);

        // Initialisation de l'interface
        Button hostButton = findViewById(R.id.host_button);
        Button scanButton = findViewById(R.id.scan_button);
        ListView devicesListView = findViewById(R.id.devices_list);

        // Adapter pour la liste des appareils
        devicesArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        devicesListView.setAdapter(devicesArrayAdapter);
        devicesListView.setOnItemClickListener(deviceClickListener);

        // Vérification et activation du Bluetooth
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth non supporté sur cet appareil", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Configuration des boutons
        hostButton.setOnClickListener(v -> {
            if (checkBluetoothPermissions()) {
                startGameAsHost();
            }
        });

        scanButton.setOnClickListener(v -> {
            if (checkBluetoothPermissions()) {
                discoverDevices();
            }
        });

        // Vérifier si Bluetooth est activé
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
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
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    private boolean checkBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{
                                Manifest.permission.BLUETOOTH_CONNECT,
                                Manifest.permission.BLUETOOTH_SCAN
                        },
                        REQUEST_BLUETOOTH_PERMISSIONS);
                return false;
            }
        }
        return true;
    }

    private final AdapterView.OnItemClickListener deviceClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if (position < devicesList.size()) {
                BluetoothDevice device = devicesList.get(position);
                startGameAsClient(device);
            }
        }
    };

    private void startGameAsHost() {
        Intent intent = new Intent(this, JeuxCapMultiplayer.class);
        intent.putExtra("isHost", true);
        startActivity(intent);
    }

    private void startGameAsClient(BluetoothDevice device) {
        Intent intent = new Intent(this,JeuxCapMultiplayer.class);
        intent.putExtra("isHost", false);
        intent.putExtra("device", device);
        startActivity(intent);
    }

    private void discoverDevices() {
        if (bluetoothAdapter == null) return;

        // Vider les listes précédentes
        devicesArrayAdapter.clear();
        devicesList.clear();

        // Obtenir les appareils déjà appairés
        if (checkBluetoothPermissions()) {
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
            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
            if (pairedDevices.size() > 0) {
                for (BluetoothDevice device : pairedDevices) {
                    devicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                    devicesList.add(device);
                }
            } else {
                devicesArrayAdapter.add("Aucun appareil appairé trouvé");
            }
        }

        // Démarrer la découverte (nécessite des permissions supplémentaires)
        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
        bluetoothAdapter.startDiscovery();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode != RESULT_OK) {
                Toast.makeText(this, "Le Bluetooth doit être activé pour le mode multijoueur", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_BLUETOOTH_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permissions accordées
                Toast.makeText(this, "Permissions Bluetooth accordées", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Les permissions Bluetooth sont nécessaires", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bluetoothAdapter != null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            bluetoothAdapter.cancelDiscovery();
        }
    }
}