package forge.game.trigger;

import java.util.Map;

import com.google.common.base.Functions;

import forge.game.ability.AbilityKey;
import forge.game.card.Card;
import forge.game.card.CounterType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.util.Aggregates;
import forge.util.Localizer;

public class TriggerCounterPlayerAddedAll extends Trigger {

    public TriggerCounterPlayerAddedAll(Map<String, String> params, Card host, boolean intrinsic) {
        super(params, host, intrinsic);
    }

    @Override
    public boolean performTest(Map<AbilityKey, Object> runParams) {
        if (!matchesValidParam("ValidSource", runParams.get(AbilityKey.Source))) {
            return false;
        }
        if (!matchesValidParam("ValidObject", runParams.get(AbilityKey.Object))) {
            return false;
        }
        if (hasParam("ValidObjectToSource")) {
            if (!matchesValid(runParams.get(AbilityKey.Object), getParam("ValidObjectToSource").split(","), getHostCard(),
                    (Player)runParams.get(AbilityKey.Source))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void setTriggeringObjects(SpellAbility sa, Map<AbilityKey, Object> runParams) {
        sa.setTriggeringObjectsFrom(runParams, AbilityKey.Source, AbilityKey.Object, AbilityKey.CounterMap);
        sa.setTriggeringObject(AbilityKey.Amount, Aggregates.sum(((Map<CounterType, Integer>) runParams.get(AbilityKey.CounterMap)).values(), Functions.identity()));
    }

    @Override
    public String getImportantStackObjects(SpellAbility sa) {
        StringBuilder sb = new StringBuilder();
        sb.append(Localizer.getInstance().getMessage("lblAddedOnce")).append(": ");
        sb.append(sa.getTriggeringObject(AbilityKey.Source)).append(": ");
        sb.append(sa.getTriggeringObject(AbilityKey.Object));
        return sb.toString();
    }

}
