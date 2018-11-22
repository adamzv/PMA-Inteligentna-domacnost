package com.example.android.pma_inteligentna_domacnost;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
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

    private TextView mTextViewResult;
    public static final String KEY_ACTIVITY_NAME = "KEY_ACTIVITY_NAME";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teplota);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
        submitOrderTep();
    }

    public void submitOrder(View view) {
        Intent ganesh = new Intent(this, Svetlo.class);
        startActivity(ganesh);
    }

    public void submitOrder3(View view) {
        Intent ganesh = new Intent(this, Bezpecnost.class);
        ganesh.putExtra(KEY_ACTIVITY_NAME,"a");
        startActivity(ganesh);
    }

    public void submitOrder4(View view) {
        Intent ganesh = new Intent(this, Bezpecnost.class);
        ganesh.putExtra(KEY_ACTIVITY_NAME,"b");
        startActivity(ganesh);
    }

    public void submitOrderTep() {
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

                            mTextViewResult.setText("Teplota:\n"+teplota+" °C\n\nVlhkosť:\n"+vlhkost+" %");
                        }
                    });
                }
            }
        });
    }
}

