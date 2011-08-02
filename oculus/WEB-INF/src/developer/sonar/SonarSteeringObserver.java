package developer.sonar;

import java.util.Timer;
import java.util.TimerTask;

import oculus.Application;
import oculus.ArduinoCommDC;
import oculus.Observer;
import oculus.State;

/**
 * Stop the robot before hitting the wall 
 */
public class SonarSteeringObserver implements Observer {

	// private static Logger log = Red5LoggerFactory.getLogger(SonarObserver.class, "oculus");
	private static final int TOO_CLOSE = 100; // cm 
	private State state = State.getReference();
	private java.util.Timer timer = new Timer();
	
	// private Settings settings = new Settings();
	private Application app = null;
	private ArduinoCommDC comm = null;

	/** register for state changes */
	public SonarSteeringObserver(Application a, ArduinoCommDC port) {
		app = a;
		comm = port;
		state.addObserver(this);

		// refresh on timer too
		timer.scheduleAtFixedRate(new SonarTast(), State.ONE_MINUTE, 1700);
	}

	@Override
	public void updated(final String key) {

		if (!key.equals(State.sonardistance)) return;
		if (state.getBoolean(State.autodocking)) return;
		if (!comm.movingforward) return;

		final int value = state.getInteger(key);
		System.out.println("_sonar: " + key + " = " + value);			
		if (value < TOO_CLOSE) { 
			comm.stopGoing();
			app.playerCallServer(Application.playerCommands.chat, "carefull, sonar is: " + value);

			//app.playerCallServer(Application.playerCommands.move, "stop");
		}
			
	}

	@Override
	public void removed(String key) {
		System.out.println("...sonar removed: " + key);
	}

	/**	 */
	private class SonarTast extends TimerTask {
	
		@Override 
		public void run() {
	
			comm.pollSensor();
			//System.out.println("poll sonar");
			//app.message("sonar task update to: " + state.get(State.sonardistance), null, null);   
		
		} 
	}
}