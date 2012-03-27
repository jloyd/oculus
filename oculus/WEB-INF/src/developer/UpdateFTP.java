package developer;

import java.io.IOException;

import oculus.Application;
import oculus.Observer;
import oculus.OptionalSettings;
import oculus.Settings;
import oculus.State;
import oculus.Util;

/** */
public class UpdateFTP implements Observer {

	public static final int WARN_LEVEL = 40;
	private Settings settings = new Settings();
	private State state = State.getReference();
	public static FTP ftp = new FTP();
	private Application app = null;
	private String host, port, user, pass, folder;

	/** Constructor */
	public UpdateFTP(Application parent) {
		app = parent;

		host = settings.readSetting(OptionalSettings.ftphost);
		user = settings.readSetting(OptionalSettings.ftpuser);
		pass = settings.readSetting(OptionalSettings.ftppass);
		port = "21"; // settings.readSetting(OptionalSettings.ftphost);
		folder = settings.readSetting(OptionalSettings.ftpfolder);

		// if ((host != null) && (user != null) && (port != null) && (folder !=
		// null)){
		state.addObserver(this);
		Util.debug("starting FTP alerts...", this);
		// }

		state.dump();
	}

	@Override
	public void updated(String key) {

		Util.debug("_ftp updated checking: " + key, this);
		
		/*
		new Thread(new Runnable() {
			@Override
			public void run() {
				
				try {
					
					Util.debug("ftp connecting", this);
					
					ftp.connect(host, port, user, pass);
					ftp.cwd(folder);
					ftp.storString("ip.php", ///state.get(State.externaladdress)
						 new java.util.Date().toString());
				
					ftp.disconnect();
					
				} catch (IOException e) {
					Util.debug(e.getLocalizedMessage(), this);
				}
				
			}
		}).start();
		*/

		// if (state.getInteger(State.batterylife) < WARN_LEVEL) {
/*
		new Runnable() {
			@Override
			public void run() {
				try {
					
					Util.debug("ftp connecting", this);
					
					ftp.connect(host, port, user, pass);
					ftp.cwd(folder);
					ftp.storString("ip.php", ///state.get(State.externaladdress)
						 new java.util.Date().toString());
					
				} catch (IOException e) {
					Util.debug(e.getLocalizedMessage(), this);
				}

				try {
					ftp.disconnect();
				} catch (IOException e) {
					Util.debug(e.getLocalizedMessage(), this);
				}

				Util.debug("done ftp", this);
			}
		};

		// } */
		
		
	}
}
