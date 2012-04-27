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

	Settings settings = new Settings();

	@Before
	public void setUp() throws Exception {
		System.out.println("before..");
	}

	@After
	public void tearDown() throws Exception {
		System.out.println("after..");
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
	}

	
	@Test
	public void validateOptionalSetting() {
		Properties defaults = OptionalSettings.createDeaults();
		for (FactorySettings factory : FactorySettings.values()) {
			String val = factory.toString();
			if (!defaults.containsKey(val))
				fail("default setting missing: " + factory.toString());
		}
	}
}
