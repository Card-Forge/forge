package forge.planarconquest;

import forge.util.XmlReader;
import forge.util.XmlWriter;
import forge.util.XmlWriter.IXmlWritable;

public class ConquestRecord implements IXmlWritable {
    private int wins, losses, tier;

    public ConquestRecord() {
    }

    public ConquestRecord(XmlReader xml) {
        wins = xml.read("wins", 0);
        losses = xml.read("losses", 0);
        tier = xml.read("tier", 0);
    }
    @Override
    public void saveToXml(XmlWriter xml) {
        xml.write("wins", wins);
        xml.write("losses", losses);
        xml.write("tier", tier);
    }

    public int getWins() {
        return wins;
    }

    public int getLosses() {
        return losses;
    }

    public int getTier() {
        return tier;
    }

    public void addWin(int tier0) {
        wins++;
        if (tier0 > tier) {
            tier = tier0;
        }
    }

    public void addLoss() {
        losses++;
    }
}
