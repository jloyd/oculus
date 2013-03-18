# oculus_surveillance.py
# Xaxxon Oculus Telnet Interface script
#
# run this script with Oculus positioned in charging dock
# change 'user variables' below to appropriate values
#
# Basic functionality:
# -Logs into Oculus telnet interface using host, username, password and port 
# -Starts up microphone, waits
# -If loud noise heard: undocks, sends 'noise detected' email if emailonsound set to True
#   -Rotates a bit, starts up camera, turns on OcuLED light if necessary (if light connected)
#   -Watches for a few moments - if motion detected, sends email, and returns to charging dock
#   -Repeats through 3 more rotation positions
#   -Returns to charging dock
# -If undockinterval is set to >0, undock/rotate/check for motion routine will run periodically
# 
# Script will exit and restart after delay if driver connects, or server unavailable

import socket, re, time, sys, os


# USER VARIABLES - change to appropriate values
host = "127.0.0.1" # ip address or domain/url 
username = "admin" # username 
password = "tEFuqZimWpXD70rHiAA7lU10JHc=" # plain text password or hashed/encrypted password from oculus_settings.txt
port = 4444 # port number
emailonsound = False # send email on sound detected when docked (True/False)
emailto = "bob@example.com" # email-to address for notifications
motionthreshold = 22 # motion sensitivty (5 or higher)
soundthreshold = 30  # (sound sensitivity 0-100)
turnseconds = 1.5 # seconds of movement between each of 4 rotations when looking around
lightlevelminimum = 25 # (brightness 0-255) OcuLED light, if attached, will turn on below this theshold
undockinterval = 1200 # seconds between periodic undocking and looking around. 0 to ONLY undock on loud noise 


# global variables
python = sys.executable
oculusock = socket.socket(socket.AF_INET, socket.SOCK_STREAM) 
serverflashreloadinterval = 700 # flash plugin needs reloading often, unstable otherwise
restartdelay = 200


#FUNCTION DEFINITIONS

# send single line commands to Oculus server
def sendString(s):
	try:
		oculusock.sendall(s+"\r\n")
	except socket.error: 
		restart("socket send error, restarting in 30 sec",30)
	print("> "+s)   

# search through unread output from Oculus server, compare to regular expresson pattern
# if driver login detected, exit script and reconnect later
def replyBufferSearch(pattern): 
	result = "" # return empty string if search fails
	oculusock.setblocking(False) # don't pause and wait for any further input
	while True:
		try:
			servermsg = (oculusfileIO.readline()).strip()
			print(servermsg)
			if re.search(pattern, servermsg, re.IGNORECASE): 
				result = servermsg # return the line containing pattern
				break
			if re.search("^<state> driver", servermsg, re.IGNORECASE): # exit if driver login
				sendString("setstreamactivitythreshold 0 0")
				sendString("publish stop")
				sendString("move stop")
				sendString("exit")
				restart("driver login, restarting in "+str(restartdelay)+"sec",restartdelay)
		except socket.error: # assuming EOF reached, reading buffer complete
			break
	oculusock.setblocking(True)
	return result    

# search output from Oculus server, WAIT until match to regular expresson pattern
# if driver login detected, exit script and reconnect later
def waitForReplySearch(pattern): 
	while True:
		try:
			servermsg = (oculusfileIO.readline()).strip()
			print(servermsg)
			if re.search(pattern, servermsg, re.IGNORECASE): 
				break
			if re.search("^<state> driver", servermsg, re.IGNORECASE): # exit if driver login
				sendString("setstreamactivitythreshold 0 0")
				sendString("publish stop")
				sendString("move stop")
				sendString("exit")
				restart("driver login, restarting in "+str(restartdelay)+"sec",restartdelay)
		except socket.error: 
			restart("socket recv error, restarting in 30 sec",30)
	return servermsg # return the line containing pattern

# restart this script
def restart(message, seconds):
	print(message)
	time.sleep(seconds)
	oculusfileIO.close()
	oculusock.close()
	os.execl(python, python, * sys.argv)

# rotate a bit, floodlight on if necessary, return True if dock within view
def rotateAndCheckForDock():
	# rotate a bit
	sendString("move right")
	time.sleep(0.6)
	sendString("move stop")
	time.sleep(0.5) # allow to come to full stop
	
	# if too dark, turn floodlight on
	sendString("getlightlevel")
	s = waitForReplySearch("getlightlevel:")
	lightlevel = int(re.findall("\d+",s,re.IGNORECASE)[0])
	if lightlevel < lightlevelminimum:
			sendString("floodlight on")

	# check if dock in view
	sendString("dockgrab")
	s = waitForReplySearch("<state> dockxsize")
	dockwidth = int(re.findall("\d+",s,re.IGNORECASE)[0])
	if dockwidth > 10 and dockwidth < 280:
			return True
	else:
			return False
	
# start camera, rotate, turn light on if necessary, wait a few seconds to see if motion	
# return True if motion detected
def rotateAndCheckForMotion():
	result=""
		
	# rotate a bit
	sendString("move right")
	time.sleep(turnseconds)
	sendString("move stop")
	time.sleep(0.5) # allow to come to full stop
	
	# if too dark, turn lights on
	sendString("getlightlevel")
	s = waitForReplySearch("getlightlevel:")
	lightlevel = int(re.findall("\d+",s,re.IGNORECASE)[0])
	if lightlevel < lightlevelminimum:
			sendString("floodlight on")
			sendString("spotlight 100")
			time.sleep(1)

	# start motion detect
	sendString("motiondetectgo "+str(motionthreshold))
	sendString("speech watching")
	
	# wait 10 seconds, checking for any sound/motion 
	for i in range(12):
		motion = replyBufferSearch("^<state> motiondetected")
		if not motion == "": # motion detected
			result = motion.split()[2]
			break
		time.sleep(1)
	sendString("motiondetectcancel") # sends cancel in case no motion detected
		
	return result
	
	
# MAIN

# connect
try:
	oculusock.connect((host, port))
except socket.error:
	restart("unable to connect, restarting in 30 sec",30)
oculusfileIO = oculusock.makefile()

# login 	
waitForReplySearch("^<telnet> LOGIN")
sendString(username+":"+password)

# get number of connected rtmp users (ie., drivers), logout and restart if any
sendString("who")
s = waitForReplySearch("^<multiline> <messageclient> active RTMP users:")
rtmpusers = int(re.findall("\d+$",s,re.IGNORECASE)[0])
if rtmpusers > 0:
	sendString("exit")
	restart("driver connected, restarting in "+str(restartdelay)+"sec",restartdelay)

# check if docked, logout and restart if not
sendString("battstats")
s = waitForReplySearch("^<state> batterystatus")
if not s.split()[2] != "draining":
	sendString("exit")
	restart("not docked, restarting in "+str(restartdelay)+"sec",restartdelay)

# prepare framegrab URL
sendString("state externaladdress")
s = waitForReplySearch("^<messageclient> <state> externaladdress")
ip = s.split()[3]
sendString("state httpPort")
s = waitForReplySearch("^<messageclient> <state> httpPort")
httpport = s.split()[3]
fgurl = "http://"+ip+":"+httpport+"/oculus/framegrabs/"

# LISTEN
# listen while docked, undock and look around if noise
# stop & restart stream periodically to allow server.html/flash reload
# undock and look around periodically if undockinterval > 0
lastflashreload = time.time()
logintime = time.time()
sendString("setstreamactivitythreshold 0 "+str(soundthreshold)) # this turns mic on
time.sleep(4)
while True:
	now = time.time()
	if now - lastflashreload > serverflashreloadinterval:
		sendString("setstreamactivitythreshold 0 0") 
		time.sleep(1)
		sendString("reloadserverhtml") # force server.html page reload 
		time.sleep(10) # allow time for page reload
		sendString("setstreamactivitythreshold 0 "+str(soundthreshold)) # this turns mic on
		lastflashreload = time.time() 
	else:
		sound = replyBufferSearch("streamactivity: audio")
		if not sound == "": # sound detected
			sendString("speech sound detected")
			if emailonsound: # send email on sound-detect, if set
				t = time.strftime("%Y-%m-%d %H:%M:%S", time.localtime())
				sendString("email "+emailto+" [Oculus detected noise] alert alert, noise detected "+t)
			break
		if now - logintime > undockinterval and not undockinterval == 0: # periodic undock
			sendString("setstreamactivitythreshold 0 0")
			time.sleep(1)
			break
	time.sleep(1)

sendString("reloadserverhtml") # force server.html page reload now, so it doesn't do it unexpectedly
time.sleep(5) 

# UNDOCK AND LOOK AROUND	
# startup camera, undock, backup, rotate and check for motion 	
sendString("publish camera")
time.sleep(5) # let video brightness settle after camera startup
sendString("dock undock")
sendString("cameracommand horiz")  
waitForReplySearch("<status> motion stopped")
sendString("move backward") 
time.sleep(1.5)
sendString("move stop")
# sendString("cameracommand upabit")
waitForReplySearch("<status> motion stopped")
for i in range(4):
	motion = rotateAndCheckForMotion()
	if not motion == "":
		sendString("speech motion detected, sending alert")
		t = time.strftime("%Y-%m-%d %H:%M:%S", time.localtime())
		sendString("framegrabtofile")
		s = waitForReplySearch("^<messageclient> frame saved as:")
		ss = s.split()
		s = "email "+emailto+" [oculus motion detected] "
		s += "alert alert, motion detected at " + t + ", rotation position: "+str(i)
		s += ", motion level: "+motion
		s += ", image link: "+ fgurl + ss[len(ss)-1]
		sendString(s)
		break

# DOCK		
# turn spotlight off, in case it was on (autodock will automatically turn floodlight off)
sendString("spotlight 0")

#keep rotating and looking for dock, start autodock if found
sendString("cameracommand horiz")  #set camera to horizontal position
sendString("publish camera")
time.sleep(4)
dockfound = False
for i in range(20):
        # sendString("messageclients attempt #: "+str(i))
        if rotateAndCheckForDock():
                dockfound = True
                break
if dockfound:
	sendString("autodock go")
	waitForReplySearch("<state> dockstatus docked")
else:  
	sendString("speech find dock failed, help me")
	t = time.strftime("%Y-%m-%d %H:%M:%S", time.localtime())
	sendString("email "+emailto+" [oculus unable to dock] alert alert, unable to find dock " + t)
	sendString("publish stop")

#exit and restart
sendString("exit")
oculusfileIO.close()
oculusock.close()
restart("restarting script in 10s",10)

