package com.example.mdp_group25;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class DebugActivity extends AppCompatActivity {
    private static String TAG = "DEBUG_ACTIVITY";
    private static Context context;
    private UtilityTool utilityTool = new UtilityTool();
    Button sendBtn;
    TextView receivedMsg;
    TextView sentMessageDisplay;
    EditText sentMsgInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug);
        LocalBroadcastManager.getInstance(this).registerReceiver(msgReceiver, new IntentFilter("incomingMessage"));
        DebugActivity.context = getApplicationContext();
        sentMsgInput = findViewById(R.id.msgInput);
        receivedMsg = findViewById(R.id.receivedMsg);
        receivedMsg.setMovementMethod(new ScrollingMovementMethod());
        sentMessageDisplay = findViewById(R.id.sentMessageDisplay);
        sentMessageDisplay.setMovementMethod(new ScrollingMovementMethod());
        sendBtn = findViewById(R.id.sendBtn);

        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String message = sentMsgInput.getText().toString();
                utilityTool.printMsg(context, message);
                sentMessageDisplay.append("\n"+message);
                sentMsgInput.getText().clear();
            }
        });
    }

    BroadcastReceiver msgReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("msgReceived");
            UtilityTool.log(TAG,"msgReceived: message --- " + message);
            receivedMsg.setText(message);
        }
    };

    @Override
    protected void onDestroy(){
        super.onDestroy();
        try{
            LocalBroadcastManager.getInstance(this).unregisterReceiver(msgReceiver);
        } catch(IllegalArgumentException e){
            e.printStackTrace();
        }
    }
}