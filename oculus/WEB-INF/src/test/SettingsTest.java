package test;

import static org.junit.Assert.*;

import java.util.Properties;

import oculus.GUISettings;
import oculus.ManualSettings;
import oculus.PlayerCommands;
import oculus.Settings;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SettingsTest {

	Settings settings = null;

	@Before
	public void setUp() {
		System.out.println(getClass().toString());
		settings = new Settings();
		
		if(settings==null) fail("no settings file found");
		
		if(Settings.settingsfile != null)
			if(Settings.settingsfile.contains("null"))
				fail("no settings file found");
		
		if(settings.readSetting("salt").equals("null")) fail("no salt!"); 
	}

	@After
	public void tearDown() throws Exception {
		System.out.println("tearDown(), after.. done");
	}
	
	@Test
	public void testReadSetting() {
		
		for (GUISettings factory : GUISettings.values()){ 
		
			if(settings.readSetting(factory.toString())==null)
				fail("setting missing in file: " + factory.toString());
			
			// System.out.println("file: " + factory.toString() + " " + settings.readSetting(factory.toString()));
		}
		
		for (ManualSettings factory : ManualSettings.values()) {
			
			if(settings.readSetting(factory.toString())==null)
				fail("setting missing in file: " + factory.toString());
			
			//System.out.println("file: " + factory.toString() + " " + settings.readSetting(factory.toString()));
			//System.out.println("default: " + factory.toString() + " " + ManualSettings.getDefault(factory));

		}
	}

	@Test
	public void validatePlayerCommands(){
		for(PlayerCommands.AdminCommands cmd : PlayerCommands.AdminCommands.values()){
		PlayerCommands exists = PlayerCommands.valueOf(cmd.toString());
			if(exists==null) fail("admin commands in player commands? " + exists);
		}
	}
	
	
	@Test
	public void validate(){
		System.out.println("-----------------------------------------");
		for(PlayerCommands cmd : PlayerCommands.values()){
			
			//PlayerCommands exists = PlayerCommands.valueOf(cmd.toString());
				
			//if(PlayerCommands.requiresArgument(cmd.toString()))
			//if(cmd.requiresArgument())
			//	System.out.println("_"+cmd.toString());
			
			//if(exists==null) fail("admin commands in player commands? " + exists);
		}
	}
	
	
	@Test
	public void validateDefaultSetting() {
		Properties defaults = GUISettings.createDeaults();
		for (GUISettings factory : GUISettings.values()) {
			String val = factory.toString();
			if (!defaults.containsKey(val))
				fail("default setting missing: " + factory.toString());
		}
		
		if(defaults.getProperty(GUISettings.vlow.toString()).split("_").length != 4) 
			 fail("vlow default values are invalid");
		if(defaults.getProperty(GUISettings.vmed.toString()).split("_").length != 4) 
			 fail("vmed default values are invalid");
		if(defaults.getProperty(GUISettings.vhigh.toString()).split("_").length != 4) 
			 fail("vhigh default values are invalid");
		if(defaults.getProperty(GUISettings.vfull.toString()).split("_").length != 4) 
			 fail("vfull default values are invalid");
		
		if(settings.readSetting(GUISettings.vlow.toString()).split("_").length != 4) 
			 fail("vlow settings are invalid");
		if(settings.readSetting(GUISettings.vmed.toString()).split("_").length != 4) 
			 fail("vmed settings are invalid");
		if(settings.readSetting(GUISettings.vhigh.toString()).split("_").length != 4) 
			 fail("vhigh settings are invalid");
		if(settings.readSetting(GUISettings.vfull.toString()).split("_").length != 4) 
			 fail("vfull settings are invalid");
		
	}

	/*
	@Test
	public void validateOptionalSetting() {
		
		// test default example: 320_240_8_85
		Properties defaults = GUISettings.createDeaults();
		String[] cords = defaults.getProperty(ManualSettings.vself.toString()).split("_");
		if(cords.length!=4) fail("vself options are invalid: " + cords.length); 
		
		// test in current file 
		cords = settings.readSetting(ManualSettings.vself.toString()).split("_");
		if(cords.length!=4) fail("vself options are invalid in settings file"); 
		
	}
	*/
	
}
