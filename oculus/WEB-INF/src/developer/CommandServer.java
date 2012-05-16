package developer;

import java.io.*;
import java.net.*;
import java.util.Vector;

import org.jasypt.util.password.ConfigurablePasswordEncryptor;

import oculus.Application;
import oculus.BatteryLife;
import oculus.LoginRecords;
import oculus.Observer;
import oculus.OptionalSettings;
import oculus.PlayerCommands;
import oculus.Settings;
import oculus.State;
import oculus.Updater;
import oculus.Util;

/**
 * Start the chat server. Start new threads for a each connection on the given port
 */
public class CommandServer implements Observer {
	
	public static final String SEPERATOR = " : ";
	
	private static ServerSocket serverSocket = null; 
	private static Application app = null;

	private static Vector<PrintWriter> printers = new Vector<PrintWriter>();
	private static BatteryLife battery = BatteryLife.getReference(); 
	private static oculus.State state = oculus.State.getReference();
	private static LoginRecords records = new LoginRecords();
	private static oculus.Settings settings = new Settings(); 	
	
	
	/** Threaded client handler */
	class ConnectionHandler extends Thread {
	
		private Socket clientSocket = null;
		private BufferedReader in = null;
		private PrintWriter out = null;
		private String user, pass;
		
		public ConnectionHandler(Socket socket) {
		
			clientSocket = socket;
			
			try {
			
				in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
				out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream())), true);
			
			} catch (IOException e) {				
				Util.log("fail aquire tcp streams: " + e.getMessage(), this);
				shutDown();
				return;
			}
	
			// send banner 
			out.println("oculus version " + new Updater().getCurrentVersion() + " ready for login."); 

			// first thing better be user:pass
			try {
				
				final String inputstr = in.readLine();
				user = inputstr.substring(0, inputstr.indexOf(':')).trim();
				pass = inputstr.substring(inputstr.indexOf(':')+1, inputstr.length()).trim();
								
				if(app.logintest(user, pass)==null){
					
				    ConfigurablePasswordEncryptor passwordEncryptor = new ConfigurablePasswordEncryptor();
					passwordEncryptor.setAlgorithm("SHA-1");
					passwordEncryptor.setPlainDigest(true);
					String encryptedPassword = (passwordEncryptor
							.encryptPassword(user + settings.readSetting("salt") + pass)).trim();
					
					if(app.logintest(user, encryptedPassword)==null){
						out.println("login failure, please drop dead");
						shutDown();
					}
				}
			} catch (Exception ex) {
				System.out.println("OCULUS: command server connection fail: " + ex.getMessage());
				shutDown();
			}
	
			// keep track of all other user sockets output streams			
			printers.add(out);	
			this.start();
		}

		/** do the client thread */
		@Override
		public void run() {
			
			state.set(oculus.State.override, true);
		
			Util.beep();
			
			sendToGroup(printers.size() + " tcp connections active");
			
			// loop on input from the client
			int i = 0;
			while (true) {

				// been closed ?
				if(out!=null) if(out.checkError()) break;			
				
				// blocking read from the client stream up to a '\n'
				String str = null;
				try {
					str = in.readLine();
				} catch (Exception e) {
					Util.log("readLine(): " + e.getMessage(), this);
					break;
				}

				// client is terminating?
				if (str == null) {
					Util.debug("read thread, closing.", this);
					break;
				}
						
				// parse and run it 
				str = str.trim();
				if(str.length()>2){
					
					Util.debug(" address [" + clientSocket + "] message [" + str + "]", this);
					
					out.println("[" + i++ + "] echo: "+str);
					
					// do both for now 
					manageCommand(str);
					
					// TODO: COLIN  
					try {
						doPlayer(str);
					} catch (Exception e) {
						// e.printStackTrace(System.err);
						// Util.debug("player err: " + e.getLocalizedMessage(), this);
					}
				}
			}
		
			// 
			shutDown();
		}

		/**
		 * @param str a give command string with one or many words 
		 */
		public void doPlayer(final String str){
			
			Util.log("doplayer("+str+")", this);	
			
			String[] cmd = str.trim().split(" ");
			if(cmd.length==1) app.playerCallServer(str, null);		
			if(cmd.length==2) app.playerCallServer(cmd[0], cmd[1]);
		}
		
		// close resources
		private void shutDown() {

			// log to console, and notify other users of leaving
			Util.log("closing socket [" + clientSocket + "]", this);
			sendToGroup(printers.size() + " tcp connections active");
			
			try {

				// close resources
				printers.remove(out);
				if(in!=null) in.close();
				if(out!=null) out.close();
				if(clientSocket!=null) clientSocket.close();
			
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		/** add extra commands, macros here */ 
		public void manageCommand(final String str){
			final String[] cmd = str.split(" ");
			
			/*
			if(cmd[0].equals("messages")) {
				for(int i = 0; i < app.userMessages.size() ;i++){
					out.println(app.userMessages.get(i));
				}
			}*/
			
			if(cmd[0].equals("help")) {
				for (PlayerCommands factory : PlayerCommands.values()) 
					out.println(factory.toString());
			}
			
			if(cmd[0].equals("tail")) {
				int lines = 30; // default if not set 
				if(cmd.length==2) lines = Integer.parseInt(cmd[1]);
				String log = Util.tail(new File(oculus.Settings.stdout), lines);
				
				if(log!=null)
					if(log.length() > 1)
						out.println(log);
			}
			
			if(cmd[0].equals("reboot")) Util.systemCall("shutdown -r -f -t 01");				
					
			//TODO: TEST 
			if(cmd[0].equals("home")) 
				Util.systemCall("java -classpath \"./webapps/oculus/WEB-INF/classes/\" developer.terminal.FindHome " 
						+ state.get(oculus.State.localaddress) + " " + serverSocket.getLocalPort() +  " " + user + " " + pass); 
	
			//TODO: TEST 
			if(cmd[0].equals("script")) 
				Util.systemCall("java -classpath \"./webapps/oculus/WEB-INF/classes/\" developer.terminal.ScriptServer " 
						+ state.get(oculus.State.localaddress) + " " + serverSocket.getLocalPort() + " " + user + " " + pass + " " + cmd[1]); 
			
			if(cmd[0].equals("restart")) app.restart(); 
		
			if(cmd[0].equals("softwareupdate")) app.softwareUpdate("update"); 
			
			if(cmd[0].equals("image")) { app.frameGrab(); }
			
			/*
			if(cmd[0].equals("move")){
				// System.out.println("move.....");
				if(cmd[1].equals("forward")) port.goForward();
				else if(cmd[1].equals("backward")) port.goBackward();
				else if(cmd[1].equals("left")) port.turnLeft();
				else if(cmd[1].equals("right")) port.turnRight();
			}
			
			if(cmd[0].equals("nudge")) port.nudge(cmd[1]);
			
			if(cmd[0].equals("publish")) app.publish(cmd[1]); 
			*/
			
			if(cmd[0].equals("cam")){ app.publish("camera"); }
			
			if(cmd[0].equals("memory")) {		
				out.println("memory : " +
						((double)Runtime.getRuntime().freeMemory()
								/ (double)Runtime.getRuntime().totalMemory()) + " %");
				
				out.println("memorytotal : "+Runtime.getRuntime().totalMemory());    
			    out.println("memoryfree : "+Runtime.getRuntime().freeMemory());
			}
			
			if(cmd[0].equals("bye")) { out.print("bye"); shutDown(); }
						
			if(cmd[0].equals("find")) {
				
				if(state.get(PlayerCommands.publish.toString()) == null) {
				
					out.println(".. the camera is off, retard.");
					return;
					
				} else {
					if(state.get(PlayerCommands.publish.toString()).equals("stop")){
						out.println(".. the camera is off, retard.");
						return;
					}
				}
				
				if(state.getBoolean(oculus.State.dockgrabbusy)){
					out.println("calling _find_ too often.");
					Util.log("calling _find_ too often.", this);
					return;
					
				} else {
					
					state.set(oculus.State.dockgrabbusy, true);
					
					new Thread(new Runnable() {
						
						@Override
						public void run() {
														
							// System.out.println("wait for grab to end... ");
							long start = System.currentTimeMillis();

							// are the same thing 
							app.dockGrab();
							if( ! state.block(oculus.State.dockgrabbusy, "false", 45000))
								Util.log("timed out waiting on dock grab ", this);
							
							
							// put results in state for any that care 
							state.set(oculus.State.dockgrabtime, (System.currentTimeMillis() - start));
							
						}
					}).start();
				 }
			}
			
			if(cmd[0].equals("battery")) battery.battStats();
								
			//if(cmd[0].equals("dock") && docker!=null) docker.autoDock("go");
			
			// if(cmd[0].equals("undock") && docker!=null) docker.dock("undock");
									
			/// if(cmd[0].equals("stop")) port.stopGoing();
				
			if(cmd[0].equals("beep")) Util.beep();
			
			if(cmd[0].equals("dump")) state.dump();
			
			//if(cmd[0].equals("email")) new SendMail("image", "body", Settings.framefile);
			
			if(cmd[0].equals("tcp")) out.println("tcp connections : " + printers.size());
	
			if(cmd[0].equals("users")){
				out.println("active users : " + records.getActive());
				if(records.toString()!=null) out.println(records.toString());
			}

			if(cmd[0].equals("state")) {
				if(cmd.length==3) state.set(cmd[1], cmd[2]);
				else out.println(state.toString());
			}		
			
			if(cmd[0].equals("settings")){
				if(cmd.length==3) { 
				
					// System.out.println(".. write setting: " + str);
					
					if(settings.readSetting(cmd[1]) == null) {
						settings.newSetting(cmd[1], cmd[2]);
					} else {
						settings.writeSettings(cmd[1], cmd[2]);
					}
					
					// clean file afterwards 
					settings.writeFile();
					
				} else if(cmd.length==2) {
					
					out.println(settings.readSetting(cmd[1])); 

					System.out.println("setting value = " + settings.readSetting(cmd[1])); 
					
				} else out.println(settings.toString());
			}	
		}
	}
	
	@Override
	/** send to socket on state change */ 
	public void updated(String key) {
		String value = state.get(key);
		if(value==null) {
			sendToGroup("state deleted: " + key + SEPERATOR + value); 
		}
		else {
			sendToGroup("state updated: " + key + SEPERATOR + value); 
		}
	}
	
	/** send input back to all the clients currently connected */
	public void sendToGroup(String str) {
		PrintWriter pw = null;
		for (int c = 0; c < printers.size(); c++) {
			pw = printers.get(c);
			if (pw.checkError()) {	
				printers.remove(pw);
				pw.close();
			} else pw.println(str);
		}
	}
	
	/** */
	public CommandServer(oculus.Application a) {
		
		if(app == null) app = a;
		else return;
		
		/** register for updates, share state with all threads */  
		state.addObserver(this);
		
		/** register shutdown hook */ 
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					
					if(serverSocket!=null)
						serverSocket.close();
					
					if(printers!=null) 
						printers.clear();
					
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}));
		
		// do long time
		new Thread(new Runnable() {
			@Override
			public void run() {
				int i = 0;
				while(true) {
					Util.debug("opening connection again: " + i++, this);
					go();
				}
			}
		}).start();
	}
	
	/** do forever */ 
	public void go(){
		
		Integer port = settings.getInteger(OptionalSettings.commandport.toString());
		if(port==Settings.ERROR) port = 4444; // default on error 
		
		try {
			serverSocket = new ServerSocket(port);
		} catch (Exception e) {
			Util.log("server sock error: " + e.getMessage(), this);
			return;
		} 
		
		Util.debug("listening with socket [" + serverSocket + "] " + serverSocket.toString(), this);
		
		// serve new connections until killed
		while (true) {
			try {

				// new user has connected
				new ConnectionHandler(serverSocket.accept());

			} catch (Exception e) {
				try {				
					serverSocket.close();
				} catch (IOException e1) {
					System.out.println("OCULUS: failed to open client socket: " + e1.getMessage());
					return;					
				}	
				
				Util.log("failed to open client socket: " + e.getMessage(), this);
			}
		}
	}
}

