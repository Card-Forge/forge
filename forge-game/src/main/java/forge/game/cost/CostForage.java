package forge.game.cost;

import forge.game.Game;
import forge.game.ability.AbilityKey;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.*;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;

import java.util.Map;

public class CostForage extends CostPartWithList {

    private static final long serialVersionUID = 1L;

    @Override
    public boolean canPay(SpellAbility ability, Player payer, boolean effect) {
        CardCollection graveyard = CardLists.filter(payer.getCardsIn(ZoneType.Graveyard), CardPredicates.canExiledBy(ability, effect));
        if (graveyard.size() >= 3) {
            return true;
        }

        CardCollection food = CardLists.filter(payer.getCardsIn(ZoneType.Battlefield), CardPredicates.isType("Food"), CardPredicates.canBeSacrificedBy(ability, effect));
        if (!food.isEmpty()) {
            return true;
        }

        return false;
    }

    @Override
    public String toString() {
        return "Forage";
    }

    @Override
    protected Card doPayment(Player payer, SpellAbility ability, Card targetCard, final boolean effect) { return null; }
    @Override
    protected boolean canPayListAtOnce() { return true; }
    @Override
    protected CardCollectionView doListPayment(Player payer, SpellAbility ability, CardCollectionView targetCards, final boolean effect) {
        final Game game = payer.getGame();
        if (targetCards.size() == 3) {
            Map<AbilityKey, Object> moveParams = AbilityKey.newMap();
            AbilityKey.addCardZoneTableParams(moveParams, table);
            CardCollection result = new CardCollection();
            for (Card targetCard : targetCards) {
                Card newCard = game.getAction().exile(targetCard, null, moveParams);
                result.add(newCard);
                SpellAbilityEffect.handleExiledWith(newCard, ability);
            }
            triggerForage(payer);
            return result;
        } else if (targetCards.size() == 1) {
            Map<AbilityKey, Object> moveParams = AbilityKey.newMap();
            AbilityKey.addCardZoneTableParams(moveParams, table);
            CardCollection result = game.getAction().sacrifice(targetCards, ability, effect, moveParams);
            triggerForage(payer);
            return result;
        } else {
            return null;
        }
    }

    protected void triggerForage(Player payer) {
        final Map<AbilityKey, Object> runParams = AbilityKey.mapFromPlayer(payer);
        payer.getGame().getTriggerHandler().runTrigger(TriggerType.Forage, runParams, false);
    }

    public static final String HashLKIListKey = "Foraged";
    public static final String HashCardListKey = "ForagedCards";

    @Override
    public String getHashForLKIList() {
        return HashLKIListKey;
    }
    @Override
    public String getHashForCardList() {
        return HashCardListKey;
    }

    public <T> T accept(ICostVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
