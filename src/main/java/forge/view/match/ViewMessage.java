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
package forge.view.match;

import java.awt.Color;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.MatteBorder;

import net.miginfocom.swing.MigLayout;
import forge.control.match.ControlMessage;
import forge.view.toolbox.FButton;
import forge.view.toolbox.FPanel;
import forge.view.toolbox.FSkin;

/**
 * Assembles Swing components of input area.
 * 
 */
@SuppressWarnings("serial")
public class ViewMessage extends FPanel {
    private final ControlMessage control;
    private final JButton btnOK, btnCancel;
    private final JTextArea tarMessage;
    private final JLabel lblGames;
    private Timer timer1 = null;
    private static int counter = 0;
    private int[] newA = null, newR = null, newG = null, newB = null;
    private boolean remindIsRunning = false;

    /**
     * Assembles UI for input area (buttons and message panel).
     * 
     */
    public ViewMessage() {
        super();
        this.setToolTipText("Input Area");
        this.setLayout(new MigLayout("wrap 2, fill, insets 0, gap 0"));

        // Cancel button
        this.btnCancel = new FButton("Cancel");
        this.btnOK = new FButton("OK");

        // Game counter
        lblGames = new JLabel();
        lblGames.setFont(FSkin.getBoldFont(12));
        lblGames.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));
        lblGames.setHorizontalAlignment(SwingConstants.CENTER);
        lblGames.setBorder(new MatteBorder(0, 0, 1, 0, FSkin.getColor(FSkin.Colors.CLR_BORDERS)));

        this.tarMessage = new JTextArea();
        this.tarMessage.setOpaque(false);
        this.tarMessage.setFocusable(false);
        this.tarMessage.setEditable(false);
        this.tarMessage.setLineWrap(true);
        this.tarMessage.setWrapStyleWord(true);
        this.tarMessage.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));
        this.tarMessage.setFont(FSkin.getFont(16));
        this.add(this.lblGames, "span 2 1, w 96%!, gapleft 2%, h 10%, wrap");
        this.add(this.tarMessage, "span 2 1, h 70%!, w 96%!, gapleft 2%, gaptop 1%");
        this.add(this.btnOK, "w 47%!, gapright 2%, gapleft 1%");
        this.add(this.btnCancel, "w 47%!, gapright 1%");

        // Resize adapter
        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                int px =  (int) (ViewMessage.this.getWidth() / 17);
                px = (px < 11 ? 11 : px);
                tarMessage.setFont(FSkin.getFont(px));
            }
        });
        // After all components are in place, instantiate controller.
        this.control = new ControlMessage(this);
    }

    /**
     * Gets the controller.
     * 
     * @return ControlMessage
     */
    public ControlMessage getControl() {
        return this.control;
    }

    /**
     * Gets the btn ok.
     * 
     * @return JButton
     */
    public JButton getBtnOK() {
        return this.btnOK;
    }

    /**
     * Gets the btn cancel.
     * 
     * @return JButton
     */
    public JButton getBtnCancel() {
        return this.btnCancel;
    }

    /** @return JTextArea */
    public JTextArea getTarMessage() {
        return this.tarMessage;
    }

    /** @return JLabel */
    public JLabel getLblGames() {
        return this.lblGames;
    }

    /** Flashes animation on input panel if play is currently waiting on input. */
    public void remind() {
        // To adjust, only touch these two values.
        final int steps = 5;    // Number of delays
        final int delay = 80;  // Milliseconds between steps

        if (remindIsRunning) { return; }

        remindIsRunning = true;
        final int oldR = getBackground().getRed();
        final int oldG = getBackground().getGreen();
        final int oldB = getBackground().getBlue();
        final int oldA = getBackground().getAlpha();
        counter = 0;
        newR = new int[steps];
        newG = new int[steps];
        newB = new int[steps];
        newA = new int[steps];

        for (int i = 0; i < steps; i++) {
            newR[i] = (int) ((255 - oldR) / steps * i);
            newG[i] = (int) (oldG / steps * i);
            newB[i] = (int) (oldB / steps * i);
            newA[i] = (int) ((255 - oldA) / steps * i);
        }

        final TimerTask tt = new TimerTask() {
            @Override
            public void run() {
                counter++;
                if (counter != (steps - 1)) {
                    SwingUtilities.invokeLater(new Runnable() { @Override
                        public void run() { setBackground(new Color(newR[counter], oldG, oldB, newA[counter])); } });
                }
                else {
                    SwingUtilities.invokeLater(new Runnable() { @Override
                        public void run() { setBackground(new Color(oldR, oldG, oldB, oldA)); } });
                    remindIsRunning = false;
                    timer1.cancel();
                    newR = null;
                    newG = null;
                    newB = null;
                    newA = null;
                }
            }
        };

        timer1 = new Timer();
        timer1.scheduleAtFixedRate(tt, 0, delay);
    }
}
