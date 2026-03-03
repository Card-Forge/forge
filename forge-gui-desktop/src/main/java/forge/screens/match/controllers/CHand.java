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
import java.util.Comparator;
import java.util.List;

import javax.swing.JLayeredPane;
import javax.swing.SwingUtilities;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import forge.Singletons;
import forge.game.card.CardView;
import forge.game.player.PlayerView;
import forge.game.zone.ZoneType;
import forge.gui.FThreads;
import forge.gui.framework.ICDoc;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.util.collect.FCollectionView;
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
                final int index = CHand.this.view.getHandArea().getCardPanels().indexOf(dragPanel);
                synchronized (ordering) {
                    ordering.remove(dragPanel.getCard());
                    ordering.add(index, dragPanel.getCard());
                    if (dragPanel.getCard().getZone() == ZoneType.Hand) {
                        // compute index among hand cards only (zone cards are interleaved
                        // visually but Zone.reorder() indexes within the hand zone)
                        int handIndex = 0;
                        for (final CardView cv : ordering) {
                            if (cv == dragPanel.getCard()) break;
                            if (cv.getZone() == ZoneType.Hand) handIndex++;
                        }
                        matchUI.getGameController(p0).reorderHand(dragPanel.getCard(), handIndex);
                    }
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

            final boolean showZoneCards = FModel.getPreferences().getPrefBoolean(FPref.UI_SHOW_PLAYABLE_ZONE_CARDS);
            final boolean orderByCmc = FModel.getPreferences().getPrefBoolean(FPref.UI_ORDER_HAND);

            if (orderByCmc && showZoneCards) {
                // Collect hand + zone cards, sort by zone group then CMC/color/name
                final List<CardView> allCards = new ArrayList<>(cards);
                final FCollectionView<CardView> flashbackCards = player.getFlashback();
                if (flashbackCards != null) {
                    for (final CardView cv : flashbackCards) {
                        if (cv.getZone() != null && cv.getZone() != ZoneType.Hand) {
                            allCards.add(cv);
                        }
                    }
                }
                allCards.sort(Comparator.comparingInt((CardView cv) -> zoneOrder(cv.getZone()))
                        .thenComparingInt(cv -> cv.getCurrentState().getManaCost().getCMC())
                        .thenComparing(cv -> cv.getCurrentState().getColors().getOrderWeight())
                        .thenComparing(cv -> cv.getCurrentState().getName()));
                ordering.addAll(allCards);
            } else {
                // Command zone cards first
                if (showZoneCards) {
                    final FCollectionView<CardView> flashbackCards = player.getFlashback();
                    if (flashbackCards != null) {
                        for (final CardView cv : flashbackCards) {
                            if (cv.getZone() == ZoneType.Command) {
                                ordering.add(cv);
                            }
                        }
                    }
                }

                // Sort hand by CMC/color at the UI layer. This duplicates the game-layer sort in
                // PlayerZone.onChanged(), but is necessary because network clients only have CardViews
                // (no access to the game model), and because toggling the preference mid-game needs
                // to take effect immediately without waiting for a zone change event.
                if (orderByCmc) {
                    final List<CardView> sorted = new ArrayList<>(cards);
                    sorted.sort(Comparator.comparingInt((CardView cv) -> cv.getCurrentState().getManaCost().getCMC())
                            .thenComparing(cv -> cv.getCurrentState().getColors().getOrderWeight())
                            .thenComparing(cv -> cv.getCurrentState().getName()));
                    ordering.addAll(sorted);
                } else {
                    ordering.addAll(cards);
                }

                // Other zone cards after hand
                if (showZoneCards) {
                    final FCollectionView<CardView> flashbackCards = player.getFlashback();
                    if (flashbackCards != null) {
                        for (final CardView cv : flashbackCards) {
                            if (cv.getZone() != null && cv.getZone() != ZoneType.Hand
                                    && cv.getZone() != ZoneType.Command) {
                                ordering.add(cv);
                            }
                        }
                    }
                }
            }
        }

        final List<CardPanel> placeholders = new ArrayList<>();
        final List<CardPanel> cardPanels = new ArrayList<>();

        for (final CardView card : ordering) {
            final ZoneType zone = card.getZone();
            final boolean isZoneCard = zone != null && zone != ZoneType.Hand;
            CardPanel cardPanel = p.getCardPanel(card.getId());
            if (cardPanel == null) {
                cardPanel = new CardPanel(matchUI, card);
                if (isZoneCard) {
                    cardPanel.setDisplayEnabled(true); // no animation for zone cards
                } else {
                    cardPanel.setDisplayEnabled(false);
                    placeholders.add(cardPanel);
                }
            }
            else {
                cardPanel.setCard(card); //ensure card view is updated
            }
            cardPanel.setZoneBanner(isZoneCard ? zone.getTranslatedName().toUpperCase() : null, isZoneCard ? zone : null);
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

    /** Sort order for zone-grouped CMC sorting: Command first, then Hand, then others. */
    private static int zoneOrder(final ZoneType zone) {
        if (zone == null) return 99;
        switch (zone) {
            case Command:   return 0;
            case Hand:      return 1;
            case Graveyard: return 2;
            case Exile:     return 3;
            case Library:   return 4;
            case Sideboard: return 5;
            default:        return 6;
        }
    }

    @Override
    public void update() {
        updateHand();
    }
}
