package oculus.commport;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import oculus.Application;
import oculus.GUISettings;
import oculus.Settings;
import oculus.State;
import oculus.Util;

import gnu.io.SerialPort;

public abstract class AbstractArduinoComm implements ArduinoPort {

	protected long lastSent = System.currentTimeMillis();
	protected long lastRead = System.currentTimeMillis();
	protected Settings settings = Settings.getReference();
	protected State state = State.getReference();
	protected Application application = null;
	protected volatile boolean isconnected = false;
	protected SerialPort serialPort = null;	
	protected String version = null;
	protected OutputStream out;
	protected InputStream in;

	// data buffer 
	protected byte[] buffer = new byte[32];
	protected int buffSize = 0;
	
	// config via settings file 
	protected int tempspeed = 999;
	protected int clicknudgedelay = 0;
	protected String tempstring = null;
	protected int tempint = 0;
	
	public int speedslow;
	public int speedmed;
	public int camservohoriz;
	public int camposmax;
	public int camposmin;
	public int nudgedelay;
	public int maxclicknudgedelay;
	public int maxclickcam;
	public double clicknudgemomentummult;
	public int steeringcomp;
	public boolean holdservo;
	
	public int camservodirection = 0;
	public int camservopos; 
	public int camwait = 400;
	public int camdelay = 50;
	public int speedfast = 255;
	public int turnspeed = 255;

	public static final String DIRECTION = State.values.tempdirection.name();

	public AbstractArduinoComm(Application app) {

		application = app;
		
		speedslow = settings.getInteger("speedslow");
		speedmed = settings.getInteger("speedmed");
		camservohoriz = settings.getInteger("camservohoriz");
		camservopos = camservohoriz;
		camposmax = settings.getInteger("camposmax");
		camposmin = settings.getInteger("camposmin");
		nudgedelay = settings.getInteger("nudgedelay");
		maxclicknudgedelay = settings.getInteger("maxclicknudgedelay");
		maxclickcam = settings.getInteger("maxclickcam");
		clicknudgemomentummult = settings.getDouble("clicknudgemomentummult");
		steeringcomp = settings.getInteger("steeringcomp");
		holdservo = settings.getBoolean(GUISettings.holdservo.toString());
		
		state.set(State.values.speed, speedfast);
		state.set(State.values.moving, false);
		state.set(State.values.sliding, false);
		state.set(State.values.movingforward, false);
		state.set(State.values.camservopos, camservopos);
		
		if (state.get(State.values.serialport) != null) {
			new Thread(new Runnable() {
				public void run() {
					connect();
					Util.delay(SETUP);
					Util.log("Connected to the motors on: " + state.get(State.values.serialport), this);

					byte[] cam = { CAM, (byte) camservohoriz };
					sendCommand(cam);
					Util.delay(camwait);
					sendCommand(CAMRELEASE);
				}
			}).start();
		}
	}

	/** inner class to send commands as a separate thread each */
	class Sender extends Thread {
		private byte[] command = null;
		public Sender(final byte[] cmd) {
			if (isconnected){
				command = cmd;
				this.start();
			}
		}

		public void run() {
			// Util.debug("sending: " + command[0], this);
			sendCommand(command);
		}
	}

	/** inner class to check if getting responses in timely manor */
	public class WatchDog extends Thread {
		public WatchDog() {
			this.setDaemon(true);
		}

		public void run() {
			Util.delay(SETUP);
			while (true) {

				if (getReadDelta() > DEAD_TIME_OUT) {
					Util.log("arduino watchdog time out, may be no hardware attached", this);
					return; // die, no point living?
				}

				if (getReadDelta() > WATCHDOG_DELAY) {
					new Sender(GET_VERSION);
					Util.delay(WATCHDOG_DELAY);
				}
			}		
		}
	}

	@Override
	public abstract void connect();
	
	@Override
	public boolean isConnected() {
		return isconnected;
	}

	public abstract void execute(); 
	
	/** */
	public void manageInput(){
		try {
			byte[] input = new byte[32];
			int read = in.read(input);
			for (int j = 0; j < read; j++) {
				// print() or println() from arduino code
				if ((input[j] == '>') || (input[j] == 13)
						|| (input[j] == 10)) {
					// do what ever is in buffer
					if (buffSize > 0)
						execute();
					// reset
					buffSize = 0;
					// track input from arduino
					lastRead = System.currentTimeMillis();
				} else if (input[j] == '<') {
					// start of message
					buffSize = 0;
				} else {
					// buffer until ready to parse
					buffer[buffSize++] = input[j];
				}
			}
		} catch (IOException e) {
			System.out.println("event : " + e.getMessage());
		}
	}
	
	@Override
	public long getWriteDelta() {
		return System.currentTimeMillis() - lastSent;
	}

	@Override
	public String getVersion() {
		return version;
	}

	@Override
	public long getReadDelta() {
		return System.currentTimeMillis() - lastRead;
	}

	@Override
	public void setEcho(boolean update) {
		if (update) new Sender(ECHO_ON);
		else new Sender(ECHO_OFF);
	}

	@Override
	public void reset() {
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
			version = null;
		} catch (Exception e) {
			System.out.println("disconnect(): " + e.getMessage());
		}
		serialPort.close();
	}

	/**
	 * Send a multi byte command to send the arduino
	 * 
	 * @param command
	 *            is a byte array of messages to send
	 */
	protected synchronized void sendCommand(final byte[] command) {

		if (!isconnected)
			return;

		try {

			// send
			out.write(command);

			// end of command
			out.write(13);

		} catch (Exception e) {
			reset();
			Util.log("OCULUS: sendCommand(), " + e.getMessage(), this);
		}

		// track last write
		lastSent = System.currentTimeMillis();
	}

	@Override
	public void stopGoing() {

		if (state.getBoolean(State.values.muteROVonMove) && state.getBoolean(State.values.moving)) {
			application.unmuteROVMic();
		}

		new Sender(STOP);
		state.set(State.values.moving, false);
		state.set(State.values.movingforward, false);
	}

	@Override
	public void goForward() {
		new Sender(new byte[] { FORWARD, (byte) state.getInteger(State.values.speed) });
		state.set(State.values.moving, true);
		state.set(State.values.movingforward, true);

		if (state.getBoolean(State.values.muteROVonMove)) {
			application.muteROVMic();
		}
	}

	@Override
	public void goBackward() {
		new Sender(new byte[] { BACKWARD, (byte) state.getInteger(State.values.speed) });
		state.set(State.values.moving, true);
		state.set(State.values.movingforward, false);

		if (state.getBoolean(State.values.muteROVonMove)) {
			application.muteROVMic();
		}
	}

	@Override
	public void turnRight() {
		int tmpspeed = turnspeed;
		int boost = 10;
		if (state.getInteger(State.values.speed) < turnspeed && (state.getInteger(State.values.speed) + boost) < speedfast)
			tmpspeed = state.getInteger(State.values.speed) + boost;

		new Sender(new byte[] { RIGHT, (byte) tmpspeed });
		state.set(State.values.moving, true);

		if (state.getBoolean(State.values.muteROVonMove)) {
			application.muteROVMic();
		}
	}

	@Override
	public void turnLeft() {
		int tmpspeed = turnspeed;
		int boost = 10;
		if (state.getInteger(State.values.speed) < turnspeed && (state.getInteger(State.values.speed) + boost) < speedfast)
			tmpspeed = state.getInteger(State.values.speed) + boost;

		new Sender(new byte[] { LEFT, (byte) tmpspeed });
		state.set(State.values.moving, true);

		if (state.getBoolean(State.values.muteROVonMove)) {
			application.muteROVMic();
		}
	}
	
	@Override
	public void camGo() {
		new Thread(new Runnable() {
			public void run() {
				while (camservodirection != 0) {
					sendCommand(new byte[] { CAM, (byte) camservopos });
					Util.delay(camdelay);
					camservopos += camservodirection;
					if (camservopos > camposmax) {
						camservopos = camposmax;
						camservodirection = 0;
					}
					if (camservopos < camposmin) {
						camservopos = camposmin;
						camservodirection = 0;
					}
				}

				checkForHoldServo();
			}
		}).start();
	}

	@Override
	public void camCommand(String str) {
		if (str.equals("stop")) {
			camservodirection = 0;
		} else if (str.equals("up")) {
			camservodirection = 1;
			camGo();
		} else if (str.equals("down")) {
			camservodirection = -1;
			camGo();
		} else if (str.equals("horiz")) {
			camHoriz();
		} else if (str.equals("downabit")) {
			camservopos -= 5;
			if (camservopos < camposmin) {
				camservopos = camposmin;
			}
			new Thread(new Runnable() {
				public void run() {
					sendCommand(new byte[] { CAM, (byte) camservopos });
					checkForHoldServo();
				}
			}).start();
		} else if (str.equals("upabit")) {
			camservopos += 5;
			if (camservopos > camposmax) {
				camservopos = camposmax;
			}
			new Thread(new Runnable() {
				public void run() {
					sendCommand(new byte[] { CAM, (byte) camservopos });
					checkForHoldServo();
				}
			}).start();
		}
		// else if (str.equals("hold")) {
		// new Thread(new Runnable() { public void run() {
		// sendCommand(new byte[] { CAM, (byte) camservopos });
		// } }).start();
		// }
	}

	@Override
	public void camHoriz() {
		camservopos = camservohoriz;
		new Thread(new Runnable() {
			public void run() {
				try {
					byte[] cam = { CAM, (byte) camservopos };
					sendCommand(cam);
					checkForHoldServo();

				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
	}

	@Override
	public void camToPos(Integer n) {
		camservopos = n;
		new Thread(new Runnable() {
			public void run() {
				try {
					sendCommand(new byte[] { CAM, (byte) camservopos });
					checkForHoldServo();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
	}

	@Override
	public void speedset(String str) {
		if (str.equals("slow")) {
			state.set(State.values.speed, speedslow);
		}
		if (str.equals("med")) {
			state.set(State.values.speed, speedmed);
		}
		if (str.equals("fast")) {
			state.set(State.values.speed, speedfast);
		}
		if (state.getBoolean(State.values.movingforward)) {
			goForward();
		}
	}

	@Override
	public void nudge(String dir) {
		state.set(DIRECTION, dir);
		new Thread(new Runnable() {
			public void run() {
				int n = nudgedelay;
				if (state.equals(DIRECTION, "right")) {
					turnRight();
				}
				if (state.equals(DIRECTION, "left")) {
					turnLeft();
				}
				if (state.equals(DIRECTION, "forward")) {
					goForward();
					state.set(State.values.movingforward, false);
					n *= 4;
				}
				if (state.equals(DIRECTION, "backward")) {
					goBackward();
					n *= 4;
				}

				Util.delay(n);

				if (state.getBoolean(State.values.movingforward)) {
					goForward();
				} else {
					stopGoing();
				}
			}
		}).start();
	}

	@Override
	public void slide(String dir) {
		if (!state.getBoolean(State.values.sliding)) {
			state.set(State.values.sliding, true);
			state.set(DIRECTION, dir);
			tempspeed = 999;
			new Thread(new Runnable() {
				public void run() {
					try {
						int distance = 300;
						int turntime = 500;
						tempspeed = state.getInteger(State.values.speed);
						state.set(State.values.speed, speedfast);
						if (state.equals(DIRECTION, "right")) {
							turnLeft();
						} else {
							turnRight();
						}
						Thread.sleep(turntime);
						if (state.getBoolean(State.values.sliding)) {
							goBackward();
							Thread.sleep(distance);
							if (state.getBoolean(State.values.sliding)) {
								if (state.equals(DIRECTION, "right")) {
									turnRight();
								} else {
									turnLeft();
								}
								Thread.sleep(turntime);
								if (state.getBoolean(State.values.sliding)) {
									goForward();
									Thread.sleep(distance);
									if (state.getBoolean(State.values.sliding)) {
										stopGoing();
										state.set(State.values.sliding, false);
										state.set(State.values.speed, tempspeed);
									}
								}
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}).start();
		}
	}

	@Override
	public void slidecancel() {
		if (state.getBoolean(State.values.sliding)) {
			if (tempspeed != 999) {
				state.set(State.values.speed, tempspeed);
				state.set(State.values.sliding, false);
			}
		}
	}

	@Override
	public Integer clickSteer(String str) {
		tempstring = str;
		tempint = 999;
		String xy[] = tempstring.split(" ");
		if (Integer.parseInt(xy[1]) != 0) {
			tempint = clickCam(Integer.parseInt(xy[1]));
		}
		new Thread(new Runnable() {
			public void run() {
				try {
					String xy[] = tempstring.split(" ");
					if (Integer.parseInt(xy[0]) != 0) {
						if (Integer.parseInt(xy[1]) != 0) {
							Thread.sleep(camwait);
						}
						clickNudge(Integer.parseInt(xy[0]));
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
		return tempint;
	}

	@Override
	public void clickNudge(Integer x) {
		if (x > 0) {
			state.set(DIRECTION, "right");
		} else {
			state.set(DIRECTION, "left");
		}
		clicknudgedelay = maxclicknudgedelay * (Math.abs(x)) / 320;
		/*
		 * multiply clicknudgedelay by multiplier multiplier increases to
		 * CONST(eg 2) as x approaches 0, 1 as approaches 320
		 * ((320-Math.abs(x))/320)*1+1
		 */
		double mult = Math.pow(((320.0 - (Math.abs(x))) / 320.0), 3)
				* clicknudgemomentummult + 1.0;
		// System.out.println("clicknudgedelay-before: "+clicknudgedelay);
		clicknudgedelay = (int) (clicknudgedelay * mult);
		// System.out.println("n: "+clicknudgemomentummult+" mult: "+mult+" clicknudgedelay-after: "+clicknudgedelay);
		new Thread(new Runnable() {
			public void run() {
				try {
					tempspeed = state.getInteger(State.values.speed);
					state.set(State.values.speed, speedfast);
//					if (state.equals(state.get(DIRECTION), "right")) {
					if (state.get(DIRECTION).equals("right")) {
						turnRight();
					} else {
						turnLeft();
					}
					Thread.sleep(clicknudgedelay);
					state.set(State.values.speed, tempspeed);
					if (state.getBoolean(State.values.movingforward)) {
						goForward();
					} else {
						stopGoing();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
	}

	@Override
	public Integer clickCam(Integer y) {
		Integer n = maxclickcam * y / 240;
		camservopos -= n;
		if (camservopos > camposmax) {
			camservopos = camposmax;
		}
		if (camservopos < camposmin) {
			camservopos = camposmin;
		}

		new Thread(new Runnable() {
			public void run() {
				try {
					byte[] command = { CAM, (byte) camservopos };
					sendCommand(command);
					checkForHoldServo();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
		return camservopos;
	}

	@Override
	public void releaseCameraServo() {
		new Thread(new Runnable() {
			public void run() {
				try {
					sendCommand(CAMRELEASE);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
	}

	public void checkForHoldServo() {
		String stream = state.get(State.values.stream);
		if (stream == null) return;

		if (!holdservo || stream.equals("stop") || stream.equals("mic") ||
				stream == null) {
			Util.delay(camwait);
			sendCommand(CAMRELEASE);
		}
		
		state.set(State.values.camservopos, camservopos);
	}

	@Override
	public void updateSteeringComp() {
		byte[] command = { COMP, (byte) steeringcomp };
		new Sender(command);
	}

	/** */
	public void digitalRead(String pin) {
		int line = Integer.parseInt(pin);
		byte[] command = { DIGITALREAD, (byte) line};
		new Sender(command);
	}
	
	/** */
	public void AnalogWrite(String str) {
		String n[] = str.split(" "); 
		byte[] command = { ANALOGWRITE, (byte) Integer.parseInt(n[0]), (byte) Integer.parseInt(n[1])};
		new Sender(command);	
	}

}