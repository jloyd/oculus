package developer.swingtool;

import static org.junit.Assert.fail;

import java.io.*;
import java.net.*;

import oculus.OptionalSettings;
import oculus.Settings;

public class Client {

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
	public static void main(String args[]) throws IOException {

		Settings settings = new Settings();
		if (Settings.settingsfile != null)
			if (Settings.settingsfile.contains("null"))
				fail("no settings file found");

		// login on connect
		String user = settings.readSetting("user0");
		String pass = settings.readSetting("pass0");
		
		// over ride
		// System.out.println("args: " + args.length);
		if(args.length==4) user = args[2];
		if(args.length==4) pass = args[3];
		
		new Client(args[0], settings.getInteger(OptionalSettings.commandport), user, pass);
	}
}