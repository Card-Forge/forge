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

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.function.BiConsumer;
import java.util.function.Function;

import javax.swing.ButtonGroup;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.SwingUtilities;

import forge.game.player.PlayerView;
import forge.game.zone.ZoneType;
import forge.gui.framework.ICDoc;
import forge.interfaces.IGameController;
import forge.screens.match.CMatchUI;
import forge.screens.match.ZoneAction;
import forge.screens.match.views.VField;
import forge.toolbox.MouseTriggerEvent;
import forge.util.Localizer;
import forge.view.arcane.FloatingZone;

/**
 * Controls Swing components of a player's field instance.
 */
public class CField implements ICDoc {
    private final CMatchUI matchUI;
    // The one who owns cards on this side of table
    private final PlayerView player;
    private final VField view;
    private boolean initializedAlready = false;

    private final MouseListener madAvatar = new MouseAdapter() {
        @Override
        public void mousePressed(final MouseEvent e) {
            if (SwingUtilities.isRightMouseButton(e)) {
                matchUI.showFullControl(player, e);
            } else {
                matchUI.getGameController().selectPlayer(player, new MouseTriggerEvent(e));
            }
        }
    };

    /**
     * Controls Swing components of a player's field instance.
     */
    public CField(final CMatchUI matchUI, final PlayerView player0, final VField v0) {
        this.matchUI = matchUI;
        this.player = player0;
        this.view = v0;

        final Function<Byte, Boolean> manaAction = colorCode -> {
            IGameController controller = matchUI.getGameController(player);
            // not a local human
            if (controller == null) {
                return Boolean.FALSE;
            }
            final int oldMana = player.getMana(colorCode);
            controller.useMana(colorCode);
            return oldMana != player.getMana(colorCode);
        };

        Function<ZoneType, Runnable> zoneActionFactory = (zone) -> new ZoneAction(matchUI, player, zone);

        final BiConsumer<ZoneType, MouseEvent> zoneRightClick = (zone, e) -> {
            final Localizer localizer = Localizer.getInstance();
            final JPopupMenu popup = new JPopupMenu();
            final ButtonGroup group = new ButtonGroup();
            final boolean isOwn = matchUI.isLocalPlayer(player);
            final boolean currentlyTabMode = FloatingZone.isTabMode(zone, isOwn);

            final JRadioButtonMenuItem windowItem = new JRadioButtonMenuItem(localizer.getMessage("lblOpenInWindow"));
            final JRadioButtonMenuItem tabItem = new JRadioButtonMenuItem(localizer.getMessage("lblAddTabToHandPanel"));
            windowItem.setSelected(!currentlyTabMode);
            tabItem.setSelected(currentlyTabMode);
            group.add(windowItem);
            group.add(tabItem);

            windowItem.addActionListener(evt -> {
                if (currentlyTabMode) {
                    FloatingZone.setTabMode(zone, false, isOwn);
                    FloatingZone.closeExisting(matchUI, player, zone);
                    FloatingZone.showOrHide(matchUI, player, zone);
                }
            });
            tabItem.addActionListener(evt -> {
                if (!currentlyTabMode) {
                    FloatingZone.setTabMode(zone, true, isOwn);
                    FloatingZone.closeExisting(matchUI, player, zone);
                    FloatingZone.showOrHide(matchUI, player, zone);
                }
            });

            popup.add(windowItem);
            popup.add(tabItem);
            popup.show(e.getComponent(), e.getX(), e.getY());
        };

        view.getDetailsPanel().setupMouseActions(zoneActionFactory, zoneRightClick, manaAction);
    }

    public final CMatchUI getMatchUI() {
        return matchUI;
    }

    @Override
    public void register() {
    }

    @Override
    public void initialize() {
        if (initializedAlready) { return; }
        initializedAlready = true;

        // Listeners
        // Player select
        this.view.getAvatarArea().addMouseListener(madAvatar);
    }

    @Override
    public void update() {
    }

}
