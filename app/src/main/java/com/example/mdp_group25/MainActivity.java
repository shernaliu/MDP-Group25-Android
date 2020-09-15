package com.example.mdp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    Button findBluetoothButton;
    Button communicationConfig;
    Button robotControl;
    Button debugButton;
    SharedPreferences sharedPreferences;
    SharedPreferences robotsharedPreferences;
    SharedPreferences.Editor editor,roboteditor;
    String connStatus;
    TextView connStatusTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        connStatusTextView = (TextView) findViewById(R.id.connStatusTextView);

        sharedPreferences = getApplicationContext().getSharedPreferences("Shared Preferences", Context.MODE_PRIVATE);
        robotsharedPreferences = getApplicationContext().getSharedPreferences("RobotControlActivity", Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
        roboteditor = robotsharedPreferences.edit();
        editor.putString("explored","");
        editor.putString("obstacle","");
        //initialize connection status on toolbar
        if (sharedPreferences.contains("connStatus")){
            editor.putString("connStatus", "Disconnected");
            TextView connStatusTextView = findViewById(R.id.connStatusTextView);
            connStatusTextView.setText("Disconnected");
        }

        if (robotsharedPreferences.contains("sentText"))    {
            roboteditor.putString("sentText", "");
        }
        if (robotsharedPreferences.contains("receivedText"))    {
            roboteditor.putString("receivedText", "");
        }
        if (robotsharedPreferences.contains("image1"))    {
            roboteditor.putString("image1", "");
        }
        if (robotsharedPreferences.contains("image2"))    {
            roboteditor.putString("image2", "");
        }
        if (robotsharedPreferences.contains("image3"))    {
            roboteditor.putString("image3", "");
        }
        if (robotsharedPreferences.contains("image4"))    {
            roboteditor.putString("image4", "");
        }
        if (robotsharedPreferences.contains("image5"))    {
            roboteditor.putString("image5", "");
        }

        roboteditor.commit();
        editor.commit();

        findBluetoothButton = (Button) findViewById(R.id.findBluetoothButton);
        findBluetoothButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent myIntent = new Intent(v.getContext(), BluetoothDeviceActivity.class);
                startActivity(myIntent);
            }
        });

        communicationConfig = findViewById(R.id.communicationsConfig);
        communicationConfig.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent myIntent = new Intent(view.getContext(), CommunicationsConfig.class);
                startActivity(myIntent);
            }
        });

        robotControl = findViewById(R.id.robotControls);
        robotControl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent myIntent = new Intent(view.getContext(), RobotControlActivity.class);
                startActivity(myIntent);
            }
        });

        debugButton = findViewById(R.id.debug);
        debugButton.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View view) {
               Intent myIntent = new Intent(view.getContext(), DebugConnectionActivity.class);
               startActivity(myIntent);
            }
        }
        );

    }

    @Override
    protected void onResume(){
        super.onResume();
        if (sharedPreferences.contains("connStatus"))
            connStatus = sharedPreferences.getString("connStatus", "");

        connStatusTextView.setText(connStatus);
    }

}
