package oculus.commport;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Vector;

import oculus.Settings;
import oculus.State;
import oculus.Util;

import gnu.io.*;

public class Discovery implements SerialPortEventListener {

	private State state = State.getReference();

	/* serial port configuration parameters    */
	public static final int[] BAUD_RATES = { 57600, 115200 };
	public static final int TIMEOUT = 2000;
	public static final int DATABITS = SerialPort.DATABITS_8;
	public static final int STOPBITS = SerialPort.STOPBITS_1;
	public static final int PARITY = SerialPort.PARITY_NONE;
	public static final int FLOWCONTROL = SerialPort.FLOWCONTROL_NONE;

	/* add known devices here, strings returned from the firmware */
	public static final String OCULUS_TILT = "oculusTilt";
	public static final String OCULUS_SONAR = "oculusSonar";
	public static final String OCULUS_DC = "oculusDC";
	public static final String LIGHTS = "oculusLights";
	public static final long RESPONSE_DELAY = 1000;

	/* reference to the underlying serial port */
	private static SerialPort serialPort = null;
	private static InputStream inputStream = null;
	private static OutputStream outputStream = null;

	/* list of all free ports */
	private static Vector<String> ports = new Vector<String>();

	/* read from device */
	byte[] buffer = null; 
	
	/* constructor makes a list of available ports */
	public Discovery() {	
		getAvailableSerialPorts();
		Util.log("discovery starting on: " + ports.size(), this);
		search();
	}

	/** */
	private void getAvailableSerialPorts() {
		@SuppressWarnings("rawtypes")
		Enumeration thePorts = CommPortIdentifier.getPortIdentifiers();
		while (thePorts.hasMoreElements()) {
			CommPortIdentifier com = (CommPortIdentifier) thePorts.nextElement();
			if (com.getPortType() == CommPortIdentifier.PORT_SERIAL) ports.add(com.getName());
		}
	}

	/** connects on start up, return true is currently connected */
	private boolean connect(final String address, final int rate) {

		Util.log("try to connect to: " + address + " buad:" + rate, this);

		try {

			/* construct the serial port */
			serialPort = (SerialPort) CommPortIdentifier.getPortIdentifier(address).open("Discovery", TIMEOUT);

			/* configure the serial port */
			serialPort.setSerialPortParams(rate, DATABITS, STOPBITS, PARITY);
			serialPort.setFlowControlMode(FLOWCONTROL);

			/* extract the input and output streams from the serial port */
			inputStream = serialPort.getInputStream();
			outputStream = serialPort.getOutputStream();

			// register for serial events
			serialPort.addEventListener(this);
			serialPort.notifyOnDataAvailable(true);
			
		} catch (Exception e) {
			Util.log("error connecting to: " + address, this);
			close();
			return false;
		}

		// be sure
		if (inputStream == null) return false;
		if (outputStream == null) return false;

		Util.log("connected: " + address + " buad:" + rate, this);
		
		return true;
	}

	/** Close the serial port streams */
	private void close() {
		
		//if(serialPort.gserialPort.removeEventListener();
		
		if (serialPort != null) {
			serialPort.close();
			serialPort = null;
		}
		try {
			if (inputStream != null) {
				inputStream.close();
			}
		} catch (Exception e) {
			System.err.println("input stream close():" + e.getMessage());
		}
		try {
			if (outputStream != null)
				outputStream.close();
		} catch (Exception e) {
			System.err.println("output stream close():" + e.getMessage());
		}
	}

	/** Loop through all available serial ports and ask for product id's */
	public void search() {
		Util.log("number buad rates to try: " + BAUD_RATES.length, this);
		for (int j = 0; j < BAUD_RATES.length; j++) {
			for (int i = ports.size() - 1; i >= 0; i--) {
				if (connect(ports.get(i), BAUD_RATES[j])) {	
					
					Util.delay(TIMEOUT*2);
					close();
				}
			}
		}
		
		// could not find, no hardware attached
		if (state.get(State.firmware) == null) {
			state.set(State.firmware, State.unknown);
			Util.log("no hardware detected", this);
		}
	}
	
	private static String getName(){
		
		String name = "";
		String com = serialPort.getName();
		
		if(Settings.os.equals("linux")) return com;
		
		for(int i = 0 ; i < com.length();i++)
			if(com.charAt(i) != '/' && com.charAt(i) != '.')
				name += com.charAt(i);
		
		return name;
	}

	/**
	 * check if this is a known derive, update in state
	 */
	public void lookup(String id){
		
		if (id == null) return;
		if (id.length() == 0) return;

		Util.log("is a product?? [" + id + "]", this);

		if(id.startsWith("id")){
			
			id = id.substring(2, id.length());
			Util.log("found product[" + id + "] on comm port: " +  getName(), this);

			if (id.equalsIgnoreCase(LIGHTS)) {

				state.set(State.lightport, getName());
				ports.remove(serialPort.getName());

			} else if (id.equalsIgnoreCase(OCULUS_DC)) {

				state.set(State.serialport, getName());
				state.set(State.firmware, OCULUS_DC);
				ports.remove(serialPort.getName());
				
			} else if (id.equalsIgnoreCase(OCULUS_SONAR)) {

				state.set(State.serialport, getName());
				state.set(State.firmware, OCULUS_SONAR);
				ports.remove(serialPort.getName());
			
			} else if (id.equalsIgnoreCase(OCULUS_TILT)) {

				state.set(State.serialport, getName());
				state.set(State.firmware, OCULUS_TILT);
				ports.remove(serialPort.getName());
				
			}

			// other devices here if grows

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
	
		Util.log("_event: " + arg0,this);
		
		if(buffer!=null){
			Util.log("too much serial ",this);
			return;
		}
		
		// don't fire again 
		// serialPort.removeEventListener();
	
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
		
		Util.log("_lookup: " + device, this);
		
		lookup(device);
		
	}
}