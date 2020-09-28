package com.example.mdp_group25;


import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class BluetoothDeviceActivity extends AppCompatActivity {

    private static final String TAG = "BluetoothDevice";
    BluetoothAdapter mBluetoothAdapter;
    public ArrayList<BluetoothDevice> mBTDevices = new ArrayList<>();
    public Set<BluetoothDevice> mBTPairedDevices;
    public ArrayList<BluetoothDevice> mBTPairedDeviceArray = new ArrayList<>();
    public DeviceListAdapter mDeviceListAdapter;
    public DeviceListAdapter mPairedDeviceListAdapter;
    ListView lvNewDevices;
    ListView lvPairedDevices;

    BluetoothConnectionService mBluetoothConnection;

    Button btnStartConnection;
    Button btnDiscoverable;
    private static final UUID MY_UUID_INSECURE =
            UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

    BluetoothDevice mBTDevice;

    TextView connStatusTextView;
    private String connStatus;
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;
    String Raspberry_Pi_Physical_Address = "";
    // Create a BroadcastReceiver for ACTION_FOUND
    private final BroadcastReceiver mBroadcastReceiver1 = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (action.equals(mBluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, mBluetoothAdapter.ERROR);

                switch(state){
                    case BluetoothAdapter.STATE_OFF:
                        Log.d(TAG, "onReceive: STATE OFF");
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Log.d(TAG, "mBroadcastReceiver1: STATE TURNING OFF");
                        break;
                    case BluetoothAdapter.STATE_ON:
                        Log.d(TAG, "mBroadcastReceiver1: STATE ON");
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        Log.d(TAG, "mBroadcastReceiver1: STATE TURNING ON");
                        break;
                }
            }
        }
    };

    /**
     * Broadcast Receiver for changes made to bluetooth states such as:
     * 1) Discoverability mode on/off or expire.
     */
    private final BroadcastReceiver mBroadcastReceiver2 = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED)) {

                int mode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, BluetoothAdapter.ERROR);

                switch (mode) {
                    //Device is in Discoverable Mode
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
                        Log.d(TAG, "mBroadcastReceiver2: Discoverability Enabled.");
                        break;
                    //Device not in discoverable mode
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
                        Log.d(TAG, "mBroadcastReceiver2: Discoverability Disabled. Able to receive connections.");
                        break;
                    case BluetoothAdapter.SCAN_MODE_NONE:
                        Log.d(TAG, "mBroadcastReceiver2: Discoverability Disabled. Not able to receive connections.");
                        break;
                    case BluetoothAdapter.STATE_CONNECTING:
                        Log.d(TAG, "mBroadcastReceiver2: Connecting....");
                        break;
                    case BluetoothAdapter.STATE_CONNECTED:
                        Log.d(TAG, "mBroadcastReceiver2: Connected.");
                        break;
                }

            }
        }
    };




    /**
     * Broadcast Receiver for listing devices that are not yet paired
     * -Executed by btnDiscover() method.
     */
    private BroadcastReceiver mBroadcastReceiver3 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.d(TAG, "onReceive: ACTION FOUND.");

            if (action.equals(BluetoothDevice.ACTION_FOUND)){
                BluetoothDevice device = intent.getParcelableExtra (BluetoothDevice.EXTRA_DEVICE);
                mBTDevices.add(device);
                Log.d(TAG, "onReceive: " + device.getName() + ": " + device.getAddress());
                mDeviceListAdapter = new DeviceListAdapter(context, R.layout.device_adapter_view, mBTDevices);
                lvNewDevices.setAdapter(mDeviceListAdapter);
            }
        }
    };


    private final BroadcastReceiver mBroadcastReceiver4 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)){
                BluetoothDevice mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                //case1: bonded already
                if (mDevice.getBondState() == BluetoothDevice.BOND_BONDED){
                    Log.d(TAG, "BroadcastReceiver: BOND_BONDED.");
                }
                //case2: creating bond
                if (mDevice.getBondState() == BluetoothDevice.BOND_BONDING){
                    Log.d(TAG, "BroadcastReceiver: BOND_BONDING.");
                }
                //case3: breaking a bond
                if (mDevice.getBondState() == BluetoothDevice.BOND_NONE){
                    Log.d(TAG, "BroadcastReceiver: BOND_NONE.");
                }
            }
        }
    };

    /**
     * Broadcast receiver for bluetooth connection status
     * NOTE: failed to connect to device will display a toast message which is found in BluetoothConnectionService ConnectThread
     */
    private BroadcastReceiver mBroadcastReceiver5 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            BluetoothDevice mDevice = intent.getParcelableExtra("Device");
            String status = intent.getStringExtra("Status");
            sharedPreferences = getApplicationContext().getSharedPreferences("Shared Preferences", Context.MODE_PRIVATE);
            editor = sharedPreferences.edit();

            if(status.equals("connected")){
                //When the device reconnects, this broadcast will be called again to enter CONNECTED if statement
                //must dismiss the previous dialog that is waiting for connection if not it will block the execution

                Log.d(TAG, "mBroadcastReceiver5: Device now connected to "+mDevice.getName());
                Toast.makeText(BluetoothDeviceActivity.this, "Device now connected to "+mDevice.getName(), Toast.LENGTH_LONG).show();
                editor.putString("connStatus", "Connected to " + mDevice.getName());
                connStatusTextView.setText("Connected to " + mDevice.getName());
            }
            else if(status.equals("disconnected")){
                Log.d(TAG, "mBroadcastReceiver5: Disconnected from "+mDevice.getName());
                Toast.makeText(BluetoothDeviceActivity.this, "Disconnected from "+mDevice.getName(), Toast.LENGTH_LONG).show();
                //start accept thread and wait on the SAME device again
                mBluetoothConnection = new BluetoothConnectionService(BluetoothDeviceActivity.this);
                mBluetoothConnection.start();

                // For displaying disconnected for all page
                sharedPreferences = getApplicationContext().getSharedPreferences("Shared Preferences", Context.MODE_PRIVATE);
                editor = sharedPreferences.edit();
                editor.putString("connStatus", "Disconnected");
                TextView connStatusTextView = findViewById(R.id.connStatusTextView);
                connStatusTextView.setText("Disconnected");
                editor.commit();

            }
            editor.commit();
        }
    };

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy: called.");
        super.onDestroy();
        try{
            unregisterReceiver(mBroadcastReceiver1);
        }catch(IllegalArgumentException e) {
            Log.d(TAG, "unregister1 fail.");
            e.printStackTrace();
        }
        try{
            unregisterReceiver(mBroadcastReceiver2);
        }catch(IllegalArgumentException e) {
            Log.d(TAG, "unregister2 fail.");
            e.printStackTrace();
        }
        try{
            unregisterReceiver(mBroadcastReceiver3);
        }catch(IllegalArgumentException e) {
            Log.d(TAG, "unregister3 fail.");
            e.printStackTrace();
        }
        try{
            unregisterReceiver(mBroadcastReceiver4);
        }catch(IllegalArgumentException e) {
            Log.d(TAG, "unregister4 fail.");
            e.printStackTrace();
        }
        try{
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver5);
        }catch(IllegalArgumentException e) {
            Log.d(TAG, "unregister5 fail.");
            e.printStackTrace();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_device);
        Button btnONOFF = (Button) findViewById(R.id.switch1);
        lvNewDevices = (ListView) findViewById(R.id.lvNewDevices);
        lvPairedDevices = (ListView) findViewById(R.id.lvPairedDevices);
        mBTDevices = new ArrayList<>();

        btnStartConnection = (Button) findViewById(R.id.btnStartConnection);
        btnDiscoverable = (Button) findViewById(R.id.btnDiscoverable);

        //Broadcasts when bond state changes (ie:pairing)
        IntentFilter filter = new IntentFilter((BluetoothDevice.ACTION_BOND_STATE_CHANGED));
        registerReceiver(mBroadcastReceiver4, filter);

        //Broadcasts when bluetooth state changes (connected, disconnected etc) custom receiver
        IntentFilter filter2 = new IntentFilter("ConnectionStatus");
        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver5, filter2);


        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(mBluetoothAdapter.isEnabled())
            mBluetoothAdapter.disable();
        lvNewDevices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l){
                mBluetoothAdapter.cancelDiscovery();

                Log.d(TAG, "onItemClick: You clicked on a device");
                String deviceName = mBTDevices.get(i).getName();
                String deviceAddress = mBTDevices.get(i).getAddress();

                Log.d(TAG, "onItemClick: deviceName = "+ deviceName);
                Log.d(TAG, "onItemClick: deviceAddress = "+ deviceAddress);
                Raspberry_Pi_Physical_Address = deviceAddress;
                //create bond
                Log.d(TAG, "Trying to pair with "+ deviceName + " " + deviceAddress);
                mBTDevices.get(i).createBond();

                mBTDevice = mBTDevices.get(i);
                mBluetoothConnection = new BluetoothConnectionService(BluetoothDeviceActivity.this);
            }
        });

        lvPairedDevices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                mBluetoothAdapter.cancelDiscovery();

                Log.d(TAG, "onItemClick: You clicked on a device");
                String deviceName = mBTPairedDeviceArray.get(i).getName();
                String deviceAddress = mBTPairedDeviceArray.get(i).getAddress();

                Log.d(TAG, "onItemClick: deviceName = " + deviceName);
                Log.d(TAG, "onItemClick: deviceAddress = " + deviceAddress);

                mBTDevice = mBTPairedDeviceArray.get(i);
                mBluetoothConnection = new BluetoothConnectionService(BluetoothDeviceActivity.this);
            }
        });



        btnONOFF.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: enabling/disabling bluetooth.");
                enableDisableBT();
            }
        });

        btnStartConnection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startConnection();
            }
        });

        // for toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //added these 3 lines
        connStatusTextView = (TextView) findViewById(R.id.connStatusTextView);
        connStatus = "Disconnected";
        sharedPreferences = getApplicationContext().getSharedPreferences("Shared Preferences", Context.MODE_PRIVATE);

        if (sharedPreferences.contains("connStatus"))
            connStatus = sharedPreferences.getString("connStatus", "");

        connStatusTextView.setText(connStatus);

    }

    //create method for starting connection
    //***remember the connection will fail and app will crash if you haven't paired first
    public void startConnection(){
        startBTConnection(mBTDevice,MY_UUID_INSECURE);
    }


    public void startBTConnection(BluetoothDevice device, UUID uuid){
        Log.d(TAG, "startBTConnection: Initializing RFCOM Bluetooth Connection.");

        mBluetoothConnection.startClient(device,uuid);
    }



    public void enableDisableBT(){
        if(mBluetoothAdapter == null){
            Log.d(TAG, "enableDisableBT: Does not have BT capabilities.");
        }
        if(!mBluetoothAdapter.isEnabled()){
            Log.d(TAG, "enableDisableBT: enabling BT.");
            Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBTIntent);

            IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            registerReceiver(mBroadcastReceiver1, BTIntent);
        }
        if(mBluetoothAdapter.isEnabled()){
            Log.d(TAG, "enableDisableBT: disabling BT.");
            mBluetoothAdapter.disable();

            IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            registerReceiver(mBroadcastReceiver1, BTIntent);
        }

    }

    public void btnDiscoverable(View view) {
        Log.d(TAG, "btnDiscoverable: Making device discoverable for 300 seconds.");

        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        startActivity(discoverableIntent);

        IntentFilter intentFilter = new IntentFilter(mBluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        registerReceiver(mBroadcastReceiver2,intentFilter);

    }


    public void btnDiscover(View view) {
        Log.d(TAG, "btnDiscover: Looking for unpaired devices.");

        mBTDevices.clear();
        if(mBluetoothAdapter.isDiscovering()){
            mBluetoothAdapter.cancelDiscovery();
            Log.d(TAG, "btnDiscover: Canceling discovery.");

            mBluetoothAdapter.startDiscovery( );
            IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(mBroadcastReceiver3, discoverDevicesIntent);
        }
        if(!mBluetoothAdapter.isDiscovering()){

            mBluetoothAdapter.startDiscovery();
            IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(mBroadcastReceiver3, discoverDevicesIntent);
        }

        mBTPairedDevices = mBluetoothAdapter.getBondedDevices();
        for (BluetoothDevice mdevice : mBTPairedDevices)
            mBTPairedDeviceArray.add(mdevice);
        mPairedDeviceListAdapter = new DeviceListAdapter(this, R.layout.device_adapter_view, mBTPairedDeviceArray);
        lvPairedDevices.setAdapter(mPairedDeviceListAdapter);

    }

}