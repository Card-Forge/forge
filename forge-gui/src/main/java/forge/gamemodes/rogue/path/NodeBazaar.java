package forge.gamemodes.rogue.path;

/**
 * Represents a Bazaar node in a Rogue Commander path.
 * Bazaars allow the player to buy cards using gold.
 */
public class NodeBazaar extends RoguePathNode {

    public NodeBazaar() {
        super();
    }

    @Override
    public boolean isSideNode() {
        return true;
    }

    @Override
    public String toString() {
        return "Bazaar (Shop)";
    }
}
