package com.example.tap;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.tapwithus.sdk.TapListener;
import com.tapwithus.sdk.TapSdk;
import com.tapwithus.sdk.TapSdkFactory;
import com.tapwithus.sdk.airmouse.AirMousePacket;
import com.tapwithus.sdk.mode.Point3;
import com.tapwithus.sdk.mode.RawSensorData;
import com.tapwithus.sdk.mode.TapInputMode;
import com.tapwithus.sdk.mouse.MousePacket;
import com.tapwithus.sdk.tap.Tap;

import okhttp3.OkHttpClient;
import okhttp3.Request;

public class MainActivity extends AppCompatActivity {

    private TapSdk sdk;


    // Tap{identifier='F8:1A:F9:44:1B:3C', name='Tap_D3962327', battery=67, serialNumber='000000A01DBB7396', hwVer='3.3', fwVer='2.3.27'}

    private boolean startWithControllerMode = true;
    private String lastConnectedTapAddress = "";
    private boolean calibrating = false;
    private int imuCounter;
    private int devCounter;
    private TextView textView;
    private TextView valueView;
    private Button btnCalibrate;
    private int xMouse = 0, yMouse = 0, pMouse = 0, led0 = -1000, led1 = 0, led2 = 1000;


    OkHttpClient client = new OkHttpClient();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        log("app started");
        textView = findViewById(R.id.textView);
        valueView = findViewById(R.id.valueView);
        btnCalibrate = findViewById(R.id.calibrate);

        SharedPreferences sharedPrefAll = PreferenceManager.getDefaultSharedPreferences(this);
        led0 = sharedPrefAll.getInt("led0", -1000);
        led2 = sharedPrefAll.getInt("led2", 1000);


        sdk = TapSdkFactory.getDefault(this);
        sdk.registerTapListener(tapListener);

        sdk.enableDebug();
        //if (!startWithControllerMode) {
        //sdk.setDefaultMode(TapInputMode.rawSensorData((byte)0,(byte)0,(byte)0), true);
        sdk.setDefaultMode(TapInputMode.text(), true);
        //}
        if (sdk.isConnectionInProgress()) {
            log("A Tap is connecting");
        }


        //sdk.startRawSensorMode("F8:1A:F9:44:1B:3C", (byte)0,(byte)0,(byte)0);
        //sdk.startControllerMode("F8:1A:F9:44:1B:3C");

    }

    public void calibrate(View view) {
        if (btnCalibrate.getText().equals("CALIBRATE")) {
            calibrating = true;
            textView.setText("Point at the left LED and then press the button:");
            btnCalibrate.setText("LEFT LED");
            return;
        } else if (btnCalibrate.getText().equals("LEFT LED")) {
            xMouse = 0;
            led0 = 0;
            yMouse = 0;
            textView.setText("Point on the middle LED and then press the button:");
            btnCalibrate.setText("MIDDLE LED");
            return;
        } else if (btnCalibrate.getText().equals("MIDDLE LED")) {
            led1 = xMouse;
            textView.setText("Point on the right LED and then press the button:");
            btnCalibrate.setText("RIGHT LED");
            return;
        } else if (btnCalibrate.getText().equals("RIGHT LED")) {
            led2 = xMouse;
            textView.setText("Press the button to calibrate the location of the LEDs:");
            btnCalibrate.setText("CALIBRATE");
            Toast.makeText(getBaseContext(), "Calibrated Successfully", Toast.LENGTH_SHORT).show();
            calibrating = false;

            SharedPreferences sharedPrefAll = PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor editorAll = sharedPrefAll.edit();
            editorAll.putInt("led1", led1);
            editorAll.putInt("led2", led2);
            editorAll.apply();

            log("led1: " + led1);
            log("led2: " + led2);
        }


    }

    @Override
    protected void onStop() {
        super.onStop();
        sdk.unregisterTapListener(tapListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sdk.unregisterTapListener(tapListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        sdk.registerTapListener(tapListener);
    }


    private TapListener tapListener = new TapListener() {

        @Override
        public void onBluetoothTurnedOn() {
            log("Bluetooth turned ON");
        }

        @Override
        public void onBluetoothTurnedOff() {
            log("Bluetooth turned OFF");
        }

        @Override
        public void onTapStartConnecting(@NonNull String tapIdentifier) {
            log("Tap started connecting - " + tapIdentifier);
        }

        @Override
        public void onTapConnected(@NonNull String tapIdentifier) {
            log("TAP connected " + tapIdentifier);
            Tap tap = sdk.getCachedTap(tapIdentifier);
            if (tap == null) {
                log("Unable to get cached Tap");
                return;
            }

            lastConnectedTapAddress = tapIdentifier;
            log(tap.toString());
        }

        @Override
        public void onTapDisconnected(@NonNull String tapIdentifier) {
            log("TAP disconnected " + tapIdentifier);

        }

        @Override
        public void onTapResumed(@NonNull String tapIdentifier) {
            log("TAP resumed " + tapIdentifier);
            Tap tap = sdk.getCachedTap(tapIdentifier);
            if (tap == null) {
                log("Unable to get cached Tap");
                return;
            }

            log(tap.toString());

        }

        @Override
        public void onTapChanged(@NonNull String tapIdentifier) {

        }


        @Override
        public void onTapInputReceived(@NonNull String tapIdentifier, int data) {
            log("tap: " + data);
        }

        @Override
        public void onMouseInputReceived(@NonNull String tapIdentifier, @NonNull MousePacket data) {
            //textView.setText(data.toString());
            //log("mouse: " + data.dx.getInt());
//            if (data.dx.getInt() > 0) {
//                xMouse++;
//            } else if (data.dx.getInt() < 0){
//                xMouse--;
//            }
            xMouse += data.dx.getInt();
            yMouse += data.dy.getInt();

            pMouse += data.proximity.getInt();

            if (calibrating) return;

            /*if(xMouse>led2 * 2) xMouse=(int)(led2 * 2);
            if(xMouse<led0 * 1.5) xMouse=(int)(led0 * 1.5);*/

        }

        @Override
        public void onAirMouseInputReceived(@NonNull String tapIdentifier, @NonNull AirMousePacket data) {
            //log(data.gesture.getString());

            System.out.println(data.gesture.getInt());

            if (calibrating) return;

            String url;

            if (data.gesture.getInt() == AirMousePacket.AIR_MOUSE_GESTURE_MIDDLE_TO_THUMB_TOUCH) {
                System.out.println("xMouse: " + xMouse + "   yMouse: " + yMouse + "   pMouse: " + pMouse);
                if (xMouse > led1 / 2 && xMouse <= (led2 + led1) / 2)
                    url = "http://192.168.5.164:5000/device/1/toggle";
                else if (xMouse > (led1 + led2) / 2)
                    url = "http://192.168.5.164:5000/device/2/toggle";
                else {
                    url = "http://192.168.5.164:5000/device/0/toggle";
                }

                Request request = new Request.Builder().url(url).build();

                sdk.vibrate(tapIdentifier, new int[]{500});

                try {
                    client.newCall(request).execute();
                } catch (Exception e) {
                }

            } else if (data.gesture.getInt() == AirMousePacket.AIR_MOUSE_GESTURE_INDEX_TO_THUMB_TOUCH) {
                xMouse = 0;
                yMouse = 0;
                pMouse = 0;
            } else if (data.gesture.getInt() == AirMousePacket.AIR_MOUSE_GESTURE_LEFT || data.gesture.getInt() == AirMousePacket.AIR_MOUSE_GESTURE_LEFT_TWO_FINGERS) {
                url = "http://192.168.5.164:5000/next";
                Request request = new Request.Builder().url(url).build();

                sdk.vibrate(tapIdentifier, new int[]{500});

                try {
                    client.newCall(request).execute();
                } catch (Exception e) {
                }
            } else if (data.gesture.getInt() == AirMousePacket.AIR_MOUSE_GESTURE_RIGHT || data.gesture.getInt() == AirMousePacket.AIR_MOUSE_GESTURE_RIGHT_TWO_FINGERS) {
                url = "http://192.168.5.164:5000/prev";
                Request request = new Request.Builder().url(url).build();

                sdk.vibrate(tapIdentifier, new int[]{500});

                try {
                    client.newCall(request).execute();
                } catch (Exception e) {
                }
            } else if (data.gesture.getInt() == AirMousePacket.AIR_MOUSE_GESTURE_UP || data.gesture.getInt() == AirMousePacket.AIR_MOUSE_GESTURE_UP_TWO_FINGERS) {
                url = "http://192.168.5.164:5000/toggle-play";
                Request request = new Request.Builder().url(url).build();

                sdk.vibrate(tapIdentifier, new int[]{500});

                try {
                    client.newCall(request).execute();
                } catch (Exception e) {
                }
            }
        }

        @Override
        public void onRawSensorInputReceived(String tapIdentifier, RawSensorData rsData) {
            //log(rsData.toString());
            //textView.setText(rsData);
            if (rsData.dataType == RawSensorData.DataType.Device) {
                // Fingers accelerometer.
                // Each point in array represents the accelerometer value of a finger (thumb, index, middle, ring, pinky).
                Point3 index = rsData.getPoint(RawSensorData.iDEV_INDEX);
                if (index != null) {
                    double x = index.x;
                    double y = index.y;
                    double z = index.z;
                    //textView.setText("index acc:\n"+x+"\n"+y+"\n"+z+"\n");
                    logText("gyro:\n" + x + "\n" + y + "\n" + z + "\n");
                }
                // Etc... use indexes: RawSensorData.iDEV_THUMB, RawSensorData.iDEV_INDEX, RawSensorData.iDEV_MIDDLE, RawSensorData.iDEV_RING, RawSensorData.iDEV_PINKY
            } else if (rsData.dataType == RawSensorData.DataType.IMU) {
                // Refers to an additional accelerometer on the Thumb sensor and a Gyro (placed on the thumb unit as well).
                Point3 gyro = rsData.getPoint(RawSensorData.iIMU_GYRO);
                if (gyro != null) {
                    double x = gyro.x;
                    double y = gyro.y;
                    double z = gyro.z;

                    //if(Math.abs(z)==0);
                    //textView.append("gyro:\n"+x+"\n"+y+"\n"+z+"\n");

                    //logText("index acc:\n"+(int)x/50+"\n"+(int)y/50+"\n"+(int)z/50+"\n");
                }

                // Etc... use indexes: RawSensorData.iIMU_GYRO, RawSensorData.iIMU_ACCELEROMETER
            }


        }

        @Override
        public void onTapChangedState(String tapIdentifier, int state) {
            log("Mode Changed to " + state);
            if (state == 1) {
                sdk.setDefaultMode(TapInputMode.controller(), true);
            } else {
                sdk.setDefaultMode(TapInputMode.text(), true);
            }

        }

        @Override
        public void onError(@NonNull String tapIdentifier, int code, @NonNull String description) {

        }


    };


    private void log(String message) {

        Log.e(this.getClass().getSimpleName(), message);

        //textView.setText(message);
    }

    private void logText(String message) {


        log(message);

        valueView.setText(message);
    }


}
