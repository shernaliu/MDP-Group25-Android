package com.example.mdp_group25;

import android.app.Activity;
import android.graphics.Bitmap;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.TextView;
import android.widget.ImageButton;
import android.widget.ToggleButton;
import android.widget.Toast;
import android.view.View;
import java.math.BigInteger;
import org.json.JSONException;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import androidx.annotation.Nullable;

public class GridMap extends View {
    private static Context ctx;
    private static final String TAG = "GridMap";
    SharedPreferences sharedPrefs;
    SharedPreferences.Editor editor;
    private static float cellSize;
    private static final int COL = 15, ROW = 20; // 20x15
    private static JSONObject backupMapInformation;
    private static JSONObject rcveJsonObject = new JSONObject();
    private static Cell[][] cells;
    private static String robotDir = "None";
    private static int[] startCoord = new int[]{-1, -1};       // col, row
    private static int[] currCoord = new int[]{-1, -1};         // col, row
    private static int[] oldCoord = new int[]{-1, -1};         // col, row
    private static int[] waypointCoord = new int[]{-1, -1};    // col, row
    private static ArrayList<String[]> imgCoord = new ArrayList<>();
    private static ArrayList<int[]> obsCoord = new ArrayList<>(); // store all the obstacles coordinates
    private static boolean automaticUpdate = false;          // false: = manual, true = auto mode
    private static boolean gridMapDrawn = false;
    private static boolean isDrawableRobot = false;
    private static boolean status_setWayPoint = false;
    private static boolean status_startCoord = false;
    private static boolean status_setObstacle = false;
    private static boolean status_setExplored = false;
    private static boolean validPos = false;
    private Util util = new Util();
    int image_type[] = new int[]{-99, -99, -99, -99, -99};
    int image_x_coordinate[] = new int[]{-99, -99, -99, -99, -99};
    int image_y_coordinate[] = new int[]{-99, -99, -99, -99, -99};
    int flag_for_image_coordinates = 0;
    private Bitmap img = BitmapFactory.decodeResource(getResources(), R.drawable.arrow_error);
    private Paint blackPaint = new Paint();
    private Paint obstacleColor = new Paint();
    private Paint robotColor = new Paint();
    private Paint startColor = new Paint();
    private Paint endColor = new Paint();
    private Paint waypointColor = new Paint();
    private Paint exploredColor = new Paint();
    private Paint unexploredColor = new Paint();
    private Paint fastestPathColor = new Paint();
    private Paint imageColor = new Paint();
    ToggleButton setStartPointToggleBtn, setWaypointToggleBtn;
    TextView robotStatusTv;
    ToggleButton manualAutoToggleBtn;

    /**
     * full constructor of the GridMap
     * @param ctx
     * @param attrs
     */
    public GridMap(Context ctx, @Nullable AttributeSet attrs) {
        super(ctx, attrs);
        this.ctx = ctx;
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

    private void init(@Nullable AttributeSet attrbuteSets) {
        setWillNotDraw(false);
    }

    public GridMap(Context ctx) {
        super(ctx);
        this.ctx = ctx;
        init(null);
    }

    /**
     * method to convert the row given a row value.
     * @param row
     * @return
     */
    private int rowConversion(int row) {
        return (20 - row);
    }

    private void createCell() {
        cells = new Cell[COL + 1][ROW + 1];
        this.calculateDimen();
        cellSize = this.getCellSize();
        // starting from the value of 1.5
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

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        ArrayList<String[]> imageCoords = this.getImageCoordinates();
        int[] curCoord = this.getCurCoord();
        if (!this.getMapDrawn()) {
            canvas.drawColor(Color.parseColor("#FFFFFF"));
            this.createCell();
            this.setEndCoords(14, 19);
            gridMapDrawn = true;
        }

        this.drawEachCell(canvas);
        this.drawGridNumber(canvas);
        if (this.getCanDrawRobot())
            this.drawRobot(canvas, curCoord);
        this.renderImages(canvas, imageCoords);
    }

    /**
     * function to move the robot in the specified direction
     * @param dir direction to move to
     */
    public void moveRobot(String dir) {
        setValidPosition(false);
        boolean flag = true;
        int[] curCoord = this.getCurCoord();
        ArrayList<int[]> obstacleCoord = this.getObstacleCoord();
        this.setOldRobotCoord(curCoord[0], curCoord[1]);
        int[] oldCoord = this.getOldRobotCoord();
        String directionOfRobot = getRobotDirection();
        String backupDirection = directionOfRobot;
        if (directionOfRobot.equals("up")) {
            if (dir.equals("forward")) {
                if (curCoord[1] != 19) {
                    curCoord[1] += 1;
                    validPos = true;
                }
            } else if (dir.equals("back")) {
                if (curCoord[1] != 2) {
                    curCoord[1] -= 1;
                    validPos = true;
                }
            } else if (dir.equals("right"))
                directionOfRobot = "right";
            else if (dir.equals("left"))
                directionOfRobot = "left";
            else
                directionOfRobot = "error up";
        } else if (directionOfRobot.equals("right")) {
            if (dir.equals("forward")) {
                if (curCoord[0] != 14) {
                    curCoord[0] += 1;
                    validPos = true;
                }
            } else if (dir.equals("back")) {
                if (curCoord[0] != 2) {
                    curCoord[0] -= 1;
                    validPos = true;
                }
            } else if (dir.equals("right"))
                directionOfRobot = "down";
            else if (dir.equals("left"))
                directionOfRobot = "up";
            else
                directionOfRobot = "error right";
        } else if (directionOfRobot.equals("down")) {
            if (dir.equals("forward")) {
                if (curCoord[1] != 2) {
                    curCoord[1] -= 1;
                    validPos = true;
                }
            } else if (dir.equals("back")) {
                if (curCoord[1] != 19) {
                    curCoord[1] += 1;
                    validPos = true;
                }
            } else if (dir.equals("right"))
                directionOfRobot = "left";
            else if (dir.equals("left"))
                directionOfRobot = "right";
            else
                directionOfRobot = "error down";
        } else if (directionOfRobot.equals("left")) {
            if (dir.equals("forward")) {
                if (curCoord[0] != 2) {
                    curCoord[0] -= 1;
                    validPos = true;
                }
            } else if (dir.equals("back")) {
                if (curCoord[0] != 14) {
                    curCoord[0] += 1;
                    validPos = true;
                }
            } else if (dir.equals("right"))
                directionOfRobot = "up";
            else if (dir.equals("left"))
                directionOfRobot = "down";
            else
                directionOfRobot = "error left";
        }

        // update the current coordinates and direction of robot
        flag = getValidPosition();
        if (flag == true)
        {
            for (int x = curCoord[0] - 1; x <= curCoord[0] + 1; x++) {
                for (int y = curCoord[1] - 1; y <= curCoord[1] + 1; y++) {
                    for (int i = 0; i < obstacleCoord.size(); i++)
                    {
                        setValidPosition(true);
                    }
                    if (getValidPosition() == false)
                        break;
                }
                if (getValidPosition() == false)
                    break;
            }
    }
        if (getValidPosition() == true) {
            this.setCurCoord(curCoord[0], curCoord[1], directionOfRobot);
        } else {
            if (dir.equals("forward") || dir.equals("back"))
                directionOfRobot = backupDirection;
            this.setCurCoord(oldCoord[0], oldCoord[1], directionOfRobot);
        }
        this.invalidate();
    }

    private void updateRobotAxis(int col, int row, String direction) {
        TextView xAxisTextView = ((Activity) this.getContext()).findViewById(R.id.xAxisTextView);
        TextView yAxisTextView = ((Activity) this.getContext()).findViewById(R.id.yAxisTextView);
        TextView directionAxisTextView = ((Activity) this.getContext()).findViewById(R.id.directionAxisTextView);
        xAxisTextView.setText(String.valueOf(col - 1));
        yAxisTextView.setText(String.valueOf(row - 1));
        directionAxisTextView.setText(direction);
    }

    public void setAutomaticUpdate(boolean autoUpdate) throws JSONException {
        if (autoUpdate == false)
            backupMapInformation = this.getRcveJsonObject();
        else {
            setRcveJsonObject(backupMapInformation);
            backupMapInformation = null;
            this.updateMapInfo();
        }
        GridMap.automaticUpdate = autoUpdate;
    }

    public void cellInitialization(String stats, int x, int y)
    {
        for(int i=0;i<3;i++)
            for(int j=0;j<3;j++)
                cells[x+i][y+j].setType(stats);
    }

    @Override
    public boolean onTouchEvent(MotionEvent evnt) {
        if (evnt.getAction() == MotionEvent.ACTION_DOWN && this.getAutomaticUpdate() == false) {
            int column = (int) (evnt.getX() / cellSize);
            int row = this.rowConversion((int) (evnt.getY() / cellSize));
            ToggleButton setWaypointToggleBtn = ((Activity) this.getContext()).findViewById(R.id.setWaypointToggleBtn);
            ToggleButton setStartPointToggleBtn = ((Activity) this.getContext()).findViewById(R.id.setStartPointToggleBtn);

            // if startCoordStatus true
            if (status_startCoord == true) {
                // remove the old starting coordinates
                if (isDrawableRobot == true) {
                    // converting screen coordinates
                    int[] startCoord = this.getStartCoord();
                    if (startCoord[0] >= 2 && startCoord[1] >= 2) {
                        startCoord[1] = this.rowConversion(startCoord[1]);
                        cellInitialization("unexplored",startCoord[0] - 1,startCoord[1] - 1);
                    }
                } else
                    isDrawableRobot = true;

                this.setStartCoords(column, row);
                status_startCoord = false;

                try {
                    util.printMessage(ctx, "S", column, row);
                    sharedPreferences();
                    TextView sentMessage = ((Activity) this.getContext()).findViewById(R.id.sentMessage);
                    sentMessage.setText(sharedPrefs.getString("sentText", ""));
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                // if setStartPointToggleBtn is checked, then uncheck it!
                if (setStartPointToggleBtn.isChecked())
                    setStartPointToggleBtn.toggle();
                this.invalidate();
                return true;
            }

            // if status_setWayPoint is true
            if (status_setWayPoint == true) {
                int[] waypointCoord = this.getWaypointCoord();

                if (waypointCoord[0] >= 1 && waypointCoord[1] >= 1)
                    cells[waypointCoord[0]][this.rowConversion(waypointCoord[1])].setType("unexplored");
                status_setWayPoint = false;

                // print
                try {
                    this.setWaypointCoord(column, row);
                    TextView xAxisTextview = ((Activity) this.getContext()).findViewById(R.id.xAxisTextViewWP);
                    TextView yAxisTextview = ((Activity) this.getContext()).findViewById(R.id.yAxisTextViewWP);

                    xAxisTextview.setText(Integer.toString(row - 1));
                    yAxisTextview.setText(Integer.toString(column - 1));

                    sharedPreferences();
                    TextView sentMessage = ((Activity) this.getContext()).findViewById(R.id.sentMessage);
                    sentMessage.setText(sharedPrefs.getString("sentText", ""));
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                // if setWaypointToggleBtn is checked, then uncheck it
                if (setWaypointToggleBtn.isChecked() == true)
                    setWaypointToggleBtn.toggle();
                this.invalidate();
                return true;
            }

            // set the type to explored
            if (status_setExplored == true) {
                cells[column][20 - row].setType("explored");
                this.invalidate();
                return true;
            }

            if (status_setObstacle == true) {
                this.setObstacleCoord(column, row);
                this.invalidate();
                return true;
            }

        }
        return false;
    }

    private void drawEachCell(Canvas canvas) {
        for (int x = 1; x <= COL; x++)
            for (int y = 0; y < ROW; y++)
                for (int i = 0; i < this.getImageCoordinates().size(); i++)
                    canvas.drawRect(cells[x][y].startX, cells[x][y].startY, cells[x][y].endX, cells[x][y].endY, cells[x][y].paint);
    }

    // draw grid number on grid map
    private void drawGridNumber(Canvas canvas) {
        // draw x-axis number
        for (int x = 1; x <= COL; x++) {
            // for 2 digit number
            float x1,y1;
            String i = Integer.toString(x-1);
            y1 = cells[x][20].startY + (cellSize / 3);
            if(x<10)
                x1 = cells[x][20].startX + (cellSize / 3);
            else
                x1 = cells[x][20].startX + (cellSize / 5);
            canvas.drawText(i,x1,y1,blackPaint);
        }
        // draw y-axis number
        for (int y = 0; y < ROW; y++) {
            float x1,y1;
            String i = Integer.toString(20 - (y + 1));
            y1 = cells[0][y].startY + (cellSize / 1.5f);
            if(y<10)
                x1 = cells[0][y].startX + (cellSize / 2);
            else
                x1 = cells[0][y].startX + (cellSize / 1.5f);
            canvas.drawText(i,x1,y1,blackPaint);
            }
    }


    /**
     * function to draw the robot given a canvas and the current coordinates
     * @param cvs
     * @param curCoord
     */
    private void drawRobot(Canvas cvs, int[] curCoord) {
        int aRowCoord = this.rowConversion(curCoord[1]);
        // remove horizontal and vertical lines
        for (int y = aRowCoord; y <= aRowCoord + 1; y++)
            cvs.drawLine(cells[curCoord[0] - 1][y].startX, cells[curCoord[0] - 1][y].startY - (cellSize / 30), cells[curCoord[0] + 1][y].endX, cells[curCoord[0] + 1][y].startY - (cellSize / 30), robotColor);
        for (int x = curCoord[0] - 1; x < curCoord[0] + 1; x++)
            cvs.drawLine(cells[x][aRowCoord - 1].startX - (cellSize / 30) + cellSize, cells[x][aRowCoord - 1].startY, cells[x][aRowCoord + 1].startX - (cellSize / 30) + cellSize, cells[x][aRowCoord + 1].endY, robotColor);
        float l1x1,l1y1,l1x2,l1y2;
        float l2x1,l2y1,l2x2,l2y2;
        // given the direction of robot, draw accordingly
        switch (this.getRobotDirection()) {
            case "up":
                l1x1 = cells[curCoord[0] - 1][aRowCoord + 1].startX;
                l1y1 = cells[curCoord[0] - 1][aRowCoord + 1].endY;
                l1x2 = (cells[curCoord[0]][aRowCoord - 1].startX + cells[curCoord[0]][aRowCoord - 1].endX) / 2;
                l1y2 = cells[curCoord[0]][aRowCoord - 1].startY;

                l2x1 = (cells[curCoord[0]][aRowCoord - 1].startX + cells[curCoord[0]][aRowCoord - 1].endX) / 2;
                l2y1 = cells[curCoord[0]][aRowCoord - 1].startY;
                l2x2 = cells[curCoord[0] + 1][aRowCoord + 1].endX;
                l2y2 = cells[curCoord[0] + 1][aRowCoord + 1].endY;
                break;
            case "down":
                l1x1 = cells[curCoord[0] - 1][aRowCoord - 1].startX;
                l1y1 = cells[curCoord[0] - 1][aRowCoord - 1].startY;
                l1x2 = (cells[curCoord[0]][aRowCoord + 1].startX + cells[curCoord[0]][aRowCoord + 1].endX) / 2;
                l1y2 = cells[curCoord[0]][aRowCoord + 1].endY;

                l2x1 = (cells[curCoord[0]][aRowCoord + 1].startX + cells[curCoord[0]][aRowCoord + 1].endX) / 2;
                l2y1 = cells[curCoord[0]][aRowCoord + 1].endY;
                l2x2 = cells[curCoord[0] + 1][aRowCoord - 1].endX;
                l2y2 = cells[curCoord[0] + 1][aRowCoord - 1].startY;

                break;
            case "right":
                l1x1 = cells[curCoord[0] - 1][aRowCoord - 1].startX;
                l1y1 = cells[curCoord[0] - 1][aRowCoord - 1].startY;
                l1x2 = cells[curCoord[0] + 1][aRowCoord].endX;
                l1y2 = cells[curCoord[0] + 1][aRowCoord - 1].endY + (cells[curCoord[0] + 1][aRowCoord].endY - cells[curCoord[0] + 1][aRowCoord - 1].endY) / 2;

                l2x1 = cells[curCoord[0] + 1][aRowCoord].endX;
                l2y1 = cells[curCoord[0] + 1][aRowCoord - 1].endY + (cells[curCoord[0] + 1][aRowCoord].endY - cells[curCoord[0] + 1][aRowCoord - 1].endY) / 2;
                l2x2 = cells[curCoord[0] - 1][aRowCoord + 1].startX;
                l2y2 = cells[curCoord[0] - 1][aRowCoord + 1].endY;
                break;
            case "left":
                l1x1 = cells[curCoord[0] + 1][aRowCoord - 1].endX;
                l1y1 = cells[curCoord[0] + 1][aRowCoord - 1].startY;
                l1x2 = cells[curCoord[0] - 1][aRowCoord].startX;
                l1y2 = cells[curCoord[0] - 1][aRowCoord - 1].endY + (cells[curCoord[0] - 1][aRowCoord].endY - cells[curCoord[0] - 1][aRowCoord - 1].endY) / 2;

                l2x1 = cells[curCoord[0] - 1][aRowCoord].startX;
                l2y1 = cells[curCoord[0] - 1][aRowCoord - 1].endY + (cells[curCoord[0] - 1][aRowCoord].endY - cells[curCoord[0] - 1][aRowCoord - 1].endY) / 2;
                l2x2 = cells[curCoord[0] + 1][aRowCoord + 1].endX;
                l2y2 = cells[curCoord[0] + 1][aRowCoord + 1].endY;
                break;
            default:
                l1x1 = 0;
                l1y1 = 0;
                l1x2 = 0;
                l1y2 = 0;

                l2x1 = 0;
                l2y1 = 0;
                l2x2 = 0;
                l2y2 = 0;
                break;
        }
        cvs.drawLine(l1x1,l1y1,l1x2,l1y2,blackPaint);
        cvs.drawLine(l2x1,l2y1,l2x2,l2y2,blackPaint);
    }

    private void calculateDimen() {
        this.setCellSize(getWidth() / (COL + 1));
    }

    public void updateMapInfo() throws JSONException {
        JSONObject mapInfo = this.getRcveJsonObject();
        JSONObject infoJsonObj;
        String hexStrObstacle, hexStrExplored, exploredStr;
        BigInteger hexExploredBI, hexObstacleBI;
        String obstacleStr = "";

        if (mapInfo == null)
            return;

        for (int i = 0; i < mapInfo.names().length(); i++) {
            switch (mapInfo.names().getString(i)) {
                case "map":
                    infoJsonObj = mapInfo.getJSONObject("map");
                    hexStrExplored = infoJsonObj.getString("explored");
                    hexExploredBI = new BigInteger(hexStrExplored, 16);
                    exploredStr = hexExploredBI.toString(2);
                    // remove padding bits
                    exploredStr = exploredStr.substring(2, 302);
                    int zeroCnt = 0;
                    int oneCnt = 0;
                    for (int z = 0; z < exploredStr.length(); z++) {
                        char b = exploredStr.charAt(z);
                        if (b == '0')
                            zeroCnt++;
                        else
                            oneCnt++;
                    }
                    int length = infoJsonObj.getInt("length");
                    hexStrObstacle = infoJsonObj.getString("obstacle");
                    if (hexStrObstacle.length() > 0) {
                        hexObstacleBI = new BigInteger(hexStrObstacle, 16);
                        obstacleStr = hexObstacleBI.toString(2);
                        int pad_number = length - obstacleStr.length();
                        for (int j = 0; j < pad_number; j++)
                            obstacleStr = '0' + obstacleStr;
                        obstacleStr = obstacleStr.substring(0, oneCnt);
                    }
                    int x, y;
                    for (int u = 0; u < exploredStr.length(); u++) {
                        String str = "";
                        y = 19 - (u / 15);
                        x = 1 + u - ((19 - y) * 15);
                        if ((String.valueOf(exploredStr.charAt(u))).equals("1") && !cells[x][y].type.equals("robot")) {
                            str = "explored";
                        } else if ((String.valueOf(exploredStr.charAt(u))).equals("0") && !cells[x][y].type.equals("robot")) {
                            str = "unexplored";
                        }
                        cells[x][y].setType(str);
                    }
                    int v = 0;
                    for (int row = ROW - 1; row >= 0; row--)
                        for (int col = 1; col <= COL; col++)
                            if ((cells[col][row].type.equals("explored") || (cells[col][row].type.equals("robot"))) && v < obstacleStr.length()) {
                                if ((String.valueOf(obstacleStr.charAt(v))).equals("1"))
                                    this.setObstacleCoord(col, 20 - row);
                                v++;
                            }
                    int[] coord_waypoint = this.getWaypointCoord();
                    if (coord_waypoint[0] >= 1 && coord_waypoint[1] >= 1)
                        cells[coord_waypoint[0]][20 - coord_waypoint[1]].setType("waypoint");
                    break;
                case "image":
                    JSONArray jsonImgArr = mapInfo.getJSONArray("image");
                    JSONObject imgJson;
                    for (int j = 0; j < jsonImgArr.length(); j++) {
                        imgJson = jsonImgArr.getJSONObject(j);
                        String imageString = imgJson.getString("imageString");
                        String imageX = imageString.substring(0, 2);
                        image_x_coordinate[flag_for_image_coordinates] = Integer.parseInt(imageX);
                        String imageY = imageString.substring(2, 4);
                        image_y_coordinate[flag_for_image_coordinates] = Integer.parseInt(imageY);
                        int typeOfImg = Integer.parseInt(imageString.substring(4));
                        image_type[flag_for_image_coordinates] = typeOfImg;
                        flag_for_image_coordinates++;
                        String imageTypeString = Integer.toString(typeOfImg);
                        this.setImageCoordinate(Integer.parseInt(imageX), Integer.parseInt(imageY), imageTypeString);
                    }
                    break;
                case "status":
                    String status = mapInfo.getString("status");
                    TextView robotStatus = ((Activity) this.getContext()).findViewById(R.id.robotStatusTextView);
                    robotStatus.setText(status);
                    break;
                case "robot":
                    String movementsRobot = mapInfo.getString("robot");
                    robotStatus = ((Activity) this.getContext()).findViewById(R.id.robotStatusTextView);
                    for(int k =0; k< movementsRobot.length(); k++){
                        char b = movementsRobot.charAt(k);
                        switch(b){
                            case 'w':
                                moveRobot("forward");
                                robotStatus.setText("Moving Forward");
                                break;
                            case 'd':
                            case 'm':
                                moveRobot("right");
                                robotStatus.setText("Turning Right");
                                break;
                            case 'a':
                            case 'n':
                                moveRobot("left");
                                robotStatus.setText("Turning Left");
                                break;
                            case 's':
                                moveRobot("back");
                                robotStatus.setText("Moving Backward");
                                break;
                            case 'c':
                                moveRobot("right");
                                moveRobot("right");
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
                                    robotStatus.setText("Moving Forward");
                                }
                                break;
                            case '0':
                                for(int j=0;j<10;j++)
                                {
                                    moveRobot("forward");
                                    robotStatus.setText("Moving Forward");
                                }
                                break;
                            case '!':
                                for(int j=0;j<11;j++)
                                {
                                    moveRobot("forward");
                                    robotStatus.setText("Moving Forward");
                                }
                                break;
                            case '@':
                                for(int j=0;j<12;j++)
                                {
                                    moveRobot("forward");
                                    robotStatus.setText("Moving Forward");
                                }
                                break;
                            case '#':
                                for(int j=0;j<13;j++)
                                {
                                    moveRobot("forward");
                                    robotStatus.setText("Moving Forward");
                                }
                                break;
                            case '$':
                                for(int j=0;j<14;j++)
                                {
                                    moveRobot("forward");
                                    robotStatus.setText("Moving Forward");
                                }
                                break;
                            case '%':
                                for(int j=0;j<15;j++)
                                {
                                    moveRobot("forward");
                                    robotStatus.setText("Moving Forward");
                                }
                                break;
                            case '^':
                                for(int j=0;j<16;j++)
                                {
                                    moveRobot("forward");
                                    robotStatus.setText("Moving Forward");
                                }
                                break;
                            case '&':
                                for(int j=0;j<17;j++)
                                {
                                    moveRobot("forward");
                                    robotStatus.setText("Moving Forward");
                                }
                                break;
                        }
                    }
                    break;
            }
        }
        this.invalidate();
    }

    public void toggleCheckedBtn(String button) {
        setStartPointToggleBtn = ((Activity) this.getContext()).findViewById(R.id.setStartPointToggleBtn);
        setWaypointToggleBtn = ((Activity) this.getContext()).findViewById(R.id.setWaypointToggleBtn);
        if (!button.equals("setStartPointToggleBtn"))
            if (setStartPointToggleBtn.isChecked() == true) {
                this.setStartCoordStatus(false);
                setStartPointToggleBtn.toggle();
            }
        if (!button.equals("setWaypointToggleBtn"))
            if (setWaypointToggleBtn.isChecked() == true) {
                this.setWaypointStatus(false);
                setWaypointToggleBtn.toggle();
            }
    }

    /**
     * function to render the images using the canvas and the image coordinates
     * @param canvas
     * @param imgCoord
     */
    private void renderImages(Canvas canvas, ArrayList<String[]> imgCoord) {
        RectF rect;
        for (int i = 0; i < imgCoord.size(); i++) {
            if (!imgCoord.get(i)[2].equals("placeholder")) {
                int column = Integer.parseInt(imgCoord.get(i)[0]);
                int row = rowConversion(Integer.parseInt(imgCoord.get(i)[1]));
                rect = new RectF(column * cellSize, row * cellSize, (column + 1) * cellSize, (row + 1) * cellSize);
                Util.showLog(TAG, imgCoord.get(i)[2]);
                int number = Integer.parseInt(imgCoord.get(i)[2]);
                if(number == 1)
                    img = BitmapFactory.decodeResource(getResources(), R.drawable.one);
                else if(number == 2)
                    img = BitmapFactory.decodeResource(getResources(), R.drawable.two);
                else if(number == 3)
                    img = BitmapFactory.decodeResource(getResources(), R.drawable.three);
                else if(number == 4)
                    img = BitmapFactory.decodeResource(getResources(), R.drawable.four);
                else if(number == 5)
                    img = BitmapFactory.decodeResource(getResources(), R.drawable.five);
                else if(number == 6)
                    img = BitmapFactory.decodeResource(getResources(), R.drawable.six);
                else if(number == 7)
                    img = BitmapFactory.decodeResource(getResources(), R.drawable.seven);
                else if(number == 8)
                    img = BitmapFactory.decodeResource(getResources(), R.drawable.eight);
                else if(number == 9)
                    img = BitmapFactory.decodeResource(getResources(), R.drawable.nine);
                else if(number == 10)
                    img = BitmapFactory.decodeResource(getResources(), R.drawable.ten);
                else if(number == 11)
                    img = BitmapFactory.decodeResource(getResources(), R.drawable.eleven);
                else if(number == 12)
                    img = BitmapFactory.decodeResource(getResources(), R.drawable.twelve);
                else if(number == 13)
                    img = BitmapFactory.decodeResource(getResources(), R.drawable.thirteen);
                else if(number == 14)
                    img = BitmapFactory.decodeResource(getResources(), R.drawable.fourteen);
                else if(number == 15)
                    img = BitmapFactory.decodeResource(getResources(), R.drawable.fifteen);
                canvas.drawBitmap(img, null, rect, null);
            }
        }
    }

    /**
     * function to reset the grid map values, as well as the shared prefs and other stuff
     */
    public void resetMap() {
        robotStatusTv = ((Activity) this.getContext()).findViewById(R.id.robotStatusTextView);
        manualAutoToggleBtn = ((Activity) this.getContext()).findViewById(R.id.manualAutoToggleBtn);
        updateRobotAxis(0, 0, "None");
        robotStatusTv.setText("status");
        sharedPreferences();
        editor.putString("receivedText", "");
        editor.putString("sentText", "");
        editor.putString("image", "");
        editor.commit();
        initialize();
    }

    public void initialize()
    {
        this.toggleCheckedBtn("None");
        rcveJsonObject = null;
        backupMapInformation = null;
        startCoord = new int[]{-1, -1};
        currCoord = new int[]{-1, -1};
        oldCoord = new int[]{-1, -1};
        robotDir = "None";
        automaticUpdate = false;
        imgCoord = new ArrayList<>();
        obsCoord = new ArrayList<>();
        waypointCoord = new int[]{-1, -1};
        gridMapDrawn = false;
        isDrawableRobot = false;
        validPos = false;
        this.invalidate();
    }

    public boolean getMapDrawn() {
        return gridMapDrawn;
    }

    public boolean getAutomaticUpdate() {
        return automaticUpdate;
    }

    public void setSetObstacleStatus(boolean status) {
        status_setObstacle = status;
    }

    private void setValidPosition(boolean status) {
        validPos = status;
    }

    public boolean getValidPosition() {
        return validPos;
    }

    public void setExploredStatus(boolean status) {
        status_setExplored = status;
    }

    public boolean getExploredStatus() {
        return status_setExplored;
    }

    public void setStartCoordStatus(boolean status) {
        status_startCoord = status;
    }

    private boolean getStartCoordStatus() {
        return status_startCoord;
    }

    public boolean getCanDrawRobot() {
        return isDrawableRobot;
    }

    public void setWaypointStatus(boolean status) {
        status_setWayPoint = status;
    }



    /**
     * setter to set the end coordinates
     * @param column
     * @param row
     */
    public void setEndCoords(int column, int row) {
        row = this.rowConversion(row);
        int x = column-1;
        while(x<=column+1) {
            int y = row - 1;
            while (y <= row + 1) {
                cells[x][y].setType("end");
                y++;
            }
            x++;
        }
    }

    /**
     * setter for the starting coordinates of the robot
     * @param column
     * @param row
     */
    public void setStartCoords(int column, int row) {
        int oldCol = startCoord[0];
        int oldRow = startCoord[1];
        if (startCoord[1] != -1 & startCoord[0] != -1) {
            // column MUST NOT BE 1 / 15
            if (oldCol == 1) {
                oldCol = oldCol + 1;
            } else if (oldCol == 15) {
                oldCol = oldCol - 1;
            }
            // row MUST NOT BE 1 / 20
            if (oldRow == 1) {
                oldRow = oldRow + 1;
            } else if (oldRow == 20) {
                oldRow = oldRow - 1;
            }
            oldRow = this.rowConversion(oldRow);
            // set cell to unexplored if start coordinates hasnt been setted
            cellInitialization("unexplored",oldCol - 1, oldRow - 1);
        }
        startCoord[1] = row;
        startCoord[0] = column;

        if (this.getStartCoordStatus())
            this.setCurCoord(column, row, "up");
    }


    /**
     * getter for the starting coordinates of the robot
     * @return
     */
    private int[] getStartCoord() {
        return startCoord;
    }

    /**
     * setter for the current coordinate of the robot
     * @param column
     * @param row
     * @param robotDir direction of the robot
     */
    public void setCurCoord(int column, int row, String robotDir) {
        this.setRobotDirection(robotDir);
        // column MUST NOT BE 1 / 15
        if (column == 1) {
            column = column + 1;
        } else if (column == 15) {
            column = column - 1;
        }
        // row MUST NOT BE 1 / 20
        if (row == 1) {
            row = row + 1;
        } else if (row == 20) {
            row = row - 1;
        }
        this.updateRobotAxis(column, row, robotDir);
        // update the current coords with converted coordinates if out of range
        currCoord[0] = column;
        currCoord[1] = row;
        // convert to coordinate
        row = this.rowConversion(row);
        // change color of robot current coordinate
        cellInitialization("robot",column - 1, row - 1);
        Util.showLog(TAG, "Exiting setCurCoord");
    }

    /**
     * setter to set the direction of the robot
     * @param dirRobot direction of the robot
     */
    public void setRobotDirection(String dirRobot) {
        this.sharedPreferences();
        robotDir = dirRobot;
        editor.putString("direction", dirRobot);
        editor.commit();
        this.invalidate();
        ;
    }

    /**
     * getter to retrieve the direction of the robot currently
     * @return a string for direction
     */
    public String getRobotDirection() {
        return robotDir;
    }

    /**
     * getter to retrieve the current coordinates
     * @return
     */
    public int[] getCurCoord() {
        return currCoord;
    }

    /**
     * setter to set the way point cordinates
     * @param column
     * @param row
     * @throws JSONException
     */
    private void setWaypointCoord(int column, int row) throws JSONException {
        waypointCoord[0] = column;
        waypointCoord[1] = row;
        row = this.rowConversion(row);
        cells[column][row].setType("waypoint");
        util.printMessage(ctx, "W", waypointCoord[0], waypointCoord[1]);
    }

    /**
     * getter to retrieve the waypoint coordinates
     * @return
     */
    public int[] getWaypointCoord() {
        return waypointCoord;
    }

    /**
     * setter to set the coordinate of the obstacle
     * @param column
     * @param row
     */
    private void setObstacleCoord(int column, int row) {
        int[] obstacleCoord = new int[]{column, row};
        GridMap.obsCoord.add(obstacleCoord);
        row = this.rowConversion(row);
        cells[column][row].setType("obstacle");// change the color of obstacle coordinate
    }

    /**
     * getter to get the coordinate of the locate  obstacles
     * @return
     */
    private ArrayList<int[]> getObstacleCoord() {
        return obsCoord;
    }

    /**
     * setter to set the old values of robot coordinate values
     * @param oldCol old value of column
     * @param oldRow old value of row
     */
    private void setOldRobotCoord(int oldCol, int oldRow) {
        oldCoord[0] = oldCol;
        oldCoord[1] = oldRow;
        oldRow = this.rowConversion(oldRow);
        cellInitialization("explored",oldCol - 1, oldRow - 1);
    }

    /**
     * retrieve the old values of robot coordinate avlues
     * @return an integer array
     */
    private int[] getOldRobotCoord() {
        return oldCoord;
    }

    /**
     * setter to set the size of the cells
     * @param cellSize
     */
    private void setCellSize(float cellSize) {
        GridMap.cellSize = cellSize;
    }

    /**
     * getter to retrieve size of the cells
     * @return
     */
    private float getCellSize() {
        return cellSize;
    }

    /**
     * set the received json obj
     * @param rcvdJsnObj
     */
    public void setRcveJsonObject(JSONObject rcvdJsnObj) {
        GridMap.rcveJsonObject = rcvdJsnObj;
        backupMapInformation = rcvdJsnObj;
    }

    /**
     * get the received json obg
     * @return
     */
    public JSONObject getRcveJsonObject() {
        return rcveJsonObject;
    }

    /**
     * set the image coordinate values with column, row and image tpye
     * @param c column
     * @param r row
     * @param imggType type of te image
     */
    private void setImageCoordinate(int c, int r, String imggType) {
        c += 1;
        r += 1;
        String[] coord_img = new String[3];
        coord_img[0] = String.valueOf(c);
        coord_img[1] = String.valueOf(r);
        coord_img[2] = imggType;

        if (cells[c][20 - r].type.equals("obstacle") || cells[c][20 - r].type.equals("image")) {
            try {
                this.getImageCoordinates().add(coord_img);
                this.sharedPreferences();
                String message = "(" + (c - 1) + ", " + (r - 1) + ", " + Integer.parseInt(coord_img[2], 16) + ")";
                editor.putString("image", sharedPrefs.getString("image", "") + "\n " + message);
                editor.commit();
                r = rowConversion(r);
                cells[c][r].setType("image");
            } catch (Exception e) {
            }
        }
    }

    /**
     * retrieve the image coordinates in a array list of strings
     * @return array list of strings of coordianates
     */
    private ArrayList<String[]> getImageCoordinates() {
        return imgCoord;
    }

    /**
     * retrieve an shared preferences of the activity
     */
    private void sharedPreferences() {
        sharedPrefs = this.getContext().getSharedPreferences("RobotControlActivity", Context.MODE_PRIVATE);
        editor = sharedPrefs.edit();
    }
}
