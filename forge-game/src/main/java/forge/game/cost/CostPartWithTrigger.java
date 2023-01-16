package forge.game.cost;

import java.util.Map;

import com.google.common.collect.Maps;

import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerHandler;
import forge.game.trigger.TriggerType;

public abstract class CostPartWithTrigger extends CostPartWithList {

    public CostPartWithTrigger(final String amount, final String type, final String description) {
        super(amount, type, description);
    }

    @Override
    protected final void handleBeforePayment(Player ai, SpellAbility ability, CardCollectionView targetCards) {
        if (payingTrigSA != null) {
            Card source = targetCards.get(0);

            Map<String, String> mapParams = Maps.newHashMap();
            mapParams.put("TriggerDescription", payingTrigSA.getParam("SpellDescription"));
            mapParams.put("Mode", TriggerType.Immediate.name());

            SpellAbility sa = payingTrigSA.copy(source, ability.getActivatingPlayer(), false);

            final Trigger immediateTrig = TriggerHandler.parseTrigger(mapParams, source, sa.isIntrinsic(), null);
            immediateTrig.setSpawningAbility(ability); // make the StaticAbility the Spawning one?

            immediateTrig.setOverridingAbility(sa);

            // Instead of registering this, add to the delayed triggers as an immediate trigger type? Which means it'll fire as soon as possible
            ai.getGame().getTriggerHandler().registerDelayedTrigger(immediateTrig);
        }
    }
    
}
