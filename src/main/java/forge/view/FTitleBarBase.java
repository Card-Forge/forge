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
    protected static final int visibleHeight = 27;
    protected static final SkinColor foreColor = FSkin.getColor(Colors.CLR_TEXT);
    protected static final SkinColor backColor = FSkin.getColor(Colors.CLR_THEME2);
    protected static final SkinColor bottomEdgeColor = FSkin.getColor(Colors.CLR_BORDERS).stepColor(0);
    protected static final SkinColor buttonBorderColor = backColor.stepColor(-80);
    protected static final SkinColor buttonHoverColor = backColor.stepColor(40);
    protected static final SkinColor buttonDownColor = backColor.stepColor(-40);
    protected static final SkinColor buttonToggleColor = backColor.stepColor(-30);

    protected final FFrame frame;
    protected final JComponentSkin<FTitleBarBase> skin = FSkin.get(this);
    protected final SpringLayout layout = new SpringLayout();
    protected final LockTitleBarButton btnLockTitleBar = new LockTitleBarButton();
    protected final MinimizeButton btnMinimize = new MinimizeButton();
    protected final FullScreenButton btnFullScreen = new FullScreenButton();
    protected final MaximizeButton btnMaximize = new MaximizeButton();
    protected final CloseButton btnClose = new CloseButton();

    protected FTitleBarBase(FFrame f) {
        this.frame = f;
        setVisible(false); //start out hidden unless frame chooses to show title bar
        setLayout(this.layout);
        skin.setBackground(backColor);
        skin.setMatteBorder(0, 0, 2, 0, bottomEdgeColor);
    }
    
    protected void addControls() {
        add(btnClose);
        layout.putConstraint(SpringLayout.EAST, btnClose, 0, SpringLayout.EAST, this);
        layout.putConstraint(SpringLayout.SOUTH, btnClose, 0, SpringLayout.SOUTH, this);

        add(btnMaximize);
        layout.putConstraint(SpringLayout.EAST, btnMaximize, 0, SpringLayout.WEST, btnClose);
        layout.putConstraint(SpringLayout.SOUTH, btnMaximize, 0, SpringLayout.SOUTH, btnClose);
        
        add(btnFullScreen);
        layout.putConstraint(SpringLayout.EAST, btnFullScreen, 0, SpringLayout.WEST, btnMaximize);
        layout.putConstraint(SpringLayout.SOUTH, btnFullScreen, 0, SpringLayout.SOUTH, btnMaximize);
        
        add(btnMinimize);
        layout.putConstraint(SpringLayout.EAST, btnMinimize, 0, SpringLayout.WEST, btnFullScreen);
        layout.putConstraint(SpringLayout.SOUTH, btnMinimize, 0, SpringLayout.SOUTH, btnFullScreen);
        
        add(btnLockTitleBar);
        layout.putConstraint(SpringLayout.EAST, btnLockTitleBar, 0, SpringLayout.WEST, btnMinimize);
        layout.putConstraint(SpringLayout.SOUTH, btnLockTitleBar, 0, SpringLayout.SOUTH, btnMinimize);
    }

    public abstract void setTitle(String title);
    public abstract void setIconImage(Image image);
    
    public void updateButtons() {
        boolean fullScreen = frame.isFullScreen();
        btnLockTitleBar.setVisible(fullScreen);
        btnMaximize.setVisible(!fullScreen);

        if (fullScreen) {
            layout.putConstraint(SpringLayout.EAST, btnFullScreen, 0, SpringLayout.WEST, btnClose);
            btnFullScreen.setToolTipText("Exit Full Screen (F11)");
            if (frame.getLockTitleBar()) {
                btnLockTitleBar.setToolTipText("Unlock Title Bar");
            }
            else {
                btnLockTitleBar.setToolTipText("Lock Title Bar");
            }
            btnLockTitleBar.repaintSelf();
        }
        else {
            layout.putConstraint(SpringLayout.EAST, btnFullScreen, 0, SpringLayout.WEST, btnMaximize);
            btnFullScreen.setToolTipText("Full Screen (F11)");
            if (frame.isMaximized()) {
                btnMaximize.setToolTipText("Restore Down");
            }
            else {
                btnMaximize.setToolTipText("Maximize");
            }
            btnMaximize.repaintSelf();
        }
        btnFullScreen.repaintSelf();
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
                    if (!TitleBarButton.this.isEnabled()) { return; }
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        pressed = true;
                        repaintSelf();
                    }
                }
                @Override
                public void mouseReleased(MouseEvent e) {
                    if (pressed && SwingUtilities.isLeftMouseButton(e)) {
                        pressed = false;
                        if (hovered) { //only handle click if mouse released over button
                            repaintSelf();
                            onClick();
                        }
                    }
                }
                @Override
                public void mouseEntered(MouseEvent e) {
                    if (!TitleBarButton.this.isEnabled()) { return; }
                    hovered = true;
                    repaintSelf();
                }
                @Override
                public void mouseExited(MouseEvent e) {
                    if (hovered) {
                        hovered = false;
                        repaintSelf();
                    }
                }
            });
        }
        
        protected abstract void onClick();
        
        @Override
        public void setEnabled(boolean enabled0) {
            if (!enabled0 && hovered) {
                hovered = false; //ensure hovered reset if disabled
            }
            super.setEnabled(enabled0);
        }
        
        @Override
        public void repaintSelf() {
            final Dimension d = this.getSize();
            repaint(0, 0, d.width, d.height);
        }
        
        protected boolean isToggled() { //virtual method to override in extended classes
            return false;
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
            else if (isToggled()) {
                int width = getWidth() - 2;
                int height = getHeight() - 2;
                skin.setGraphicsColor(g, buttonToggleColor);
                g.fillRect(1, 1, width, height);
                skin.setGraphicsColor(g, buttonBorderColor);
                g.drawRect(1, 1, width - 1, height - 1);
            }
        }
    }
    
    public class LockTitleBarButton extends TitleBarButton {
        private LockTitleBarButton() {
            //Tooltip set in updateButtons()
        }
        @Override
        protected void onClick() {
            frame.setLockTitleBar(!frame.getLockTitleBar());
        }
        @Override
        protected boolean isToggled() {
            return frame.getLockTitleBar();
        }
        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);

            int knobWidth = 5;
            int knobHeight = 7;
            int pinHeight = 4;
            int centerX = getWidth() / 2;
            int x1 = centerX - knobWidth / 2;
            int x2 = x1 + knobWidth - 1;
            int y1 = (getHeight() - knobHeight) / 2 - 1;
            int y2 = y1 + knobHeight - 1;

            skin.setGraphicsColor(g, foreColor);
            
            g.drawRect(x1, y1, knobWidth - 1, knobHeight - 1);
            g.drawLine(x2 - 1, y1 + 1, x2 - 1, y2 - 1);
            g.drawLine(x1 - 1, y2, x2 + 1, y2);
            g.drawLine(centerX, y2, centerX, y2 + pinHeight);
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
    
    public class FullScreenButton extends TitleBarButton {
        private FullScreenButton() {
            //Tooltip set in updateButtons()
        }
        @Override
        protected void onClick() {
            frame.setFullScreen(!frame.isFullScreen());
        }
        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);

            int thickness = 2;
            int offset = 6;
            int arrowLength = 4;
            int x1 = offset;
            int y1 = offset;
            int x2 = getWidth() - offset - 1;
            int y2 = getHeight() - offset - 1;
            
            Graphics2D g2d = (Graphics2D) g;
            skin.setGraphicsColor(g2d, foreColor);

            if (frame.isFullScreen()) { //draw arrows facing inward
                g2d.drawLine(x1 + arrowLength, y1, x1 + arrowLength, y1 + arrowLength);
                g2d.drawLine(x1, y1 + arrowLength, x1 + arrowLength, y1 + arrowLength);
                g2d.drawLine(x2 - arrowLength, y1, x2 - arrowLength, y1 + arrowLength);
                g2d.drawLine(x2, y1 + arrowLength, x2 - arrowLength, y1 + arrowLength);
                g2d.drawLine(x1 + arrowLength, y2, x1 + arrowLength, y2 - arrowLength);
                g2d.drawLine(x1, y2 - arrowLength, x1 + arrowLength, y2 - arrowLength);
                g2d.drawLine(x2 - arrowLength, y2, x2 - arrowLength, y2 - arrowLength);
                g2d.drawLine(x2, y2 - arrowLength, x2 - arrowLength, y2 - arrowLength);
            }
            else { //draw arrows facing outward
                x1--; x2++; y1--; y2++;//temporary adjustment so arrows align with angled lines using anti-aliasing below
                g2d.drawLine(x1, y1, x1, y1 + arrowLength);
                g2d.drawLine(x1, y1, x1 + arrowLength, y1);
                g2d.drawLine(x2, y1, x2, y1 + arrowLength);
                g2d.drawLine(x2, y1, x2 - arrowLength, y1);
                g2d.drawLine(x1, y2, x1, y2 - arrowLength);
                g2d.drawLine(x1, y2, x1 + arrowLength, y2);
                g2d.drawLine(x2, y2, x2, y2 - arrowLength);
                g2d.drawLine(x2, y2, x2 - arrowLength, y2);
                x1++; x2--; y1++; y2--;
            }
            
            //draw angled lines for arrows
            arrowLength--; //decrease length to account for anti-aliasing
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setStroke(new BasicStroke(thickness));

            g2d.drawLine(x1, y1, x1 + arrowLength, y1 + arrowLength);
            g2d.drawLine(x2, y1, x2 - arrowLength, y1 + arrowLength);
            g2d.drawLine(x1, y2, x1 + arrowLength, y2 - arrowLength);
            g2d.drawLine(x2, y2, x2 - arrowLength, y2 - arrowLength);
        }
    }
    
    public class MaximizeButton extends TitleBarButton {
        private MaximizeButton() {
            //Tooltip set in updateButtons()
        }
        @Override
        protected void onClick() {
            frame.setMaximized(!frame.isMaximized());
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
            
            if (frame.isMaximized()) { //draw 2 rectangles offset if icon to restore window
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
