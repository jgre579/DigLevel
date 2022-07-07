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
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import java.util.List;
import java.util.UUID;

public class DeviceActivity extends AppCompatActivity {
    private UUID dataCharacteristic = UUID.fromString("0000ffe4-0000-1000-8000-00805f9a34fb");
    class ViewHolder {
        CustomBullseye bullseye;
        public ViewHolder() {
            bullseye = findViewById(R.id.bullseye);


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

    private int h;
    private int w;

    private double[] transformAngleToScreen(double x, double y) {

        Log.d("wh", "x: " + x + " y: " + y);
        w = vh.bullseye.getWidth();
        h = vh.bullseye.getHeight();
        Log.d("wh", "Width: " + w + " Height: " + h);

        double input_start = -90;
        double input_end = 90;

        //Used to emphasize inaccuracies, smaller angles make bigger changes to dot position.
        double close_stretch = 2;

        x = (x * close_stretch);
        y = (y * close_stretch);


        double xOut = ((x-input_start) * (w/(input_end*2)));
        double yOut = ((y-input_start) * (h/(input_end*2)));
        Log.d("wh", "New w: " + xOut + " New h: " + yOut);



        double[] xy = {xOut, yOut};


        return xy;



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
                    Log.d(TAG,"Notify");
                    super.onCharacteristicChanged(gatt, characteristic);
                    byte startByte = 0x55;
                    byte angleByte = 0x61;
                    float angleX, angleY, angleZ = 0;
                    final byte[] data = characteristic.getValue();

                    Log.d(TAG, byteToHex(data[0]));
                    Log.d(TAG, byteToHex(data[1]));

                    if(data[0] == startByte && data[1] == angleByte) {

                        byte rollH = data[15];
                        byte rollL = data[14];

                        byte pitchH= data[17];
                        byte pitchL= data[16];

                        byte yawH = data[19];
                        byte yawL = data[18];

                        Log.d(TAG, "Roll H: "+ rollH);
                        Log.d(TAG, "Roll H b : "+ byteToHex(rollH));
                        long t = (long) rollH * (256);
                        long t1 = t + rollL;
                        long t2 = t1/32768;
                        long t3 = t2 * 180;
                        angleX = (((float) rollH * 256 + rollL)/32768)*180;
                        angleY = (((float) pitchH * 256 + pitchL)/32768)*180;
                        angleZ = (((float) yawH * 256 + yawL)/32768)*180;

//                            angleX = (((rollH<<8)|rollL)/32768) * 180;
//                            angleY = ((pitchH<<8)|pitchL)/32768*180;
//                            angleZ = ((yawH<<8)|yawL)/32768*180;

                        Log.d("xyz", "X: " + String.valueOf(angleX) + " Y: " + String.valueOf(angleY) + " Z: " + String.valueOf(angleZ));
                        double[] xy = transformAngleToScreen(angleX, angleY);
                        vh.bullseye.setCircleXY((float)xy[0],(float)xy[1]);
                        vh.bullseye.invalidate();

                    }


                    for(byte b : data) {

                        //Log.d(TAG, byteToHex(b));
                        //Log.d(TAG, String.valueOf(b));
                    }
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
                                //gatt.readCharacteristic(characteristic);


                                if(characteristic.getUuid().equals(dataCharacteristic)) {
                                    Log.d(TAG, "match found reading...");
                                    //gatt.readCharacteristic(characteristic);

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
                public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                    super.onCharacteristicRead(gatt, characteristic, status);
                    final byte[] data = characteristic.getValue();
                    for(byte b : data) {
                        Log.d(TAG, byteToHex(b));
                    }

                }
            };



}