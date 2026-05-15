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
package forge.screens.match.views;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.event.MouseEvent;

import javax.swing.JPanel;

import forge.gui.UiCommand;
import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.gui.framework.ILocalRepaint;
import forge.gui.framework.IVDoc;
import forge.localinstance.skin.FSkinProp;
import forge.screens.match.controllers.CDock;
import forge.toolbox.FLabel;
import forge.toolbox.FMouseAdapter;
import forge.toolbox.FSkin;
import forge.toolbox.FSkin.SkinColor;
import forge.toolbox.FSkin.SkinImage;
import forge.toolbox.FSkin.SkinnedLabel;
import forge.util.Localizer;

/**
 * Assembles Swing components of button dock area.
 * 
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 */
public class VDock implements IVDoc<CDock> {
    final Localizer localizer = Localizer.getInstance();
    // Fields used with interface IVDoc
    private DragCell parentCell;
    private final DragTab tab = new DragTab(localizer.getMessage("lblDock"));
    // Dock button instances
    private final DockButton btnConcede = new DockButton(FSkin.getIcon(FSkinProp.ICO_CONCEDE), localizer.getMessage("lblConcedeGame"));
    private final DockButton btnSettings = new DockButton(FSkin.getIcon(FSkinProp.ICO_SETTINGS), localizer.getMessage("lblYieldSettings"));
    private final DockButton btnEndTurn = new DockButton(FSkin.getIcon(FSkinProp.ICO_ENDTURN), localizer.getMessage("lblEndTurn"));
    private final DockButton btnViewDeckList = new DockButton(FSkin.getIcon(FSkinProp.ICO_DECKLIST), localizer.getMessage("lblViewDeckList"));
    private final DockButton btnAutoPass = new DockButton(FSkin.getIcon(FSkinProp.ICO_AUTOPASS), localizer.getMessage("lblYieldBtnAutoPassTooltip"));
    private final DockButton btnAlphaStrike = new DockButton(FSkin.getIcon(FSkinProp.ICO_ALPHASTRIKE), localizer.getMessage("lblAlphaStrike"));
    private final FLabel btnTargeting = new FLabel.Builder().icon(FSkin.getIcon(FSkinProp.ICO_ARCSOFF))
                .hoverable(true).iconInBackground(true).iconScaleFactor(1.0).build();

    private final CDock controller;

    public VDock(final CDock controller) {
        this.controller = controller;

        btnTargeting.setPreferredSize(new Dimension(30, 30));
        btnTargeting.setMinimumSize(new Dimension(30, 30));
        btnTargeting.setMaximumSize(new Dimension(30, 30));
    }

    //========= Overridden methods

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#populate()
     */
    @Override
    public void populate() {
        btnTargeting.setFocusable(false); // don't let the targeting arc switcher get focus
        final JPanel pnl = parentCell.getBody();
        pnl.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 10));

        pnl.add(btnAutoPass);
        pnl.add(btnSettings);
        pnl.add(btnEndTurn);
        pnl.add(btnAlphaStrike);
        pnl.add(btnTargeting);
        pnl.add(btnViewDeckList);
        pnl.add(btnConcede);
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#setParentCell()
     */
    @Override
    public void setParentCell(final DragCell cell0) {
        this.parentCell = cell0;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getParentCell()
     */
    @Override
    public DragCell getParentCell() {
        return this.parentCell;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getDocumentID()
     */
    @Override
    public EDocID getDocumentID() {
        return EDocID.BUTTON_DOCK;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getTabLabel()
     */
    @Override
    public DragTab getTabLabel() {
        return tab;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getLayoutControl()
     */
    @Override
    public CDock getLayoutControl() {
        return controller;
    }

    //========= Retrieval methods

    public DockButton getBtnConcede() {
        return btnConcede;
    }

    public DockButton getBtnSettings() {
        return btnSettings;
    }

    public DockButton getBtnEndTurn() {
        return btnEndTurn;
    }

    public DockButton getBtnViewDeckList() {
        return btnViewDeckList;
    }

    public DockButton getBtnAutoPass() {
        return btnAutoPass;
    }

    public DockButton getBtnAlphaStrike() {
        return btnAlphaStrike;
    }

    /** @return {@link forge.toolbox.FLabel} */
    public FLabel getBtnTargeting() {
        return btnTargeting;
    }

    //========= Custom class handling
    /**
     * Buttons in Dock. JLabels are used to allow hover effects.
     */
    @SuppressWarnings("serial")
    public class DockButton extends SkinnedLabel implements ILocalRepaint {
        private final SkinImage img;
        private final SkinColor hoverBG = FSkin.getColor(FSkin.Colors.CLR_HOVER);
        private final Color toggledBG = new Color(218, 165, 32);
        private final Color defaultBG = new Color(0, 0, 0, 0);
        private final Color defaultBorderColor = new Color(0, 0, 0, 0);
        private UiCommand command;
        private boolean toggled;
        private int w, h;

        /**
         * Buttons in Dock. JLabels are used to allow hover effects.
         * 
         * @param i0
         *            &emsp; ImageIcon to show in button
         * @param s0
         *            &emsp; Tooltip string
         */
        public DockButton(final SkinImage i0, final String s0) {
            super();
            this.setToolTipText(s0);
            this.setOpaque(false);
            this.setBackground(this.defaultBG);
            this.img = i0;

            Dimension size = new Dimension(30, 30);
            this.setMinimumSize(size);
            this.setMaximumSize(size);
            this.setPreferredSize(size);

            this.addMouseListener(new FMouseAdapter() {
                @Override
                public void onLeftClick(final MouseEvent e) {
                    if (DockButton.this.command != null) {
                        DockButton.this.command.run();
                    }
                }

                @Override
                public void onMouseEnter(final MouseEvent e) {
                    DockButton.this.setBackground(DockButton.this.hoverBG);
                }

                @Override
                public void onMouseExit(final MouseEvent e) {
                    DockButton.this.setBackground(DockButton.this.defaultBG);
                }
            });
        }

        public void setCommand(UiCommand command0) {
            this.command = command0;
        }

        public void setToggled(final boolean t) {
            this.toggled = t;
            repaintSelf();
        }

        @Override
        public void repaintSelf() {
            final Dimension d = getSize();
            repaint(0, 0, d.width, d.height);
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
            final boolean highlighted = this.toggled || this.getSkin().getBackground() == this.hoverBG;
            if (this.toggled) {
                g.setColor(this.toggledBG);
            } else {
                g.setColor(this.getBackground());
            }
            g.fillRect(0, 0, this.w, this.h);

            if (highlighted) {
                FSkin.setGraphicsColor(g, FSkin.getColor(FSkin.Colors.CLR_BORDERS));
            }
            else {
                g.setColor(this.defaultBorderColor);
            }
            g.drawRect(0, 0, this.w - 1, this.h - 1);
            FSkin.drawImage(g, this.img, 0, 0, this.w, this.h);
            super.paintComponent(g);
        }
    }
}
