package com.example.datacollection;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class pedometer extends Activity implements SensorEventListener {
    Boolean running;
    private SensorManager sensorManager;
    private TextView steps;
    private TextView timer;
    private EditText stride;
    private EditText filename;
    private Button startButton;
    private Button stopButton;
    private FileWriter writer;
    int stepCounter;
    long startTimeMilli;
    final String TAG = "SensorLog";

    long MillisecondTime, StartTime, TimeBuff, UpdateTime = 0L ;
    Handler handler;
    int Seconds, Minutes, MilliSeconds ;


    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onCreate(Bundle savedInstanceState) {
        PackageManager pm = getPackageManager();

        super.onCreate(savedInstanceState);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        handler = new Handler() ;
        setContentView(R.layout.activity_main);//Layout loaded from activity_main.html
        timer = findViewById(R.id.timer);
        steps = findViewById(R.id.steps);
        steps.setText("0");
        timer.setText("00:00:00");
        filename = findViewById(R.id.file_name);
        stride = findViewById(R.id.stride_length);

        startButton = findViewById(R.id.start);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onStartClicked();
            }
        });
        stopButton = findViewById(R.id.stop);
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onStopClicked();
            }
        });
        running=false;
    }

    public Runnable runnable = new Runnable() {

        public void run() {

            MillisecondTime = SystemClock.uptimeMillis() - StartTime;

            UpdateTime = TimeBuff + MillisecondTime;

            Seconds = (int) (UpdateTime / 1000);

            Minutes = Seconds / 60;

            Seconds = Seconds % 60;

            MilliSeconds = (int) (UpdateTime % 1000);

            timer.setText("" + Minutes + ":"
                    + String.format("%02d", Seconds) + ":"
                    + String.format("%03d", MilliSeconds));

            handler.postDelayed(this, 0);
        }

    };


    protected void onStartClicked() {
        startTimeMilli = System.currentTimeMillis();
        if (!running){
            sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR),
                    SensorManager.SENSOR_DELAY_UI);
            sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE),
                    SensorManager.SENSOR_DELAY_UI);
            sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                    SensorManager.SENSOR_DELAY_UI);
            sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
                    SensorManager.SENSOR_DELAY_UI);

            stepCounter = 0;
            StartTime = SystemClock.uptimeMillis();
            TimeUnit.MILLISECONDS.toSeconds(StartTime);
            Log.d(TAG, "milli: " + String.valueOf(StartTime));
            Log.d(TAG, "start time: " + String.valueOf(TimeUnit.MILLISECONDS.toSeconds(StartTime)));
            handler.postDelayed(runnable, 0);
            running = true;
        }
    }

    protected void onStopClicked() {
        if(running){
            sensorManager.unregisterListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR));
            sensorManager.unregisterListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE));
            sensorManager.unregisterListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));
            sensorManager.unregisterListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE));
            String[] sensors = {Sensor.STRING_TYPE_ACCELEROMETER, Sensor.STRING_TYPE_PRESSURE, Sensor.STRING_TYPE_STEP_DETECTOR, Sensor.STRING_TYPE_GYROSCOPE};
            try {
                for(int i=0;i<sensors.length;i++){
                    writer = new FileWriter(new File(this.getExternalFilesDir(null).getAbsolutePath(), filename.getText().toString() + "-" + sensors[i] + ".txt"), true);
                    writer.write("\n\n");
                    writer.close();
                }
                writer = new FileWriter(new File(this.getExternalFilesDir(null).getAbsolutePath(),  filename.getText().toString() + "-android.sensor.data.txt"), true);
                writer.write(stride.getText().toString() + "\n");
                writer.write(timer.getText().toString() + "\n\n");
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            TimeBuff += MillisecondTime;
//        MillisecondTime = 0;
            MillisecondTime = 0L ;
            StartTime = 0L ;
            TimeBuff = 0L ;
            UpdateTime = 0L ;
            Seconds = 0 ;
            Minutes = 0 ;
            MilliSeconds = 0 ;
            stepCounter = 0;

            steps.setText("0");
            timer.setText("00:00:00");
            handler.removeCallbacks(runnable);
            running=false;
        }

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float[] values = event.values;
//        Log.d(TAG, "Writing to " + this.getExternalFilesDir(null).getAbsolutePath());
        try {
            writer = new FileWriter(new File(this.getExternalFilesDir(null).getAbsolutePath(), filename.getText().toString() + "-" + event.sensor.getStringType() + ".txt"), true);
            switch(event.sensor.getType()){
                case Sensor.TYPE_STEP_DETECTOR:
                case Sensor.TYPE_PRESSURE:
                    writer.write((TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()-startTimeMilli)+1) + ", " + event.values[0] + "\n");
                    break;
                case Sensor.TYPE_ACCELEROMETER:
                case Sensor.TYPE_GYROSCOPE:
                    writer.write((TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()-startTimeMilli)+1) + ", " + event.values[0] + ", " + event.values[1] + ", " + event.values[2] + "\n");
                    break;
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (event.sensor.getType() == Sensor.TYPE_STEP_DETECTOR) {
            stepCounter+=event.values[0];
            steps.setText(String.valueOf(stepCounter));
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
