package developer.swingtool;

import java.io.*;
import java.net.*;

import javax.swing.*;

import oculus.PlayerCommands;
import oculus.State;
import oculus.Util;

import java.awt.event.*;

public class Input extends JTextField implements KeyListener {

	private static final long serialVersionUID = 1L;
	private Socket socket = null;
	private PrintWriter out = null;
	private String userInput = null;
	private int ptr = 0;

	public Input(Socket s, final String usr, final String pass) {
		super();
		socket = s;

		try {
			out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
					socket.getOutputStream())), true);
		} catch (Exception e) {
			Util.log("can not connect", this);
			System.exit(-1);
		}

		// if connected, login now
		out.println(usr + ":" + pass);

		// listen for key input
		addKeyListener(this);

		// send dummy messages
		new WatchDog().start();
	}

	/** inner class to check if getting responses in timely manor */
	public class WatchDog extends Thread {
		public WatchDog() {
			this.setDaemon(true);
		}

		public void run() {
			Util.delay(2000);
			while (true) {
				Util.delay(2000);
				if (out.checkError()) {
					Util.log("watchdog closing", this);
					System.exit(-1);
				}

				// send dummy
				out.println("\t\t\n");
			}
		}
	}

	// Manager user input
	public void send() {
		try {

			// get keyboard input
			userInput = getText().trim();

			// send the user input to the server if is valid
			if (userInput.length() > 0)
				out.println(userInput);

			if (out.checkError())
				System.exit(-1);

			if (userInput.equalsIgnoreCase("quit"))
				System.exit(-1);

			if (userInput.equalsIgnoreCase("bye"))
				System.exit(-1);

		} catch (Exception e) {
			System.exit(-1);
		}
	}

	@Override
	public void keyTyped(KeyEvent e) {
		final char c = e.getKeyChar();
		
		if(c == '?') {
			String str = getText();
			str = str.trim();
			for (PlayerCommands command : PlayerCommands.values()) {
				if(command.toString().startsWith(str)){
					out.println("match: " + str);	
				}
			}	
		}
		
		if (c == '\n' || c == '\r') {
			final String input = getText().trim();
			if (input.length() > 2) {
				send();
				// clear input screen
				setText("");
			}
		}
	}

	@Override
	public void keyPressed(KeyEvent e) {

		if (out == null) return;
		PlayerCommands[] cmds = PlayerCommands.values();

		if (e.getKeyCode() == KeyEvent.VK_UP) {

			if (ptr++ >= cmds.length)
				ptr = 0;

			setText(cmds[ptr].toString() + " ");

			setCaretPosition(getText().length());

		} else if (e.getKeyCode() == KeyEvent.VK_DOWN) {

			if (ptr-- <= 0)
				ptr = cmds.length;

			setText(cmds[ptr].toString() + " ");
			setCaretPosition(getText().length());
			
		} else if (e.getKeyChar() == '*') {

			new Thread(new Runnable() {
				@Override
				public void run() {
			
					for (PlayerCommands factory : PlayerCommands.values()) {
						if (!factory.equals(PlayerCommands.restart)) {
							out.println(factory.toString());
							Util.log("sending: " + factory.toString());
							Util.delay(500);
						}
					}
				}}).start();
			
		} else if (e.getKeyChar() == 'z') {

			new Thread(new Runnable() {
				@Override
				public void run() {

					String urlString = "http://"+ State.getReference().get(State.values.externaladdress.name())+":5080/oculus/frameGrabHTTP";
					
					try {
						Util.saveUrl("_local.jpg", urlString );
					} catch (Exception e) {
						System.out.println("can't get image: " + e.getLocalizedMessage());
					}
					
				}}).start();
		}
	}

	
	@Override
	public void keyReleased(KeyEvent e) {}
}
