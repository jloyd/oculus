package oculus;

import java.util.Properties;

/** place extensions to settings here */
public enum ManualSettings {
	
	emailaddress, emailpassword, developer, debugenabled, commandport, stopdelay, vself, arduinoculus, oculed;

	/** get basic settings */
	public static Properties createDeaults(){
		Properties config = new Properties();
		config.setProperty(developer.toString(), "false");
		config.setProperty(debugenabled.toString(), "false");
		config.setProperty(stopdelay.toString(), "500");
		config.setProperty(vself.toString(), "320_240_8_85");
		config.setProperty(arduinoculus.toString(), "discovery");
		config.setProperty(oculed.toString(), "discovery");
		config.setProperty(emailaddress.toString(), "null");
		config.setProperty(emailpassword.toString(), "null");
		config.setProperty(commandport.toString(), "null");
		return config;
	}
	
	public static String getDefault(ManualSettings setting){
		Properties defaults = createDeaults();
		return defaults.getProperty(setting.toString());
	}
	
	/*
	public static boolean isDefault(ManualSettings setting){
		Settings.
		if() return true;
	}
	*/
	
	@Override
	public String toString() {
		return super.toString();
	}
}
