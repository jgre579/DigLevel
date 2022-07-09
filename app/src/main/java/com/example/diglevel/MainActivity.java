package com.example.diglevel;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import com.welie.blessed.*;



public class MainActivity extends AppCompatActivity {

    class ViewHolder {
        ArrayAdapter adapter;
        ListView listView;
        ArrayList<String> devices;
        ArrayList<BluetoothPeripheral> btDevices;
        public ViewHolder() {
            listView = (ListView) findViewById(R.id.devices_list);
            devices = new ArrayList<>();
            btDevices = new ArrayList<>();
            devices.add("Devices : ");
            adapter = new ArrayAdapter<String>(getApplicationContext(), R.layout.activity_device_list_item, devices);
            listView.setAdapter(adapter);

            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @SuppressLint("MissingPermission")
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    //Toast.makeText(MainActivity.this, btDevices.get(position - 1).getName(), Toast.LENGTH_SHORT).show();
                    BluetoothPeripheral device;
                    if (position > btDevices.size()) {
                        return;
                    } else {
                        device = btDevices.get(position - 1);
                        Toast.makeText(MainActivity.this, device.getName(), Toast.LENGTH_SHORT).show();
                    }

                    Intent intent = new Intent(getBaseContext(), DeviceActivity.class);
                    intent.putExtra("ble_device", (Parcelable) device);
                    startActivity(intent);


                }
            });
        }
    }

        ViewHolder vh;
    private String TAG = "gatt";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        vh = new ViewHolder();
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        initBluetooth(bluetoothAdapter);
        checkPermissions();



// Create BluetoothCentral and receive callbacks on the main thread
        central = new BluetoothCentralManager(getApplicationContext(), bluetoothCentralManagerCallback, new Handler(Looper.getMainLooper()));

// Scan for peripherals with a certain service UUID
        Log.d(TAG, "Starting scan");
        String[] names = {"WT901BLE67"};
        central.scanForPeripheralsWithNames(names);

    }

    BluetoothCentralManager central;

    private final BluetoothCentralManagerCallback bluetoothCentralManagerCallback = new BluetoothCentralManagerCallback() {
        @Override
        public void onDiscoveredPeripheral(BluetoothPeripheral peripheral, ScanResult scanResult) {
            Log.d(TAG, "Peripheral Discovered " + peripheral.getName());
            central.stopScan();
            central.close();
            String macAddr = peripheral.getAddress();
            Intent intent = new Intent(getBaseContext(), DeviceActivity.class);
            intent.putExtra("ble_device", macAddr);
            startActivity(intent);
        }
    };





    private void initBluetooth(BluetoothAdapter bluetoothAdapter) {

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "Bluetooth LE is not avaliable", Toast.LENGTH_SHORT).show();
            finish();
        }

        else if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not avaliable", Toast.LENGTH_SHORT).show();
            finish();
        } else if (bluetoothAdapter.isEnabled() == false) {
            Toast.makeText(this, "Bluetooth is not enabled", Toast.LENGTH_SHORT).show();
            finish();
        }

        checkPermissions();

    }


    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

        for (int i : grantResults) {
            if(i == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(this, "Permisions were denied", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    public boolean checkPermissions() {

        String[] perm = {Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.ACCESS_BACKGROUND_LOCATION, Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION};

        for (String p : perm) {

            if (ContextCompat.checkSelfPermission(MainActivity.this, p) == PackageManager.PERMISSION_DENIED) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{p}, 2);

                }
            }

        }

        return true;

    }
}