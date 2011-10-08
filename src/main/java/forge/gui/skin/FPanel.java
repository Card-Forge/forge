package forge.gui.skin;

import java.awt.Graphics;
import java.awt.LayoutManager;
import javax.swing.ImageIcon;
import javax.swing.JPanel;

/** <p>FPanel.</p>
 * The core JPanel used throughout the Forge project. 
 * Allows tiled images and ...
 *
 */
@SuppressWarnings("serial")
public class FPanel extends JPanel {
    private ImageIcon bgImg             = null;
    private int w, h, iw, ih, x, y      = 0;
    
    public FPanel() {
        super();
    }
    
    public FPanel(LayoutManager lm) {
        this();
        this.setLayout(lm);
    }

    protected void paintComponent(Graphics g) {
        //System.out.print("\nRepainting. ");
        if(this.bgImg != null) {
            w = getWidth();
            h = getHeight();
            iw = this.bgImg.getIconWidth();
            ih = this.bgImg.getIconHeight();

            while(x < w) {
                while(y < h) {
                    g.drawImage(bgImg.getImage(),x,y,null);
                    y += ih;
                }
                x += iw;
                y = 0;
            }
            x = 0;
        }
        
        super.paintComponent(g);
    }
    
    public void setBGImg(ImageIcon icon) {
        this.bgImg = icon; 
        if(this.bgImg != null) {
            this.setOpaque(false);
        }
    }
}
