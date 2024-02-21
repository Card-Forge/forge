package forge.game.cost;

import forge.game.Game;
import forge.game.ability.AbilityKey;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

import java.util.Map;

public class CostCollectEvidence extends CostPartWithList {
    // CollectEvidence<Amount>

    public CostCollectEvidence(final String amount) {
        this.setAmount(amount);
    }

    public static final String HashLKIListKey = "Collected";
    public static final String HashCardListKey = "CollectedCards";

    @Override
    public String getHashForLKIList() {
        return HashLKIListKey;
    }
    @Override
    public String getHashForCardList() {
        return HashCardListKey;
    }

    @Override
    public int paymentOrder() {
        // needs to be aligned with CostExile because of Lamplight Phoenix
        return 15;
    }

    @Override
    public boolean canPay(SpellAbility ability, Player payer, boolean effect) {
        int amount = this.getAbilityAmount(ability);

        // This may need to be updated if we get a card like "Cards in graveyards can't be exiled to pay for costs"

        return CardLists.getTotalCMC(payer.getCardsIn(ZoneType.Graveyard)) >= amount;
    }

    @Override
    public <T> T accept(ICostVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Collect evidence ");
        sb.append(this.getAmount());
        return sb.toString();
    }

    @Override
    protected Card doPayment(Player payer, SpellAbility ability, Card targetCard, boolean effect) {
        final Game game = targetCard.getGame();
        Map<AbilityKey, Object> moveParams = AbilityKey.newMap();
        moveParams.put(AbilityKey.LastStateBattlefield, game.getLastStateBattlefield());
        moveParams.put(AbilityKey.LastStateGraveyard, game.getLastStateGraveyard());
        moveParams.put(AbilityKey.InternalTriggerTable, table);
        Card newCard = game.getAction().exile(targetCard, null, moveParams);
        SpellAbilityEffect.handleExiledWith(newCard, ability);
        return newCard;
    }
}
