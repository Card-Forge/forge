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
package forge.quest.data.bazaar;

/**
 * <p>
 * QuestStallDefinition class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class QuestStallDefinition {

    /** The name. */
    private String name;

    /** The display name. */
    private String displayName;

    /** The icon name. */
    private String iconName;

    /** The fluff. */
    private String fluff;

    /**
     * <p>
     * Constructor for QuestStallDefinition.
     * </p>
     * 
     * @param name
     *            a {@link java.lang.String} object.
     * @param displayName
     *            a {@link java.lang.String} object.
     * @param fluff
     *            a {@link java.lang.String} object.
     * @param iconName
     *            a {@link java.lang.String} object.
     */
    public QuestStallDefinition(final String name, final String displayName, final String fluff, final String iconName) {
        this.setName(name);
        this.setDisplayName(displayName);
        this.setFluff(fluff);
        this.setIconName(iconName);
    }

    /**
     * Gets the fluff.
     * 
     * @return the fluff
     */
    public String getFluff() {
        return this.fluff;
    }

    /**
     * Sets the fluff.
     * 
     * @param fluff0
     *            the fluff to set
     */
    public void setFluff(final String fluff0) {
        this.fluff = fluff0;
    }

    /**
     * Gets the icon name.
     * 
     * @return the iconName
     */
    public String getIconName() {
        return this.iconName;
    }

    /**
     * Sets the icon name.
     * 
     * @param iconName0
     *            the iconName to set
     */
    public void setIconName(final String iconName0) {
        this.iconName = iconName0;
    }

    /**
     * Gets the display name.
     * 
     * @return the displayName
     */
    public String getDisplayName() {
        return this.displayName;
    }

    /**
     * Sets the display name.
     * 
     * @param displayName0
     *            the displayName to set
     */
    public void setDisplayName(final String displayName0) {
        this.displayName = displayName0;
    }

    /**
     * Gets the name.
     * 
     * @return the name
     */
    public String getName() {
        return this.name;
    }

    /**
     * Sets the name.
     * 
     * @param name0
     *            the name to set
     */
    public void setName(final String name0) {
        this.name = name0;
    }
}
