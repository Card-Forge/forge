package forge.card;

import java.security.InvalidParameterException;
import java.util.ArrayList;

import forge.AllZone;
import forge.Card;
import forge.SetInfo;

/**
 * <p>CardReference class.</p>
 *
 * @author Forge
 * @version $Id: CardReference.java 9708 2011-08-09 19:34:12Z jendave $
 */
public final class CardReference {
    private static final boolean ENABLE_CONSISTENCY_CHECK = true;

    private String name;
    private String cardSet;
    private short pictureNumber = 0;
    private boolean foiled = false;

    public CardReference(final String named, final String set, final int picNum, final boolean foil) {
        this(named, set, picNum);
        foiled = foil;
    }

    public CardReference(final String named, final String set, final int picNum) {
        this(named, set);
        pictureNumber = (short) picNum;
    }

    public CardReference(final String named, final String set) {
        name = named;
        cardSet = set;

        if (set == null || ENABLE_CONSISTENCY_CHECK) {
            Card c = AllZone.getCardFactory().getCard(name, null);
            if (c == null) {
                String error = String.format("Invalid reference! The card named '%s' is unknown to Forge", name);
                throw new InvalidParameterException(error);
            }

            ArrayList<SetInfo> validSets = c.getSets();
            boolean isSetValid = false;
            if (cardSet != null) {
                for (SetInfo si : validSets) {
                    if (si.Code.equals(set)) { isSetValid = true; break; }
                }
            }

            if (!isSetValid) {
                cardSet = c.getMostRecentSet();
                // String error = String.format("The card '%s' is not a part of '%s' set", name, set);
                // throw new InvalidParameterException(error);
            }
        }
    }

    public CardReference(final String named) { this(named, (String) null); }

    public String getName() { return name; }
    public String getSet() { return cardSet; }
    public short getPictureIndex() { return pictureNumber; }
    public boolean isFoil() { return foiled; }

    // Want this class to be a key for HashTable
    @Override
    public boolean equals(Object obj) {
        if (this == obj) { return true; }
        if (obj == null) { return false; }
        if (getClass() != obj.getClass()) { return false; }

        CardReference other = (CardReference) obj;
        if (!name.equals(other.name)) { return false; }
        if (!cardSet.equals(other.cardSet)) { return false; }
        if (other.foiled != this.foiled || other.pictureNumber != this.pictureNumber) { return false; }

        return true;
    }

    @Override
    public int hashCode() {
        int code = name.hashCode() * 11 + cardSet.hashCode() * 59 + pictureNumber * 2;
        if (foiled) { return code + 1; }
        return code;
    }
}
