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
import java.util.function.Function;

import javax.swing.SwingUtilities;

import forge.game.player.PlayerView;
import forge.game.zone.ZoneType;
import forge.gamemodes.match.input.Input;
import forge.gamemodes.match.input.InputPayMana;
import forge.gui.framework.ICDoc;
import forge.player.PlayerControllerHuman;
import forge.screens.match.CMatchUI;
import forge.screens.match.ZoneAction;
import forge.screens.match.views.VField;
import forge.toolbox.MouseTriggerEvent;

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
            if (matchUI.getGameController() instanceof PlayerControllerHuman) {
                final PlayerControllerHuman controller = (PlayerControllerHuman) matchUI.getGameController();
                final Input ipm = controller.getInputQueue().getInput();
                if (ipm instanceof InputPayMana && ipm.getOwner().equals(player)) {
                    final int oldMana = player.getMana(colorCode);
                    controller.useMana(colorCode);
                    return oldMana != player.getMana(colorCode);
                }
            }
            return Boolean.FALSE;
        };

        Function<ZoneType, Runnable> zoneActionFactory = (zone) -> new ZoneAction(matchUI, player, zone);
        view.getDetailsPanel().setupMouseActions(zoneActionFactory, manaAction);
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
