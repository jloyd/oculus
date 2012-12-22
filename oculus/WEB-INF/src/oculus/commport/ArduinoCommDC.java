package oculus.commport;

import oculus.Application;
import oculus.State;
import oculus.Util;

import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;

public class ArduinoCommDC extends AbstractArduinoComm implements SerialPortEventListener, ArduinoPort {

	public ArduinoCommDC(Application app) {
		super(app);

		// check for lost connection
		new WatchDog().start();
	}

	public void connect(){
		try {

			serialPort = (SerialPort) CommPortIdentifier.getPortIdentifier(
					state.get(State.values.serialport)).open(
					AbstractArduinoComm.class.getName(), SETUP);
			serialPort.setSerialPortParams(115200, SerialPort.DATABITS_8,
					SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);

			// open streams
			out = serialPort.getOutputStream();
			in = serialPort.getInputStream();

			// register for serial events
			serialPort.addEventListener(this);
			serialPort.notifyOnDataAvailable(true);
			
//			if (Settings.os == "linux") { Util.delay(SETUP); }

		} catch (Exception e) {
			Util.log("could NOT connect to the motors on: " + state.get(State.values.serialport), this);
			return;
		}
	}

	public void serialEvent(SerialPortEvent event) {
		if (event.getEventType() == SerialPortEvent.DATA_AVAILABLE) {
			manageInput();
		}
	}
	
	@Override
	public void execute() {
		String reply = "";
		for (int i = 0; i < buffSize; i++)
			reply += (char) buffer[i];

		// Util.debug("in: " + reply, this);

		// take action as arduino has just turned on
		if (reply.equals("reset")) {
			isconnected = true;
			version = null;
			new Sender(GET_VERSION);
			updateSteeringComp();
		} else if (reply.startsWith("version:")) {
			if (version == null) {
				// get just the number
				version = reply.substring(reply.indexOf("version:") + 8, reply.length());
				application.message("oculusDC: " + version, null, null);
			} 
		} else if (reply.charAt(0) != GET_VERSION[0]) {
			// don't bother showing watch dog pings to user screen
			application.message("oculusDC: " + reply, null, null);
//		} else if( reply.startsWith("analog")) {
//			String[] ans = reply.split(" ");
//			state.set("analog", ans[1]);
		} else if( reply.startsWith("digital")) {
			String[] ans = reply.split(" ");
			state.set("digital", ans[1]);
		}
	}
}