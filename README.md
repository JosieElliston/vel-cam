# vel-cam

Vel Cam is a client-side Fabric mod for Minecraft 26.2. Press `C` to toggle a
camera that faces in the direction of the player's velocity. Mouse movement
continues to control the player's underlying look direction. When the player is
stationary, the camera uses the player's look direction as normal. Movement is
measured from the player's current and previous positions, including while riding.
Successive movement samples are interpolated at the render frame rate.

While the velocity camera is enabled in first person, a second crosshair marks
the player's underlying look direction whenever that direction is on screen.

The key can be rebound under Options > Controls > Key Binds > Miscellaneous.

## License

This project is available under the CC0 license.
