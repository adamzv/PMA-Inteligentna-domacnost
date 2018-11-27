package com.example.android.pma_inteligentna_domacnost;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

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

public class Alarm extends AppCompatActivity {

    public static final String KEY_ACTIVITY_NAME = "KEY_ACTIVITY_NAME";
    public SwipeRefreshLayout swipeRefreshLayout;
    private ImageView imgView;
    private TextView mTextViewResult;
    public int tag;
    String sprava;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);

        imgView = (ImageView) findViewById(R.id.alarm);
        mTextViewResult = findViewById(R.id.textOdpoved);

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
                        getAlarm();

                    }
                }, 2000);
            }
        });

        submitOrder(imgView);
        getAlarm();

    }

    public void submitOrder3(View view) {
        Intent ganesh = new Intent(this, Svetlo.class);
        startActivity(ganesh);
    }

    public void submitOrder2(View view) {
        Intent ganesh = new Intent(this, Teplota.class);
        startActivity(ganesh);
    }

    public void submitOrder4(View view) {
        Intent ganesh = new Intent(this, Bezpecnost.class);
        ganesh.putExtra(KEY_ACTIVITY_NAME, "b");
        startActivity(ganesh);
    }

    public void getAlarm() {

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

                    try {
                        JSONArray pole = new JSONArray(response.body().string());
                        JSONObject reader = pole.getJSONObject(7);

                        status = reader.getString("status");

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    final String statusAlarm = status;

                    Alarm.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (statusAlarm.equals("off")) {
                                tag = 1;
                                mTextViewResult.setText("Alarm je vypnutý!");
                            } else {
                                tag = 2;
                                mTextViewResult.setText("Alarm je zapnutý");
                            }

                            imgView.setTag(tag);
                            if (imgView.getTag().equals(1)) {
                                imgView.setImageResource(R.drawable.alarm_off);
                            } else {
                                imgView.setImageResource(R.drawable.alarm_on);
                            }
                        }
                    });
                }
            }
        });
    }

    public void submitOrder(View view) {

        imgView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                final OkHttpClient client = new OkHttpClient();

                String url = "http://iot-python.eu-de.mybluemix.net/api/alarm";

                String on_off;
                if (tag == 2) {
                    on_off = "off";
                    sprava = "Alarm je vypnutý!";
                } else {
                    on_off = "on";
                    sprava = "Alarm je zapnutý";
                }

                final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
                JsonObject json = new JsonObject();
                json.addProperty("senzor", "pir");
                json.addProperty("status", on_off);

                final RequestBody body = RequestBody.create(JSON, json.toString());

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

                            Alarm.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mTextViewResult.setText(sprava);

                                    if (tag == 1) tag = 2;
                                    else tag = 1;

                                    imgView.setTag(tag);

                                    if (imgView.getTag().equals(1)) {
                                        imgView.setImageResource(R.drawable.alarm_off);
                                    } else {
                                        imgView.setImageResource(R.drawable.alarm_on);
                                    }
                                }
                            });
                        }
                    }
                });
            }
        });
    }
}
