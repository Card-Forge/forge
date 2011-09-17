package forge.quest.gui.main;

import forge.deck.Deck;

/** 
 * <p>QuestEvent.</p>
 *
 * MODEL - A basic event instance in Quest mode. Can be extended for use in
 * unique event types: battles, quests, and others.
 */
public class QuestEvent {
    // Default vals if none provided in the event file.
    public Deck    eventDeck           = null;
    public String  title               = "Mystery Event";
    public String  description         = "";
    public String  difficulty          = "Medium";
    public String  icon                = "Unknown.jpg";
    public String  name                 = "Noname";
    
    /**
     * <p>getTitle.</p>
     *
     * @return a {@link java.lang.String}.
     */
    public final String getTitle() { 
        return title; 
    }

    /**
     * <p>getDifficulty.</p>
     *
     * @return a {@link java.lang.String}.
     */
    public final String getDifficulty() { 
        return difficulty; 
    }
    
    /**
     * <p>getDescription.</p>
     *
     * @return a {@link java.lang.String}.
     */
    public final String getDescription() { 
        return description; 
    }
    
    /**
     * <p>getEventDeck.</p>
     * 
     * @return {@link forge.deck.Deck}
     */
    public final Deck getEventDeck() {
        return eventDeck; 
    }
    
    /**
     * <p>getIcon.</p>
     *
     * @return a {@link java.lang.String}.
     */    
    public final String getIcon() { 
        return icon; 
    }
    
    /**
     * <p>getName.</p>
     *
     * @return a {@link java.lang.String}.
     */
    public final String getName() { 
        return name; 
    }
}
