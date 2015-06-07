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

import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.ScrollPaneConstants;
import javax.swing.Timer;

import forge.Singletons;
import forge.assets.FSkinProp;
import forge.game.card.CardView;
import forge.game.player.PlayerView;
import forge.game.zone.ZoneType;
import forge.gui.framework.SDisplayUtil;
import forge.model.FModel;
import forge.properties.ForgePreferences;
import forge.properties.ForgePreferences.FPref;
import forge.screens.match.CMatchUI;
import forge.toolbox.FMouseAdapter;
import forge.toolbox.FScrollPane;
import forge.toolbox.FSkin;
import forge.toolbox.MouseTriggerEvent;
import forge.util.collect.FCollectionView;
import forge.util.Lang;
import forge.view.FDialog;
import forge.view.FFrame;

public class FloatingCardArea extends CardArea {
    private static final long serialVersionUID = 1927906492186378596L;

    private static final String COORD_DELIM = ","; 

    private static final ForgePreferences prefs = FModel.getPreferences();
    private static final Map<Integer, FloatingCardArea> floatingAreas = new HashMap<Integer, FloatingCardArea>();

    private static int getKey(final PlayerView player, final ZoneType zone) {
        return 40 * player.getId() + zone.hashCode();
    }
    public static void showOrHide(final CMatchUI matchUI, final PlayerView player, final ZoneType zone) {
        final FloatingCardArea cardArea = _init(matchUI, player, zone);
        cardArea.showOrHideWindow();
    }
    public static void show(final CMatchUI matchUI, final PlayerView player, final ZoneType zone) {
        final FloatingCardArea cardArea = _init(matchUI, player, zone);
        cardArea.showWindow(); 
    }
    private static FloatingCardArea _init(final CMatchUI matchUI, final PlayerView player, final ZoneType zone) {
        final int key = getKey(player, zone);
        FloatingCardArea cardArea = floatingAreas.get(key);
        if (cardArea == null || cardArea.getMatchUI() != matchUI) {
            cardArea = new FloatingCardArea(matchUI, player, zone);
            floatingAreas.put(key, cardArea);
        } else {
            cardArea.setPlayer(player); //ensure player is updated if needed
        }
        return cardArea;
    }
    public static CardPanel getCardPanel(final CMatchUI matchUI, final CardView card) {
        final FloatingCardArea window = _init(matchUI, card.getController(), card.getZone());
        return window.getCardPanel(card.getId());
    }
    public static void refresh(final PlayerView player, final ZoneType zone) {
        FloatingCardArea cardArea = floatingAreas.get(getKey(player, zone));
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
        for (final FloatingCardArea cardArea : floatingAreas.values()) {
            cardArea.window.setVisible(false);
        }
        floatingAreas.clear();
    }

    private final ZoneType zone;
    private PlayerView player;
    private String title;
    private FPref locPref;
    private boolean hasBeenShown, locLoaded;

    @SuppressWarnings("serial")
    private final FDialog window = new FDialog(false, true, "0") {
        @Override
        public void setLocationRelativeTo(Component c) {
            //don't change location this way if dialog has already been shown or location was loaded from preferences
            if (hasBeenShown || locLoaded) { return; }
            super.setLocationRelativeTo(c);
        }

        @Override
        public void setVisible(boolean b0) {
            if (isVisible() == b0) { return; }
            if (!b0 && hasBeenShown && locPref != null) {
                //update preference before hiding window, as otherwise its location will be 0,0
                prefs.setPref(locPref,
                        getX() + COORD_DELIM + getY() + COORD_DELIM +
                        getWidth() + COORD_DELIM + getHeight());
                //don't call prefs.save(), instead allowing them to be saved when match ends
            }
            super.setVisible(b0);
            if (b0) {
                refresh();
                hasBeenShown = true;
            }
        }
    };

    private FloatingCardArea(final CMatchUI matchUI, final PlayerView player0, final ZoneType zone0) {
        super(matchUI, new FScrollPane(false, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER));
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

    private void showWindow() {
        onShow();
        window.setVisible(true);
    }
    private void showOrHideWindow() {
        onShow();
        window.setVisible(!window.isVisible());
    }
    private void onShow() {
        if (!hasBeenShown) {
            loadLocation();
            window.getTitleBar().addMouseListener(new FMouseAdapter() {
                @Override public final void onLeftDoubleClick(final MouseEvent e) {
                    window.setVisible(false); //hide window if titlebar double-clicked
                }
            });
        }
    }

    private void loadLocation() {
        if (locPref != null) {
            String value = prefs.getPref(locPref);
            if (value.length() > 0) {
                String[] coords = value.split(COORD_DELIM);
                if (coords.length == 4) {
                    try {
                        int x = Integer.parseInt(coords[0]);
                        int y = Integer.parseInt(coords[1]);
                        int w = Integer.parseInt(coords[2]);
                        int h = Integer.parseInt(coords[3]);
    
                        //ensure the window is accessible
                        int centerX = x + w / 2;
                        int centerY = y + h / 2;
                        Rectangle screenBounds = SDisplayUtil.getScreenBoundsForPoint(new Point(centerX, centerY)); 
                        if (centerX < screenBounds.x) {
                            x = screenBounds.x;
                        }
                        else if (centerX > screenBounds.x + screenBounds.width) {
                            x = screenBounds.x + screenBounds.width - w;
                            if (x < screenBounds.x) {
                                x = screenBounds.x;
                            }
                        }
                        if (centerY < screenBounds.y) {
                            y = screenBounds.y;
                        }
                        else if (centerY > screenBounds.y + screenBounds.height) {
                            y = screenBounds.y + screenBounds.height - h;
                            if (y < screenBounds.y) {
                                y = screenBounds.y;
                            }
                        }
                        window.setBounds(x, y, w, h);
                        locLoaded = true;
                        return;
                    }
                    catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
                prefs.setPref(locPref, ""); //clear value if invalid
                prefs.save();
            }
        }
        //fallback default size
        FFrame mainFrame = Singletons.getView().getFrame();
        window.setSize(mainFrame.getWidth() / 5, mainFrame.getHeight() / 2);
    }

    private void refresh() {
        if (!window.isVisible()) { return; } //don't refresh while window hidden

        List<CardPanel> cardPanels = new ArrayList<CardPanel>();
        FCollectionView<CardView> cards = player.getCards(zone);
        if (cards != null) {
            for (final CardView card : cards) {
                CardPanel cardPanel = getCardPanel(card.getId());
                if (cardPanel == null) {
                    cardPanel = new CardPanel(getMatchUI(), card);
                    cardPanel.setDisplayEnabled(true);
                }
                else {
                    cardPanel.setCard(card); //ensure card view updated
                }
                cardPanels.add(cardPanel);
            }
        }

        boolean hadCardPanels = getCardPanels().size() > 0;
        setCardPanels(cardPanels);
        window.setTitle(String.format(title, cardPanels.size()));

        //if window had cards and now doesn't, hide window
        //(e.g. cast final card from Flashback zone)
        if (hadCardPanels && cardPanels.size() == 0) {
            window.setVisible(false);
        }
    }

    @Override
    public void doLayout() {
        if (window.isResizing()) {
            //delay layout slightly to reduce flicker during window resize
            layoutTimer.restart();
        }
        else {
            finishDoLayout();
        }
    }

    private final Timer layoutTimer = new Timer(250, new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            layoutTimer.stop();
            finishDoLayout();
        }
    });

    private void finishDoLayout() {
        super.doLayout();
    }

    @Override
    public final void mouseOver(final CardPanel panel, final MouseEvent evt) {
        getMatchUI().setCard(panel.getCard(), evt.isShiftDown());
        super.mouseOver(panel, evt);
    }
    @Override
    public final void mouseLeftClicked(final CardPanel panel, final MouseEvent evt) {
        getMatchUI().getGameController().selectCard(panel.getCard(), null, new MouseTriggerEvent(evt));
        super.mouseLeftClicked(panel, evt);
    }
    @Override
    public final void mouseRightClicked(final CardPanel panel, final MouseEvent evt) {
        getMatchUI().getGameController().selectCard(panel.getCard(), null, new MouseTriggerEvent(evt));
        super.mouseRightClicked(panel, evt);
    }
}
