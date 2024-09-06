package forge.item;

import com.google.common.collect.Lists;
import forge.card.CardRarity;
import forge.card.CardRules;
import forge.card.CardType.CoreType;
import forge.card.ICardFace;
import forge.card.MagicColor;
import forge.util.PredicateCard;
import forge.util.PredicateString;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

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

    public boolean isRebalanced();
}