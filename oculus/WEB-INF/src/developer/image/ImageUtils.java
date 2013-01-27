package developer.image;

import java.awt.image.BufferedImage;

import oculus.Application;
import oculus.Util;

public class ImageUtils {
	
	public static final int matrixres = 10;
	private static int imgaverage;
	
	public static int[] convertToGrey(BufferedImage img) { // convert image to 8bit greyscale int array
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

	public static BufferedImage intToImage(int[] pixelRGB, int width, int height) { // dev tool
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
	
	public static int[][] convertToMatrix(int[] greyimg, int width, int height) {
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
	
	public static int[] findCenter(int[][] matrix, int[][] ctrMatrix, int width, int height) {
		
		int widthRes = width/matrixres;
		int heightRes = height/matrixres;
		int totalCompared;
		int total;
		int winningx = -1;
		int winningy = -1;

		int winningTotal = 999999999; //max possible int  //(width/matrixRes)*(height/matrixRes)*255; // maximum possible
	
		for (int x=-(widthRes/2); x<=widthRes/2; x++) {
			for (int y=-(heightRes/2); y<=heightRes/2; y++) {
				total = 0;
				totalCompared =0;
				for (int xx=0; xx<matrix.length; xx++) {
					for (int yy=0;yy<matrix[xx].length; yy++) {
						if (xx+x >= 0 && xx+x < widthRes && yy+y >=0 && yy+y <heightRes) { 
							total += Math.abs(matrix[xx+x][yy+y] - ctrMatrix[xx][yy]);
							totalCompared++;
						}
					}						
				}
				if (total/totalCompared < winningTotal) {
					winningTotal = total/totalCompared;
					winningx = x;
					winningy = y;
				}
			}
		}
		if (winningx != -1 && winningy != -1) { // found valid ctr
			winningx = width/2 + (winningx*matrixres) + (matrixres/2);
			winningy = height/2 + (winningy*matrixres) + (matrixres/2);
		}
		return new int[]{winningx, winningy};
		
	}
	
}
