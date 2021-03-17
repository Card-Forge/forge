package forge.gamemodes.planarconquest;

import forge.gamemodes.planarconquest.ConquestEvent.ConquestEventRecord;
import forge.item.PaperCard;
import forge.model.FModel;
import forge.util.XmlReader;
import forge.util.XmlWriter;
import forge.util.XmlWriter.IXmlWritable;

public class ConquestPlaneData implements IXmlWritable {
    private final ConquestEventRecord[] eventResults;
    private ConquestLocation location;

    public ConquestPlaneData(ConquestPlane plane0) {
        location = new ConquestLocation(plane0, 0, 0, 0);
        eventResults = new ConquestEventRecord[plane0.getEventCount()];
    }

    public ConquestPlaneData(XmlReader xml) {
        location = xml.read("location", ConquestLocation.class);
        eventResults = new ConquestEventRecord[location.getPlane().getEventCount()];
        xml.read("eventResults", eventResults, ConquestEventRecord.class);
    }
    @Override
    public void saveToXml(XmlWriter xml) {
        xml.write("location", location);
        xml.write("eventResults", eventResults);
    }

    public boolean hasConquered(ConquestLocation loc) {
        return hasConquered(loc.getEventIndex());
    }
    public boolean hasConquered(int regionIndex, int row, int col) {
        return hasConquered(location.getPlane().getEventIndex(regionIndex, row, col));
    }
    private boolean hasConquered(int eventIndex) {
        ConquestEventRecord result = eventResults[eventIndex];
        return result != null && result.hasConquered();
    }

    public ConquestEventRecord getEventRecord(ConquestLocation loc) {
        return eventResults[loc.getEventIndex()];
    }
    public ConquestEventRecord getEventRecord(int regionIndex, int row, int col) {
        return eventResults[location.getPlane().getEventIndex(regionIndex, row, col)];
    }

    private ConquestEventRecord getOrCreateResult(ConquestBattle event) {
        int eventIndex = event.getLocation().getEventIndex();
        ConquestEventRecord result = eventResults[eventIndex];
        if (result == null) {
            result = new ConquestEventRecord();
            eventResults[eventIndex] = result;
        }
        return result;
    }

    public ConquestLocation getLocation() {
        return location;
    }
    public void setLocation(ConquestLocation location0) {
        location = location0;
    }

    public void addWin(ConquestBattle event) {
        getOrCreateResult(event).addWin(event.getTier());
    }

    public void addLoss(ConquestBattle event) {
        getOrCreateResult(event).addLoss(event.getTier());
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

    public int getTotalWins() {
        int wins = 0;
        for (int i = 0; i < eventResults.length; i++) {
            ConquestEventRecord result = eventResults[i];
            if (result != null) {
                wins += result.getTotalWins();
            }
        }
        return wins;
    }

    public int getTotalLosses() {
        int losses = 0;
        for (int i = 0; i < eventResults.length; i++) {
            ConquestEventRecord result = eventResults[i];
            if (result != null) {
                losses += result.getTotalLosses();
            }
        }
        return losses;
    }

    public int getUnlockedCardCount() {
        int count = 0;
        ConquestData model = FModel.getConquest().getModel();
        for (PaperCard pc : location.getPlane().getCardPool().getAllCards()) {
            if (model.hasUnlockedCard(pc)) {
                count++;
            }
        }
        return count;
    }
}
