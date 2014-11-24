package forge.planarconquest;

import java.util.ArrayList;
import java.util.List;

public class ConquestPlaneData {
    private final List<ConquestCommander> commanders = new ArrayList<ConquestCommander>();

    private int wins, losses;

    public List<ConquestCommander> getCommanders() {
        return commanders;
    }

    public void addWin() {
        wins++;
    }

    public void addLoss() {
        losses++;
    }

    public int getWins() {
        return wins;
    }

    public int getLosses() {
        return losses;
    }
}
