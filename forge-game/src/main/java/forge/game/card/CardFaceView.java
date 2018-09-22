package forge.game.card;

import java.io.Serializable;

public class CardFaceView implements Serializable, Comparable<CardFaceView> {
    private String name;

    public CardFaceView(String faceName) {
        this.name = faceName;
    }

    public String getName() { return name;}

    public void setName(String name) {
        this.name = name;
    }

    public String toString() {
        return name;
    }

    @Override
    public int compareTo(CardFaceView o) {
        return this.getName().compareTo(o.getName());
    }
}