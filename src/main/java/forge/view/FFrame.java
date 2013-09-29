package forge.view;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;

@SuppressWarnings("serial")
public class FFrame extends JFrame {
    private static final int borderThickness = 3;
    private Point locBeforeMove;
    private Dimension sizeBeforeResize;
    private Point mouseDownLoc;
    private int resizeCursor;
    private FTitleBar titleBar;
    private boolean maximized;
    private final JRootPane innerPane = new JRootPane();

    public FFrame() {
        setUndecorated(true);
        setContentPane(innerPane);
    }
    
    public void initialize() {
        // Frame border
        updateBorder();
        addResizeSupport();

        // Title bar
        this.titleBar = new FTitleBar(this);
        this.titleBar.setVisible(true);
        addMoveSupport();
    }

    public FTitleBar getTitleBar() {
        return this.titleBar;
    }
    
    @Override
    public void setTitle(String title) {
        super.setTitle(title);
        if (this.titleBar != null) {
            this.titleBar.setTitle(title);
        }
    }
    
    @Override
    public void setIconImage(Image image) {
        super.setIconImage(image);
        if (this.titleBar != null) {
            this.titleBar.setIconImage(image);
        }
    }
    
    public boolean getMinimized() {
        return getState() == Frame.ICONIFIED;
    }

    public void setMinimized(boolean minimized0) {
        if (minimized0) {
            setState(Frame.ICONIFIED);
        }
        else {
            setState(Frame.NORMAL);
        }
    }
    
    public boolean getMaximized() {
        return this.maximized;
    }
    
    public void setMaximized(boolean maximized0) {
        if (this.maximized == maximized0) { return; }
        if (maximized0) {
            this.setExtendedState(Frame.MAXIMIZED_BOTH);
        }
        else {
            this.setExtendedState(Frame.NORMAL);
        }
    }
    
    public void setExtendedState(int state) {
        super.setExtendedState(state);
        if (getMinimized()) { return; } //skip remaining logic while minimized

        this.maximized = (state == Frame.MAXIMIZED_BOTH);
        updateBorder();
        this.titleBar.handleMaximizedChanged(); //update icon and tooltip for maximize button
    }
    
    private void updateBorder() {
        if (this.maximized) {
            getRootPane().setBorder(null);
        }
        else {
            getRootPane().setBorder(BorderFactory.createLineBorder(new Color(0, 0, 61), borderThickness));
        }
    }
    
    public JRootPane getInnerPane() {
        return innerPane;
    }
    
    private void addMoveSupport() {
        this.titleBar.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    if (e.getClickCount() == 1) {
                        if (!maximized) { //TODO: consider supporting moving while maximized by first un-maximizing
                            locBeforeMove = getLocation();
                            mouseDownLoc = e.getLocationOnScreen();
                        }
                    }
                    else {
                        setMaximized(!getMaximized());
                    }
                }
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    locBeforeMove = null;
                    mouseDownLoc = null;
                }
            }
        });
        this.titleBar.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (mouseDownLoc != null) {
                    final Point loc = e.getLocationOnScreen();
                    final int dx = loc.x - mouseDownLoc.x;
                    final int dy = loc.y - mouseDownLoc.y;
                    setLocation(locBeforeMove.x + dx, locBeforeMove.y + dy);
                }
            }
        });
    }
    
    private void setResizeCursor(int resizeCursor0) {
        this.resizeCursor = resizeCursor0;
        this.getRootPane().setCursor(Cursor.getPredefinedCursor(resizeCursor0));
    }
    
    private void addResizeSupport() {
        final JRootPane resizeBorders = getRootPane();
        resizeBorders.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (resizeCursor != Cursor.DEFAULT_CURSOR && SwingUtilities.isLeftMouseButton(e)) {
                    locBeforeMove = getLocation();
                    sizeBeforeResize = getSize();
                    mouseDownLoc = e.getLocationOnScreen();
                }
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    locBeforeMove = null;
                    sizeBeforeResize = null;
                    mouseDownLoc = null;
                    setResizeCursor(Cursor.DEFAULT_CURSOR);
                }
            }
            @Override
            public void mouseExited(MouseEvent e) {
                if (mouseDownLoc == null) {
                    setResizeCursor(Cursor.DEFAULT_CURSOR);
                }
            }
        });
        resizeBorders.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                if (mouseDownLoc == null && !maximized) {
                    final int grabArea = borderThickness * 2;
                    final Point loc = e.getPoint();
                    if (loc.x < grabArea) {
                        if (loc.y < grabArea) {
                            setResizeCursor(Cursor.NW_RESIZE_CURSOR);
                        }
                        else if (loc.y >= getHeight() - grabArea) {
                            setResizeCursor(Cursor.SW_RESIZE_CURSOR);
                        }
                        else {
                            setResizeCursor(Cursor.W_RESIZE_CURSOR);
                        }
                    }
                    else if (loc.x >= getWidth() - grabArea) {
                        if (loc.y < grabArea) {
                            setResizeCursor(Cursor.NE_RESIZE_CURSOR);
                        }
                        else if (loc.y >= getHeight() - grabArea) {
                            setResizeCursor(Cursor.SE_RESIZE_CURSOR);
                        }
                        else {
                            setResizeCursor(Cursor.E_RESIZE_CURSOR);
                        }
                    }
                    else if (loc.y < grabArea) {
                        setResizeCursor(Cursor.N_RESIZE_CURSOR);
                    }
                    else {
                        setResizeCursor(Cursor.S_RESIZE_CURSOR);
                    }
                }
            }
            @Override
            public void mouseDragged(MouseEvent e) {
                if (mouseDownLoc == null) { return; }

                final Point loc = e.getLocationOnScreen();
                int dx = loc.x - mouseDownLoc.x;
                int dy = loc.y - mouseDownLoc.y;
                
                //determine new size based on resize direction
                int width = sizeBeforeResize.width;
                int height = sizeBeforeResize.height;
                switch (resizeCursor) {
                    case Cursor.E_RESIZE_CURSOR:
                        width += dx;
                        break;
                    case Cursor.W_RESIZE_CURSOR:
                        width -= dx;
                        break;
                    case Cursor.S_RESIZE_CURSOR:
                        height += dy;
                        break;
                    case Cursor.N_RESIZE_CURSOR:
                        height -= dy;
                        break;
                    case Cursor.SE_RESIZE_CURSOR:
                        width += dx;
                        height += dy;
                        break;
                    case Cursor.NE_RESIZE_CURSOR:
                        width += dx;
                        height -= dy;
                        break;
                    case Cursor.SW_RESIZE_CURSOR:
                        width -= dx;
                        height += dy;
                        break;
                    case Cursor.NW_RESIZE_CURSOR:
                        width -= dx;
                        height -= dy;
                        break;
                }
                
                //ensure new size in bounds
                Dimension minSize = getMinimumSize();
                Dimension maxSize = getMaximumSize();
                if (width < minSize.width) {
                    dx += (width - minSize.width);
                    width = minSize.width;
                }
                else if (width > maxSize.width) {
                    dx -= (width - maxSize.width);
                    width = maxSize.width;
                }
                if (height < minSize.height) {
                    dy += (height - minSize.height);
                    height = minSize.height;
                }
                else if (height > maxSize.height) {
                    dy -= (height - maxSize.height);
                    height = maxSize.height;
                }
                
                //determine new location based on resize direction
                int x = locBeforeMove.x;
                int y = locBeforeMove.y;
                switch (resizeCursor) {
                    case Cursor.W_RESIZE_CURSOR:
                    case Cursor.SW_RESIZE_CURSOR:
                        x += dx;
                        break;
                    case Cursor.N_RESIZE_CURSOR:
                    case Cursor.NE_RESIZE_CURSOR:
                        y += dy;
                        break;
                    case Cursor.NW_RESIZE_CURSOR:
                        x += dx;
                        y += dy;
                        break;
                }
                
                //set bounds based on new size and location
                setBounds(x, y, width, height);
            }
        });
    }
}
