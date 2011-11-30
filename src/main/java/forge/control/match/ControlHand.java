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

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JLayeredPane;
import javax.swing.SwingUtilities;

import arcane.ui.HandArea;
import arcane.ui.util.Animation;
import forge.AllZone;
import forge.Card;
import forge.Constant.Zone;
import forge.PlayerZone;
import forge.view.match.ViewHand;
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
        final ViewTopLevel t = (ViewTopLevel) AllZone.getDisplay();

        AllZone.getHumanPlayer().getZone(Zone.Hand).addObserver(new Observer() {
            @Override
            public void update(final Observable a, final Object b) {
                final PlayerZone pZone = (PlayerZone) a;
                final HandArea p = view.getHandArea();

                final Card[] c = pZone.getCards();

                List<Card> tmp, diff;
                tmp = new ArrayList<Card>();
                for (final arcane.ui.CardPanel cpa : p.getCardPanels()) {
                    tmp.add(cpa.getGameCard());
                }
                diff = new ArrayList<Card>(tmp);
                diff.removeAll(Arrays.asList(c));
                if (diff.size() == p.getCardPanels().size()) {
                    p.clear();
                } else {
                    for (final Card card : diff) {
                        p.removeCardPanel(p.getCardPanel(card.getUniqueNumber()));
                    }
                }
                diff = new ArrayList<Card>(Arrays.asList(c));
                diff.removeAll(tmp);

                JLayeredPane layeredPane = SwingUtilities.getRootPane(t.getTopLevelFrame()).getLayeredPane();
                int fromZoneX = 0, fromZoneY = 0;
                final Rectangle pb = t.getFieldControllers().get(1).getView().getLblLibrary().getBounds();
                final Point zoneLocation = SwingUtilities.convertPoint(t.getFieldControllers().get(1).getView().getLblLibrary(),
                        Math.round(pb.width / 2.0f), Math.round(pb.height / 2.0f), layeredPane);
                fromZoneX = zoneLocation.x;
                fromZoneY = zoneLocation.y;
                int startWidth, startX, startY;
                startWidth = 10;
                startX = fromZoneX - Math.round(startWidth / 2.0f);
                startY = fromZoneY - Math.round(Math.round(startWidth * arcane.ui.CardPanel.ASPECT_RATIO) / 2.0f);

                int endWidth, endX, endY;
                arcane.ui.CardPanel toPanel = null;

                for (final Card card : diff) {
                    toPanel = p.addCard(card);
                    endWidth = toPanel.getCardWidth();
                    final Point toPos = SwingUtilities.convertPoint(view.getHandArea(),
                            toPanel.getCardLocation(), layeredPane);
                    endX = toPos.x;
                    endY = toPos.y;
                    final arcane.ui.CardPanel animationPanel = new arcane.ui.CardPanel(card);
                    if (t.getTopLevelFrame().isShowing()) {
                        Animation.moveCard(startX, startY, startWidth, endX, endY, endWidth, animationPanel, toPanel,
                                layeredPane, 500);
                    } else {
                        Animation.moveCard(toPanel);
                    }
                }
            }
        });
    }

    /** Adds listeners to hand panel: clicks, mouseover, etc. */
    public void addListeners() {
        final ViewTopLevel t = (ViewTopLevel) AllZone.getDisplay();

        view.getHandArea().addMouseListener(new MouseAdapter() {
            // Card click
            @Override
            public void mousePressed(final MouseEvent e) {
                if (e.getButton() != MouseEvent.BUTTON1) {
                    return;
                }
                final Card c = view.getHandArea().getCardFromMouseOverPanel();
                if (c != null) {
                    t.getInputController().getInputControl().selectCard(c, AllZone.getHumanPlayer().getZone(Zone.Hand));
                }
            }
        });

        view.getHandArea().addMouseMotionListener(new MouseMotionAdapter() {
            // Card mouseover
            @Override
            public void mouseMoved(final MouseEvent me) {
                final Card c = view.getHandArea().getCardFromMouseOverPanel();
                if (c != null) {
                    t.setCard(c);
                }
            } // mouseMoved
        });
    }

    /**
     * Adds various listeners for cards in hand. Uses CardPanel instance from
     * ViewHand.
     * 
     * @param c
     *            &emsp; CardPanel object
     */
   /* public void addCardPanelListeners(final CardPanel c) {
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
    }*/

    /**
     * Adds the card.
     * 
     * @param c
     *            &emsp; Card object
     */
    public void addCard(final Card c) {
        this.cardsInPanel.add(c);
        //this.view.refreshLayout();
    }

    /**
     * Adds the cards.
     * 
     * @param c
     *            &emsp; List of Card objects
     */
    public void addCards(final List<Card> c) {
        this.cardsInPanel.addAll(c);
        //this.view.refreshLayout();
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
        //this.view.refreshLayout();
    }

    /**
     * Removes the cards.
     * 
     * @param c
     *            &emsp; List of Card objects
     */
    public void removeCards(final List<Card> c) {
        this.cardsInPanel.removeAll(c);
        //this.view.refreshLayout();
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
