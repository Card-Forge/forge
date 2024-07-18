package forge.gamemodes.limited;

import com.google.common.collect.ForwardingList;
import forge.item.PaperCard;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;

public class DraftPack extends ForwardingList<PaperCard> {
    private final List<PaperCard> cards;
    private final int id;
    private LimitedPlayer passedFrom;
    private Map.Entry<LimitedPlayer, PaperCard> awaitingGuess;

    public DraftPack(List<PaperCard> cards, int id) {
        this.cards = cards;
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public LimitedPlayer getPassedFrom() {
        return passedFrom;
    }

    public void setPassedFrom(LimitedPlayer passedFrom) {
        this.passedFrom = passedFrom;
    }

    public void setAwaitingGuess(LimitedPlayer player, PaperCard card) {
        this.awaitingGuess = new AbstractMap.SimpleEntry<>(player, card);
    }

    public Map.Entry<LimitedPlayer, PaperCard> getAwaitingGuess() {
        return awaitingGuess;
    }

    public void resetAwaitingGuess() {
        this.awaitingGuess = null;
    }

    @Override
    protected List<PaperCard> delegate() {
        return cards;
    }
}
