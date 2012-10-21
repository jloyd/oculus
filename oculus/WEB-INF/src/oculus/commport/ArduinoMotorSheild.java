package oculus.commport;

import oculus.Application;
import oculus.State;

import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;

public class ArduinoMotorSheild extends AbstractArduinoComm implements SerialPortEventListener, ArduioPort {
	
	private static final int AVERAGE_LEVEL = 4;
	int right, left, rightTotal, leftTotal, samples = 0;

	public ArduinoMotorSheild(Application app) {	
		super(app);
		new WatchDog().start();
	}


	@Override
	public void serialEvent(SerialPortEvent event) {
		if (event.getEventType() == SerialPortEvent.DATA_AVAILABLE) {
			super.manageInput();
		}
	}

	@Override
	public void execute() {
		String response = "";
		for (int i = 0; i < buffSize; i++)
			response += (char) buffer[i];

		if (response.equals("reset")) {

			isconnected = true;
			version = null;
			new Sender(GET_VERSION);
			updateSteeringComp();

		} else if (response.startsWith("version:")) {
			if (version == null) {
				version = response.substring(response.indexOf("version:") + 8, response.length());
				application.message("arduinoShield v: " + version, null, null);
			}
		} else if (response.charAt(0) != GET_VERSION[0]) {

			if(response.startsWith("right")){
				right = Integer.parseInt(response.split(" ")[1]);
				state.set("rightCurrent", right);
				rightTotal += right;
			}
			
			if(response.startsWith("left")){
				left = Integer.parseInt(response.split(" ")[1]);
				state.set("leftCurrent", left);
				leftTotal += left;
			}
			
			samples++;
			if(samples >= AVERAGE_LEVEL){
				application.sendplayerfunction("debug", "right: " + rightTotal/samples + " left: " + leftTotal/samples);
				samples = 0;
				rightTotal = 0;
				leftTotal = 0;
			}
		}
	}

	@Override
	public void connect() {
		try {

			serialPort = (SerialPort) CommPortIdentifier.getPortIdentifier(
					state.get(State.values.serialport)).open(
					ArduinoMotorSheild.class.getName(), SETUP);
			serialPort.setSerialPortParams(115200, SerialPort.DATABITS_8,
					SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);

			// open streams
			out = serialPort.getOutputStream();
			in = serialPort.getInputStream();

			// register for serial events
			serialPort.addEventListener(this);
			serialPort.notifyOnDataAvailable(true);

		} catch (Exception e) {
			return;
		}
	}

}