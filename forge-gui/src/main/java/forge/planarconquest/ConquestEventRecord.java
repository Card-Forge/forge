package forge.planarconquest;

public class ConquestEventRecord {
    private int wins, losses;
    private int winStreakBest = 0;
    private int winStreakCurrent = 0;

    public void addWin() {
        wins++;
        winStreakCurrent++;
        if (winStreakCurrent > winStreakBest) {
            winStreakBest = winStreakCurrent;
        }
    }

    public void addLoss(ConquestCommander opponent) {
        losses++;
        winStreakCurrent = 0;
    }

    public int getWins() {
        return wins;
    }

    public int getLosses() {
        return losses;
    }

    public int getWinStreakBest() {
        return winStreakBest;
    }

    public int getWinStreakCurrent() {
        return winStreakCurrent;
    }
}
