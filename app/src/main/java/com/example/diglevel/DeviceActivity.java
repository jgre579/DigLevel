package com.example.diglevel;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.ScanResult;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.welie.blessed.BluetoothBytesParser;
import com.welie.blessed.BluetoothCentralManager;
import com.welie.blessed.BluetoothCentralManagerCallback;
import com.welie.blessed.BluetoothPeripheral;
import com.welie.blessed.BluetoothPeripheralCallback;
import com.welie.blessed.GattStatus;
import com.welie.blessed.HciStatus;
import com.welie.blessed.WriteType;

import java.util.List;
import java.util.UUID;

public class DeviceActivity extends AppCompatActivity {

    class ViewHolder {
        CustomBullseye bullseye;
        TextView xTV, yTV, zTV, calibrateTV;
        public ViewHolder() {
            bullseye = findViewById(R.id.bullseye);
            xTV = findViewById(R.id.angle_x_text);
            yTV = findViewById(R.id.angle_y_text);
            zTV = findViewById(R.id.angle_z_text);
            calibrateTV = findViewById(R.id.calibrate_text);



        }
    }
    ViewHolder vh;
    BluetoothCentralManager central;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);
        String macAddr = getIntent().getStringExtra("ble_device");
        central = new BluetoothCentralManager(getApplicationContext(), bluetoothCentralManagerCallback, new Handler(Looper.getMainLooper()));
        vh = new ViewHolder();

        Bitmap dbitmap = BitmapFactory.decodeResource(getResources(), R.drawable.bullseye);
        Bitmap bitmap = dbitmap.copy(Bitmap.Config.ARGB_8888, true);
        vh.bullseye.setImageBitmap(bitmap);
        vh.bullseye.invalidate();

        BluetoothPeripheral peripheral = central.getPeripheral(macAddr);
        Log.d("gatt", "Passed to device: " + peripheral.getName());

        Log.d(TAG, "Attempting Connect");
        central.connectPeripheral(peripheral, peripheralCallback);


        //Toast.makeText(this, device.getName(), Toast.LENGTH_SHORT).show();

    }


    String TAG = "gatt";
    private final BluetoothPeripheralCallback peripheralCallback = new BluetoothPeripheralCallback() {

        public void onConnectedPeripheral(BluetoothPeripheral peripheral) {

            Log.d(TAG, "Peripheral connected");

        }
        public void onConnectionFailed(BluetoothPeripheral peripheral, HciStatus status) {

        }
        public void onDisconnectedPeripheral(BluetoothPeripheral peripheral, HciStatus status) {

        }



        public void onServicesDiscovered(BluetoothPeripheral peripheral) {

            Log.d(TAG, "Services discovered");
            BluetoothGattService service =  peripheral.getService(UUID.fromString("0000ffe5-0000-1000-8000-00805f9a34fb"));
            BluetoothGattCharacteristic notifyChar =  peripheral.getCharacteristic(service.getUuid(), UUID.fromString("0000ffe4-0000-1000-8000-00805f9a34fb"));
            BluetoothGattCharacteristic writeChar =  peripheral.getCharacteristic(service.getUuid(), UUID.fromString("0000ffe9-0000-1000-8000-00805f9a34fb"));
            Log.d(TAG, "Notify : " + notifyChar.getUuid().toString());
            Log.d(TAG, "Write : " + writeChar.getUuid().toString());

            peripheral.setNotify(notifyChar, true);
            byte[] commandAltitude = {(byte) 0xFF, (byte) 0xAA, 0x27, 0x45 ,0x00};
            peripheral.writeCharacteristic(writeChar, commandAltitude, WriteType.WITHOUT_RESPONSE);


        }
        public void onCharacteristicUpdate(BluetoothPeripheral peripheral, byte[] value, BluetoothGattCharacteristic characteristic, GattStatus status) {

            final byte[] data = characteristic.getValue();
            String x = "";

            for (int i = 0; i < data.length; i++) {

                x = x + byteToHex(data[i]) + " ";

            }

            Log.d(TAG, x);
            newData(data);


        }

        public void onCharacteristicWrite(BluetoothPeripheral peripheral, byte[] value, BluetoothGattCharacteristic characteristic, final GattStatus status) {

            Log.d(TAG, "WRITTEN");

        }


        public String byteToHex(byte num) {
                    char[] hexDigits = new char[2];
                    hexDigits[0] = Character.forDigit((num >> 4) & 0xF, 16);
                    hexDigits[1] = Character.forDigit((num & 0xF), 16);
                    return new String(hexDigits);
        }

    };





    private final BluetoothCentralManagerCallback bluetoothCentralManagerCallback = new BluetoothCentralManagerCallback() {

    };

    private double calibrationX, calibrationY = 0.0f;
    double angleX, angleY, angleZ = 0;
    double cAngleX, cAngleY, cAngleZ = 0;

    public void clickCalibrate(View view) {

        calibrationX = -angleX;
        calibrationY = -angleY;

    }

    public void clickReset(View view) {

        calibrationX = 0.0f;
        calibrationY = 0.0f;
    }


    private int h;
    private int w;
    String deg = "\u00B0";

    private double[] calibrate(double x, double y) {


        double xOut = x + calibrationX;
        double yOut = y + calibrationY;
        vh.calibrateTV.setText("Current Calibration X : " + String.format("%.2f",calibrationX) + deg + " Y: " + String.format("%.2f",calibrationY) + deg );

        double[] xy = {xOut, yOut};

        return xy;


    }

    private double[] transformAngleToScreen(double x, double y) {

        //Log.d("wh", "x: " + x + " y: " + y);
        w = vh.bullseye.getWidth();
        h = vh.bullseye.getHeight();
        //Log.d("wh", "Width: " + w + " Height: " + h);

        double input_start = -90;
        double input_end = 90;

        //Used to emphasize inaccuracies, smaller angles make bigger changes to dot position.
        double close_stretch = 10;

        x = (x * close_stretch);
        y = (y * close_stretch);


        double xOut = ((x-input_start) * (w/(input_end*2)));
        double yOut = ((y-input_start) * (h/(input_end*2)));
        //Log.d("wh", "New w: " + xOut + " New h: " + yOut);



        double[] xy = {xOut, yOut};


        return xy;



    }

    private void newData(byte[] data) {
        byte startByte = 0x55;
        byte angleByte = 0x61;

        if(data[0] == startByte && data[1] == angleByte) {

            byte rollH = data[15];
            byte rollL = data[14];

            byte pitchH= data[17];
            byte pitchL= data[16];

            byte yawH = data[19];
            byte yawL = data[18];

            angleX = (( rollH * 256.0 + rollL)/32768)*180;
            angleY = (( pitchH * 256.0 + pitchL)/32768)*180;
            angleZ = (( yawH * 256.0 + yawL)/32768)*180;

            double[] calxy = calibrate(angleX, angleY);
            cAngleX = calxy[0];
            cAngleY = calxy[1];




            vh.xTV.setText("X: " + String.format("%.2f",cAngleX) + deg);
            vh.yTV.setText("Y: " + String.format("%.2f", cAngleY) + deg);
            vh.zTV.setText("Z: " + String.format("%.2f", angleZ) + deg);

            Log.d("xyz", "X: " + String.valueOf(cAngleX) + " Y: " + String.valueOf(cAngleY) + " Z: " + String.valueOf(angleZ));
            double[] xy = transformAngleToScreen(cAngleX, cAngleY);
            vh.bullseye.setCircleXY(xy[0], xy[1]);
            vh.bullseye.invalidate();

        }
    }




}