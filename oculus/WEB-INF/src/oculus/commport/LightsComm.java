package oculus.commport;

import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.TooManyListenersException;

import oculus.Application;
import oculus.State;
import oculus.Util;

public class LightsComm implements SerialPortEventListener {

	// shared state variables 
	private State state = State.getReference();
	
	public static final int SETUP = 2000;
	public static final int BAUD_RATE = 57600;
	//public static final int WATCHDOG_DELAY = 1500;

	public static final byte DIM = 'f';
	public static final byte BRIGHTER = 'b';
	//public static final byte SET_PWM = 's';
	public static final byte GET_VERSION = 'y';
	//private static final byte ECHO_ON = 'e';
	//private static final byte ECHO_OFF = 'o';
	
	// comm cannel 
	private SerialPort serialPort = null;
	private InputStream in= null;
	private OutputStream out= null;
	
	// will be discovered from the device 
	protected String version = null;

	// track write times
	private long lastSent = System.currentTimeMillis();
	private long lastRead = System.currentTimeMillis();

	// make sure all threads know if connected 
	private boolean isconnected = false;
	
	private int spotLightBrightness = 0;
	private boolean floodLightOn = false;
	
	// call back
	private Application application = null;

	/**
	 * Constructor but call connect to configure
	 * 
	 * @param app 
	 * 			  is the main oculus application, we need to call it on
	 * 			Serial events like restet            
	 */
	public LightsComm(Application app) {
		application = app; 
		
		if( state.get(State.lightport) != null ){
			new Thread(new Runnable() { 
				public void run() {
					connect();				
					Util.delay(SETUP);
				}	
			}).start();
		}	
	}
	
	/** open port, enable read and write, enable events */
	public void connect() {
		try {

			serialPort = (SerialPort)CommPortIdentifier.getPortIdentifier(
					state.get(State.lightport)).open(LightsComm.class.getName(), SETUP);
			serialPort.setSerialPortParams(BAUD_RATE, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);

			// open streams
			out = serialPort.getOutputStream();
			in = serialPort.getInputStream();
			
		} catch (Exception e) {
			Util.log("could NOT connect to the the lights on:" + state.get(State.lightport), this);
			return;
		}
		
		
		// register for serial events
		try {
			serialPort.addEventListener(this);
		} catch (TooManyListenersException e) {
			Util.log(e.getMessage(), this);
		}
		serialPort.notifyOnDataAvailable(true);
		isconnected = true;	
		
		Util.log("connected to the the lights on:" + state.get(State.lightport), this);
	
	}

	/** @return True if the serial port is open */
	public boolean isConnected(){
		return isconnected;
	}
	
	public int spotLightBrightness() {
		return spotLightBrightness;
	}
	
	public boolean floodLightOn() {
		return floodLightOn;
	}
	
	@Override
	/** buffer input on event and trigger parse on '>' charter  
	 * 
	 * Note, all feedback must be in single xml tags like: <feedback 123>
	 */
	public void serialEvent(SerialPortEvent event) {
		if (event.getEventType() == SerialPortEvent.DATA_AVAILABLE) {
			try {
				byte[] input = new byte[32];
				int read = in.read(input);
				for (int j = 0; j < read; j++) {
					
					Util.log("exec[" + "] read: " + read, this);
					
					
				}
			} catch (IOException e) {
				System.out.println("event : " + e.getMessage());
			}
		}
	}

	/** @return the time since last write() operation */
	public long getWriteDelta() {
		return System.currentTimeMillis() - lastSent;
	}

	/** @return this device's firmware version */
	public String getVersion(){
		return version;
	}
	
	/** @return the time since last read operation */
	public long getReadDelta() {
		return System.currentTimeMillis() - lastRead;
	}

	/** inner class to send commands */
	private class Sender extends Thread {		
		private byte command = 13;
		public Sender(final byte cmd) {
			command = cmd;
			if(isConnected())start();
		}
		public void run() {
			sendCommand(command);
			Util.debug("send: " + command, this);
		}
	}

	
	
	/** inner class to check if getting responses in timely manor 
	private class WatchDog extends Thread {
		public WatchDog() {
			this.setDaemon(true);
		}
		public void run() {
			Util.delay(SETUP);
			application.message("starting watchdog thread", null, null);
			while (true) {
				if (getReadDelta() > DEAD_TIME_OUT) {
					if (isconnected) {
						reset(); 
						application.message("watchdog time out, resetting", null, null);
					}
				}

				// send ping to keep connection alive 
				if(getReadDelta() > (DEAD_TIME_OUT / 3))
					if(isconnected) new Sender(GET_VERSION);
			
				Util.delay(WATCHDOG_DELAY);
			}
		}
	}*/
	
	public void reset(){
		if (isconnected) {
			new Thread(new Runnable() { 
				public void run() {
					disconnect();
					connect();
				}
			}).start();
		}
	}

	/** shutdown serial port */
	protected void disconnect() {
		try {
			in.close();
			out.close();
			isconnected = false;
		} catch (Exception e) {
			System.out.println("close(): " + e.getMessage());
		}
		serialPort.close();
	}

	/**
	 * Send a multi byte command to send the arduino 
	 * 
	 * @param command
	 *            is a byte array of messages to send
	 */
	private synchronized void sendCommand(final byte command) {
		
		if(!isconnected) return;
		
		try {
			out.write(command);
		} catch (Exception e) {
			reset();
			System.out.println(e.getMessage());
		}

		// track last write
		lastSent = System.currentTimeMillis();
	}

	public synchronized void setSpotLightBrightness(int target){
		
		if( !isConnected()){
			Util.log("lights NOT found", this);
			return;
		}
		
		int n = target*255/100;
		Util.log("set spot light: ", this);
		new Sender((byte) n);
		
		spotLightBrightness = target;
		application.message("spotlight brightness set to "+target+"%", "light", Integer.toString(spotLightBrightness));
	}
	
	public synchronized void floodLight(String str){
		if( !isConnected()){
			Util.log("lights NOT found", this);
			application.message("lights not found", null, null);
			return;
		}
		if (str.equals("on")) { 
			new Sender((byte)'e');
			floodLightOn = true;
		}
		else { 
			new Sender((byte)'d');
			floodLightOn = false; 
		}
		application.message("floodlight "+str, null, null);
	}
}