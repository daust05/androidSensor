package com.example.test;

import static java.lang.System.exit;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private static final String TAG = "MainActivity";
    private SensorManager sensorManager;
    private Sensor linearAccelerSensor;
    TextView tvgXaxis, tvgYaxis, tvgZaxis, audioVlm;
    Button recordBtn;
    private static MyHandler myHandler;
    float lAccX, lAccY, lAccZ;

    private final int SAMPLE_RATE = 44100;
    private final int AUDIO_RECORD_REQUEST_CODE = 1001;

    private int minBufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT);

    private AudioRecord ar = null;
    private Thread rt = null;
    private boolean isRecording = false;

    class BtnOnClickListener implements Button.OnClickListener {
        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.btn_record:
                    if (isRecording) {
                        isRecording = false;
                        recordBtn.setText("start recording");
                    } else {
                        isRecording = true;
                        Log.d(TAG, "record start");
                        recordThread();
                        rt.start();
                        recordBtn.setText("stop recording");
                    }
                    return;
            }
        }
    }

    class MyHandler extends Handler{
        static final int AUDIO_VLM = 0;

        @Override
        public void handleMessage(Message msg){
            super.handleMessage(msg);
            switch (msg.what){
                case AUDIO_VLM:
                    Bundle bundle = msg.getData();
                    int maxDB = bundle.getInt("maxDB");
                    audioVlm.setText(Integer.toString(maxDB) + " dB");
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvgXaxis = (TextView) findViewById(R.id.tvgXaxis);
        tvgYaxis = (TextView) findViewById(R.id.tvgYaxis);
        tvgZaxis = (TextView) findViewById(R.id.tvgZaxis);
        audioVlm = (TextView) findViewById(R.id.audioVlm);

        BtnOnClickListener onClickListener = new BtnOnClickListener();
        recordBtn = (Button) findViewById(R.id.btn_record);
        recordBtn.setOnClickListener(onClickListener);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        linearAccelerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);

        myHandler = new MyHandler();
    }

    public void recordThread() {
        checkSelfPermission();
        ar = new AudioRecord(MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_STEREO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBufferSize);
        ar.startRecording();
        rt = new Thread(new Runnable() {
            @Override
            public void run() {
                short[] readData = new short[minBufferSize];
                while (isRecording) {
                    int ret = ar.read(readData, 0, minBufferSize);
//                    Log.d(TAG, "read bytes is " + ret);
                    int maxDB = 0;
                    for (short s : readData) {
                        if (Math.abs(s) > maxDB) {
                            maxDB = Math.abs(s);
                        }
                    }
                    Bundle bundle = new Bundle();
                    bundle.putInt("maxDB",maxDB);

                    Message message = new Message();
                    message.what = MyHandler.AUDIO_VLM;
                    message.setData(bundle);

                    myHandler.sendMessage(message);

                    Log.d(TAG,"max: "+ maxDB);
                }
                Log.d(TAG, "record stop");

                ar.stop();
                ar.release();
                ar = null;
            }
        });
    }

    public void checkSelfPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, AUDIO_RECORD_REQUEST_CODE);
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, AUDIO_RECORD_REQUEST_CODE + 1);
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, AUDIO_RECORD_REQUEST_CODE + 2);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, linearAccelerSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        if (event.sensor == linearAccelerSensor) {
            //exclude gravity
            lAccX = event.values[0];
            lAccY = event.values[1];
            lAccZ = event.values[2];

            tvgXaxis.setText("X axis : " + String.format("%.2f", lAccX));
            tvgYaxis.setText("Y axis : " + String.format("%.2f", lAccY));
            tvgZaxis.setText("Z axis : " + String.format("%.2f", lAccZ));

        }
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }


}