package test;

import static org.junit.Assert.*;

import java.util.Properties;

import oculus.GUISettings;
import oculus.ManualSettings;
import oculus.PlayerCommands;
import oculus.Settings;

import org.junit.Before;
import org.junit.Test;

public class SettingsTest {

	Settings settings = null;

	@Before
	public void setUp() {
		System.out.println("running: " + getClass().toString());
		settings = new Settings();
		
		if(settings==null) fail("no settings file found");
		
		if(Settings.settingsfile != null)
			if(Settings.settingsfile.contains("null"))
				fail("no settings file found");
		
		if(settings.readSetting("salt").equals("null")) fail("no salt!"); 
	}

	//@After
	//public void tearDown() throws Exception {
	//System.out.println("tearDown(), after.. done");
	
	@Test
	public void testReadSetting() {	
		for (GUISettings factory : GUISettings.values()){ 
			if(settings.readSetting(factory.toString())==null)
				fail("setting missing in file: " + factory.toString());
		}
		
		for (ManualSettings factory : ManualSettings.values()) {
			if(settings.readSetting(factory.toString())==null)
				fail("setting missing in file: " + factory.toString());
		}
	}

	@Test 
	public void playerCommands(){
		
		// make sure no duplicates A - B
		for (PlayerCommands factory : PlayerCommands.values()) {
			String val = factory.toString();
			for (developer.TelnetServer.Commands cmd : developer.TelnetServer.Commands.values()){
				if(cmd.toString().equals(val)) 
					fail("player commands overlap telnet commands: " + val);				
			}
		}
		
		// make sure no duplicates B - A 
		for (developer.TelnetServer.Commands factory : developer.TelnetServer.Commands.values()) {
			String val = factory.toString();
			for (PlayerCommands cmd : PlayerCommands.values()){
				if(cmd.toString().equals(val)) 
					fail("player commands overlap telnet commands: " + val);				
			}
		}
		
		// make sure is a subset 
		//for (developer.TelnetServer.Commands factory : developer.TelnetServer.Commands.values()) {
			
		//}
		
		// list admin or params 
		/*
		for (PlayerCommands factory : PlayerCommands.values()) {
			String val = factory.toString();
			System.out.print(val);
			if(PlayerCommands.requiresArgument(val)) System.out.print(val + " - requires Argument");
			if(PlayerCommands.requiresAdmin(val)) System.out.print(" - requires Admin");
			System.out.println();
		}
		*/
		
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
