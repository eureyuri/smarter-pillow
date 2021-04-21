package com.example.smartpillowapp;

import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

// https://developer.android.com/guide
//https://developer.android.com/training/basics/firstapp/building-ui

class NetworkAsyncTask extends AsyncTask<Void, Void, String> {
    private String root;
    private String path;
    private String json_msg;
    private String res;
    private String method;

    // constructor
    public NetworkAsyncTask(String r, String p, String j, String method) {
        root = r;
        path = p;
        json_msg = j;
        this.method = method;
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected String doInBackground(Void... voids) {
        try {
            URL url = new URL(root + path);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod(this.method);
            con.setRequestProperty("Content-Type", "application/json; utf-8");
            con.setRequestProperty("Accept", "application/json");

            if (this.method == "POST") con.setDoOutput(true);
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