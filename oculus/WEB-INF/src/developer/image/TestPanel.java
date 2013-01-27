package developer.image;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.Graphics;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.FlowLayout;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.Button;

public class TestPanel extends JFrame {

	private JPanel panel_2;
	private JPanel panel;
	private JPanel panel_1;
	private BufferedImage img;
	JLabel picLabel = new JLabel();
	JLabel picLabel1 = new JLabel();
	private JButton btnFindCtr;
	
	private int[][] ctrMatrix;
	
	/*
	 * run with red5 running, and streaming camera
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
		
		initialize();
	}
	
	private void initialize() {
		final JLabel picLabel2;

		try {
			img = ImageIO.read(new URL("http://127.0.0.1:5080/oculus/frameGrabHTTP"));
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
						img = ImageIO.read(new URL("http://127.0.0.1:5080/oculus/frameGrabHTTP"));
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
		int[] greypxls = ImageUtils.convertToGrey(img);
		ctrMatrix = ImageUtils.convertToMatrix(greypxls, img.getWidth(), img.getHeight());
		BufferedImage gi = ImageUtils.intToImage(greypxls, img.getWidth(), img.getHeight());
		
		//put red dot in ctr 
		int red = (255<<16) + (0<<8) + 0;
		int x = img.getWidth()/2 + ImageUtils.matrixres/2;
		int y = img.getHeight()/2 + ImageUtils.matrixres/2;
		gi.setRGB(x,y, red);
		gi.setRGB(x-1,y, red);
		gi.setRGB(x+1,y, red);
		gi.setRGB(x,y-1, red);
		gi.setRGB(x,y+1, red);
		
		picLabel.setIcon(new ImageIcon(gi));
		panel.repaint();
	}
	
	private void findCtr() {
		int[] greypxls = ImageUtils.convertToGrey(img);
		int[][] matrix = ImageUtils.convertToMatrix(greypxls, img.getWidth(), img.getHeight());
		int[] ctr = ImageUtils.findCenter(matrix, ctrMatrix, img.getWidth(), img.getHeight());
		System.out.println("ctr: "+ctr[0]+","+ctr[1]);
		BufferedImage imgwithctr = ImageUtils.intToImage(greypxls, img.getWidth(), img.getHeight());
		
		//put red dot at previous found ctr
		if (ctr[0] != -1 && ctr[1] != -1) {
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
}
