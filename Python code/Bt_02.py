from bluetooth import *
import sys
from subprocess import call
import time

if sys.version < '3':
    input = raw_input

print("no device specified. Searching all nearby bluetooth devices for")

# search for the SL4A service
uuid = 'fa87c0d0-afac-11de-8a39-0800200c9a66' 
addr = '58:2A:F7:F0:BF:1D'
#'60:45:CB:0E:3D:C4' 
#'58:2A:F7:F0:BF:1D'
service_matches = find_service( uuid = uuid, address = addr )

if len(service_matches) == 0:
    print("couldn't find a bluetooth service =(")
    sys.exit(0)

first_match = service_matches[0]
port = first_match["port"]
name = first_match["name"]
host = first_match["host"]

print("connecting to \"%s\" on %s" % (name, host))

# Create the client socket
sock=BluetoothSocket( RFCOMM )
sock.connect((host, port))
print("connected.")

# Waypoint mission
Alt = 3.0;
Spd = 1.0;

x1 = 5.146388
y1 = 100.497892
x2 = 5.146267
y2 = 100.497888
x3 = 5.146265
y3 = 100.498067
x4 = 5.146412
y4 = 100.498064

try:
    sock.send("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s" % (Alt, Spd, x1, y1, x2, y2, x3, y3, x4, y4))
except IOError:
    pass

time.sleep(1)
print("disconnected")
sock.close()
print("all done")

