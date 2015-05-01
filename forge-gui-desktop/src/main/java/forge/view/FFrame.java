package forge.view;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowStateListener;

import javax.swing.BorderFactory;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;

import forge.Singletons;
import forge.gui.framework.SDisplayUtil;
import forge.gui.framework.SResizingUtil;
import forge.match.HostedMatch;
import forge.model.FModel;
import forge.properties.ForgePreferences;
import forge.properties.ForgePreferences.FPref;
import forge.sound.SoundSystem;
import forge.toolbox.FSkin;
import forge.toolbox.FSkin.Colors;
import forge.toolbox.FSkin.CompoundSkinBorder;
import forge.toolbox.FSkin.LineSkinBorder;
import forge.toolbox.FSkin.SkinnedFrame;

@SuppressWarnings("serial")
public class FFrame extends SkinnedFrame implements ITitleBarOwner {
    private static final int borderThickness = 3;
    private Point locBeforeMove;
    private Dimension sizeBeforeResize;
    private Point mouseDownLoc;
    private boolean moveInProgress;
    private int resizeCursor;
    private FTitleBarBase titleBar;
    private boolean minimized, maximized, fullScreen, hideBorder, lockTitleBar, hideTitleBar, isMainFrame, paused;
    private Rectangle normalBounds;

    public FFrame() {
        setUndecorated(true);
    }

    public void initialize(final FTitleBarBase titleBar0) {
        this.isMainFrame = (FView.SINGLETON_INSTANCE.getFrame() == this);

        // Frame border
        this.hideBorder = true; //ensure border shown when window layout loaded
        this.hideTitleBar = true; //ensure titlebar shown when window layout loaded
        this.lockTitleBar = this.isMainFrame && FModel.getPreferences().getPrefBoolean(FPref.UI_LOCK_TITLE_BAR);
        addResizeSupport();
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowActivated(final WindowEvent e) {
                resume(); //resume music when main frame regains focus
            }

            @Override
            public void windowDeactivated(final WindowEvent e) {
                if (e.getOppositeWindow() == null) {
                    pause(); //pause music when main frame loses focus to outside application

                    if (fullScreen) {
                        setMinimized(true); //minimize if switching from Full Screen Forge to outside application window
                    }
                }
            }
        });
        this.addWindowStateListener(new WindowStateListener() {
            @Override
            public void windowStateChanged(final WindowEvent e) {
                setState(e.getNewState());
            }
        });

        // Title bar
        this.titleBar = titleBar0;
        addMoveSupport();
    }

    private void pause() {
        if (paused || !isMainFrame) { return; }

        // Pause the sound
        SoundSystem.instance.pause();

        // Pause all hosted matches
        for (final HostedMatch hostedMatch : Singletons.getControl().getCurrentMatches()) {
            hostedMatch.pause();
        }
        paused = true;
    }

    private void resume() {
        if (!paused || !isMainFrame) { return; }

        // Resume the sound
        SoundSystem.instance.resume();

        // Resume all hosted matches
        for (final HostedMatch hostedMatch : Singletons.getControl().getCurrentMatches()) {
            hostedMatch.resume();
        }
        paused = false;
    }

    public FTitleBarBase getTitleBar() {
        return this.titleBar;
    }

    @Override
    public boolean getLockTitleBar() {
        return this.lockTitleBar;
    }

    @Override
    public void setLockTitleBar(final boolean lockTitleBar0) {
        if (this.lockTitleBar == lockTitleBar0) { return; }
        this.lockTitleBar = lockTitleBar0;
        if (this.isMainFrame) {
            final ForgePreferences prefs = FModel.getPreferences();
            prefs.setPref(FPref.UI_LOCK_TITLE_BAR, lockTitleBar0);
            prefs.save();
        }
        updateTitleBar();
    }

    public boolean isTitleBarHidden() {
        return this.hideTitleBar;
    }

    private void updateTitleBar() {
        this.titleBar.updateButtons();
        if (this.hideTitleBar == (this.fullScreen && !this.lockTitleBar)) {
            return;
        }
        this.hideTitleBar = !this.hideTitleBar;
        this.titleBar.setVisible(!this.hideTitleBar);
        if (this.isMainFrame) {
            SResizingUtil.resizeWindow(); //ensure window layout updated to account for titlebar visibility change
        }
    }

    @Override
    public void setTitle(final String title) {
        super.setTitle(title);
        if (this.titleBar != null) {
            this.titleBar.setTitle(title);
        }
    }

    @Override
    public void setIconImage(final Image image) {
        super.setIconImage(image);
        if (this.titleBar != null) {
            this.titleBar.setIconImage(image);
        }
    }

    //ensure un-maximized if location or size changed
    @Override
    public void setLocation(final Point point) {
        resetState();
        super.setLocation(point);
    }
    @Override
    public void setLocation(final int x, final int y) {
        resetState();
        super.setLocation(x, y);
    }
    @Override
    public void setSize(final Dimension size) {
        resetState();
        super.setSize(size);
    }
    @Override
    public void setSize(final int width, final int height) {
        resetState();
        super.setSize(width, height);
    }

    private void resetState() {
        if (this.minimized || this.maximized || this.fullScreen) {
            this.minimized = false;
            this.maximized = false;
            if (this.fullScreen) { //need to cancel full screen here
                SDisplayUtil.setFullScreenWindow(this, false);
                this.fullScreen = false;
            }
            updateState();
        }
    }

    public void setWindowLayout(final int x, final int y, final int width, final int height, final boolean maximized0, final boolean fullScreen0) {
        this.normalBounds = new Rectangle(x, y, width, height);
        this.maximized = maximized0;
        this.fullScreen = fullScreen0;
        updateState();
    }

    public Rectangle getNormalBounds() {
        return this.normalBounds;
    }

    public void updateNormalBounds() {
        if (this.minimized || this.maximized || this.fullScreen) {
            return;
        }
        this.normalBounds = this.getBounds();
    }

    @Override
    public boolean isMinimized() {
        return this.minimized;
    }

    @Override
    public void setMinimized(final boolean minimized0) {
        if (this.minimized == minimized0) { return; }
        this.minimized = minimized0;
        updateState();

        //pause or resume when minimized changes
        if (minimized0) {
            pause();
        }
        else {
            resume();
        }
    }

    @Override
    public boolean isMaximized() {
        return this.maximized;
    }

    @Override
    public void setMaximized(final boolean maximized0) {
        if (this.maximized == maximized0) { return; }
        this.maximized = maximized0;
        updateState();
    }

    @Override
    public boolean isFullScreen() {
        return this.fullScreen;
    }

    @Override
    public void setFullScreen(final boolean fullScreen0) {
        if (this.fullScreen == fullScreen0) { return; }
        this.fullScreen = fullScreen0;
        if (!fullScreen0) { //cancel full screen here instead of updateState
            SDisplayUtil.setFullScreenWindow(this, false);
        }
        updateState();
    }

    private void updateState() {
        if (this.minimized) {
            super.setExtendedState(Frame.ICONIFIED);
            return;
        }
        updateBorder();
        updateTitleBar();

        super.setExtendedState(Frame.NORMAL);

        if (this.fullScreen) {
            if (SDisplayUtil.setFullScreenWindow(this, true)) {
                return; //nothing else needed if full-screen successful
            }
            this.fullScreen = false; //reset if full screen failed
            updateBorder(); //ensure border updated for non-full screen if needed
            updateTitleBar(); //ensure titlebar updated for non-full screen if needed
        }

        if (this.maximized) {
            this.setBounds(SDisplayUtil.getScreenMaximizedBounds(this.normalBounds));
        }
        else {
            this.setBounds(this.normalBounds);
        }
    }

    private void updateBorder() {
        if (this.minimized || this.hideBorder == (this.maximized || this.fullScreen)) {
            return; //don't update border if minimized or border visibility wouldn't change
        }
        this.hideBorder = !this.hideBorder;
        if (this.hideBorder) {
            this.setBorder((Border)null);
        }
        else {
            this.setBorder(new CompoundSkinBorder(
                    BorderFactory.createLineBorder(Color.BLACK, 1),
                    new LineSkinBorder(FSkin.getColor(Colors.CLR_BORDERS), borderThickness - 1)));
        }
    }

    //override normal state behavior
    @Override
    public synchronized void setState(final int state) {
        setMinimized(state == Frame.ICONIFIED);
        if (state == Frame.MAXIMIZED_BOTH) {
            this.setMaximized(true);
        }
    }

    //override normal extended state behavior
    @Override
    public void setExtendedState(final int state) {
        if (this.isActive()) { //only update state set this way when active, otherwise window will be minimized when deactivated
            this.minimized = (state & Frame.ICONIFIED) == Frame.ICONIFIED;
            this.maximized = (state & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH;
            updateState();
        }
    }

    private void addMoveSupport() {
        this.titleBar.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && !fullScreen) { //don't allow moving or restore down when Full Screen
                    if (e.getClickCount() == 1) {
                        locBeforeMove = getLocation();
                        mouseDownLoc = e.getLocationOnScreen();
                    }
                    else {
                        setMaximized(!isMaximized());
                    }
                }
            }
            @Override
            public void mouseReleased(final MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    locBeforeMove = null;
                    mouseDownLoc = null;
                    moveInProgress = false;
                }
            }
        });
        this.titleBar.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(final MouseEvent e) {
                if (mouseDownLoc != null) {
                    final Point loc = e.getLocationOnScreen();
                    final int dx = loc.x - mouseDownLoc.x;
                    final int dy = loc.y - mouseDownLoc.y;
                    if (!moveInProgress) {
                        if (isMaximized() && dx * dx + dy * dy < 25) {
                            //don't start frame move if maximized until you've moved the mouse at least than 5 pixels
                            return;
                        }
                        moveInProgress = true;
                    }
                    setLocation(locBeforeMove.x + dx, locBeforeMove.y + dy);
                }
            }
        });
    }

    private void setResizeCursor(final int resizeCursor0) {
        this.resizeCursor = resizeCursor0;
        this.getRootPane().setCursor(Cursor.getPredefinedCursor(resizeCursor0));
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
}
