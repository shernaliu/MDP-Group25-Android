package com.example.mdp_group25;

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
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;



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

    GridMap gridMap;
    ToggleButton explore, fastest;
    TextView robotStatusTextView;
    ImageButton moveForwardImageBtn, turnRightImageBtn, turnLeftImageBtn;
    Button reset_map;
    ToggleButton set_starting_point, set_way_point;
    TextView xAxisTextView, yAxisTextView, directionAxisTextView, xAxisTextViewWP, yAxisTextViewWP;
    TextView receivedP1, receivedP2;
    TextView receivedImg1, receivedImg2, receivedImg3, receivedImg4, receivedImg5;
    ImageButton directionChangeImageBtn, exploredImageBtn, obstacleImageBtn, clearImageBtn;
    ToggleButton auto_manual_switch;
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
        gridMap = new GridMap(
                this);

        // find all view by id
        gridMap = findViewById(R.id.mapView);
        explore = findViewById(R.id.explore);
        fastest = findViewById(R.id.fastest);
        robotStatusTextView = findViewById(R.id.robotStatusTextView);
        reset_map = findViewById(R.id.reset_map);
        set_starting_point = findViewById(R.id.set_starting_point);
        set_way_point = findViewById(R.id.set_way_point);
        xAxisTextView = findViewById(R.id.xAxisTextView);
        yAxisTextView = findViewById(R.id.yAxisTextView);
        directionAxisTextView = findViewById(R.id.directionAxisTextView);
        directionChangeImageBtn = findViewById(R.id.directionChangeImageBtn);
        exploredImageBtn = findViewById(R.id.exploredImageBtn);
        obstacleImageBtn = findViewById(R.id.obstacleImageBtn);
        clearImageBtn = findViewById(R.id.clearImageBtn);
        connStatusTextView = findViewById(R.id.connStatusTextView);
        xAxisTextViewWP = findViewById(R.id.xAxisTextViewWP);
        yAxisTextViewWP = findViewById(R.id.yAxisTextViewWP);
/*        receivedP1 = findViewById(R.id.receivedP1);
        receivedP2 = findViewById(R.id.receivedP2);
        receivedImg1 = findViewById(R.id.receivedImg1);
        receivedImg2 = findViewById(R.id.receivedImg2);
        receivedImg3 = findViewById(R.id.receivedImg3);
        receivedImg4 = findViewById(R.id.receivedImg4);
        receivedImg5 = findViewById(R.id.receivedImg5);
        tiltSwitch = findViewById(R.id.tiltSwitch);

        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, new IntentFilter("incomingMessage"));

        sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        tiltSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);


        if (sharedPreferencesConn.contains("connStatus"))
            connStatus = sharedPreferencesConn.getString("connStatus", "");

        connStatusTextView.setText(connStatus);
*/

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
                        Util.showLog(TAG,"device lifted up");
                        //gridMap.moveRobot("forward");
                        //refreshLabel();
                        showToast("device lifted up");
                        gridMap.moveRobot("forward");
                        //refreshLabel();
                        util.printMessage(context,"Af");
                        return;
                    } else if (event.values[1] > 2.5f +delta-1) {
                        Util.showLog(TAG,"device lowered");
                        //gridMap.moveRobot("back");
                        //refreshLabel();
                        showToast("device lowered");
                        gridMap.moveRobot("back");
                        //refreshLabel();
                        util.printMessage(context,"r");
                        return;
                    }
                    if (event.values[0] > 5.5){ //1.5+5 = 6.5
                        Util.showLog(TAG,"turned left");
                        showToast("turned left");
                        if (turnedLeft){
                            turnedLeft = false;
                        }
                        else {
                            gridMap.moveRobot("left");
                            //refreshLabel();
                            util.printMessage(context,"Al");
                            turnedLeft = true;
                            return;
                        }
                    } else if (event.values[0] < -5.5) {//1.5-5 = -3.5
                        Util.showLog(TAG,"turned right");
                        showToast("turned right");
                        if (turnedRight){
                            turnedRight = false;
                        }
                        else {
                            gridMap.moveRobot("right");
                            //refreshLabel();
                            util.printMessage(context,"Ar");
                            turnedRight = true;
                            return;
                        }
                    }
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        };

//        sensorManager.registerListener(tiltSensorListener,tiltSensor, SensorManager.SENSOR_DELAY_NORMAL);
//        sensorManager.registerListener(tiltSensorListener,tiltSensor, 1000000);


/*
        manualUpdateBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                util.printMessage(context, "sendArena");
            }
        });
*/

        explore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                util.showLog(TAG, "Clicked explore");
                Button explore = (Button) view;
                if (explore.getText().equals("EXPLORE")) {
                    //end exploration
                    util.printMessage(context, "Xg");
                    util.printMessage(context, "Ag");
                }
                else if (explore.getText().equals("STOP")) {
                    //start exploration
                    util.printMessage(context, "Xe");
                }
                else {
                    showToast("Else statement: " + explore.getText());
                }
                util.showLog(TAG,"Exiting explore");
            }
        });


        fastest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                util.showLog(TAG, "Clicked fastest");
                Button fastest = (Button) view;
                if (fastest.getText().equals("FASTEST")) {
                    //end fastest path
                    util.printMessage(context, "Xg");
                    util.printMessage(context, "Ag");
                }
                else if (fastest.getText().equals("STOP")) {
                    //start fastest path
                    util.printMessage(context, "Xs");
                }
                else
                    showToast(fastest.getText().toString());
                util.showLog(TAG, "Exiting fastest");
            }
        });


        //MAP DETAILS
        reset_map.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                util.showLog(TAG,"Clicked reset_map");
                showToast("Map Reset in progress...");
                gridMap.resetMap();
/*              receivedP1.setText("P1: ");
                receivedP2.setText("P2: ");
                receivedImg1.setText("image1: ");
                receivedImg2.setText("image2: ");
                receivedImg3.setText("image3: ");
                receivedImg4.setText("image4: ");
                receivedImg5.setText("image5: ");*/
                refreshLabel();
            }
        });

        set_starting_point.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                util.showLog(TAG,"Clicked set_starting_point");
                if (set_starting_point.getText().equals("Set Start Point"))
                    showToast("Cancelled selecting starting point");
                else if (set_starting_point.getText().equals("CANCEL") && !gridMap.getAutoUpdate()) {
                    showToast("Please select starting point");
                    gridMap.setStartCoordStatus(true);
                    gridMap.toggleCheckedBtn("set_starting_point");
                    refreshLabel();
                } else
                    showToast("Please select manual mode");
                util.showLog(TAG,"Exiting set_starting_point");
            }
        });

        set_way_point.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                util.showLog(TAG, "Clicked set_way_point");
                if (set_way_point.getText().equals("Set Way Point"))
                    showToast("Cancelled selecting waypoint");
                else if (set_way_point.getText().equals("CANCEL")) {
                    showToast("Please select waypoint");
                    gridMap.setWaypointStatus(true);
                    gridMap.toggleCheckedBtn("set_way_point");
                }
                else
                    showToast("Please select manual mode");
                util.showLog(TAG, "Exiting set_way_point");
            }
        });

        exploredImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
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

    private void updateStatus(String message) {
        robotStatusTextView.setText(message);
    }
/*
       private void updateImgString(){
            System.out.println("UPDATE IMAGE");
            System.out.println(sharedPreferences.getString("image1",""));

            String image1 = "image1: " + reformatImgString(sharedPreferences.getString("image1",""));
            receivedImg1.setText(image1);
            String image2 = "image2: " + reformatImgString(sharedPreferences.getString("image2",""));
            receivedImg2.setText(image2);
            String image3 = "image3: " + reformatImgString(sharedPreferences.getString("image3",""));
            receivedImg3.setText(image3);
            String image4 = "image4: " + reformatImgString(sharedPreferences.getString("image4",""));
            receivedImg4.setText(image4);
            String image5 = "image5: " + reformatImgString(sharedPreferences.getString("image5",""));
            receivedImg5.setText(image5);
        }

        private String reformatImgString(String imageString){
            String imageX = "";
            String imageY = "";
            String imageType = "";
            if (imageString.length() == 0){
                return "";
            }
            if (imageString.charAt(0) == '0')
            { imageX = imageString.substring(1,2);}
            else
            { imageX = imageString.substring(0,2);}
            if (imageString.charAt(2) == '0')
            { imageY = imageString.substring(3,4);}
            else
            { imageY = imageString.substring(2,4);}
            if (imageString.charAt(4) == '0')
            { imageType = imageString.substring(5,6);}
            else
            { imageType = imageString.substring(4,6);}
            String finalString = "(".concat(imageType).concat(",").concat(imageX).concat(",").concat(imageY).concat(")");
            return finalString;
        }*/
    // for refreshing all the label in the screen
    private void refreshLabel() {
        util.showLog(TAG,"Entering Refresh Label");
        xAxisTextView.setText(String.valueOf(gridMap.getCurCoord()[0]+1));
        yAxisTextView.setText(String.valueOf(gridMap.getCurCoord()[1]+1));
        xAxisTextViewWP.setText(String.valueOf(gridMap.getWaypointCoord()[0]+1));
        yAxisTextViewWP.setText(String.valueOf(gridMap.getWaypointCoord()[1]+1));
        String direction = sharedPreferences.getString("direction", "");
        directionAxisTextView.setText(sharedPreferences.getString("direction",""));
        //updateP1P2();

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
            if(message.charAt(0) == 'q'){
                util.printMessage(context, "XK");
            }
            if(message.charAt(0) == '{'){
                JSONObject parsedMessage = util.parseMDFString(message);
                try {
                    if(parsedMessage.has("map")){
                        editor.putString("explored", parsedMessage.getJSONObject("map").getString("explored"));
                        System.out.println("INSIDE EXPLORE");
                        editor.putString("obstacle", parsedMessage.getJSONObject("map").getString("obstacle"));
                        System.out.println("INSIDE OBSTACLE");


                    }

                    if (parsedMessage.has("image")){
                        util.showLog(TAG,"found image info");
                        JSONArray images = parsedMessage.getJSONArray("image");
                        JSONObject image;
                        //max 5 image
                        for(int j=1; j<= images.length(); j++) {
                            util.showLog(TAG, "image number"+j);
                            image = images.getJSONObject(j-1);
                            String imageString = image.getString("imageString");
                            editor.putString("image"+j,imageString);
                        }
                        util.printMessage(context, "XI");
                    }
                    gridMap.setReceivedJsonObject(parsedMessage);
                    gridMap.updateMapInformation();
                    util.showLog(TAG,"messageReceiver: try decode successful");
                } catch (JSONException e) {
                    util.showLog(TAG, "messageReceiver: try decode unsuccessful");
                }
            }
            String receivedText = sharedPreferences.getString("receivedText", "") + "\n " + message;
            editor.putString("receivedText", receivedText);
            editor.commit();
            /*updateP1P2();
            updateImgString();*/
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


    //logging & status display

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

}
