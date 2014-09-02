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

import forge.UiCommand;
import forge.gui.framework.ICDoc;
import forge.screens.match.views.VCommand;
import forge.view.PlayerView;

/**
 * Controls Swing components of a player's command instance.
 */
public class CCommand implements ICDoc {
    private final PlayerView player;
    private final VCommand view;

    /**
     * Controls Swing components of a player's command instance.
     * 
     * @param player2 &emsp; {@link forge.game.player.Player}
     * @param v0 &emsp; {@link forge.screens.match.views.VCommand}
     */
    public CCommand(final PlayerView player2, final VCommand v0) {
        this.player = player2;
        this.view = v0;
    }

    @Override
    public void initialize() {
    }

    @Override
    public void update() {
    }

    /** @return {@link forge.game.player.Player} */
    public PlayerView getPlayer() {
        return this.player;
    }

    /** @return {@link forge.screens.match.views.VField} */
    public VCommand getView() {
        return this.view;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#getCommandOnSelect()
     */
    @Override
    public UiCommand getCommandOnSelect() {
        return null;
    }
} // End class CCommand
