/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.view;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import forge.gui.toolbox.FProgressBar;
import forge.gui.toolbox.FSkin;

/**
 * Shows the splash frame as the application starts.
 */
@SuppressWarnings("serial")
public class SplashFrame extends JFrame {
    private static final int BAR_PADDING_X = 20;
    private static final int BAR_PADDING_Y = 20;
    private static final int BAR_HEIGHT = 57;

    private static final int DISCLAIMER_HEIGHT = 20;
    private static final int DISCLAIMER_TOP = 300;
    private static final int DISCLAIMER_FONT_SIZE = 9;

    private static final int CLOSEBTN_PADDING_Y = 15;
    private static final int CLOSEBTN_SIDELENGTH = 15;
    private static final Color CLOSEBTN_COLOR = new Color(215, 208, 188);

    /** Preload bar, static accessible. */
    public static final FProgressBar PROGRESS_BAR = new FProgressBar();

    /**
     * Create the frame; this <strong>must</strong> be called from an event
     * dispatch thread.
     * 
     */
    public SplashFrame() {
        super();

        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException(
                    "SplashFrame() must be called from an event dispatch thread.");
        }

        final ImageIcon bgIcon = FSkin.getIcon(FSkin.Backgrounds.BG_SPLASH);
        final int splashWidthPx = bgIcon.getIconWidth();
        final int splashHeightPx = bgIcon.getIconHeight();

        this.setUndecorated(true);
        this.setMinimumSize(new Dimension(splashWidthPx, splashHeightPx));
        this.setLocationRelativeTo(null);
        this.setResizable(false);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setTitle("Loading Forge...");

        // Insert JPanel to hold content above background
        final JPanel pnlContent = new JPanel();
        this.setContentPane(pnlContent);
        pnlContent.setLayout(null);

        // Add disclaimer
        final JLabel lblDisclaimer = new JLabel("<html><center>"
                + "Forge is not affiliated in any way with Wizards of the Coast."
                + "<br>Forge is open source software, released under "
                + "the GNU Public License.</center></html>");

        lblDisclaimer.setBounds(0, SplashFrame.DISCLAIMER_TOP, splashWidthPx, SplashFrame.DISCLAIMER_HEIGHT);

        lblDisclaimer.setFont(new Font("Tahoma", Font.PLAIN, SplashFrame.DISCLAIMER_FONT_SIZE));
        lblDisclaimer.setHorizontalAlignment(SwingConstants.CENTER);
        lblDisclaimer.setForeground(UIManager.getColor("ProgressBar.selectionForeground"));
        pnlContent.add(lblDisclaimer);

        // Add close button
        final JButton btnClose = new JButton("X");
        btnClose.setBounds(splashWidthPx - (2 * SplashFrame.CLOSEBTN_PADDING_Y), SplashFrame.CLOSEBTN_PADDING_Y,
                SplashFrame.CLOSEBTN_SIDELENGTH, SplashFrame.CLOSEBTN_SIDELENGTH);
        btnClose.setForeground(SplashFrame.CLOSEBTN_COLOR);
        btnClose.setBorder(BorderFactory.createLineBorder(SplashFrame.CLOSEBTN_COLOR));
        btnClose.setOpaque(false);
        btnClose.setBackground(new Color(0, 0, 0));
        btnClose.setFocusPainted(false);
        pnlContent.add(btnClose);

        // Actions and listeners for close button (also mapped to ESC)
        final MouseAdapter madClose = new MouseAdapter() {
            @Override
            public void mouseEntered(final java.awt.event.MouseEvent evt) {
                btnClose.setBorder(BorderFactory.createLineBorder(Color.white));
                btnClose.setForeground(Color.white);
            }

            @Override
            public void mouseExited(final java.awt.event.MouseEvent evt) {
                btnClose.setBorder(BorderFactory.createLineBorder(SplashFrame.CLOSEBTN_COLOR));
                btnClose.setForeground(SplashFrame.CLOSEBTN_COLOR);
            }
        };

        final Action actClose = new AbstractAction() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                System.exit(0);
            }
        };

        btnClose.addMouseListener(madClose);
        btnClose.addActionListener(actClose);
        pnlContent.getInputMap().put(KeyStroke.getKeyStroke("ESCAPE"), "escAction");
        pnlContent.getActionMap().put("escAction", actClose);

        PROGRESS_BAR.setString("Welcome to Forge.");
        PROGRESS_BAR.setBounds(SplashFrame.BAR_PADDING_X, splashHeightPx - SplashFrame.BAR_PADDING_Y
                - SplashFrame.BAR_HEIGHT, splashWidthPx - (2 * SplashFrame.BAR_PADDING_X), SplashFrame.BAR_HEIGHT);
        pnlContent.add(PROGRESS_BAR);

        final JLabel bgLabel = new JLabel(bgIcon);
        bgLabel.setBounds(0, 0, splashWidthPx, splashHeightPx);
        pnlContent.add(bgLabel);

        this.pack();
        this.setVisible(true);
    }
}
