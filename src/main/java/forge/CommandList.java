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

import java.util.ArrayList;
import java.util.Iterator;

/**
 * <p>
 * CommandList class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class CommandList implements java.io.Serializable, Command, Iterable<Command> {
    /** Constant <code>serialVersionUID=-1532687201812613302L</code>. */
    private static final long serialVersionUID = -1532687201812613302L;

    private final ArrayList<Command> a = new ArrayList<Command>();

    /**
     * default constructor TODO Write javadoc for Constructor.
     */
    public CommandList() {
        // nothing to do
    }

    /**
     * constructor TODO Write javadoc for Constructor.
     * 
     * @param c
     *            a Command
     */
    public CommandList(final Command c) {
        this.a.add(c);
    }

    /**
     * <p>
     * iterator.
     * </p>
     * 
     * @return a {@link java.util.Iterator} object.
     */
    @Override
    public final Iterator<Command> iterator() {
        return this.a.iterator();
    }

    // bug fix, when token is pumped up like with Giant Growth
    // and Sorceress Queen targets token, the effects need to be done
    // in this order, weird I know, DO NOT CHANGE THIS
    /**
     * <p>
     * add.
     * </p>
     * 
     * @param c
     *            a {@link forge.Command} object.
     */
    public final void add(final Command c) {
        this.a.add(0, c);
    }

    /**
     * <p>
     * get.
     * </p>
     * 
     * @param i
     *            a int.
     * @return a {@link forge.Command} object.
     */
    public final Command get(final int i) {
        return this.a.get(i);
    }

    /**
     * <p>
     * remove.
     * </p>
     * 
     * @param i
     *            a int.
     * @return a {@link forge.Command} object.
     */
    public final Command remove(final int i) {
        return this.a.remove(i);
    }

    /**
     * <p>
     * size.
     * </p>
     * 
     * @return a int.
     */
    public final int size() {
        return this.a.size();
    }

    /**
     * <p>
     * clear.
     * </p>
     */
    public final void clear() {
        this.a.clear();
    }

    /**
     * <p>
     * execute.
     * </p>
     */
    @Override
    public final void execute() {
        for (int i = 0; i < this.size(); i++) {
            this.get(i).execute();
        }
    }

}
