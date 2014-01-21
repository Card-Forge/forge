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
package forge.game.phase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.google.common.collect.Lists;

import forge.Command;
import forge.game.player.Player;


/**
 * <p>
 * Phase class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class Phase implements java.io.Serializable {

    private static final long serialVersionUID = 4665309652476851977L;

    protected final PhaseType type; // mostly decorative field - it's never used

    public Phase(PhaseType type) {
        this.type = type;
    }

    /** The at. */
    protected final List<Command> at = new ArrayList<Command>();
    /**
     * <p>
     * Add a hardcoded trigger that will execute "at <phase>".
     * </p>
     * 
     * @param c
     *            a {@link forge.Command} object.
     */
    public final void addAt(final Command c) {
        this.at.add(0, c);
    }

    /**
     * <p>
     * Executes any hardcoded triggers that happen "at <phase>".
     * </p>
     */
    public void executeAt() {
        this.execute(this.at);
    }

    /** The until. */
    private final List<Command> until = new ArrayList<Command>();

    /**
     * <p>
     * Add a Command that will terminate an effect with "until <phase>".
     * </p>
     * 
     * @param c
     *            a {@link forge.Command} object.
     */
    public final void addUntil(final Command c) {
        this.until.add(0, c);
    }

    /**
     * <p>
     * Executes the termination of effects that apply "until <phase>".
     * </p>
     */
    public final void executeUntil() {
        this.execute(this.until);
    }

    /** The until map. */
    private final HashMap<Player, List<Command>> untilMap = new HashMap<Player, List<Command>>();

    /**
     * <p>
     * Add a Command that will terminate an effect with "until <Player's> next <phase>".
     * Use cleanup phase to terminate an effect with "until <Player's> next turn"
     */
    public final void addUntil(Player p, final Command c) {
        if (this.untilMap.containsKey(p)) {
            this.untilMap.get(p).add(0, c);
        } else {
            this.untilMap.put(p, Lists.newArrayList(c));
        }
    }

    /**
     * <p>
     * Executes the termination of effects that apply "until <Player's> next <phase>".
     * </p>
     * 
     * @param p
     *            the player the execute until for
     */
    public final void executeUntil(final Player p) {
        if (this.untilMap.containsKey(p)) {
            this.execute(this.untilMap.get(p));
        }
    }

    /**
     * <p>
     * execute.
     * </p>
     * 
     * @param c
     *            a {@link forge.CommandList} object.
     */
    protected void execute(final List<Command> c) {
        final int length = c.size();

        for (int i = 0; i < length; i++) {
            c.remove(0).run();
        }
    }

} //end class Phase
