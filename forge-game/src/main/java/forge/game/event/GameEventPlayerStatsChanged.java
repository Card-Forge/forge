package forge.game.event;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import forge.game.card.CardView;
import forge.game.player.Player;
import forge.game.player.PlayerView;
import forge.util.Lang;
import forge.util.TextUtil;

/**
 * This means card's characteristics have changed on server, clients must re-request them
 */
public record GameEventPlayerStatsChanged(Collection<PlayerView> players, boolean updateCards, Collection<CardView> allCards) implements GameEvent {

    public GameEventPlayerStatsChanged(Collection<Player> players, boolean updateCards) {
        this(PlayerView.getCollection(players), updateCards,
             updateCards ? players.stream().flatMap(p -> StreamSupport.stream(p.getAllCards().spliterator(), false))
                     .map(CardView::get).collect(Collectors.toList()) : Collections.emptyList());
    }

    public GameEventPlayerStatsChanged(Player affected, boolean updateCards) {
        this(Arrays.asList(affected), updateCards);
    }

    /* (non-Javadoc)
     * @see forge.game.event.GameEvent#visit(forge.game.event.IGameEventVisitor)
     */
    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        if (null == players || players.isEmpty()) {
            return "Player state changes: (empty list)";
        }
        return TextUtil.concatWithSpace("Player state changes:", Lang.joinHomogenous(players));
    }

}
