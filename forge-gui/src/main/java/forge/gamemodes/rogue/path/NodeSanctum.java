package forge.gamemodes.rogue.path;

/**
 * Represents a Sanctum node in a Rogue Commander path.
 * Sanctums allow the player to heal life, craft a carry item, or gain removal credits.
 */
public class NodeSanctum extends RoguePathNode {

    private int healAmount;
    // Legacy serialized field retained for backward compatibility with older saved runs.
    private int freeRemoves;

    public NodeSanctum() {
        super();
        this.healAmount = 5;
        this.freeRemoves = 3;
    }

    public NodeSanctum(int healAmount, int freeRemoves) {
        super();
        this.healAmount = healAmount;
        this.freeRemoves = freeRemoves;
    }

    // Getters and Setters
    public int getHealAmount() {
        return healAmount;
    }

    public void setHealAmount(int healAmount) {
        this.healAmount = healAmount;
    }

    public int getFreeRemoves() {
        return freeRemoves;
    }

    public void setFreeRemoves(int freeRemoves) {
        this.freeRemoves = freeRemoves;
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
