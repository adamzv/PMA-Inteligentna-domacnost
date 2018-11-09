package com.example.android.pma_inteligentna_domacnost;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.support.v4.app.NotificationCompat;

import android.widget.EditText;

import com.ibm.mobilefirstplatform.clientsdk.android.core.api.BMSClient;
import com.ibm.mobilefirstplatform.clientsdk.android.push.api.MFPPush;
import com.ibm.mobilefirstplatform.clientsdk.android.push.api.MFPPushException;
import com.ibm.mobilefirstplatform.clientsdk.android.push.api.MFPPushResponseListener;
import com.ibm.mobilefirstplatform.clientsdk.android.push.api.MFPPushNotificationListener;
import com.ibm.mobilefirstplatform.clientsdk.android.push.api.MFPSimplePushNotification;


public class MainActivity extends AppCompatActivity {

    private MFPPush push;
    private MFPPushNotificationListener notificationListener;
    private NotificationManagerCompat notificationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
        notificationManager = NotificationManagerCompat.from(this);

        // Core SDK must be initialized to interact with Bluemix Mobile services.
        BMSClient.getInstance().initialize(getApplicationContext(), BMSClient.REGION_GERMANY);



        /*
         * Initialize the Push Notifications client SDK with the App Guid and Client Secret from your Push Notifications service instance on Bluemix.
         * This enables authenticated interactions with your Push Notifications service instance.
         */
        push = MFPPush.getInstance();
        push.initialize(getApplicationContext(), getString(R.string.pushAppGuid), getString(R.string.pushClientSecret));

        /*
         * Attempt to register your Android device with your Bluemix Push Notifications service instance.
         * Developers should put their user ID as the first argument.
         */
        push.registerDevice(new MFPPushResponseListener<String>() {

            @Override
            public void onSuccess(String response) {

                // Split response and convert to JSON object to display User ID confirmation from the backend.
                String[] resp = response.split("Text: ");
                String userId = "";
                try {
                    org.json.JSONObject responseJSON = new org.json.JSONObject(resp[1]);
                    userId = responseJSON.getString("userId");

                } catch (org.json.JSONException e) {
                    e.printStackTrace();
                }

                android.util.Log.i("YOUR_TAG_HERE", "Successfully registered for Bluemix Push Notifications with USER ID: " + userId);
            }

            @Override
            public void onFailure(MFPPushException ex) {

                String errLog = "Error registering for Bluemix Push Notifications: ";
                String errMessage = ex.getErrorMessage();
                int statusCode = ex.getStatusCode();

                // Create an error log based on the response code and returned error message.
                if (statusCode == 401) {
                    errLog += "Cannot authenticate successfully with Bluemix Push Notifications service instance. Ensure your CLIENT SECRET is correct.";
                } else if (statusCode == 404 && errMessage.contains("Push GCM Configuration")) {
                    errLog += "Your Bluemix Push Notifications service instance's GCM/FCM Configuration does not exist.\n" +
                            "Ensure you have configured GCM/FCM Push credentials on your Bluemix Push Notifications dashboard correctly.";
                } else if (statusCode == 404) {
                    errLog += "Cannot find Bluemix Push Notifications service instance, ensure your APP GUID is correct.";
                } else if (statusCode >= 500) {
                    errLog += "Bluemix and/or the Bluemix Push Notifications service are having problems. Please try again later.";
                } else if (statusCode == 0) {
                    errLog += "Request to Bluemix Push Notifications service instance timed out. Ensure your device is connected to the Internet.";
                }

                android.util.Log.e("YOUR_TAG_HERE", errLog);
            }
        });

        // A notification listener is needed to handle any incoming push notifications within the Android application.
        notificationListener = new MFPPushNotificationListener() {

            @Override
            public void onReceive (final MFPSimplePushNotification message) {
                // TODO: Process the message and add your logic here.
                android.util.Log.i("YOUR_TAG_HERE", "Received a push notification: " + message.toString());
                runOnUiThread(new Runnable() {
                    public void run() {
                        android.app.DialogFragment fragment = PushReceiverFragment.newInstance("Push notification received", message.getAlert());
                        fragment.show(getFragmentManager(), "dialog");
                        sendOnChannel1(message.getAlert());
                    }
                });
            }
        };

    }

    @Override
    public void onResume() {
        super.onResume();
        // Enable the Push Notifications client SDK to listen for push notifications using the predefined notification listener.
        if (push != null) {
            push.listen(notificationListener);
        }


    }

    @Override
    public void onPause() {
        super.onPause();
        if (push != null) {
            push.hold();
        }
    }

    public void submitOrder(View view) {
        Intent ganesh = new Intent(this, Svetlo.class);
        startActivity(ganesh);
    }

    public void submitOrder2(View view) {
        Intent ganesh = new Intent(this, Teplota.class);
        startActivity(ganesh);
    }

    public void sendOnChannel1(String imessage) {

        String title = "Varovanie!";
        String message =  imessage;

        Notification notification = new NotificationCompat.Builder(this, App.CHANNEL_1_ID)
                .setSmallIcon(R.drawable.ic_one)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .build();

        notificationManager.notify(1, notification);
    }


}
