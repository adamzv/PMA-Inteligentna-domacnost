package com.example.android.pma_inteligentna_domacnost;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
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

public class Alarm extends AppCompatActivity {

    public static final String KEY_ACTIVITY_NAME = "KEY_ACTIVITY_NAME";
    private ImageView imgView;
    private TextView mTextViewResult;
    public static final String SHARED_PREFS = "sharedPrefs";
    public static final String TEXT = "text";

    private int hodnota;
    public int tag;
    String sprava;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
        loadData();
        updateViews();
        imgView = (ImageView) findViewById(R.id.alarm);
        imgView.setTag(tag);
        if(imgView.getTag().equals(1)){
            imgView.setImageResource(R.drawable.alarm_off);
            saveData();
        }else{
            imgView.setImageResource(R.drawable.alarm_on);
            saveData();
        }

        mTextViewResult = findViewById(R.id.textOdpoved);

        if (tag == 1)  sprava = "Alarm je vypnutý!";
        else sprava = "Alarm je zapnutý";

        mTextViewResult.setText(sprava);

        submitOrder(imgView);

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
        ganesh.putExtra(KEY_ACTIVITY_NAME,"b");
        startActivity(ganesh);
    }

    public void submitOrder(View view){

        imgView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                OkHttpClient client = new OkHttpClient();

                String url = "http://iot-python.eu-de.mybluemix.net/api/alarm";

                String on_off;
                if (tag == 2) {
                    on_off = "off";
                    sprava = "Alarm je vypnutý!";
                }
                else {
                    on_off = "on";
                    sprava = "Alarm je zapnutý";
                }

                final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
                JsonObject json = new JsonObject();
                json.addProperty("senzor", "pir");
                json.addProperty("status",on_off);


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

                            Alarm.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mTextViewResult.setText(sprava);

                                    loadData();
                                    updateViews();

                                    if (tag == 1) tag = 2;
                                    else tag = 1;

                                    imgView.setTag(tag);

                                    if(imgView.getTag().equals(1)){
                                        imgView.setImageResource(R.drawable.alarm_off);
                                        saveData();
                                    }else{
                                        imgView.setImageResource(R.drawable.alarm_on);
                                        saveData();
                                    }
                                }
                            });
                        }
                    }
                });
            }
        });
    }

    public void saveData() {
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putInt(TEXT,tag);
        editor.apply();

    }

    public void loadData() {
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        hodnota = sharedPreferences.getInt(TEXT, 1);
    }

    public void updateViews() {
        tag = hodnota;
    }
}
