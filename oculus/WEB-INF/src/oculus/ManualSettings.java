package oculus;

import java.util.Properties;

import oculus.commport.Discovery;

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
		config.setProperty(arduinoculus.name(), Discovery.params.discovery.name());
		config.setProperty(oculed.name(), Discovery.params.discovery.name());
		config.setProperty(emailaddress.name(), State.values.disabled.name());
		config.setProperty(emailpassword.name(), State.values.disabled.name());
		config.setProperty(commandport.name(), State.values.disabled.name());
		return config;
	}
	
	public static String getDefault(ManualSettings setting){
		Properties defaults = createDeaults();
		return defaults.getProperty(setting.name());
	}
	
	public static boolean isDefault(ManualSettings manual){
		Settings settings = Settings.getReference();
		if(settings.readSetting(manual).equals(getDefault(manual))) return true;
		
		return false;
	}
}
