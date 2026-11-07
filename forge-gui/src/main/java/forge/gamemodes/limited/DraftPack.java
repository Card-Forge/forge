package forge.gamemodes.limited;

import com.google.common.collect.ForwardingList;
import forge.item.PaperCard;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class DraftPack extends ForwardingList<PaperCard> {
    private final List<PaperCard> cards;
    private final int id;
    private LimitedPlayer passedFrom;
    private Map.Entry<LimitedPlayer, PaperCard> awaitingGuess;
    private LimitedPlayer spyWatcher;
    private LimitedPlayer destination;

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

    public LimitedPlayer getSpyWatcher() {
        return spyWatcher;
    }

    public void setSpyWatcher(LimitedPlayer spyWatcher) {
        this.spyWatcher = spyWatcher;
    }

    /** A Canal Dredger recipient chosen before the pack is passed; null passes normally. */
    public LimitedPlayer getDestination() {
        return destination;
    }

    public void setDestination(LimitedPlayer destination) {
        this.destination = destination;
    }

    @Override
    protected List<PaperCard> delegate() {
        return cards;
    }

    @Override
    public String toString() {
        return String.format("Pack #%d: (%d)%s", id, cards.size(), cards);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof DraftPack that)) return false;
        if (!super.equals(o)) return false;
        return id == that.id && Objects.equals(cards, that.cards) && Objects.equals(awaitingGuess, that.awaitingGuess);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), cards, id, awaitingGuess);
    }
}
