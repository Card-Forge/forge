package forge.card.abilityfactory.effects;

import forge.Singletons;
import forge.card.abilityfactory.SpellEffect;
import forge.card.spellability.SpellAbility;
import forge.card.trigger.Trigger;
import forge.card.trigger.TriggerHandler;

public class DelayedTriggerEffect extends SpellEffect {
    
    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#resolve(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected String getStackDescription(java.util.Map<String,String> mapParams, SpellAbility sa) {
        if (mapParams.containsKey("TriggerDescription")) {
            return mapParams.get("TriggerDescription");
        }

        return "";

    }

    @Override
    public void resolve(java.util.Map<String,String> mapParams, SpellAbility sa) {

        if (mapParams.containsKey("Cost")) {
            mapParams.remove("Cost");
        }

        if (mapParams.containsKey("SpellDescription")) {
            mapParams.put("TriggerDescription", mapParams.get("SpellDescription"));
            mapParams.remove("SpellDescription");
        }

        final Trigger delTrig = TriggerHandler.parseTrigger(mapParams, sa.getSourceCard(), true);

        Singletons.getModel().getGame().getTriggerHandler().registerDelayedTrigger(delTrig);
    }
}