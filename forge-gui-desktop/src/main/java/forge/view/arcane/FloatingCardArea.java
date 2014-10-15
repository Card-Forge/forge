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

import forge.Singletons;
import forge.assets.FSkinProp;
import forge.game.card.CardView;
import forge.game.player.PlayerView;
import forge.game.zone.ZoneType;
import forge.screens.match.CMatchUI;
import forge.screens.match.controllers.CPrompt;
import forge.toolbox.FMouseAdapter;
import forge.toolbox.FScrollPane;
import forge.toolbox.FSkin;
import forge.toolbox.MouseTriggerEvent;
import forge.util.FCollectionView;
import forge.util.Lang;
import forge.view.FDialog;
import forge.view.FFrame;

import java.awt.Component;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class FloatingCardArea extends CardArea {
    private static final long serialVersionUID = 1927906492186378596L;

    private static final HashMap<Integer, FloatingCardArea> floatingAreas = new HashMap<Integer, FloatingCardArea>();

    private static int getKey(PlayerView player, ZoneType zone) {
        return 40 * player.getId() + zone.hashCode();
    }
    public static void show(PlayerView player, ZoneType zone) {
        int key = getKey(player, zone);
        FloatingCardArea cardArea = floatingAreas.get(key);
        if (cardArea == null) {
            cardArea = new FloatingCardArea(player, zone);
            floatingAreas.put(key, cardArea);
        }
        cardArea.showWindow();
    }
    public static void refresh(PlayerView player, ZoneType zone) {
        FloatingCardArea cardArea = floatingAreas.get(getKey(player, zone));
        if (cardArea != null) {
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

    private final PlayerView player;
    private final ZoneType zone;
    private final String title;

    @SuppressWarnings("serial")
    private final FDialog window = new FDialog(false, true, "0") {
        @Override
        public void setLocationRelativeTo(Component c) {
            if (hasBeenShown) { return; } //don't change location this way if dialog has already been shown
            super.setLocationRelativeTo(c);
        }

        @Override
        public void setVisible(boolean b0) {
            if (isVisible() == b0) { return; }
            super.setVisible(b0);
            if (b0) {
                refresh();
                hasBeenShown = true;
            }
        }
    };
    private boolean hasBeenShown;

    private FloatingCardArea(PlayerView player0, ZoneType zone0) {
        super(new FScrollPane(false));
        window.add(getScrollPane(), "grow, push");
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
        default:
            break;
        }
        title = Lang.getPossessedObject(player0.getName(), zone0.name()) + " (%d)";
        player = player0;
        zone = zone0;
        setVertical(true);
    }

    private void showWindow() {
        if (!hasBeenShown) { //only set size if first time showing window
            FFrame mainFrame = Singletons.getView().getFrame();
            window.setSize(mainFrame.getWidth() / 4, mainFrame.getHeight() * 2 / 3);

            window.getTitleBar().addMouseListener(new FMouseAdapter() {
                @Override
                public void onLeftDoubleClick(MouseEvent e) {
                    window.setVisible(false); //hide window if titlebar double-clicked
                }
            });
        }
        window.setVisible(true);
    }

    public void refresh() {
        if (!window.isVisible()) { return; } //don't refresh while window hidden

        List<CardPanel> cardPanels = new ArrayList<CardPanel>();
        FCollectionView<CardView> cards = player.getCards(zone);
        if (cards != null) {
            for (final CardView card : cards) {
                CardPanel cardPanel = getCardPanel(card.getId());
                if (cardPanel == null) {
                    cardPanel = new CardPanel(card);
                    cardPanel.setDisplayEnabled(true);
                }
                cardPanels.add(cardPanel);
            }
        }
        setCardPanels(cardPanels);
        window.setTitle(String.format(title, cardPanels.size()));
    }

    @Override
    public final void mouseOver(final CardPanel panel, final MouseEvent evt) {
        CMatchUI.SINGLETON_INSTANCE.setCard(panel.getCard(), evt.isShiftDown());
        super.mouseOver(panel, evt);
    }
    @Override
    public final void mouseLeftClicked(final CardPanel panel, final MouseEvent evt) {
        CPrompt.SINGLETON_INSTANCE.selectCard(panel.getCard(), new MouseTriggerEvent(evt));
        super.mouseLeftClicked(panel, evt);
    }
    @Override
    public final void mouseRightClicked(final CardPanel panel, final MouseEvent evt) {
        CPrompt.SINGLETON_INSTANCE.selectCard(panel.getCard(), new MouseTriggerEvent(evt));
        super.mouseRightClicked(panel, evt);
    }
}
