package oculus;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;

public class State {
	
	public enum values{driver, logintime, usercommand, developer, 
		boottime, batterylife, externaladdress, framegrabbusy,
		firmware, serialport, commwatchdog, motionenabled, motioncommand, // motors
		speed, tempdirection, moving, sliding, movingforward, camservopos, // motors
		dockgrabbusy, docking, dockstatus, dockgrabtime, autodocktimeout, autodocking, losttarget, // dock 
		dockxsize, dockysize, dockslope, dockxpos, dockypos,  // dock 
		sonarback, sonarright, sonarleft, centerpoint, // experimental
		floodlighton, lightport, spotlightbrightness, // lights
		batterystatus, localaddress,   
		streamActivityThresholdEnabled, streamActivityThreshold, videosoundmode, stream, playerstream, 
		pendinguserconnected, httpPort, muteROVonMove, volume
	};

//	public static final String SEPERATOR = " : ";

	public static final int ERROR = -1;

	/** notify these on change events */
	public Vector<Observer> observers = new Vector<Observer>();
	
	/** reference to this singleton class */
	private static State singleton = new State();

	/** properties object to hold configuration */
	private HashMap<String, String> props = new HashMap<String, String>(); 
	
	public static State getReference() {
		return singleton;
	}

	/** private constructor for this singleton class */
	private State() {
		props.put(values.boottime.name(), String.valueOf(System.currentTimeMillis()));
		props.put(values.localaddress.name(), Util.getLocalAddress());
		new Thread(new Runnable() {
			@Override
			public void run() {
				String ip = null; 
				while(ip==null){
					ip = Util.getExternalIPAddress();
					if(ip!=null)
						State.getReference().set(values.externaladdress.name(), ip);
					else Util.delay(500);
				}
			}
		}).start();
	}
	
	/** */
	public Properties getProperties(){
		return (Properties) props.clone();
	}

	/** */
	public void addObserver(Observer obs){
		observers.add(obs);
	}
	
	/** test for string equality. any nulls will return false */ 
	public boolean equals(final String a, final String b){
		String aa = get(a);
		if(aa==null) return false; 
		if(b==null) return false; 
		if(aa.equals("")) return false;
		if(b.equals("")) return false;
		
		return aa.equalsIgnoreCase(b);
	}
	
	
	/** */
//	public String dump(){
////		System.out.println("state number of listeners: " + observers.size());
////		for(int i = 0 ; i < observers.size() ; i++) 
////			System.out.println(i + " " + observers.get(i).getClass().getName() + "\n");
////		
////		try {
////			Set<String> keys = props.keySet();
////			for(Iterator<String> i = keys.iterator(); i.hasNext(); ){
////				String key = i.next();
////				System.out.println( key + "<> " + props.get(key));
////			}
////			
////		} catch (Exception e) {
////			Util.log(e.getLocalizedMessage(), this);
////		}
//		return
//	}
	
	/** */
	@Override
	public String toString(){	
		String str = "";
		Set<String> keys = props.keySet();
		for(Iterator<String> i = keys.iterator(); i.hasNext(); ){
			String key = i.next();
			str += (key + " " + props.get(key) + "<br>");
		}
		return str;
	}
	
	/** */
	public boolean block(final String member, final String target, int timeout){
		
		long start = System.currentTimeMillis();
		String current = null;
		while(true){
			
			// keep checking 
			current = get(member); 
			
			if(current!=null){
				if(target.equals("*")) return true;	
				if(target.equals(current)) return true;
				if(target.startsWith(current)) return true;
			}
				
			//
			// TODO: FIX ?? 
			//
			Util.delay(50);
			//System.out.print(".");
			if (System.currentTimeMillis()-start > timeout){ 
				//System.out.println();
				return false;
			}
		}
	}
	
	/** Put a name/value pair into the configuration */
	public synchronized void set(final String key, final String value) {
		if(key==null) return;
		if(value==null) return;
		try {
			props.put(key.trim(), value.trim());
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		for(int i = 0 ; i < observers.size() ; i++)
			observers.get(i).updated(key.trim());	
	}

	/** Put a name/value pair into the config */
	public void set(final String key, final long value) {
		set(key, Long.toString(value));
	}
	
	public String get(values key){
		return get(key.name());
	}
	
	/** */
	public synchronized String get(final String key) {

		String ans = null;
		try {

			ans = props.get(key.trim());

		} catch (Exception e) {
			System.err.println(e.getStackTrace());
			return null;
		}

		return ans;
	}


	
	
	/** */
	public synchronized boolean getBoolean(String key) {
		key = key.toLowerCase();
		boolean value = false;
		try {

			value = Boolean.parseBoolean(get(key));

		} catch (Exception e) {
			if(key.equals("yes")) return true;
			else return false;
		}

		return value;
	}

	/** */
	public int getInteger(final String key) {

		String ans = null;
		int value = ERROR;

		try {

			ans = get(key);
			value = Integer.parseInt(ans);

		} catch (Exception e) {
			return ERROR;
		}

		return value;
	}
	
	/** */
	public long getLong(final String key) {

		String ans = null;
		long value = ERROR;

		try {

			ans = get(key);
			value = Long.parseLong(ans);

		} catch (Exception e) {
			return ERROR;
		}

		return value;
	}
	
	/** @return the ms since last boot */
	public long getUpTime(){
		return System.currentTimeMillis() - getLong(values.boottime.name());
	}
	
	/** @return the ms since last user log in */
	public long getLoginSince(){
		return System.currentTimeMillis() - getLong(values.logintime.name());
	}

	/** */
	public synchronized void set(String key, boolean b) {
		if(b) set(key, "true");
		else set(key, "false");
	}
	
	/** */
	public synchronized boolean exists(String key) {
		return props.containsKey(key);
	}

	/** */ 
	public synchronized void delete(String key) {
		props.remove(key);
		for(int i = 0 ; i < observers.size() ; i++)
			observers.get(i).updated(key);	
	}

	//access to playercommands for state values defeats purpose of enum values
//	public void delete(PlayerCommands cmd) {
//		delete(cmd.name());
//	}
//
//	public void set(PlayerCommands cmd, String str) {
//		set(cmd.name(), str);
//	}
//	
//	public String get(PlayerCommands cmd){ 
//		return get(cmd.name()); 
//	}

	public void set(values key, values value) {
		set(key.name(), value.name());
	}

	public void delete(values key) {
		delete(key.name());
	}

	public int getInteger(values key) {
		return getInteger(key.name());
	}
	
	public long getLong(values key){
		return getLong(key.name());
	}
	
	public boolean getBoolean(values key){
		return getBoolean(key.name());
	}
	
	public void set(values key, long data){
		set(key.name(), data);
	}
	
	public void set(values key, String value){
		set(key.name(), value);
	}
	
	public void set(values key, boolean value){
		set(key.name(), value);
	}

}