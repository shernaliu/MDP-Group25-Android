# MDP-Group25-Android

The Android Remote Controller Module (ACRM) is an android application that serves as an interface for the user to control and monitor robot in the maze arena. The ARCM allows the user to control the robot by sending string commands to the Raspberry Pi, which will be executed by the autonomous robot. Aside from providing control over robot, the ACRM also provides a 2D grid map display of the arena, which is meant to help the user to visualize the robot’s movement as well as view of the maze arena, for example, which area of the maze is set as obstacles, unexplored and explored, as the robot is exploring the maze while sending back status updates to the ACRM.

The Android Remote Controller Module is developed for the device Samsung Galaxy Tab A.

The ARCM is equipped with a set of functions, namely:

- Establishing Bluetooth Connection with the robot
- Control robot movements (move forward, reverse, turn right, turn left)
- Start Robot Exploration and Robot fastest path
- Display 2D view of the Grid map to represent the maze environment and current position of the robot
- Display image IDs of signs recognized by the robot in the 2D Grid map
- Set Way Point Coordinates, Start Point Coordinates and obstacles on the grid map.
- Allows the user to get updates of the 2D Grid Map information either manually or automatically from the robot as it explores the maze arena

The tools used by the developers to develop the ARCM are as follows:

Android Studio Development IDE
AMD Tool (for testing Bluetooth connection as well as sending/receiving message)
Android Emulator
Samsung Galaxy Tab A (Android Tablet)

```Note: This is still  Work In Progress.```

## Screenshots

![MDP-1](https://res.cloudinary.com/shernaliu/image/upload/v1603385985/github-never-delete/mdp-screenshots/Slide1.png)

![MDP-2](https://res.cloudinary.com/shernaliu/image/upload/v1603385985/github-never-delete/mdp-screenshots/Slide2.png)

| Buttons       | Functionalities           | 
| ------------- |:-------------------------:|
| Explore       | Start the exploration of the robot |
| Fastest       | Start the robot fastest path      |
| Manual/Auto   | Set update of the Grid Map to manual update or automatic update      |
| Set Waypoint  | Allows user to set waypoint coordinates by clicking on the Grid Map |
| Set Start point       | Sets the start point on the Grid Map and marks 9 grid cells as ‘robot’ by clicking on the Grid Map |
| Reset       | Reset the Grid Map and marks the grid as ‘unexplored’ by clicking on the Grid Map |
| Set Obstacles       | Sets obstacles on the Grid Map and marks the grid as “obstacles” by clicking on the Grid map |
| Turn Right       | Rotate the direction of the robot in the clockwise direction |
| Turn Left       |Rotate the direction of the robot in the anticlockwise direction |
| Move Forward       | Move the robot forward by 1 grid |
| Reverse       | Move the robot backward by 1 grid |
| Map information       | Allow the users to request for map information from the robot to display on the 2D Grid Map available |

## Installing MDP-Group25-Android

```
# clone this project
git clone https://github.com/shernaliu/MDP-Group25-Android.git
```
Run the project in Android Studio.
