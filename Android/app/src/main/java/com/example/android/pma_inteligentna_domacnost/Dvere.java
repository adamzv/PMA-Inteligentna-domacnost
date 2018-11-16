package com.example.android.pma_inteligentna_domacnost;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class Dvere extends AppCompatActivity {

    public static final String KEY_ACTIVITY_NAME = "KEY_ACTIVITY_NAME";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dvere);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);

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
        ganesh.putExtra(KEY_ACTIVITY_NAME,"a");
        startActivity(ganesh);
    }


}
