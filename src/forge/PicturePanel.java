package forge;

import java.awt.Image;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;

import forge.error.ErrorViewer;

public class PicturePanel extends JPanel {
	private static final long serialVersionUID = 2282867940272644768L;

	private static int nextUniqueNumber;
	private int uniqueNumber = nextUniqueNumber++;
	
	private JLabel display;
	private ImageIcon imageIcon;
	private Image image;

	public PicturePanel(File f) {
		if (!f.exists()) {
			ErrorViewer.showError("PicturePanel : file does not exist - %s", f);
			throw new RuntimeException("PicturePanel : file does not exist - " + f);
		}

		if (imageIcon == null) {
			imageIcon = new ImageIcon();
		}
		try {
			image = ImageIO.read(f);
		} catch (IOException e) {
			e.printStackTrace();
		}
		imageIcon.setImage(image);
		if (display == null) {
			display = new JLabel();
			display.setIcon(imageIcon);
			this.add(display);
		}
		//System.out.println("PicturePanel: " + uniqueNumber);
	}
	
	public void updateCardImage(File f) {
		if (!f.exists()) {
			ErrorViewer.showError("PicturePanel : file does not exist - %s", f);
			throw new RuntimeException("PicturePanel : file does not exist - " + f);
		}

		try {
			image = ImageIO.read(f);
		} catch (IOException e) {
			e.printStackTrace();
		}
		imageIcon.setImage(image);
		System.gc();
		
		//System.out.println("PicturePanel, update: " + uniqueNumber);
	}
}