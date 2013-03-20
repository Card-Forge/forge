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
package forge.gui.match.nonsingleton;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JLayeredPane;
import javax.swing.SwingUtilities;

import forge.Card;
import forge.Command;
import forge.Singletons;
import forge.game.player.Player;
import forge.game.zone.PlayerZone;
import forge.game.zone.ZoneType;
import forge.gui.framework.ICDoc;
import forge.gui.match.CMatchUI;
import forge.gui.match.controllers.CMessage;
import forge.view.arcane.CardPanel;
import forge.view.arcane.HandArea;
import forge.view.arcane.util.Animation;

/**
 * Controls Swing components of a player's hand instance.
 * 
 */
public class CHand implements ICDoc {
    private final Player player;
    private final VHand view;
    private boolean initializedAlready = false;

    private final List<Card> cardsInPanel = new ArrayList<Card>();

    private final MouseListener madCardClick = new MouseAdapter() { @Override
        public void mousePressed(final MouseEvent e) {
            cardclickAction(e); } };

    private final Observer o1 = new Observer() { @Override
        public void update(final Observable a, final Object b) {
            observerAction(a); } };

    /**
     * Controls Swing components of a player's hand instance.
     * 
     * @param p0 &emsp; {@link forge.game.player.Player}
     * @param v0 &emsp; {@link forge.gui.match.nonsingleton.VHand}
     */
    public CHand(final Player p0, final VHand v0) {
        this.player = p0;
        this.view = v0;
    }

    @Override
    public void initialize() {
        if (initializedAlready) { return; }
        initializedAlready = true;

        player.getZone(ZoneType.Hand).addObserver(o1);

        view.getHandArea().addMouseListener(madCardClick);
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
        final Card cardobj = c.getCard();

        // Sidebar pic/detail on card hover
        c.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(final MouseEvent e) {
                CMatchUI.SINGLETON_INSTANCE.setCard(cardobj);
            }
        });

        // Mouse press
        c.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent e) {
                if (e.getButton() != MouseEvent.BUTTON1) { return; }

                CMessage.SINGLETON_INSTANCE.getInputControl().selectCard(cardobj);
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

    private void cardclickAction(final MouseEvent e) {
        if (e.getButton() != MouseEvent.BUTTON1) {
            return;
        }
        final Card c = view.getHandArea().getHoveredCard(e);
        if (c != null) {
            CMessage.SINGLETON_INSTANCE.getInputControl().selectCard(c);
        }
    }

    private void observerAction(final Observable a) {
        final PlayerZone pZone = (PlayerZone) a;
        final HandArea p = view.getHandArea();
        final Rectangle rctLibraryLabel = CMatchUI.SINGLETON_INSTANCE
                .getFieldControls().get(0)
                .getView().getLblLibrary().getBounds();
        final List<Card> c = pZone.getCards();

        // Animation starts from the library label and runs to the hand panel.
        // This check prevents animation running if label hasn't been realized yet.
        if (rctLibraryLabel.isEmpty() || p.getWidth() <= 0) {
            return;
        }

        List<Card> tmp, diff;
        tmp = new ArrayList<Card>();
        for (final forge.view.arcane.CardPanel cpa : p.getCardPanels()) {
            tmp.add(cpa.getGameCard());
        }
        diff = new ArrayList<Card>(tmp);
        diff.removeAll(c);
        if (diff.size() == p.getCardPanels().size()) {
            p.clear();
        } else {
            for (final Card card : diff) {
                p.removeCardPanel(p.getCardPanel(card.getUniqueNumber()));
            }
        }
        diff = new ArrayList<Card>(c);
        diff.removeAll(tmp);

        JLayeredPane layeredPane = Singletons.getView().getFrame().getLayeredPane();
        int fromZoneX = 0, fromZoneY = 0;

        final Point zoneLocation = SwingUtilities.convertPoint(CMatchUI.SINGLETON_INSTANCE
                .getFieldControls()
                .get(1).getView().getLblLibrary(),
                Math.round(rctLibraryLabel.width / 2.0f), Math.round(rctLibraryLabel.height / 2.0f), layeredPane);
        fromZoneX = zoneLocation.x;
        fromZoneY = zoneLocation.y;
        int startWidth, startX, startY;
        startWidth = 10;
        startX = fromZoneX - Math.round(startWidth / 2.0f);
        startY = fromZoneY - Math.round(Math.round(startWidth * forge.view.arcane.CardPanel.ASPECT_RATIO) / 2.0f);

        int endWidth, endX, endY;
        forge.view.arcane.CardPanel toPanel = null;

        for (final Card card : diff) {
            toPanel = p.addCard(card);
            endWidth = toPanel.getCardWidth();
            final Point toPos = SwingUtilities.convertPoint(view.getHandArea(),
                    toPanel.getCardLocation(), layeredPane);
            endX = toPos.x;
            endY = toPos.y;

            final forge.view.arcane.CardPanel animationPanel = new forge.view.arcane.CardPanel(card);
            if (Singletons.getView().getFrame().isShowing()) {
                Animation.moveCard(startX, startY, startWidth, endX, endY, endWidth, animationPanel, toPanel,
                        layeredPane, 500);
            } else {
                Animation.moveCard(toPanel);
            }
        }
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#getCommandOnSelect()
     */
    @Override
    public Command getCommandOnSelect() {
        return null;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#update()
     */
    @Override
    public void update() {
    }
}
