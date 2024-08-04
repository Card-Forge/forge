package forge.game.cost;

import forge.game.player.Player;
import forge.game.player.PlayerCollection;
import forge.game.spellability.SpellAbility;

public class CostPromiseGift extends CostPart {
    // Promise Gift is a very specific cost. A more generic version might be "Choose Player/Opponent"

    @Override
    public int paymentOrder() {
        // Its just choosing a person
        return -1;
    }

    @Override
    public boolean canPay(SpellAbility ability, Player payer, boolean effect) {
        // You can always promise a gift
        return true;
    }

    @Override
    public <T> T accept(ICostVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        // Extract the description from the SA

        return "Gift something";
    }

    @Override
    public boolean payAsDecided(Player payer, PaymentDecision pd, SpellAbility sa, boolean effect) {
        if (pd.players.isEmpty()) {
            sa.getHostCard().setPromisedGift(null);
            return false;
        }

        sa.getHostCard().setPromisedGift(pd.players.get(0));
        return true;
    }

    public PlayerCollection getPotentialPlayers(final Player payer, final SpellAbility ability) {
        return payer.getOpponents();
    }
}
