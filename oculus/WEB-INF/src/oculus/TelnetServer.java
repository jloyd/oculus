package oculus;

import java.io.*;
import java.net.*;
import java.util.Vector;

import oculus.PlayerCommands.RequiresArguments;

import org.jasypt.util.password.ConfigurablePasswordEncryptor;


/**
 * Start the terminal server. Start a new thread for a each connection. 
 */
public class TelnetServer implements Observer {
	
	//TODO: add junit test to check that all commands below are PlayerCommands duplicated
	// OR just move these all to playercommands?
	public static enum Commands {chat, bye, quit};
	public static final boolean ADMIN_ONLY = true;
//	public static final int MIN_LENGTH = 1; //TODO: why 2? Why not 1?
	public static final String MSGPLAYERTAG = "<messageclient>";
	public static final String MSGGRABBERTAG = "<messageserverhtml>";
	public static final String TELNETTAG = "<telnet>";
	public static final String STATETAG = "<state>";		
	public static Vector<PrintWriter> printers = new Vector<PrintWriter>();
	
	private static oculus.State state = oculus.State.getReference();
//	private static LoginRecords records = new LoginRecords();
	private static oculus.Settings settings =Settings.getReference();
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
				shutDown("fail aquire tcp streams: " + e.getMessage());
				return;
			}
	
			// send banner 
			sendToSocket("Welcome to Oculus version " + new Updater().getCurrentVersion()); 
			sendToSocket("LOGIN with admin user:password OR user:encrypted_password");
			
			try {
				
				// first thing better be user:pass
				final String inputstr = in.readLine();
				if(inputstr.indexOf(':')<=0) shutDown("login failure");
				user = inputstr.substring(0, inputstr.indexOf(':')).trim();
				pass = inputstr.substring(inputstr.indexOf(':')+1, inputstr.length()).trim();
								
				// Admin only 
				if(ADMIN_ONLY) if( ! user.equals(settings.readSetting("user0"))) shutDown("must be admin user for telnet");
							
				// try salted 
				if(app.logintest(user, pass)==null){
					
				    ConfigurablePasswordEncryptor passwordEncryptor = new ConfigurablePasswordEncryptor();
					passwordEncryptor.setAlgorithm("SHA-1");
					passwordEncryptor.setPlainDigest(true);
					String encryptedPassword = (passwordEncryptor
							.encryptPassword(user + settings.readSetting("salt") + pass)).trim();
					
					// try plain text 
					if(app.logintest(user, encryptedPassword)==null)
						shutDown("login failure: " + user);
			
				}
			} catch (Exception ex) {
				shutDown("command server connection fail: " + ex.getMessage());
			}
	
//			state.set(oculus.State.values.user, user);
			// new LoginRecords().beDriver();
			
			// keep track of all other user sockets output streams			
			printers.add(out);	
			sendToSocket(user + " connected via socket");
			Util.log(user+" connected via socket", this);
			this.start();
		}

		/** do the client thread */
		@Override
		public void run() {
			
//			if(state.get(oculus.State.values.user.name())==null) state.set(oculus.State.values.user.name(), user);
			if(settings.getBoolean(GUISettings.loginnotify)) app.saySpeech("lawg inn telnet");
			sendToGroup(TELNETTAG+" "+printers.size() + " tcp connections active");
			
			// loop on input from the client
			String str = null;
			while (true) {
				try {
					str = in.readLine();
				} catch (Exception e) {
					Util.debug("readLine(): " + e.getMessage(), this);
					break;
				}

				// client is terminating?
				if (str == null) {
					Util.debug("read thread, closing.", this);
					break;
				}
						
				// parse and run it 
				str = str.trim();
				if(str.length()>=1){
					
					Util.debug("socket user '"+user+"' sending from "+clientSocket.getInetAddress().toString() + " : " + str, this);	
					if( ! manageCommand(str)) {			
						
						Util.debug("doPlayer(" + str + ")", this);	
						doPlayer(str);
						
					}
				}
			}
		
			// close up, must have a closed socket  
			shutDown("user disconnected");
		}

		/**
		 * @param str is a multi word string of commands to pass to Application. 
		 */
		private void doPlayer(final String str){
			
			final String[] cmd = str.split(" ");
			String args = new String(); 			
			for(int i = 1 ; i < cmd.length ; i++) args += " " + cmd[i].trim();
			
			PlayerCommands player = null; 
			try { // create command from input 
				player = PlayerCommands.valueOf(cmd[0]);
			} catch (Exception e) {
				sendToSocket("error: unknown command, " + cmd[0]);
				return;
			}
			
			// test if needs an argument, but is missing. 
			if(player.requiresArgument()){
				
				RequiresArguments req = PlayerCommands.RequiresArguments.valueOf(cmd[0]);
			
				if(cmd.length==1){
					sendToSocket("error: this command requires arguments " + req.getArguments());
					return;
				}
			
				if(req.getValues().size() > 1){
					if( ! req.matchesArgument(cmd[1])){
						sendToSocket("error: this command requires arguments " + req.getArguments());
						return;
					}
				}
					
				if(req.usesBoolean()){
					if( ! PlayerCommands.validBoolean(cmd[1])){
						sendToSocket("error: requires {BOOLEAN}");
						return;
					}	
				}
				
				if(req.usesInt()){
					if( ! PlayerCommands.validInt(cmd[1])){
						sendToSocket("error: requires {INT}");
						return;
					}
				}
				
				if(req.usesDouble()){
					if( ! PlayerCommands.validDouble(cmd[1])){
						sendToSocket("error: requires {DOUBLE}");
						return;
					}
				}
	
				if(req.requiresParse()){
					
					// do min test, check for the same number of arguments 
					String[] list = req.getArgumentList()[0].split(" ");
					if(list.length != (cmd.length-1)){
						sendToSocket("error: wrong number args, requires [" + list.length + "]");
						return;
					}		
				}
			}
		
			// check for null vs string("")
			args = args.trim();
			if(args.length()==0) args = "";
			
			// now send it, assign driver status 1st 
			app.passengerOverride = true;	
			app.playerCallServer(player, args);
			app.passengerOverride = false;		
		}
		
		// close resources
		private void shutDown(final String reason) {

			// log to console, and notify other users of leaving
			sendToSocket("shutting down "+reason);
			Util.debug("closing socket [" + clientSocket + "] " + reason, this);
			sendToGroup(TELNETTAG+" "+printers.size() + " tcp connections active");
			
			try {

				// close resources
				printers.remove(out);
				if(in!=null) in.close();
				if(out!=null) out.close();
				if(clientSocket!=null) clientSocket.close();
			
			} catch (Exception e) {
				Util.log("shutdown: " + e.getMessage(), this);
			}
		}
		
		/** add extra commands, macros here. Return true if the command was found */ 
		private boolean manageCommand(final String str){
			
			final String[] cmd = str.split(" ");
			Commands telnet = null;
			try {
				telnet = Commands.valueOf(cmd[0]);
			} catch (Exception e) {
				return false;
			}
			
			switch (telnet) {
			
			case chat: // overrides playercommands chat
				String args = new String(); 		
				for(int i = 1 ; i < cmd.length ; i++) args += " " + cmd[i].trim();
				if(args.length()>1)
					app.playerCallServer(PlayerCommands.chat, 
							"<i>" + user.toUpperCase() + "</i>:" + args);
				return true;
					
//			case tcp: 
//				sendToSocket("tcp connections : " + printers.size());
//				return true;
//				
//			case users: 
//				sendToSocket("active users : " + records.getActive());
//				if(records.toString()!=null) sendToSocket(records.toString());
//				return true;
				
//			case state:
//				if(cmd.length==3) state.set(cmd[1], cmd[2]);
//				else {
//					sendToSocket(state.toString());
//					// state.dump();
//				}
//				return true;

//			case settings: 
//				if(cmd.length==3) { 
//					if(settings.readSetting(cmd[1]) == null) settings.newSetting(cmd[1], cmd[2]);
//					else settings.writeSettings(cmd[1], cmd[2]);
//				
//					// clean file afterwards 
//					settings.writeFile();
//					return true;
//					
//				} else{
//					sendToSocket(settings.toString());
//					return true;
//				}
			
			case bye: 
			case quit: shutDown("user quit"); return true;
			}
			
			// command was not managed 
			return false;	
		}
		
		private void sendToSocket(String str) {
			Boolean multiline = false;
			if (str.matches(".*<br>.*")) { 
				multiline = true;
				str = (str.replaceAll("<br>", "\r\n")).trim();
			}
			if (multiline) { out.print("<multiline> "); }
			out.println("<telnet> " + str);
			if (multiline) { out.println("</multiline>"); }
		}
		
	} // end inner class
	
	@Override
	/** send to socket on state change */ 
	public void updated(String key) {
		String value = state.get(key);
		if(value==null)	sendToGroup(STATETAG + " deleted: " + key); 
		else sendToGroup(STATETAG + " " + key + " " + value); 
	}
	
	/** send input back to all the clients currently connected */
	public void sendToGroup(String str) {
		Boolean multiline = false;
		if (str.contains("<br>")) {
			multiline = true;
			str = (str.replaceAll("<br>", "\r\n")).trim();
		}
		PrintWriter pw = null;
		for (int c = 0; c < printers.size(); c++) {
			pw = printers.get(c);
			if (pw.checkError()) {	
				printers.remove(pw);
				pw.close();
			} else {
				if (multiline) { pw.print("<multiline> "); }
				pw.println(str);
				if (multiline) { pw.println("</multiline>"); }
			}
		}

	}

	/** constructor */
	public TelnetServer(oculus.Application a) {
		
		if(app == null) app = a;
		else return;
		
		/** register for updates, share state with all threads */  
		state.addObserver(this);
		
		/** register shutdown hook */ 
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					
					if(serverSocket!=null) serverSocket.close();
					
					if(printers!=null)
						for(int i = 0 ; i < printers.size() ; i++)
							printers.get(i).close();
					
				} catch (IOException e) {
					Util.debug(e.getMessage(), this);
				}
			}
		}));
		
		/** do long time */
		new Thread(new Runnable() {
			@Override
			public void run() {
				while(true) go();
			}
		}).start();
	}
	
	/** do forever */ 
	private void go(){
		
		// wait for system to startup 
		Util.delay(1000);
		
		final Integer port = settings.getInteger(ManualSettings.commandport);
		if(port < 1024) return;
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

