package oculus;

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
	
	public enum booleanArguments {
		arduinoecho, holdservo, opennisensor, videosoundmode, pushtotalktoggle ;
	}
	
	//public enum onoffArgument {
	//	floodlight, monitor ;
	//}
	
	// sub-set that are require parameters 
	public enum RequiresArguments {
		publish, floodlight, move, nudge, slide, getdrivingsettings, drivingsettingsupdate, cameracommand, 
		speedset, dock, relaunchgrabber, clicksteer, chat, systemcall, streamsettingsset, 
		streamsettingscustom, playerbroadcast, password_update, 
		new_user_add, user_list, delete_user, extrauser_password_update, username_update, 
		disconnectotherconnections, monitor, assumecontrol, softwareupdate, 
		setsystemvolume, beapassenger, spotlightsetbrightness, writesetting, holdservo, 
		opennisensor, videosoundmode, pushtotalktoggle ;
	}
	
	/** */
	public static boolean booleanArgument(final String str) {
		booleanArguments command = null;
		try {
			command = booleanArguments.valueOf(str);
		} catch (Exception e) {}
		
		if(command==null) return false;
			
		return true; 
	}
	
	/** */
	public static boolean booleanArgument(final PlayerCommands str) {
		booleanArguments command = null;
		try {
			command = booleanArguments.valueOf(str.toString());
		} catch (Exception e) {}
		
		if(command==null) return false;
			
		return true; 
	}
	
	/** */
	public static boolean requiresArgument(final String str) {
		RequiresArguments command = null;
		try {
			command = RequiresArguments.valueOf(str);
		} catch (Exception e) {}
		
		if(command==null) return false;
			
		return true; 
	}
	
	/** */
	public static boolean requiresArgument(final PlayerCommands str) {
		RequiresArguments command = null;
		try {
			command = RequiresArguments.valueOf(str.toString());
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
