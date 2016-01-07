package forge.planarconquest;

import forge.item.PaperCard;
import forge.model.FModel;
import forge.planarconquest.ConquestPlane.Region;
import forge.util.XmlReader;
import forge.util.XmlWriter;
import forge.util.XmlWriter.IXmlWritable;

public class ConquestPlaneData implements IXmlWritable {
    private final ConquestRecord[] eventResults;
    private ConquestLocation location;

    public ConquestPlaneData(ConquestPlane plane0) {
        location = new ConquestLocation(plane0, 0, 0, 0);
        eventResults = new ConquestRecord[plane0.getEventCount()];
    }

    public ConquestPlaneData(XmlReader xml) {
        location = xml.read("location", ConquestLocation.class);
        eventResults = new ConquestRecord[location.getPlane().getEventCount()];
        xml.read("eventResults", eventResults, ConquestRecord.class);
    }
    @Override
    public void saveToXml(XmlWriter xml) {
        xml.write("location", location);
        xml.write("eventResults", eventResults);
    }

    public boolean hasConquered(ConquestLocation loc) {
        return hasConquered(loc.getRegionIndex(), loc.getRow(), loc.getCol());
    }
    public boolean hasConquered(int regionIndex, int row, int col) {
        return hasConquered(regionIndex * Region.ROWS_PER_REGION * Region.COLS_PER_REGION + row * Region.COLS_PER_REGION + col);
    }
    private boolean hasConquered(int index) {
        ConquestRecord result = eventResults[index];
        return result != null && result.getWins() > 0;
    }

    public ConquestRecord getEventRecord(ConquestLocation loc) {
        return getEventRecord(loc.getRegionIndex(), loc.getRow(), loc.getCol());
    }
    public ConquestRecord getEventRecord(int regionIndex, int row, int col) {
        return eventResults[regionIndex * Region.ROWS_PER_REGION * Region.COLS_PER_REGION + row * Region.COLS_PER_REGION + col];
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

    public ConquestLocation getLocation() {
        return location;
    }
    public void setLocation(ConquestLocation location0) {
        location = location0;
    }

    public void addWin(ConquestEvent event) {
        getOrCreateResult(event).addWin(event.getTier());
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
        for (PaperCard pc : location.getPlane().getCardPool().getAllCards()) {
            if (model.hasUnlockedCard(pc)) {
                count++;
            }
        }
        return count;
    }
}
