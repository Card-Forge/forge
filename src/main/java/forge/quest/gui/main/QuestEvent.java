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
    public Deck eventDeck = null;

    /** The title. */
    public String title = "Mystery Event";

    /** The description. */
    public String description = "";

    /** The difficulty. */
    public String difficulty = "Medium";

    /** The icon. */
    public String icon = "Unknown.jpg";

    /** The name. */
    public String name = "Noname";

    /** The event type. */
    public String eventType = null;

    /**
     * <p>
     * getTitle.
     * </p>
     * 
     * @return a {@link java.lang.String}.
     */
    public final String getTitle() {
        return title;
    }

    /**
     * <p>
     * getDifficulty.
     * </p>
     * 
     * @return a {@link java.lang.String}.
     */
    public final String getDifficulty() {
        return difficulty;
    }

    /**
     * <p>
     * getDescription.
     * </p>
     * 
     * @return a {@link java.lang.String}.
     */
    public final String getDescription() {
        return description;
    }

    /**
     * <p>
     * getEventDeck.
     * </p>
     * 
     * @return {@link forge.deck.Deck}
     */
    public final Deck getEventDeck() {
        return eventDeck;
    }

    /**
     * <p>
     * getEventDeck.
     * </p>
     * 
     * @return {@link forge.deck.Deck}
     */
    public final String getEventType() {
        return eventType;
    }

    /**
     * <p>
     * getIcon.
     * </p>
     * 
     * @return a {@link java.lang.String}.
     */
    public final String getIcon() {
        return icon;
    }

    /**
     * <p>
     * getName.
     * </p>
     * 
     * @return a {@link java.lang.String}.
     */
    public final String getName() {
        return name;
    }
}
