package forge.view;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsDevice.WindowTranslucency;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.PopupMenu;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.awt.geom.RoundRectangle2D;
import java.util.Stack;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;

import net.miginfocom.swing.MigLayout;
import forge.Singletons;
import forge.assets.FSkinProp;
import forge.toolbox.FMouseAdapter;
import forge.toolbox.FPanel;
import forge.toolbox.FSkin;
import forge.toolbox.FSkin.Colors;
import forge.toolbox.FSkin.CompoundSkinBorder;
import forge.toolbox.FSkin.LineSkinBorder;
import forge.toolbox.FSkin.SkinColor;
import forge.toolbox.FSkin.SkinnedDialog;
import forge.util.OperatingSystem;

@SuppressWarnings("serial")
public class FDialog extends SkinnedDialog implements ITitleBarOwner, KeyEventDispatcher {
    private static final int borderThickness = 3;
    private static final SkinColor borderColor = FSkin.getColor(FSkin.Colors.CLR_BORDERS);
    private static final int cornerDiameter = 20;
    private static final boolean isSetShapeSupported;
    private static final boolean antiAliasBorder;

    static {
        final GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        final GraphicsDevice gd = ge.getDefaultScreenDevice();
        isSetShapeSupported = gd.isWindowTranslucencySupported(WindowTranslucency.PERPIXEL_TRANSPARENT);

        //only apply anti-aliasing to border on Windows, as it creates issues on Linux
        antiAliasBorder = OperatingSystem.isWindows();
    }

    private Point locBeforeMove;
    private Dimension sizeBeforeResize;
    private Point mouseDownLoc;
    private int resizeCursor;
    private final FTitleBar titleBar;
    private final FPanel innerPanel;
    private JComponent defaultFocus;
    private final boolean allowResize;

    public FDialog() {
        this(true, false, "dialog");
    }

    public FDialog(final boolean modal0, final boolean allowResize0, final String insets) {
        super(JOptionPane.getRootFrame(), modal0);
        allowResize = allowResize0;
        setUndecorated(true);
        setIconImage(FSkin.getIcon(FSkinProp.ICO_FAVICON)); //use Forge icon by default

        innerPanel = new FPanel(new MigLayout("insets " + insets + ", gap 0, center, fill"));
        innerPanel.setBackgroundTexture(FSkin.getIcon(FSkinProp.BG_TEXTURE));
        innerPanel.setBackgroundTextureOverlay(FSkin.getColor(FSkin.Colors.CLR_THEME)); //use theme color as overlay to reduce background texture opacity
        innerPanel.setBorderToggle(false);
        innerPanel.setOpaque(false);
        super.setContentPane(innerPanel);

        titleBar = new FTitleBar(this);
        titleBar.setVisible(true);
        addMoveSupport();

        if (allowResize) {
            this.setBorder(new CompoundSkinBorder(
                    BorderFactory.createLineBorder(Color.BLACK, 1),
                    new LineSkinBorder(FSkin.getColor(Colors.CLR_BORDERS), borderThickness - 1)));
            addResizeSupport();
        }

        addWindowFocusListener(new WindowFocusListener() {
            @Override
            public void windowGainedFocus(final WindowEvent e) {
                if (FDialog.this.defaultFocus != null) {
                    FDialog.this.defaultFocus.grabFocus();
                    FDialog.this.defaultFocus = null; //reset default focused component so it doesn't receive focus if the dialog later loses then regains focus
                }
            }

            @Override
            public void windowLostFocus(final WindowEvent e) {
            }
        });

        if (isSetShapeSupported && !allowResize) { //if possible, set rounded rectangle shape for dialog
            addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(final ComponentEvent e) { //must update shape whenever dialog is resized
                    final int arc = cornerDiameter - 4; //leave room for border aliasing
                    FDialog.this.setShape(new RoundRectangle2D.Float(0, 0, FDialog.this.getWidth(), FDialog.this.getHeight(), arc, arc));
                }
            });
        }
    }

    //Make Escape key close dialog if allowed
    @Override
    public boolean dispatchKeyEvent(final KeyEvent e) {
        if (e.getID() == KeyEvent.KEY_PRESSED) {
            if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                final WindowEvent wev = new WindowEvent(this, WindowEvent.WINDOW_CLOSING);
                Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(wev);
                return true;
            }
        }
        return false;
    }

    @Override
    public void paint(final Graphics g) {
        super.paint(g);
        if (allowResize) { return; }

        //draw rounded border
        final Graphics2D g2d = (Graphics2D) g.create();
        if (antiAliasBorder) {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        }
        FSkin.setGraphicsColor(g2d, borderColor);
        if (isSetShapeSupported) {
            g2d.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, cornerDiameter, cornerDiameter);
        }
        else { //draw non-rounded border instead if setShape isn't supported
            g2d.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
        }
        g2d.dispose();
    }

    @Override
    public void dispose() {
        setVisible(false); //ensure overlay hidden when disposing
        super.dispose();
    }

    @Override
    public void setVisible(final boolean visible) {
        if (isVisible() == visible) { return; }

        if (visible) {
            FMouseAdapter.forceMouseUp(); //ensure mouse up handled if dialog shown between mouse down and mouse up

            if (openModals.isEmpty()) {
                setLocationRelativeTo(JOptionPane.getRootFrame());
            }
            else {
                setLocationRelativeTo(openModals.peek());
            }
            KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(this); //support handling keyboard shortcuts in dialog
            if (isModal()) {
                if (openModals.isEmpty()) {
                    backdropPanel.setVisible(true);
                    Singletons.getView().getNavigationBar().setMenuShortcutsEnabled(false);
                }
                openModals.push(this);
            }
        }
        else {
            KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(this);
            if (isModal()) {
                openModals.pop();
                if (openModals.isEmpty()) {
                    backdropPanel.setVisible(false);
                    Singletons.getView().getNavigationBar().setMenuShortcutsEnabled(true);
                }
            }
        }
        super.setVisible(visible);
    }

    public void setDefaultFocus(final JComponent comp) {
        defaultFocus = comp;
    }

    @Override
    public void setTitle(final String title) {
        super.setTitle(title);
        if (titleBar != null) {
            titleBar.setTitle(title);
        }
    }

    @Override
    public void setIconImage(final Image image) {
        super.setIconImage(image);
        if (titleBar != null) {
            titleBar.setIconImage(image);
        }
    }

    public boolean allowResize() {
        return allowResize;
    }

    public FTitleBar getTitleBar() {
        return titleBar;
    }

    //relay certain methods to the inner panel if it has been initialized
    @Override
    public void setContentPane(final Container contentPane) {
        if (innerPanel != null) {
            innerPanel.add(contentPane, "w 100%!, h 100%!");
        }
        super.setContentPane(contentPane);
    }

    @Override
    public Component add(final Component comp) {
        if (innerPanel != null) {
            return innerPanel.add(comp);
        }
        return super.add(comp);
    }

    @Override
    public void add(final PopupMenu popup) {
        if (innerPanel != null) {
            innerPanel.add(popup);
            return;
        }
        super.add(popup);
    }

    public void add(final Component comp, final int x, final int y, final int w, final int h) {
        add(comp, "x " + x + ", y " + y + ", w " + w + ", h " + h);
    }

    @Override
    public void add(final Component comp, final Object constraints) {
        if (innerPanel != null) {
            innerPanel.add(comp, constraints);
            return;
        }
        super.add(comp, constraints);
    }

    @Override
    public Component add(final Component comp, final int index) {
        if (innerPanel != null) {
            return innerPanel.add(comp, index);
        }
        return super.add(comp, index);
    }

    @Override
    public void add(final Component comp, final Object constraints, final int index) {
        if (innerPanel != null) {
            innerPanel.add(comp, constraints, index);
            return;
        }
        super.add(comp, constraints, index);
    }

    @Override
    public Component add(final String name, final Component comp) {
        if (innerPanel != null) {
            return innerPanel.add(name, comp);
        }
        return super.add(name, comp);
    }

    private void addMoveSupport() {
        titleBar.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    if (e.getClickCount() == 1) {
                        locBeforeMove = getLocation();
                        mouseDownLoc = e.getLocationOnScreen();
                    }
                }
            }
            @Override
            public void mouseReleased(final MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    locBeforeMove = null;
                    mouseDownLoc = null;
                }
            }
        });
        titleBar.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(final MouseEvent e) {
                if (mouseDownLoc != null) {
                    final Point loc = e.getLocationOnScreen();
                    final int dx = loc.x - mouseDownLoc.x;
                    final int dy = loc.y - mouseDownLoc.y;
                    setLocation(locBeforeMove.x + dx, locBeforeMove.y + dy);
                }
            }
        });
    }

    private void setResizeCursor(final int resizeCursor0) {
        resizeCursor = resizeCursor0;
        getRootPane().setCursor(Cursor.getPredefinedCursor(resizeCursor0));
    }

    public boolean isResizing() {
        return sizeBeforeResize != null;
    }

    private void addResizeSupport() {
        final JRootPane resizeBorders = getRootPane();
        resizeBorders.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent e) {
                if (resizeCursor != Cursor.DEFAULT_CURSOR && SwingUtilities.isLeftMouseButton(e)) {
                    locBeforeMove = getLocation();
                    sizeBeforeResize = getSize();
                    mouseDownLoc = e.getLocationOnScreen();
                }
            }
            @Override
            public void mouseReleased(final MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    locBeforeMove = null;
                    sizeBeforeResize = null;
                    mouseDownLoc = null;
                    setResizeCursor(Cursor.DEFAULT_CURSOR);
                }
            }
            @Override
            public void mouseExited(final MouseEvent e) {
                if (mouseDownLoc == null) {
                    setResizeCursor(Cursor.DEFAULT_CURSOR);
                }
            }
        });
        resizeBorders.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(final MouseEvent e) {
                if (mouseDownLoc == null) {
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
                    else if (loc.y >= getHeight() - grabArea) {
                        setResizeCursor(Cursor.S_RESIZE_CURSOR);
                    }
                    else {
                        setResizeCursor(Cursor.DEFAULT_CURSOR);
                    }
                }
            }
            @Override
            public void mouseDragged(final MouseEvent e) {
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
                final Dimension minSize = getMinimumSize();
                final Dimension maxSize = getMaximumSize();
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

    @Override
    public boolean isMinimized() {
        return false;
    }

    @Override
    public void setMinimized(final boolean b) {
    }

    @Override
    public boolean isMaximized() {
        return false;
    }

    @Override
    public void setMaximized(final boolean b) {
    }

    @Override
    public boolean isFullScreen() {
        return false;
    }

    @Override
    public void setFullScreen(final boolean b) {
    }

    @Override
    public boolean getLockTitleBar() {
        return false;
    }

    @Override
    public void setLockTitleBar(final boolean b) {
    }

    @Override
    public Image getIconImage() {
        return getIconImages().isEmpty() ? null : getIconImages().get(0);
    }

    private static final Stack<FDialog> openModals = new Stack<FDialog>();
    private static final BackdropPanel backdropPanel = new BackdropPanel();

    public static boolean isModalOpen() {
        return !openModals.isEmpty();
    }

    public static JPanel getBackdropPanel() {
        return backdropPanel;
    }

    private static class BackdropPanel extends JPanel {
        private static final SkinColor backColor = FSkin.getColor(FSkin.Colors.CLR_OVERLAY).alphaColor(120);

        private BackdropPanel() {
            setOpaque(false);
            setVisible(false);
            setFocusable(false);
        }

        @Override
        public void paintComponent(final Graphics g) {
            super.paintComponent(g);
            FSkin.setGraphicsColor(g, backColor);
            g.fillRect(0, 0, getWidth(), getHeight());
        }
    }
}