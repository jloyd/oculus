package developer.image;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;

import javax.imageio.ImageIO;

import org.red5.server.api.IConnection;
import org.red5.server.api.service.IServiceCapableConnection;

import oculus.Application;
import oculus.State;
import oculus.Util;

public class motionDetect {
	private State state = State.getReference();
	private ImageUtils imageUtils = new ImageUtils();
	private IConnection grabber = null;
	private Application app = null;
	private int threshold;
	private int[] lastMassCtr=new int[2];
	
	public motionDetect(Application a, IConnection g, int t) {
		threshold = t;
		this.grabber = g;
		this.app = a;
		start();
	}
	
	private void start() {
		state.delete(State.values.motiondetected);
		state.set(State.values.motiondetectwatching, true);

		
		new Thread(new Runnable() {
			public void run() {
				try {
					int frameno = 0;
					while (state.getBoolean(State.values.motiondetectwatching)) { // TODO: time out after a while
						
						if(state.getBoolean(State.values.framegrabbusy.name()) || 
								 !(state.get(State.values.stream).equals("camera") || 
										 state.get(State.values.stream).equals("camandmic"))) {

							app.message("framegrab busy or stream unavailable", null,null);
							state.set(State.values.motiondetectwatching, false);
							return;
						}
						
						state.set(State.values.framegrabbusy.name(), true);
						IServiceCapableConnection sc = (IServiceCapableConnection) grabber;
						sc.invoke("framegrabMedium", new Object[] {});
						
						while (state.getBoolean(State.values.framegrabbusy)) {
							int n = 0;
							Thread.sleep(5);
							n++;
							if (n> 2000) {  // give up after 10 seconds 
								Util.debug("frame grab timed out", this);
								state.set(State.values.framegrabbusy, false);
								state.set(State.values.motiondetectwatching, false);
								return;
							}
						}
						
						ByteArrayInputStream in = new ByteArrayInputStream(Application.framegrabimg);
						BufferedImage img = ImageIO.read(in);
						int[] greypxls = imageUtils.convertToGrey(img);
						int[] bwpxls = imageUtils.convertToBW(greypxls);

						int sensitivity = 4;
						int[] ctrxy = imageUtils.middleMass(bwpxls, img.getWidth(), img.getHeight(), sensitivity);
						if (frameno >= 1) { // ignore frames 0
							int compared = Math.abs(ctrxy[0]-lastMassCtr[0])+Math.abs(ctrxy[1]-lastMassCtr[1]);
//							app.message("compared = "+compared, null, null); // debug
							if (compared> threshold) { //motion detected above noise level
//								lastMassCtr[0] = -1;
								state.set(State.values.motiondetected, compared); // System.currentTimeMillis());
								state.set(State.values.motiondetectwatching, false);
							}
						}
						lastMassCtr = ctrxy;
						frameno ++;

					}
					
				} catch (Exception e) { e.printStackTrace(); }
			}
		}).start();
	}
	

}
