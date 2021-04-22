package com.example.smartpillowapp;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.TimePicker;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class FirstFragment extends Fragment implements TimePickerDialog.OnTimeSetListener {
    private TextView alarmText;
    private TextView alarmTime;
    private TextView hoursSleptText;
    private TextView snoreText;
    private TextView movementText;
    private int sleepQuality = 0;
    private boolean isWakeup = false;

    private String root = "http://ec2-18-206-197-126.compute-1.amazonaws.com:8080";

    PieChart pieChart;
    PieData pieData;
    List<PieEntry> pieEntryList;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_first, container, false);
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        alarmText = view.findViewById(R.id.alarm_text);
        alarmTime = view.findViewById(R.id.alarm);
        hoursSleptText = view.findViewById(R.id.hours_slept);
        movementText = view.findViewById(R.id.movement);
        snoreText = view.findViewById(R.id.snore);

        Button buttonSetAlarm = view.findViewById(R.id.set_alarm_brn);
        buttonSetAlarm.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View view) {
               DialogFragment timePicker = new TimePickerFragment();
               timePicker.show(getFragmentManager(), "time picker");
           }
        });

        Button cancelAlarm = view.findViewById(R.id.cancel_alarm_btn);
        cancelAlarm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cancelAlarm();
            }
        });

        getOverallQuality();

        pieEntryList = new ArrayList<>();
        // Pie chart
        pieChart = view.findViewById(R.id.pieChart);
        pieChart.setUsePercentValues(true);
        pieChart.getLegend().setEnabled(false);
        pieChart.setDrawEntryLabels(false);
        pieChart.setTouchEnabled(false);

        // Text inside pie chart
        pieChart.setCenterText(Integer.toString(sleepQuality) + '%');
        pieChart.setCenterTextSize(40);
        pieChart.setCenterTextRadiusPercent(90);
        pieChart.setCenterTextColor(Color.rgb(48, 108, 189));

        // pie chart hole
        pieChart.setHoleColor(Color.argb(0,0,0,0));
        pieChart.setHoleRadius(70);

        pieEntryList.add(new PieEntry(sleepQuality,"slept"));
        pieEntryList.add(new PieEntry(100 - sleepQuality,"not slept"));
        PieDataSet pieDataSet = new PieDataSet(pieEntryList,"quality");
        pieDataSet.setColors(new int[]{Color.rgb(48, 108, 189), Color.rgb(0, 0, 0)});
        pieData = new PieData(pieDataSet);
        pieChart.setData(pieData);
        pieChart.invalidate();
    }

    @Override
    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, hourOfDay);
        c.set(Calendar.MINUTE, minute);
        c.set(Calendar.SECOND, 0);

        updateTimeText(hourOfDay, minute);
        setSleepHours(hourOfDay, minute);
        startAlarm(c);
        startWeightSensor();
    }

    private void updateTimeText(int hour, int min) {
        String t = "Alarm set";
        alarmText.setText(t);

        String minString = String.valueOf(min);
        if (min < 10) minString = "0" + minString;
        alarmTime.setText(hour + ":" + minString);
    }

    private void setSleepHours(int hour, int min) {
//        Get current time
        Calendar cal = Calendar.getInstance();
        long current = cal.getTime().getTime();

//      Get alarm time
        Calendar cal2 = Calendar.getInstance();
        int todayYear = Calendar.getInstance().get(Calendar.YEAR) ;
        int todayMonth = Calendar.getInstance().get(Calendar.MONTH);
        int todayDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
        cal2.set(todayYear, todayMonth, todayDay, hour, min);
        long alarm = cal2.getTime().getTime();

//      Get hours slept from diff
        double millis = Math.abs(alarm - current);
        double hours = millis / (double)(1000 * 60 * 60);
        DecimalFormat df = new DecimalFormat("#.#");
        String slept = df.format(hours);

//        Send to backend
        String dayFormatted = Integer.toString(todayYear);
        todayMonth++;
        if (todayMonth < 10) dayFormatted += "-0" + todayMonth;
        else dayFormatted += "-" + todayMonth;

        if (todayDay < 10) dayFormatted += "0" + todayDay;
        else dayFormatted += "-" + todayDay;

        dayFormatted += "T00:00:00.000Z";

        String j_msg = "{\"datetime\": \"" + dayFormatted + "\", \"time\": " + slept + "}";
        NetworkAsyncTask obj = new NetworkAsyncTask(root, "/insert_time", j_msg, "POST");
        try {
            obj.execute().get();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void startWeightSensor() {
        String j_msg = "{\"state\": true}";
        NetworkAsyncTask obj = new NetworkAsyncTask(root, "/set_weight_sensor", j_msg, "POST");
        try {
            obj.execute().get();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void stopWeightSensor() {
        String j_msg = "{\"state\": false}";
        NetworkAsyncTask obj = new NetworkAsyncTask(root, "/set_weight_sensor", j_msg, "POST");
        try {
            obj.execute().get();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void startAlarm(Calendar c) {
        AlarmManager alarmManager = (AlarmManager) getActivity().getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(getActivity(), AlertReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(getActivity(), 1, intent, 0);
        if (c.before(Calendar.getInstance())) {
            c.add(Calendar.DATE, 1);
        }
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, c.getTimeInMillis(), pendingIntent);
    }

    private void cancelAlarm() {
        AlarmManager alarmManager = (AlarmManager) getActivity().getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(getActivity(), AlertReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(getActivity(), 1, intent, 0);
        alarmManager.cancel(pendingIntent);
        stopWeightSensor();
        deflatePillow();
        alarmText.setText("Alarm not set");
    }

    public void wakeup() {
        String j_msg = "{\"pillow\": \"lower\", \"state\": true}";
        String j_msg2 = "{\"pillow\": \"upper\", \"state\": true}";
        NetworkAsyncTask obj = new NetworkAsyncTask(root, "/set_pillow_height", j_msg, "POST");
        NetworkAsyncTask obj2 = new NetworkAsyncTask(root, "/set_pillow_height", j_msg2, "POST");
        try {
            obj.execute().get();
            obj2.execute().get();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void deflatePillow() {
        String j_msg = "{\"pillow\": \"lower\", \"state\": false}";
        String j_msg2 = "{\"pillow\": \"upper\", \"state\": false}";
        NetworkAsyncTask obj = new NetworkAsyncTask(root, "/set_pillow_height", j_msg, "POST");
        NetworkAsyncTask obj2 = new NetworkAsyncTask(root, "/set_pillow_height", j_msg2, "POST");
        try {
            obj.execute().get();
            obj2.execute().get();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void getOverallQuality() {
//        Get yesterday's date
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);
        Date yesterday = cal.getTime();
        String yesterdayFormatted = (String) DateFormat.format("yyyy-MM-ddT00:00:00.000Z", yesterday);

        String j_msg = "{\"datetime\": \"" + yesterdayFormatted + "\"}";
        NetworkAsyncTask obj = new NetworkAsyncTask(root, "/sleep_quality", j_msg, "POST");
        try {
            String response = obj.execute().get();
            try {
                JSONObject res = new JSONObject(response);
                System.out.println(res);

//                JSONObject time = res.getJSONObject("time");
//                String  sleepTime = time.getString("time");
                double time = res.getDouble("time");
                String  sleepTime = String.valueOf(time);

                String movement = res.getString("movement");
                String snore = res.getString("snore");
                sleepQuality = Math.round(Float.parseFloat(res.getString("sleep_quality")) * 100);

                hoursSleptText.setText(sleepTime);
                movementText.setText(String.valueOf(Math.round(Float.parseFloat(movement) * 100)) + '%');
                snoreText.setText(String.valueOf(Math.round(Float.parseFloat(snore) * 100)) + '%');
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}