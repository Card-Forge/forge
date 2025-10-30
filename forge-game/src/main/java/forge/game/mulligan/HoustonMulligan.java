package forge.game.mulligan;

import forge.game.card.Card;
import forge.game.player.Player;

public class HoustonMulligan extends AbstractMulligan {

    private static final int STARTING_DRAW_SIZE = 10;
    private static final int TUCK_COUNT = 3;

    public HoustonMulligan(Player p) {
        // Force isMulliganFree to be false, although the logic here will be controlled by canMulligan()
        super(p, false);

        p.setStartingHandSize(STARTING_DRAW_SIZE);
    }

    @Override
    public boolean canMulligan() {
        // Only allow one mulligan attempt (when timesMulliganed is 0) and only if the hand hasn't been kept.
        return !kept && timesMulliganed < 1;
    }

    @Override
    public int handSizeAfterNextMulligan() {
        // The hand size is always 10, regardless of the number of mulligans (which is capped at 1).
        return STARTING_DRAW_SIZE;
    }

    @Override
    public void mulliganDraw() {
        player.drawCards(STARTING_DRAW_SIZE);

        timesMulliganed++;

        // This is the forced tuck of 3 cards.
        for (final Card c : player.getController().londonMulliganReturnCards(player, TUCK_COUNT)) {
            player.getGame().getAction().moveToLibrary(c, -1, null);
        }
    }

    @Override
    public int tuckCardsAfterKeepHand() {
        // Only require the tuck after the *single* Houston Mulligan draw, otherwise 0.
        return timesMulliganed > 0 ? TUCK_COUNT : 0;
    }
}