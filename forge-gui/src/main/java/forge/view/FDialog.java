package forge.view;

import java.awt.Color;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import forge.gui.toolbox.FSkin;
import forge.gui.toolbox.FSkin.Colors;
import forge.gui.toolbox.FSkin.CompoundSkinBorder;
import forge.gui.toolbox.FSkin.LineSkinBorder;

@SuppressWarnings("serial")
public class FDialog extends JDialog implements ITitleBarOwner {
    private static final int borderThickness = 3;
    private Point locBeforeMove;
    private Point mouseDownLoc;
    private final FTitleBar titleBar;

    public FDialog() {
        this(true);
    }

    public FDialog(boolean modal0) {
        super(JOptionPane.getRootFrame(), modal0);
        this.setUndecorated(true);
        FSkin.get(this).setIconImage(FSkin.getIcon(FSkin.InterfaceIcons.ICO_FAVICON)); //use Forge icon by default
        titleBar = new FTitleBar(this);
        titleBar.setVisible(true);
        addMoveSupport();
        FSkin.get(getRootPane()).setBorder(new CompoundSkinBorder(
                BorderFactory.createLineBorder(Color.BLACK, 1),
                new LineSkinBorder(FSkin.getColor(Colors.CLR_BORDERS), borderThickness - 1)));
    }

    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            setLocationRelativeTo(JOptionPane.getRootFrame());
        }
        super.setVisible(visible);
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

    private void addMoveSupport() {
        this.titleBar.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    if (e.getClickCount() == 1) {
                        locBeforeMove = getLocation();
                        mouseDownLoc = e.getLocationOnScreen();
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

    @Override
    public boolean isMinimized() {
        return false;
    }

    @Override
    public void setMinimized(boolean b) {
    }

    @Override
    public boolean isMaximized() {
        return false;
    }

    @Override
    public void setMaximized(boolean b) {
    }

    @Override
    public boolean isFullScreen() {
        return false;
    }

    @Override
    public void setFullScreen(boolean b) {
    }

    @Override
    public boolean getLockTitleBar() {
        return false;
    }

    @Override
    public void setLockTitleBar(boolean b) {
    }

    @Override
    public Image getIconImage() {
        return getIconImages().isEmpty() ? null : getIconImages().get(0);
    }
}