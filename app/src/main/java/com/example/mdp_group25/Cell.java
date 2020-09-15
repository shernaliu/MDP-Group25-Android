package com.example.mdp_group25;

import android.graphics.Color;
import android.graphics.Paint;

// cell class
public class Cell {
    private static String TAG = "CELL";
    float startX, startY, endX, endY;
    Paint paint;
    String type;

    private Paint obstacleColor = new Paint();      // black = obstacles position
    private Paint robotColor = new Paint();         // cyan = robot position
    private Paint endColor = new Paint();           // red = end position
    private Paint startColor = new Paint();         // green = start position
    private Paint waypointColor = new Paint();      // yellow = waypoint position
    private Paint unexploredColor = new Paint();    // gray = unexplored position
    private Paint exploredColor = new Paint();      // white = explored position
    private Paint fastestPathColor = new Paint();   // magenta = fastest path position
    private Paint imageColor = new Paint();         // black= image color

    public Cell(float startX, float startY, float endX, float endY, Paint paint, String type) {
        this.startX = startX;
        this.startY = startY;
        this.endX = endX;
        this.endY = endY;
        this.paint = paint;
        this.type = type;

        obstacleColor.setColor(Color.BLACK);                // black = obstacles position
        robotColor.setColor(Color.GREEN);                   // black = obstacles position
        endColor.setColor(Color.RED);                       // red = end position
        startColor.setColor(Color.CYAN);                    // green = start position
        waypointColor.setColor(Color.YELLOW);               // yellow = waypoint position
        unexploredColor.setColor(Color.GRAY);               // gray = unexplored position
        exploredColor.setColor(Color.WHITE);                // white = explored position
        fastestPathColor.setColor(Color.MAGENTA);           // magenta = fastest path position
        imageColor.setColor(Color.BLACK);                   // black = image color
    }

    public void setType(String type) {
        this.type = type;
        switch (type) {
            case "obstacle":
                this.paint = obstacleColor;
                break;
            case "robot":
                this.paint = robotColor;
                break;
            case "end":
                this.paint = endColor;
                break;
            case "start":
                this.paint = startColor;
                break;
            case "waypoint":
                this.paint = waypointColor;
                break;
            case "unexplored":
                this.paint = unexploredColor;
                break;
            case "explored":
                this.paint = exploredColor;
                break;
            case "fastestPath":
                this.paint = fastestPathColor;
                break;
            case "image":
                this.paint = imageColor;
                break;
            default:
                Util.showLog(TAG, "setTtype default: " + type);
                break;
        }
    }

    public float getStartX() {
        return startX;
    }

    public void setStartX(float startX) {
        this.startX = startX;
    }

    public float getStartY() {
        return startY;
    }

    public void setStartY(float startY) {
        this.startY = startY;
    }

    public float getEndX() {
        return endX;
    }

    public void setEndX(float endX) {
        this.endX = endX;
    }

    public float getEndY() {
        return endY;
    }

    public void setEndY(float endY) {
        this.endY = endY;
    }
}

