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
 * Command interface.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public interface Command extends java.io.Serializable {
    /** Constant <code>Blank</code>. */
    Command BLANK = new Command() {

        private static final long serialVersionUID = 2689172297036001710L;

        @Override
        public void execute() {
        }
    };

    /**
     * <p>
     * execute.
     * </p>
     */
    void execute();
}
