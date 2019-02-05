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

import java.util.HashMap;
import java.util.Map;

import javax.swing.ScrollPaneConstants;
import javax.swing.WindowConstants;

import forge.assets.FSkinProp;
import forge.game.card.CardView;
import forge.game.player.PlayerView;
import forge.game.zone.ZoneType;
import forge.properties.ForgePreferences.FPref;
import forge.screens.match.CMatchUI;
import forge.toolbox.FScrollPane;
import forge.toolbox.FSkin;
import forge.util.Lang;

public class FloatingZone extends FloatingCardArea {
    private static final long serialVersionUID = 1927906492186378596L;

    private static final Map<Integer, FloatingZone> floatingAreas = new HashMap<Integer, FloatingZone>();

    private static int getKey(final PlayerView player, final ZoneType zone) {
        return 40 * player.getId() + zone.hashCode();
    }
    public static void showOrHide(final CMatchUI matchUI, final PlayerView player, final ZoneType zone) {
        final FloatingZone cardArea = _init(matchUI, player, zone);
        cardArea.showOrHideWindow();
    }
    public static void show(final CMatchUI matchUI, final PlayerView player, final ZoneType zone) {
        final FloatingZone cardArea = _init(matchUI, player, zone);
        cardArea.showWindow(); 
    }
    public static void hide(final CMatchUI matchUI, final PlayerView player, final ZoneType zone) {
        final FloatingZone cardArea = _init(matchUI, player, zone);
        cardArea.hideWindow(); 
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

    protected Iterable<CardView> getCards() {
	return player.getCards(zone);
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
        default:
            locPref = null;
            break;
        }
        zone = zone0;
        setPlayer(player0);
        setVertical(true);
    }

    private void setPlayer(PlayerView player0) {
        if (player == player0) { return; }
        player = player0;
        title = Lang.getPossessedObject(player0.getName(), zone.name()) + " (%d)";

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
        default:
            locPref = null;
            break;
        }
    }

}
