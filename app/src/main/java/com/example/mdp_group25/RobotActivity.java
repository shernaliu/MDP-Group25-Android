package com.example.mdp_group25;

import android.content.SharedPreferences;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Context;
import android.content.DialogInterface;
import android.view.HapticFeedbackConstants;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Switch;
import android.app.ProgressDialog;
import android.widget.ToggleButton;
import org.json.JSONObject;
import org.json.JSONException;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class RobotActivity extends AppCompatActivity {

    private static SharedPreferences robotSharedPrefs;
    private static SharedPreferences.Editor editor, editorConn;
    BluetoothConnService mBluetoothConnection;
    private static Context context;
    private static SharedPreferences pref;
    private static boolean autoUpdate = false;
    private static boolean tiltControl = false;
    private static String TAG = "RobotActivity";
    private Util util = new Util();
    boolean turnedLeft = false;
    boolean turnedRight = false;
    boolean goStraight = false;
    boolean goback = false;
    GridMap gridMap;
    View rectangleBgManual, rectangleBgAuto;
    Button resetMapBtn, fn1Btn, fn2Btn, manualUpdateBtn;
    ToggleButton manualAutoToggleBtn, exploreToggleBtn, fastestToggleBtn, setStartPointToggleBtn, setWaypointToggleBtn;
    TextView robotStatusTextView, connStatusTextView, xAxisTextView, yAxisTextView, directionAxisTextView, sentMessage, receivedMessage, xAxisTextViewWP, yAxisTextViewWP;
    ImageButton moveForwardImageBtn, turnRightImageBtn, moveBackwardImageBtn, turnLeftImageBtn;
    ImageButton directionChangeImageBtn, exploredImageBtn, obstacleImageBtn, clearImageBtn;
    SharedPreferences sharedPreferencesConn;
    String connStatus;
    Switch tiltSwitch;
    SensorManager sensorManager;
    Sensor tiltSensor;
    SensorEventListener tiltSensorListener;
    String current_command = "";
    String mdf_string_final = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_robot);

        /* Shared Preferences for  retrieving, storing and writing data */
        robotSharedPrefs = getApplicationContext().getSharedPreferences("RobotControlActivity", Context.MODE_PRIVATE);
        sharedPreferencesConn = getApplicationContext().getSharedPreferences("Shared Preferences", Context.MODE_PRIVATE);
        pref = getApplicationContext().getSharedPreferences("CommunicationsPreferences", Context.MODE_PRIVATE);
        editor = robotSharedPrefs.edit();
        editorConn = sharedPreferencesConn.edit();

        /* Init GridMap */
        gridMap = new GridMap(this);

        /* Get a reference to all the required components in RobotActivity */
        gridMap = findViewById(R.id.mapView);
        manualAutoToggleBtn = findViewById(R.id.manualAutoToggleBtn);
        fastestToggleBtn = findViewById(R.id.fastestToggleBtn);
        exploreToggleBtn = findViewById(R.id.exploreToggleBtn);
        robotStatusTextView = findViewById(R.id.robotStatusTextView);
        moveForwardImageBtn = findViewById(R.id.moveForwardImageBtn);
        moveBackwardImageBtn = findViewById(R.id.moveBackwardImageBtn);
        turnRightImageBtn = findViewById(R.id.turnRightImageBtn);
        turnLeftImageBtn = findViewById(R.id.turnLeftImageBtn);
        resetMapBtn = findViewById(R.id.resetMapBtn);
        fn1Btn = findViewById(R.id.buttonF1);
        fn2Btn = findViewById(R.id.buttonF2);
        xAxisTextView = findViewById(R.id.xAxisTextView);
        yAxisTextView = findViewById(R.id.yAxisTextView);
        directionAxisTextView = findViewById(R.id.directionAxisTextView);
        directionChangeImageBtn = findViewById(R.id.directionChangeImageBtn);
        setStartPointToggleBtn = findViewById(R.id.setStartPointToggleBtn);
        setWaypointToggleBtn = findViewById(R.id.setWaypointToggleBtn);
        exploredImageBtn = findViewById(R.id.exploredImageBtn);
        obstacleImageBtn = findViewById(R.id.obstacleImageBtn);
        clearImageBtn = findViewById(R.id.clearImageBtn);
        manualUpdateBtn = findViewById(R.id.manualUpdateBtn);
        connStatusTextView = findViewById(R.id.connStatusTextView);
        sentMessage = findViewById(R.id.sentMessage);
        receivedMessage = findViewById(R.id.receivedMsg);
        xAxisTextViewWP = findViewById(R.id.xAxisTextViewWP);
        yAxisTextViewWP = findViewById(R.id.yAxisTextViewWP);
        tiltSwitch = findViewById(R.id.tiltSwitch);
        receivedMessage.setMovementMethod(new ScrollingMovementMethod());
        sentMessage.setMovementMethod(new ScrollingMovementMethod());
        rectangleBgManual = findViewById(R.id.rectangle_bg_manual);
        rectangleBgAuto = findViewById(R.id.rectangle_bg_auto);

        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, new IntentFilter("incomingMessage"));
        sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        tiltSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        if (sharedPreferencesConn.contains("connStatus")){
            connStatus = sharedPreferencesConn.getString("connStatus", "");
        }
        connStatusTextView.setText(connStatus);

        final android.os.Handler customHandler = new android.os.Handler();

        /* Create a new thread for the customHandler */
        Runnable runthisthread = new Runnable()
        {
            public void run()
            {
                current_command = "";
                customHandler.postDelayed(this, 2000);
            }
        };
        customHandler.postDelayed(runthisthread, 0);

        /* TiltSensor event handler to perform robot movements based on tilting of tablet. */
        tiltSensorListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {

                if (tiltControl){
                    float[] rotationMatrix = new  float[16];
                    SensorManager.getRotationMatrixFromVector(rotationMatrix,event.values);
                    float delta = 6;
                    if (event.values[1] < 2.5f-delta){
                        // Sensor is tilted forward
                        if(!current_command.equals("Forward"))
                        {
                            Util.showLog(TAG, "Tablet is lifted up!");
                            gridMap.moveRobot("forward");
                            updateTextViews();
                            displayToast("Tablet is lifted up!");
                            util.printMessage(context, "AR>w");
                            current_command = "Forward";
                            return;
                        }
                    }
                    else if (event.values[1] > 2.5f +delta-1) {
                        if(!current_command.equals("Backward"))
                        {
                        Util.showLog(TAG, "Tablet is lowered!");
                        gridMap.moveRobot("back");
                        updateTextViews();
                        displayToast("Tablet is lowered!");
                        util.printMessage(context, "AR>s");
                        current_command = "Backward";
                        return;
                    }
                    }
                    if (event.values[0] > 5.5){
                        if(!current_command.equals("Left"))
                        {
                            if (turnedLeft) {
                                turnedLeft = false;
                            }
                            else {
                                Util.showLog(TAG, "Tablet is tilted left!");
                                displayToast("Tablet is tilted left!");
                                gridMap.moveRobot("left");
                                updateTextViews();
                                util.printMessage(context, "AR>a");
                                turnedLeft = true;
                                current_command = "Left";
                                return;
                            }
                        }
                    } else if (event.values[0] < -5.5) {
                        if(!current_command.equals("Right")) {
                            if (turnedRight){
                                turnedRight = false;
                            }
                            else {
                                Util.showLog(TAG, "Tablet is tilted right!");
                                displayToast("Tablet is tilted right!");
                                gridMap.moveRobot("right");
                                updateTextViews();
                                util.printMessage(context, "AR>d");
                                turnedRight = true;
                                current_command = "Right";
                                return;
                            }
                        }
                    }
                }

            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
                Util.showLog(TAG, "Accuracy changed.");
            }
        };
        sensorManager.registerListener(tiltSensorListener,tiltSensor, 3000000);

        /* Button event handler to perform update. */
        manualUpdateBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                performHaptic();
                util.printMessage(context, "AL>sa");
            }
        });

        // TODO: INTEGRATE WITH ALGO
        exploreToggleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                performHaptic();
                if (check_valid_time("Explore")) {
                    util.showLog(TAG, "Clicked exploreToggleBtn");
                    Button exploreToggleBtn = (Button) view;
                    if (exploreToggleBtn.getText().equals("EXPLORE")) {
                        // end exploration
                        fastestToggleBtn.setEnabled(true);
                        resetMapBtn.setEnabled(true);
                        setStartPointToggleBtn.setEnabled(true);
                        setWaypointToggleBtn.setEnabled(true);
                        util.printMessage(context, "AL>st");
                    } else if (exploreToggleBtn.getText().equals("STOP")) {
                        // start exploration
                        fastestToggleBtn.setEnabled(false);
                        resetMapBtn.setEnabled(false);
                        setStartPointToggleBtn.setEnabled(false);
                        setWaypointToggleBtn.setEnabled(false);
                        util.printMessage(context, "AL>ex");
                    } else {
                        displayToast("Else statement: " + exploreToggleBtn.getText());
                    }
                    util.showLog(TAG, "Exiting exploreToggleBtn");
                }
            }
        });

        /* Button event handler to perform fastest path. */
        fastestToggleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                performHaptic();
                setWaypointToggleBtn.setEnabled(false);
                util.showLog(TAG, "Clicked fastestToggleBtn");
                Button fastestToggleBtn = (Button) view;
                if (fastestToggleBtn.getText().equals("FASTEST")) {
                    // end fastest path
                    setWaypointToggleBtn.setEnabled(true);
                    util.printMessage(context, "AL>st");
                }
                else if (fastestToggleBtn.getText().equals("STOP")) {
                    // start fastest path
                    util.printMessage(context, "AL>fp");

                }
                else
                    displayToast(fastestToggleBtn.getText().toString());
                util.showLog(TAG, "Exiting fastestToggleBtn");
            }
        });

        /* Button event handler to allow robot to move forward. */
        moveForwardImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                performHaptic();
                util.showLog(TAG,"Pressed moveForwardImageBtn!");
                if (gridMap.getAutomaticUpdate())
                    updateStatus(StatusRobot.ManualModeReq);
                else if (gridMap.getCanDrawRobot() && !gridMap.getAutomaticUpdate()) {
                    gridMap.moveRobot("forward");
                    if (gridMap.getValidPosition())
                        updateStatus(StatusRobot.MoveForward);
                    else
                        updateStatus(StatusRobot.UnableForward);
                    util.printMessage(context, "AR>w");
                    updateTextViews();
                }
                else
                    updateStatus(StatusRobot.SetStartPoint);
                util.showLog(TAG, "Exiting moveForwardImageBtn");
            }
        });

        /* Button event handler to allow robot to move backward. */
        moveBackwardImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                performHaptic();
                util.showLog(TAG,"Pressed moveBackwardImageBtn!");
                if (gridMap.getAutomaticUpdate())
                    updateStatus(StatusRobot.ManualModeReq);
                else if (gridMap.getCanDrawRobot() && !gridMap.getAutomaticUpdate()) {
                    gridMap.moveRobot("back");
                    updateTextViews();
                    if (gridMap.getValidPosition())
                        updateStatus(StatusRobot.MoveBackward);
                    else
                        updateStatus(StatusRobot.UnableBackward);
                    util.printMessage(context, "r");
                    updateTextViews();
                }
                else
                    updateStatus(StatusRobot.SetStartPoint);
                util.showLog(TAG,"Exiting moveBackwardImageBtn");
            }
        });

        /* Button event handler to allow robot to turn right. */
        turnRightImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                performHaptic();
                util.showLog(TAG,"Pressed turnRightImageBtn!");
                if (gridMap.getAutomaticUpdate())
                    updateStatus(StatusRobot.ManualModeReq);
                else if (gridMap.getCanDrawRobot() && !gridMap.getAutomaticUpdate()) {
                    gridMap.moveRobot("right");
                    updateStatus(StatusRobot.TurinRight);
                    util.printMessage(context, "AR>d");
                    updateTextViews();
                }
                else
                    updateStatus(StatusRobot.SetStartPoint);
                util.showLog(TAG,"Exiting turnRightImageBtn");
            }
        });

        /* Button event handler to allow robot to turn left. */
        turnLeftImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                performHaptic();
                util.showLog(TAG, "Pressed turnLeftImageBtn!");
                if (gridMap.getAutomaticUpdate())
                    updateStatus(StatusRobot.ManualModeReq);
                else if (gridMap.getCanDrawRobot() && !gridMap.getAutomaticUpdate()) {
                    gridMap.moveRobot("left");
                    updateTextViews();
                    updateStatus(StatusRobot.TurinLeft);
                    util.printMessage(context,"AR>a");
                    updateTextViews();
                }
                else
                    updateStatus(StatusRobot.SetStartPoint);
                util.showLog(TAG, "Exiting turnLeftImageBtn");
            }
        });

        /* Button event handler to perform function 1. */
        fn1Btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                performHaptic();
                util.showLog(TAG,"Pressed buttonF1!");
                String firstFunction = pref.getString(FunctionsActivity.functionOne, "");
                if(firstFunction != ""){
                    util.printMessage(context, firstFunction);
                    updateTextViews();
                }
            }
        });

        /* Button event handler to perform function 2. */
        fn2Btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                performHaptic();
                util.showLog(TAG,"Pressed buttonF2!");
                String secondFunction = pref.getString(FunctionsActivity.functionTwo, "");
                if(secondFunction != ""){
                    util.printMessage(context, secondFunction);
                    updateTextViews();
                }
            }
        });

        /* Button event handler to reset the GridMap. */
        resetMapBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                performHaptic();
                util.showLog(TAG,"Pressed resetMapBtn!");
                gridMap.resetMap();
                displayToast("GridMap reset!");
                updateTextViews();
            }
        });

        /* Button event handler to set the start point. */
        setStartPointToggleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                performHaptic();
                util.showLog(TAG,"Pressed setStartPointToggleBtn!");
                if (setStartPointToggleBtn.getText().equals("Set Start Point"))
                    displayToast("Cancelled selecting starting point!");
                else if (setStartPointToggleBtn.getText().equals("CANCEL") && !gridMap.getAutomaticUpdate()) {
                    displayToast("Please select starting point!");
                    gridMap.setStartCoordStatus(true);
                    gridMap.toggleCheckedBtn("setStartPointToggleBtn");
                    updateTextViews();
                } else
                    displayToast("Please select manual mode!");
                util.showLog(TAG,"Exiting setStartPointToggleBtn!");
            }
        });

        /* Button event handler to set the way point. */
        setWaypointToggleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                performHaptic();
                util.showLog(TAG, "Pressed setWaypointToggleBtn!");
                if (setWaypointToggleBtn.getText().equals("Set Way Point"))
                    displayToast("Cancelled selecting waypoint!");
                else if (setWaypointToggleBtn.getText().equals("CANCEL")) {
                    displayToast("Please select waypoint!");
                    gridMap.setWaypointStatus(true);
                    gridMap.toggleCheckedBtn("setWaypointToggleBtn");
                }
                else
                    displayToast("Please select manual mode!");
                util.showLog(TAG, "Exiting setWaypointToggleBtn!");
            }
        });

        /* Button event handler to perform exploration. */
        exploredImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                performHaptic();
                util.showLog(TAG,"Pressed exploredImageBtn!");
                if (!gridMap.getExploredStatus()) {
                    displayToast("Please check cell!");
                    gridMap.setExploredStatus(true);
                    gridMap.toggleCheckedBtn("exploredImageBtn");
                }
                else if (gridMap.getExploredStatus())
                    gridMap.setSetObstacleStatus(false);
                util.showLog(TAG,"Exiting exploredImageBtn!");
            }
        });

        /* Button event handler to perform placement of obstacles in GridMap. */
        obstacleImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                util.showLog(TAG,"Pressed obstacleImageBtn!");
                if (!gridMap.getSetObstacleStatus()) {
                    displayToast("Please plot obstacles!");
                    gridMap.setSetObstacleStatus(true);
                    gridMap.toggleCheckedBtn("obstacleImageBtn");
                }
                else if (gridMap.getSetObstacleStatus())
                    gridMap.setSetObstacleStatus(false);
                util.showLog(TAG,"Exiting obstacleImageBtn!");
            }
        });

        /* Button event handler to clear all. */
        clearImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                util.showLog(TAG,"Pressed clearImageBtn!");
                if (!gridMap.getUnSetCellStatus()) {
                    displayToast("Please remove cells!");
                    gridMap.toggleCheckedBtn("clearImageBtn");
                    gridMap.setUnSetCellStatus(true);
                }
                else if (gridMap.getUnSetCellStatus())
                    gridMap.setUnSetCellStatus(false);
                util.showLog(TAG,"Exiting clearImageBtn!");
            }
        });

        /* Button event handler to toggle between Auto and Manual mode. */
        manualAutoToggleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                performHaptic();
                util.showLog(TAG,"Pressed manualAutoToggleBtn!");
                if (manualAutoToggleBtn.getText().equals("AUTO")) {
                    try {
                        gridMap.setAutomaticUpdate(true);
                        autoUpdate = true;
                        gridMap.toggleCheckedBtn("None");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    manualAutoToggleBtn.setBackgroundResource(R.drawable.btn_bg_auto);
                    (findViewById(R.id.rectangle_bg_auto)).setVisibility(View.VISIBLE);
                    (findViewById(R.id.exploreToggleBtn)).setVisibility(View.VISIBLE);
                    (findViewById(R.id.fastestToggleBtn)).setVisibility((View.VISIBLE));
                    (findViewById(R.id.setWaypointToggleBtn)).setVisibility(View.VISIBLE);
                    (findViewById(R.id.wayPointLabel)).setVisibility(View.VISIBLE);
                    (findViewById(R.id.xLabelTextViewWP)).setVisibility(View.VISIBLE);
                    (findViewById(R.id.yAxisTextViewWP)).setVisibility(View.VISIBLE);
                    (findViewById(R.id.yLabelTextViewWP)).setVisibility(View.VISIBLE);
                    (findViewById(R.id.xAxisTextViewWP)).setVisibility(View.VISIBLE);
                    (findViewById(R.id.moveBackwardImageBtn)).setVisibility(View.INVISIBLE);
                    (findViewById(R.id.buttonF1)).setVisibility(View.INVISIBLE);
                    (findViewById(R.id.buttonF2)).setVisibility(View.INVISIBLE);
                    (findViewById(R.id.moveForwardImageBtn)).setVisibility(View.INVISIBLE);
                    (findViewById(R.id.turnLeftImageBtn)).setVisibility(View.INVISIBLE);
                    (findViewById(R.id.turnRightImageBtn)).setVisibility(View.INVISIBLE);
                    (findViewById(R.id.tiltSwitch)).setVisibility(View.INVISIBLE);
                    (findViewById(R.id.rectangle_bg_manual)).setVisibility(View.INVISIBLE);
                    (findViewById(R.id.manualUpdateBtn)).setVisibility(View.INVISIBLE);
                    gridMap.resetMap();
                    displayToast("AUTO mode");
                }
                else if (manualAutoToggleBtn.getText().equals("MANUAL")) {
                    try {
                        gridMap.setAutomaticUpdate(false);
                        autoUpdate = false;
                        gridMap.toggleCheckedBtn("None");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    manualAutoToggleBtn.setBackgroundResource(R.drawable.btn_bg_manual);
                    (findViewById(R.id.rectangle_bg_auto)).setVisibility(View.INVISIBLE);
                    (findViewById(R.id.rectangle_bg_manual)).setVisibility(View.VISIBLE);
                    (findViewById(R.id.exploreToggleBtn)).setVisibility(View.INVISIBLE);
                    (findViewById(R.id.fastestToggleBtn)).setVisibility((View.INVISIBLE));
                    (findViewById(R.id.setWaypointToggleBtn)).setVisibility(View.INVISIBLE);
                    (findViewById(R.id.wayPointLabel)).setVisibility(View.INVISIBLE);
                    (findViewById(R.id.xLabelTextViewWP)).setVisibility(View.INVISIBLE);
                    (findViewById(R.id.yAxisTextViewWP)).setVisibility(View.INVISIBLE);
                    (findViewById(R.id.yLabelTextViewWP)).setVisibility(View.INVISIBLE);
                    (findViewById(R.id.xAxisTextViewWP)).setVisibility(View.INVISIBLE);
                    (findViewById(R.id.buttonF1)).setVisibility(View.VISIBLE);
                    (findViewById(R.id.buttonF2)).setVisibility(View.VISIBLE);
                    (findViewById(R.id.moveForwardImageBtn)).setVisibility(View.VISIBLE);
                    (findViewById(R.id.turnLeftImageBtn)).setVisibility(View.VISIBLE);
                    (findViewById(R.id.turnRightImageBtn)).setVisibility(View.VISIBLE);
                    (findViewById(R.id.tiltSwitch)).setVisibility(View.VISIBLE);
                    (findViewById(R.id.moveBackwardImageBtn)).setVisibility(View.VISIBLE);
                    (findViewById(R.id.manualUpdateBtn)).setVisibility(View.VISIBLE);
                    gridMap.resetMap();
                    displayToast("MANUAL mode");
                }
                util.showLog(TAG,"Exiting manualAutoToggleBtn");
            }
        });

        /* Button event handler for Tilt switch. */
        tiltSwitch.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                if (tiltControl){
                    tiltControl = false;
                    displayToast("Tilt Control is disabled!");
                }
                else{
                    tiltControl = true;
                    displayToast("Tilt Control is enabled!");
                }
            }
        });

        // progressDialog
        RobotActivity.context = getApplicationContext();
        ProgressDialog progressDialog;
        progressDialog = new ProgressDialog(RobotActivity.this);
        progressDialog.setMessage("Waiting for other device to reconnect...");
        progressDialog.setCancelable(false);
        progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        try{
            // Broadcast whenever the state of the bluetooth changes (e.g. connected, disconnected)
            IntentFilter filter2 = new IntentFilter("ConnectionStatus");
            LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver5, filter2);
        } catch(IllegalArgumentException e){
            e.printStackTrace();
        }
    }

    private void updateSentMessage(){
        String sent = robotSharedPrefs.getString("sentText", "");
        if(sent.length()>0){
            sentMessage.setText(sent);
        } else{
            sentMessage.setText("no sent message");
        }
    }

    private void updateReceivedMessage(){
        String received = robotSharedPrefs.getString("receivedText", "");
        if(received.length()>0){
            receivedMessage.setText(received);
        } else{
            receivedMessage.setText("no received message");
        }
    }

    private void updateStatus(String message) {
        robotStatusTextView.setText(message);
    }

    /* Method to update the required TextViews in RobotActivity. */
    private void updateTextViews() {
        xAxisTextView.setText(String.valueOf(gridMap.getCurCoord()[0]+1));
        yAxisTextView.setText(String.valueOf(gridMap.getCurCoord()[1]+1));
        xAxisTextViewWP.setText(String.valueOf(gridMap.getWaypointCoord()[0]+1));
        yAxisTextViewWP.setText(String.valueOf(gridMap.getWaypointCoord()[1]+1));
        directionAxisTextView.setText(robotSharedPrefs.getString("direction",""));
        updateSentMessage();
        updateReceivedMessage();
        util.showLog(TAG,"updateTextViews(): Updated all TextViews!");
    }

    /* Bluetooth BroadcastReceiver methods for bluetooth functionalities. */
    private BroadcastReceiver mBroadcastReceiver5 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            BluetoothDevice mDevice = intent.getParcelableExtra("Device");
            String status = intent.getStringExtra("Status");
            if(status.equals("connected")){
                try {
                    // progressDialog.dismiss();
                } catch(NullPointerException e){
                    e.printStackTrace();
                }
                String msg = "Tablet is now connected to " + mDevice.getName();
                displayToast(msg);
                editorConn.putString("connStatus", "Connected to " + mDevice.getName());
            }
            else if(status.equals("disconnected")){
                String msg = "Disconnected from " + mDevice.getName();
                displayToast(msg);
                mBluetoothConnection = new BluetoothConnService(RobotActivity.this);
                mBluetoothConnection.start();
                editorConn.putString("connStatus", "Disconnected");
            }
            editorConn.commit();
            connStatus = sharedPreferencesConn.getString("connStatus", "");
            connStatusTextView.setText(connStatus);
        }
    };

    // for receiving from bluetooth
    BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("receivedMessage");
            String arr_queue[] = new String[5];
            String mess_new = "";
            int index = 0;
            boolean flag = false;
            for (int i = 0; i < message.length(); i++) {

                char b = message.charAt(i);
                if (b == '{') {
                    flag = true;
                }
                if (b == '}') {
                    flag = false;
                    mess_new = mess_new + '}';

                    arr_queue[index] = mess_new;
                    mess_new = "";
                    index++;
                }
                if (flag == true) {
                    mess_new = mess_new + b;
                }
            }
            for(int i=0;i<index;i++) {
                message = arr_queue[i];
                util.showLog(TAG, "receivedMessage: message --- " + message);
                if (message.charAt(0) == '{') {
                    JSONObject parsedMessage = util.parseMDFString(message);

                    try {
                        gridMap.setRcveJsonObject(parsedMessage);
                        gridMap.updateMapInfo();
                        util.showLog(TAG, "messageReceiver: try decode successful");
                    } catch (JSONException e) {
                        util.showLog(TAG, "messageReceiver: try decode unsuccessful");
                    }
                } else if (message.equals("stope")) {
                    exploreToggleBtn.setChecked(false);
                    exploreToggleBtn.setEnabled(false);
                    setWaypointToggleBtn.setEnabled(true);
                    fastestToggleBtn.setEnabled(true);
                    resetMapBtn.setEnabled(false);
                    setStartPointToggleBtn.setEnabled(false);
                    displayExploreDialog();

                } else if (message.equals("stopf")) {
                    exploreToggleBtn.setEnabled(true);
                    exploreToggleBtn.setChecked(false);

                    fastestToggleBtn.setChecked(false);
                    resetMapBtn.setEnabled(true);
                    setStartPointToggleBtn.setEnabled(true);
                    setWaypointToggleBtn.setEnabled(true);
                    displayFastestDialog();
                }

                String receivedText = robotSharedPrefs.getString("receivedText", "") + "\n " + message;
                editor.putString("receivedText", receivedText);
                editor.commit();
                updateReceivedMessage();
            }
        }
    };


    /* onResume: When the activity resumes, register the bluetooth connection status. */
    @Override
    protected void onResume(){
        super.onResume();
        try{
            // Broadcast whenever the state of the bluetooth changes (e.g. connected, disconnected)
            IntentFilter filter2 = new IntentFilter("ConnectionStatus");
            LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver5, filter2);
        } catch(IllegalArgumentException e){
            e.printStackTrace();
        }
    }

    /* onDestroy: Unregister all receivers. */
    @Override
    protected void onDestroy(){
        super.onDestroy();
        try{
            LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver);
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver5);
        } catch(IllegalArgumentException e){
            e.printStackTrace();
        }
    }

    /* Display the dialog for Exploration. */
    public void displayExploreDialog()
    {
        // Setting message manually and performing action on button click
        String final_str = "MDF String Explored:" + Util.final_mdf_string_explored + " \n" + "MDF String Obstacle:" + Util.final_mdf_string_obstacle+" \n";
        for(int i=0;i<5;i++)
        {
            if(gridMap.image_type[i]!=-99)
            {
                final_str=final_str+"Image "+(i+1)+ " ID:"+gridMap.image_type[i]+", X:"+gridMap.image_x_coordinate[i]+" Y:"+gridMap.image_y_coordinate[i]+"\n";
            }
        }
        new MaterialAlertDialogBuilder(this, R.style.CustomAlertDialogTheme)
                .setTitle(" Exploration Finished!")
                .setMessage(final_str)
                .setPositiveButton("Dismiss", /* listener = */ null)
                .show();

    }

    /* Display the dialog for Fastest Path. */
    public void displayFastestDialog()
    {
        // Setting message manually and performing action on button click
        new MaterialAlertDialogBuilder(this, R.style.CustomAlertDialogTheme)
                .setTitle(" Fastest Path Finished!")
                .setPositiveButton("Dismiss", /* listener = */ null)
                .show();
    }

    /* Method to perform haptic. */
    public void performHaptic()
    {
        getWindow().getDecorView().performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
    }

    /* Method to check valid time. */
    public boolean check_valid_time(String str)
    {
        return(true);
    }

    /* Shortcut method to display Toast messages. */
    private void displayToast(String txtMsg) {
        Toast.makeText(getApplicationContext(), txtMsg, Toast.LENGTH_SHORT).show();
    }
}
