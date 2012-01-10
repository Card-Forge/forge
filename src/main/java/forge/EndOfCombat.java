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
package forge;

/**
 * <p>
 * Handles "until end of combat" effects and "at end of combat" hardcoded triggers.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class EndOfCombat implements java.io.Serializable {

    /** Constant <code>serialVersionUID=3035250030566186842L</code>. */
    private static final long serialVersionUID = 3035250030566186842L;

    private final CommandList at = new CommandList();
    private final CommandList until = new CommandList();

    /**
     * <p>
     * Add a hardcoded trigger that will execute "at end of combat".
     * </p>
     * 
     * @param c
     *            a {@link forge.Command} object.
     */
    public final void addAt(final Command c) {
        this.at.add(c);
    }

    /**
     * <p>
     * Add a Command that will terminate an effect with "until end of combat".
     * </p>
     * 
     * @param c
     *            a {@link forge.Command} object.
     */
    public final void addUntil(final Command c) {
        this.until.add(c);
    }

    /**
     * <p>
     * Executes any hardcoded triggers that happen "at end of combat".
     * </p>
     */
    public final void executeAt() {
        this.execute(this.at);
    }

    /**
     * <p>
     * Executes the termination of effects that apply "until end of combat".
     * </p>
     */
    public final void executeUntil() {
        this.execute(this.until);
    }

    /**
     * <p>
     * execute.
     * </p>
     * 
     * @param c
     *            a {@link forge.CommandList} object.
     */
    private void execute(final CommandList c) {
        final int length = c.size();

        for (int i = 0; i < length; i++) {
            c.remove(0).execute();
        }
    }

} // end class EndOfCombat
