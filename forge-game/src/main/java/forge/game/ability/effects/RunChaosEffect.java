package forge.game.ability.effects;

import com.google.common.collect.Lists;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerType;
import forge.game.trigger.WrappedAbility;

import java.util.List;

public class RunChaosEffect extends SpellAbilityEffect {

    @Override
    public void resolve(SpellAbility sa) {

        List<SpellAbility> validSA = Lists.newArrayList();
        for (final Card c : getTargetCards(sa)) {
            for (Trigger t : c.getTriggers()) {
                if (t.getMode() == TriggerType.ChaosEnsues) {
                    SpellAbility triggerSA = t.ensureAbility().copy(sa.getActivatingPlayer());

                    Player decider = sa.getActivatingPlayer();
                    if (t.hasParam("OptionalDecider")) {
                        sa.setOptionalTrigger(true);
                        decider = AbilityUtils.getDefinedPlayers(c, t.getParam("OptionalDecider"), sa).get(0);
                    } else if (t.hasParam("Cost")) {
                        sa.setOptionalTrigger(true);
                    }

                    final WrappedAbility wrapperAbility = new WrappedAbility(t, triggerSA, decider);
                    validSA.add(wrapperAbility);
                }
            }
        }
        sa.getActivatingPlayer().getController().orderAndPlaySimultaneousSa(validSA);
    }
}
