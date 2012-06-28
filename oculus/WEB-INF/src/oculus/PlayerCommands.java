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
	
		publish("camera", "camadnmic", "stop"), 
		floodlight("on", "off"), 
		move("left", "right", "forward", "backward"),
		nudge("left", "right", "forward", "backward"),
		slide("left", "right", "forward", "backward"), 
		getdrivingsettings, 
		drivingsettingsupdate, 
		cameracommand, 
		speedset("slow", "med", "fast"), 
		dock("dock", "undock"), 
		relaunchgrabber, 
		clicksteer, 
		chat, 
		systemcall, 
		streamsettingsset, 
		streamsettingscustom, 
		playerbroadcast, 
		password_update, 
		new_user_add, 
		user_list, 
		delete_user, 
		extrauser_password_update, 
		username_update, 
		disconnectotherconnections, 
		monitor("on", "off"), 
		assumecontrol("*"), 
		softwareupdate("version", "update"),
		arduinoecho("true", "false"),
		setsystemvolume, 
		beapassenger("*"), 
		spotlightsetbrightness, writesetting, holdservo ("true", "false"), 
		opennisensor, 
		videosoundmode("true", "false"), 
		pushtotalktoggle("true", "false") ;
	
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
