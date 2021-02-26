package forge.game.mulligan;

import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

public class OriginalMulligan extends AbstractMulligan {
    public OriginalMulligan(Player p, boolean firstMullFree) {
        super(p, firstMullFree);
    }

    public boolean canMulligan() {
        if (timesMulliganed > 0) {
            return false;
        }

        int totalCards = 0;
        int lands = 0;
        for(Card c : player.getCardsIn(ZoneType.Hand)) {
            if (c.isLand()) {
                lands++;
            }
            totalCards++;
        }

        return lands == 0 || lands == totalCards;
    }
    
    @Override
    public void revealPreMulligan(CardCollection toMulligan) {
        //for(Card card : toMulligan) {
            // TODO Reveal the cards. 
        //}
    }

    public void mulliganDraw() {
        player.drawCards(handSizeAfterNextMulligan());
    }

    public int handSizeAfterNextMulligan() {
        return player.getMaxHandSize();
    }
}
