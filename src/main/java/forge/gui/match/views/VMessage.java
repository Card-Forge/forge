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
package forge.gui.match.views;

import java.awt.Component;
import java.awt.Font;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.border.MatteBorder;

import net.miginfocom.swing.MigLayout;
import forge.gui.layout.DragTab;
import forge.gui.layout.EDocID;
import forge.gui.layout.ICDoc;
import forge.gui.layout.IVDoc;
import forge.gui.toolbox.FButton;
import forge.gui.toolbox.FLabel;
import forge.gui.toolbox.FSkin;

/**
 * Assembles Swing components of message report.
 * 
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 */
public enum VMessage implements IVDoc {
    /** */
    SINGLETON_INSTANCE;

    private final JPanel pnl = new JPanel();
    private final DragTab tab = new DragTab("Message Report");

    private final JButton btnOK = new FButton("OK");
    private final JButton btnCancel = new FButton("Cancel");
    private final JTextArea tarMessage = new JTextArea();
    private final JLabel lblGames = new FLabel.Builder().fontScaleAuto(false)
            .fontSize(12).fontStyle(Font.BOLD).fontAlign(SwingConstants.CENTER).build();

    /* (non-Javadoc)
     * @see forge.gui.layout.IVDoc#populate()
     */
    @Override
    public void populate() {
        pnl.removeAll();
        pnl.setToolTipText("Input Area");
        pnl.setLayout(new MigLayout("wrap 2, fill, insets 0, gap 0"));

        lblGames.setBorder(new MatteBorder(0, 0, 1, 0, FSkin.getColor(FSkin.Colors.CLR_BORDERS)));

        tarMessage.setOpaque(false);
        tarMessage.setFocusable(false);
        tarMessage.setEditable(false);
        tarMessage.setLineWrap(true);
        tarMessage.setWrapStyleWord(true);
        tarMessage.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));
        tarMessage.setFont(FSkin.getFont(16));

        pnl.add(lblGames, "span 2 1, w 96%!, gapleft 2%, h 10%, wrap");
        pnl.add(tarMessage, "span 2 1, h 70%!, w 96%!, gapleft 2%, gaptop 1%");
        pnl.add(btnOK, "w 47%!, gapright 2%, gapleft 1%");
        pnl.add(btnCancel, "w 47%!, gapright 1%");
    }

    /* (non-Javadoc)
     * @see forge.gui.layout.IVDoc#getDocumentID()
     */
    @Override
    public EDocID getDocumentID() {
        return EDocID.REPORT_MESSAGE;
    }

    /* (non-Javadoc)
     * @see forge.gui.layout.IVDoc#getControl()
     */
    @Override
    public ICDoc getControl() {
        return null;
    }

    /* (non-Javadoc)
     * @see forge.gui.layout.IVDoc#getDocument()
     */
    @Override
    public Component getDocument() {
        return pnl;
    }

    /* (non-Javadoc)
     * @see forge.gui.layout.IVDoc#getTabLabel()
     */
    @Override
    public DragTab getTabLabel() {
        return tab;
    }

    /** Flashes animation on input panel if play is currently waiting on input. */
    public void remind() {
        /*
         * THIS SHOULD COMPLETELY AND TOTALLY BE IN A
         * UTIL LIBRARY THAT CAN FLASH IT ON ANY COMPONENT
         * IN THE LAYOUT.
         */

        /*
         * THESE VALUES WERE PREVIOUSLY CLASS VARIABLES; SHOULD BE METHOD-SPECIFIC SOMEHOW.
         *    private Timer timer1 = null;
    private static int counter = 0;
    private int[] newA = null, newR = null, newG = null, newB = null;
    private boolean remindIsRunning = false;
         */


        // To adjust, only touch these two values.
       /* final int steps = 5;    // Number of delays
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
        timer1.scheduleAtFixedRate(tt, 0, delay);*/
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
}
