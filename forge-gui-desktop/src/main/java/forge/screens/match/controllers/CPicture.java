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

import forge.UiCommand;
import forge.game.card.Card;
import forge.game.card.CardView;
import forge.gui.CardPicturePanel;
import forge.gui.framework.ICDoc;
import forge.item.IPaperCard;
import forge.item.InventoryItem;
import forge.match.MatchUtil;
import forge.screens.match.views.VPicture;
import forge.toolbox.FMouseAdapter;
import forge.toolbox.special.CardZoomer;

/**
 * Singleton controller for VPicture.
 * <p>
 * Can be used to display images associated with a {@code Card} or
 * {@code InventoryItem} in {@code CardPicturePanel}.<br>
 * <br>
 * Can also be used to display details associated with a {@code Card}.
 * 
 * @version: $Id:$
 * 
 */
public enum CPicture implements ICDoc {
    SINGLETON_INSTANCE;

    // For brevity, local shortcuts to singletons & child controls...
    private final VPicture view = VPicture.SINGLETON_INSTANCE;
    private final CardPicturePanel picturePanel = this.view.getPnlPicture();
    private final JLabel flipIndicator = this.view.getLblFlipcard();
    private final CardZoomer zoomer = CardZoomer.SINGLETON_INSTANCE;

    private CardView currentView = null;
    private boolean isDisplayAlt = false;

    /**
     * Shows card details and/or picture in sidebar cardview tabber.
     * 
     */
    public void showCard(final CardView c, boolean showAlt) {
        if (c == null) {
            return;
        }

        boolean canFlip = MatchUtil.canFaceDownCardBeShown(c);

        currentView = c;
        isDisplayAlt = showAlt;
        flipIndicator.setVisible(canFlip);
        picturePanel.setCard(c.getState(showAlt));
        if (showAlt && canFlip) {
            flipCard();
        }
    }

    /**
     * Displays image associated with either a {@code Card}
     * or {@code InventoryItem} instance.
     */
    public void showImage(final InventoryItem item) {
        if (item instanceof IPaperCard) {
            final IPaperCard paperCard = ((IPaperCard)item);
            final CardView c = CardView.getCardForUi(paperCard);
            if (paperCard.isFoil() && c.getCurrentState().getFoilIndex() == 0) {
                // FIXME should assign a random foil here in all cases
                // (currently assigns 1 for the deck editors where foils "flicker" otherwise)
                if (item instanceof Card) {
                    c.getCurrentState().setFoilIndexOverride(-1); //-1 to choose random
                }
                else if (item instanceof IPaperCard) {
                    c.getCurrentState().setFoilIndexOverride(1);
                }
            }
            showCard(c, false);
        }
        else {
            currentView = null;
            isDisplayAlt = false;
            flipIndicator.setVisible(false);
            picturePanel.setCard(item);
        }
    }

    @Override
    public UiCommand getCommandOnSelect() {
        return null;
    }

    @Override
    public void initialize() {
        setMouseWheelListener();
        setMouseButtonListener();
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
            public void onLeftClick(MouseEvent e) {
                flipCard();
            }

            @Override
            public void onMiddleMouseDown(MouseEvent e) {
                if (isCardDisplayed()) {
                    CardZoomer.SINGLETON_INSTANCE.doMouseButtonZoom(currentView);
                }
            }

            @Override
            public void onMiddleMouseUp(MouseEvent e) {
                if (isCardDisplayed()) {
                    CardZoomer.SINGLETON_INSTANCE.closeZoomer();
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
            public void mouseWheelMoved(MouseWheelEvent arg0) {
                if (isCardDisplayed()) {
                    if (arg0.getWheelRotation() < 0) {
                        zoomer.doMouseWheelZoom(currentView);
                    }
                }
            }
        });
    }

    private boolean isCardDisplayed() {
        return (currentView != null);
    }

    @Override
    public void update() {
    }

    public void flipCard() {
        if (MatchUtil.canFaceDownCardBeShown(currentView)) {
            isDisplayAlt = !isDisplayAlt;
            picturePanel.setCard(currentView.getState(isDisplayAlt));
            CDetail.SINGLETON_INSTANCE.showCard(currentView, isDisplayAlt);
        }
    }
}
