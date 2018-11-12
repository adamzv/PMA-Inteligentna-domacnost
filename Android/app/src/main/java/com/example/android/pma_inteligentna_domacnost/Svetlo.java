package com.example.android.pma_inteligentna_domacnost;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;

import com.google.gson.JsonObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Svetlo extends AppCompatActivity {

    private String cislo_miestnosti;
    private String status;
    int ID;
    private TextView mTextViewResult;
    public static final String SHARED_PREFS = "sharedPrefs";

    // Switch 1
    private Switch switch1;
    public static final String SWITCH1 = "switch1";
    private boolean switchOnOff1;

    // Switch 2
    private Switch switch2;
    public static final String SWITCH2 = "switch2";
    private boolean switchOnOff2;

    // Switch 3
    private Switch switch3;
    public static final String SWITCH3 = "switch3";
    private boolean switchOnOff3;

    // Switch 4
    private Switch switch4;
    public static final String SWITCH4 = "switch4";
    private boolean switchOnOff4;

    // Switch 5
    private Switch switch5;
    public static final String SWITCH5 = "switch5";
    private boolean switchOnOff5;

    // Switch 6
    private Switch switch6;
    public static final String SWITCH6 = "switch6";
    private boolean switchOnOff6;

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
        loadData();
        updateViews();
    }

    public void submitOrder2(View view) {
        Intent ganesh = new Intent(this, Teplota.class);
        startActivity(ganesh);
    }

    public void submitOrder3(View view) {
        Intent ganesh = new Intent(this, Alarm.class);
        startActivity(ganesh);
    }

    public void submitOrder(View view){
        loadData();
        updateViews();

        ID = view.getId();  // Podľa ID rozhodne ktorý POST poslať
        if (ID == R.id.s1) {
            cislo_miestnosti = "1";
            if (switchOnOff1) status = "off";
            else status = "on";
        } else if (ID == R.id.s2) {
            cislo_miestnosti = "2";
            if (switchOnOff2) status = "off";
            else status = "on";
        } else if (ID == R.id.s3) {
            cislo_miestnosti = "3";
            if (switchOnOff3) status = "off";
            else status = "on";
        } else if (ID == R.id.s4) {
            cislo_miestnosti = "4";
            if (switchOnOff4) status = "off";
            else status = "on";
        } else if (ID == R.id.s5) {
            cislo_miestnosti = "5";
            if (switchOnOff5) status = "off";
            else status = "on";
        } else if (ID == R.id.s6) {
            cislo_miestnosti = "6";
            if (switchOnOff6) status = "off";
            else status = "on";
        }

        OkHttpClient client = new OkHttpClient();

        String url = "http://iot-python.eu-de.mybluemix.net/api/alarm";

        final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        JsonObject json = new JsonObject();
        json.addProperty("senzor", "led");
        json.addProperty("miestnost",cislo_miestnosti);
        json.addProperty("status",status);

        RequestBody body = RequestBody.create(JSON,json.toString());

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

                                    // Ak bola použiadavka úspešná ... zmení switch a uloží udaje

                                    mTextViewResult.setText(myResponse+" "+cislo_miestnosti+" "+status);
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
                                    saveData();
                                    loadData();
                                    updateViews();
                                }
                            });
                        }
                    }
                });


    }

    public void saveData() {
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putBoolean(SWITCH1, switch1.isChecked());
        editor.putBoolean(SWITCH2, switch2.isChecked());
        editor.putBoolean(SWITCH3, switch3.isChecked());
        editor.putBoolean(SWITCH4, switch4.isChecked());
        editor.putBoolean(SWITCH5, switch5.isChecked());
        editor.putBoolean(SWITCH6, switch6.isChecked());
        editor.apply();

    }

    public void loadData() {
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        switchOnOff1 = sharedPreferences.getBoolean(SWITCH1, false);
        switchOnOff2 = sharedPreferences.getBoolean(SWITCH2, false);
        switchOnOff3 = sharedPreferences.getBoolean(SWITCH3, false);
        switchOnOff4 = sharedPreferences.getBoolean(SWITCH4, false);
        switchOnOff5 = sharedPreferences.getBoolean(SWITCH5, false);
        switchOnOff6 = sharedPreferences.getBoolean(SWITCH6, false);
    }

    public void updateViews() {
        switch1.setChecked(switchOnOff1);
        switch2.setChecked(switchOnOff2);
        switch3.setChecked(switchOnOff3);
        switch4.setChecked(switchOnOff4);
        switch5.setChecked(switchOnOff5);
        switch6.setChecked(switchOnOff6);
    }
}