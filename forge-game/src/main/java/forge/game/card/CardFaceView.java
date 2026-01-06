package forge.game.card;

import java.io.Serializable;

import forge.card.ICardFace;
import forge.util.CardTranslation;
import forge.util.ITranslatable;

public record CardFaceView(String name, String displayName) implements Serializable, ITranslatable, Comparable<CardFaceView> {
    private static final long serialVersionUID = 1874016432028306386L;

    public CardFaceView(ICardFace face) {
        this(face.getName(), face.getDisplayName());
    }

    @Override
    public String getName() {
        return this.name;
    }
    @Override
    public String getTranslatedName() {
        return CardTranslation.getTranslatedName(this.displayName);
    }
    @Override
    public String toString() {
        return name;
    }

    @Override
    public int compareTo(CardFaceView o) {
        return this.getName().compareTo(o.getName());
    }

    @Override
    public int hashCode() {
        return this.name.hashCode();
    }
}