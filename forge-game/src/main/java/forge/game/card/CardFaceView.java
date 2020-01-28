package forge.game.card;

import java.io.Serializable;

public class CardFaceView implements Serializable, Comparable<CardFaceView> {
    /**
     *
     */
    private static final long serialVersionUID = 1874016432028306386L;
    private String displayName;
    private String oracleName;

    public CardFaceView(String displayName) {
        this(displayName, displayName);
    }

    public CardFaceView(String displayFaceName, String oracleFaceName ) {
        this.displayName = displayFaceName;
        this.oracleName = oracleFaceName;
    }

    public String getName() { return displayName;}

    public void setName(String name) {
        this.displayName = name;
    }

    public String getOracleName() { return oracleName; }

    public void setOracleName(String name) {
        this.oracleName = name;
    }

    public String toString() {
        return displayName;
    }

    @Override
    public int compareTo(CardFaceView o) {
        return this.getOracleName().compareTo(o.getOracleName());
    }
}