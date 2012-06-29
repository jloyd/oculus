package oculus;

import java.util.Arrays;
import java.util.List;

/**
 * JUnit tests will validate the sub-sets for admin and player commands  
 *
 *
 */
public enum PlayerCommands {

	// all valid commands
	publish, floodlight, move, nudge, slide, dockgrab, framegrab, battstats, docklineposupdate, autodock,  autodockcalibrate, 
	speech, getdrivingsettings, drivingsettingsupdate, gettiltsettings, cameracommand, tiltsettingsupdate, 
	tilttest, speedset, dock, relaunchgrabber, clicksteer, chat, statuscheck, systemcall, streamsettingsset, 
	streamsettingscustom, motionenabletoggle, playerexit, playerbroadcast, password_update, 
	new_user_add, user_list, delete_user, extrauser_password_update, username_update, 
	disconnectotherconnections, showlog, monitor, assumecontrol, softwareupdate,
	arduinoecho, arduinoreset, setsystemvolume, beapassenger, muterovmiconmovetoggle, spotlightsetbrightness, 
    writesetting, holdservo, opennisensor, videosoundmode, pushtotalktoggle, restart;

	// sub-set that are restricted to "user0"
	public enum AdminCommands {
		new_user_add, user_list, delete_user, extrauser_password_update, restart, disconnectotherconnections, 
		showlog, softwareupdate, relaunchgrabber, systemcall
	}
	
	// sub-set that are require parameters 
	public enum RequiresArguments {
	
		publish("camera", "camadnmic", "mic", "stop"), 
		floodlight("on", "off"), 
		move("left", "right", "forward", "backward", "stop"),
		nudge("left", "right", "forward", "backward"),
		slide("left", "right"), 
		dockgrab,
		framegrab,
		battstats,
		docklineposupdate("{INT}"),
		autodock("cancel", "go", "dockgrabbed", "dockgrabbed {STRING}", "calibrate", "getdocktarget"),
		autodockcalibrate("{INT} {INT}"),
		speech("{STRING}"),
		getdrivingsettings, 
		drivingsettingsupdate("[0-255] [0-255] {INT} {INT} {DOUBLE} {INT}"),
		gettiltsettings,
		cameracommand("stop", "up", "down", "horiz", "downabit", "upabit"),
		tiltsettingsupdate("[0-255] [0-255] [0-255] {INT} {INT}"),
		tilttest("[0-255]"),
		speedset("slow", "med", "fast"), 
		dock("dock", "undock"), 
		relaunchgrabber, 
		clicksteer("{INT} {INT}"), 
		chat("{STRING}"), 
		statuscheck("", "battstats"),
		systemcall("{STRING}"), 
		streamsettingsset("low","med","high","full","custom"), 
		streamsettingscustom("{INT}_{INT}_{INT}_[0-100]"), 
		motionenabletoggle,
		playerexit,
		playerbroadcast("camera", "camadnmic", "mic", "stop"), 
		password_update("{STRING}"), 
		new_user_add("{STRING} {STRING}"), 
		user_list, 
		delete_user("{STRING}"), 
		extrauser_password_update("{STRING} {STRING}"), 
		username_update("{STRING} {STRING}"), 
		disconnectotherconnections, 
		monitor("on", "off"), 
		assumecontrol("{STRING}"), 
		softwareupdate("check", "download","versiononly"),
		arduinoecho("{BOOLEAN}"),
		arduinoreset,
		setsystemvolume("[0-100]"), 
		beapassenger("{STRING}"), 
		muterovmiconmovetoggle,
		spotlightsetbrightness("0","10","20","30","40","50","60","70","80","90","100"), 
		writesetting("{STRING} {STRING}"), 
		holdservo ("{BOOLEAN}"), 
		opennisensor("on", "off"), 
		videosoundmode("low", "high"), 
		pushtotalktoggle("{BOOLEAN}"),
		restart;
			
		private final List<String> values;

		RequiresArguments(String ...values) {
			this.values = Arrays.asList(values);
		}

		public List<String> getValues() {
			return values;
		}

		/*
		public static boolean RequiresArguments(final String cmd, final String target){
			RequiresArguments arg = null;
			try{
				arg = RequiresArguments.valueOf(cmd);
			} catch (Exception e) { return false; }
			 
			return arg.getValues().contains(target);
		}
		 */
		
		/*
		public static RequiresArguments find(String name) {
		    for (RequiresArguments lang : RequiresArguments.values()) {
		        if (lang.getValues().contains(name)) {
		            return lang;
		        }
		    }
		    return null;
		}*/
		
	}
	
	public enum helpText{ 
		
		publish("Robot video/audio control"), 
		floodlight("Controls wide angle light"), 
		move("Continuous movement"),
		nudge("Move for amount of milliseconds specified by 'nudgedelay' setting, then stop"),
		slide("Rearward triangular movement macro, that positions robot slightly to the left or right of starting spot"), 
		dockgrab("Find dock target within robots camera view, returns target metrics. Robot camera must be running"),
		framegrab("Capture jpg image of current view and write to url 'http://{host}:{http-port}/oculus/frameGrabHTTP"),
		battstats("Returns battery charging state and charge remaining"),
		docklineposupdate("Set manual dock line position within camera FOV in +/- pixels offset from center"),
		autodock("Autodocking system control. Camera must be running"),
		autodockcalibrate("Start autodock calibration, by sending xy pixel position of a white area within the dock target"),
		speech("Voice synthesizer"),
		getdrivingsettings("Returns drive motor calibration settings"), 
		drivingsettingsupdate("Set drive motor calibration settings: slow speed, medium speed, nudge delay, maxclicknudgedelay (time in ms for robot to turn 1/2 of screen width), momentum factor, steering compensation"),
		gettiltsettings("Returns camera calibration settings"),
		cameracommand("Camera periscope tilt servo movement"),
		tiltsettingsupdate("Set camera calibration settins: horiz tilt, max tilt, min tilt, maxclickcam (time in ms for tilt to move 1/2 screen height), video scale %"),
		tilttest("Move persicope tilt servo to specified position"),
		speedset("Set drive motor speed"), 
		dock("Start manual dock routine"), 
		relaunchgrabber("Launch server.html browser page on robot"), 
		clicksteer("Camera tilt and drive motor movement macro to re-position center of screen by x,y pixels"), 
		chat("Send text chat to all other connected users"), 
		statuscheck("request current statuses for various settings/modes. Call with 'battstats' to also get battery status"),
		systemcall("Execute OS system command"), 
		streamsettingsset("Set robot camera resolution and quality"), 
		streamsettingscustom("Set values for 'custom' stream: resolutionX_resolutionY_fps_quality"), 
		motionenabletoggle("Enable/disable robot drive motors"),
		playerexit("End rtmp connection with robot"),
		playerbroadcast("Client video/audio control (to be broadcast thru robot screen/speakers)"), 		
		password_update("Set new password for currently connected user"), 
		new_user_add("Add new user with 'username' 'password'"), 
		user_list("Returns list of user accounts"), 
		delete_user("Delete user 'username'"), 
		extrauser_password_update("Set new password for user with 'username' 'password'"), 
		username_update("Change non-connected username with 'oldname' 'newname'"), 
		disconnectotherconnections("Close rtmp connection with all user connections other than current connection"), 
		monitor("Robot monitor sleep/wake control"), 
		assumecontrol("Assume control from current drive, specify new driver 'username'"), 
		softwareupdate("Robot server software update control"),
		arduinoecho("Set ArduinOculus microcontroller to echo all commands"),
		arduinoreset("Reset ArduinOculus microcontroller"),
		setsystemvolume("Set robot operating system audio volume"), 
		beapassenger("Be passenger of current driver, specify passenger 'username'"), 
		muterovmiconmovetoggle("Set/unset mute-rov-mic-on-move' setting "),
		spotlightsetbrightness("Set main spotlight brightness. 0=off"), 
		writesetting("Write setting to oculus_settings.txt"), 
		holdservo ("Set/unset use of power break for persicope servo"), 
		opennisensor("Kinect/Xtion Primesense sensor control"), 
		videosoundmode("Robot video compression codec"), 
		pushtotalktoggle("When broadcasting client mic through robot speakers, always on or mute until keypress"),
		restart("Restart server application on robot");

        private final String message;

        helpText(String msg) {
                this.message = msg;
        }
        
        public String help(){
                return message;
        }
	}
	
	/** 
	public static boolean booleanArgument(final String str) {
		RequiresArguments command = null;
		try {
			command = RequiresArguments.valueOf(str);
		} catch (Exception e) {return false;}
		
		if(command.getValues().contains("true")) return true;
		if(command.getValues().contains("false")) return true;
		
		return false; 
	}*/
	
	/** */
	public static boolean requiresArgument(final String str) {
		RequiresArguments command = null;
		try {
			command = RequiresArguments.valueOf(str);
		} catch (Exception e) {}
		
		if(command==null) return false;
			
		return true; 
	}
	
	/** @return true if given command is in the sub-set */
	public static boolean requiresAdmin(final String str) {
		AdminCommands command = null;
		try {
			command = AdminCommands.valueOf(str);
		} catch (Exception e) {}
		
		if(command==null) return false;
			
		return true; 
	}
	
	/** @return true if given command is in the sub-set */
	public static boolean requiresAdmin(final PlayerCommands cmd) {
		AdminCommands command = null;
		try {
			command = AdminCommands.valueOf(cmd.toString());
		} catch (Exception e) {}
		
		if(command==null) return false;
			
		return true; 
	}
}
