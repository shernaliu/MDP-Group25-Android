package com.example.mdp_group25;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class DebugActivity extends AppCompatActivity {
    private static String TAG = "DEBUG_ACTIVITY";
    private static Context context;

    private Util util = new Util();

    EditText sentMessage;
    TextView receivedMessage;
    Button sendButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug);
        LocalBroadcastManager.getInstance(this).registerReceiver(debugMessageReceiver, new IntentFilter("incomingMessage"));
        DebugActivity.context = getApplicationContext();

        sentMessage = findViewById(R.id.messageSent);
        receivedMessage = findViewById(R.id.receivedMessage);
        sendButton = findViewById(R.id.sendButton);


        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String message = sentMessage.getText().toString();
                util.printMessage(context, message);
            }
        });
    }

    BroadcastReceiver debugMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("receivedMessage");
            Util.showLog(TAG,"receivedMessage: message --- " + message);
            receivedMessage.setText(message);
        }
    };

    @Override
    protected void onDestroy(){
        super.onDestroy();
        try{
            LocalBroadcastManager.getInstance(this).unregisterReceiver(debugMessageReceiver);
        } catch(IllegalArgumentException e){
            e.printStackTrace();
        }
    }
}