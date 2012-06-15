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
	/*
	public enum AdminCommands {
		new_user_add, user_list, delete_user, extrauser_password_update, restart, disconnectotherconnections, 
		showlog, softwareupdate, relaunchgrabber, systemcall
	}
	*/
	
	// sub-set that are require parameters 
	/*
	public enum RequiresArguments {
		publish, floodlight, move, nudge, slide, getdrivingsettings, drivingsettingsupdate, cameracommand, 
		speedset, dock, relaunchgrabber, clicksteer, chat, systemcall, streamsettingsset, 
		streamsettingscustom, playerbroadcast, password_update, 
		new_user_add, user_list, delete_user, extrauser_password_update, username_update, 
		disconnectotherconnections, monitor, assumecontrol, softwareupdate, uinoecho, 
		setsystemvolume, beapassenger, spotlightsetbrightness, writesetting, holdservo, 
		opennisensor, videosoundmode, pushtotalktoggle ;
	}
	*/
	
	/*
	public static boolean requiresArgument(String str) {
		RequiresArguments command = PlayerCommands.RequiresArguments.valueOf(str);
		if(command!=null) return true; 
		return false;
	}
	*/
	
	/** @return true if given command is in the sub-set 
	public static boolean requiresAdmin(PlayerCommands cmd) {
		for (AdminCommands admin : AdminCommands.values()) {
			if (admin.toString().equals(cmd.toString()))
				return true;
		}

		return false;
	}*/
	
	/** @return true if given command is in the sub-set 
	public boolean requiresAdmin() {
		
		for (AdminCommands admin : AdminCommands.values()) {
			if (admin.equals(this))
				return true;
		}

		return false;
	}*/
	/*public boolean requiresArgument() {
		// TODO Auto-generated method stub
		return false;
	}

	public static String match(String str) {
		for (AdminCommands admin : AdminCommands.values()) {
			if (admin.toString().startsWith(str))
				return admin.toString();
		}

		return null;
	}
	*/
	
	/*
	@Override
	public String toString() {
		return super.toString();
	}*/
	
}
