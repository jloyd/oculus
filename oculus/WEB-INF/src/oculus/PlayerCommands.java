package oculus;

public enum PlayerCommands {

	// all valid commands
	publish, floodlight, move, nudge, slide, dockgrab, framegrab, battstats, docklineposupdate, autodock, autodockcalibrate, 
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
	
	// sub-set that are require a single long string of commands for cmmd mgr 
	public enum MultiCommand {
		chat, new_user_add;
	}

	/**
	 * @return true if given command is in the sub-set
	 */
	public static boolean requiresAdmin(PlayerCommands cmd) {
		for (AdminCommands admin : AdminCommands.values()) {
			if (admin.equals(cmd))
				return true;
		}

		return false;
	}
	
	/**
	 * @return true if given command is in the sub-set
	 */
	public static boolean isMultiCommand(PlayerCommands cmd) {
		if(cmd == null) return false;
		
		Util.log("is multi: " + cmd.toString());
		
		for (MultiCommand admin : MultiCommand.values()) {
			if (admin.equals(cmd))
				return true;
		}

		return false;
	}
	
	/**
	 * @return true if given command is in the sub-set
	 */
	public static boolean isMultiCommand(String str) {
		return isMultiCommand(PlayerCommands.valueOf(str));
	}


	/**
	 * @return true if given command is in the sub-set
	 */
	public boolean requiresAdmin() {
		for (AdminCommands admin : AdminCommands.values()) {
			if (admin.equals(this))
				return true;
		}

		return false;
	}

	public static String match(String str) {
		for (AdminCommands admin : AdminCommands.values()) {
			if (admin.toString().startsWith(str))
				return admin.toString();
		}

		return null;
	}


	
	@Override
	public String toString() {
		return super.toString();
	}
}
