package forge.game.cost;

import forge.game.ability.effects.PutStickerEffect;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

public class CostPutSticker extends CostPart {

    @Override
    public boolean canPay(SpellAbility ability, Player payer, boolean effect) {
        return true;
    }

    @Override
    public int paymentOrder() { return 9; }

    @Override
    public <T> T accept(ICostVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        return "Put Sticker";
    }

    @Override
    public boolean payAsDecided(Player payer, PaymentDecision pd, SpellAbility sa, boolean effect) {
        Card host = sa.getHostCard();
        PutStickerEffect.chooseAndPlaceSticker(sa, host, true);
        return true;
    }

}
