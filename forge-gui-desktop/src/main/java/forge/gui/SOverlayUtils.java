package forge.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;
import forge.Singletons;
import forge.assets.FSkinProp;
import forge.toolbox.FLabel;
import forge.toolbox.FOverlay;
import forge.toolbox.FPanel;
import forge.toolbox.FSkin;
import forge.toolbox.FSkin.SkinnedButton;

/**
 * All overlay interaction is handled here.
 *
 * <br><br><i>(S at beginning of class name denotes a static factory.)</i>
 */
public final class SOverlayUtils {

    /**
     * Private constructor to prevent instantiation.
     */
    private SOverlayUtils() {
    }

    /**
     * A standardized overlay for a game start condition.
     */
    public static void startGameOverlay() {
        startGameOverlay("Loading new game...");
    }
    public static void startGameOverlay(String message) {
        final JPanel overlay = SOverlayUtils.genericOverlay();
        final int w = overlay.getWidth();
        final int h = overlay.getHeight();
        final int pnlW = 400;
        final int pnlH = 300;
        final int labelHeight = 50;
        final int logoSize = pnlH - labelHeight;

        // Adds the "loading" panel to generic overlay container
        // (which is preset with null layout and close button)
        final FPanel pnl = new FPanel();
        pnl.setLayout(new MigLayout("insets 0, gap 0, ax center, wrap"));
        pnl.setBackground(FSkin.getColor(FSkin.Colors.CLR_ACTIVE).alphaColor(191));
        pnl.setBounds(new Rectangle(((w - pnlW) / 2), ((h - pnlH) / 2), pnlW, pnlH));

        pnl.add(new FLabel.Builder().icon(FSkin.getIcon(FSkinProp.ICO_LOGO))
                .iconScaleFactor(1d).iconInBackground().build(),
                "w " + logoSize + "px!, h " + logoSize + "px!, align center");
        pnl.add(new FLabel.Builder().text(message)
                .fontSize(22).build(), "h " + labelHeight + "px!, align center");

        overlay.add(pnl);
    }

    /**
     * A template overlay with close button, null layout, ready for anything.
     * @return {@link javax.swing.JPanel}
     */
    public static JPanel genericOverlay() {
        final JPanel overlay = FOverlay.SINGLETON_INSTANCE.getPanel();
        final int w = overlay.getWidth();

        final SkinnedButton btnCloseTopRight = new SkinnedButton("X");
        btnCloseTopRight.setBounds(w - 25, 10, 15, 15);
        btnCloseTopRight.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));
        btnCloseTopRight.setBorder(new FSkin.LineSkinBorder(FSkin.getColor(FSkin.Colors.CLR_TEXT)));
        btnCloseTopRight.setOpaque(false);
        btnCloseTopRight.setBackground(new Color(0, 0, 0));
        btnCloseTopRight.setFocusPainted(false);
        btnCloseTopRight.addActionListener(new ActionListener() { @Override
            public void actionPerformed(final ActionEvent arg0) { SOverlayUtils.hideOverlay(); } });

        overlay.removeAll();
        overlay.setLayout(null);
        overlay.add(btnCloseTopRight);

        return overlay;
    }

    private static boolean _overlayHasFocus;
    public static boolean overlayHasFocus() {
        return _overlayHasFocus;
    }

    private static Component prevFocusOwner;
    public static void showOverlay() {
        Singletons.getView().getNavigationBar().setEnabled(false);
        prevFocusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getPermanentFocusOwner();
        FOverlay.SINGLETON_INSTANCE.getPanel().setVisible(true);
        // ensure no background element has focus
        FOverlay.SINGLETON_INSTANCE.getPanel().requestFocusInWindow();
        _overlayHasFocus = true;
    }

    /**
     * Removes child components and closes overlay.
     */
    public static void hideOverlay() {
        Singletons.getView().getNavigationBar().setEnabled(true);
        FOverlay.SINGLETON_INSTANCE.getPanel().removeAll();
        FOverlay.SINGLETON_INSTANCE.getPanel().setVisible(false);
        if (null != prevFocusOwner) {
            prevFocusOwner.requestFocusInWindow();
            prevFocusOwner = null;
        }
        _overlayHasFocus = false;
    }

}
