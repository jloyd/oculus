package developer;

import java.io.*;
import java.net.*;
import java.util.Vector;

import org.jasypt.util.password.ConfigurablePasswordEncryptor;

import oculus.Application;
import oculus.GUISettings;
import oculus.LoginRecords;
import oculus.Observer;
import oculus.ManualSettings;
import oculus.PlayerCommands;
import oculus.Settings;
import oculus.Updater;
import oculus.Util;
import oculus.commport.Discovery;

/**
 * Start the terminal server. Start new threads for a each connection. 
 */
public class CommandServer implements Observer {
	
	public static final String SEPERATOR = " : ";
	
	private static Vector<PrintWriter> printers = new Vector<PrintWriter>();
	private static oculus.State state = oculus.State.getReference();
	private static LoginRecords records = new LoginRecords();
	private static oculus.Settings settings = new Settings();
	private static ServerSocket serverSocket = null;  	
	private static Application app = null;
	
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
			
			try {
				
				// first thing better be user:pass
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
				Util.log("command server connection fail: " + ex.getMessage(), this);
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
			if(settings.getBoolean(GUISettings.loginnotify)) Util.beep();
			sendToGroup(printers.size() + " tcp connections active");
			
			// loop on input from the client
			int i = 0;
			String str = null;
			while (true) {
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
					
					Util.debug(clientSocket.getInetAddress().toString() + "input: " + str, this);					
					out.println("[" + i++ + "] echo: "+str);
					
					// try extra commands first 
					if( ! manageCommand(str)){
						try {
							doPlayer(str);
						} catch (Exception e) {
							Util.debug("player err: " + e.getLocalizedMessage(), this);
						}
					}
				}
			}
		
			// close up, must have a closed socket  
			shutDown();
		}

		/**
		 * @param str a give command string with one or many words 
		 */
		public void doPlayer(final String str){
			
			String[] cmd = str.trim().split(" ");
			Util.debug("doplayer("+str+") split: " + cmd.length, this);	
			
			if(cmd.length==1) {
				if( ! PlayerCommands.requiresArgument(cmd[0]))
					app.playerCallServer(str, null);	
				
			} else if(cmd.length>=2) {	
				// collect all arguments
				String args = new String(); 		
				for(int i = 1 ; i < cmd.length ; i++) 
					args += " " + cmd[i].trim();
				
				// now send it 
				app.playerCallServer(cmd[0], args);
			}
		}
		
		// close resources
		private void shutDown() {

			// log to console, and notify other users of leaving
			Util.log("closing socket [" + clientSocket + "]", this);
			sendToGroup(printers.size() + " tcp connections active");
			state.delete(oculus.State.override);		
			
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
		
		/** add extra commands, macros here. Return true if the command was found */ 
		public boolean manageCommand(final String str){
			
			final String[] cmd = str.split(" ");
			
			// send HTML format 
			if(str.startsWith("chat")){
				String args = new String(); 		
				for(int i = 1 ; i < cmd.length ; i++) args += " " + cmd[i].trim();
				if(args.length()>1)
					app.playerCallServer(PlayerCommands.chat, 
							"<u><i>" + user.toUpperCase() + "</i></u>:" + args);
				return true;
			}
			
			if(str.startsWith("help")) {
				for (PlayerCommands factory : PlayerCommands.values()) 
					out.println(factory.toString());
				return true;
			}
			
			if(str.startsWith("tail")) {
				int lines = 30; // default if not set 
				if(cmd.length==2) lines = Integer.parseInt(cmd[1]);
				String log = Util.tail(new File(oculus.Settings.stdout), lines);
				if(log!=null)
					if(log.length() > 1)
						out.println(log);
				return true;
			}
			
			if(str.startsWith("reboot")) {
				Util.systemCall("shutdown -r -f -t 01");			
				return true;
			}
					
			//TODO: TEST 
			if(str.startsWith("home")){ 
				Util.systemCall("java -classpath \"./webapps/oculus/WEB-INF/classes/\" developer.terminal.FindHome " 
						+ state.get(oculus.State.localaddress) + " " + serverSocket.getLocalPort() +  " " + user + " " + pass); 
				return true;
			}
			
			//TODO: TEST 
			if(str.startsWith("script")){ 
				Util.systemCall("java -classpath \"./webapps/oculus/WEB-INF/classes/\" developer.terminal.ScriptServer " 
						+ state.get(oculus.State.localaddress) + " " + serverSocket.getLocalPort() + " " + user + " " + pass + " " + cmd[1]); 
				return true;
			}
			
			if(str.startsWith("restart")){ app.restart(); return true;}
		
			if(str.startsWith("softwareupdate")) { app.softwareUpdate("update"); return true; }
			
			if(str.startsWith("image")) { app.frameGrab(); return true; }
			
			if(str.startsWith("cam")){ app.publish("camera"); return true; }
			
			if(str.startsWith("memory")) {		
				out.println("memory : " +
						((double)Runtime.getRuntime().freeMemory()
								/ (double)Runtime.getRuntime().totalMemory()) + " %");
				
				out.println("memorytotal : "+Runtime.getRuntime().totalMemory());    
			    out.println("memoryfree : "+Runtime.getRuntime().freeMemory());
			}
			
			if(str.startsWith("bye")) { out.print("bye"); shutDown(); }
			
			if(str.startsWith("quit")) { out.print("bye"); shutDown(); }
						
			if(str.startsWith("find")) {	
				if(state.get(PlayerCommands.publish.toString()) == null) {
					out.println("error: camera is off");
					return true;
				} else {
					if(state.get(PlayerCommands.publish.toString()).equals("stop")){
						out.println("error: camera is off");
						return true;
					}
				}
				
				if(state.getBoolean(oculus.State.dockgrabbusy)){
					out.println("error: dock grab is busy");
					return true;
				} else {
					
					// take a new reading, send back result if success
					state.set(oculus.State.dockgrabbusy, true);
					new Thread(new Runnable() {
						@Override
						public void run() {
							long start = System.currentTimeMillis(); 
							app.dockGrab();
							if( ! state.block(oculus.State.dockgrabbusy, "false", 45000))
								Util.log("timed out waiting on dock grab ", this);
					
				
							// put results in state for any that care 
							state.set(oculus.State.dockgrabtime, (System.currentTimeMillis() - start));
						}
					}).start();
					return true;
				 }
			}
			

			if(str.startsWith("beep")) {
				Util.beep(); 
				return true;
			}
						
			if(str.startsWith("tcp")) {
				out.println("tcp connections : " + printers.size());
				return true;
			}
	
			if(str.startsWith("users")){
				out.println("active users : " + records.getActive());
				if(records.toString()!=null) out.println(records.toString());
				return true;
			}

			if(str.startsWith("state")) {
				if(cmd.length==3) state.set(cmd[1], cmd[2]);
				else out.println(state.toString());
				return true;
			}		
			
			if(str.startsWith("settings")){
				if(cmd.length==3) { 
					if(settings.readSetting(cmd[1]) == null) settings.newSetting(cmd[1], cmd[2]);
					else settings.writeSettings(cmd[1], cmd[2]);
				
					// clean file afterwards 
					settings.writeFile();
					return true;
					
				} else{
					out.println(settings.toString());
					return true;
				}
			}	
			
			// command not found 
			return false;
		}
	}
	
	@Override
	/** send to socket on state change */ 
	public void updated(String key) {
		String value = state.get(key);
		if(value==null)	sendToGroup("state deleted: " + key); 
		else sendToGroup("state updated: " + key + SEPERATOR + value); 
	}
	
	/** send input back to all the clients currently connected */
	public void sendToGroup(final String str) {
		
		// ignore this message
		if(str.startsWith("message: status check")) return;
		
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
				//while(true) {
					go();
				//}
			}
		}).start();
	}
	
	/** do forever */ 
	public void go(){
		
		// disabled configuration 
		if(settings.readSetting(ManualSettings.commandport).equals(Discovery.params.disabled)) return; 
		
		Integer port = settings.getInteger(ManualSettings.commandport.toString());
		if(port==Settings.ERROR) return; 
		
		try {
			serverSocket = new ServerSocket(port);
		} catch (Exception e) {
			Util.log("server sock error: " + e.getMessage(), this);
			return;
		} 
		
		Util.debug("listening with socket: " + serverSocket.toString(), this);
		
		// serve new connections until killed
		while (true) {
			try {

				// new user has connected
				new ConnectionHandler(serverSocket.accept());

			} catch (Exception e) {
				try {				
					serverSocket.close();
				} catch (IOException e1) {
					Util.log("socket error: " + e1.getMessage());
					return;					
				}	
				
				Util.log("failed to open client socket: " + e.getMessage(), this);
			}
		}
	}
}

