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
import forge.properties.ForgePreferences;
import forge.properties.ForgePreferences.FPref;

/**
 * Shows the splash frame as the application starts.
 */
@SuppressWarnings("serial")
public class SplashFrame extends JFrame {
    private FProgressBar barProgress;

    private final MouseAdapter madClose = new MouseAdapter() {
        @Override
        public void mouseEntered(final java.awt.event.MouseEvent evt) {
            ((JButton) evt.getSource()).setBorder(BorderFactory.createLineBorder(Color.white));
            ((JButton) evt.getSource()).setForeground(Color.white);
        }

        @Override
        public void mouseExited(final java.awt.event.MouseEvent evt) {
            ((JButton) evt.getSource()).setBorder(BorderFactory.createLineBorder(new Color(215, 208, 188)));
            ((JButton) evt.getSource()).setForeground(new Color(215, 208, 188));
        }
    };

    private final Action actClose = new AbstractAction() {
        @Override
        public void actionPerformed(final ActionEvent e) {
            System.exit(0);
        }
    };

    /**
     * Create the frame; this <strong>must</strong> be called from an event
     * dispatch thread.
     * 
     */
    public SplashFrame() {
        super();
        final ForgePreferences prefs = new ForgePreferences();
        FSkin.loadLight(prefs.getPref(FPref.UI_SKIN));

        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    SplashFrame.this.init();
                }
            });
        }
        catch (Exception e) {
        }
    }

    private void init() {
        final ImageIcon bgIcon = FSkin.getIcon(FSkin.Backgrounds.BG_SPLASH);
        barProgress = new FProgressBar();

        this.setUndecorated(true);
        this.setMinimumSize(new Dimension(450, 450));
        this.setLocationRelativeTo(null);
        this.setResizable(false);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setTitle("Loading Forge...");

        // Insert JPanel to hold content above background
        final JPanel pnlContent = new JPanel();
        this.setContentPane(pnlContent);
        pnlContent.setLayout(null);

        // Disclaimer
        final JLabel lblDisclaimer = new JLabel("<html><center>"
                + "Forge is not affiliated in any way with Wizards of the Coast."
                + "<br>Forge is open source software, released under "
                + "the GNU Public License.</center></html>");

        lblDisclaimer.setBounds(0, 300, 450, 20);
        lblDisclaimer.setFont(new Font("Tahoma", Font.PLAIN, 9));
        lblDisclaimer.setHorizontalAlignment(SwingConstants.CENTER);
        lblDisclaimer.setForeground(UIManager.getColor("ProgressBar.selectionForeground"));
        pnlContent.add(lblDisclaimer);

        // Close button
        final JButton btnClose = new JButton("X");
        btnClose.setBounds(420, 15, 15, 15);
        btnClose.setForeground(new Color(215, 208, 188));
        btnClose.setBorder(BorderFactory.createLineBorder(new Color(215, 208, 188)));
        btnClose.setOpaque(false);
        btnClose.setBackground(new Color(0, 0, 0));
        btnClose.setFocusPainted(false);
        pnlContent.add(btnClose);

        btnClose.addMouseListener(madClose);
        btnClose.addActionListener(actClose);
        pnlContent.getInputMap().put(KeyStroke.getKeyStroke("ESCAPE"), "escAction");
        pnlContent.getActionMap().put("escAction", actClose);

        barProgress.setString("Welcome to Forge.");
        barProgress.setBounds(20, 373, 410, 57);
        pnlContent.add(barProgress);

        final JLabel bgLabel = new JLabel(bgIcon);
        bgLabel.setBounds(0, 0, 450, 450);
        pnlContent.add(bgLabel);

        this.pack();
        this.setVisible(true);
    }

    /** @return {@link forge.gui.toolbox.FProgressBar} */
    public FProgressBar getProgressBar() {
        return this.barProgress;
    }
}
