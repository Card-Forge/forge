package forge.planarconquest;

public class ConquestEventResult {
    private int wins, losses;

    public int getWins() {
        return wins;
    }

    public int getLosses() {
        return losses;
    }

    public void addWin() {
        wins++;
    }

    public void addLoss() {
        losses++;
    }
}
