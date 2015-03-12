package forge.planarconquest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import forge.item.PaperCard;
import forge.model.FModel;

public class ConquestPlaneData {
    private final List<ConquestCommander> commanders = new ArrayList<ConquestCommander>();
    private final Map<String, Integer> winsPerOpponent = new HashMap<String, Integer>();
    private final Map<String, Integer> lossesPerOpponent = new HashMap<String, Integer>();
    private final ConquestPlane plane;

    private int wins, losses;
    private int winStreakBest = 0;
    private int winStreakCurrent = 0;

    public ConquestPlaneData(ConquestPlane plane0) {
        plane = plane0;
    }

    public List<ConquestCommander> getCommanders() {
        return commanders;
    }

    public boolean hasCommander(PaperCard pc) {
        for (ConquestCommander c : commanders) {
            if (c.getCard() == pc) {
                return true;
            }
        }
        return false;
    }

    public int getWinsAgainst(PaperCard pc) {
        Integer wins = winsPerOpponent.get(pc.getName());
        return wins == null ? 0 : wins.intValue();
    }

    public int getLossesAgainst(PaperCard pc) {
        Integer losses = lossesPerOpponent.get(pc.getName());
        return losses == null ? 0 : losses.intValue();
    }

    public void addWin(ConquestCommander opponent) {
        wins++;
        winStreakCurrent++;
        if (winStreakCurrent > winStreakBest) {
            winStreakBest = winStreakCurrent;
        }
        winsPerOpponent.put(opponent.getName(), getWinsAgainst(opponent.getCard()) + 1);
    }

    public void addLoss(ConquestCommander opponent) {
        losses++;
        winStreakCurrent = 0;
        lossesPerOpponent.put(opponent.getName(), getLossesAgainst(opponent.getCard()) + 1);
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

    public int getUnlockedCount() {
        int count = 0;
        HashSet<PaperCard> collection = FModel.getConquest().getModel().getCollection();
        for (PaperCard pc : plane.getCardPool().getAllCards()) {
            if (collection.contains(pc)) {
                count++;
            }
        }
        return count;
    }
}
