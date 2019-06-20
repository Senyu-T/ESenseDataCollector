/* ESenseApplication: for eSense earbud to change configuration and record data
 *                    built based on eSense IO sample application
 * author: senyut
 */
package io.esense;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.app.ActivityCompat;
import android.content.pm.PackageManager;

import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.FileOutputStream;

import io.esense.esenselib.ESenseConfig;
import io.esense.esenselib.ESenseConnectionListener;
import io.esense.esenselib.ESenseEvent;
import io.esense.esenselib.ESenseEventListener;
import io.esense.esenselib.ESenseManager;
import io.esense.esenselib.ESenseSensorListener;

public class MainActivity extends Activity implements ESenseConnectionListener {
    ESenseManager manager;
    RadioGroup accRangeGroup;
    RadioGroup gyroRangeGroup;
    RadioGroup accLPFGroup;
    RadioGroup gyroLPFGroup;
    ESenseConfig deviceConfig;

    // Audio Recording Manager
    AudioManager mAudioManager;
    MediaRecorder mRecorder;
    BluetoothManager mBluetoothManager;
    BluetoothAdapter mBluetoothAdapter;

    private final String TAG = "ESenseActivity";
    private final int MY_PERMISSION_AUDIO = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        final int PERMISSION_REQUEST_COARSE_LOCATION = 456;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
        }
        mAudioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);

    }

    @Override
    protected void onStart(){
        super.onStart();

        accRangeGroup = findViewById(R.id.radAccRange);
        gyroRangeGroup = findViewById(R.id.radGyroRange);
        accLPFGroup = findViewById(R.id.radAccLPF);
        gyroLPFGroup = findViewById(R.id.radGyroLPF);

        findViewById(R.id.connect_btn).setOnClickListener(handleClick);
        findViewById(R.id.disconnect_btn).setOnClickListener(handleClick);
        findViewById(R.id.check_con_btn).setOnClickListener(handleClick);
        findViewById(R.id.regist_sensor_listener).setOnClickListener(handleClick);
        findViewById(R.id.unregist_sensor_listener).setOnClickListener(handleClick);
        findViewById(R.id.advertisment_get).setOnClickListener(handleClick);
        findViewById(R.id.advertisment_set).setOnClickListener(handleClick);
        findViewById(R.id.regist_evt_listener).setOnClickListener(handleClick);
        findViewById(R.id.unregist_evt_listener).setOnClickListener(handleClick);
        // Device configuration readings
        findViewById(R.id.btnSetName).setOnClickListener(handleClick);
        findViewById(R.id.btnReadName).setOnClickListener(handleClick);
        findViewById(R.id.btnSetConfig).setOnClickListener(handleClick);
        findViewById(R.id.btnReadConfig).setOnClickListener(handleClick);
        findViewById(R.id.btnReadOffset).setOnClickListener(handleClick);
        findViewById(R.id.btnReadBattery).setOnClickListener(handleClick);
        // Data Recording
        findViewById(R.id.recordData).setOnClickListener(handleClick);
        findViewById(R.id.stopRecordData).setOnClickListener(handleClick);
        findViewById(R.id.audio_connect).setOnClickListener(handleClick);

    }



    private View.OnClickListener handleClick = new View.OnClickListener(){
        // Sensor Listener
        ESSL mESSL = null;
        public void onClick(View view) {
            Button btn = (Button)view;
            switch(btn.getId()){
                case R.id.audio_connect:
                    Log.i(TAG, "Checking audio connection status");
                    int status = -100;
                    mBluetoothManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
                    mBluetoothAdapter = mBluetoothManager.getAdapter();
                    if (mBluetoothAdapter.isEnabled()) {
                        status = mBluetoothAdapter.getProfileConnectionState(BluetoothProfile.HEADSET);
                    }
                    TextView audTv = (TextView) findViewById(R.id.rtn_aud_connected);
                    if (status == BluetoothProfile.STATE_CONNECTED) {
                        audTv.setText("Audio Connected");

                    } else {
                        audTv.setText("Audio unconnected, audio recording disabled");
                    }
                    break;
                case R.id.recordData:
                    if (mESSL != null) {
                        Log.i(TAG, "set ESSL mode to RECORDING");
                        mESSL.setOnRecording(true);
                        Log.i(TAG, "recording audio");
                        startRecordingAudio();
                    }
                    break;
                case R.id.stopRecordData:
                    if (mESSL != null) {
                        Log.i(TAG, "setting mode to Stop Recording");
                        mESSL.setOnRecording(false);
                        stopRecordingAudio();
                    }
                    break;
                case R.id.connect_btn:
                    EditText nameText = findViewById(R.id.txtName);
                    String name = nameText.getText().toString();
                    manager = new ESenseManager(name, MainActivity.this.getApplicationContext(), MainActivity.this);
                    manager.connect(10000);
                    break;
                case R.id.disconnect_btn:
                    manager.disconnect();
                    break;
                case R.id.check_con_btn:
                    TextView tv2 = (TextView) findViewById(R.id.rtn_is_connect);
                    tv2.setText("" + manager.isConnected());
                    break;
                case R.id.regist_sensor_listener:
                    EditText samplingText = findViewById(R.id.set_sampling);
                    if(!samplingText.getText().toString().isEmpty()) {
                        mESSL = new ESSL();
                        manager.registerSensorListener(mESSL, Integer.parseInt(samplingText.getText().toString()));
                    }
                    break;
                case R.id.unregist_sensor_listener:
                    manager.unregisterSensorListener();
                    break;
                case R.id.advertisment_get:
                    manager.getAdvertisementAndConnectionInterval();
                    break;
                case R.id.advertisment_set:
                    EditText advMin = findViewById(R.id.ad_set_min);
                    EditText advMax = findViewById(R.id.ad_set_max);
                    EditText connMin = findViewById(R.id.conn_set_min);
                    EditText connMax = findViewById(R.id.conn_set_max);

                    if(!advMin.getText().toString().isEmpty() && !advMax.getText().toString().isEmpty() && !connMin.getText().toString().isEmpty() && !connMax.getText().toString().isEmpty()) {
                        manager.setAdvertisementAndConnectiontInterval(Integer.parseInt(advMin.getText().toString()), Integer.parseInt(advMax.getText().toString()),
                                Integer.parseInt(connMin.getText().toString()), Integer.parseInt(connMax.getText().toString()));
                    }
                    break;
                case R.id.regist_evt_listener:
                    ESEL esel = new ESEL();
                    manager.registerEventListener(esel);
                    break;
                case R.id.unregist_evt_listener:
                    manager.unregisterEventListener();
                    break;
                case R.id.btnSetName:
                    EditText eName = findViewById(R.id.txtName);
                    manager.setDeviceName(eName.getText().toString());
                    break;
                case R.id.btnReadName:
                    manager.getDeviceName();
                    break;
                case R.id.btnReadConfig:
                    manager.getSensorConfig();
                    break;
                case R.id.btnReadBattery:
                    manager.getBatteryVoltage();
                    break;
                case R.id.btnSetConfig:
                    RadioButton accRange = findViewById(accRangeGroup.getCheckedRadioButtonId());
                    RadioButton gyrorange = findViewById(gyroRangeGroup.getCheckedRadioButtonId());
                    RadioButton accLpf = findViewById(accLPFGroup.getCheckedRadioButtonId());
                    RadioButton gyroLpf = findViewById(gyroLPFGroup.getCheckedRadioButtonId());

                    ESenseConfig config = new ESenseConfig(ESenseConfig.AccRange.valueOf(accRange.getText().toString()), ESenseConfig.GyroRange.valueOf(gyrorange.getText().toString()),
                            ESenseConfig.AccLPF.valueOf(accLpf.getText().toString()), ESenseConfig.GyroLPF.valueOf(gyroLpf.getText().toString()));
                    manager.setSensorConfig(config);
                    break;
                case R.id.btnReadOffset:
                    manager.getAccelerometerOffset();
                    break;
            }

        }
    };

    public void onConnected(final ESenseManager manager){
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                TextView tv = (TextView) findViewById(R.id.main_txt);
                tv.setText("onConnect");
            }
        });
    }

    public void onDisconnected(ESenseManager manager){
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                TextView tv = (TextView) findViewById(R.id.main_txt);
                tv.setText("onDisconnect");
            }
        });
    }

    public void onDeviceFound(ESenseManager manager){
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                TextView tv = (TextView) findViewById(R.id.main_txt);
                tv.setText("onDeviceFound");
            }
        });
    }

    public void onDeviceNotFound(ESenseManager manager){
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                TextView tv = (TextView) findViewById(R.id.main_txt);
                tv.setText("onDeviceNotFound");
            }
        });
    }

    // Bluetooth Audio recorder
    private void startRecordingAudio() {
        String startTime = String.valueOf(System.currentTimeMillis());
        final String pathSave = getExternalFilesDir(null).toString() + "/" + startTime + ".3gpp";
        // Ask for permission to record
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    MY_PERMISSION_AUDIO);
            Log.i(TAG, "run-time audio access permission granted");
        }
        Thread aud_record = new Thread() {
            @Override
            public void run() {
                mRecorder = new MediaRecorder();
                mRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
                mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
                mRecorder.setOutputFile(pathSave);
                mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
                try {
                    mRecorder.prepare();
                } catch (Exception e) {
                    Log.i(TAG, "recorder prepare failed!");
                }
                mAudioManager.stopBluetoothSco();
                mAudioManager.startBluetoothSco();
                Log.i(TAG, "recording begins");

                registerReceiver(new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        //Log.i(TAG, "AudioManager.SCO_AUDIO_STATE_CONNECTED" );
                        // Open SCO
                        mAudioManager.setBluetoothScoOn(true);
                        //Log.i(TAG, "Routing:" + mAudioManager.isBluetoothScoOn());
                        mAudioManager.setMode(AudioManager.STREAM_MUSIC);
                        // Begin recording
                        long curTime = System.currentTimeMillis();
                        mRecorder.start();
                        Log.i(TAG, "Begin recording audio at: " + curTime);
                        unregisterReceiver(this);
                    }
                }, new IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED));
            }
        };
        aud_record.run();
    }

    private void stopRecordingAudio() {
        //mAudioManager.stopBluetoothSco();
        mRecorder.stop();
        mRecorder.release();
        mRecorder = null;
        Log.i(TAG, "audio recorder stopped");
        if (mAudioManager.isBluetoothScoOn()) {
            mAudioManager.setBluetoothScoOn(false);
            mAudioManager.stopBluetoothSco();
        }
    }

    // ESense Earbud Event Interface
    public class ESEL implements ESenseEventListener {

        public void onButtonEventChanged(final boolean pressed){
            runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    TextView tv = (TextView) findViewById(R.id.main_txt);
                    tv.setText("onButtonEventChanged");
                    TextView tv2 = (TextView) findViewById(R.id.txt_btn);
                    tv2.setText("" + pressed);
                }
            });
        }

        public void onAdvertisementAndConnectionIntervalRead(final int minAdvertsingInterval, final int maxAdvertsingInterval, final int minConnectionInterval, final int maxConnectionInterval){
            Log.i(TAG, "onAdvertisementAndConnectionIntervalRead() min=" + minAdvertsingInterval + ", max=" + maxAdvertsingInterval);
            runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    TextView tv = (TextView) findViewById(R.id.ad_set_min);
                    tv.setText("" + minAdvertsingInterval);
                    tv = (TextView) findViewById(R.id.ad_set_max);
                    tv.setText("" + maxAdvertsingInterval);

                    tv = (TextView) findViewById(R.id.conn_set_min);
                    tv.setText("" + minConnectionInterval);
                    tv = (TextView) findViewById(R.id.conn_set_max);
                    tv.setText("" + maxConnectionInterval);
                }
            });
        }
        public void onDeviceNameRead(String deviceName){
            Log.i(TAG, "onDeviceNameRead() called & value is : " + deviceName);
        }

        @Override
        public void onSensorConfigRead(ESenseConfig config) {
            deviceConfig = config;
            Log.i(TAG, "Acc Range: " + config.getAccRange() + " Gyro Range: " + config.getGyroRange());
            Log.i(TAG, "ACC LPF: " + config.getAccLPF() + " Gyro LPF: " + config.getGyroLPF());
        }

        @Override
        public void onAccelerometerOffsetRead(int offsetX, int offsetY, int offsetZ) {
            Log.i(TAG, "Acc Offset: " + offsetX + " | " + offsetY + " | " + offsetZ);
        }

        @Override
        public void onBatteryRead(double voltage){
            Log.i(TAG,"onBatteryRead() voltage is " + voltage);
        }
    }


    // ESense Earbud Sensor Class.
    // currently the moment when registering sensor, recording begins
    public class ESSL implements ESenseSensorListener {
        // TextViews for displaying data on phone
        private TextView accXView;
        private TextView accYView;
        private TextView accZView;
        private TextView gyroXView;
        private TextView gyroYView;
        private TextView gyroZView;
        // Flag for recording
        private boolean onRecording;
        // File directory for data saved
        private String pathSave;
        private File mData;
        private byte[] comma;
        private byte[] newLine;

        public ESSL(){
            accXView = findViewById(R.id.a_x);
            accYView = findViewById(R.id.a_y);
            accZView = findViewById(R.id.a_z);
            gyroXView = findViewById(R.id.g_x);
            gyroYView = findViewById(R.id.g_y);
            gyroZView = findViewById(R.id.g_z);
            String startTime = String.valueOf(System.currentTimeMillis());
            onRecording = false;
            pathSave = getExternalFilesDir(null).toString() + "/" + startTime + "txt";
            mData= new File(pathSave);
            try {
                if (!mData.exists()){
                    Log.i(TAG, "IMU data storage file created at: " + pathSave);
                    mData.createNewFile();
                }
            } catch (IOException e) {
                Log.d(TAG, "Error in creating IMU recording file");
            }
            String c = " ";
            String n = "\n";
            comma = c.getBytes();
            newLine = n.getBytes();
        }

        private void updateUI(final double acc[], final double gyro[]){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    DecimalFormat f = new DecimalFormat("##.000");
                    accXView.setText("" + f.format(acc[0]));
                    accYView.setText("" + f.format(acc[1]));
                    accZView.setText("" + f.format(acc[2]));
                    gyroXView.setText("" + f.format(gyro[0]));
                    gyroYView.setText("" + f.format(gyro[1]));
                    gyroZView.setText("" + f.format(gyro[2]));
                }
            });
        }

        // logging IMU data into the txt file as byte stream.
        private void storeIMU(final double acc[], final double gyro[]) {
            Thread imuIO = new Thread() {
                @Override
                public void run() {
                    long curTime = System.currentTimeMillis();
                    // 6 doubles, 7 " ", 1 "\n", 1 long (time)
                    byte[] acc_x = String.valueOf(acc[0]).getBytes();
                    byte[] acc_y = String.valueOf(acc[1]).getBytes();
                    byte[] acc_z = String.valueOf(acc[2]).getBytes();
                    byte[] gyr_x = String.valueOf(gyro[0]).getBytes();
                    byte[] gyr_y = String.valueOf(gyro[1]).getBytes();
                    byte[] gyr_z = String.valueOf(gyro[2]).getBytes();
                    byte[] time = String.valueOf(curTime).getBytes();

                    ByteBuffer info = ByteBuffer.allocate(acc_x.length + acc_y.length + acc_z.length
                            + gyr_x.length + gyr_y.length + gyr_z.length + comma.length * 6 +
                            newLine.length + time.length);
                    info.put(time);
                    info.put(comma);
                    info.put(acc_x);
                    info.put(comma);
                    info.put(acc_y);
                    info.put(comma);
                    info.put(acc_z);
                    info.put(comma);
                    info.put(gyr_x);
                    info.put(comma);
                    info.put(gyr_y);
                    info.put(comma);
                    info.put(gyr_z);
                    info.put(newLine);
                    byte[] toBeWritten = info.array();

                    // write data into the file
                    try {
                        OutputStream fo = new FileOutputStream(mData, true);
                        fo.write(toBeWritten);
                        fo.close();
                    } catch (IOException e) {
                        Log.d(TAG, "Error in writing IMU data");
                    }
                }
            };
            imuIO.start();
        }

        public void setOnRecording(boolean flag) {
            onRecording = flag;
        }

        public void onSensorChanged(final ESenseEvent evt){
            if(deviceConfig != null){
                // If we know the device configuration we can convert the raw data
                double[] acc_g;
                double[] gyro_deg;
                acc_g = evt.convertAccToG(deviceConfig);
                gyro_deg = evt.convertGyroToDegPerSecond(deviceConfig);
                if (onRecording) {
                    long curTime = System.currentTimeMillis();
                    Log.i(TAG, "Start recording IMU data at: " + curTime);
                    storeIMU(acc_g, gyro_deg);
                }
                updateUI(acc_g, gyro_deg);
            } else {
                short[] acc;
                short[] gyro;
                acc = evt.getAccel();
                gyro = evt.getGyro();

                double[] acc_double = new double[acc.length];
                double[] gyro_double = new double[gyro.length];

                for (int j=0;j<acc.length;j++) {
                    acc_double[j] = (double)acc[j];
                    gyro_double[j] = (double)gyro[j];
                }
                if (onRecording) {
                    long curTime = System.currentTimeMillis();
                    Log.i(TAG, "Start recording IMU data at: " + curTime);
                    storeIMU(acc_double, gyro_double);
                }
                updateUI(acc_double, gyro_double);
            }
        }

    }

}

