package developer;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;

import oculus.Observer;
import oculus.Settings;
import oculus.State;
import oculus.Util;

import java.io.File;
import java.io.IOException;
import java.nio.ShortBuffer;
import javax.imageio.ImageIO;
import org.OpenNI.*;

public class MotionTracker implements IObserver<ErrorStateEventArgs>, Observer {

	protected static final long START_UP_DELAY = 500;
	protected static final long POLL_DELAY = 300;
	
	private static MotionTracker singleton = null;
	private State state = State.getReference();
	private boolean running = false;
	private Context context;
	private DepthGenerator depth;
	private DepthMetaData depthMD;
	private int xRes = 0;
	private int yRes = 0;
	private byte[] imgbytes;
	private float histogram[];
	private BufferedImage bimg;
	
	
	/** */
	public static MotionTracker getReference() {
		if(singleton==null) singleton = new MotionTracker();
		return singleton;
	}

	/** */
	private MotionTracker() {

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
		
		// setup must be done before any reads 
		depth.getMetaData(depthMD);
		xRes = depthMD.getFullXRes();
		yRes = depthMD.getFullXRes();
		
		Util.debug("start up, xRes: " + xRes + " yRes: " + yRes, this);
		initImage();
		
		state.addObserver(this);
		start();
	}
	
	/** */
	public void stop()  {
		try {
			running = false;
			depth.stopGenerating();
		} catch (StatusException e) {
			Util.debug("stop(): " + e.getLocalizedMessage(), this);
		}
	}
	
	/** */
	public void start(){
		
		running = true;
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				
				Util.delay(START_UP_DELAY);
				
				// read at fixed rate 
				while(running){ 
					
					Util.delay(POLL_DELAY);
					
					updateCenter();
					
				//	updateDepth();
				
				//	save("test.png");
					
				}
			}
		}).start();
	}
	
	/** send current center point to state. (distance in mm) */
	private void updateCenter(){
		int center = depthMD.getData().readPixel(xRes / 2, yRes / 2);		
		if(center != state.getInteger(State.values.centerpoint))
			state.set(State.values.centerpoint, center);
	}

	
	/** */ 
	public void initImage(){

        histogram = new float[10000];
		imgbytes = new byte[xRes*yRes]; 
        
		DataBufferByte dataBuffer = new DataBufferByte(imgbytes, xRes*yRes);        
		Raster raster = Raster.createPackedRaster(dataBuffer, (Integer) xRes, yRes, 8, null);   
		bimg = new BufferedImage((Integer) xRes, yRes, BufferedImage.TYPE_BYTE_GRAY);
		bimg.setData(raster);
		
	}   
	
	
	/** */ 
	private void calcHist(){
        // reset
        for (int i = 0; i < histogram.length; ++i) histogram[i] = 0;
        ShortBuffer depth = depthMD.getData().createShortBuffer();
        depth.rewind();

        int points = 0;
        while(depth.remaining() > 0){	
            short depthVal = depth.get();
            if (depthVal != 0){
                histogram[depthVal]++;
                points++;
            }
        }
        
        for (int i = 1; i < histogram.length; i++)
        	histogram[i] += histogram[i-1];
       

        if (points > 0) {
            for (int i = 1; i < histogram.length; i++) {
                histogram[i] = (int)(256 * (1.0f - (histogram[i] / (float)points)));
            }
        }
    }

	
	/** */ 
    private void updateDepth(){
        try {
        	
            context.waitAnyUpdateAll();
            calcHist();
            ShortBuffer depth = depthMD.getData().createShortBuffer();
            depth.rewind();
            
            while(depth.remaining() > 0) {
                int pos = depth.position();
                short pixel = depth.get();
                imgbytes[pos] = (byte)histogram[pixel];
            }
        } catch (GeneralException e) {
        	Util.log("updateDepth(): " + e.getLocalizedMessage(), this);
        }
    }

    
    /** */ 
    public boolean isRunning(){ 
    	return running;
    }
    
    
    /** */ 
    public BufferedImage getHist(){
    	updateDepth();
    	DataBufferByte dataBuffer = new DataBufferByte(imgbytes, xRes*yRes);
    	Raster raster = Raster.createPackedRaster(dataBuffer, xRes, yRes, 8, null);
    	bimg.setData(raster);
    	return bimg;
    }

    /** */ 
    public void save(final String filename){ 
    
    	// TODO: remove later.. 
    	// long start = System.currentTimeMillis();
    	updateDepth();
    	
        DataBufferByte dataBuffer = new DataBufferByte(imgbytes, xRes*yRes);
        Raster raster = Raster.createPackedRaster(dataBuffer, xRes, yRes, 8, null);
        bimg.setData(raster);

        File outputfile = new File(filename);
        try {
			ImageIO.write(bimg, "png", outputfile);
		} catch (IOException e) {
			Util.log("save(): " + e.getLocalizedMessage(), this);
		}
      
        // takes under 100ms
        // Util.log("saved to file: " + outputfile.getAbsolutePath() + (System.currentTimeMillis()-start)+" ms", this);
    }
	
	
	/** @return the current frame as an array of distance values in mm 
	public int[][] getFrame(){
		
		// TODO: remove later.. range is 8ms to 20m on my dell 
		// long start = System.currentTimeMillis();
		
		int frame[][] = new int[xRes][yRes];
		for(int x = 0; x < xRes ; x++){
			for(int y = 0; y < yRes ; y++){
				frame[x][y] = depthMD.getData().readPixel(x, y);
			}
		}
		
		//TODO: use enum 
		///state.set("readTime", (System.currentTimeMillis()-start)+" ms");
		
		return frame;
	}*/
	
	/** subtract pixel for pixel 
	public int[][] diff(final int[][] a, final int[][] b){
		int frame[][] = new int[xRes][yRes];
		for(int x = 0; x < xRes ; x++){
			for(int y = 0; y < yRes ; y++){
				frame[x][y] = a[x][y] - b[x][y];
			}
		}
		return frame;
	}*/
	
	/** subtract pixel for pixel 
	public Vector difference(final int[][] a, final int[][] b){
		//int frame[][] = new int[xRes][yRes];
		Vector section = new Vector();
		for(int x = 0; x < xRes ; x++){
			for(int y = 0; y < yRes ; y++){
				section.add(new pixel(x,y, a[x][y] - b[x][y]));
			}
		}
		return section;
	}*/
	
	/**use when holding an array of coords and values 
	public class pixel {
		int z = 0;
		int x = 0;
		int y = 0;
		
		public pixel(int xval, int yval, int zval){
			z = zval;
			y = yval;
			x = xval;
		}
		
	}*/ 
	
	/** 
	public void printFrame(final int[][] frame){
		for(int y = 0; y < yRes ; y+=10)
			Util.log("y: " + y + " " + getRow(frame, y), this);
	}*/
	
	/** 
	public String getRow(final int[][] frame, final int y){		
		String line = "";
		for(int x = 0; x < xRes ; x++)
			line += frame[x][y] + " ";

		return line;
	}*/
	
	@Override
	public void updated(final String key) {
		//Util.log("state changed: " + key + " value: " + state.get(key), this);
	}	
	
	@Override
	public void update(IObservable<ErrorStateEventArgs> arg0, ErrorStateEventArgs arg1) {
		Util.log("Global error state has changed: " + arg1.getCurrentError(), this);
		System.exit(1);
	}
}
