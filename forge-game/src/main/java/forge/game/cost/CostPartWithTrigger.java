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
    /**
     * Serializables need a version ID.
     */
    private static final long serialVersionUID = 1L;

    public CostPartWithTrigger(final String amount, final String type, final String description) {
        super(amount, type, description);
    }

    protected Trigger payTrig;

    @Override
    protected final void handleBeforePayment(Player ai, SpellAbility ability, CardCollectionView targetCards) {
        if (payingTrigSA != null) {
            Card source = payingTrigSA.getHostCard();

            Map<String, String> mapParams = Maps.newHashMap();
            mapParams.put("TriggerDescription", payingTrigSA.getParam("SpellDescription"));
            mapParams.put("Mode", TriggerType.Immediate.name());

            SpellAbility sa = payingTrigSA.copy(source, ability.getActivatingPlayer(), false);
            sa.changeText();

            payTrig = TriggerHandler.parseTrigger(mapParams, source, sa.isIntrinsic(), null);
            payTrig.setSpawningAbility(ability); // make the StaticAbility the Spawning one?

            payTrig.setOverridingAbility(sa);

            // Instead of registering this, add to the delayed triggers as an immediate trigger type? Which means it'll fire as soon as possible
            ai.getGame().getTriggerHandler().registerDelayedTrigger(payTrig);
        }
    }
    
}
