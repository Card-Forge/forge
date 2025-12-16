package forge.gamemodes.rogue;

/**
 * Represents a Bazaar node in a Rogue Commander path.
 * Bazaars allow the player to buy cards and loot.
 * TODO: Add properties for shop inventory and pricing.
 */
public class NodeBazaar extends RoguePathNode {

    public NodeBazaar() {
        super();
    }

    @Override
    public String toString() {
        return "Bazaar (Shop)";
    }
}
