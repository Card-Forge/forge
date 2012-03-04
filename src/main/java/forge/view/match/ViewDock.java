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
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;
import forge.Singletons;
import forge.control.match.ControlDock;
import forge.gui.toolbox.FButton;
import forge.gui.toolbox.FOverlay;
import forge.gui.toolbox.FPanel;
import forge.gui.toolbox.FSkin;

/**
 * Swing component for button dock.
 * 
 */
@SuppressWarnings("serial")
public class ViewDock extends FPanel {
    private final ControlDock control;
    private final Action actClose;

    /**
     * Swing component for button dock.
     * 
     */
    public ViewDock() {
        super();
        this.setToolTipText("Shortcut Button Dock");
        //this.setLayout(new MigLayout("insets 0, gap 0, ay center, ax center"));

        // Mig layout does not support wrapping!
        // http://stackoverflow.com/questions/5715833/how-do-you-make-miglayout-behave-like-wrap-layout
        FlowLayout layFlow = new FlowLayout();
        layFlow.setHgap(10);
        layFlow.setVgap(10);
        this.setLayout(layFlow);

        this.actClose = new AbstractAction() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                Singletons.getView().getOverlay().hideOverlay();
            }
        };

        final JLabel btnConcede = new DockButton(FSkin.getIcon(FSkin.DockIcons.ICO_CONCEDE), "Concede Game");
        btnConcede.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent e) {
                ViewDock.this.control.concede();
            }
        });

        final JLabel btnSettings = new DockButton(FSkin.getIcon(FSkin.DockIcons.ICO_SETTINGS), "Game Settings");
        btnSettings.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent e) {
                ViewDock.this.overlaySettings();
            }
        });

        final JLabel btnEndTurn = new DockButton(FSkin.getIcon(FSkin.DockIcons.ICO_ENDTURN), "End Turn");
        btnEndTurn.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent e) {
                ViewDock.this.control.endTurn();
            }
        });

        final JLabel btnViewDeckList = new DockButton(FSkin.getIcon(FSkin.DockIcons.ICO_DECKLIST), "View Deck List");
        btnViewDeckList.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent e) {
                ViewDock.this.control.viewDeckList();
            }
        });

        this.add(btnConcede);
        //this.add(btnShortcuts);
        this.add(btnSettings);
        this.add(btnEndTurn);
        this.add(btnViewDeckList);

        // After all components are in place, instantiate controller.
        this.control = new ControlDock(this);
    }

    /**
     * Gets the controller.
     * 
     * @return ControlDock
     */
    public ControlDock getControl() {
        return this.control;
    }

    /**
     * Buttons in Dock. JLabels are used to allow hover effects.
     */
    public class DockButton extends JLabel {
        private final Image img;
        private final Color hoverBG = FSkin.getColor(FSkin.Colors.CLR_HOVER);
        private final Color defaultBG = new Color(0, 0, 0, 0);
        private Color clrBorders = new Color(0, 0, 0, 0);
        private int w, h;

        /**
         * Buttons in Dock. JLabels are used to allow hover effects.
         * 
         * @param i0
         *            &emsp; ImageIcon to show in button
         * @param s0
         *            &emsp; Tooltip string
         */
        public DockButton(final ImageIcon i0, final String s0) {
            super();
            this.setToolTipText(s0);
            this.setOpaque(false);
            this.setBackground(this.defaultBG);
            this.img = i0.getImage();

            Dimension size = new Dimension(30, 30);
            this.setMinimumSize(size);
            this.setMaximumSize(size);
            this.setPreferredSize(size);

            this.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(final MouseEvent e) {
                    DockButton.this.clrBorders = FSkin.getColor(FSkin.Colors.CLR_BORDERS);
                    DockButton.this.setBackground(DockButton.this.hoverBG);
                }

                @Override
                public void mouseExited(final MouseEvent e) {
                    DockButton.this.clrBorders = new Color(0, 0, 0, 0);
                    DockButton.this.setBackground(DockButton.this.defaultBG);
                }
            });
        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.swing.JComponent#paintComponent(java.awt.Graphics)
         */
        @Override
        public void paintComponent(final Graphics g) {
            this.w = this.getWidth();
            this.h = this.getHeight();
            g.setColor(this.getBackground());
            g.fillRect(0, 0, this.w, this.h);
            g.setColor(this.clrBorders);
            g.drawRect(0, 0, this.w - 1, this.h - 1);
            g.drawImage(this.img, 0, 0, this.w, this.h, null);
            super.paintComponent(g);
        }
    }

    /** */
    private void overlaySettings() {
        final FOverlay overlay = Singletons.getView().getOverlay();
        overlay.setLayout(new MigLayout("insets 0"));
        overlay.showOverlay();

        final JPanel parent = new JPanel();
        parent.setBackground(Color.red.darker());
        overlay.add(parent, "w 80%!, h 80%!, gaptop 10%, gapleft 10%, span 2 1");

        final FButton btnOK = new FButton("Save and Exit");
        final FButton btnCancel = new FButton("Exit Without Save");

        overlay.add(btnOK, "width 30%, newline, gapright 10%, gapleft 15%, gaptop 10px");
        overlay.add(btnCancel, "width 30%!");

        btnOK.setAction(this.actClose);
        btnOK.setText("Save and Exit");

        btnCancel.setAction(this.actClose);
        btnCancel.setText("Exit Without Save");

        final JLabel test = new JLabel();
        test.setForeground(Color.white);
        test.setText("<html><center>'Settings' does not do anything yet.<br>"
                + "This button is just here to demonstrate the dock feature.<br>"
                + "'Settings' can be removed or developed further.</center></html>");

        parent.add(test);
    }
}
