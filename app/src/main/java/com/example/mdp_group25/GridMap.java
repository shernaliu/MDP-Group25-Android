package com.example.mdp_group25;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigInteger;
import java.util.ArrayList;

import androidx.annotation.Nullable;

public class GridMap extends View {

    // declarations of attributes
    // static variable is created only one in the program at the time of loading of class
    private static Context context;
    private static final String TAG = "GRID_MAP";
    private static final int COL = 15, ROW = 20;
    private static float cellSize;      // indicating the cell size
    private static JSONObject receivedJsonObject = new JSONObject();    // for storing the current map information
    private static JSONObject backupMapInformation;     // for saving a copy of the received map information
    private static Cell[][] cells;      // for creating cells
    private static String robotDirection = "None";      // indicate the current direction of the robot
    private static int[] startCoord = new int[]{-1, -1};       // 0: col, 1: row
    private static int[] curCoord = new int[]{-1, -1};         // 0: col, 1: row
    private static int[] oldCoord = new int[]{-1, -1};         // 0: col, 1: row
    private static int[] waypointCoord = new int[]{-1, -1};    // 0: col, 1: row
    private static ArrayList<String[]> imageCoordinates = new ArrayList<>();
    private static ArrayList<int[]> obstacleCoord = new ArrayList<>(); // storing all obstacles coordinate
    private static boolean autoUpdate = false;          // false: manual mode, true: auto mode
    private static boolean mapDrawn = false;            // false: map not drawn, true: map drawn
    private static boolean canDrawRobot = false;        // false: cannot draw robot, true: can draw robot
    private static boolean setWaypointStatus = false;   // false: cannot set waypoint, true: can set waypoint
    private static boolean startCoordStatus = false;    // false: cannot set starting point, true: can set starting point
    private static boolean setObstacleStatus = false;   // false: cannot set obstacle, true: can set obstacle
    private static boolean unSetCellStatus = false;     // false: cannot unset cell, true: can unset cell
    private static boolean setExploredStatus = false;   // false: cannot check cell, true: can check cell
    private static boolean validPosition = false;       // false: robot out of range, true: robot within range
    private Bitmap image = BitmapFactory.decodeResource(getResources(), R.drawable.arrow_error);

    private Paint blackPaint = new Paint();         // for lines, etc
    private Paint obstacleColor = new Paint();      // black = obstacles position
    private Paint robotColor = new Paint();         // cyan = robot position
    private Paint endColor = new Paint();           // red = end position
    private Paint startColor = new Paint();         // green = start position
    private Paint waypointColor = new Paint();      // yellow = waypoint position
    private Paint unexploredColor = new Paint();    // gray = unexplored position
    private Paint exploredColor = new Paint();      // white = explored position
    private Paint fastestPathColor = new Paint();   // magenta = fastest path position
    private Paint imageColor = new Paint();         // black= image color

    private Util util = new Util();

    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;

    // constructor of grid map
    public GridMap(Context context) {
        super(context);
        this.context = context;
        init(null);
    }

    // constructor of grid map
    public GridMap(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        init(attrs);
        blackPaint.setStyle(Paint.Style.FILL_AND_STROKE);   // for lines, etc
        obstacleColor.setColor(Color.BLACK);                // black = obstacles position
        robotColor.setColor(Color.GREEN);                   // black = robot position
        endColor.setColor(Color.RED);                       // red = end position
        startColor.setColor(Color.CYAN);                    // green = start position
        waypointColor.setColor(Color.YELLOW);               // yellow = waypoint position
        unexploredColor.setColor(Color.GRAY);               // gray = unexplored position
        exploredColor.setColor(Color.WHITE);                // white = explored position
        fastestPathColor.setColor(Color.MAGENTA);           // magenta = fastest path position
        imageColor.setColor(Color.BLACK);                   // black = image color
    }

    // nullable allows parameter, field or method return value to be null if needed
    private void init(@Nullable AttributeSet attrs) {
        setWillNotDraw(false);
    }

    // to convert from android coordinate to screen coordinate, vice versa
    private int convertRow(int row) {
        return (20 - row);
    }

    // draw the custom view grid map
    @Override
    protected void onDraw(Canvas canvas) {
        Util.showLog(TAG, "Entering onDraw");
        super.onDraw(canvas);
        Util.showLog(TAG, "Redrawing map");

        ArrayList<String[]> imageCoordinates = this.getImageCoordinates();
        int[] curCoord = this.getCurCoord();

        if (!this.getMapDrawn()) {
            canvas.drawColor(Color.parseColor("#e9d8f2"));
            // image
            String[] placeholderImageCoord = new String[3];
            placeholderImageCoord[0] = "999";
            placeholderImageCoord[1] = "999";
            placeholderImageCoord[2] = "placeholder";
            imageCoordinates.add(placeholderImageCoord);
            this.createCell();

            this.setEndCoord(14, 19);
            mapDrawn = true;
        }

        // draw individual cell
        this.drawIndividualCell(canvas);
        // draw grid number
        this.drawGridNumber(canvas);
        // draw robot position
        if (this.getCanDrawRobot())
            this.drawRobot(canvas, curCoord);
        // draw images
        this.renderImages(canvas, imageCoordinates);

        Util.showLog(TAG,"Exiting onDraw");
    }

    // coordinates for each cell in the grid
    private void createCell() {
        Util.showLog(TAG,"Entering createCell");
        cells = new Cell[COL + 1][ROW + 1];
        this.calculateDimension();
        cellSize = this.getCellSize();

        //start from 1.5 to accommodate grid number
        for (int x = 0; x <= COL; x++)
            for (int y = 0; y <= ROW; y++)
                cells[x][y] = new Cell( x * cellSize + (cellSize / 30), y * cellSize + (cellSize / 30), (x + 1) * cellSize, (y + 1) * cellSize, unexploredColor, "unexplored");
        Util.showLog(TAG,"Exiting createCell");
    }

    // set auto update
    public void setAutoUpdate(boolean autoUpdate) throws JSONException {
        Log.d(TAG,"Entering setAutoUpdate");
        Util.showLog(TAG, String.valueOf(backupMapInformation));
        if (!autoUpdate)
            backupMapInformation = this.getReceivedJsonObject();
        else {
            setReceivedJsonObject(backupMapInformation);
            backupMapInformation = null;
            this.updateMapInformation();
        }
        GridMap.autoUpdate = autoUpdate;
        Log.d(TAG,"Exiting setAutoUpdate");
    }

    // for updating the text view when robot changes it's current coordinates
    private void updateRobotAxis(int col, int row, String direction) {
        // for updating the x-axis, y-axis and direction axis (for auto mode)
        Util.showLog(TAG,"updateRobotAxis");
        Util.showLog(TAG,direction);
        TextView xAxisTextView =  ((Activity)this.getContext()).findViewById(R.id.xAxisTextView);
        TextView yAxisTextView =  ((Activity)this.getContext()).findViewById(R.id.yAxisTextView);
        TextView directionAxisTextView =  ((Activity)this.getContext()).findViewById(R.id.directionAxisTextView);

        xAxisTextView.setText(String.valueOf(col-1));
        yAxisTextView.setText(String.valueOf(row-1));
        directionAxisTextView.setText(direction);
    }

    // move robot coordinate
    public void moveRobot(String direction) {
        Util.showLog(TAG,"Entering moveRobot");
        setValidPosition(false);  // reset it to default value
        int[] curCoord = this.getCurCoord();                        // screen coordinate
        ArrayList<int[]> obstacleCoord = this.getObstacleCoord();   // screen coordinate
        this.setOldRobotCoord(curCoord[0], curCoord[1]);            // screen coordinate
        int[] oldCoord = this.getOldRobotCoord();                   // screen coordinate
        String robotDirection = getRobotDirection();
        String backupDirection = robotDirection;

        System.out.println("CURRENT COORDINATES");
        System.out.println(curCoord[0]);
        System.out.println(curCoord[1]);
        // to move robot if validPosition is true
        switch (robotDirection) {
            case "up":
                switch (direction) {
                    case "forward":
                        if (curCoord[1] != 19) {
                            curCoord[1] += 1;
                            validPosition = true;
                        }
                        break;
                    case "right":
                        robotDirection = "right";
                        break;
                    case "down":
                        if (curCoord[1] != 2) {
                            curCoord[1] -= 1;
                            validPosition = true;
                        }
                        break;
                    case "left":
                        robotDirection = "left";
                        break;
                    default:
                        robotDirection = "error up";
                        break;
                }
                break;
            case "right":
                switch (direction) {
                    case "forward":
                        if (curCoord[0] != 14) {
                            curCoord[0] += 1;
                            validPosition = true;
                        }
                        break;
                    case "right":
                        robotDirection = "down";
                        break;
                    case "back":
                        if (curCoord[0] != 2) {
                            curCoord[0] -= 1;
                            validPosition = true;
                        }
                        break;
                    case "left":
                        robotDirection = "up";
                        break;
                    default:
                        robotDirection = "error right";
                }
                break;
            case "down":
                switch (direction) {
                    case "forward":
                        if (curCoord[1] != 2) {
                            curCoord[1] -= 1;
                            validPosition = true;
                        }
                        break;
                    case "right":
                        robotDirection = "left";
                        break;
                    case "back":
                        if (curCoord[1] != 19) {
                            curCoord[1] += 1;
                            validPosition = true;
                        }
                        break;
                    case "left":
                        robotDirection = "right";
                        break;
                    default:
                        robotDirection = "error down";
                }
                break;
            case "left":
                switch (direction) {
                    case "forward":
                        if (curCoord[0] != 2) {
                            curCoord[0] -= 1;
                            validPosition = true;
                        }
                        break;
                    case "right":
                        robotDirection = "up";
                        break;
                    case "back":
                        if (curCoord[0] != 14) {
                            curCoord[0] += 1;
                            validPosition = true;
                        }
                        break;
                    case "left":
                        robotDirection = "down";
                        break;
                    default:
                        robotDirection = "error left";
                }
                break;
            default:
                robotDirection = "error moveCurCoord";
                break;
        }
        // update on current coordinate and robot direction
        if (getValidPosition())
            for (int x = curCoord[0] - 1; x <= curCoord[0] + 1; x++) {
                for (int y = curCoord[1] - 1; y <= curCoord[1] + 1; y++) {
                    for (int i = 0; i < obstacleCoord.size(); i++) {
//                        if (obstacleCoord.get(i)[0] != x || obstacleCoord.get(i)[1] != y)
                        setValidPosition(true);
//                        else {
//                            setValidPosition(false);
//                            break;
//                        }
                    }
                    if (!getValidPosition())
                        break;
                }
                if (!getValidPosition())
                    break;
            }
        if (getValidPosition()) {
            System.out.println("GET VALID POSITION TRUE");
            this.setCurCoord(curCoord[0], curCoord[1], robotDirection);
        }
        else {
            System.out.println("GET VALID POSITION FALSE");
            if (direction.equals("forward") || direction.equals("back"))
                robotDirection = backupDirection;
            this.setCurCoord(oldCoord[0], oldCoord[1], robotDirection);
        }
        this.invalidate();
        Util.showLog(TAG,"Exiting moveRobot");
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        Util.showLog(TAG,"Entering onTouchEvent");
        if (event.getAction() == MotionEvent.ACTION_DOWN && this.getAutoUpdate() == false) {     // new touch started
            int column = (int) (event.getX() / cellSize);
            int row = this.convertRow((int) (event.getY() / cellSize)); // convert to screen coordinate
            // for toggling the button if it is set
            ToggleButton setStartPointToggleBtn = ((Activity)this.getContext()).findViewById(R.id.setStartPointToggleBtn);
            ToggleButton setWaypointToggleBtn = ((Activity)this. getContext()).findViewById(R.id.setWaypointToggleBtn);

            // if start coordinate status is true
            if (startCoordStatus) {
                // remove old starting coordinates
                if (canDrawRobot) {
                    // convert to screen coordinates
                    int[] startCoord = this.getStartCoord();
                    if (startCoord[0] >= 2 && startCoord[1] >= 2) {
                        startCoord[1] = this.convertRow(startCoord[1]);
                        for (int x = startCoord[0] - 1; x <= startCoord[0] + 1; x++)
                            for (int y = startCoord[1] - 1; y <= startCoord[1] + 1; y++)
                                cells[x][y].setType("unexplored");
                    }
                }
                else
                    canDrawRobot = true;
                // set new starting coordinates
                this.setStartCoord(column, row);
                // set start coordinate status to false
                startCoordStatus = false;

                try {
                    util.printMessage(context, "S", column, row);
                    sharedPreferences();
                    TextView sentMessage =  ((Activity)this.getContext()).findViewById(R.id.sentMessage);
                    sentMessage.setText(sharedPreferences.getString("sentText", ""));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                // if the button is checked, uncheck it
                if (setStartPointToggleBtn.isChecked())
                    setStartPointToggleBtn.toggle();
                this.invalidate();
                return true;
            }
            // if waypoint coordinate status is true
            if (setWaypointStatus) {
                int[] waypointCoord = this.getWaypointCoord();

                if (waypointCoord[0] >= 1 && waypointCoord[1] >= 1)
                    cells[waypointCoord[0]][this.convertRow(waypointCoord[1])].setType("unexplored");
                // set start coordinate status to false
                setWaypointStatus = false;
                // print out the message sent to other device
                try {
                    this.setWaypointCoord(column, row);
                    TextView xAxisTextview = ((Activity)this.getContext()).findViewById(R.id.xAxisTextViewWP);
                    TextView yAxisTextview = ((Activity)this.getContext()).findViewById(R.id.yAxisTextViewWP);

                    xAxisTextview.setText(Integer.toString(row-1));
                    yAxisTextview.setText(Integer.toString(column-1));

                    sharedPreferences();
                    TextView sentMessage =  ((Activity)this.getContext()).findViewById(R.id.sentMessage);
                    sentMessage.setText(sharedPreferences.getString("sentText", ""));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                // if the button is checked, uncheck it
                if (setWaypointToggleBtn.isChecked())
                    setWaypointToggleBtn.toggle();
                this.invalidate();
                return true;
            }
            // if obstacle status is true
            if (setObstacleStatus) {
                this.setObstacleCoord(column, row);
                this.invalidate();
                return true;
            }
            // if explored status is true
            if (setExploredStatus) {
                cells[column][20-row].setType("explored");
                this.invalidate();
                return true;
            }
            // if unset cell status is true
            if (unSetCellStatus) {
                Util.showLog(TAG,"UnsetCell Status");
                ArrayList<int[]> obstacleCoord = this.getObstacleCoord();
                cells[column][20-row].setType("unexplored");
                for (int i=0; i<obstacleCoord.size(); i++)
                    if (obstacleCoord.get(i)[0] == column && obstacleCoord.get(i)[1] == row)
                        obstacleCoord.remove(i);
                this.invalidate();
                return true;
            }
        }
        Util.showLog(TAG,"Exiting onTouchEvent");
        return false;
    }

    // draw individual cell
    private void drawIndividualCell(Canvas canvas) {
        Util.showLog(TAG,"Entering drawIndividualCell");
        for (int x = 1; x <= COL; x++)
            for (int y = 0; y < ROW; y++)
                for (int i = 0; i < this.getImageCoordinates().size(); i++)
                    canvas.drawRect(cells[x][y].startX, cells[x][y].startY, cells[x][y].endX, cells[x][y].endY, cells[x][y].paint);

        Util.showLog(TAG,"Exiting drawIndividualCell");
    }

    // draw grid number on grid map
    private void drawGridNumber(Canvas canvas) {
        Util.showLog(TAG,"Entering drawGridNumber");
        // draw x-axis number
        for (int x = 1; x <= COL; x++) {
            // for 2 digit number
            if (x > 10)
                canvas.drawText(Integer.toString(x-1), cells[x][20].startX + (cellSize / 5), cells[x][20].startY + (cellSize / 3), blackPaint);
            else
                canvas.drawText(Integer.toString(x-1), cells[x][20].startX + (cellSize / 3), cells[x][20].startY + (cellSize / 3), blackPaint);
        }
        // draw y-axis number
        for (int y = 0; y < ROW; y++) {
            // for 2 digit number
            if (20 - (y+1) > 9)
                canvas.drawText(Integer.toString(20 - (y+1)), cells[0][y].startX + (cellSize / 2), cells[0][y].startY + (cellSize / 1.5f), blackPaint);
            else
                canvas.drawText(Integer.toString(20 - (y+1)), cells[0][y].startX + (cellSize / 1.5f), cells[0][y].startY + (cellSize / 1.5f), blackPaint);
        }
        Util.showLog(TAG,"Exiting drawGridNumber");
    }

    // draw robot position
    private void drawRobot(Canvas canvas, int[] curCoord) {
        Util.showLog(TAG,"Entering drawRobot");

        int androidRowCoord = this.convertRow(curCoord[1]);
        // remove horizontal lines for robot
        for (int y = androidRowCoord; y <= androidRowCoord + 1; y++)
            canvas.drawLine(cells[curCoord[0] - 1][y].startX, cells[curCoord[0] - 1][y].startY - (cellSize / 30), cells[curCoord[0] + 1][y].endX, cells[curCoord[0] + 1][y].startY - (cellSize / 30), robotColor);
        // remove vertical lines for robot
        for (int x = curCoord[0] - 1; x < curCoord[0] + 1; x++)
            canvas.drawLine(cells[x][androidRowCoord - 1].startX - (cellSize / 30) + cellSize, cells[x][androidRowCoord - 1].startY, cells[x][androidRowCoord + 1].startX - (cellSize / 30) + cellSize, cells[x][androidRowCoord + 1].endY, robotColor);

        // draw robot shape
        switch (this.getRobotDirection()) {
            case "up":
                // draw from bottom left to top center
                canvas.drawLine(cells[curCoord[0] - 1][androidRowCoord + 1].startX, cells[curCoord[0] - 1][androidRowCoord + 1].endY, (cells[curCoord[0]][androidRowCoord - 1].startX + cells[curCoord[0]][androidRowCoord - 1].endX) / 2, cells[curCoord[0]][androidRowCoord - 1].startY, blackPaint);
                // draw from top center to bottom right
                canvas.drawLine((cells[curCoord[0]][androidRowCoord - 1].startX + cells[curCoord[0]][androidRowCoord - 1].endX) / 2, cells[curCoord[0]][androidRowCoord - 1].startY, cells[curCoord[0] + 1][androidRowCoord + 1].endX, cells[curCoord[0] + 1][androidRowCoord + 1].endY, blackPaint);
                break;
            case "down":
                // draw from top left to bottom center
                canvas.drawLine(cells[curCoord[0] - 1][androidRowCoord - 1].startX, cells[curCoord[0] - 1][androidRowCoord - 1].startY, (cells[curCoord[0]][androidRowCoord + 1].startX + cells[curCoord[0]][androidRowCoord + 1].endX) / 2, cells[curCoord[0]][androidRowCoord + 1].endY, blackPaint);
                // draw from bottom center to top right
                canvas.drawLine((cells[curCoord[0]][androidRowCoord + 1].startX + cells[curCoord[0]][androidRowCoord + 1].endX) / 2, cells[curCoord[0]][androidRowCoord + 1].endY, cells[curCoord[0] + 1][androidRowCoord - 1].endX, cells[curCoord[0] + 1][androidRowCoord - 1].startY, blackPaint);
                break;
            case "right":
                // draw from top left to right center
                canvas.drawLine(cells[curCoord[0] - 1][androidRowCoord - 1].startX, cells[curCoord[0] - 1][androidRowCoord - 1].startY, cells[curCoord[0] + 1][androidRowCoord].endX, cells[curCoord[0] + 1][androidRowCoord - 1].endY + (cells[curCoord[0] + 1][androidRowCoord].endY - cells[curCoord[0] + 1][androidRowCoord - 1].endY) / 2, blackPaint);
                // draw from right center to bottom left
                canvas.drawLine(cells[curCoord[0] + 1][androidRowCoord].endX, cells[curCoord[0] + 1][androidRowCoord - 1].endY + (cells[curCoord[0] + 1][androidRowCoord].endY - cells[curCoord[0] + 1][androidRowCoord - 1].endY) / 2, cells[curCoord[0] - 1][androidRowCoord + 1].startX, cells[curCoord[0] - 1][androidRowCoord + 1].endY, blackPaint);
                break;
            case "left":
                // draw from top right to left center
                canvas.drawLine(cells[curCoord[0] + 1][androidRowCoord - 1].endX, cells[curCoord[0] + 1][androidRowCoord - 1].startY, cells[curCoord[0] - 1][androidRowCoord].startX, cells[curCoord[0] - 1][androidRowCoord - 1].endY + (cells[curCoord[0] - 1][androidRowCoord].endY - cells[curCoord[0] - 1][androidRowCoord - 1].endY) / 2, blackPaint);
                // draw from left center to bottom right
                canvas.drawLine(cells[curCoord[0] - 1][androidRowCoord].startX, cells[curCoord[0] - 1][androidRowCoord - 1].endY + (cells[curCoord[0] - 1][androidRowCoord].endY - cells[curCoord[0] - 1][androidRowCoord - 1].endY) / 2, cells[curCoord[0] + 1][androidRowCoord + 1].endX, cells[curCoord[0] + 1][androidRowCoord + 1].endY, blackPaint);
                break;
            default:
                Toast.makeText(this.getContext(), "Error with drawing robot (unknown direction)", Toast.LENGTH_LONG).show();
                break;
        }
        Util.showLog(TAG,"Exiting drawRobot");
    }

    // calculate dimension
    private void calculateDimension() {
        this.setCellSize(getWidth()/(COL+1));
    }

    // update map information on auto mode
    public void updateMapInformation() throws JSONException {
        Util.showLog(TAG,"Entering updateMapInformation");
        // current map information
        JSONObject mapInformation = this.getReceivedJsonObject();
        Util.showLog(TAG,"updateMapInformation --- mapInformation: " + mapInformation);


        JSONObject infoJsonObject;
        String hexStringExplored, hexStringObstacle, exploredString, obstacleString;
        BigInteger hexBigIntegerExplored, hexBigIntegerObstacle;

        obstacleString="";

        if (mapInformation == null)
            return;


        for(int i=0; i< mapInformation.names().length(); i++){
            switch(mapInformation.names().getString(i)){
                case "map":
                    infoJsonObject = mapInformation.getJSONObject("map");

                    //set explored
                    hexStringExplored = infoJsonObject.getString("explored");
                    hexBigIntegerExplored = new BigInteger(hexStringExplored, 16);
                    exploredString = hexBigIntegerExplored.toString(2);
                    //throw away padding bits
                    exploredString = exploredString.substring(2, 302);

                    int length = infoJsonObject.getInt("length");
                    hexStringObstacle = infoJsonObject.getString("obstacle");
                    if(hexStringObstacle.length()>0){
                        hexBigIntegerObstacle = new BigInteger(hexStringObstacle, 16);
                        obstacleString = hexBigIntegerObstacle.toString(2);
                    }

                    //calculate 1 in explored string
//                    int exploredLength = 0;
//                    for(int k = 0; k< exploredString.length(); k++){
//                        if(String.valueOf(exploredString.charAt(k)).equals("1")){
//                            exploredLength++;
//                        }
//                    }

                    //check if exploredLength is equal to obstacleLength

                    int x, y;
                    for(int j =0; j<exploredString.length(); j++){
                        //convert to android coord
                        y = 19 - (j/15);
                        x = 1 + j - ((19-y)*15);
                        if ((String.valueOf(exploredString.charAt(j))).equals("1") && !cells[x][y].type.equals("robot")){
                            cells[x][y].setType("explored");
                        }
                        else if ((String.valueOf(exploredString.charAt(j))).equals("0") && !cells[x][y].type.equals("robot")){
                            cells[x][y].setType("unexplored");
                        }
                    }

                    //pad front of obstacle string
                    while(obstacleString.length()<length){
                        obstacleString = "0" + obstacleString;
                    }

                    int kj = 0;
                    for (int row = ROW-1; row >= 0; row--)
                        for (int col = 1; col <= COL; col++)
                            if ((cells[col][row].type.equals("explored")||(cells[col][row].type.equals("robot"))) && kj < obstacleString.length()) { // ||cells[col][row].type.equals("arrow")
                                if ((String.valueOf(obstacleString.charAt(kj))).equals("1")) //  && !cells[col][row].type.equals("arrow")
                                    this.setObstacleCoord(col, 20 - row);
                                kj++;
                            }

                    // set waypoint cells if it exist
                    int[] waypointCoord = this.getWaypointCoord();
                    if (waypointCoord[0] >= 1 && waypointCoord[1] >= 1)
                        cells[waypointCoord[0]][20-waypointCoord[1]].setType("waypoint");
                    break;
                case "image":
                    JSONArray images = mapInformation.getJSONArray("image");
                    JSONObject image;
                    for(int j=0; j< images.length(); j++){
                        image = images.getJSONObject(j);
                        String imageString = image.getString("imageString");
                        Util.showLog(TAG,"imageString " + mapInformation);
                        String imageX = imageString.substring(0,2);
                        Util.showLog(TAG,"imageString X" + imageX);
                        String imageY = imageString.substring(2,4);
                        Util.showLog(TAG,"imageString Y" + imageY);
                        int imageType = Integer.parseInt(imageString.substring(4));
                        String imageTypeString = Integer.toString(imageType);
                        Util.showLog(TAG,"imageString type" + imageTypeString);
                        this.setImageCoordinate(Integer.parseInt(imageX), Integer.parseInt(imageY), imageTypeString);
                    }
                    break;
                case "status":
                    String status = mapInformation.getString("status");
                    TextView robotStatusTextView =  ((Activity)this.getContext()).findViewById(R.id.robotStatusTextView);
                    robotStatusTextView.setText(status);
                    break;
                case "robot":
                    String robotMovements = mapInformation.getString("robot");
                    System.out.println("ROBOT MOVEMENT");
                    System.out.println(robotMovements);
                    for(int k =0; k< robotMovements.length(); k++){
                        switch(robotMovements.charAt(k)){
                            case 'w':
                                moveRobot("forward");
                                System.out.println("FORWARD");
                                break;
                            case 'd':
                                moveRobot("right");
                                System.out.println("RIGHT");
                                break;
                            case 'a':
                                moveRobot("left");
                                System.out.println("LEFT");
                                break;
                            case 's':
                                moveRobot("down");
                                System.out.println("DOWN");
                                break;
                        }
                    }
                    break;
            }
        }
        Util.showLog(TAG,"Exiting updateMapInformation");
        this.invalidate();
    }

    // toggle all button if enabled/checked, except for the clicked button
    public void toggleCheckedBtn(String buttonName) {
        ToggleButton setStartPointToggleBtn = ((Activity)this.getContext()).findViewById(R.id.setStartPointToggleBtn);
        ToggleButton setWaypointToggleBtn = ((Activity)this.getContext()).findViewById(R.id.setWaypointToggleBtn);
        ImageButton obstacleImageBtn = ((Activity)this.getContext()).findViewById(R.id.obstacleImageBtn);
        ImageButton exploredImageBtn = ((Activity)this.getContext()).findViewById(R.id.exploredImageBtn);
        ImageButton clearImageBtn = ((Activity)this. getContext()).findViewById(R.id.clearImageBtn);

        if (!buttonName.equals("setStartPointToggleBtn"))
            if (setStartPointToggleBtn.isChecked()) {
                this.setStartCoordStatus(false);
                setStartPointToggleBtn.toggle();
            }
        if (!buttonName.equals("setWaypointToggleBtn"))
            if (setWaypointToggleBtn.isChecked()) {
                this.setWaypointStatus(false);
                setWaypointToggleBtn.toggle();
            }
        if (!buttonName.equals("exploredImageBtn"))
            if (exploredImageBtn.isEnabled())
                this.setExploredStatus(false);
        if (!buttonName.equals("obstacleImageBtn"))
            if (obstacleImageBtn.isEnabled())
                this.setSetObstacleStatus(false);
        if (!buttonName.equals("clearImageBtn"))
            if (clearImageBtn.isEnabled())
                this.setUnSetCellStatus(false);
    }

    //renderImages
    private void renderImages(Canvas canvas, ArrayList<String[]> imageCoordinates) {
        Util.showLog(TAG,"Entering renderImages");
        RectF rect;

        for (int i = 0; i < imageCoordinates.size(); i++) {

            if (!imageCoordinates.get(i)[2].equals("placeholder")) {

                int column = Integer.parseInt(imageCoordinates.get(i)[0]);

                int row = convertRow(Integer.parseInt(imageCoordinates.get(i)[1]));

                rect = new RectF(column * cellSize, row * cellSize, (column + 1) * cellSize, (row + 1) * cellSize);
                Util.showLog(TAG, imageCoordinates.get(i)[2]);
                switch (imageCoordinates.get(i)[2]) {

                    case "1":
                        image = BitmapFactory.decodeResource(getResources(), R.drawable.one);
                        break;

                    case "2":
                        System.out.println("Entering 2");
                        image = BitmapFactory.decodeResource(getResources(), R.drawable.two);
                        break;

                    case "3":
                        image = BitmapFactory.decodeResource(getResources(), R.drawable.three);
                        break;

                    case "4":
                        image = BitmapFactory.decodeResource(getResources(), R.drawable.four);
                        break;

                    case "5":
                        image = BitmapFactory.decodeResource(getResources(), R.drawable.five);
                        break;

                    case "6":
                        image = BitmapFactory.decodeResource(getResources(), R.drawable.six);
                        break;

                    case "7":
                        image = BitmapFactory.decodeResource(getResources(), R.drawable.seven);
                        break;

                    case "8":
                        image = BitmapFactory.decodeResource(getResources(), R.drawable.eight);
                        break;

                    case "9":
                        image = BitmapFactory.decodeResource(getResources(), R.drawable.nine);
                        break;

                    case "10":
                        image = BitmapFactory.decodeResource(getResources(), R.drawable.ten);
                        break;

                    case "11":
                        image = BitmapFactory.decodeResource(getResources(), R.drawable.eleven);
                        break;

                    case "12":
                        image = BitmapFactory.decodeResource(getResources(), R.drawable.twelve);
                        break;

                    case "13":
                        image = BitmapFactory.decodeResource(getResources(), R.drawable.thirteen);
                        break;

                    case "14":
                        image = BitmapFactory.decodeResource(getResources(), R.drawable.fourteen);
                        break;

                    case "15":
                        image = BitmapFactory.decodeResource(getResources(), R.drawable.fifteen);
                        break;

                    default:
                        break;
                }

                canvas.drawBitmap(image, null, rect, null);
            }
            Util.showLog(TAG,"exiting renderImages");
        }
    }

    // reset map
    public void resetMap() {
        Util.showLog(TAG,"Entering resetMap");
        // reset screen text
        TextView robotStatusTextView =  ((Activity)this.getContext()).findViewById(R.id.robotStatusTextView);
        ToggleButton manualAutoToggleBtn = ((Activity)this.getContext()).findViewById(R.id.manualAutoToggleBtn);
        updateRobotAxis(0, 0, "None");
        robotStatusTextView.setText("status");
        sharedPreferences();
        editor.putString("receivedText", "");
        editor.putString("sentText", "");
        editor.putString("image", "");
        editor.commit();

        /*if (manualAutoToggleBtn.isChecked())
            manualAutoToggleBtn.toggle();*/
        this.toggleCheckedBtn("None");

        // reset all the values
        receivedJsonObject = null;      //new JSONObject();
        backupMapInformation = null;    //new JSONObject();
        startCoord = new int[]{-1, -1};         // 0: col, 1: row
        curCoord = new int[]{-1, -1};           // 0: col, 1: row
        oldCoord = new int[]{-1, -1};           // 0: col, 1: row
        robotDirection = "None";        // reset the robot direction
        autoUpdate = false;             // reset it to manual mode
        imageCoordinates = new ArrayList<>();
        obstacleCoord = new ArrayList<>();  // reset the obstacles coordinate array list
        waypointCoord = new int[]{-1, -1};      // 0: col, 1: row
        mapDrawn = false;           // set map drawn to false
        canDrawRobot = false;       // set can draw robot to false
        validPosition = false;      // set valid position to false

        Util.showLog(TAG,"Exiting resetMap");
        this.invalidate();
    }

    // get auto update
    public boolean getAutoUpdate() {
        return autoUpdate;
    }

    // get message received status
    public boolean getMapDrawn() {
        return mapDrawn;
    }

    // set valid position status
    private void setValidPosition(boolean status) {
        validPosition = status;
    }

    // get valid position status
    public boolean getValidPosition() {
        return validPosition;
    }

    // set unset cell status
    public void setUnSetCellStatus(boolean status) {
        unSetCellStatus = status;
    }

    // get unset cell status
    public boolean getUnSetCellStatus() {
        return unSetCellStatus;
    }

    // set set obstacle status
    public void setSetObstacleStatus(boolean status) {
        setObstacleStatus = status;
    }

    // get set obstacle status
    public boolean getSetObstacleStatus() {
        return setObstacleStatus;
    }

    // get explored cell status
    public void setExploredStatus(boolean status) {
        setExploredStatus = status;
    }

    // get set obstacle status
    public boolean getExploredStatus() {
        return setExploredStatus;
    }


    // set start coordinate status
    public void setStartCoordStatus(boolean status) {
        startCoordStatus = status;
    }

    // get start coordinate status
    private boolean getStartCoordStatus() {
        return startCoordStatus;
    }

    // set way point status
    public void setWaypointStatus(boolean status) {
        setWaypointStatus = status;
    }

    // get can draw robot boolean value
    public boolean getCanDrawRobot() {
        return canDrawRobot;
    }

    // set ending coordinates
    public void setEndCoord(int col, int row) {
        Util.showLog(TAG,"Entering setEndCoord");
        //convert to android coordinate
        row = this.convertRow(row);
        // change the color of ending coordinate
        for (int x = col - 1; x <= col + 1; x++)
            for (int y = row - 1; y <= row + 1; y++)
                cells[x][y].setType("end");
        Util.showLog(TAG,"Exiting setEndCoord");
    }

    // set starting coordinates
    public void setStartCoord(int col, int row) {
        Util.showLog(TAG,"Entering setStartCoord");
        int oldCol=startCoord[0];
        int oldRow=startCoord[1];

        if(startCoord[1] != -1 & startCoord[0] !=-1){
            // column cannot be 1 and 15
            if(oldCol == 1){
                oldCol = oldCol + 1;
            }
            else if(oldCol == 15){
                oldCol = oldCol - 1;
            }
            // row cannot be 1 and 20
            if(oldRow == 1){
                oldRow = oldRow + 1;
            }
            else if(oldRow == 20){
                oldRow = oldRow - 1;
            }

            oldRow = this.convertRow(oldRow);
            //if start coordinates has been set before, set it to unexplored
            for (int x = oldCol - 1; x <= oldCol + 1; x++)
                for (int y = oldRow - 1; y <= oldRow + 1; y++)
                    cells[x][y].setType("unexplored");
        }

        startCoord[1] = row;
        startCoord[0] = col;

        // if starting coordinate not set
        if (this.getStartCoordStatus())
            // convert to android coordinate
            this.setCurCoord(col, row, "up");
        Util.showLog(TAG,"Exiting setStartCoord");
    }

    // get starting coordinates (for auto/manual)
    private int[] getStartCoord() {
        return startCoord;
    }

    // set robot current coordinates
    public void setCurCoord(int col, int row, String direction) {
        Util.showLog(TAG,"Entering setCurCoord");
        this.setRobotDirection(direction);

        // column cannot be 1 and 15
        if(col == 1){
            col = col + 1;
        }
        else if(col == 15){
            col = col - 1;
        }
        // row cannot be 1 and 20
        if(row == 1){
            row = row + 1;
        }
        else if(row == 20){
            row = row - 1;
        }

        this.updateRobotAxis(col, row, direction);

        //update the current coordinates with the converted coordinates if out of range
        curCoord[0] = col;
        curCoord[1] = row;

        // convert to android coordinate
        row = this.convertRow(row);
        // change the color of robot current coordinate
        for (int x = col - 1; x <= col + 1; x++)
            for (int y = row - 1; y <= row + 1; y++)
                cells[x][y].setType("robot");
        Util.showLog(TAG,"Exiting setCurCoord");
    }

    // get current coordinate
    public int[] getCurCoord() {
        // screen coordinate
        return curCoord;
    }

    // set direction of the robot
    public void setRobotDirection(String direction) {
        this.sharedPreferences();
        robotDirection = direction;
        editor.putString("direction", direction);
        editor.commit();
        this.invalidate();;
    }

    // get direction of the robot
    public String getRobotDirection() {
        return robotDirection;
    }

    // set waypoint coordinate
    private void setWaypointCoord(int col, int row) throws JSONException {
        Util.showLog(TAG,"Entering setWaypointCoord");
        waypointCoord[0] = col;
        waypointCoord[1] = row;

        // convert to android coordinate
        row = this.convertRow(row);
        cells[col][row].setType("waypoint");

        // toast is a small message displayed on the screen, similar to a popup notification that remains visible for a short time period
        util.printMessage(context, "W", waypointCoord[0], waypointCoord[1]);
        Util.showLog(TAG,"Exiting setWaypointCoord");
    }

    // get waypoint coordinate
    public int[] getWaypointCoord() {
        // screen coordinate
        return waypointCoord;
    }

    // set obstacle coordinate
    private void setObstacleCoord(int col, int row) {
        Util.showLog(TAG,"Entering setObstacleCoord");
        // screen coordinate
        int[] obstacleCoord = new int[]{col, row};
        GridMap.obstacleCoord.add(obstacleCoord);
        // convert to android coordinate
        row = this.convertRow(row);
        // change the color of obstacle coordinate
        cells[col][row].setType("obstacle");
        Util.showLog(TAG,"Exiting setObstacleCoord");
    }

    // get obstacle coordinate (screen coordinate)
    private ArrayList<int[]> getObstacleCoord() {
        return obstacleCoord;
    }

    // set old robot coordinate
    private void setOldRobotCoord(int oldCol, int oldRow) {
        Util.showLog(TAG,"Entering setOldRobotCoord");
        oldCoord[0] = oldCol;
        oldCoord[1] = oldRow;
        // convert to android coordinate
        oldRow = this.convertRow(oldRow);
        // change the color of robot current coordinate
        for (int x = oldCol - 1; x <= oldCol + 1; x++)
            for (int y = oldRow - 1; y <= oldRow + 1; y++)
                cells[x][y].setType("explored");
        Util.showLog(TAG,"Exiting setOldRobotCoord");
    }

    // get old robot coordinate
    private int[] getOldRobotCoord() {
        return oldCoord;
    }

    // set cell size
    private void setCellSize(float cellSize) {
        GridMap.cellSize = cellSize;
    }

    // get cell size
    private float getCellSize() {
        return cellSize;
    }

    // set map information
    public void setReceivedJsonObject(JSONObject receivedJsonObject) {
        Util.showLog(TAG,"Entered setReceivedJsonObject");
        GridMap.receivedJsonObject = receivedJsonObject;
        // to prevent screen from refreshing with old values
        backupMapInformation = receivedJsonObject;
    }

    // get received map information
    public JSONObject getReceivedJsonObject() {
        return receivedJsonObject;
    }

    //get and set image coordinates
    private void setImageCoordinate(int column, int row, String imageType) {
        Util.showLog(TAG,"Entering setImageCoordinates");
        column += 1;

        row += 1;

        String[] imageCoordinates = new String[3];

        imageCoordinates[0] = String.valueOf(column);

        imageCoordinates[1] = String.valueOf(row);

        imageCoordinates[2] = imageType;
        if (cells[column][20 - row].type.equals("obstacle") || cells[column][20 - row].type.equals("image") ) {
            try {

                this.getImageCoordinates().add(imageCoordinates);

                this.sharedPreferences();

                String message = "(" + (column - 1) + ", " + (row - 1) + ", " + Integer.parseInt(imageCoordinates[2], 16) + ")";

                editor.putString("image", sharedPreferences.getString("image", "") + "\n " + message);

                editor.commit();

                row = convertRow(row);

                cells[column][row].setType("image");

            } catch (Exception e) {
            }
        }

        Util.showLog(TAG,"Exiting setImageCoordinates");

    }


    private ArrayList<String[]> getImageCoordinates() {
        return imageCoordinates;
    }

    // for activating sharedPreferences
    private void sharedPreferences() {
        // set TAG and Mode for shared preferences
        sharedPreferences = this.getContext().getSharedPreferences("RobotControlActivity", Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }
}
