package forge.planarconquest;

import java.util.HashSet;
import forge.item.PaperCard;
import forge.model.FModel;
import forge.planarconquest.ConquestPlane.Region;

public class ConquestPlaneData {
    private final ConquestPlane plane;
    private final int[][][] eventResults;
    private int bossResult;

    private int wins, losses;
    private int winStreakBest = 0;
    private int winStreakCurrent = 0;

    public ConquestPlaneData(ConquestPlane plane0) {
        plane = plane0;
        eventResults = new int[plane.getRegions().size()][Region.ROWS_PER_REGION][Region.COLS_PER_REGION];
    }

    public int getEventResult(ConquestLocation loc) {
        return getEventResult(loc.getRegionIndex(), loc.getRow(), loc.getCol());
    }
    public int getEventResult(int regionIndex, int row, int col) {
        if (regionIndex == -1) {
            return 1; //bottom portal is always conquered
        }
        if (regionIndex == plane.getRegions().size()) {
            return bossResult; 
        }
        return eventResults[regionIndex][row][col];
    }

    public int getBossResult() {
        return bossResult;
    }

    public void addWin(ConquestCommander opponent) {
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
