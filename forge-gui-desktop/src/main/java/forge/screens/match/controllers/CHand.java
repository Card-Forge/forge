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

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JLayeredPane;
import javax.swing.SwingUtilities;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import forge.FThreads;
import forge.Singletons;
import forge.game.card.CardView;
import forge.game.player.PlayerView;
import forge.gui.framework.ICDoc;
import forge.screens.match.CMatchUI;
import forge.screens.match.views.VField;
import forge.screens.match.views.VHand;
import forge.view.arcane.CardPanel;
import forge.view.arcane.HandArea;
import forge.view.arcane.util.Animation;
import forge.view.arcane.util.CardPanelMouseAdapter;

/**
 * Controls Swing components of a player's hand instance.
 */
public class CHand implements ICDoc {
    private final CMatchUI matchUI;
    private final PlayerView player;
    private final VHand view;
    private final List<CardView> ordering = Lists.newArrayList();

    /**
     * Controls Swing components of a player's hand instance.
     */
    public CHand(final CMatchUI matchUI, final PlayerView p0, final VHand v0) {
        this.matchUI = matchUI;
        this.player = p0;
        this.view = v0;
        v0.getHandArea().addCardPanelMouseListener(new CardPanelMouseAdapter() {
            @Override
            public void mouseDragEnd(final CardPanel dragPanel, final MouseEvent evt) {
                //update index of dragged card in hand zone to match new index within hand area
                final int index = CHand.this.view.getHandArea().getCardPanels().indexOf(dragPanel);
                synchronized (ordering) {
                    ordering.remove(dragPanel.getCard());
                    ordering.add(index, dragPanel.getCard());
                    matchUI.getGameController(p0).reorderHand(dragPanel.getCard(), index);
                }
            }
        });
    }

    @Override
    public void register() {
    }

    @Override
    public void initialize() {
    }

    public void updateHand() {
        FThreads.assertExecutedByEdt(true);

        final HandArea p = view.getHandArea();

        final VField vf = matchUI.getFieldViewFor(player);
        if (vf == null) {
            return;
        }
        final Rectangle rctLibraryLabel = vf.getDetailsPanel().getLblLibrary().getBounds();

        // Animation starts from the library label and runs to the hand panel.
        // This check prevents animation running if label hasn't been realized yet.
        if (rctLibraryLabel.isEmpty()) {
            return;
        }

        // Don't perform animations if the user's in another tab.
        if (!matchUI.isCurrentScreen()) {
            return;
        }

        //update card panels in hand area
        final List<CardView> cards;
        if (player.getHand() == null) {
            cards = ImmutableList.of();
        } else {
            synchronized (player) {
                cards = ImmutableList.copyOf(player.getHand());
            }
        }

        synchronized (ordering) {
            ordering.clear();
            ordering.addAll(cards);
        }

        final List<CardPanel> placeholders = new ArrayList<CardPanel>();
        final List<CardPanel> cardPanels = new ArrayList<CardPanel>();

        for (final CardView card : ordering) {
            CardPanel cardPanel = p.getCardPanel(card.getId());
            if (cardPanel == null) { //create placeholders for new cards
                cardPanel = new CardPanel(matchUI, card);
                cardPanel.setDisplayEnabled(false);
                placeholders.add(cardPanel);
            }
            else {
                cardPanel.setCard(card); //ensure card view is updated
            }
            cardPanels.add(cardPanel);
        }

        p.setCardPanels(cardPanels);

        //animate new cards into positions defined by placeholders
        final JLayeredPane layeredPane = Singletons.getView().getFrame().getLayeredPane();
        int fromZoneX = 0, fromZoneY = 0;

        final Point zoneLocation = SwingUtilities.convertPoint(vf.getDetailsPanel().getLblLibrary(),
                Math.round(rctLibraryLabel.width / 2.0f), Math.round(rctLibraryLabel.height / 2.0f), layeredPane);
        fromZoneX = zoneLocation.x;
        fromZoneY = zoneLocation.y;
        int startWidth, startX, startY;
        startWidth = 10;
        startX = fromZoneX - Math.round(startWidth / 2.0f);
        startY = fromZoneY - Math.round(Math.round(startWidth * forge.view.arcane.CardPanel.ASPECT_RATIO) / 2.0f);

        int endWidth, endX, endY;

        for (final CardPanel placeholder : placeholders) {
            endWidth = placeholder.getCardWidth();
            final Point toPos = SwingUtilities.convertPoint(view.getHandArea(), placeholder.getCardLocation(), layeredPane);
            endX = toPos.x;
            endY = toPos.y;

            if (Singletons.getView().getFrame().isShowing()) {
                final CardPanel animationPanel = new CardPanel(matchUI, placeholder.getCard());
                Animation.moveCard(startX, startY, startWidth, endX, endY, endWidth, animationPanel, placeholder,
                        layeredPane, 500);
            }
            else {
                Animation.moveCard(placeholder);
            }
        }
    }

    @Override
    public void update() {
        updateHand();
    }
}
