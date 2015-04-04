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

import com.google.common.base.Function;

import forge.Singletons;
import forge.UiCommand;
import forge.game.player.PlayerView;
import forge.game.zone.ZoneType;
import forge.gui.framework.ICDoc;
import forge.match.MatchConstants;
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
            matchUI.getCPrompt().selectPlayer(player, new MouseTriggerEvent(e));
        }
    };

    /**
     * Controls Swing components of a player's field instance.
     * 
     * @param player0 &emsp; {@link forge.game.player.Player}
     * @param v0 &emsp; {@link forge.screens.match.views.VField}
     * @param playerViewer 
     */
    public CField(final CMatchUI matchUI, final PlayerView player0, final VField v0) {
        this.matchUI = matchUI;
        this.player = player0;
        this.view = v0;

        final ZoneAction handAction      = new ZoneAction(matchUI, player, ZoneType.Hand,      MatchConstants.HUMANHAND);
        final ZoneAction libraryAction   = new ZoneAction(matchUI, player, ZoneType.Library,   MatchConstants.HUMANLIBRARY);
        final ZoneAction exileAction     = new ZoneAction(matchUI, player, ZoneType.Exile,     MatchConstants.HUMANEXILED);
        final ZoneAction graveAction     = new ZoneAction(matchUI, player, ZoneType.Graveyard, MatchConstants.HUMANGRAVEYARD);
        final ZoneAction flashBackAction = new ZoneAction(matchUI, player, ZoneType.Flashback, MatchConstants.HUMANFLASHBACK);
        final ZoneAction commandAction   = new ZoneAction(matchUI, player, ZoneType.Command,   MatchConstants.HUMANCOMMAND);

        final Function<Byte, Boolean> manaAction = new Function<Byte, Boolean>() {
            public Boolean apply(Byte colorCode) {
                if (CField.this.player.isLobbyPlayer(Singletons.getControl().getGuiPlayer())) {
                    return matchUI.getGameController().useMana(colorCode.byteValue());
                }
                return false;
            }
        };

        view.getDetailsPanel().setupMouseActions(handAction, libraryAction, exileAction, graveAction, flashBackAction, commandAction, manaAction);
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

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#getCommandOnSelect()
     */
    @Override
    public UiCommand getCommandOnSelect() {
        return null;
    }
} // End class CField
