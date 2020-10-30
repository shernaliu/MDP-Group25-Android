package com.example.mdp_group25;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.content.SharedPreferences;
import java.nio.charset.Charset;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;
import androidx.appcompat.app.AppCompatActivity;

public class UtilityTool extends AppCompatActivity {
    private static String TAG = "UtilityTool";
    static String explored_mdf = "";
    static String obstacle_mdf = "";
    private static SharedPreferences sharedPrefs, sharedPrefsMain;
    private static SharedPreferences.Editor editor, editorMain;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPrefsMain = getApplicationContext().getSharedPreferences("Shared Preferences", Context.MODE_PRIVATE);
        editorMain = sharedPrefsMain.edit();
    }

    /**
     * UtilityTool to print message easily.
     * @param ctx
     * @param msgType
     * @param x
     * @param y
     * @throws JSONException
     */
    public void printMsg(Context ctx, String msgType, int x, int y) throws JSONException {
        String message = "AL>" + msgType + "(" + (x - 1) + "," + (y - 1) + ")" + " \n";
        if (BluetoothConnService.BTConnectionStatus == true) {
            byte[] bytes = message.getBytes(Charset.defaultCharset());
            BluetoothConnService.write(bytes);
        }
        sharedPrefs = ctx.getSharedPreferences("RobotControlActivity", MODE_PRIVATE);
        editor = sharedPrefs.edit();
        log(TAG, message);
        editor.putString("sentText", sharedPrefs.getString("sentText", "") + " \n" + message);
        editor.commit();
        log(TAG, sharedPrefs.getString("sentText", ""));
    }

    /**
     * UtilityTool to print message easily.
     * @param ctx
     * @param msg
     */
    public void printMsg(Context ctx, String msg) {
        String str = msg;
        msg = msg + '\n';
        if (BluetoothConnService.BTConnectionStatus == true) {
            byte[] bytes = msg.getBytes(Charset.defaultCharset());
            BluetoothConnService.write(bytes);
        }
        sharedPrefs = ctx.getSharedPreferences("RobotControlActivity", MODE_PRIVATE);
        editor = sharedPrefs.edit();
        editor.putString("sentText: ", sharedPrefs.getString("sentText", "") + "\n " + str);
        editor.commit();
        log(TAG, sharedPrefs.getString("sentText", ""));
    }

    /**
     * logger method
     * @param tag
     * @param messg
     */
    public static void log(String tag, String messg) {
        Log.d(tag, messg);
    }

    /**
     * method to convert the mdf string into a json obj
     *
     * @param rcvdMsg
     * @return
     */
    public static JSONObject parseMDFString(String rcvdMsg) {
        JSONObject jsonMapObj = new JSONObject();
        try {
            String exploredStr = "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff";
            JSONObject msgObj = new JSONObject(rcvdMsg);
            log(TAG, msgObj.toString());
            int len = msgObj.names().length();

            for (int i = 0; i < len; i++) {
                switch (msgObj.names().getString(i)) {
                    case "grid":
                        String mdfStr = msgObj.getString("grid");
                        String obstacle = "";
                        if (mdfStr.length() > 76) {
                            exploredStr = mdfStr.substring(0, 76);
                            explored_mdf = "\"" + exploredStr + "\"";
                            obstacle = mdfStr.substring(76);
                            obstacle_mdf = "\"" + obstacle + "\"";
                        }
                        if (mdfStr.length() == 76) {
                            exploredStr = mdfStr;
                        }
                        if (mdfStr.length() < 76) {
                            obstacle = mdfStr;
                        }
                        // converting explored string to length bits
                        int obs_bin_length = obstacle.length() * 4;
                        JSONObject parsedJsonMsg = new JSONObject();
                        parsedJsonMsg.put("explored", exploredStr);
                        parsedJsonMsg.put("obstacle", obstacle);
                        parsedJsonMsg.put("length", obs_bin_length);
                        jsonMapObj.put("map", parsedJsonMsg);
                        break;
                    case "image":
                        JSONArray images = msgObj.getJSONArray("image");
                        jsonMapObj.put("image", images);
                        break;
                    case "robot":
                        String robotMovement = msgObj.getString("robot");
                        jsonMapObj.put("robot", robotMovement);
                        break;
                }
            }
        } catch (JSONException exception) {
            log(TAG, "UtilityTool: Exception occured: " + exception.toString());
        }
        return jsonMapObj;
    }
}

