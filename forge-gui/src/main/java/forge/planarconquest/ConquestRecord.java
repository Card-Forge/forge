package forge.planarconquest;

import forge.util.XmlWriter;
import forge.util.XmlWriter.IXmlWritable;

public class ConquestRecord implements IXmlWritable {
    private int wins, losses, level;

    public int getWins() {
        return wins;
    }

    public int getLosses() {
        return losses;
    }

    public int getLevel() {
        return level;
    }

    public void addWin() {
        wins++;
    }

    public void addLoss() {
        losses++;
    }

    public void levelUp() {
        level++;
    }

    @Override
    public void saveToXml(XmlWriter xml) {
        xml.write("wins", wins);
        xml.write("losses", losses);
        xml.write("level", level);
    }
}
