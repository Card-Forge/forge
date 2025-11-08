package forge.game.mulligan;

import com.google.common.collect.Lists;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

import java.util.List;

public class HoustonMulligan extends AbstractMulligan {

    private static final int TUCK_COUNT = 3;

    @Override
    public int handSizeAfterNextMulligan() {
        return player.getMaxHandSize();
    }

    public HoustonMulligan(Player p, boolean firstMullFree) {
        super(p, false);
    }

    @Override
    public void keep() {
        if (timesMulliganed == 0) {
            mulligan();
            return;
        }

        kept = true;
        timesMulliganed = 1;
    }

    @Override
    public void afterMulligan() {
        List<Card> cardsToTuckDown = Lists.newArrayList(
                player.getController().londonMulliganReturnCards(player, TUCK_COUNT)
        );

        for (final Card c : cardsToTuckDown) {
            player.getGame().getAction().moveToLibrary(
                    c,
                    -1,
                    (SpellAbility)null
            );
        }
    }

    @Override
    public boolean canMulligan() {
        return false;
    }
}