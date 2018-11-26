package com.example.android.pma_inteligentna_domacnost;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.view.View;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class Teplota extends AppCompatActivity {

    public SwipeRefreshLayout swipeRefreshLayout;
    public static final String KEY_ACTIVITY_NAME = "KEY_ACTIVITY_NAME";
    public static final String SHARED_PREFS = "sharedPrefs";
    public static final String TEXT = "text";
    public static final String TEXT2 = "text2";

    private TextView mTextViewResult;

    private String hodnota_teplota = "";
    private String hodnota_vlhkost = "";

    public String tag_teplota = "";
    public String tag_vlhkost = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teplota);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);

        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swiperefresh);
        swipeRefreshLayout.setColorSchemeColors ( getResources().getColor(R.color.swipe_1),
                getResources().getColor(R.color.swipe_2), getResources().getColor(R.color.swipe_3));

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                swipeRefreshLayout.setRefreshing(true);
                (new Handler()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        swipeRefreshLayout.setRefreshing(false);
                            submitOrderTep();

                    }
                }, 2000);

            }
        });

        submitOrderTep();
    }

    public void submitOrder(View view) {
        Intent ganesh = new Intent(this, Svetlo.class);
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

    public void submitOrderTep() {

        loadData();
        updateViews();

        mTextViewResult = findViewById(R.id.result);

        OkHttpClient client = new OkHttpClient();

        String url = "http://iot-python.eu-de.mybluemix.net/api/dht";

        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                mTextViewResult.setText("Teplota:\n" + tag_teplota + " °C\n\nVlhkosť:\n" + tag_vlhkost + " %");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String teplota1 = "";
                    String vlhkost1 = "";
                    try {
                        JSONObject t = new JSONObject(response.body().string());
                        teplota1 = t.getString("teplota");
                        vlhkost1 = t.getString("vlhkost");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    final String teplota = teplota1;
                    final String vlhkost = vlhkost1;

                    Teplota.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mTextViewResult.setText("Teplota:\n" + teplota + " °C\n\nVlhkosť:\n" + vlhkost + " %");
                            tag_teplota = teplota;
                            tag_vlhkost = vlhkost;
                            saveData();
                        }
                    });
                }
            }
        });
    }

    public void saveData() {
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putString(TEXT, tag_teplota);
        editor.putString(TEXT2, tag_vlhkost);
        editor.apply();
    }

    public void loadData() {
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        hodnota_teplota = sharedPreferences.getString(TEXT, "");
        hodnota_vlhkost = sharedPreferences.getString(TEXT2, "");
    }

    public void updateViews() {
        tag_teplota = hodnota_teplota;
        tag_vlhkost = hodnota_vlhkost;
    }
}


