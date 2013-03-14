# low_battery_rss.rb
# Xaxxon Oculus Telnet Interface script - ruby
#
# Will check for draining battery - if no drivers logged in, will post to rss
# Oculus rss feed viewable at url http://[domain or ip]:[port]/oculus/rss.xml
# Use rss feed as trigger in service such as ifttt.com


require 'socket'

host = "127.0.0.1" # ip address or domain/url 
username = "admin" # username 
password = "password" # plain text password or hashed/encrypted password from oculus_settings.txt
port = 4444 # port number
$oculussock
posted = false

#FUNCTIONS

#send text over socket
def sendString(str)
	$oculussock.puts str
	puts "> "+str    
end

#receive text over socket, waiting for pattern
def waitForReplySearch(pattern)
    while true
        servermsg = $oculussock.gets.strip
        puts servermsg
        if servermsg =~ pattern then break end
	end
    return servermsg
end

#MAIN

# check battery status periodically, send email if draining
while true
	#login 
	$oculussock = TCPSocket.new host, port
	waitForReplySearch(/^<telnet> LOGIN/)
	sendString(username+":"+password)
	
	#announce
	sendString("messageclients low_battery_rss.rb "+Time.now().to_s)

	# get battery status
	sendString("battstats")
	batteryisdraining = waitForReplySearch(/^<state> batterystatus/) =~ /draining$/
	
	# only post once per battery draining instance
	if not batteryisdraining then posted = false end

	# get any connected rtmp users
	sendString("who")
	rtmpusers = waitForReplySearch(/^<multiline> <messageclient> active RTMP users:/)[/\d+$/].to_i
	
	# post to rss if battery draining, no connected users, and haven't already posted
	if batteryisdraining and rtmpusers == 0 and not posted
		# add time to title to ensure title differs from previous
		sendString("rssadd [low battery at "+Time.now.strftime("%Y-%m-%d %H:%M:%S")+"] alert alert")
		sendString("messageclient low battery alert rss created")
		posted = true
	end
	
	sendString("quit")   #logout
	$oculussock.close         

	sleep 300 # five minutes
end

