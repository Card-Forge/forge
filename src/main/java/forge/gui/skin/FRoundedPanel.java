package forge.gui.skin;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RenderingHints;

import javax.swing.JPanel;

import forge.AllZone;

/** <p>FRoundedPanel.</p>
 * A subclass of JPanel with optional rounded corners and 
 * optional drop shadow, for special cases only. FPanel recommended for regular use.
 * Limitations - cannot use background image, and single line border only. 
 * 
 * Default values provided, later updated from skin settings, and can be
 * set dynamically.
 *
 */
@SuppressWarnings("serial")
public class FRoundedPanel extends JPanel {
    private FSkin skin;
    private Color shadowColor           = new Color(150,150,150,150);
    private Color borderColor           = Color.black;
    private Dimension shadowDistance    = new Dimension(5,5);
    private int shadowThickness         = 5;
    private int cornerRadius            = 20; // Note: this number is actually diameter.
    
    /**
     * <p>FRoundedPanel.</p>
     * 
     * Constructor, null layout manager.
     */
    public FRoundedPanel() {
        super();
        this.setOpaque(false);
        skin = AllZone.getSkin();
        
        Color tempC;
        Dimension tempD;
        String tempS;
        
        tempC = parseColorString(skin.getSetting("shadowColor"));
        if(tempC != null) { shadowColor = tempC; }
        
        tempC = parseColorString(skin.getSetting("borderColor"));
        if(tempC != null) { borderColor = tempC; }
        
        tempD = parseDimensionString(skin.getSetting("shadowDistance"));
        if(tempD != null) { shadowDistance = tempD; }
        
        tempS = skin.getSetting("shadowThickness");
        if(tempS != null) { shadowThickness = Integer.parseInt(tempS); }
        
        tempS = skin.getSetting("cornerRadius");
        if(tempS != null) { cornerRadius = Integer.parseInt(tempS); }
    }
    
    /**
     * <p>FRoundedPanel.</p>
     * 
     * Constructor.
     * @param {@link java.awt.LayoutManager}
     */
    public FRoundedPanel(LayoutManager lm) {
        this();
        this.setLayout(lm);
    }

    /**
     * <p>FRoundedPanel.</p>
     * 
     * Constructor.
     * @param {@link java.awt.Graphics}
     */
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        int w = getWidth();
        int h = getHeight();
        
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
                RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Draw shadow
        g2d.setColor(shadowColor);
        g2d.setStroke(new BasicStroke(shadowThickness));
        g2d.drawRoundRect(
                0 + (int)shadowDistance.getWidth() + (shadowThickness/2), 
                0 + (int)shadowDistance.getHeight() + (shadowThickness/2), 
                w - (int)shadowDistance.getWidth() - shadowThickness, 
                h - (int)shadowDistance.getHeight() - shadowThickness, 
                cornerRadius, cornerRadius);
        
        // Draw content rectangle (on top of shadow)
        g2d.setColor(this.getBackground()); 
        g2d.fillRoundRect(
                0, 0,
                w - shadowThickness, 
                h - shadowThickness, 
                cornerRadius, cornerRadius);
        
        // Stroke border
        g2d.setColor(this.borderColor);
        g2d.setStroke(new BasicStroke(1));
        g2d.drawRoundRect(
                0,0,
                w - shadowThickness, 
                h - shadowThickness, 
                cornerRadius, cornerRadius);        
    }
    
    /**
     * <p>setShadowColor.</p>
     * Sets color of shadow behind rounded panel. 
     *
     * @param {@link java.awt.Color}
     */
    public void setShadowColor(Color c) {
        this.shadowColor = c;
    }
    
    /**
     * <p>setBorderColor.</p>
     * Sets color of border around rounded panel. 
     *
     * @param {@link java.awt.Color}
     */
    public void setBorderColor(Color c) {
        this.borderColor = c;
    }
    
    /**
     * <p>setShadowDistance.</p>
     * Sets distance of shadow from rounded panel.
     *
     * @param {@link java.lang.Integer} side
     */
    public void setShadowDistance(int side) {
        this.shadowDistance = new Dimension(side,side);
    }
    
    /**
     * <p>setShadowDistance.</p>
     * Sets distance of shadow from rounded panel.
     *
     * @param {@link java.lang.Integer} x
     * @param {@link java.lang.Integer} y
     */
    public void setShadowDistance(int x, int y) {
        this.shadowDistance = new Dimension(x,y);
    }
    
    /**
     * <p>setShadowDistance.</p>
     * Sets thickness of rounded panel shadow.
     *
     * @param {@link java.lang.Integer} t
     */
    public void setShadowThickness(int t) {
        this.shadowThickness = t;
    }
    
    /**
     * <p>setCornerRadius.</p>
     * Sets radius of each corner on rounded panel.
     *
     * @param {@link java.lang.Integer} r
     */
    public void setCornerRadius(int r) {
        if(r < 0) {
            r = 0;
        }
        
        this.cornerRadius = r*2;
    }    
    
    /**
     * <p>parseColorString.</p>
     * Uses string from settings file to make a new rgba color instance.
     *
     * @param {@link java.lang.String} s
     */
    private Color parseColorString(String s) {
        Color c = null;
        int r,g,b,a = 0;
        String[] temp = s.split(",");
        
        if(temp.length==3 || temp.length==4) {
            r = Integer.parseInt(temp[0]);
            g = Integer.parseInt(temp[1]);
            b = Integer.parseInt(temp[2]);
            if(temp.length==4) {
                a = Integer.parseInt(temp[3]);
            }
            c = new Color(r,g,b,a);
        } 
        
        return c;
    }
    
    /**
     * <p>parseDimensionString.</p>
     * Uses string from settings file to make a new dimension instance.
     *
     * @param {@link java.lang.String} s
     */
    private Dimension parseDimensionString(String s) {
        Dimension d = null;
        int w,h = 0;
        String[] temp = s.split(",");
        
        if(temp.length==2) {
            w = Integer.parseInt(temp[0]);
            h = Integer.parseInt(temp[0]);
            d = new Dimension(w,h);
        } 
        else if(temp.length==1 && !temp[0].equals("")) {
            w = Integer.parseInt(temp[0]);
            h = w;
            d = new Dimension(w,h);
        }
        
        return d;
    }
}
