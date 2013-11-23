package forge.card;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import com.google.common.base.Predicate;

import forge.item.PaperCard;

public interface ICardDatabase extends Iterable<PaperCard> {
    PaperCard tryGetCard(String cardName);
    PaperCard tryGetCard(String cardName, boolean fromLastSet);
    PaperCard tryGetCard(String cardName, String edition);
    PaperCard tryGetCard(String cardName, String edition, int artIndex);
    PaperCard tryGetCardPrintedByDate(String name0, boolean fromLatestSet, Date printedBefore);
    
    PaperCard getCard(String cardName);
    PaperCard getCard(String cardName, boolean fromLastSet);
    PaperCard getCard(String cardName, String edition);
    PaperCard getCard(String cardName, String edition, int artIndex);
    PaperCard getCardPrintedByDate(String name0, boolean fromLatestSet, Date printedBefore);
    
    PaperCard getFoiled(PaperCard cpi);

    int getPrintCount(String cardName, String edition);
    int getMaxPrintCount(String cardName);

    Collection<PaperCard> getUniqueCards();
    List<PaperCard> getAllCards();
    List<PaperCard> getAllCards(Predicate<PaperCard> predicate);

    Predicate<? super PaperCard> wasPrintedInSets(List<String> allowedSetCodes);
}