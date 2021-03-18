package forge.gamemodes.planarconquest;

import forge.util.XmlReader;
import forge.util.XmlWriter;
import forge.util.XmlWriter.IXmlWritable;

public class ConquestRecord implements IXmlWritable {
    private int wins, losses;

    public ConquestRecord() {
    }
    public ConquestRecord(XmlReader xml) {
        wins = xml.read("wins", 0);
        losses = xml.read("losses", 0);
    }

    @Override
    public void saveToXml(XmlWriter xml) {
        xml.write("wins", wins);
        xml.write("losses", losses);
    }

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
