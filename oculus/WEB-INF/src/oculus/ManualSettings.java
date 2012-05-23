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
		config.setProperty(arduinoculus.toString(), Discovery.params.discovery.toString());
		config.setProperty(oculed.toString(), Discovery.params.discovery.toString());
		config.setProperty(emailaddress.toString(), State.disabled.toString());
		config.setProperty(emailpassword.toString(), State.disabled.toString());
		config.setProperty(commandport.toString(), State.disabled.toString());
		return config;
	}
	
	public static String getDefault(ManualSettings setting){
		Properties defaults = createDeaults();
		return defaults.getProperty(setting.toString());
	}
	
	/**/
	
	public static boolean isDefault(ManualSettings manual){
		Settings settings = new Settings();
		if(settings.readSetting(manual).equals(getDefault(manual))) return true;
		
		return false;
	}
	
	@Override
	public String toString() {
		return super.toString();
	}
}
