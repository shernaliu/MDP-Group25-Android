package com.example.mdp_group25;

import android.content.Intent;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    Button bluetoothBtn, robotControlBtn, functionsBtn, debugBtn;
    SharedPreferences mainSharedPrefs, robotsharedPrefs;
    SharedPreferences.Editor editor, roboteditor;
    String connStatus;
    TextView connStatusTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bluetoothBtn = findViewById(R.id.bluetooth);
        functionsBtn = findViewById(R.id.functions);
        robotControlBtn = findViewById(R.id.robotControls);
        debugBtn = findViewById(R.id.debug);
        connStatusTextView = (TextView) findViewById(R.id.connStatusTextView);

        /* OnClickListener event handler to open up the different pages*/
        bluetoothBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent myIntent = new Intent(view.getContext(), BluetoothDeviceActivity.class);
                startActivity(myIntent);
            }
        });

        functionsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent myIntent = new Intent(view.getContext(), FunctionsActivity.class);
                startActivity(myIntent);
            }
        });

        robotControlBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent myIntent = new Intent(view.getContext(), RobotActivity.class);
                startActivity(myIntent);
            }
        });

        debugBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent myIntent = new Intent(view.getContext(), DebugActivity.class);
                startActivity(myIntent);
            }
        });

        /* Shared Peferences for various items */
        mainSharedPrefs = getApplicationContext().getSharedPreferences("Shared Preferences", Context.MODE_PRIVATE);
        robotsharedPrefs = getApplicationContext().getSharedPreferences("RobotControlActivity", Context.MODE_PRIVATE);
        editor = mainSharedPrefs.edit();
        roboteditor = robotsharedPrefs.edit();
        editor.putString("explored", "");
        editor.putString("obstacle", "");
        if (mainSharedPrefs.contains("connStatus")) {
            editor.putString("connStatus", "Disconnected");
            TextView connStatusTextView = findViewById(R.id.connStatusTextView);
            connStatusTextView.setText("Disconnected");
        }
        if (robotsharedPrefs.contains("sentText")) {
            roboteditor.putString("sentText", "");
        }
        if (robotsharedPrefs.contains("receivedText")) {
            roboteditor.putString("receivedText", "");
        }
        if (robotsharedPrefs.contains("image1")) {
            roboteditor.putString("image1", "");
        }
        if (robotsharedPrefs.contains("image2")) {
            roboteditor.putString("image2", "");
        }
        if (robotsharedPrefs.contains("image3")) {
            roboteditor.putString("image3", "");
        }
        if (robotsharedPrefs.contains("image4")) {
            roboteditor.putString("image4", "");
        }
        if (robotsharedPrefs.contains("image5")) {
            roboteditor.putString("image5", "");
        }
        roboteditor.commit();
        editor.commit();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mainSharedPrefs.contains("connStatus"))
            connStatus = mainSharedPrefs.getString("connStatus", "");

        connStatusTextView.setText(connStatus);
    }

}
