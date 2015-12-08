package forge.planarconquest;

public class ConquestRecord {
    private int wins, losses, level;

    public int getWins() {
        return wins;
    }

    public int getLosses() {
        return losses;
    }

    public int getLevel() {
        return level;
    }

    public void addWin() {
        wins++;
    }

    public void addLoss() {
        losses++;
    }

    public void levelUp() {
        level++;
    }
}
