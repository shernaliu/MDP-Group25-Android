package com.example.mdp_group25;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import org.json.JSONException;
import org.json.JSONObject;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.concurrent.TimeUnit;


public class RobotControlActivity extends AppCompatActivity {

    private static SharedPreferences sharedPreferences;
    private static SharedPreferences.Editor editor;
    private static SharedPreferences.Editor editorConn;
    BluetoothConnectionService mBluetoothConnection;
    private static Context context;
    private static SharedPreferences pref;
    private static boolean autoUpdate = false;
    private static boolean tiltControl = false;
    private static String TAG="ROBOT_CONTROL_ACTIVITY";
    private Util util = new Util();

    boolean turnedLeft = false;
    boolean turnedRight = false;
    boolean goStraight = false;
    boolean goback = false;
    GridMap gridMap;
    ToggleButton exploreToggleBtn, fastestToggleBtn;
    TextView robotStatusTextView;
    ImageButton moveForwardImageBtn, turnRightImageBtn, moveBackwardImageBtn, turnLeftImageBtn;
    Button resetMapBtn;
    ToggleButton setStartPointToggleBtn, setWaypointToggleBtn;
    TextView xAxisTextView, yAxisTextView, directionAxisTextView, sentMessage, receivedMessage, xAxisTextViewWP, yAxisTextViewWP;
    ImageButton directionChangeImageBtn, exploredImageBtn, obstacleImageBtn, clearImageBtn;
    ToggleButton manualAutoToggleBtn;
    Button buttonF1;
    Button buttonF2;
    Button manualUpdateBtn;
    SharedPreferences sharedPreferencesConn;
    TextView connStatusTextView;
    String connStatus;
    Switch tiltSwitch;
    SensorManager sensorManager;
    Sensor tiltSensor;
    SensorEventListener tiltSensorListener;
    String current_command = "";
    String mdf_string_final = "";
    View rectangleBgManual;
    View rectangleBgAuto;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_robot_control);


        //shared Preferences
        sharedPreferences = getApplicationContext().getSharedPreferences("RobotControlActivity", Context.MODE_PRIVATE);
        sharedPreferencesConn = getApplicationContext().getSharedPreferences("Shared Preferences", Context.MODE_PRIVATE);
        pref = getApplicationContext().getSharedPreferences("CommunicationsPreferences", Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
        editorConn = sharedPreferencesConn.edit();

        // create a new map
        gridMap = new GridMap(this);

        // find all view by id
        gridMap = findViewById(R.id.mapView);
        exploreToggleBtn = findViewById(R.id.exploreToggleBtn);
        fastestToggleBtn = findViewById(R.id.fastestToggleBtn);
        robotStatusTextView = findViewById(R.id.robotStatusTextView);
        moveForwardImageBtn = findViewById(R.id.moveForwardImageBtn);
        turnRightImageBtn = findViewById(R.id.turnRightImageBtn);
        moveBackwardImageBtn = findViewById(R.id.moveBackwardImageBtn);
        turnLeftImageBtn = findViewById(R.id.turnLeftImageBtn);
        resetMapBtn = findViewById(R.id.resetMapBtn);
        setStartPointToggleBtn = findViewById(R.id.setStartPointToggleBtn);
        setWaypointToggleBtn = findViewById(R.id.setWaypointToggleBtn);
        xAxisTextView = findViewById(R.id.xAxisTextView);
        yAxisTextView = findViewById(R.id.yAxisTextView);
        directionAxisTextView = findViewById(R.id.directionAxisTextView);
        directionChangeImageBtn = findViewById(R.id.directionChangeImageBtn);
        exploredImageBtn = findViewById(R.id.exploredImageBtn);
        obstacleImageBtn = findViewById(R.id.obstacleImageBtn);
        clearImageBtn = findViewById(R.id.clearImageBtn);
        manualAutoToggleBtn = findViewById(R.id.manualAutoToggleBtn);
        buttonF2 = findViewById(R.id.buttonF2);
        buttonF1 = findViewById(R.id.buttonF1);
        manualUpdateBtn = findViewById(R.id.manualUpdateBtn);
        connStatusTextView = findViewById(R.id.connStatusTextView);
        sentMessage = findViewById(R.id.sentMessage);
        receivedMessage = findViewById(R.id.receivedMsg);
        xAxisTextViewWP = findViewById(R.id.xAxisTextViewWP);
        yAxisTextViewWP = findViewById(R.id.yAxisTextViewWP);
        rectangleBgManual = findViewById(R.id.rectangle_bg_manual);
        rectangleBgAuto = findViewById(R.id.rectangle_bg_auto);

        sentMessage.setMovementMethod(new ScrollingMovementMethod());
        receivedMessage.setMovementMethod(new ScrollingMovementMethod());
        tiltSwitch = findViewById(R.id.tiltSwitch);

        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, new IntentFilter("incomingMessage"));

        sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        tiltSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);




        if (sharedPreferencesConn.contains("connStatus"))
            connStatus = sharedPreferencesConn.getString("connStatus", "");

        connStatusTextView.setText(connStatus);


        final android.os.Handler customHandler = new android.os.Handler();
        Runnable runthisthread = new Runnable()
        {
            public void run()
            {
                current_command = "";
                customHandler.postDelayed(this, 2000);
            }
        };
        customHandler.postDelayed(runthisthread, 0);


        tiltSensorListener = new SensorEventListener() {

            @Override
            public void onSensorChanged(SensorEvent event) {

                if (tiltControl){
                    float[] rotationMatrix = new  float[16];
                    SensorManager.getRotationMatrixFromVector(rotationMatrix,event.values);
                    //z value
                    //default value = 9.81 (0-(-9.81))
                    float delta = 6;
                    if (event.values[1] < 2.5f-delta){
                        // Sensor is tilted forward
                        if(!current_command.equals("Forward"))
                        {
                            Util.showLog(TAG, "device lifted up");
                            gridMap.moveRobot("forward");
                            refreshLabel();
                            showToast("device lifted up");
                            util.printMessage(context, "AR>w");
                            current_command = "Forward";
                            return;
                        }
                    }
                    else if (event.values[1] > 2.5f +delta-1) {
                        if(!current_command.equals("Backward"))
                        {
                        Util.showLog(TAG, "device lowered");
                        gridMap.moveRobot("back");
                        refreshLabel();
                        showToast("device lowered");
                        util.printMessage(context, "AR>s");
                        current_command = "Backward";
                        return;
                    }
                    }
                    if (event.values[0] > 5.5){ //1.5+5 = 6.5
                        if(!current_command.equals("Left"))
                        {
                            if (turnedLeft) {
                                turnedLeft = false;
                            }
                            else {
                                Util.showLog(TAG, "turned left");
                                showToast("turned left");
                                gridMap.moveRobot("left");
                                refreshLabel();
                                util.printMessage(context, "AR>a");
                                turnedLeft = true;
                                current_command = "Left";
                                return;
                            }
                        }
                    } else if (event.values[0] < -5.5) {//1.5-5 = -3.5
                        if(!current_command.equals("Right")) {
                            if (turnedRight){
                                turnedRight = false;
                            }
                            else {
                                Util.showLog(TAG, "turned right");
                                showToast("turned right");
                                gridMap.moveRobot("right");
                                refreshLabel();
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

            }
        };
        sensorManager.registerListener(tiltSensorListener,tiltSensor, 3000000);




        manualUpdateBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                util.printMessage(context, "AL>sa");
            }
        });

        // TODO: INTEGRATE WITH ALGO
        exploreToggleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                perform_haptic();
                util.showLog(TAG, "Clicked exploreToggleBtn");
                Button exploreToggleBtn = (Button) view;
                if (exploreToggleBtn.getText().equals("EXPLORE")) {
                    //end exploration
                    util.printMessage(context, "AL>st");
                }
                else if (exploreToggleBtn.getText().equals("STOP")) {
                    //start exploration
                    util.printMessage(context, "AL>ex");
                }
                else {
                    showToast("Else statement: " + exploreToggleBtn.getText());
                }
                util.showLog(TAG,"Exiting exploreToggleBtn");
            }
        });


        fastestToggleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                perform_haptic();
                util.showLog(TAG, "Clicked fastestToggleBtn");
                Button fastestToggleBtn = (Button) view;
                if (fastestToggleBtn.getText().equals("FASTEST")) {
                    //end fastest path
                    util.printMessage(context, "AL>st");
                }
                else if (fastestToggleBtn.getText().equals("STOP")) {
                    //start fastest path
                    util.printMessage(context, "AL>fp");
                }
                else
                    showToast(fastestToggleBtn.getText().toString());
                util.showLog(TAG, "Exiting fastestToggleBtn");
            }
        });


        //ROBOT MOVEMENT
        moveForwardImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                perform_haptic();
                util.showLog(TAG,"Clicked moveForwardImageBtn");
                if (gridMap.getAutoUpdate())
                    updateStatus(Status.W2);
                else if (gridMap.getCanDrawRobot() && !gridMap.getAutoUpdate()) {
                    gridMap.moveRobot("forward");
                    if (gridMap.getValidPosition())
                        updateStatus(Status.MF);
                    else
                        updateStatus(Status.UF);
                    util.printMessage(context, "AR>w");
                    refreshLabel();
                }
                else
                    updateStatus(Status.W1);
                util.showLog(TAG, "Exiting moveForwardImageBtn");
            }
        });

        turnRightImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                perform_haptic();
                util.showLog(TAG,"Clicked turnRightImageBtn");
                if (gridMap.getAutoUpdate())
                    updateStatus(Status.W2);
                else if (gridMap.getCanDrawRobot() && !gridMap.getAutoUpdate()) {
                    gridMap.moveRobot("right");
                    updateStatus(Status.TR);
                    util.printMessage(context, "AR>d");
                    refreshLabel();
                }
                else
                    updateStatus(Status.W1);
                util.showLog(TAG,"Exiting turnRightImageBtn");
            }
        });

        moveBackwardImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                perform_haptic();
                util.showLog(TAG,"Clicked moveBackwardImageBtn");
                if (gridMap.getAutoUpdate())
                    updateStatus(Status.W2);
                else if (gridMap.getCanDrawRobot() && !gridMap.getAutoUpdate()) {
                    gridMap.moveRobot("back");
                    refreshLabel();
                    if (gridMap.getValidPosition())
                        updateStatus(Status.MB);
                    else
                        updateStatus(Status.UB);
                    util.printMessage(context, "r");
                    refreshLabel();
                }
                else
                    updateStatus(Status.W1);
                util.showLog(TAG,"Exiting moveBackwardImageBtn");
            }
        });

        turnLeftImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                perform_haptic();
                util.showLog(TAG, "Clicked turnLeftImageBtn");
                if (gridMap.getAutoUpdate())
                    updateStatus(Status.W2);
                else if (gridMap.getCanDrawRobot() && !gridMap.getAutoUpdate()) {
                    gridMap.moveRobot("left");
                    refreshLabel();
                    updateStatus(Status.TL);
                    util.printMessage(context,"AR>a");
                    refreshLabel();
                }
                else
                    updateStatus(Status.W1);
                util.showLog(TAG, "Exiting turnLeftImageBtn");
            }
        });

        buttonF1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                perform_haptic();
                util.showLog(TAG,"Clicked buttonF1");
                String firstFunction = pref.getString(FunctionsActivity.functionOne, "");
                if(firstFunction != ""){
                    util.printMessage(context, firstFunction);
                    refreshLabel();
                }
            }
        });

        buttonF2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                perform_haptic();
                util.showLog(TAG,"Clicked buttonF2");
                String secondFunction = pref.getString(FunctionsActivity.functionTwo, "");
                if(secondFunction != ""){
                    util.printMessage(context, secondFunction);
                    refreshLabel();
                }
            }
        });

        //MAP DETAILS
        resetMapBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                perform_haptic();
                util.showLog(TAG,"Clicked resetMapBtn");
                showToast("Reseting map...");
                gridMap.resetMap();
                refreshLabel();
            }
        });

        setStartPointToggleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                perform_haptic();
                util.showLog(TAG,"Clicked setStartPointToggleBtn");
                if (setStartPointToggleBtn.getText().equals("Set Start Point"))
                    showToast("Cancelled selecting starting point");
                else if (setStartPointToggleBtn.getText().equals("CANCEL") && !gridMap.getAutoUpdate()) {
                    showToast("Please select starting point");
                    gridMap.setStartCoordStatus(true);
                    gridMap.toggleCheckedBtn("setStartPointToggleBtn");
                    refreshLabel();
                } else
                    showToast("Please select manual mode");
                util.showLog(TAG,"Exiting setStartPointToggleBtn");
            }
        });

        setWaypointToggleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                perform_haptic();
                util.showLog(TAG, "Clicked setWaypointToggleBtn");
                if (setWaypointToggleBtn.getText().equals("Set Way Point"))
                    showToast("Cancelled selecting waypoint");
                else if (setWaypointToggleBtn.getText().equals("CANCEL")) {
                    showToast("Please select waypoint");
                    gridMap.setWaypointStatus(true);
                    gridMap.toggleCheckedBtn("setWaypointToggleBtn");
                }
                else
                    showToast("Please select manual mode");
                util.showLog(TAG, "Exiting setWaypointToggleBtn");
            }
        });

        exploredImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                perform_haptic();
                util.showLog(TAG,"Clicked exploredImageBtn");
                if (!gridMap.getExploredStatus()) {
                    showToast("Please check cell");
                    gridMap.setExploredStatus(true);
                    gridMap.toggleCheckedBtn("exploredImageBtn");
                }
                else if (gridMap.getExploredStatus())
                    gridMap.setSetObstacleStatus(false);
                util.showLog(TAG,"Exiting exploredImageBtn");
            }
        });

        obstacleImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                util.showLog(TAG,"Clicked obstacleImageBtn");
                if (!gridMap.getSetObstacleStatus()) {
                    showToast("Please plot obstacles");
                    gridMap.setSetObstacleStatus(true);
                    gridMap.toggleCheckedBtn("obstacleImageBtn");
                }
                else if (gridMap.getSetObstacleStatus())
                    gridMap.setSetObstacleStatus(false);
                util.showLog(TAG,"Exiting obstacleImageBtn");
            }
        });

        clearImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                util.showLog(TAG,"Clicked clearImageBtn");
                if (!gridMap.getUnSetCellStatus()) {
                    showToast("Please remove cells");
                    gridMap.toggleCheckedBtn("clearImageBtn");
                    gridMap.setUnSetCellStatus(true);
                }
                else if (gridMap.getUnSetCellStatus())
                    gridMap.setUnSetCellStatus(false);
                util.showLog(TAG,"Exiting clearImageBtn");
            }
        });


        // when manual auto button clicked
        manualAutoToggleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                perform_haptic();
                util.showLog(TAG,"Clicked manualAutoToggleBtn");
                if (manualAutoToggleBtn.getText().equals("AUTO")) {
                    try {
                        gridMap.setAutoUpdate(true);
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
                    showToast("AUTO mode");
                }
                else if (manualAutoToggleBtn.getText().equals("MANUAL")) {
                    try {
                        gridMap.setAutoUpdate(false);
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

                    showToast("MANUAL mode");
                }
                util.showLog(TAG,"Exiting manualAutoToggleBtn");
            }
        });

        tiltSwitch.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                if (tiltControl){
                    tiltControl = false;
                    showToast("Tilt Control Disabled");
                }
                else{
                    tiltControl = true;
                    showToast("Tilt Control Enabled");
                }
            }
        });

        //test
        RobotControlActivity.context = getApplicationContext();
        ProgressDialog myDialog;

        myDialog = new ProgressDialog(RobotControlActivity.this);
        myDialog.setMessage("Waiting for other device to reconnect...");
        myDialog.setCancelable(false);
        myDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        try{
            //Broadcasts when bluetooth state changes (connected, disconnected etc) custom receiver
            IntentFilter filter2 = new IntentFilter("ConnectionStatus");
            LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver5, filter2);
        } catch(IllegalArgumentException e){
            e.printStackTrace();
        }
    }


    private void updateSentMessage(){
        String sent = sharedPreferences.getString("sentText", "");
        if(sent.length()>0){
            sentMessage.setText(sent);
        } else{
            sentMessage.setText("no sent message");
        }
    }

    private void updateReceivedMessage(){
        String received = sharedPreferences.getString("receivedText", "");
        if(received.length()>0){
            receivedMessage.setText(received);
        } else{
            receivedMessage.setText("no received message");
        }
    }

    private void updateStatus(String message) {
        robotStatusTextView.setText(message);
    }

    // for refreshing all the label in the screen
    private void refreshLabel() {
        util.showLog(TAG,"Entering Refresh Label");
        xAxisTextView.setText(String.valueOf(gridMap.getCurCoord()[0]+1));
        yAxisTextView.setText(String.valueOf(gridMap.getCurCoord()[1]+1));
        xAxisTextViewWP.setText(String.valueOf(gridMap.getWaypointCoord()[0]+1));
        yAxisTextViewWP.setText(String.valueOf(gridMap.getWaypointCoord()[1]+1));
        String direction = sharedPreferences.getString("direction", "");
        directionAxisTextView.setText(sharedPreferences.getString("direction",""));
        updateSentMessage();
        updateReceivedMessage();
        util.showLog(TAG,"Exiting Refresh Label");
    }

    // for bluetooth
    private BroadcastReceiver mBroadcastReceiver5 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            BluetoothDevice mDevice = intent.getParcelableExtra("Device");
            String status = intent.getStringExtra("Status");

            if(status.equals("connected")){
                //When the device reconnects, this broadcast will be called again to enter CONNECTED if statement
                //must dismiss the previous dialog that is waiting for connection if not it will block the execution
                try {
                    //myDialog.dismiss();
                } catch(NullPointerException e){
                    e.printStackTrace();
                }

                Toast.makeText(RobotControlActivity.this, "Device now connected to "+mDevice.getName(), Toast.LENGTH_LONG).show();
                editorConn.putString("connStatus", "Connected to " + mDevice.getName());
            }
            else if(status.equals("disconnected")){
                Toast.makeText(RobotControlActivity.this, "Disconnected from "+mDevice.getName(), Toast.LENGTH_LONG).show();
                //start accept thread and wait on the SAME device again
                mBluetoothConnection = new BluetoothConnectionService(RobotControlActivity.this);
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
            util.showLog(TAG, "receivedMessage: message --- " + message);
            if(message.charAt(0) == '{'){
                JSONObject parsedMessage = util.parseMDFString(message);

                try {
                    gridMap.setReceivedJsonObject(parsedMessage);
                    gridMap.updateMapInformation();
                    util.showLog(TAG,"messageReceiver: try decode successful");
                } catch (JSONException e) {
                    util.showLog(TAG, "messageReceiver: try decode unsuccessful");
                }
            }
            else if(message.equals("stop"))
                showdialog();


            String receivedText = sharedPreferences.getString("receivedText", "") + "\n " + message;
            editor.putString("receivedText", receivedText);
            editor.commit();
            updateReceivedMessage();
        }
    };


    //register bluetooth connection status broadcast when the activity resumes
    @Override
    protected void onResume(){
        super.onResume();
        try{
            //Broadcasts when bluetooth state changes (connected, disconnected etc) custom receiver
            IntentFilter filter2 = new IntentFilter("ConnectionStatus");
            LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver5, filter2);
        } catch(IllegalArgumentException e){
            e.printStackTrace();
        }
    }

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

    public void showdialog()
    {

        //Setting message manually and performing action on button click
        String final_str="MDF String Explored:"+Util.final_mdf_string_explored+" \n"+"MDF String Obstacle:"+Util.final_mdf_string_obstacle+" \n";
        for(int i=0;i<5;i++)
        {
            if(gridMap.image_type[i]!=-99)
            {
                final_str=final_str+"Image "+(i+1)+ " ID:"+gridMap.image_type[i]+", X:"+gridMap.image_x_coordinate[i]+" Y:"+gridMap.image_y_coordinate[i]+"\n";
            }
        }
        new MaterialAlertDialogBuilder(this, R.style.CustomAlertDialogTheme)
                .setTitle("\t Exploration Finished!")
                .setMessage(final_str)
                .setPositiveButton("Dismiss", /* listener = */ null)
                .show();

    }
    public void perform_haptic()
    {
        getWindow().getDecorView().performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);

    }
    //logging & status display
    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

}
