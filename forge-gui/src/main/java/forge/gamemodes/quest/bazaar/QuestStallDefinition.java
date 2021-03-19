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
package forge.gamemodes.quest.bazaar;

import java.util.ArrayList;
import java.util.List;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

import forge.localinstance.skin.FSkinProp;

/**
 * <p>
 * QuestStallDefinition class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
@XStreamAlias("stall")
public class QuestStallDefinition {

    /** The name. */
    @XStreamAsAttribute
    private final String name;

    /** The display name. */
    @XStreamAsAttribute
    private final String displayName;

    @XStreamAsAttribute
    private final FSkinProp icon;

    private final String description;

    private final List<String> items;

    /**
     * <p>
     * Constructor for QuestStallDefinition.
     * </p> Not used anyway
     * 
     * @param name
     *            a {@link java.lang.String} object.
     * @param displayName
     *            a {@link java.lang.String} object.
     * @param description
     *            a {@link java.lang.String} object.
     * @param icon0
     *            a {@link fgd.ImageIcon} object.
     */
    private QuestStallDefinition() {
        name = null;
        displayName = null;
        description = null;
        items = new ArrayList<>();
        icon = null;
    }

    /**
     * Gets the fluff.
     * 
     * @return the fluff
     */
    public String getFluff() {
        return this.description;
    }

    /**
     * Gets the icon.
     * 
     * @return the icon
     */
    public FSkinProp getIcon() {
        return icon;
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
     * Gets the name.
     * 
     * @return the name
     */
    public String getName() {
        return this.name;
    }

    public List<String> getItems() {
        return items;
    }
}
