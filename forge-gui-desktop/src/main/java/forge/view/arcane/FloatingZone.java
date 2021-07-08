/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Nate
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
package forge.view.arcane;

import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import javax.swing.ScrollPaneConstants;
import javax.swing.WindowConstants;

import forge.game.card.CardView;
import forge.game.player.PlayerView;
import forge.game.zone.ZoneType;
import forge.gui.FThreads;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.localinstance.skin.FSkinProp;
import forge.screens.match.CMatchUI;
import forge.toolbox.FMouseAdapter;
import forge.toolbox.FScrollPane;
import forge.toolbox.FSkin;
import forge.util.Localizer;
import forge.util.collect.FCollection;

public class FloatingZone extends FloatingCardArea {
    private static final long serialVersionUID = 1927906492186378596L;

    private static final Map<Integer, FloatingZone> floatingAreas = new HashMap<>();

    private static int getKey(final PlayerView player, final ZoneType zone) {
        return 40 * player.getId() + zone.hashCode();
    }

    public static void showOrHide(final CMatchUI matchUI, final PlayerView player, final ZoneType zone) {
        final FloatingZone cardArea = _init(matchUI, player, zone);
        cardArea.showOrHideWindow();
    }

    public static boolean show(final CMatchUI matchUI, final PlayerView player, final ZoneType zone) {
        final FloatingZone cardArea = _init(matchUI, player, zone);

        if (cardArea.isVisible()) {
            return false;
        }

        FThreads.invokeInEdtNowOrLater(new Runnable() {
            @Override public void run() {
                cardArea.showWindow();
            }
        });

        return true;
    }

    public static boolean hide(final CMatchUI matchUI, final PlayerView player, final ZoneType zone) {
        final FloatingZone cardArea = _init(matchUI, player, zone);

        if (!cardArea.isVisible()) {
            return false;
        }

        FThreads.invokeInEdtNowOrLater(new Runnable() {
            @Override public void run() {
                cardArea.hideWindow();
            }
        });

        return true;
    }

    private static FloatingZone _init(final CMatchUI matchUI, final PlayerView player, final ZoneType zone) {
        final int key = getKey(player, zone);
        FloatingZone cardArea = floatingAreas.get(key);
        if (cardArea == null || cardArea.getMatchUI() != matchUI) {
            cardArea = new FloatingZone(matchUI, player, zone);
            floatingAreas.put(key, cardArea);
        } else {
            cardArea.setPlayer(player); //ensure player is updated if needed
        }
        return cardArea;
    }

    public static CardPanel getCardPanel(final CMatchUI matchUI, final CardView card) {
        final FloatingZone window = _init(matchUI, card.getController(), card.getZone());
        return window.getCardPanel(card.getId());
    }

    public static void refresh(final PlayerView player, final ZoneType zone) {
        FloatingZone cardArea = floatingAreas.get(getKey(player, zone));
        if (cardArea != null) {
            cardArea.setPlayer(player); //ensure player is updated if needed
            cardArea.refresh();
        }

        //refresh flashback zone when graveyard, library, or exile zones updated
        switch (zone) {
            case Graveyard:
            case Library:
            case Exile:
                refresh(player, ZoneType.Flashback);
                break;
            default:
                break;
        }
    }

    public static void closeAll() {
        for (final FloatingZone cardArea : floatingAreas.values()) {
            cardArea.window.setVisible(false);
        }
        floatingAreas.clear();
    }

    public static void refreshAll() {
        for (final FloatingZone cardArea : floatingAreas.values()) {
            cardArea.refresh();
        }
    }

    private final ZoneType zone;
    private PlayerView player;

    protected boolean sortedByName = false;
    protected FCollection<CardView> cardList;

    private final Comparator<CardView> comp = new Comparator<CardView>() {
        @Override
        public int compare(CardView lhs, CardView rhs) {
            if (!getMatchUI().mayView(lhs)) {
                return (getMatchUI().mayView(rhs)) ? 1 : 0;
            } else if (!getMatchUI().mayView(rhs)) {
                return -1;
            } else {
                return lhs.getName().compareTo(rhs.getName());
            }
        }
    };

    protected Iterable<CardView> getCards() {
        Iterable<CardView> zoneCards = player.getCards(zone);
        if (zoneCards != null) {
            cardList = new FCollection<>(zoneCards);
            if (sortedByName) {
                Collections.sort(cardList, comp);
            }
            return cardList;
        } else {
            return null;
        }
    }

    private FloatingZone(final CMatchUI matchUI, final PlayerView player0, final ZoneType zone0) {
        super(matchUI, new FScrollPane(false, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER));
        window.add(getScrollPane(), "grow, push");
        window.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE); //pfps so that old content does not reappear?
        getScrollPane().setViewportView(this);
        setOpaque(false);
        switch (zone0) {
            case Exile:
                window.setIconImage(FSkin.getImage(FSkinProp.IMG_ZONE_EXILE));
                break;
            case Graveyard:
                window.setIconImage(FSkin.getImage(FSkinProp.IMG_ZONE_GRAVEYARD));
                break;
            case Hand:
                window.setIconImage(FSkin.getImage(FSkinProp.IMG_ZONE_HAND));
                break;
            case Library:
                window.setIconImage(FSkin.getImage(FSkinProp.IMG_ZONE_LIBRARY));
                break;
            case Flashback:
                window.setIconImage(FSkin.getImage(FSkinProp.IMG_ZONE_FLASHBACK));
                break;
            case Command:
                window.setIconImage(FSkin.getImage(FSkinProp.IMG_PLANESWALKER));
                break;
            case Ante:
                window.setIconImage(FSkin.getImage(FSkinProp.IMG_ZONE_ANTE));
                break;
            case Sideboard:
                window.setIconImage(FSkin.getImage(FSkinProp.IMG_ZONE_SIDEBOARD));
                break;
            default:
                locPref = null;
                break;
        }
        zone = zone0;
        setPlayer(player0);
        setVertical(true);
    }

    private void toggleSorted() {
        sortedByName = !sortedByName;
        setTitle();
        refresh();
        // revalidation does not appear to be necessary here
        getWindow().repaint();
    }

    @Override
    protected void onShow() {
        super.onShow();
        if (!hasBeenShown) {
            getWindow().getTitleBar().addMouseListener(new FMouseAdapter() {
                @Override
                public final void onRightClick(final MouseEvent e) {
                    toggleSorted();
                }
            });
        }
    }

    private void setTitle() {
        final String sort_detail = sortedByName ? Localizer.getInstance().getMessage("lblRightClickToUnSort") : Localizer.getInstance().getMessage("lblRightClickToSort");
        title = Localizer.getInstance().getMessage("lblPlayerZoneNCardSortStatus", player.getName(), zone.getTranslatedName(), "%d" , sort_detail);
    }

    private void setPlayer(PlayerView player0) {
        if (player == player0) {
            return;
        }
        player = player0;
        setTitle();

        boolean isAi = player0.isAI();
        switch (zone) {
            case Exile:
                locPref = isAi ? FPref.ZONE_LOC_AI_EXILE : FPref.ZONE_LOC_HUMAN_EXILE;
                break;
            case Graveyard:
                locPref = isAi ? FPref.ZONE_LOC_AI_GRAVEYARD : FPref.ZONE_LOC_HUMAN_GRAVEYARD;
                break;
            case Hand:
                locPref = isAi ? FPref.ZONE_LOC_AI_HAND : FPref.ZONE_LOC_HUMAN_HAND;
                break;
            case Library:
                locPref = isAi ? FPref.ZONE_LOC_AI_LIBRARY : FPref.ZONE_LOC_HUMAN_LIBRARY;
                break;
            case Flashback:
                locPref = isAi ? FPref.ZONE_LOC_AI_FLASHBACK : FPref.ZONE_LOC_HUMAN_FLASHBACK;
                break;
            case Command:
                locPref = isAi ? FPref.ZONE_LOC_AI_COMMAND : FPref.ZONE_LOC_HUMAN_COMMAND;
                break;
            case Ante:
                locPref = isAi ? FPref.ZONE_LOC_AI_ANTE : FPref.ZONE_LOC_HUMAN_ANTE;
                break;
            case Sideboard:
                locPref = isAi ? FPref.ZONE_LOC_AI_SIDEBOARD : FPref.ZONE_LOC_HUMAN_SIDEBOARD;
                break;
            default:
                locPref = null;
                break;
        }
    }

}
