package forge.gamemodes.rogue;

/**
 * Represents an Event node in a Rogue Commander path.
 * Events trigger random occurrences or choices for the player.
 * TODO: Add properties for event type and options.
 */
public class NodeEvent extends RoguePathNode {

    public NodeEvent() {
        super();
    }

    @Override
    public String toString() {
        return "Event (???)";
    }
}
