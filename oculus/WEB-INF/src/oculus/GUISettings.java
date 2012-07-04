package oculus;

import java.util.Properties;

public enum GUISettings {

	/** these settings must be available in basic configuration */
	skipsetup, speedslow, speedmed, steeringcomp, camservohoriz, camposmax, camposmin, nudgedelay, 
	docktarget, vidctroffset, vlow, vmed, vhigh, vfull, vcustom, vset, maxclicknudgedelay, 
	clicknudgedelaymomentumfactor, clicknudgemomentummult, maxclickcam, muteonrovmove, 
	videoscale, volume, holdservo, loginnotify, reboot, selfmicpushtotalk, pushtotalk; 
	
	
	/** get basic settings */
	public static Properties createDeaults() {
		Properties config = new Properties();
		config.setProperty(skipsetup.name() , "no");
		config.setProperty(speedslow.name() , "130");
		config.setProperty(speedmed.name() , "180");
		config.setProperty(steeringcomp.name() , "128");
		config.setProperty(camservohoriz.name() , "68");
		config.setProperty(camposmax.name() , "89");
		config.setProperty(camposmin.name() , "58");
		config.setProperty(nudgedelay.name() , "150");
		config.setProperty(docktarget.name() , "1.194_0.23209_0.17985_0.22649_129_116_80_67_-0.045455");
		config.setProperty(vidctroffset.name() , "0");
		config.setProperty(vlow.name() , "320_240_4_85");
		config.setProperty(vmed.name() , "320_240_8_95");
		config.setProperty(vhigh.name() , "640_480_8_85");
		config.setProperty(vfull.name() , "640_480_8_95");
		config.setProperty(vcustom.name() , "1024_768_8_85");
		config.setProperty(vset.name() , "vmed");
		config.setProperty(selfmicpushtotalk.name() , "true");
		config.setProperty(pushtotalk.name() , "true");
		config.setProperty(maxclicknudgedelay.name() , "580");
		config.setProperty(clicknudgemomentummult.name() , "0.7");
		config.setProperty(clicknudgedelaymomentumfactor.name() , "0.7");
		config.setProperty(maxclickcam.name() , "14");
		config.setProperty(volume.name() , "20");
		config.setProperty(muteonrovmove.name() , "true"); 
		config.setProperty(videoscale.name() , "100");
		config.setProperty(holdservo.name() , "false");
		config.setProperty(loginnotify.name() , "false");
		config.setProperty(reboot.name() , "false");
		config.setProperty(pushtotalk.name() , "false");
		
		return config;
	}

	/** @returns true if all settings are in properties */
	public static boolean validate(Properties conf) {
		Settings fromfile = new Settings();
		String value = null;
		for (GUISettings settings : GUISettings.values()) {
			value = fromfile.readSetting(settings.name() );
			if (value == null) {
				System.out.println(conf.toString());
				System.out.println("settings file missing: " + settings);
				return false;
			}
		}
		return true;
	}

	public static String getDefault(GUISettings factory) {
		Properties defaults = createDeaults();
		return defaults.getProperty(factory.name() );	
	}
}
