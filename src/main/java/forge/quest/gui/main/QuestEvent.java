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
package forge.quest.gui.main;

import forge.deck.Deck;

/**
 * <p>
 * QuestEvent.
 * </p>
 * 
 * MODEL - A basic event instance in Quest mode. Can be extended for use in
 * unique event types: battles, quests, and others.
 */
public class QuestEvent {
    // Default vals if none provided in the event file.
    /** The event deck. */
    private Deck eventDeck = null;

    /** The title. */
    private String title = "Mystery Event";

    /** The description. */
    private String description = "";

    /** The difficulty. */
    private String difficulty = "Medium";

    /** The icon. */
    private String icon = "Unknown.jpg";

    /** The name. */
    private String name = "Noname";

    /** The event type. */
    private String eventType = null;

    /**
     * <p>
     * getTitle.
     * </p>
     * 
     * @return a {@link java.lang.String}.
     */
    public final String getTitle() {
        return this.title;
    }

    /**
     * <p>
     * getDifficulty.
     * </p>
     * 
     * @return a {@link java.lang.String}.
     */
    public final String getDifficulty() {
        return this.difficulty;
    }

    /**
     * <p>
     * getDescription.
     * </p>
     * 
     * @return a {@link java.lang.String}.
     */
    public final String getDescription() {
        return this.description;
    }

    /**
     * <p>
     * getEventDeck.
     * </p>
     * 
     * @return {@link forge.deck.Deck}
     */
    public final Deck getEventDeck() {
        return this.eventDeck;
    }

    /**
     * <p>
     * getEventDeck.
     * </p>
     * 
     * @return {@link forge.deck.Deck}
     */
    public final String getEventType() {
        return this.eventType;
    }

    /**
     * <p>
     * getIcon.
     * </p>
     * 
     * @return a {@link java.lang.String}.
     */
    public final String getIcon() {
        return this.icon;
    }

    /**
     * <p>
     * getName.
     * </p>
     * 
     * @return a {@link java.lang.String}.
     */
    public final String getName() {
        return this.name;
    }

    /**
     * Sets the event type.
     * 
     * @param eventType0
     *            the eventType to set
     */
    public void setEventType(final String eventType0) {
        this.eventType = eventType0;
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

    /**
     * Sets the title.
     * 
     * @param title0
     *            the title to set
     */
    public void setTitle(final String title0) {
        this.title = title0;
    }

    /**
     * Sets the difficulty.
     * 
     * @param difficulty0
     *            the difficulty to set
     */
    public void setDifficulty(final String difficulty0) {
        this.difficulty = difficulty0;
    }

    /**
     * Sets the description.
     * 
     * @param description0
     *            the description to set
     */
    public void setDescription(final String description0) {
        this.description = description0;
    }

    /**
     * Sets the event deck.
     * 
     * @param eventDeck0
     *            the eventDeck to set
     */
    public void setEventDeck(final Deck eventDeck0) {
        this.eventDeck = eventDeck0;
    }

    /**
     * Sets the icon.
     * 
     * @param icon0
     *            the icon to set
     */
    public void setIcon(final String icon0) {
        this.icon = icon0;
    }
}
