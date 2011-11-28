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

//handles "until end of combat" and "at end of combat" commands from cards
/**
 * <p>
 * EndOfCombat class.
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
     * addAt.
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
     * addUntil.
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
     * executeAt.
     * </p>
     */
    public final void executeAt() {
        // AllZone.getStateBasedEffects().rePopulateStateBasedList();
        this.execute(this.at);
    } // executeAt()

    /**
     * <p>
     * executeUntil.
     * </p>
     */
    public final void executeUntil() {
        this.execute(this.until);
    }

    /**
     * <p>
     * sizeAt.
     * </p>
     * 
     * @return a int.
     */
    public final int sizeAt() {
        return this.at.size();
    }

    /**
     * <p>
     * sizeUntil.
     * </p>
     * 
     * @return a int.
     */
    public final int sizeUntil() {
        return this.until.size();
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
