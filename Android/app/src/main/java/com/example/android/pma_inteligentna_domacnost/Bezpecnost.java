package com.example.android.pma_inteligentna_domacnost;

import android.content.Intent;
import android.content.pm.ActivityInfo;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bezpecnost);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);

        text1 = findViewById(R.id.text_code);
    }

    public void submitOrder(View view) {

        if (zadane.length() < 4) {

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
        if (zadane.equals(code)) {
            if (getIntent().getStringExtra(KEY_ACTIVITY_NAME).equals("a")) {
                Intent ganesh = new Intent(this, Alarm.class);
                startActivity(ganesh);
            } else if (getIntent().getStringExtra(KEY_ACTIVITY_NAME).equals("b")) {
                Intent ganesh = new Intent(this, Dvere.class);
                startActivity(ganesh);
            }
        } else {
            zadane = "";
            text_pre_textView = "";
            text1.setText(text_pre_textView);
            Toast.makeText(this, "Nesprávne heslo!", Toast.LENGTH_SHORT).show();
        }
    }
}
