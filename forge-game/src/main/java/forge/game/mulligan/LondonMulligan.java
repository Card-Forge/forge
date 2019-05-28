package forge.game.mulligan;

import forge.game.player.Player;

public class LondonMulligan extends AbstractMulligan {
    public LondonMulligan(Player p, boolean firstMullFree) {
        super(p, firstMullFree);
    }

    @Override
    public boolean canMulligan() {
        return !kept && timesMulliganed < player.getMaxHandSize();
    }

    @Override
    public int handSizeAfterNextMulligan() {
        return player.getMaxHandSize();
    }

    @Override
    public int tuckCardsAfterKeepHand() {
        if (timesMulliganed == 0) {
            return 0;
        }

        int extraCard = firstMulliganFree ? 1 : 0;
        return timesMulliganed - extraCard;
    }

    @Override
    public void afterMulligan() {
        int tuckingCards = tuckCardsAfterKeepHand();
        player.getController().londonMulliganReturnCards(player, tuckingCards);
        super.afterMulligan();
    }
}
