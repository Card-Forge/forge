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

import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import forge.AllZone;
import forge.Card;
import forge.Constant.Zone;
import forge.PlayerZone;
import forge.view.match.ViewHand;
import forge.view.match.ViewHand.CardPanel;
import forge.view.match.ViewTopLevel;

/**
 * Child controller - handles operations related to cards in user's hand and
 * their Swing components, which are assembled in ViewHand.
 * 
 */
public class ControlHand {
    private final List<Card> cardsInPanel;
    private final ViewHand view;

    /**
     * Child controller - handles operations related to cards in user's hand and
     * their Swing components, which are assembled in ViewHand.
     * 
     * @param v
     *            &emsp; The Swing component for user hand
     */
    public ControlHand(final ViewHand v) {
        this.view = v;
        this.cardsInPanel = new ArrayList<Card>();
    }

    /** Adds observers to hand panel. */
    public void addObservers() {
        final Observer o1 = new Observer() {
            @Override
            public void update(final Observable a, final Object b) {
                ControlHand.this.resetCards(Arrays.asList(((PlayerZone) a).getCards()));
            }
        };
        AllZone.getHumanPlayer().getZone(Zone.Hand).addObserver(o1);
    }

    /** Adds listeners to hand panel: window resize, etc. */
    public void addListeners() {
        this.view.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(final ComponentEvent e) {
                // Ensures cards in hand scale properly with parent.
                ControlHand.this.view.refreshLayout();
            }
        });
    }

    /**
     * Adds various listeners for cards in hand. Uses CardPanel instance from
     * ViewHand.
     * 
     * @param c
     *            &emsp; CardPanel object
     */
    public void addCardPanelListeners(final CardPanel c) {
        // Grab top level controller to facilitate interaction between children
        final ViewTopLevel display = (ViewTopLevel) (AllZone.getDisplay());
        final Card cardobj = c.getCard();

        // Sidebar pic/detail on card hover
        c.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(final MouseEvent e) {
                display.getCardviewerController().showCard(cardobj);
            }
        });

        // Mouse press
        c.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent e) {

                if (e.getButton() != MouseEvent.BUTTON1) {
                    return;
                }

                display.getInputController().getInputControl()
                        .selectCard(cardobj, AllZone.getHumanPlayer().getZone(Zone.Hand));
            }
        });
    }

    /**
     * Adds the card.
     * 
     * @param c
     *            &emsp; Card object
     */
    public void addCard(final Card c) {
        this.cardsInPanel.add(c);
        this.view.refreshLayout();
    }

    /**
     * Adds the cards.
     * 
     * @param c
     *            &emsp; List of Card objects
     */
    public void addCards(final List<Card> c) {
        this.cardsInPanel.addAll(c);
        this.view.refreshLayout();
    }

    /**
     * Gets the cards.
     * 
     * @return List<Card>
     */
    public List<Card> getCards() {
        return this.cardsInPanel;
    }

    /**
     * Removes the card.
     * 
     * @param c
     *            &emsp; Card object
     */
    public void removeCard(final Card c) {
        this.cardsInPanel.remove(c);
        this.view.refreshLayout();
    }

    /**
     * Removes the cards.
     * 
     * @param c
     *            &emsp; List of Card objects
     */
    public void removeCards(final List<Card> c) {
        this.cardsInPanel.removeAll(c);
        this.view.refreshLayout();
    }

    /**
     * Reset cards.
     * 
     * @param c
     *            &emsp; List of Card objects
     */
    public void resetCards(final List<Card> c) {
        this.cardsInPanel.clear();
        this.addCards(c);
    }
}
