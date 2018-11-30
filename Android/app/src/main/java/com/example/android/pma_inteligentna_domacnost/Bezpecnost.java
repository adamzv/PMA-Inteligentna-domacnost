package com.example.android.pma_inteligentna_domacnost;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.CountDownTimer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;


public class Bezpecnost extends AppCompatActivity {

    public static final String KEY_ACTIVITY_NAME = "KEY_ACTIVITY_NAME";
    public TextView text1;
    public String text_pre_textView = "";
    private String code = "1234";
    private String zadane = "";
    private int ID;
    private int pocetPokusov = 0;

    private CountDownTimer countDownTimer;
    private long timeLeftInMilliseconds = 18000;
    private boolean timerRunning;

    public static final String SHARED_PREFSS = "sharedPrefss";
    public static final String TEXT_SECURE = "text_secure";
    public static final String TEXT2_SECURE = "text2_secure";

    private long hodnota_timer = 0;
    private int hodnota_pokus = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bezpecnost);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);

        text1 = findViewById(R.id.text_code);

        loadData_secure();
        updateViews_secure();
        if (pocetPokusov > 2) {
            startTimer();
        }

    }

    public void submitOrder(View view) {

        if (zadane.length() < 4 && pocetPokusov <= 2) {

            ID = view.getId();
            if (ID == R.id.num_0) {
                zadane = zadane + "0";
                text_pre_textView += "*";
            } else if (ID == R.id.num_1) {
                zadane = zadane + "1";
                text_pre_textView += "*";
            } else if (ID == R.id.num_2) {
                zadane = zadane + "2";
                text_pre_textView += "*";
            } else if (ID == R.id.num_3) {
                zadane = zadane + "3";
                text_pre_textView += "*";
            } else if (ID == R.id.num_4) {
                zadane = zadane + "4";
                text_pre_textView += "*";
            } else if (ID == R.id.num_5) {
                zadane = zadane + "5";
                text_pre_textView += "*";
            } else if (ID == R.id.num_6) {
                zadane = zadane + "6";
                text_pre_textView += "*";
            } else if (ID == R.id.num_7) {
                zadane = zadane + "7";
                text_pre_textView += "*";
            } else if (ID == R.id.num_8) {
                zadane = zadane + "8";
                text_pre_textView += "*";
            } else if (ID == R.id.num_9) {
                zadane = zadane + "9";
                text_pre_textView += "*";
            }
            text1.setText(text_pre_textView);
        }
    }

    public void submitOrder2(View view) {
        if (zadane.length() > 0) {
            text_pre_textView = text_pre_textView.substring(0, text_pre_textView.length() - 1);
            zadane = zadane.substring(0, zadane.length() - 1);
            text1.setText(text_pre_textView);
        }
    }

    public void submitOrder3(View view) {
        pocetPokusov++;
         if (zadane.equals(code)) {
             pocetPokusov = 0;
             timeLeftInMilliseconds = 18000;
             saveData_secure();
            if (getIntent().getStringExtra(KEY_ACTIVITY_NAME).equals("a")) {
                Intent ganesh = new Intent(this, Alarm.class);
                startActivity(ganesh);
            } else if (getIntent().getStringExtra(KEY_ACTIVITY_NAME).equals("b")) {
                Intent ganesh = new Intent(this, Dvere.class);
                startActivity(ganesh);
            }
        } else if (pocetPokusov > 2) {
             saveData_secure();
             loadData_secure();
             updateViews_secure();
             startTimer();
         }
        else {
            saveData_secure();
            loadData_secure();
            updateViews_secure();
            zadane = "";
            text_pre_textView = "";
            text1.setText(text_pre_textView);
            Toast.makeText(this, "Nesprávne heslo!", Toast.LENGTH_SHORT).show();
        }
    }


    public void startTimer() {
        countDownTimer = new CountDownTimer(timeLeftInMilliseconds, 1000) {
            @Override
            public void onTick(long l) {
                timeLeftInMilliseconds = l;
                updateTimer();
                saveData_secure();
            }

            @Override
            public void onFinish() {
                timeLeftInMilliseconds = 18000;
                pocetPokusov = 0;
                saveData_secure();
                loadData_secure();
                updateTimer();
                text1.setText("");
            }
        }.start();

        timerRunning = true;
    }

    public void updateTimer() {
        int minutes = (int) timeLeftInMilliseconds / 60000;
        int seconds = (int) timeLeftInMilliseconds % 60000 / 1000;

        String timeLeftText;

        timeLeftText = "" + minutes;
        timeLeftText += ":";
        if (seconds < 10) timeLeftText += "0";
        timeLeftText += seconds;

        text1.setText(timeLeftText);
    }

    public void saveData_secure() {
        SharedPreferences sharedPreferencess = getSharedPreferences(SHARED_PREFSS, MODE_PRIVATE);
        SharedPreferences.Editor editorik = sharedPreferencess.edit();

        editorik.putLong(TEXT_SECURE, timeLeftInMilliseconds);
        editorik.putInt(TEXT2_SECURE, pocetPokusov);
        editorik.apply();
    }

    public void loadData_secure() {
        SharedPreferences sharedPreferencess = getSharedPreferences(SHARED_PREFSS, MODE_PRIVATE);
        hodnota_timer = sharedPreferencess.getLong(TEXT_SECURE, 180000);
        hodnota_pokus = sharedPreferencess.getInt(TEXT2_SECURE, 0);
    }

    public void updateViews_secure() {
        timeLeftInMilliseconds = hodnota_timer;
        pocetPokusov = hodnota_pokus;
    }
}
