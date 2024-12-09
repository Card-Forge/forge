package forge.item;

import forge.card.CardRarity;
import forge.card.CardRules;
import forge.card.ICardFace;

import java.io.Serializable;
import java.util.Set;

public interface IPaperCard extends InventoryItem, Serializable {

    String NO_COLLECTOR_NUMBER = "N.A.";  // Placeholder for No-Collection number available
    int DEFAULT_ART_INDEX = 1;
    int NO_ART_INDEX = -1;  // Placeholder when NO ArtIndex is Specified
    String NO_ARTIST_NAME = "";
    String NO_FUNCTIONAL_VARIANT = "";


    String getName();
    String getEdition();
    String getCollectorNumber();
    String getFunctionalVariant();
    Set<String> getColorID();
    int getArtIndex();
    boolean isFoil();
    boolean isToken();
    CardRules getRules();
    CardRarity getRarity();
    String getArtist();
    String getItemType();
    boolean hasBackFace();
    ICardFace getMainFace();
    ICardFace getOtherFace();
    String getCardImageKey();
    String getCardAltImageKey();
    String getCardWSpecImageKey();
    String getCardUSpecImageKey();
    String getCardBSpecImageKey();
    String getCardRSpecImageKey();
    String getCardGSpecImageKey();

    boolean isRebalanced();

    @Override
    default String getTranslationKey() {
        if(!NO_FUNCTIONAL_VARIANT.equals(getFunctionalVariant()))
            return getName() + " $" + getFunctionalVariant();
        return getName();
    }

    @Override
    default String getUntranslatedType() {
        return getRules().getType().toString();
    }

    @Override
    default String getUntranslatedOracle() {
        return getRules().getOracleText();
    }
}