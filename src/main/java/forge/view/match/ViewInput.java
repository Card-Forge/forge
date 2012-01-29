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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.border.MatteBorder;

import net.miginfocom.swing.MigLayout;
import forge.Singletons;
import forge.control.match.ControlInput;
import forge.view.toolbox.FButton;
import forge.view.toolbox.FRoundedPanel;
import forge.view.toolbox.FSkin;

/**
 * Assembles Swing components of input area.
 * 
 */
@SuppressWarnings("serial")
public class ViewInput extends FRoundedPanel {
    private final ControlInput control;
    private final JButton btnOK, btnCancel;
    private final JTextArea tarMessage;
    private final FSkin skin;
    private final JLabel lblGames;
    private Timer timer1 = null;
    private static int counter = 0;
    private boolean remindIsRunning = false;

    /**
     * Assembles UI for input area (buttons and message panel).
     * 
     */
    public ViewInput() {
        super();
        this.skin = Singletons.getView().getSkin();
        this.setToolTipText("Input Area");
        this.setBackground(this.skin.getColor(FSkin.Colors.CLR_THEME));
        this.setForeground(this.skin.getColor(FSkin.Colors.CLR_TEXT));
        this.setLayout(new MigLayout("wrap 2, fill, insets 0, gap 0"));

        // Cancel button
        this.btnCancel = new FButton("Cancel");
        this.btnOK = new FButton("OK");

        // Game counter
        lblGames = new JLabel();
        lblGames.setFont(skin.getBoldFont(12));
        lblGames.setForeground(skin.getColor(FSkin.Colors.CLR_TEXT));
        lblGames.setHorizontalAlignment(SwingConstants.CENTER);
        lblGames.setBorder(new MatteBorder(0, 0, 1, 0, skin.getColor(FSkin.Colors.CLR_BORDERS)));

        this.tarMessage = new JTextArea();
        this.tarMessage.setOpaque(false);
        this.tarMessage.setFocusable(false);
        this.tarMessage.setEditable(false);
        this.tarMessage.setLineWrap(true);
        this.tarMessage.setWrapStyleWord(true);
        this.tarMessage.setForeground(this.skin.getColor(FSkin.Colors.CLR_TEXT));
        this.tarMessage.setFont(this.skin.getFont(16));
        this.add(this.lblGames, "span 2 1, w 96%!, gapleft 2%, h 10%, wrap");
        this.add(this.tarMessage, "span 2 1, h 70%!, w 96%!, gapleft 2%, gaptop 1%");
        this.add(this.btnOK, "w 47%!, gapright 2%, gapleft 1%");
        this.add(this.btnCancel, "w 47%!, gapright 1%");

        // Resize adapter
        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                int px =  (int) (ViewInput.this.getWidth() / 17);
                px = (px < 11 ? 11 : px);
                tarMessage.setFont(Singletons.getView().getSkin().getFont(px));
            }
        });
        // After all components are in place, instantiate controller.
        this.control = new ControlInput(this);
    }

    /**
     * Gets the controller.
     * 
     * @return ControlInput
     */
    public ControlInput getController() {
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
        if (remindIsRunning) { return; }

        remindIsRunning = true;
        final int[] steps = {210, 215, 220, 220, 220, 215, 210};
        final Color oldBG = getBackground();
        counter = 0;

        ActionListener fader = new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent evt) {
                counter++;
                if (counter != (steps.length - 1)) {
                    setBackground(new Color(255, 0, 0, steps[counter]));
                }
                else {
                    setBackground(oldBG);
                    remindIsRunning = false;
                    timer1.stop();
                }
            }
        };

        timer1 = new Timer(100, fader);
        timer1.start();
    }
}
