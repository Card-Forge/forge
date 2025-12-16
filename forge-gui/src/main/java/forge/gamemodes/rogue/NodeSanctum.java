package forge.gamemodes.rogue;

/**
 * Represents a Sanctum node in a Rogue Commander path.
 * Sanctums allow the player to heal life and remove cards from their deck.
 */
public class NodeSanctum extends RoguePathNode {

    private int healAmount;
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
    public String toString() {
        return "Sanctum (Heal " + healAmount + ", Remove up to " + freeRemoves + ")";
    }
}
