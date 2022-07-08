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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.UUID;

public class DeviceActivity extends AppCompatActivity {
    private UUID dataCharacteristic = UUID.fromString("0000ffe4-0000-1000-8000-00805f9a34fb");



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
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);
        BluetoothDevice device = getIntent().getParcelableExtra("ble_device");
        vh = new ViewHolder();

        Bitmap dbitmap = BitmapFactory.decodeResource(getResources(), R.drawable.bullseye);
        Bitmap bitmap = dbitmap.copy(Bitmap.Config.ARGB_8888, true);
        vh.bullseye.setImageBitmap(bitmap);
        vh.bullseye.invalidate();


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            device.connectGatt(DeviceActivity.this, false, bluetoothGattCallback, BluetoothDevice.TRANSPORT_AUTO);
        } else {
            device.connectGatt(DeviceActivity.this, false, bluetoothGattCallback);
        }
        Toast.makeText(this, device.getName(), Toast.LENGTH_SHORT).show();

    }

    private double calibrationX, calibrationY = 0.0f;
    float angleX, angleY, angleZ = 0;

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

        Log.d("wh", "x: " + x + " y: " + y);
        w = vh.bullseye.getWidth();
        h = vh.bullseye.getHeight();
        Log.d("wh", "Width: " + w + " Height: " + h);

        double input_start = -90;
        double input_end = 90;

        //Used to emphasize inaccuracies, smaller angles make bigger changes to dot position.
        double close_stretch = 10;

        x = (x * close_stretch);
        y = (y * close_stretch);


        double xOut = ((x-input_start) * (w/(input_end*2)));
        double yOut = ((y-input_start) * (h/(input_end*2)));
        Log.d("wh", "New w: " + xOut + " New h: " + yOut);



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

            angleX = (((float) rollH * 256 + rollL)/32768)*180;
            angleY = (((float) pitchH * 256 + pitchL)/32768)*180;
            angleZ = (((float) yawH * 256 + yawL)/32768)*180;

            double[] calxy = calibrate(angleX, angleY);
            angleX = (float) calxy[0];
            angleY = (float) calxy[1];




            vh.xTV.setText("X: " + String.format("%.2f",angleX) + deg);
            vh.yTV.setText("Y: " + String.format("%.2f", angleY) + deg);
            vh.zTV.setText("Z: " + String.format("%.2f", angleZ) + deg);

            Log.d("xyz", "X: " + String.valueOf(angleX) + " Y: " + String.valueOf(angleY) + " Z: " + String.valueOf(angleZ));
            double[] xy = transformAngleToScreen(angleX, angleY);
            vh.bullseye.setCircleXY((float)xy[0],(float)xy[1]);
            vh.bullseye.invalidate();

        }
    }

    private String TAG = "gatt";

    BluetoothGattCallback bluetoothGattCallback =
            new BluetoothGattCallback() {
                @SuppressLint("MissingPermission")
                @Override
                public void onConnectionStateChange(BluetoothGatt gatt, int status,
                                                    int newState) {
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        Log.i(TAG, "Connected to GATT server.");
                        gatt.discoverServices();

                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {

                        Log.i(TAG, "Disconnected from GATT server.");
                    }
                }

                @Override
                public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                    //Log.d(TAG,"Notify");
                    super.onCharacteristicChanged(gatt, characteristic);
                    final byte[] data = characteristic.getValue();
                    String x = "";
                    for (int i = 0; i < data.length; i++) {

                        x = x + byteToHex(data[i]) + " ";

                    }

                    Log.d(TAG, x);


                }



                public String byteToHex(byte num) {
                    char[] hexDigits = new char[2];
                    hexDigits[0] = Character.forDigit((num >> 4) & 0xF, 16);
                    hexDigits[1] = Character.forDigit((num & 0xF), 16);
                    return new String(hexDigits);
                }

                @SuppressLint("MissingPermission")
                @Override
                public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        List<BluetoothGattService> services = gatt.getServices();
                        for (BluetoothGattService service : services) {
                            List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
                            for (BluetoothGattCharacteristic characteristic : characteristics) {

                                ///Once you have a characteristic object, you can perform read/write
                                //operations with it
                                Log.d(TAG, characteristic.getUuid().toString());

                               


                                if(characteristic.getUuid().equals(dataCharacteristic)) {
                                    Log.d(TAG, "match found reading...");
                                    gatt.readCharacteristic(characteristic);

                                    gatt.setCharacteristicNotification(characteristic, true);
                                    BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                                            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
                                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                                    gatt.writeDescriptor(descriptor);





                                }
                            }


                        }
                    }
                }
                @Override
                public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                    super.onCharacteristicWrite(gatt, characteristic, status);
                    Log.d(TAG, "Characteristic " + characteristic.getUuid() + " written");
                }
                @Override
                public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                    super.onCharacteristicRead(gatt, characteristic, status);
                    final byte[] data = characteristic.getValue();
                    for(byte b : data) {
                        Log.d(TAG, byteToHex(b));
                    }

                }
            };



}