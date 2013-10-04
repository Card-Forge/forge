package forge.view;

import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;

import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.SpringLayout;
import javax.swing.SwingUtilities;

import forge.gui.framework.ILocalRepaint;
import forge.gui.toolbox.FSkin;
import forge.gui.toolbox.FSkin.Colors;
import forge.gui.toolbox.FSkin.JComponentSkin;
import forge.gui.toolbox.FSkin.JLabelSkin;
import forge.gui.toolbox.FSkin.SkinColor;

@SuppressWarnings("serial")
public abstract class FTitleBarBase extends JMenuBar {
    protected static final int visibleHeight = 26;
    protected static final SkinColor foreColor = FSkin.getColor(Colors.CLR_TEXT);
    protected static final SkinColor backColor = FSkin.getColor(Colors.CLR_THEME2);
    protected static final SkinColor borderColor = backColor.stepColor(-80);
    protected static final SkinColor buttonHoverColor = backColor.stepColor(40);
    protected static final SkinColor buttonDownColor = backColor.stepColor(-40);

    protected final FFrame frame;
    protected final JComponentSkin<FTitleBarBase> skin = FSkin.get(this);
    protected final SpringLayout layout = new SpringLayout();
    protected final MinimizeButton btnMinimize = new MinimizeButton();
    protected final MaximizeButton btnMaximize = new MaximizeButton();
    protected final CloseButton btnClose = new CloseButton();

    protected FTitleBarBase(FFrame f) {
        this.frame = f;
        setVisible(false); //start out hidden unless frame chooses to show title bar
        setLayout(this.layout);
        skin.setBackground(backColor);
        skin.setMatteBorder(0, 0, 1, 0, borderColor);
    }
    
    protected void addControls() {
        add(btnClose);
        layout.putConstraint(SpringLayout.EAST, btnClose, 0, SpringLayout.EAST, this);
        layout.putConstraint(SpringLayout.SOUTH, btnClose, 0, SpringLayout.SOUTH, this);

        add(btnMaximize);
        layout.putConstraint(SpringLayout.EAST, btnMaximize, 0, SpringLayout.WEST, btnClose);
        layout.putConstraint(SpringLayout.SOUTH, btnMaximize, 0, SpringLayout.SOUTH, btnClose);
        
        add(btnMinimize);
        layout.putConstraint(SpringLayout.EAST, btnMinimize, 0, SpringLayout.WEST, btnMaximize);
        layout.putConstraint(SpringLayout.SOUTH, btnMinimize, 0, SpringLayout.SOUTH, btnMaximize);
    }

    public abstract String getTitle();
    public abstract void setTitle(String title);
    public abstract void setIconImage(Image image);
    
    public void handleMaximizedChanged() {
        if (frame.getMaximized()) {
            btnMaximize.setToolTipText("Restore Down");
        }
        else {
            btnMaximize.setToolTipText("Maximize");
        }
        btnMaximize.repaintSelf();
    }
    
    /* (non-Javadoc)
     * @see javax.swing.JComponent#setVisible(boolean)
     */
    @Override
    public void setVisible(boolean visible) {
        //use height 0 to hide rather than setting visible to false to allow menu item accelerators to work
        setPreferredSize(new Dimension(this.frame.getWidth(), visible ? visibleHeight : 0));
        revalidate();
    }
    
    public abstract class TitleBarButton extends JLabel implements ILocalRepaint {
        protected JLabelSkin<TitleBarButton> skin = FSkin.get(this);
        private boolean pressed, hovered;

        private TitleBarButton() {
            setPreferredSize(new Dimension(25, 25));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        pressed = true;
                        repaintSelf();
                    }
                }
                @Override
                public void mouseReleased(MouseEvent e) {
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        if (pressed) {
                            pressed = false;
                            if (hovered) { //only handle click if mouse released over button
                                repaintSelf();
                                onClick();
                            }
                        }
                    }
                }
                @Override
                public void mouseEntered(MouseEvent e) {
                    hovered = true;
                    repaintSelf();
                }
                @Override
                public void mouseExited(MouseEvent e) {
                    hovered = false;
                    repaintSelf();
                }
            });
        }
        
        protected abstract void onClick();
        
        @Override
        public void repaintSelf() {
            final Dimension d = this.getSize();
            repaint(0, 0, d.width, d.height);
        }
        
        @Override
        public void paintComponent(Graphics g) {
            if (hovered) {
                if (pressed) {
                    skin.setGraphicsColor(g, buttonDownColor);
                    g.fillRect(0, 0, getWidth(), getHeight());
                    g.translate(1, 1); //translate icon to give pressed button look
                }
                else {
                    skin.setGraphicsColor(g, buttonHoverColor);
                    g.fillRect(0, 0, getWidth(), getHeight());
                }
            }
        }
    }
    
    public class MinimizeButton extends TitleBarButton {
        private MinimizeButton() {
            setToolTipText("Minimize");
        }
        @Override
        protected void onClick() {
            frame.setMinimized(true);
        }
        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);

            int thickness = 2;
            int offsetX = 8;
            int offsetY = 7;
            int x1 = offsetX;
            int x2 = getWidth() - offsetX;
            int y = getHeight() - offsetY - thickness;
            
            Graphics2D g2d = (Graphics2D) g;
            skin.setGraphicsColor(g2d, foreColor);
            g2d.setStroke(new BasicStroke(thickness));
            g2d.drawLine(x1, y, x2, y);
        }
    }
    
    public class MaximizeButton extends TitleBarButton {
        private MaximizeButton() {
            setToolTipText("Maximize");
        }
        @Override
        protected void onClick() {
            frame.setMaximized(!frame.getMaximized());
        }
        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);

            int thickness = 2;
            int offsetX = 7;
            int offsetY = 8;
            int x = offsetX;
            int y = offsetY;
            int width = getWidth() - 2 * offsetX;
            int height = getHeight() - 2 * offsetY;
            
            Graphics2D g2d = (Graphics2D) g;
            skin.setGraphicsColor(g2d, foreColor);
            g2d.setStroke(new BasicStroke(thickness));
            
            if (frame.getMaximized()) { //draw 2 rectangles offset if icon to restore window
                x -= 1;
                y += 2;
                width -= 1;
                height -= 1;
                g2d.drawRect(x, y, width, height);
                x += 3;
                y -= 3;
                //draw back rectangle as 4 lines so front rectangle doesn't show back rectangle through it
                g2d.drawLine(x, y, x, y + 2);
                g2d.drawLine(x, y, x + width, y);
                x += width;
                g2d.drawLine(x, y, x, y + height);
                y += height;
                g2d.drawLine(x - 2, y, x, y);
            }
            else { //otherwise just draw 1 rectangle if icon to maximize window
                g2d.drawRect(x, y, width, height);
            }
        }
    }
    
    public class CloseButton extends TitleBarButton {
        private CloseButton() {
            setToolTipText("Close");
        }
        @Override
        protected void onClick() {
            WindowEvent wev = new WindowEvent(frame, WindowEvent.WINDOW_CLOSING);
            Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(wev);
        }
        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);

            int thickness = 2;
            int offset = 7;
            int x1 = offset;
            int y1 = offset;
            int x2 = getWidth() - offset - 1;
            int y2 = getHeight() - offset - 1;

            Graphics2D g2d = (Graphics2D) g;
            skin.setGraphicsColor(g2d, foreColor);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setStroke(new BasicStroke(thickness));
            g2d.drawLine(x1, y1, x2, y2);
            g2d.drawLine(x2, y1, x1, y2);
        }
    }
}
