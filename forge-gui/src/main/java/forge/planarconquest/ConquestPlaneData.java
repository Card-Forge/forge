package forge.planarconquest;

import forge.item.PaperCard;
import forge.model.FModel;
import forge.planarconquest.ConquestPlane.Region;

public class ConquestPlaneData {
    private final ConquestPlane plane;
    private final ConquestRecord[] eventResults;

    public ConquestPlaneData(ConquestPlane plane0) {
        plane = plane0;
        eventResults = new ConquestRecord[plane.getEventCount()];
    }

    public boolean hasConqueredBoss() {
        return hasConquered(eventResults.length - 1);
    }
    public boolean hasConquered(ConquestLocation loc) {
        return hasConquered(loc.getRegionIndex(), loc.getRow(), loc.getCol());
    }
    public boolean hasConquered(int regionIndex, int row, int col) {
        if (regionIndex == -1) {
            return true; //bottom portal is always conquered
        }
        if (regionIndex == plane.getRegions().size()) {
            return hasConqueredBoss();
        }
        return hasConquered(regionIndex * Region.ROWS_PER_REGION * Region.COLS_PER_REGION + row * Region.COLS_PER_REGION + col);
    }
    private boolean hasConquered(int index) {
        ConquestRecord result = eventResults[index];
        return result != null && result.getWins() > 0;
    }

    private ConquestRecord getOrCreateResult(ConquestEvent event) {
        ConquestLocation loc = event.getLocation();
        int index = loc.getRegionIndex() * Region.ROWS_PER_REGION * Region.COLS_PER_REGION + loc.getRow() * Region.COLS_PER_REGION + loc.getCol();
        ConquestRecord result = eventResults[index];
        if (result == null) {
            result = new ConquestRecord();
            eventResults[index] = result;
        }
        return result;
    }

    public void addWin(ConquestEvent event) {
        getOrCreateResult(event).addWin();
    }

    public void addLoss(ConquestEvent event) {
        getOrCreateResult(event).addLoss();
    }

    public int getConqueredCount() {
        int conquered = 0;
        for (int i = 0; i < eventResults.length; i++) {
            if (hasConquered(i)) {
                conquered++;
            }
        }
        return conquered;
    }

    public int getUnlockedCount() {
        int count = 0;
        ConquestData model = FModel.getConquest().getModel();
        for (PaperCard pc : plane.getCardPool().getAllCards()) {
            if (model.hasUnlockedCard(pc)) {
                count++;
            }
        }
        return count;
    }
}
