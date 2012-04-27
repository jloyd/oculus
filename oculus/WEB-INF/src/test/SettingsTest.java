package test;

import static org.junit.Assert.*;

import java.util.Properties;

import oculus.FactorySettings;
import oculus.OptionalSettings;
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
		if(Settings.settingsfile != null)
			if(Settings.settingsfile.contains("null"))
				fail("no settings file found");
	}

	@After
	public void tearDown() throws Exception {
		System.out.println("tearDown(), after.. done");
	}
	
	@Test
	public void testReadSetting() {
		// read settings file, check against factory settings
		for (FactorySettings factory : FactorySettings.values()) {
			String val = settings.readSetting(factory.toString());
			if (val == null) 
				fail("setting missing in file: " + factory.toString());
		}
	}

	@Test
	public void validateDefaultSetting() {
		Properties defaults = FactorySettings.createDeaults();
		for (FactorySettings factory : FactorySettings.values()) {
			String val = factory.toString();
			if (!defaults.containsKey(val))
				fail("default setting missing: " + factory.toString());
		}
		
		if(defaults.getProperty(FactorySettings.vlow.toString()).split("_").length != 4) 
			 fail("vlow default values are invalid");
		if(defaults.getProperty(FactorySettings.vmed.toString()).split("_").length != 4) 
			 fail("vmed default values are invalid");
		if(defaults.getProperty(FactorySettings.vhigh.toString()).split("_").length != 4) 
			 fail("vhigh default values are invalid");
		if(defaults.getProperty(FactorySettings.vfull.toString()).split("_").length != 4) 
			 fail("vfull default values are invalid");
		
		if(settings.readSetting(FactorySettings.vlow.toString()).split("_").length != 4) 
			 fail("vlow settings are invalid");
		if(settings.readSetting(FactorySettings.vmed.toString()).split("_").length != 4) 
			 fail("vmed settings are invalid");
		if(settings.readSetting(FactorySettings.vhigh.toString()).split("_").length != 4) 
			 fail("vhigh settings are invalid");
		if(settings.readSetting(FactorySettings.vfull.toString()).split("_").length != 4) 
			 fail("vfull settings are invalid");
		
	}

	
	@Test
	public void validateOptionalSetting() {
		
		// test default example: 320_240_8_85
		Properties defaults = OptionalSettings.createDeaults();
		String[] cords = defaults.getProperty(OptionalSettings.vself.toString()).split("_");
		if(cords.length!=4) fail("vself options are invalid: " + cords.length); 
		
		// test in current file 
		cords = settings.readSetting(OptionalSettings.vself.toString()).split("_");
		if(cords.length!=4) fail("vself options are invalid in settings file"); 
		
	}
}
