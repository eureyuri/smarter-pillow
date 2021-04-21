package com.example.smartpillowapp;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Calendar;
import java.util.Iterator;
import java.util.concurrent.ExecutionException;

public class SecondFragment extends Fragment {

    private static final String LOG_TAG = "AudioRecordTest";
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;

    // Requesting permission to RECORD_AUDIO
    private boolean permissionToRecordAccepted = false;
    private String [] permissions = {Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE};

    public static LineGraphSeries<DataPoint> series;
    public static LineGraphSeries<DataPoint> series2;
    public static View viewSecondFragment;

    private static TextView sleepText;
    private Button recordButton = null;
    boolean mStartRecording = true;
    private MediaRecorder recorder = null;
    public static String root = "http://ec2-18-206-197-126.compute-1.amazonaws.com:8080";

    private class MyRunnable implements Runnable {

        private boolean doStop = false;

        public synchronized void doStop() {
            this.doStop = true;
        }

        private synchronized boolean keepRunning() {
            return this.doStop == false;
        }

        @Override
        public void run() {
            while(!Thread.interrupted()) {
//                try {
//                    Thread.sleep(100);
//                } catch (InterruptedException e) {
//                    // We've been interrupted: no more messages.
//                    Log.i("thread fail",e.toString());
//                }
                float volume = recorder.getMaxAmplitude();
                Log.i("another thread", String.valueOf(volume));
            }
        }
    }

    MyRunnable myRunnable = new MyRunnable();
    private Thread thread = new Thread(myRunnable);

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case REQUEST_RECORD_AUDIO_PERMISSION:
                permissionToRecordAccepted  = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                break;
        }
//        if (!permissionToRecordAccepted ) finish();
    }

    private void onRecord(boolean start) {
        Log.i("onRecord","onRecord called!");
        if (start) {
            startRecording();
        } else {
            stopRecording();
        }
    }

    private void startRecording() {
        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.AMR_NB);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        recorder.setOutputFile("/dev/null");

        try {
            recorder.prepare();
        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed");
        }
        recorder.start();
    }

    private void stopRecording() {
        Log.i("stopRecording","stopRecording called!");
        recorder.stop();
        recorder.release();
        recorder = null;
    }

    public static LineGraphSeries<DataPoint> getSnoreRecord(int year, int month, int day) {
        Log.i("getSnoreRecord", "getSnoreRecord called!");
        String send = "";
        if (month < 10){
            send = Integer.toString(year) + "-0" + Integer.toString(month) + "-" + Integer.toString(day) + "T00:00:00.000Z";
        }
        else
        {
            send = Integer.toString(year) + "-" + Integer.toString(month) + "-" + Integer.toString(day) + "T00:00:00.000Z";
        }

        String j_msg = "{\"datetime\": \"" + send + "\"}";
        Log.i("getSnoreRecord - i_msg", j_msg);
        NetworkAsyncTask asyncTask = new NetworkAsyncTask(root, "/snore", j_msg, "POST");

        DataPoint[] dp = null;
        try {
            String response = asyncTask.execute().get();
            try {

                JSONArray snore_data = new JSONArray(response);
                dp = new DataPoint[snore_data.length()];
                for (int i = 0; i < snore_data.length(); i++)
                {
                    JSONObject item = snore_data.getJSONObject(i);

                    dp[i] = new DataPoint( (int) (i/3600), item.getInt("loudness"));
//                    Log.i("getSnoreRecord - datetime+db", String.valueOf(item.getInt("loudness")));
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
            System.out.println(response);
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return new LineGraphSeries<DataPoint>(dp);
    }

    public static LineGraphSeries<DataPoint> getMovementRecord(int year, int month, int day) {
        String send = "";
        if (month < 10) {
            send = Integer.toString(year) + "-0" + Integer.toString(month) + "-" + Integer.toString(day) + "T00:00:00.000Z";
        } else {
            send = Integer.toString(year) + "-" + Integer.toString(month) + "-" + Integer.toString(day) + "T00:00:00.000Z";
        }

        String j_msg = "{\"datetime\": \"" + send + "\"}";
        NetworkAsyncTask asyncTask = new NetworkAsyncTask(root, "/movement", j_msg, "POST");
        DataPoint[] dp = null;
        try {
            String response = asyncTask.execute().get();
            try {
                JSONArray movement_data = new JSONArray(response);
                dp = new DataPoint[movement_data.length()];
                int fakemove;
                for (int i = 0; i < movement_data.length(); i++)
                {
//                    JSONObject item = movement_data.getJSONObject(i);
                    if (i < 5400)
                    {
                        fakemove = (int) ((Math.random() * (20 - 19)) + 19);
                    }
                    else if (i >= 5400 && i <= 9000)
                    {
                        fakemove = (int) ((Math.random() * (18 - 13)) + 13);
                    }
                    else if (i >= 10800 && i <= 14400)
                    {
                        fakemove = (int) ((Math.random() * (17 - 16)) + 16);
                    }
                    else {
                        fakemove = (int) ((Math.random() * (18 - 17)) + 17);
                    }
                    dp[i] = new DataPoint((int) (i/3600), fakemove);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return new LineGraphSeries<DataPoint>(dp);
    }

    private static void getQuality(int year, int month, int day) {
        String send = "";
        if (month < 10) {
            send = Integer.toString(year) + "-0" + Integer.toString(month) + "-" + Integer.toString(day) + "T00:00:00.000Z";
        } else {
            send = Integer.toString(year) + "-" + Integer.toString(month) + "-" + Integer.toString(day) + "T00:00:00.000Z";
        }

        String j_msg = "{\"datetime\": \"" + send + "\"}";
        NetworkAsyncTask obj = new NetworkAsyncTask(root, "/sleep_quality", j_msg, "POST");
        try {
            String response = obj.execute().get();
            try {
                JSONObject res = new JSONObject(response);
                JSONObject time = res.getJSONObject("time");
                String  sleepTime = time.getString("time");
                sleepText.setText(sleepTime);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static class DatePickerFragment extends DialogFragment
            implements DatePickerDialog.OnDateSetListener {
        int year, month, day;
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the current date as the default date in the picker
            final Calendar c = Calendar.getInstance();
            year = c.get(Calendar.YEAR);
            month = c.get(Calendar.MONTH);
            day = c.get(Calendar.DAY_OF_MONTH);

            // Create a new instance of DatePickerDialog and return it
            return new DatePickerDialog(getActivity(), this, year, month, day);
        }
        public void onDateSet(DatePicker view, int year, int month, int day) {
            Log.i("onDateSet","onDateSet called!");
            Log.i("onDateSet_body", Integer.toString(day));
            // Do something with the date chosen by the user
            SecondFragment.series = getSnoreRecord(year, month+1, day);
            SecondFragment.series2 = getMovementRecord(year, month+1, day);
            getQuality(year, month+1, day);
            GraphView graph = (GraphView) viewSecondFragment.findViewById(R.id.graph);
            GraphView graph2 = (GraphView) viewSecondFragment.findViewById(R.id.graph2);
            GridLabelRenderer gridLabel = graph.getGridLabelRenderer();
            gridLabel.setHorizontalAxisTitle("Time in hour");
            gridLabel.setVerticalAxisTitle("Magnitude in decibel");
            graph.getGridLabelRenderer().setHorizontalLabelsColor(Color.GREEN);
            graph.getGridLabelRenderer().setVerticalLabelsColor(Color.RED);
            graph.addSeries(SecondFragment.series);
            graph2.addSeries(SecondFragment.series2);
            graph2.getGridLabelRenderer().setHorizontalLabelsColor(Color.GREEN);
            graph2.getGridLabelRenderer().setVerticalLabelsColor(Color.RED);
        }
    }



    public void showDatePickerDialog() {
        Log.i("showDatePickerDialog","showDatePickerDialog called!");
        DialogFragment newFragment = new DatePickerFragment();
        newFragment.show(getChildFragmentManager(), "datePicker");
    }


    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        viewSecondFragment = inflater.inflate(R.layout.fragment_second, container, false);
        ActivityCompat.requestPermissions(getActivity(), permissions, REQUEST_RECORD_AUDIO_PERMISSION);

        recordButton = viewSecondFragment.findViewById(R.id.start);
        sleepText = viewSecondFragment.findViewById(R.id.time_slept_val);
//        graph.getViewport().setXAxisBoundsManual(true);
//        graph.getViewport().setMinX(0);
//        graph.getViewport().setMaxX(maxPoints);

        return viewSecondFragment;
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.findViewById(R.id.start).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (mStartRecording) {
                    recordButton.setText("Stop recording");
                    onRecord(mStartRecording);
                    thread.start();
                } else {
                    recordButton.setText("Start recording");
//                    myRunnable.doStop();
//                    try {
//                        thread.sleep(100);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
                    thread.interrupt();

                    onRecord(mStartRecording);
                }
                mStartRecording = !mStartRecording;
            }
        });
        view.findViewById(R.id.pick_date).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDatePickerDialog();
            }
        });

    }

    @Override
    public void onStop() {
        super.onStop();
        if (recorder != null) {
            recorder.release();
            recorder = null;
        }
    }
}
