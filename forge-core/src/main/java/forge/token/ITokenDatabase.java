package forge.token;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import com.google.common.base.Predicate;

import forge.card.CardDb;
import forge.item.PaperToken;

public interface ITokenDatabase extends Iterable<PaperToken> {
    PaperToken getToken(String tokenName);
    PaperToken getToken(String tokenName, String edition);
    PaperToken getToken(String tokenName, String edition, int artIndex);
    PaperToken getTokenFromEdition(String tokenName, CardDb.SetPreference fromSet);
    PaperToken getTokenFromEdition(String tokenName, Date printedBefore, CardDb.SetPreference fromSet);
    PaperToken getTokenFromEdition(String tokenName, Date printedBefore, CardDb.SetPreference fromSet, int artIndex);

    PaperToken getFoiled(PaperToken cpi);

    int getPrintCount(String tokenName, String edition);
    int getMaxPrintCount(String tokenName);

    int getArtCount(String tokenName, String edition);

    Collection<PaperToken> getUniqueTokens();
    List<PaperToken> getAllTokens();
    List<PaperToken> getAllTokens(String tokenName);
    List<PaperToken> getAllTokens(Predicate<PaperToken> predicate);

    Predicate<? super PaperToken> wasPrintedInSets(List<String> allowedSetCodes);
}
