import java.awt.Graphics;
import java.awt.Image;

import javax.swing.ImageIcon;
import javax.swing.JPanel;

public class ImageJPanel extends JPanel
{
	private static final long serialVersionUID = 2908946871498362599L;
	
	private Image aImage = null;
	
	public ImageJPanel(String s)
	{
		init(s);
	}
	
	private void init(String image)
	{

		ImageIcon imageIcon = new ImageIcon("pics\\" + image);
		aImage = imageIcon.getImage();
		
	}
	
	public void paint(Graphics g) 
    {
		super.paint(g);
        g.drawImage(aImage, 0, 0, this);
    }
}