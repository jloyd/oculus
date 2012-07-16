package developer.swingtool;

import java.io.*;
import java.net.*;

public class Client {
	
	// force red5 path 
	oculus.Settings settings = new oculus.Settings("../../");

	public Client(String host, int port, final String usr, final String pass) throws IOException {
		try {

			// construct the client socket
			Socket s = new Socket(host, port);

			// create a useful title
			String title = usr + s.getInetAddress().toString();

			// pass socket on to read and write swing components
			Frame frame = new Frame(new Input(s, usr, pass), new Output(s), title);

			// create and show this application's GUI.
			javax.swing.SwingUtilities.invokeLater(frame);

		} catch (Exception e) {
			System.out.println(e.getMessage());
			System.exit(-1);
		}
	}

	// driver
	public static void main(String args[]) throws Exception {
		
		String user = "brad";
		String pass = "zdy";
		String ip = "127.0.0.1";
		int port = 4444;
		
		//if(args.length==0) {			
		//	settings = Settings.getReference();
		//	if (Settings.settingsfile != null)
		//		if (Settings.settingsfile.contains("null"))
		//			throw(new Exception("no settings file found"));
			
			// login info from settings
		//	user = settings.readSetting("user0");
		//	pass = settings.readSetting("pass0");
		//	port = settings.getInteger(ManualSettings.commandport); 
		//}
		
		// use params off command line 
		if(args.length==4){
			ip = args[0];
			port = Integer.parseInt(args[1]);
			user = args[2];
			pass = args[3];
		} 
		
		new Client(ip, port, user, pass);
	}
}