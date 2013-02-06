package forge.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.border.LineBorder;

import net.miginfocom.swing.MigLayout;
import forge.gui.match.TargetingOverlay;
import forge.gui.toolbox.FLabel;
import forge.gui.toolbox.FOverlay;
import forge.gui.toolbox.FPanel;
import forge.gui.toolbox.FSkin;

/** 
 * All overlay interaction is handled here.
 *
 * <br><br><i>(S at beginning of class name denotes a static factory.)</i>
 */
public final class SOverlayUtils {
    private static int counter = 0;

    /** 
     * A standardized overlay for a game start condition.
     */
    public static void startGameOverlay() {
        final JPanel overlay = SOverlayUtils.genericOverlay();
        final int w = overlay.getWidth();
        final int h = overlay.getHeight();
        final int pnlW = 400;
        final int pnlH = 300;

        // Adds the "loading" panel to generic overlay container
        // (which is preset with null layout and close button)
        final FPanel pnl = new FPanel();
        pnl.setLayout(new MigLayout("insets 0, gap 0, ax center, wrap"));
        pnl.setBackground(FSkin.getColor(FSkin.Colors.CLR_ACTIVE));
        pnl.setBounds(new Rectangle(((w - pnlW) / 2), ((h - pnlH) / 2), pnlW, pnlH));

        pnl.add(new FLabel.Builder().icon(FSkin.getIcon(FSkin.InterfaceIcons.ICO_LOGO)).build(),
                "h 200px!, align center");
        pnl.add(new FLabel.Builder().text("Loading new game...")
                .fontSize(22).build(), "h 40px!, align center");

        overlay.add(pnl);
    }

    /**
     * A standardized overlay for a loading condition (note: thread issues, as of 1-Mar-12).
     * @param msg0 &emsp; {@link java.lang.String}
     * @return {@link javax.swing.JPanel}
     */
    // NOTE: This animation happens on the EDT; if the EDT is tied up doing something
    // else, the animation is effectively frozen.  So, this needs some work.
    public static JPanel loadingOverlay(final String msg0) {
        final JPanel overlay = SOverlayUtils.genericOverlay();
        final FPanel pnlLoading = new FPanel();
        final int w = overlay.getWidth();
        final int h = overlay.getHeight();

        final JLabel lblLoading = new JLabel("");
        lblLoading.setOpaque(true);
        lblLoading.setBackground(FSkin.getColor(FSkin.Colors.CLR_TEXT));
        lblLoading.setMinimumSize(new Dimension(0, 20));

        pnlLoading.setBounds(((w - 170) / 2), ((h - 80) / 2), 170, 80);
        pnlLoading.setLayout(new MigLayout("wrap, align center"));
        pnlLoading.add(new FLabel.Builder().fontSize(18)
                .text(msg0).build(), "h 20px!, w 140px!, gap 0 0 5px 0");
        pnlLoading.add(lblLoading, "gap 0 0 0 10px");

        overlay.add(pnlLoading);

        SOverlayUtils.counter = 0;
        final Timer timer = new Timer(300, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                lblLoading.setMinimumSize(new Dimension(10 * (SOverlayUtils.counter++), 20));
                lblLoading.revalidate();
                if (SOverlayUtils.counter > 13) { SOverlayUtils.counter = 0; }
            }
        });
        timer.start();

        return overlay;
    }

    /** 
     * A template overlay with close button, null layout, ready for anything.
     * @return {@link javax.swing.JPanel}
     */
    public static JPanel genericOverlay() {
        final JPanel overlay = FOverlay.SINGLETON_INSTANCE.getPanel();
        final int w = overlay.getWidth();

        final JButton btnCloseTopRight = new JButton("X");
        btnCloseTopRight.setBounds(w - 25, 10, 15, 15);
        btnCloseTopRight.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));
        btnCloseTopRight.setBorder(new LineBorder(FSkin.getColor(FSkin.Colors.CLR_TEXT), 1));
        btnCloseTopRight.setOpaque(false);
        btnCloseTopRight.setBackground(new Color(0, 0, 0));
        btnCloseTopRight.setFocusPainted(false);
        btnCloseTopRight.addActionListener(new ActionListener() { @Override
            public void actionPerformed(ActionEvent arg0) { SOverlayUtils.hideOverlay(); } });

        overlay.removeAll();
        overlay.setLayout(null);
        overlay.add(btnCloseTopRight);

        return overlay;
    }

    public static void showOverlay() {
        FOverlay.SINGLETON_INSTANCE.getPanel().setVisible(true);
        // ensure no background element has focus
        FOverlay.SINGLETON_INSTANCE.getPanel().requestFocusInWindow();
    }

    /**
     * Removes child components and closes overlay.
     */
    public static void hideOverlay() {
        FOverlay.SINGLETON_INSTANCE.getPanel().removeAll();
        FOverlay.SINGLETON_INSTANCE.getPanel().setVisible(false);
    }

    public static void showTargetingOverlay() {
        TargetingOverlay.SINGLETON_INSTANCE.getPanel().setVisible(true);
    }

    /**
     * Removes child components and closes overlay.
     */
    public static void hideTargetingOverlay() {
        TargetingOverlay.SINGLETON_INSTANCE.getPanel().setVisible(false);
    }
}
