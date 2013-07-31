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
package forge.gui.match.nonsingleton;

import java.awt.Event;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseMotionListener;
import forge.Card;
import forge.Command;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.gui.framework.ICDoc;
import forge.gui.match.CMatchUI;
import forge.gui.match.controllers.CMessage;
/**
 * Controls Swing components of a player's command instance.
 */
public class CCommand implements ICDoc {
    private final Player player;
    private final VCommand view;

    /**
     * Controls Swing components of a player's command instance.
     * 
     * @param p0 &emsp; {@link forge.game.player.Player}
     * @param v0 &emsp; {@link forge.gui.match.nonsingleton.VCommand}
     */
    public CCommand(final Player p0, final VCommand v0) {
        this.player = p0;
        this.view = v0;
    }

    @Override
    public void initialize() {
    }

    @Override
    public void update() {
    }

    /** @return {@link forge.game.player.Player} */
    public Player getPlayer() {
        return this.player;
    }

    /** @return {@link forge.gui.nonsingleton.VField} */
    public VCommand getView() {
        return this.view;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#getCommandOnSelect()
     */
    @Override
    public Command getCommandOnSelect() {
        return null;
    }
} // End class CCommand
