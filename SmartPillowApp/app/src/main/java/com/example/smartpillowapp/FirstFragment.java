package com.example.smartpillowapp;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.TimePicker;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static androidx.core.content.ContextCompat.getSystemService;

public class FirstFragment extends Fragment implements TimePickerDialog.OnTimeSetListener {
    private TextView alarmText;
    private TextView alarmTime;

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

        pieEntryList = new ArrayList<>();
        // Pie chart
        pieChart = view.findViewById(R.id.pieChart);
        pieChart.setUsePercentValues(true);
        pieChart.getLegend().setEnabled(false);
        pieChart.setDrawEntryLabels(false);
        pieChart.setTouchEnabled(false);

        // Text inside pie chart
        pieChart.setCenterText(Integer.toString(95) + '%');
        pieChart.setCenterTextSize(40);
        pieChart.setCenterTextRadiusPercent(90);
        pieChart.setCenterTextColor(Color.rgb(48, 108, 189));

        // pie chart hole
        pieChart.setHoleColor(Color.argb(0,0,0,0));
        pieChart.setHoleRadius(70);

        pieEntryList.add(new PieEntry(95,"slept"));
        pieEntryList.add(new PieEntry(5,"not slept"));
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
        startAlarm(c);
    }

    private void updateTimeText(int hour, int min) {
        String t = "Alarm set";
        alarmText.setText(t);
        alarmTime.setText(hour + ":" + min);
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
        alarmText.setText("Alarm not set");
    }
}