package forge.card;

import com.google.common.base.Predicate;
import forge.card.CardDb.SetPreference;
import forge.item.PaperCard;

import java.util.Collection;
import java.util.Date;
import java.util.List;

public interface ICardDatabase extends Iterable<PaperCard> {
    PaperCard getCard(String cardName);
    PaperCard getCard(String cardName, String edition);
    PaperCard getCard(String cardName, String edition, int artIndex);
    PaperCard getCardFromEdition(String cardName, SetPreference fromSet);
    PaperCard getCardFromEdition(String cardName, Date printedBefore, SetPreference fromSet);
    PaperCard getCardFromEdition(String cardName, Date printedBefore, SetPreference fromSet, int artIndex);
    
    PaperCard getFoiled(PaperCard cpi);

    int getPrintCount(String cardName, String edition);
    int getMaxPrintCount(String cardName);

    int getArtCount(String cardName, String edition);

    Collection<PaperCard> getUniqueCards();
    List<PaperCard> getAllCards();
    List<PaperCard> getAllCards(String cardName);
    List<PaperCard> getAllCards(Predicate<PaperCard> predicate);

    Predicate<? super PaperCard> wasPrintedInSets(List<String> allowedSetCodes);
    
}