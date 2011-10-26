package forge.quest.gui.main;

/**
 * <p>
 * QuestDuel class.
 * </p>
 * MODEL - A single duel event data instance, including meta and deck.
 * 
 */
public class QuestDuel extends QuestEvent {

    /**
     * Instantiates a new quest duel.
     */
    public QuestDuel() {
        super();
        eventType = "duel";
    }

}
