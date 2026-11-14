package forge.gamemodes.rogue.path;

/**
 * Represents a Sanctum node in a Rogue Commander path.
 * Sanctums allow the player to heal life, craft a carry item, or gain removal credits.
 */
public class NodeSanctum extends RoguePathNode {

    private final int healAmount;

    public NodeSanctum() {
        super();
        this.healAmount = 5;
    }

    // Getters and Setters
    public int getHealAmount() {
        return healAmount;
    }

    @Override
    public boolean isSideNode() {
        return true;
    }

    @Override
    public String toString() {
        return "Sanctum (Heal " + healAmount + ", Cook random Food, or gain 3 Removal Credits)";
    }
}
