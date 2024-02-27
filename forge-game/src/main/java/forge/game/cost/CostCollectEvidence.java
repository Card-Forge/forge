package forge.game.cost;

import forge.game.Game;
import forge.game.ability.AbilityKey;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;

import java.util.Map;

public class CostCollectEvidence extends CostPartWithList {
    // CollectEvidence<Amount>

    private static final long serialVersionUID = 1L;

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

        return CardLists.getTotalCMC(CardLists.filter(payer.getCardsIn(ZoneType.Graveyard), CardPredicates.canExiledBy(ability, effect))) >= amount;
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
    protected boolean canPayListAtOnce() { return true; }

    @Override
    protected Card doPayment(Player payer, SpellAbility ability, Card targetCard, boolean effect) {
        return null;
    }

    @Override
    protected CardCollectionView doListPayment(Player payer, SpellAbility ability, CardCollectionView targetCards, final boolean effect) {
        final Game game = payer.getGame();
        Map<AbilityKey, Object> moveParams = AbilityKey.newMap();
        AbilityKey.addCardZoneTableParams(moveParams, table);
        CardCollection moved = game.getAction().exile(new CardCollection(targetCards), ability, moveParams);
        SpellAbilityEffect.handleExiledWith(moved, ability);
        game.getTriggerHandler().runTrigger(TriggerType.CollectEvidence, AbilityKey.mapFromPlayer(payer), false);
        return moved;
    }
}
