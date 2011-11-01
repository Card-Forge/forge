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
     * @param eventType the eventType to set
     */
    public void setEventType(String eventType) {
        this.eventType = eventType; // TODO: Add 0 to parameter's name.
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name; // TODO: Add 0 to parameter's name.
    }

    /**
     * @param title the title to set
     */
    public void setTitle(String title) {
        this.title = title; // TODO: Add 0 to parameter's name.
    }

    /**
     * @param difficulty the difficulty to set
     */
    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty; // TODO: Add 0 to parameter's name.
    }

    /**
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description; // TODO: Add 0 to parameter's name.
    }

    /**
     * @param eventDeck the eventDeck to set
     */
    public void setEventDeck(Deck eventDeck) {
        this.eventDeck = eventDeck; // TODO: Add 0 to parameter's name.
    }

    /**
     * @param icon the icon to set
     */
    public void setIcon(String icon) {
        this.icon = icon; // TODO: Add 0 to parameter's name.
    }
}
