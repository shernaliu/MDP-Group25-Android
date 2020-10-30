package com.example.mdp_group25;

import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.Context;
import android.util.Log;
import android.app.ProgressDialog;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.UUID;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class BluetoothConnService {
    private static final String TAG = "BluetoothConnectionServ";
    private static final UUID MY_UUID_INSECURE = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    private static final String androidAppName = "MDP-Group25-Android";
    Context mContext;
    private final BluetoothAdapter mBluetoothAdapter;
    private AcceptThread mInsecureAcceptThread;
    private UUID deviceUUID;
    private ConnectThread mConnectThread;
    private BluetoothDevice mBluetoothDevice;
    private static ConnectedThread mConnectedThread;
    public static boolean BTConnectionStatus = false;
    Intent connectionStatus;
    ProgressDialog mProgressDialog;

    public BluetoothConnService(Context ctx) {
        mContext = ctx;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        start();
    }

    /* AcceptThread: The thread is able to run and listen to incoming connections until a connection is accepted or cancelled. */
    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mServerSocket;

        public AcceptThread() {
            BluetoothServerSocket btServerSocket = null;
            try {
                btServerSocket = mBluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(androidAppName, MY_UUID_INSECURE);
                Log.d(TAG, "AcceptThread(): Set up a server with UUID: " + MY_UUID_INSECURE);
            } catch (IOException exception) {
                Log.e(TAG, "AcceptThread: IOException caught: " + exception.getMessage());
            }
            mServerSocket = btServerSocket;
        }

        public void run() {
            Log.d(TAG, "run(): AcceptThread running...");
            BluetoothSocket btSocket = null;
            try {
                // Returns on successful connection or an exception
                Log.d(TAG, "run: Server socket is starting...");
                btSocket = mServerSocket.accept();
                Log.d(TAG, "run: Server socket has accepted connection.");
            } catch (IOException exception) {
                Log.e(TAG, "AcceptThread: IOException caught: " + exception.getMessage());
            }

            if (btSocket != null) {
                connected(btSocket, mBluetoothDevice);
            }

            Log.i(TAG, "END mAcceptThread ");
        }

        public void cancel() {
            Log.d(TAG, "cancel: Canceling AcceptThread..");
            try {
                mServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "cancel: Close of AcceptThread ServerSocket failed. " + e.getMessage());
            }
        }

    }

    /* ConnectThread: Runs and attempts an outgoing connection with the device. */
    private class ConnectThread extends Thread {
        private BluetoothSocket mBluetoothSocket;
        public ConnectThread(BluetoothDevice device, UUID uuid) {
            Log.d(TAG, "ConnectThread(): started!");
            mBluetoothDevice = device;
            deviceUUID = uuid;
        }

        public void run() {
            BluetoothSocket tmp = null;
            try {
                Log.d(TAG, "ConnectThread(): Trying to create InsecureRfcommSocket using UUID: "
                        + MY_UUID_INSECURE);
                tmp = mBluetoothDevice.createRfcommSocketToServiceRecord(deviceUUID);
            } catch (IOException e) {
                Log.e(TAG, "ConnectThread(): Could not create InsecureRfcommSocket " + e.getMessage());
            }

            mBluetoothSocket = tmp;

            // Cancel the bluetooth discovery as it slows down connection
            mBluetoothAdapter.cancelDiscovery();

            // Create a connection to the BluetoothSocket
            try {
                mBluetoothSocket.connect();
                Log.d(TAG, "run: ConnectThread() connected!");
                connected(mBluetoothSocket, mBluetoothDevice);
            } catch (IOException e) {
                // Attempt to close the bluetooth socket
                try {
                    mBluetoothSocket.close();
                    Log.d(TAG, "run: Closed Socket.");
                } catch (IOException ex) {
                    Log.e(TAG, "mConnectThread: run: Unable to close connection bluetooth socket " + ex.getMessage());
                }
                Log.d(TAG, "run: ConnectThread: Could not connect to the UUID: " + MY_UUID_INSECURE);
            }
            // Clsoe mProgressDialog when the connection has been established
            try {
                mProgressDialog.dismiss();
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
        }

        public void cancel() {
            try {
                Log.d(TAG, "Cancel(): Closing the client socket.");
                mBluetoothSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Cancel(): failed to close of mmSocket in Connectthread!!" + e.getMessage());
            }
        }
    }

    /* Start(): Start AcceptThread to begin session in listening server mode, which is caleld by onResume of activitty.*/
    public synchronized void start() {
        Log.d(TAG, "start");
        // If any thread attempts to connect, cancel
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
        if (mInsecureAcceptThread == null) {
            mInsecureAcceptThread = new AcceptThread();
            mInsecureAcceptThread.start();
        }
    }

    /* startClient(): starts and waits for a connection. */
    public void startClient(BluetoothDevice device, UUID uuid) {
        Log.d(TAG, "startClient: Started.");
        // init mProgressDialog
        mProgressDialog = ProgressDialog.show(mContext, "Connecting Bluetooth"
                , "Please Wait...", true);

        mConnectThread = new ConnectThread(device, uuid);
        mConnectThread.start();
    }

    /* ConnectedThread - Maintain Bluetooth connection, send and receive data from input and output streams. */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mSocket;
        private final InputStream mInputStream;
        private final OutputStream mOutputStream;

        public ConnectedThread(BluetoothSocket socket) {
            Log.d(TAG, "ConnectedThread(): Starting!");
            connectionStatus = new Intent("ConnectionStatus");
            connectionStatus.putExtra("Status", "connected");
            connectionStatus.putExtra("Device", mBluetoothDevice);
            LocalBroadcastManager.getInstance(mContext).sendBroadcast(connectionStatus);
            BTConnectionStatus = true;
            mSocket = socket;
            InputStream inputStream = null;
            OutputStream outputStream = null;
            try {
                inputStream = mSocket.getInputStream();
                outputStream = mSocket.getOutputStream();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            mInputStream = inputStream;
            mOutputStream = outputStream;
        }

        public void run() {
            // buffer store for the stream
            byte[] buffer = new byte[1024];
            int bytes; // bytes returned from read()

            // While loop to continuously listen to InputStream until exception occurs
            while (true) {
                // Reading from InputStream
                try {
                    bytes = mInputStream.read(buffer);
                    String incomingMessage = new String(buffer, 0, bytes);
                    Log.d(TAG, "InputStream: " + incomingMessage);
                    Intent incomingMessageIntent = new Intent("incomingMessage");
                    incomingMessageIntent.putExtra("receivedMessage", incomingMessage);
                    LocalBroadcastManager.getInstance(mContext).sendBroadcast(incomingMessageIntent);
                } catch (IOException e) {
                    Log.e(TAG, "write: Error reading Input Stream. " + e.getMessage());
                    connectionStatus = new Intent("ConnectionStatus");
                    connectionStatus.putExtra("Status", "disconnected");
                    connectionStatus.putExtra("Device", mBluetoothDevice);
                    LocalBroadcastManager.getInstance(mContext).sendBroadcast(connectionStatus);
                    BTConnectionStatus = false;
                    break;
                }
            }
        }

        /* Method for the main activity to send data to the device */
        public void write(byte[] bytes) {
            String text = new String(bytes, Charset.defaultCharset());
            Log.d(TAG, "write: Writing to outputstream: " + text);
            try {
                mOutputStream.write(bytes);
            } catch (IOException e) {
                Log.e(TAG, "write: Error writing to output stream. " + e.getMessage());
            }
        }

        /* Method for the main activity to close the connection */
        public void cancel() {
            try {
                mSocket.close();
            } catch (IOException e) {
            }
        }
    }

    /*
    * Connected(): The device information will be retrieved when we press on the connect button if the app initates the connection.
    * If the other device initiates instead, we need to retrieve device info from socket.
    * */
    private void connected(BluetoothSocket mmSocket, BluetoothDevice device) {
        Log.d(TAG, "connected: Starting.");
        if (device != null) {
            mBluetoothDevice = device;
        } else {
            mBluetoothDevice = mmSocket.getRemoteDevice();
        }
        if (mInsecureAcceptThread != null) {
            mInsecureAcceptThread.cancel();
            mInsecureAcceptThread = null;
        }
        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(mmSocket);
        mConnectedThread.start();
    }

    /* Write to the ConnectedThread in an unsynchronized manner */
    public static void write(byte[] out) {
        ConnectedThread r;
        Log.d(TAG, "write: Write Called.");
        // write to connected thread
        mConnectedThread.write(out);
    }

}

























