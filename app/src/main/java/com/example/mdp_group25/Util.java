package com.example.mdp_group25;


import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.Charset;

import androidx.appcompat.app.AppCompatActivity;

public class Util extends AppCompatActivity {
    private static String TAG = "UTIL";
    private static SharedPreferences sharedPreferences;
    private static SharedPreferences.Editor editor;
    private static SharedPreferences sharedPreferencesMain;
    private static SharedPreferences.Editor editorMain;

    static String final_mdf_string = "";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPreferencesMain = getApplicationContext().getSharedPreferences("Shared Preferences", Context.MODE_PRIVATE);
        editorMain = sharedPreferencesMain.edit();
    }

        public void printMessage(Context context, String messageType, int x, int y) throws JSONException {
            showLog(TAG,"Entering Print Message");
                String message = "AL>" + messageType + "(" + (x - 1) + "," + (y - 1) + ")" + " \n";
                if (BluetoothConnectionService.BluetoothConnectionStatus == true) {
                    byte[] bytes = message.getBytes(Charset.defaultCharset());
                    BluetoothConnectionService.write(bytes);
                }

                sharedPreferences = context.getSharedPreferences("RobotControlActivity", MODE_PRIVATE);
                editor = sharedPreferences.edit();
                showLog(TAG, message);
                editor.putString("sentText", sharedPreferences.getString("sentText", "") + " \n" + message);
                editor.commit();
                showLog(TAG, sharedPreferences.getString("sentText", ""));

        }

        public void printMessage(Context context, String message) {
            showLog(TAG,"Entering Print Message");
            String str = message;
            message=message+'\n';
            if (BluetoothConnectionService.BluetoothConnectionStatus == true) {
                byte[] bytes = message.getBytes(Charset.defaultCharset());
                BluetoothConnectionService.write(bytes);
            }
            sharedPreferences = context.getSharedPreferences("RobotControlActivity", MODE_PRIVATE);
            editor = sharedPreferences.edit();
            editor.putString("sentText", sharedPreferences.getString("sentText", "") + "\n " + str);
            editor.commit();
            showLog(TAG, sharedPreferences.getString("sentText", ""));
        }

    public static void showLog(String TAG, String message) {
        Log.d(TAG, message);
    }

    public static JSONObject parseMDFString(String receivedMessage){
        JSONObject mapObject = new JSONObject();
        try {
            //convert mdfString to JSON Object
            System.out.println("ENTERING PARSE MDF STRING");
            System.out.println(receivedMessage);
            String explored = "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff";
            JSONObject messageObject = new JSONObject(receivedMessage);
            System.out.println("MESSAGE OBJECT");
            System.out.println(messageObject);
            showLog(TAG, messageObject.toString());
            int len = messageObject.names().length();

            for(int j =0 ; j< len; j++){
                switch(messageObject.names().getString(j)){
                    case "grid":
                        String mdfString = messageObject.getString("grid");
                        final_mdf_string = "\""+mdfString+"\"";
                        String obstacle = "";
                        if(mdfString.length()> 76){
                            // explored string is also sent
                            explored = mdfString.substring(0, 76);
                            obstacle = mdfString.substring(76);
                        }

                        if(mdfString.length()==76){
                            explored = mdfString;
                        }

                        if(mdfString.length() < 76){
                            obstacle = mdfString;
                        }

                        //convert explored string to bit
                        int obs_bin_length = obstacle.length() * 4;


                        JSONObject parsedMessage = new JSONObject();
                        parsedMessage.put("explored", explored);
                        parsedMessage.put("obstacle", obstacle);
                        parsedMessage.put("length", obs_bin_length);

                        mapObject.put("map", parsedMessage);
                        break;

                    case "image":
                        System.out.println("ENTERING IMAGE");
                        JSONArray images = messageObject.getJSONArray("image");
                        System.out.println("IMAGES");
                        System.out.println(images);
                        mapObject.put("image", images);
                        break;
                    /*case "status":
                        String status = messageObject.getString("status");
                        switch(status){
                            case "MF":
                                mapObject.put("status", Status.MF);
                                break;
                            case "MB":
                                mapObject.put("status", Status.MB);
                                break;
                            case "S":
                                mapObject.put("status", Status.S);
                                break;
                            case "TR":
                                mapObject.put("status", Status.TR);
                                break;
                            case "TL":
                                mapObject.put("status", Status.TL);
                                break;
                            default:
                                break;
                        }
                        break;*/
                    case "robot":
                        String robotMovement = messageObject.getString("robot");
                        mapObject.put("robot", robotMovement);
                        break;
                }
            }
        }catch (JSONException err){
            showLog(TAG, "ERROR" + err.toString());
        }

        System.out.println(mapObject);

        return mapObject;

    }
}

