package com.example.android.pma_inteligentna_domacnost;

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

public class Dvere extends AppCompatActivity {

    public static final String KEY_ACTIVITY_NAME = "KEY_ACTIVITY_NAME";
    public SwipeRefreshLayout swipeRefreshLayout;
    private String hodnota;
    private String status;

    public TextView mTextViewResult;
    public Switch switchDvere;

    public TextView mTextViewResult2;
    public Switch switchGaraz;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dvere);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);

        mTextViewResult = (TextView) findViewById(R.id.textik);
        switchDvere = (Switch) findViewById(R.id.idDvere);

        mTextViewResult2 = (TextView) findViewById(R.id.textik2);
        switchGaraz = (Switch) findViewById(R.id.idGaraz);

        if (checkConnection()) getDvere();
        else {
            switchDvere.setChecked(false);
            switchGaraz.setChecked(false);
        }

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

                        if (checkConnection()) {
                            switchDvere.setClickable(true);
                            switchGaraz.setClickable(true);
                            getDvere();
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

    public void submitOrder4(View view) {

        int ID = view.getId();

        if (checkConnection()) {

            if (ID == R.id.idDvere) {
                hodnota = "dvere";
                if (switchDvere.isChecked()) status = "on";
                else status = "off";
            } else if (ID == R.id.idGaraz) {
                hodnota = "garaz";
                if (switchGaraz.isChecked()) status = "on";
                else status = "off";
            }

            OkHttpClient client = new OkHttpClient();

            String url = "http://iot-python.eu-de.mybluemix.net/api/dvere";

            final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
            JsonObject json = new JsonObject();
            json.addProperty("senzor", "servo");
            json.addProperty("hodnota", hodnota);
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

                        Dvere.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                if (hodnota.equals("dvere")) {
                                    mTextViewResult.setText(myResponse);
                                } else if (hodnota.equals("garaz")) {
                                    mTextViewResult2.setText(myResponse);
                                }

                            }
                        });
                    }
                }
            });
        } else {

            if (ID == R.id.idDvere) {
                if (switchDvere.isChecked()) switchDvere.setChecked(false);
                else switchDvere.setChecked(true);
            } else if (ID == R.id.idGaraz) {
                if (switchGaraz.isChecked()) switchGaraz.setChecked(false);
                else switchGaraz.setChecked(true);
            }

            switchDvere.setClickable(false);
            switchGaraz.setClickable(false);
        }
    }

    public void getDvere() {

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

                    String miestnost = "";
                    String status = "";

                    String miestnost2 = "";
                    String status2 = "";

                    try {
                        JSONArray pole = new JSONArray(response.body().string());
                        JSONObject reader = pole.getJSONObject(8);
                        JSONObject reader2 = pole.getJSONObject(9);

                        miestnost = reader.getString("miestnost");
                        status = reader.getString("status");

                        miestnost2 = reader2.getString("miestnost");
                        status2 = reader2.getString("status");

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    final String miestnostDvere = miestnost;
                    final String statusDvere = status;

                    final String miestnostGaraz = miestnost2;
                    final String statusGaraz = status2;

                    Dvere.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            mTextViewResult.setText("Miestnost: " + miestnostDvere + " \nStatus:" + statusDvere);
                            mTextViewResult2.setText("Miestnost: " + miestnostGaraz + " \nStatus:" + statusGaraz);

                            if (statusDvere.equals("off")) switchDvere.setChecked(false);
                            else switchDvere.setChecked(true);

                            if (statusGaraz.equals("off")) switchGaraz.setChecked(false);
                            else switchGaraz.setChecked(true);
                        }
                    });
                }
            }
        });
    }

    public void submitOrder(View view) {
        Intent ganesh = new Intent(this, Svetlo.class);
        startActivity(ganesh);
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


}
