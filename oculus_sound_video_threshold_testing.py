# oculus_sound_video_threshold_testing.py
#
# change 'user variables' below to appropriate values

import socket, re, time

# USER VARIABLES - change to appropriate values
host = "127.0.0.1" # ip address or domain/url 
username = "admin" # username 
password = "tEFuqZimWpXD70rHiAA7lU10JHc=" # plain text password or hashed/encrypted password from oculus_settings.txt
port = 4444 # port number
soundthreshold = 0  # (sound sensitivity 0-100, 0 to disable)
vidthreshold = 8  # (video motion sensitivity 0-100, 0 to disable)


#FUNCTION DEFINITIONS

# send single line commands to Oculus server
def sendString(s):
	oculusock.sendall(s+"\r\n")
	print("> "+s)   

# search output from Oculus server, WAIT until match to regular expresson pattern
def waitForReplySearch(pattern): 
	while True:
		servermsg = (oculusfileIO.readline()).strip()
		print(servermsg)
		if re.search(pattern, servermsg, re.IGNORECASE): 
			break
	return servermsg # return line containing pattern

	
# MAIN

# connect
oculusock = socket.socket(socket.AF_INET, socket.SOCK_STREAM) 
oculusock.connect((host, port))
oculusfileIO = oculusock.makefile()

# login 	
waitForReplySearch("^<telnet> LOGIN")
sendString(username+":"+password)

while True:
	sendString("setstreamactivitythreshold "+str(vidthreshold)+" "+str(soundthreshold))
	s = waitForReplySearch("<messageclient> streamactivity:")
	activity = s.split()[2]
	sendString("publish stop")
	time.sleep(2)
	sendString("speech "+activity+" detected")
	time.sleep(3)
