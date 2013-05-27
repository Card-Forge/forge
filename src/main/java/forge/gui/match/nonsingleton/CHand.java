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
import forge.FThreads;
import forge.Singletons;
import forge.game.player.Player;
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
public class CHand implements ICDoc, Observer {
    private final Player player;
    private final VHand view;
    private boolean initializedAlready = false;

    private final MouseListener madCardClick = new MouseAdapter() { @Override
        public void mousePressed(final MouseEvent e) {
            cardclickAction(e); } };


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

        if (player != null)
            player.getZone(ZoneType.Hand).addObserver(this);

        HandArea area = view.getHandArea();
        area.addMouseListener(madCardClick);
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

    private void cardclickAction(final MouseEvent e) {
        if (e.getButton() != MouseEvent.BUTTON1) {
            return;
        }
        final Card c = view.getHandArea().getHoveredCard(e);
        if (c != null) {
            CMessage.SINGLETON_INSTANCE.getInputControl().selectCard(c);
        }
    }

    public void update(final Observable a, final Object b) {
        FThreads.invokeInEdtNowOrLater(updateRoutine);
    }
    
    private final Runnable updateRoutine = new Runnable() { 
        @Override public void run() { updateHand(); }
    };
    
    public void updateHand() {
        FThreads.assertExecutedByEdt(true);
        
        final HandArea p = view.getHandArea();

        VField vf = CMatchUI.SINGLETON_INSTANCE.getFieldViewFor(player);
        final Rectangle rctLibraryLabel = vf.getLblLibrary().getBounds();
        final List<Card> cc = player.getZone(ZoneType.Hand).getCards();

        // Animation starts from the library label and runs to the hand panel.
        // This check prevents animation running if label hasn't been realized yet.
        if (rctLibraryLabel.isEmpty() ) {
            return;
        }
/* || p.getWidth() <= 0 */

        List<Card> tmp, diff;
        tmp = new ArrayList<Card>();
        for (final forge.view.arcane.CardPanel cpa : p.getCardPanels()) {
            tmp.add(cpa.getGameCard());
        }
        diff = new ArrayList<Card>(tmp);
        diff.removeAll(cc);
        if (diff.size() == p.getCardPanels().size()) {
            p.clear();
        } else {
            for (final Card card : diff) {
                p.removeCardPanel(p.getCardPanel(card.getUniqueNumber()));
            }
        }
        diff = new ArrayList<Card>(cc);
        diff.removeAll(tmp);

        JLayeredPane layeredPane = Singletons.getView().getFrame().getLayeredPane();
        int fromZoneX = 0, fromZoneY = 0;

        final Point zoneLocation = SwingUtilities.convertPoint(vf.getLblLibrary(),
                Math.round(rctLibraryLabel.width / 2.0f), Math.round(rctLibraryLabel.height / 2.0f), layeredPane);
        fromZoneX = zoneLocation.x;
        fromZoneY = zoneLocation.y;
        int startWidth, startX, startY;
        startWidth = 10;
        startX = fromZoneX - Math.round(startWidth / 2.0f);
        startY = fromZoneY - Math.round(Math.round(startWidth * forge.view.arcane.CardPanel.ASPECT_RATIO) / 2.0f);

        int endWidth, endX, endY;

        for (final Card card : diff) {
            CardPanel toPanel = p.addCard(card);
            endWidth = toPanel.getCardWidth();
            final Point toPos = SwingUtilities.convertPoint(view.getHandArea(), toPanel.getCardLocation(), layeredPane);
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
