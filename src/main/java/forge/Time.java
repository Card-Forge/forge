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
 * Time class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class Time {
    private long startTime;
    private long stopTime;

    /**
     * <p>
     * Constructor for Time.
     * </p>
     */
    public Time() {
        this.start();
    }

    /**
     * <p>
     * start.
     * </p>
     */
    public final void start() {
        this.startTime = System.currentTimeMillis();
    }

    /**
     * <p>
     * stop.
     * </p>
     * 
     * @return a double.
     */
    public final double stop() {
        this.stopTime = System.currentTimeMillis();
        return this.getTime();
    }

    /**
     * <p>
     * getTime.
     * </p>
     * 
     * @return a double.
     */
    public final double getTime() {
        return (this.stopTime - this.startTime) / 1000.0;
    }
}
