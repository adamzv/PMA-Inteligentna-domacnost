package com.example.android.pma_inteligentna_domacnost;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;

import com.google.gson.JsonObject;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Svetlo extends AppCompatActivity {

    public static final String KEY_ACTIVITY_NAME = "KEY_ACTIVITY_NAME";
    public SwipeRefreshLayout swipeRefreshLayout;
    public int ID;

    private String cislo_miestnosti;
    private String status;
    private TextView mTextViewResult;

    // Switch
    private Switch switch1;
    private Switch switch2;
    private Switch switch3;
    private Switch switch4;
    private Switch switch5;
    private Switch switch6;

    @SuppressLint("ResourceAsColor")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_svetlo);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);

        mTextViewResult = findViewById(R.id.result);
        switch1 = findViewById(R.id.s1);
        switch2 = findViewById(R.id.s2);
        switch3 = findViewById(R.id.s3);
        switch4 = findViewById(R.id.s4);
        switch5 = findViewById(R.id.s5);
        switch6 = findViewById(R.id.s6);

        if (checkConnection()) {
            getSvetlo();
        } else {
            switch1.setClickable(false);
            switch2.setClickable(false);
            switch3.setClickable(false);
            switch4.setClickable(false);
            switch5.setClickable(false);
            switch6.setClickable(false);
        }

        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swiperefresh);
        swipeRefreshLayout.setColorSchemeColors(getResources().getColor(R.color.swipe_1),
                getResources().getColor(R.color.swipe_2), getResources().getColor(R.color.swipe_3));

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                swipeRefreshLayout.setRefreshing(true);
                (new Handler()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        swipeRefreshLayout.setRefreshing(false);

                        if (checkConnection()) {
                            switch1.setClickable(true);
                            switch2.setClickable(true);
                            switch3.setClickable(true);
                            switch4.setClickable(true);
                            switch5.setClickable(true);
                            switch6.setClickable(true);
                            getSvetlo();
                        }
                    }
                }, 2000);

            }
        });

    }

    public boolean checkConnection() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState() == NetworkInfo.State.CONNECTED ||
                connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState() == NetworkInfo.State.CONNECTED) {
            return true;
        } else
            return false;
    }

    public void submitOrder2(View view) {
        Intent ganesh = new Intent(this, Teplota.class);
        startActivity(ganesh);
    }

    public void submitOrder3(View view) {
        Intent ganesh = new Intent(this, Bezpecnost.class);
        ganesh.putExtra(KEY_ACTIVITY_NAME, "a");
        startActivity(ganesh);
    }

    public void submitOrder4(View view) {
        Intent ganesh = new Intent(this, Bezpecnost.class);
        ganesh.putExtra(KEY_ACTIVITY_NAME, "b");
        startActivity(ganesh);
    }

    public void getSvetlo() {

        OkHttpClient client = new OkHttpClient();

        String url = "http://iot-python.eu-de.mybluemix.net/api/status";

        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {

                    String status = "";
                    String status2 = "";
                    String status3 = "";
                    String status4 = "";
                    String status5 = "";
                    String status6 = "";

                    try {
                        JSONArray pole = new JSONArray(response.body().string());
                        JSONObject reader = pole.getJSONObject(1);
                        JSONObject reader2 = pole.getJSONObject(2);
                        JSONObject reader3 = pole.getJSONObject(3);
                        JSONObject reader4 = pole.getJSONObject(4);
                        JSONObject reader5 = pole.getJSONObject(5);
                        JSONObject reader6 = pole.getJSONObject(6);

                        status = reader.getString("status");
                        status2 = reader2.getString("status");
                        status3 = reader3.getString("status");
                        status4 = reader4.getString("status");
                        status5 = reader5.getString("status");
                        status6 = reader6.getString("status");

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    final String status_1 = status;
                    final String status_2 = status2;
                    final String status_3 = status3;
                    final String status_4 = status4;
                    final String status_5 = status5;
                    final String status_6 = status6;

                    Svetlo.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            if (status_1.equals("off")) switch1.setChecked(false);
                            else switch1.setChecked(true);

                            if (status_2.equals("off")) switch2.setChecked(false);
                            else switch2.setChecked(true);

                            if (status_3.equals("off")) switch3.setChecked(false);
                            else switch3.setChecked(true);

                            if (status_4.equals("off")) switch4.setChecked(false);
                            else switch4.setChecked(true);

                            if (status_5.equals("off")) switch5.setChecked(false);
                            else switch5.setChecked(true);

                            if (status_6.equals("off")) switch6.setChecked(false);
                            else switch6.setChecked(true);
                        }
                    });
                }
            }
        });
    }

    public void submitOrder(View view) {

        ID = view.getId();  // Podľa ID rozhodne ktorý POST poslať

        if (checkConnection()) {

            if (ID == R.id.s1) {
                cislo_miestnosti = "1";
                if (switch1.isChecked()) status = "on";
                else status = "off";
            } else if (ID == R.id.s2) {
                cislo_miestnosti = "2";
                if (switch2.isChecked()) status = "on";
                else status = "off";
            } else if (ID == R.id.s3) {
                cislo_miestnosti = "3";
                if (switch3.isChecked()) status = "on";
                else status = "off";
            } else if (ID == R.id.s4) {
                cislo_miestnosti = "4";
                if (switch4.isChecked()) status = "on";
                else status = "off";
            } else if (ID == R.id.s5) {
                cislo_miestnosti = "5";
                if (switch5.isChecked()) status = "on";
                else status = "off";
            } else if (ID == R.id.s6) {
                cislo_miestnosti = "6";
                if (switch6.isChecked()) status = "on";
                else status = "off";
            }

            OkHttpClient client = new OkHttpClient();

            String url = "http://iot-python.eu-de.mybluemix.net/api/svetlo";

            final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
            JsonObject json = new JsonObject();
            json.addProperty("senzor", "led");
            json.addProperty("miestnost", cislo_miestnosti);
            json.addProperty("status", status);

            RequestBody body = RequestBody.create(JSON, json.toString());

            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {

                @Override
                public void onFailure(Call call, IOException e) {
                    e.printStackTrace();
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        final String myResponse = response.body().string();

                        Svetlo.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                mTextViewResult.setText(myResponse + " " + cislo_miestnosti + " " + status);
                                if (ID == R.id.s1) {
                                    if (switch1.isChecked()) switch1.setChecked(true);
                                    else switch1.setChecked(false);
                                } else if (ID == R.id.s2) {
                                    if (switch2.isChecked()) switch2.setChecked(true);
                                    else switch2.setChecked(false);
                                } else if (ID == R.id.s3) {
                                    if (switch3.isChecked()) switch3.setChecked(true);
                                    else switch3.setChecked(false);
                                } else if (ID == R.id.s4) {
                                    if (switch4.isChecked()) switch4.setChecked(true);
                                    else switch4.setChecked(false);
                                } else if (ID == R.id.s5) {
                                    if (switch5.isChecked()) switch5.setChecked(true);
                                    else switch5.setChecked(false);
                                } else if (ID == R.id.s6) {
                                    if (switch6.isChecked()) switch6.setChecked(true);
                                    else switch6.setChecked(false);
                                }
                            }
                        });
                    }
                }
            });

        } else {
            if (ID == R.id.s1) {
                if (switch1.isChecked()) switch1.setChecked(false);
                else switch1.setChecked(true);
            } else if (ID == R.id.s2) {
                if (switch2.isChecked()) switch2.setChecked(false);
                else switch2.setChecked(true);
            } else if (ID == R.id.s3) {
                if (switch3.isChecked()) switch3.setChecked(false);
                else switch3.setChecked(true);
            } else if (ID == R.id.s4) {
                if (switch4.isChecked()) switch4.setChecked(false);
                else switch4.setChecked(true);
            } else if (ID == R.id.s5) {
                if (switch5.isChecked()) switch5.setChecked(false);
                else switch5.setChecked(true);
            } else if (ID == R.id.s6) {
                if (switch6.isChecked()) switch6.setChecked(false);
                else switch6.setChecked(true);
            }

            switch1.setClickable(false);
            switch2.setClickable(false);
            switch3.setClickable(false);
            switch4.setClickable(false);
            switch5.setClickable(false);
            switch6.setClickable(false);
        }
    }


}