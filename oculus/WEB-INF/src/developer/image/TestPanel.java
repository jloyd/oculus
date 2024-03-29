package developer.image;

import java.awt.EventQueue;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.JTextField;

public class TestPanel extends JFrame {

	private JPanel panel_2;
	private JPanel panel;
	private JPanel panel_1;
	private BufferedImage img; // constantly refreshed from framegrab at start, available for general use
	JLabel picLabel = new JLabel();
	JLabel picLabel1 = new JLabel();
	private JButton btnFindCtr;
	
	private int[][] ctrMatrix;
	ImageUtils imageUtils = new ImageUtils();
//	final String url = "http://127.0.0.1:5080/oculus/frameGrabHTTP";
	final String url = "http://192.168.0.182:5080/oculus/frameGrabHTTP";
	private JTextField textField;
	
	/*
	 * INSTRUCTIONS
	 * run with red5 running, and streaming camera at 320x480 (medium)
	 * use 'record' to remember view
	 * use 'find' to find offset of remembered view from previous view (previous ctr indicated by dot)
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					TestPanel frame = new TestPanel();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the frame.
	 */
	public TestPanel() {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 692, 563);
		JPanel contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);
		
		panel = new JPanel();
		panel.setBounds(10, 274, 320, 240);
		contentPane.add(panel);
		
		panel_1 = new JPanel();
		panel_1.setBounds(340, 274, 320, 240);
		contentPane.add(panel_1);
		
		panel_2 = new JPanel();
		panel_2.setBounds(10, 11, 320, 240);
		contentPane.add(panel_2);
		
		JButton btnRecordCtr = new JButton("Record Ctr");
		btnRecordCtr.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				recordCtr();
			}
		});
		btnRecordCtr.setBounds(340, 11, 100, 23);
		contentPane.add(btnRecordCtr);
		
		btnFindCtr = new JButton("Find Ctr");
		btnFindCtr.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				findCtr();
			}
		});
		btnFindCtr.setBounds(340, 45, 100, 23);
		contentPane.add(btnFindCtr);
		
		JButton btnNewButton = new JButton("edges");
		btnNewButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				edges();
			}
		});
		btnNewButton.setBounds(340, 79, 100, 23);
		contentPane.add(btnNewButton);
		
		JButton btnEdgesBlur = new JButton("edges, blur");
		btnEdgesBlur.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				edgeswithblur();
			}
		});
		btnEdgesBlur.setBounds(340, 113, 100, 23);
		contentPane.add(btnEdgesBlur);
		
		JButton btnNewButton_1 = new JButton("midlmass BW");
		btnNewButton_1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				middleMass();
			}
		});
		btnNewButton_1.setBounds(340, 147, 100, 23);
		contentPane.add(btnNewButton_1);
		
		textField = new JTextField();
		textField.setBounds(450, 11, 210, 20);
		contentPane.add(textField);
		textField.setColumns(10);
		
		JButton btnNewButton_2 = new JButton("middle mass G");
		btnNewButton_2.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				middleMassGrey();
			}
		});
		btnNewButton_2.setBounds(340, 181, 100, 23);
		contentPane.add(btnNewButton_2);
		
		initialize();
	}
	
	private void initialize() {
		final JLabel picLabel2;

		try {
			img = ImageIO.read(new URL(url));
		}  catch (IOException e2) {
			e2.printStackTrace();
		}
		picLabel2 = new JLabel(new ImageIcon(img));
		panel_2.add(picLabel2);
		panel_2.repaint(); 
	
		// continuous framegrabs to panel_2
		new Thread(new Runnable() {
			public void run() {
				try {
					while(true) {
						img = ImageIO.read(new URL(url));
						picLabel2.setIcon(new ImageIcon(img));
						panel_2.repaint(); 
					}
					
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();		
		
		panel.add(picLabel);		
		panel_1.add(picLabel1);	
	}

	
	
	private void recordCtr() {
		int[] greypxls = imageUtils.convertToGrey(img);
		ctrMatrix = imageUtils.convertToMatrix(greypxls, img.getWidth(), img.getHeight());
		BufferedImage gi = imageUtils.intToImage(greypxls, img.getWidth(), img.getHeight());
		
		//put red dot in ctr 
		int red = (255<<16) + (0<<8) + 0;
		int x = img.getWidth()/2 + imageUtils.matrixres/2;
		int y = img.getHeight()/2 + imageUtils.matrixres/2;
		gi.setRGB(x,y, red);
		gi.setRGB(x-1,y, red);
		gi.setRGB(x+1,y, red);
		gi.setRGB(x,y-1, red);
		gi.setRGB(x,y+1, red);
		
		picLabel.setIcon(new ImageIcon(gi));
		panel.repaint();
	}
	
	private void findCtr() {
		int[] greypxls = imageUtils.convertToGrey(img);
		int[][] matrix = imageUtils.convertToMatrix(greypxls, img.getWidth(), img.getHeight());
		int[] ctr = imageUtils.findCenter(matrix, ctrMatrix, img.getWidth(), img.getHeight());
		BufferedImage imgwithctr = imageUtils.intToImage(greypxls, img.getWidth(), img.getHeight());
		
		//put red dot at returned point
		if (ctr[0] > 0 && ctr[0] < img.getWidth() && ctr[1] > 0 && ctr[1] < img.getHeight()) {
			int red = (255<<16) + (0<<8) + 0;
			imgwithctr.setRGB(ctr[0], ctr[1], red);
			imgwithctr.setRGB(ctr[0]-1, ctr[1], red);
			imgwithctr.setRGB(ctr[0]+1, ctr[1], red);
			imgwithctr.setRGB(ctr[0], ctr[1]-1, red);
			imgwithctr.setRGB(ctr[0], ctr[1]+1, red);
		}
		
		picLabel1.setIcon(new ImageIcon(imgwithctr));
		panel_1.repaint();
	}
	
	private void edges() {
		int[] greypxls = imageUtils.convertToGrey(img);
		int[] edgepxls = imageUtils.edges(greypxls, img.getWidth(), img.getHeight());
		BufferedImage greyimg = imageUtils.intToImage(greypxls, img.getWidth(), img.getHeight());
		picLabel.setIcon(new ImageIcon(greyimg));
		panel.repaint();
		BufferedImage edgeimg = imageUtils.intToImage(edgepxls, img.getWidth(), img.getHeight());
		picLabel1.setIcon(new ImageIcon(edgeimg));
		panel_1.repaint();
	}
	
	private void edgeswithblur() {
		img = imageUtils.blur(img);
		int[] greypxls = imageUtils.convertToGrey(img);
		int[] edgepxls = imageUtils.edges(greypxls, img.getWidth(), img.getHeight());
		BufferedImage greyimg = imageUtils.intToImage(greypxls, img.getWidth(), img.getHeight());
		picLabel.setIcon(new ImageIcon(greyimg));
		panel.repaint();
		BufferedImage edgeimg = imageUtils.intToImage(edgepxls, img.getWidth(), img.getHeight());
		picLabel1.setIcon(new ImageIcon(edgeimg));
		panel_1.repaint();
	}
	
	private int[] lastMassCtr={160,120};
	private void middleMass() {
//		img = imageUtils.blur(img);
		int[] greypxls = imageUtils.convertToGrey(img);
		BufferedImage greyimg = imageUtils.intToImage(greypxls, img.getWidth(), img.getHeight());
		picLabel.setIcon(new ImageIcon(greyimg));
		panel.repaint();
		
		int[] bwpxls = imageUtils.convertToBW(greypxls);
		BufferedImage bwimg = imageUtils.intToImage(bwpxls, img.getWidth(), img.getHeight());
		picLabel1.setIcon(new ImageIcon(bwimg));
		panel_1.repaint();
		
		int sensitivity = 4;
		int[] ctrxy = imageUtils.middleMass(bwpxls, img.getWidth(), img.getHeight(), sensitivity);	
		int compared = Math.abs(ctrxy[0]-lastMassCtr[0])+Math.abs(ctrxy[1]-lastMassCtr[1]);
		lastMassCtr = ctrxy;
		String motion = "no motion";
		if (compared> 5) { motion="MOTION"; }
		textField.setText("x="+Integer.toString(ctrxy[0])+", y="+Integer.toString(ctrxy[1])+ 
				", diff="+compared+", "+motion+", "+imageUtils.imgaverage);
	}
	
	private void middleMassGrey() {
//		img = imageUtils.blur(img);
		int[] greypxls = imageUtils.convertToGrey(img);
		BufferedImage greyimg = imageUtils.intToImage(greypxls, img.getWidth(), img.getHeight());
		picLabel.setIcon(new ImageIcon(greyimg));
		panel.repaint();
		
		int[] ctrxy = imageUtils.middleMassGrey(greypxls, img.getWidth(), img.getHeight());	
		int compared = Math.abs(ctrxy[0]-lastMassCtr[0])+Math.abs(ctrxy[1]-lastMassCtr[1]);
		lastMassCtr = ctrxy;
		textField.setText("x="+Integer.toString(ctrxy[0])+", y="+Integer.toString(ctrxy[1])+ 
				", diff="+compared+", imgavg= "+imageUtils.imgaverage);
	}
}
