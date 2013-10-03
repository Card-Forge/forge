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

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.SpringLayout;
import javax.swing.SwingUtilities;

import forge.gui.framework.ILocalRepaint;
import forge.gui.toolbox.FDigitalClock;
import forge.gui.toolbox.FSkin;
import forge.gui.toolbox.FSkin.Colors;
import forge.gui.toolbox.FSkin.JLabelSkin;
import forge.gui.toolbox.FSkin.SkinColor;

@SuppressWarnings("serial")
public class FTitleBar extends JMenuBar {
    private static final SkinColor foreColor = FSkin.getColor(Colors.CLR_TEXT);
    private static final SkinColor backColor = FSkin.getColor(Colors.CLR_THEME2);
    private static final SkinColor buttonHoverColor = backColor.stepColor(40);
    private static final SkinColor buttonDownColor = backColor.stepColor(-40);

    private final FFrame frame;
    private final SpringLayout layout = new SpringLayout();
    private final JLabel lblTitle = new JLabel();
    private final FDigitalClock clock = new FDigitalClock();
    private final MinimizeButton btnMinimize = new MinimizeButton();
    private final MaximizeButton btnMaximize = new MaximizeButton();
    private final CloseButton btnClose = new CloseButton();

    public FTitleBar(FFrame f) {
        this.frame = f;
        f.setJMenuBar(this);
        setPreferredSize(new Dimension(f.getWidth(), 26));
        setLayout(this.layout);
        FSkin.get(this).setBackground(backColor);
        setTitle(f.getTitle()); //set default title based on frame title
        setIconImage(f.getIconImage()); //set default icon image based on frame icon image
        FSkin.get(lblTitle).setForeground(foreColor);
        FSkin.get(clock).setForeground(foreColor);

        add(lblTitle);
        layout.putConstraint(SpringLayout.WEST, lblTitle, 1, SpringLayout.WEST, this);
        layout.putConstraint(SpringLayout.NORTH, lblTitle, 4, SpringLayout.NORTH, this);

        add(btnClose);
        layout.putConstraint(SpringLayout.EAST, btnClose, 0, SpringLayout.EAST, this);
        layout.putConstraint(SpringLayout.NORTH, btnClose, 0, SpringLayout.NORTH, this);

        add(btnMaximize);
        layout.putConstraint(SpringLayout.EAST, btnMaximize, 0, SpringLayout.WEST, btnClose);
        layout.putConstraint(SpringLayout.NORTH, btnMaximize, 0, SpringLayout.NORTH, btnClose);
        
        add(btnMinimize);
        layout.putConstraint(SpringLayout.EAST, btnMinimize, 0, SpringLayout.WEST, btnMaximize);
        layout.putConstraint(SpringLayout.NORTH, btnMinimize, 0, SpringLayout.NORTH, btnMaximize);
        
        add(clock);
        clock.setVisible(false); //hide unless maximized
        layout.putConstraint(SpringLayout.EAST, clock, -6, SpringLayout.WEST, btnMinimize);
        layout.putConstraint(SpringLayout.NORTH, clock, 0, SpringLayout.NORTH, lblTitle);
    }
    
    public void handleMaximizedChanged() {
        clock.setVisible(frame.getMaximized());
        if (frame.getMaximized()) {
            btnMaximize.setToolTipText("Restore Down");
        }
        else {
            btnMaximize.setToolTipText("Maximize");
        }
        btnMaximize.repaintSelf();
    }
    
    public String getTitle() {
        return this.lblTitle.getText();
    }

    public void setTitle(String title) {
        this.lblTitle.setText(title);
    }
    
    public void setIconImage(Image image) {
        if (image != null) {
            this.lblTitle.setIcon(new ImageIcon(image.getScaledInstance(16, 16, Image.SCALE_AREA_AVERAGING)));
        }
        else {
            this.lblTitle.setIcon(null);
        }
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
