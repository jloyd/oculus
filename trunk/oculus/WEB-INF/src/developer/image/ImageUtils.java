package developer.image;

import java.awt.image.BufferedImage;

public class ImageUtils {
	
	public final int matrixres = 10;
	private int imgaverage;
	
	public ImageUtils() {}
	
	public int[] convertToGrey(BufferedImage img) { // convert image to 8bit greyscale int array
		int[] pixelRGB = img.getRGB(0, 0, img.getWidth(), img.getHeight(), null, 0, img.getWidth());
		
		int p; 
		int[] greyimg = new int[img.getWidth()*img.getHeight()];
		int n = 0;
		int runningttl = 0;			
		for (int i=0; i < pixelRGB.length; i++) {
			int  red   = (pixelRGB[i] & 0x00ff0000) >> 16;
			int  green = (pixelRGB[i] & 0x0000ff00) >> 8;
			int  blue  =  pixelRGB[i] & 0x000000ff;
			p = (int) (red*0.3 + green*0.59 + blue*0.11) ;
			greyimg[n]=p;
			n++;
			runningttl += p;
		}
		imgaverage = runningttl/n;
		return greyimg;
	}

	public BufferedImage intToImage(int[] pixelRGB, int width, int height) { // dev tool
		BufferedImage img  = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		for(int y=0; y<height; y++) {
			for (int x=0; x<width; x++) {
				int grey = pixelRGB[x + y*width];
				int argb = (grey<<16) + (grey<<8) + grey;
				img.setRGB(x, y, argb);
			}
		}
		return img;
	}
	
	public int[][] convertToMatrix(int[] greyimg, int width, int height) {
//		var result:Array = [];
		int[][] matrix = new int[width/matrixres][height/matrixres]; //TODO: may need to add or subtract 1?
		int n;
		int xx;
		int yy;
		int runningttl;
		for (int x = 0; x < width; x += matrixres) {			
			//TODO: finished to here
			for (int y=0; y<height; y+=matrixres) {
				
				runningttl = 0;
				for (xx=0; xx<matrixres; xx++) {
					for (yy=0; yy<matrixres; yy++) {
						runningttl += greyimg[x + xx + (y+yy)*width]; 
					}
				}
				
				n = runningttl/(matrixres*matrixres);				
				matrix[x/matrixres][y/matrixres] = n - imgaverage;
														
			}
		}
		return matrix;
	}
	
	public int[] findCenter(int[][] matrix, int[][] ctrMatrix, int width, int height) {
		
		int widthRes = width/matrixres;
		int heightRes = height/matrixres;
		int compared = 0;
		int total = 0;
		int winningx = 0;
		int winningy = 0;
		
		int winningTotal = 0; // debug log only 
		int winningCompared = 0; // debug log only

		double winningRatio = 9999999; 
	
		for (int x=-(widthRes/2); x<=widthRes/2; x++) {
			for (int y=-(heightRes/2); y<=heightRes/2; y++) {
				total = 0;
				compared =0;
				for (int xx=0; xx<matrix.length; xx++) {
					for (int yy=0;yy<matrix[xx].length; yy++) {
						if (xx+x >= 0 && xx+x < widthRes && yy+y >=0 && yy+y <heightRes) { 
							total += Math.abs(matrix[xx+x][yy+y] - ctrMatrix[xx][yy]);
							compared++;
						}
					}						
				}
				if ( (double) total / (double) compared < winningRatio) {
					winningRatio = (double) total/ (double) compared;
					winningTotal = total; // debug log only 
					winningCompared = compared; // debug log only 
					winningx = x;
					winningy = y;
				}
			}
		}
		System.out.print("ctr mxy: "+winningx+", "+winningy+", ");
		winningx = width/2 + (winningx*matrixres) + (matrixres/2);
		winningy = height/2 + (winningy*matrixres) + (matrixres/2);
		System.out.println("ctr pxy: "+winningx+","+winningy+", wttl: "+winningRatio+", ttl: "+winningTotal+", comp: "+ winningCompared);
		return new int[]{winningx, winningy};	
	}
	
}
