package oculus.commport;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Vector;

import oculus.State;
import oculus.Util;

import gnu.io.*;

public class Discovery {

	private State state = State.getReference();

	/* serial port configuration parameters */
	public static final int[] BAUD_RATES = { 57600, 115200 };
	public static final int TIMEOUT = 2000;
	public static final int DATABITS = SerialPort.DATABITS_8;
	public static final int STOPBITS = SerialPort.STOPBITS_1;
	public static final int PARITY = SerialPort.PARITY_NONE;
	public static final int FLOWCONTROL = SerialPort.FLOWCONTROL_NONE;

	/* add known devices here, strings returned from the firmware */
	public static final String OCULUS_TILT = "id:oculusTilt";
	public static final String OCULUS_SONAR = "id:oculusSonar";
	public static final String OCULUS_DC = "id:oculusDC";
	public static final String LIGHTS = "id:oculusLights";
	public static final long RESPONSE_DELAY = 1000;

	/* reference to the underlying serial port */
	private SerialPort serialPort = null;
	private InputStream inputStream = null;
	private OutputStream outputStream = null;

	/* list of all free ports */
	private Vector<String> ports = new Vector<String>();

	/* constructor makes a list of available ports */
	public Discovery() {
		getAvailableSerialPorts();
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

		Util.log("connecting to: " + address + " buad:" + rate, this);

		try {

			/* construct the serial port */
			serialPort = (SerialPort) CommPortIdentifier.getPortIdentifier(address).open("Discovery", TIMEOUT);

			/* configure the serial port */
			serialPort.setSerialPortParams(rate, DATABITS, STOPBITS, PARITY);
			serialPort.setFlowControlMode(FLOWCONTROL);

			/* extract the input and output streams from the serial port */
			inputStream = serialPort.getInputStream();
			outputStream = serialPort.getOutputStream();

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

	/**
	 * Loop through all available serial ports and ask for product id's
	 */
	public void search() {
		Util.log("number buad rates to try: " + BAUD_RATES.length, this);
		for (int j = 0; j < BAUD_RATES.length; j++) {
			for (int i = ports.size() - 1; i >= 0; i--) {
				if (connect(ports.get(i), BAUD_RATES[j])) {

					Util.delay(TIMEOUT);
					String id = getProduct();

					// if (id == null)break;
					// if (id.length() == 0)break;

					Util.log("search product :" + id + " buad:" + BAUD_RATES[j], this);

					if (id.length() > 1) {

						// trim delimiters "<xxxxx>" first
						// test for '>'??
						id = id.substring(1, id.length() - 1).trim();

						if (id.equalsIgnoreCase(LIGHTS)) {

							state.set(State.lightport, ports.get(i));

						} else if (id.equalsIgnoreCase(OCULUS_DC)) {

							state.set(State.serialport, ports.get(i));
							state.set(State.firmware, OCULUS_DC);

						} else if (id.equalsIgnoreCase(OCULUS_SONAR)) {

							state.set(State.serialport, ports.get(i));
							state.set(State.firmware, OCULUS_SONAR);
							
						} else if (id.equalsIgnoreCase(OCULUS_TILT)) {

							state.set(State.serialport, ports.get(i));
							state.set(State.firmware, OCULUS_TILT);

						}

						// other devices here if grows

					}
				}

				// close on each loop
				close();
			}
		}
		
		// could not find, no hardware attached
		if (state.get(State.firmware) == null) {
			state.set(State.firmware, State.unknown);
			Util.log("no hardware detected", this);
			// state.dump();
		}
	}

	/** send command to get product id */
	public String getProduct() {

		byte[] buffer = new byte[32];
		String device = new String();

		// be sure there is no old bytes in our reply
		try {
			inputStream.skip(inputStream.available());
		} catch (IOException e) {
			e.printStackTrace();
		}
		// send command to arduino
		try {
			outputStream.write(new byte[] { 'x', 13 });
		} catch (IOException e) {
			e.printStackTrace();
		}

		// wait for reply
		Util.delay(RESPONSE_DELAY);

		// read it
		int read = 0;
		try {
			read = inputStream.read(buffer); // TODO: hangs here on Linux
												// desktop w/ physical serial
												// port
		} catch (IOException e) {
			e.printStackTrace();
		}
		for (int j = 0; j < read; j++)
			device += (char) buffer[j];

		return device.trim();
	}
}