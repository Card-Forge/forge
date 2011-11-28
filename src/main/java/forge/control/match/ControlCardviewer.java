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
package forge.control.match;

import java.awt.Image;

import forge.Card;
import forge.ImageCache;
import forge.view.match.ViewCardviewer;

/**
 * 
 * Controls the vertical tabber in sidebar used for viewing card details and
 * picture.
 * 
 */
public class ControlCardviewer {
    private final ViewCardviewer view;
    private Card currentCard = null;

    /**
     * Controls the vertical tabber in sidebar used for viewing card details and
     * picture.
     * 
     * @param v
     *            &emsp; The CardViewer Swing component.
     */
    public ControlCardviewer(final ViewCardviewer v) {
        this.view = v;
    }

    /**
     * Shows card details and/or picture in sidebar cardview tabber.
     * 
     * @param c
     *            &emsp; Card object
     */
    public void showCard(final Card c) {
        final Image img = ImageCache.getImage(c);
        this.currentCard = c;
        this.view.getPnlCardPic().setCard(c);
        this.view.getPnlCardDetail().setCard(c);

        if (img != null) {
            this.showPnlCardPic();
        } else {
            this.showPnlCardDetail();
        }
    }

    /**
     * Gets the current card.
     * 
     * @return Card
     */
    public Card getCurrentCard() {
        return this.currentCard;
    }

    /**
     * Programatically forces card layout of sidebar tabber to show "CardDetail"
     * panel.
     */
    public void showPnlCardDetail() {
        this.view.getVtpCardviewer().showTab(1);
    }

    /**
     * Programatically forces card layout of sidebar tabber to show card picture
     * panel.
     */
    public void showPnlCardPic() {
        this.view.getVtpCardviewer().showTab(0);
    }
}
