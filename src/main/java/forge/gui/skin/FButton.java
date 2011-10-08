package forge.gui.skin;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import javax.swing.JButton;
import forge.AllZone;

/** 
 * The core JButton used throughout the Forge project. 
 * Follows skin font and theme button styling.
 *
 */
@SuppressWarnings("serial")
public class FButton extends JButton {   
    protected Image imgL, imgM, imgR;
    private int w, h = 0;
    private boolean allImagesPresent = false;
    private RenderingHints rh;
    private FSkin skin;
    private AlphaComposite disabledComposite = 
            AlphaComposite.getInstance(AlphaComposite.SRC_OVER,0.25f);
    
    public FButton(String msg) {
        super(msg);
        this.skin = AllZone.getSkin();
        this.setOpaque(false);
        this.setForeground(skin.txt1a); 
        this.setBackground(Color.red);
        this.setContentAreaFilled(false);
        this.setFont(skin.font1.deriveFont(Font.PLAIN,14));
        this.imgL = skin.btnLup.getImage();
        this.imgM = skin.btnMup.getImage();
        this.imgR = skin.btnRup.getImage();
        
        rh = new RenderingHints(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
                );
        
        if(this.imgL != null && this.imgM != null && this.imgR != null) {
            allImagesPresent = true;
        }
        
        this.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                if(isEnabled()) {
                    imgL = skin.btnLover.getImage();
                    imgM = skin.btnMover.getImage();
                    imgR = skin.btnRover.getImage();
                }
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                if(isEnabled()) {
                    imgL = skin.btnLup.getImage();
                    imgM = skin.btnMup.getImage();
                    imgR = skin.btnRup.getImage();
                }
            }
            public void mousePressed(java.awt.event.MouseEvent evt) {
                if(isEnabled()) {
                    imgL = skin.btnLdown.getImage();
                    imgM = skin.btnMdown.getImage();
                    imgR = skin.btnRdown.getImage();
                }
            }
        }); 
    }
    
    protected void paintComponent(Graphics g) {
        if(!allImagesPresent) {
            return;
        }
        
        Graphics2D g2d = (Graphics2D)g;
        g2d.setRenderingHints(rh);
        
        if(!isEnabled()) {
            g2d.setComposite(disabledComposite);
        }
        
        w = this.getWidth();
        h = this.getHeight();        
        
        g2d.drawImage(imgL,0,0,h,h,null);
        g2d.drawImage(imgM,h,0,w - 2*h,h,null);
        g2d.drawImage(imgR,w-h,0,h,h,null); 
        
        super.paintComponent(g);
    }
}