package forge.game.ability.effects;

import forge.game.Game;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.TriggerType;
import forge.util.Lang;

import java.util.Map;

public class ClaimThePrizeEffect extends SpellAbilityEffect {

    @Override
    public void resolve(SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Player activator = sa.getActivatingPlayer();
        final Game game = activator.getGame();
        final CardCollection attractions = AbilityUtils.getDefinedCards(host, sa.getParamOrDefault("Defined", "Self"), sa);

        for(Card c : attractions) {
            final Map<AbilityKey, Object> runParams = AbilityKey.mapFromPlayer(activator);
            runParams.put(AbilityKey.Card, c);
            game.getTriggerHandler().runTrigger(TriggerType.ClaimPrize, runParams, false);
        }
    }

    @Override
    protected String getStackDescription(SpellAbility sa) {
        final Card host = sa.getHostCard();
        final CardCollection attractions = AbilityUtils.getDefinedCards(host, sa.getParamOrDefault("Defined", "Self"), sa);
        return String.format("Claim the Prize from %s!", Lang.joinHomogenous(attractions));
    }
}
