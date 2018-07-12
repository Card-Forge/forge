package forge.game.trigger;

import forge.game.card.Card;
import forge.game.spellability.SpellAbility;

import java.util.Map;

public class TriggerPayCost extends Trigger {

    public TriggerPayCost(final java.util.Map<String, String> params, final Card host, final boolean intrinsic) {
        super(params, host, intrinsic);
    }

    @Override
    public boolean performTest(Map<String, Object> runParams2) {
        return false;
    }

    @Override
    public void setTriggeringObjects(SpellAbility sa) {

    }

    @Override
    public String getImportantStackObjects(SpellAbility sa) {
        return null;
    }
}
