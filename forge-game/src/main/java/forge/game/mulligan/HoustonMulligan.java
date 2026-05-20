package forge.game.mulligan;

import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

public class HoustonMulligan extends AbstractMulligan {

    private static final int TUCK_COUNT = 3;

    @Override
    public int handSizeAfterNextMulligan() {
        return player.getMaxHandSize();
    }

    public HoustonMulligan(Player p, boolean firstMullFree) {
        super(p, firstMullFree);
    }

    public int tuckCardsDuringMulligan() {
        return TUCK_COUNT;
    }

    public void beforeFirstMulligan() {
        player.drawCards(TUCK_COUNT);
    }

    @Override
    public void keep() {
        super.keep();
        CardCollection hand = new CardCollection(player.getCardsIn(ZoneType.Hand));
        for (final Card c : player.getController().tuckCardsViaMulligan(hand, tuckCardsDuringMulligan())) {
            player.getGame().getAction().moveToLibrary(c, -1, null);
        }
    }

    @Override
    public boolean canMulligan() {
        return false;
    }
}