package com.example.smartwatch_esp8266;

import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;

// https://developer.android.com/guide
//https://developer.android.com/training/basics/firstapp/building-ui

/*
    Controls the smartwatch through both buttons and voice command.
    Voice commands are: turn on, turn off, time, close time, and any other messages for simple message
 */
public class FirstFragment extends Fragment {

    private static EditText text;
    private boolean btn1_toggle = false;
    private boolean btn2_toggle = false;
    private String root = "https://5b96aa4a7859.ngrok.io";

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        View view = inflater.inflate(R.layout.fragment_first, container, false);

        Button btn1 = view.findViewById(R.id.turn_display);
        Button btn2 = view.findViewById(R.id.display_time);
        Button btn3 = view.findViewById(R.id.display_message);
        Button btn4 = view.findViewById(R.id.tweet);

        TextView tv1 = view.findViewById(R.id.display_btn_err);
        TextView tv2 = view.findViewById(R.id.time_btn_err);
        TextView tv3 = view.findViewById(R.id.msg_btn_err);
        TextView tv4 = view.findViewById(R.id.tweet_btn_err);

        btn1.setOnClickListener(v -> {
            Log.i("onClick", "btn1 clicked");
            String j_msg;
            if (btn1_toggle = !btn1_toggle) {
                j_msg = "{\"state\": true}";
            } else {
                j_msg = "{\"state\": false}";
            }

            NetworkAsyncTask obj = new NetworkAsyncTask(root, "/turn_display", j_msg);
            try {
                String response = obj.execute().get();
                String[] tokens = response.split(":");
                String token = tokens[2].trim().substring(1, tokens[2].length()-3);
                if (token.equals("")) {
                    tv1.setText(R.string.success);
                } else {
                    tv1.setText(token);
                }
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        btn2.setOnClickListener(v -> {
            Log.i("onClick", "btn2 clicked");
            String j_msg;
            if (btn2_toggle = !btn2_toggle) {
                j_msg = "{\"state\": true}";
            } else {
                j_msg = "{\"state\": false}";
            }

            NetworkAsyncTask obj = new NetworkAsyncTask(root, "/display_time", j_msg);
            try {
                String response = obj.execute().get();
                String[] tokens = response.split(":");
                tv2.setText(tokens[2].trim().substring(1, tokens[2].length()-3));
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        // button to send the spoken message.
        btn3.setOnClickListener(v -> {
            Log.i("onClick", "btn3 clicked");
            if (text.getText().toString().equals(""))
            {
                Log.i("EditText", "Empty EditText" );
                tv3.setText(R.string.empty_text_field);
            }
            else {
                String j_msg;
                Log.i("EditText", "success" );

                NetworkAsyncTask obj;
                if (text.getText().toString().equals("turn on")) {
                    j_msg = "{\"state\": true}";
                    obj = new NetworkAsyncTask(root, "/turn_display", j_msg);
                } else if (text.getText().toString().equals("turn off")) {
                    j_msg = "{\"state\": false}";
                    obj = new NetworkAsyncTask(root, "/turn_display", j_msg);
                } else if (text.getText().toString().equals("time")) {
                    j_msg = "{\"state\": true}";
                    obj = new NetworkAsyncTask(root, "/display_time", j_msg);
                } else if (text.getText().toString().equals("close time")) {
                    j_msg = "{\"state\": false}";
                    obj = new NetworkAsyncTask(root, "/display_time", j_msg);
                } else if(text.getText().toString().equals("weather")){
                    j_msg = "{\"state\": true}";
                    obj = new NetworkAsyncTask(root, "/display_weather", j_msg);
                } else if(text.getText().toString().equals("close weather")){
                    j_msg = "{\"state\": false}";
                    obj = new NetworkAsyncTask(root, "/display_weather", j_msg);
                }else {
                    j_msg = String.format("{\"state\": true, \"message\": \"%s\"}", text.getText().toString());
                    obj =  new NetworkAsyncTask(root, "/display_message", j_msg);
                }

                try {
                    String stringMess = obj.execute().get();
                    String[] responses = stringMess.split(":");
                    tv3.setText(responses[2].trim().substring(1,responses[2].length()-3));
                } catch (ExecutionException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        });

        btn4.setOnClickListener(v -> {
            Log.i("onClick", "btn4 clicked");

            if (text.getText().toString().equals(""))
            {
                Log.i("EditText", "Empty EditText" );
                tv4.setText(R.string.empty_text_field);
            }
            else
            {
                String j_msg;
                Log.i("EditText", "success" );

                j_msg = String.format("{\"state\": true, \"message\": \"%s\"}", text.getText().toString());
                NetworkAsyncTask obj = new NetworkAsyncTask(root, "/tweet", j_msg);
                try {
                    String response = obj.execute().get();
                    String[] tokens = response.split(":");
                    tv4.setText(tokens[2].trim().substring(1, tokens[2].length()-3));
                } catch (ExecutionException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        });

        // Inflate the layout for this fragment
        return view;
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        text = getView().findViewById(R.id.editText);
    }

    public static void setText(String newText) {
        text.setText(newText);
    }
}

class NetworkAsyncTask extends AsyncTask<Void, Void, String> {
    private String root;
    private String path;
    private String json_msg;
    private String res;

    // constructor
    public NetworkAsyncTask(String r, String p, String j) {
        root = r;
        path = p;
        json_msg = j;
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected String doInBackground(Void... voids) {
        try {
            URL url = new URL(root + path);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json; utf-8");
            con.setRequestProperty("Accept", "application/json");
            con.setDoOutput(true);
            String jsonInputString = json_msg;
            OutputStream os = con.getOutputStream();
            byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
            BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder response = new StringBuilder();
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
            res = response.toString();
            con.disconnect();
            Log.i("doInBackground: success","SUCCESS");
        } catch (IOException e) {
            Log.i("doInBackground: error", "ERROR");
            e.printStackTrace();
        }
        return res;
    }
}