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
package forge.screens.match.controllers;

import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import javax.swing.JLabel;

import forge.game.card.CardView;
import forge.game.card.CardView.CardStateView;
import forge.gui.CardPicturePanel;
import forge.gui.framework.ICDoc;
import forge.item.InventoryItem;
import forge.screens.match.views.VPicture;
import forge.toolbox.FMouseAdapter;
import forge.toolbox.special.CardZoomer;

/**
 * Controller for {@link VPicture}. May be used as part of a
 * {@link CDetailPicture}.
 * <p>
 * Can be used to display images associated with a {@link CardView} or
 * {@link InventoryItem} in {@link CardPicturePanel}.<br>
 * <br>
 * Can also be used to display details associated with a {@link CardView}. <br>
 * <br>
 * <i>(C at beginning of class name denotes a control class.)</i>
 */
public class CPicture implements ICDoc {
    private final CDetailPicture controller;
    private final VPicture view;
    CPicture(final CDetailPicture controller) {
        this.controller = controller;
        this.view = new VPicture(this);
        picturePanel = this.view.getPnlPicture();
        flipIndicator = this.view.getLblFlipcard();

        setMouseWheelListener();
        setMouseButtonListener();
    }

    public VPicture getView() {
        return view;
    }

    // For brevity, local shortcuts to singletons & child controls...
    private final CardPicturePanel picturePanel;
    private final JLabel flipIndicator;
    private final CardZoomer zoomer = CardZoomer.SINGLETON_INSTANCE;

    /**
     * Shows card details and/or picture in sidebar cardview tabber.
     *
     */
    void showCard(final CardView c, final boolean isInAltState, final boolean mayView, final boolean mayFlip) {
        final CardStateView toShow = c != null && mayView ? c.getState(isInAltState) : null;
        flipIndicator.setVisible(toShow != null && mayFlip);
        picturePanel.setCard(toShow, mayView);
        zoomer.setCard(toShow, mayFlip);
    }

    void showItem(final InventoryItem item) {
        flipIndicator.setVisible(false);
        picturePanel.setItem(item);
    }

    @Override
    public void register() {
    }

    @Override
    public void initialize() {
    }

    /**
     * Adds a mouse button listener to CardPicturePanel.
     * <p><ul>
     * <li>Shows the displayed card in the zoomer while the middle mouse button or
     * both left and right buttons are held down simultaneously.
     * <li>Displays the alternate image for applicable cards on mouse click.
     */
    private void setMouseButtonListener() {
        this.picturePanel.addMouseListener(new FMouseAdapter() {
            @Override
            public void onLeftClick(final MouseEvent e) {
                controller.flip();
            }

            @Override
            public void onMiddleMouseDown(final MouseEvent e) {
                if (isCardDisplayed()) {
                    zoomer.doMouseButtonZoom();
                }
            }

            @Override
            public void onMiddleMouseUp(final MouseEvent e) {
                if (isCardDisplayed()) {
                    zoomer.closeZoomer();
                }
            }
        });
    }

    /**
     * Adds a mouse wheel listener to CardPicturePanel.
     * <p>
     * Shows the displayed card in the zoomer if the mouse wheel is rotated
     * while the mouse pointer is hovering over the image.
     */
    private void setMouseWheelListener() {
        picturePanel.addMouseWheelListener(new MouseWheelListener() {
            @Override
            public void mouseWheelMoved(final MouseWheelEvent arg0) {
                if (isCardDisplayed()) {
                    if (arg0.getWheelRotation() < 0) {
                        zoomer.doMouseWheelZoom();
                    }
                }
            }
        });
    }

    private boolean isCardDisplayed() {
        return controller.getCurrentCard() != null;
    }

    @Override
    public void update() {
    }

}
