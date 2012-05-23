package oculus.commport;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Vector;

import oculus.Application;
import oculus.ManualSettings;
import oculus.Settings;
import oculus.State;
import oculus.Util;

import gnu.io.*;

public class Discovery implements SerialPortEventListener {
	
	// two states to watch for in settings 
	public static enum params {discovery, disabled};
	
	private static Settings settings = new Settings();
	private static final String motors = settings.readSetting(ManualSettings.arduinoculus);
	private static final String lights = settings.readSetting(ManualSettings.oculed);
	private static State state = State.getReference();
	
	/* serial port configuration parameters */
	//public static final int[] BAUD_RATES = { 115200, 57600 };
	public static final int TIMEOUT = 2000;
	public static final int DATABITS = SerialPort.DATABITS_8;
	public static final int STOPBITS = SerialPort.STOPBITS_1;
	public static final int PARITY = SerialPort.PARITY_NONE;
	public static final int FLOWCONTROL = SerialPort.FLOWCONTROL_NONE;

	/* add known devices here, strings returned from the firmware */
	public static final String OCULUS_TILT = "oculusTilt";
	public static final String OCULUS_SONAR = "oculusSonar";
	public static final String OCULUS_DC = "oculusDC";
	public static final String LIGHTS = "L";
	public static final long RESPONSE_DELAY = 1000;

	/* reference to the underlying serial port */
	private static SerialPort serialPort = null;
	private static InputStream inputStream = null;
	private static OutputStream outputStream = null;

	/* list of all free ports */
	private static Vector<String> ports = new Vector<String>();

	/* read from device */
	private static byte[] buffer = null; 
	
	private boolean handlingEvent = false;
	
	/* constructor makes a list of available ports */
	public Discovery() {
		
		if(motors.equals(params.disabled.toString()) && lights.equals(params.disabled.toString())) {
			Util.debug("discovery starting is disabled", this);
			return;
		}
		
		getAvailableSerialPorts();
		if(ports.size()==0){
			Util.log("no serial ports found on host", this);
			return;
		}
		
		if(motors.equals(params.discovery.toString())){		
			searchMotors(); 	
		} else if( ! motors.equals(params.disabled.toString())){			
			Util.debug("skipping discovery, motors on: " + motors, this);
			state.set(State.serialport, motors);
			state.set(State.firmware, OCULUS_DC);
		}
		
		if(lights.equals(params.discovery.toString())){	
			searchLights();	
		} else if( ! lights.equals(params.disabled.toString())){
			Util.debug("skipping discovery, lights on: " + lights, this);
			state.set(State.lightport, lights);
		}
	}
	
	/** */
	private static String getPortName(){
		
		String name = "";
		String com = serialPort.getName();
		
		//TODO: get a port name, or full device path for linux 
		if(Settings.os.equals("linux")) return com;
		else for(int i = 0 ; i < com.length();i++)
			if(com.charAt(i) != '/' && com.charAt(i) != '.')
				name += com.charAt(i);
		
		return name;
	}
	
	/** */
	private static void getAvailableSerialPorts() {
		ports.clear();
		@SuppressWarnings("rawtypes")
		Enumeration thePorts = CommPortIdentifier.getPortIdentifiers();
		while (thePorts.hasMoreElements()) {
			CommPortIdentifier com = (CommPortIdentifier) thePorts.nextElement();
			if (com.getPortType() == CommPortIdentifier.PORT_SERIAL) ports.add(com.getName());
		}
	}

	/** connects on start up, return true is currently connected */
	private boolean connect(final String address, final int rate) {

		Util.debug("try to connect to: " + address + " buad:" + rate, this);

		try {

			/* construct the serial port */
			serialPort = (SerialPort) CommPortIdentifier.getPortIdentifier(address).open("Discovery", TIMEOUT);

			/* configure the serial port */
			serialPort.setSerialPortParams(rate, DATABITS, STOPBITS, PARITY);
			serialPort.setFlowControlMode(FLOWCONTROL);

			/* extract the input and output streams from the serial port */
			inputStream = serialPort.getInputStream();
			outputStream = serialPort.getOutputStream();
			
			Util.debug("connected: " + address + " buad:" + rate, this);
			
			if (true) { // rate==115200) {
				Util.delay(TIMEOUT*2);
				doPortQuery();
			}
			else {
				// register for serial events
				Util.debug("registering port listeners... ",this);
				serialPort.addEventListener(this);
				serialPort.notifyOnDataAvailable(true);
			}

		} catch (Exception e) {
			Util.log("error connecting to: " + address, this);
			close();
			return false;
		}

		// be sure
		if (inputStream == null) return false;
		if (outputStream == null) return false;

		return true;
	}

	/** Close the serial port streams */
	private void close() {
		
		if (handlingEvent) { return; } // hopefully this is never used
		
		if (serialPort != null) {
			Util.log("close port: " + serialPort.getName() + " baud: " + serialPort.getBaudRate(), this);
			serialPort.removeEventListener();
			serialPort.close();
			serialPort = null;
		}
		
		try {
			if (inputStream != null) inputStream.close();
		} catch (Exception e) {
			Util.log("input stream close():" + e.getMessage(), this);
		}
		try {
			if (outputStream != null) outputStream.close();
		} catch (Exception e) {
			Util.log("output stream close():" + e.getMessage(), this);
		}
		
		buffer = null;
		// Util.delay(TIMEOUT);

	}

	/** Loop through all available serial ports and ask for product id's */
	public void searchLights() {
	
		// try to limit searching
		if(ports.contains(motors)) ports.remove(motors);
		if(state.get(State.serialport) != null) 
			ports.remove(state.getBoolean(State.serialport));
			
		Util.debug("discovery for lights starting on ports: " + ports.size(), this);
		
		for (int i = ports.size() - 1; i >= 0; i--) {
			if (state.get(State.lightport)!=null) { break; } // stop if find it
			//if (connect(ports.get(i), BAUD_RATES[0])) {	
			if (connect(ports.get(i), 57600)) {
				Util.delay(TIMEOUT*2);
				if (serialPort != null) { close(); }
			}
		}
	}
	
	/** Loop through all available serial ports and ask for product id's */
	public void searchMotors() {
			
		// try to limit searching 
		if(ports.contains(lights)) ports.remove(lights);
		
		Util.debug("discovery for motors starting on ports: " + ports.size(), this); 
	
		//for (int i = ports.size() - 1; i >= 0; i--) {
		for (int i=0; i<ports.size(); i++) {
			if (state.get(State.serialport)!=null) { break; } // stop if find it
			//if (connect(ports.get(i), BAUD_RATES[1])) {
			if (connect(ports.get(i), 115200)) {
				Util.delay(TIMEOUT*2);
				if (serialPort != null) { close(); }
			}
		}
	}
	
	/** check if this is a known derive, update in state */
	public void lookup(String id){	
		
		if (id == null) return;
		if (id.length() == 0) return;
		id = id.trim();
		
		Util.debug("is a product ID? [" + id + "] length: " + id.length(), this);

		if (id.length() == 1 ){
			if(id.equals(LIGHTS)){		
				state.set(State.lightport, getPortName());
				Util.debug("found lights on comm port: " +  getPortName(), this);		
			}
			
			return;
		} 
			
		if(id.startsWith("id")){	
			
			id = id.substring(2, id.length());
				
			Util.debug("found product id[" + id + "] on comm port: " +  getPortName(), this);

			if (id.equalsIgnoreCase(OCULUS_DC)) {

				state.set(State.serialport, getPortName());
				state.set(State.firmware, OCULUS_DC);
				
			} else if (id.equalsIgnoreCase(OCULUS_SONAR)) {

				state.set(State.serialport, getPortName());
				state.set(State.firmware, OCULUS_SONAR);	
			
			} else if (id.equalsIgnoreCase(OCULUS_TILT)) {

				state.set(State.serialport, getPortName());
				state.set(State.firmware, OCULUS_TILT);
				
			}

			//TODO: other devices here if grows
			
		}
	}
	
	/** send command to get product id */
	public void getProduct() {
		try {
			inputStream.skip(inputStream.available());
		} catch (IOException e) {
			Util.log(e.getStackTrace().toString(),this);
			return;
		}
		try {
			outputStream.write(new byte[] { 'x', 13 });
		} catch (IOException e) {
			Util.log(e.getStackTrace().toString(),this);
			return;
		}

		// wait for reply
		Util.delay(RESPONSE_DELAY);
	}

	@Override
	public void serialEvent(SerialPortEvent arg0) {
	
		if (!handlingEvent) { // ignoring other events .. hopefully not anything important
			handlingEvent = true;
			Util.debug("_event: " + arg0,this);
			
			if(buffer!=null){
				Util.debug("...too much serial?",this);
				return;
			}
			
			doPortQuery();				

			handlingEvent = false;
		}
	}
	
	private void doPortQuery() {
		byte[] buffer = new byte[32];
		
		getProduct();
		
		String device = new String();
		int read = 0;
		try {
			
			read = inputStream.read(buffer);
			
		} catch (IOException e) {
			Util.log(e.getStackTrace().toString(),this);
		}
		
		// read buffer 
		for (int j = 0; j < read; j++) {
			if(Character.isLetter((char) buffer[j]))
				device += (char) buffer[j];
		}
		
		// Util.debug("_lookup: " + device, this);
		
		lookup(device);

		close();
	}

	/** match types of firmware names and versions */
	public AbstractArduinoComm getMotors(Application application) {
		return new ArduinoCommDC(application);
	}
	

	/** manage types of ights here */
	public LightsComm getLights(Application application) {
		return new LightsComm(application);
	}
}