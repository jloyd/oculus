package developer;

import oculus.Observer;
import oculus.Settings;
import oculus.State;
import oculus.Util;

import org.OpenNI.*;

public class MotionTracker implements IObserver<ErrorStateEventArgs>, Observer {

	private static Context context;
	private static DepthGenerator depth;
	private static DepthMetaData depthMD;
	private State state = State.getReference();
	

	public MotionTracker() {

		Util.log("start up.. ", this);
		
		String sep = "\\"; 
		if (Settings.os.equals("linux")) sep = "/";
		String SAMPLES_XML = System.getenv("RED5_HOME") + sep + "webapps" + sep + "oculus" + sep + "openNIconfig.xml";

		try {
			
			OutArg<ScriptNode> scriptNodeArg = new OutArg<ScriptNode>();
			context = Context.createFromXmlFile(SAMPLES_XML, scriptNodeArg);
			context.getErrorStateChangedEvent().addObserver(this);
			depth = (DepthGenerator) context.findExistingNode(NodeType.DEPTH);
			depthMD = new DepthMetaData();
			
		} catch (Throwable e) {
			Util.debug("constructor: " + e.getLocalizedMessage(), this);
			try {
				depth.stopGenerating();
			} catch (StatusException e1) {
				Util.debug("constructor: " + e1.getLocalizedMessage(), this);
				return;
			}
			return;
		}
		
		state.addObserver(this);
		start();
	}

	public void start(){
		new Thread(new Runnable() {
			@Override
			public void run() {
				
				// setup must be done before any reads 
				depth.getMetaData(depthMD);
				
				// read at fixed rate 
				while(true){
					
					Util.delay(300);
					
					state.set("center_point", depthMD.getData().readPixel(depthMD.getXRes() / 2, depthMD.getYRes() / 2));
					
				}
			}
		}).start();
	}

	@Override
	public void updated(String key) {
		// Util.log("state changed: " + key + " value: " + state.get(key), this);
	}	
	
	@Override
	public void update(IObservable<ErrorStateEventArgs> arg0, ErrorStateEventArgs arg1) {
		Util.log("Global error state has changed: " + arg1.getCurrentError(), this);
		System.exit(1);
	}
}
