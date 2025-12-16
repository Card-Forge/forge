package forge.gamemodes.rogue;

/**
 * Represents a Loot node in a Rogue Commander path.
 * Loot nodes provide rewards without combat.
 * TODO: Add properties for loot contents.
 */
public class NodeChest extends RoguePathNode {

    public NodeChest() {
        super();
    }

    @Override
    public String toString() {
        return "Loot (Treasure)";
    }
}
