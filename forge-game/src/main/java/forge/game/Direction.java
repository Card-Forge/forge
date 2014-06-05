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
package forge.game;

import java.util.List;

import com.google.common.collect.ImmutableList;

/**
 *  Represents a direction (left or right).
 */
public enum Direction {
	Left,
	Right;
	
	private static final String LEFT = "Left";
	private static final String RIGHT = "Right";
	/** Immutable list of all directions (in order, Left and Right). */
	private static final List<Direction> listOfDirections =
			ImmutableList.of(getDefaultDirection(), getDefaultDirection().getOtherDirection());

	/** @return The default direction. */
	public static final Direction getDefaultDirection() { return Left; }
	
	/** @return Immutable list of all directions (in order, Left and Right). */
	public static List<Direction> getListOfDirections() { return listOfDirections; }
	
	/** @return True if and only if this is the default direction. */
	public boolean isDefaultDirection() {
		return this.equals(getDefaultDirection());
	}
	
	/**
	 * Get the index by which the turn order is shifted, given this Direction.
	 * @return 1 or -1.
	 */
	public int getShift() {
		if (this.isDefaultDirection()) {
			return 1;
		}
		return -1;
	}
	
	/**
	 * Give the other Direction.
	 * @return Right if this is Left, and vice versa.
	 */
	public Direction getOtherDirection() {
		switch (this) {
		case Left:
			return Direction.Right;
		case Right:
			return Direction.Left;
		}
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public String toString() {
		switch(this) {
		case Left:
			return LEFT;
		case Right:
			return RIGHT;
		}
		return null;
	}
}
