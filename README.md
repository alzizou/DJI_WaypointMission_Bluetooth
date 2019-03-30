# DJI_WaypointMission_Bluetooth
This is an Android app + Python code for receiving waypoints commands from an Ubuntu-powered computer via Bluetooth.

Note that, for the first time that you run the app, your device should be connected to internet, 
in order to have the app be registered. For the any number of further run, there is no need for accessing to internet.
Use the button “Commanded position” to enter the page for receiving the waypoints via Bluetooth.

At the same time, you should run the below Python code in “Terminal” of the Ubuntu-powered computer. 
The computer should have Bluetooth port.

In the current Python code, the desired 4 waypoints should be set manually. 
Also, the IP of the receiving android device should be set as input, manually. 
In “terminal” of an Ubuntu-powered computer you should use “hcitool scan” command to find the available Bluetooth devices, 
and then use the corresponding ip address in the code as the value for “addr” variable.

I have tested the code and the app, for Phantom3 and it works well.

Later, I should work on the Python code in order to make the process of generating the desired waypoints automatically, 
according to the received streaming from those guarding drones. Then, the command to each of the connected android devices, 
will be sent via Bluetooth. 

Note that, Jetson TX1 has Bluetooth port. 

See the video for the test at https://youtu.be/S4uprQfYsOA .
