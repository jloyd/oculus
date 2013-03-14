# finddock.py
# Oculus Telnet Interface script
#
# run using: python finddock.py host username password

import socket, re, time, sys

host = sys.argv[1] # ip address or domain/url
username = sys.argv[2] # username
password = sys.argv[3] # password or hashed/encrypted password
port = 4444

#FUNCTIONS

def sendString(s):
        oculusock.sendall(s+"\r\n")
        print("> "+s)    

def waitForReplySearch(p):
    while True:
        servermsg = (oculusfileIO.readline()).strip()
        print(servermsg)
        if re.search(p, servermsg): 
            break
    return servermsg

def rotateAndCheckForDock():
        # rotate a bit
        sendString("move right")
        time.sleep(0.6)
        sendString("move stop")
        time.sleep(0.5) # allow to come to full stop
        
        # if too dark, turn floodlight on
        sendString("getlightlevel")
        s = waitForReplySearch("getlightlevel:")
        lightlevel = int(re.findall("\d+",s)[0])
        if lightlevel < 25:
                sendString("floodlight on")

        # check if dock in view
        sendString("dockgrab")
        s = waitForReplySearch("<state> dockxsize")
        dockwidth = int(re.findall("\d+",s)[0])
        if dockwidth > 10 and dockwidth < 280:
                return True
        else:
                return False

#MAIN

#connect
oculusock = socket.socket(socket.AF_INET, socket.SOCK_STREAM) 
oculusock.connect((host, port))
oculusfileIO = oculusock.makefile() # convert to file IO to simplify things

#login 
waitForReplySearch("^<telnet> LOGIN")
sendString(username+":"+password)

#tell any connections what's about to happen
sendString("messageclients launching finddock.py")

#turn camera on if it isn't already
sendString("state stream")
s = waitForReplySearch("<state> stream")
if not re.search("(camera)|(camandmic)$", s):
        sendString("publish camera")
        time.sleep(5)

sendString("cameracommand horiz")  #set camera to horizontal, 
sendString("state motionenabled true")  # make sure motion is enabled

#keep rotating and looking for dock, start autodock if found
dockfound = False
for i in range(20):
        sendString("messageclients attempt #: "+str(i))
        if rotateAndCheckForDock():
                dockfound = True
                break
if dockfound:
	sendString("autodock go")
	waitForReplySearch("<state> dockstatus docked")
else:
	sendString("messageclients finddock.py failed")
        
oculusfileIO.close()
oculusock.close()
