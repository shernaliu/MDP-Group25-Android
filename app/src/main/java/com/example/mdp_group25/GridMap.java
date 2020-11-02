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

    private static Context context;

    private Util util = new Util();
    private static final String TAG = "GRID_MAP";
    private static final int COL = 15, ROW = 20;
    private static float cellSize;
    private static JSONObject receivedJsonObject = new JSONObject();
    private static JSONObject backupMapInformation;
    private static Cell[][] cells;      // for creating cells
    private static String robotDirection = "None";
    private static int[] startCoord = new int[]{-1, -1};
    private static int[] curCoord = new int[]{-1, -1};
    private static int[] oldCoord = new int[]{-1, -1};
    private static int[] waypointCoord = new int[]{-1, -1};
    int image_type [] = new int[]{-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99};
    int image_x_coordinate [] = new int[]{-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99};
    int image_y_coordinate [] = new int[]{-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99};
    int flag_for_image_coordinates = 0;
    String image_string_output = "";
    private static ArrayList<String[]> imageCoordinates = new ArrayList<>();
    private static ArrayList<int[]> obstacleCoord = new ArrayList<>();
    private static boolean autoUpdate = false;
    private static boolean mapDrawn = false;
    private static boolean canDrawRobot = false;
    private static boolean setWaypointStatus = false;
    private static boolean startCoordStatus = false;
    private static boolean setObstacleStatus = false;
    private static boolean unSetCellStatus = false;
    private static boolean setExploredStatus = false;
    private static boolean validPosition = false;
    private Bitmap image = BitmapFactory.decodeResource(getResources(), R.drawable.arrow_error);
    private Paint startColor = new Paint();
    private Paint endColor = new Paint();
    private Paint robotColor = new Paint();
    private Paint waypointColor = new Paint();
    private Paint exploredColor = new Paint();
    private Paint unexploredColor = new Paint();
    private Paint obstacleColor = new Paint();
    private Paint fastestPathColor = new Paint();
    private Paint blackPaint = new Paint();
    private Paint imageColor = new Paint();
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;

    public GridMap(Context context) {
        super(context);
        this.context = context;
        init(null);
    }

    public GridMap(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        init(attrs);
        blackPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        obstacleColor.setColor(Color.BLACK);
        robotColor.setColor(Color.GREEN);
        endColor.setColor(Color.RED);
        startColor.setColor(Color.CYAN);
        waypointColor.setColor(Color.YELLOW);
        unexploredColor.setColor(Color.GRAY);
        exploredColor.setColor(Color.WHITE);
        fastestPathColor.setColor(Color.BLUE);
        imageColor.setColor(Color.BLACK);
    }

    private void init(@Nullable AttributeSet attrs) {
        setWillNotDraw(false);
    }

    private int convertRow(int row) {
        return (20 - row);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        ArrayList<String[]> imageCoordinates = this.getImageCoordinates();
        int[] curCoord = this.getCurCoord();
        boolean b = this.getMapDrawn();
        if (!b) {
            canvas.drawColor(Color.parseColor("#FFFFFF"));
            String[] placeholderImageCoord = new String[3];
            placeholderImageCoord[0] = "999";
            placeholderImageCoord[1] = "999";
            placeholderImageCoord[2] = "placeholder";
            imageCoordinates.add(placeholderImageCoord);
            this.createCell();
            this.setEndCoord(14, 19);
            mapDrawn = true;
        }
        this.drawIndividualCell(canvas);
        this.drawGridNumber(canvas);
        if (this.getCanDrawRobot())
            this.drawRobot(canvas, curCoord);
        this.renderImages(canvas, imageCoordinates);
    }

    private void createCell() {
        cells = new Cell[COL + 1][ROW + 1];
        this.calculateDimension();
        cellSize = this.getCellSize();
        int x = 0;
        String type = "unexplored";
        while(x<=COL)
        {
            int y = 0;
            while(y<=ROW)
            {
                float startX = x * cellSize + (cellSize / 30);
                float endX = (x + 1) * cellSize;
                float startY = y * cellSize + (cellSize / 30);
                float endY = (y + 1) * cellSize;
                cells[x][y] = new Cell( startX , startY, endX, endY, unexploredColor, type);
                y++;
            }
            x++;
        }
    }

    public void setAutoUpdate(boolean autoUpdate) throws JSONException {
        if (autoUpdate == false)
            backupMapInformation = this.getReceivedJsonObject();
        else {
            setReceivedJsonObject(backupMapInformation);
            backupMapInformation = null;
            this.updateMapInformation();
        }
        GridMap.autoUpdate = autoUpdate;
    }

    private void updateRobotAxis(int col, int row, String direction) {
        TextView xAxisTextView =  ((Activity)this.getContext()).findViewById(R.id.xAxisTextView);
        TextView yAxisTextView =  ((Activity)this.getContext()).findViewById(R.id.yAxisTextView);
        TextView directionAxisTextView =  ((Activity)this.getContext()).findViewById(R.id.directionAxisTextView);

        xAxisTextView.setText(String.valueOf(col-1));
        yAxisTextView.setText(String.valueOf(row-1));
        directionAxisTextView.setText(direction);
    }

    public void moveRobot(String direction) {
        setValidPosition(false);
        int[] curCoord = this.getCurCoord();
        ArrayList<int[]> obstacleCoord = this.getObstacleCoord();
        this.setOldRobotCoord(curCoord[0], curCoord[1]);
        int[] oldCoord = this.getOldRobotCoord();
        String robotDirection = getRobotDirection();
        String backupDirection = robotDirection;
        if(robotDirection.equals("up"))
        {
            if(direction.equals("forward"))
            {
                if (curCoord[1] != 19) {
                    curCoord[1] += 1;
                    validPosition = true;
                }
            }
            else if(direction.equals("back")) {
                if (curCoord[1] != 2) {
                    curCoord[1] -= 1;
                    validPosition = true;
                }
            }
            else if(direction.equals("right"))
                robotDirection = "right";
            else if(direction.equals("left"))
                    robotDirection = "left";
            else
                robotDirection = "error up";
        }
        else if (robotDirection.equals("right"))
            {
                if(direction.equals("forward"))
                {
                    if (curCoord[0] != 14) {
                        curCoord[0] += 1;
                        validPosition = true;
                    }
                }
                else if(direction.equals("back"))
                {
                    if (curCoord[0] != 2) {
                        curCoord[0] -= 1;
                        validPosition = true;
                    }
                }
                else if(direction.equals("right"))
                    robotDirection = "down";
                else if(direction.equals("left"))
                    robotDirection = "up";
                else
                    robotDirection = "error right";
            }
        else if (robotDirection.equals("down"))
            {
                if(direction.equals("forward"))
                {
                    if (curCoord[1] != 2) {
                        curCoord[1] -= 1;
                        validPosition = true;
                    }
                }
                else if(direction.equals("back"))
                {
                    if (curCoord[1] != 19) {
                        curCoord[1] += 1;
                        validPosition = true;
                    }
                }
                else if(direction.equals("right"))
                    robotDirection = "left";
                else if(direction.equals("left"))
                    robotDirection = "right";
                else
                    robotDirection = "error down";
            }
            else if (robotDirection.equals("left"))
            {
                if(direction.equals("forward"))
                {
                    if (curCoord[0] != 2) {
                        curCoord[0] -= 1;
                        validPosition = true;
                    }
                }
                else if(direction.equals("back"))
                {
                    if (curCoord[0] != 14) {
                        curCoord[0] += 1;
                        validPosition = true;
                    }
                }
                else if(direction.equals("right"))
                    robotDirection = "up";
                else if(direction.equals("left"))
                    robotDirection = "down";
                else
                    robotDirection = "error left";
            }
        if(getValidPosition())
        {
            int x = curCoord[0] - 1;
            while(x<=curCoord[0]+1)
            {
                int y = curCoord[1] - 1;
                while(y <= curCoord[1] + 1)
                {
                    for (int i = 0; i < obstacleCoord.size(); i++)
                        setValidPosition(true);
                    if (!getValidPosition())
                        break;
                    y++;
                }
                x++;
                if (!getValidPosition())
                    break;
            }
        }
        if (getValidPosition()) {
            this.setCurCoord(curCoord[0], curCoord[1], robotDirection);
        }
        else {
            if (direction.equals("forward") || direction.equals("back"))
                robotDirection = backupDirection;
            this.setCurCoord(oldCoord[0], oldCoord[1], robotDirection);
        }
        this.invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN && this.getAutoUpdate() == false) {
            int column = (int) (event.getX() / cellSize);
            int row = this.convertRow((int) (event.getY() / cellSize));
            ToggleButton setStartPointToggleBtn = ((Activity)this.getContext()).findViewById(R.id.setStartPointToggleBtn);
            ToggleButton setWaypointToggleBtn = ((Activity)this. getContext()).findViewById(R.id.setWaypointToggleBtn);

            if (startCoordStatus) {
                if (canDrawRobot) {
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
                this.setStartCoord(column, row);
                startCoordStatus = false;

                try {
                    util.printMessage(context, "S", column, row);
                    sharedPreferences();
                    TextView sentMessage =  ((Activity)this.getContext()).findViewById(R.id.sentMessage);
                    sentMessage.setText(sharedPreferences.getString("sentText", ""));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if (setStartPointToggleBtn.isChecked())
                    setStartPointToggleBtn.toggle();
                this.invalidate();
                return true;
            }
            if (setWaypointStatus) {
                int[] waypointCoord = this.getWaypointCoord();
                if (waypointCoord[0] >= 1 && waypointCoord[1] >= 1)
                    cells[waypointCoord[0]][this.convertRow(waypointCoord[1])].setType("unexplored");
                setWaypointStatus = false;
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
                if (setWaypointToggleBtn.isChecked())
                    setWaypointToggleBtn.toggle();
                this.invalidate();
                return true;
            }
            if (setObstacleStatus) {
                this.setObstacleCoord(column, row);
                this.invalidate();
                return true;
            }
            if (setExploredStatus) {
                cells[column][20-row].setType("explored");
                this.invalidate();
                return true;
            }
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
                    hexStringExplored = infoJsonObject.getString("explored");
                    hexBigIntegerExplored = new BigInteger(hexStringExplored, 16);

                    exploredString = hexBigIntegerExplored.toString(2);
                    exploredString = exploredString.substring(2, 302);

                    int count_zero = 0;
                    int count_one = 0;

                    for (int z = 0; z < exploredString.length(); z++) {
                        char b = exploredString.charAt(z);
                        if (b == '0')
                            count_zero++;
                        else
                            count_one++;
                    }

                    int length = infoJsonObject.getInt("length");

                    hexStringObstacle = infoJsonObject.getString("obstacle");

                    if(hexStringObstacle.length()>0){
                        System.out.println(count_one);
                        hexBigIntegerObstacle = new BigInteger(hexStringObstacle, 16);
                        obstacleString = hexBigIntegerObstacle.toString(2);
                        int pad_number = length - obstacleString.length();

                        for(int j=0;j<pad_number;j++)
                            obstacleString = '0'+obstacleString;

                        obstacleString = obstacleString.substring(0,count_one);

                    }

                    int x, y;
                    for(int j =0; j<exploredString.length(); j++) {
                        y = 19 - (j / 15);
                        x = 1 + j - ((19 - y) * 15);
                        if ((String.valueOf(exploredString.charAt(j))).equals("1") && !cells[x][y].type.equals("robot")) {
                            cells[x][y].setType("explored");
                        } else if ((String.valueOf(exploredString.charAt(j))).equals("0") && !cells[x][y].type.equals("robot")) {
                            cells[x][y].setType("unexplored");
                        }
                    }
                    int kj = 0;
                    for (int row = ROW-1; row >= 0; row--)
                        for (int col = 1; col <= COL; col++)
                            if ((cells[col][row].type.equals("explored")||(cells[col][row].type.equals("robot"))) && kj < obstacleString.length()) { // ||cells[col][row].type.equals("arrow")
                                if ((String.valueOf(obstacleString.charAt(kj))).equals("1")) //  && !cells[col][row].type.equals("arrow")
                                    this.setObstacleCoord(col, 20 - row);
                                kj++;
                            }

                    int[] waypointCoord = this.getWaypointCoord();
                    if (waypointCoord[0] >= 1 && waypointCoord[1] >= 1)
                        cells[waypointCoord[0]][20-waypointCoord[1]].setType("waypoint");
                    break;
                case "image":
                    flag_for_image_coordinates = 0;
                    String imageString = mapInformation.getString("image");
                    image_string_output = imageString;
                    int length_image = imageString.length();
                    boolean flag_for_trio = false;
                    String image_individual_str = "";
                    int count = 1;
                    String image_type_string = "";
                    int x_coordinate_int = 0;
                    int y_coordinate_int = 0;

                    for(int z=0;z<length_image;z++)
                    {
                        char b = imageString.charAt(z);

                        if(flag_for_trio==true && (b == ',' || b==')'))
                        {
                            if(count == 1)
                            {
                                image_type_string = image_individual_str;
                                image_type[flag_for_image_coordinates] = Integer.parseInt(image_type_string);
                            }
                            else if(count == 2)
                            {
                                x_coordinate_int = Integer.parseInt(image_individual_str);
                                image_x_coordinate[flag_for_image_coordinates] = x_coordinate_int;
                            }
                            else if(count == 3)
                            {
                                y_coordinate_int = Integer.parseInt(image_individual_str);
                                image_y_coordinate[flag_for_image_coordinates] = y_coordinate_int;
                            }
                            count++;
                            image_individual_str = "";
                        }

                        if(b=='(')
                        {
                            flag_for_trio = true;
                        }
                        else if(b==')')
                        {
                            flag_for_trio = false;

                            flag_for_image_coordinates++;

                            this.setImageCoordinate(x_coordinate_int, y_coordinate_int, image_type_string);
                            count = 1;
                            image_individual_str = "";
                        }


                        if(flag_for_trio == true)
                        {
                            if(b!='(' && b!=',')
                                image_individual_str = image_individual_str + b;
                        }
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
                    robotStatusTextView =  ((Activity)this.getContext()).findViewById(R.id.robotStatusTextView);

                    for(int k =0; k< robotMovements.length(); k++){
                        char b = robotMovements.charAt(k);
                        switch(b){
                            case 'w':
                                moveRobot("forward");
                                System.out.println("FORWARD");
                                robotStatusTextView.setText("Moving Forward");
                                break;
                            case 'd':
                            case 'm':
                                moveRobot("right");
                                System.out.println("RIGHT");
                                robotStatusTextView.setText("Turning Right");
                                break;
                            case 'a':
                            case 'n':
                                moveRobot("left");
                                System.out.println("LEFT");
                                robotStatusTextView.setText("Turning Left");
                                break;
                            case 's':
                                moveRobot("back");
                                System.out.println("BACK");
                                robotStatusTextView.setText("Moving Backward");
                                break;
                            case 'c':
                                moveRobot("right");
                                moveRobot("right");
                                System.out.println("DOUBLE RIGHT");
                                break;

                            case '1':
                            case '2':
                            case '3':
                            case '4':
                            case '5':
                            case '6':
                            case '7':
                            case '8':
                            case '9':
                                for(int j=0;j<Character.getNumericValue(b);j++)
                                {
                                    moveRobot("forward");
                                    System.out.println("FORWARD");
                                    robotStatusTextView.setText("Moving Forward");
                                }
                                break;
                            case '0':
                                for(int j=0;j<10;j++)
                            {
                                moveRobot("forward");
                                System.out.println("FORWARD");
                                robotStatusTextView.setText("Moving Forward");
                            }
                                break;
                            case '!':
                                for(int j=0;j<11;j++)
                                {
                                    moveRobot("forward");
                                    System.out.println("FORWARD");
                                    robotStatusTextView.setText("Moving Forward");
                                }
                                break;
                            case '@':
                                for(int j=0;j<12;j++)
                                {
                                    moveRobot("forward");
                                    System.out.println("FORWARD");
                                    robotStatusTextView.setText("Moving Forward");
                                }
                                break;
                            case '#':
                                for(int j=0;j<13;j++)
                            {
                                moveRobot("forward");
                                System.out.println("FORWARD");
                                robotStatusTextView.setText("Moving Forward");
                            }
                            break;
                            case '$':
                                for(int j=0;j<14;j++)
                                {
                                    moveRobot("forward");
                                    System.out.println("FORWARD");
                                    robotStatusTextView.setText("Moving Forward");
                                }
                                break;
                            case '%':
                                for(int j=0;j<15;j++)
                                {
                                    moveRobot("forward");
                                    System.out.println("FORWARD");
                                    robotStatusTextView.setText("Moving Forward");
                                }
                                break;
                            case '^':
                                for(int j=0;j<16;j++)
                            {
                                moveRobot("forward");
                                System.out.println("FORWARD");
                                robotStatusTextView.setText("Moving Forward");
                            }
                            break;
                            case '&':
                                for(int j=0;j<17;j++)
                                {
                                    moveRobot("forward");
                                    System.out.println("FORWARD");
                                    robotStatusTextView.setText("Moving Forward");
                                }
                                break;
                        }
                    }
                    break;
            }
        }
        Util.showLog(TAG,"Exiting updateMapInformation");
        this.invalidate();
    }

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

    private void renderImages(Canvas canvas, ArrayList<String[]> imageCoordinates) {
        RectF rect;

        for (int i = 0; i < imageCoordinates.size(); i++) {

            if (!imageCoordinates.get(i)[2].equals("placeholder")) {

                int column = Integer.parseInt(imageCoordinates.get(i)[0]);

                int row = convertRow(Integer.parseInt(imageCoordinates.get(i)[1]));

                rect = new RectF(column * cellSize, row * cellSize, (column + 1) * cellSize, (row + 1) * cellSize);
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
        }
    }

    public void resetMap() {
        TextView robotStatusTextView =  ((Activity)this.getContext()).findViewById(R.id.robotStatusTextView);
        ToggleButton manualAutoToggleBtn = ((Activity)this.getContext()).findViewById(R.id.manualAutoToggleBtn);
        updateRobotAxis(0, 0, "None");
        robotStatusTextView.setText("status");
        sharedPreferences();
        editor.putString("receivedText", "");
        editor.putString("sentText", "");
        editor.putString("image", "");
        editor.commit();
        this.toggleCheckedBtn("None");

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

    public void setEndCoord(int col, int row) {
        row = this.convertRow(row);
        for (int x = col - 1; x <= col + 1; x++)
            for (int y = row - 1; y <= row + 1; y++)
                cells[x][y].setType("end");
    }

    public void setStartCoord(int col, int row) {
        int oldCol=startCoord[0];
        int oldRow=startCoord[1];

        if(startCoord[1] != -1 && startCoord[0] !=-1){
            if(oldCol == 1){
                oldCol = oldCol + 1;
            }
            else if(oldCol == 15){
                oldCol = oldCol - 1;
            }
            if(oldRow == 1){
                oldRow = oldRow + 1;
            }
            else if(oldRow == 20){
                oldRow = oldRow - 1;
            }

            oldRow = this.convertRow(oldRow);
            for (int x = oldCol - 1; x <= oldCol + 1; x++)
                for (int y = oldRow - 1; y <= oldRow + 1; y++)
                    cells[x][y].setType("unexplored");
        }

        startCoord[1] = row;
        startCoord[0] = col;

        if (this.getStartCoordStatus())
            this.setCurCoord(col, row, "up");
    }

    private int[] getStartCoord() {
        return startCoord;
    }

    public void setCurCoord(int col, int row, String direction) {
        this.setRobotDirection(direction);

        if(col == 1){
            col = col + 1;
        }
        else if(col == 15){
            col = col - 1;
        }
        if(row == 1){
            row = row + 1;
        }
        else if(row == 20){
            row = row - 1;
        }

        this.updateRobotAxis(col, row, direction);

        curCoord[0] = col;
        curCoord[1] = row;

        row = this.convertRow(row);
        int x = col-1;
        while(x<=(col+1))
        {
            int y = row-1;
            while(y<=(row+1))
            {
                cells[x][y].setType("robot");
                y++;
            }
            x++;
        }
    }

    public int[] getCurCoord() {
        return curCoord;
    }

    public void setRobotDirection(String direction) {
        this.sharedPreferences();
        robotDirection = direction;
        editor.putString("direction", direction);
        editor.commit();
        this.invalidate();;
    }

    public String getRobotDirection() {
        return robotDirection;
    }


    private void setWaypointCoord(int col, int row) throws JSONException {
        waypointCoord[0] = col;
        waypointCoord[1] = row;

        row = this.convertRow(row);
        cells[col][row].setType("waypoint");
        util.printMessage(context, "W", waypointCoord[0], waypointCoord[1]);
    }

    public int[] getWaypointCoord() {
        return waypointCoord;
    }

    private void setObstacleCoord(int col, int row) {
        int[] obstacleCoord = new int[]{col, row};
        GridMap.obstacleCoord.add(obstacleCoord);
        row = this.convertRow(row);
        cells[col][row].setType("obstacle");
    }

    private ArrayList<int[]> getObstacleCoord() {
        return obstacleCoord;
    }

    private void setOldRobotCoord(int oldCol, int oldRow) {
        oldCoord[0] = oldCol;
        oldCoord[1] = oldRow;
        oldRow = this.convertRow(oldRow);
        for (int x = oldCol - 1; x <= oldCol + 1; x++)
            for (int y = oldRow - 1; y <= oldRow + 1; y++)
                cells[x][y].setType("explored");
    }

    private int[] getOldRobotCoord() {
        return oldCoord;
    }

    private void setCellSize(float cellSize) {
        GridMap.cellSize = cellSize;
    }

    private float getCellSize() {
        return cellSize;
    }

    public void setReceivedJsonObject(JSONObject receivedJsonObject) {
        GridMap.receivedJsonObject = receivedJsonObject;
        backupMapInformation = receivedJsonObject;
    }

    public JSONObject getReceivedJsonObject() {
        return receivedJsonObject;
    }

    private void setImageCoordinate(int column, int row, String imageType) {
        String[] imageCoordinates = new String[3];
        column += 1;
        row += 1;

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
            }
            catch (Exception e) {
            }
        }

    }

    private ArrayList<String[]> getImageCoordinates() {
        return imageCoordinates;
    }

    private void sharedPreferences() {
        sharedPreferences = this.getContext().getSharedPreferences("RobotControlActivity", Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }
}
