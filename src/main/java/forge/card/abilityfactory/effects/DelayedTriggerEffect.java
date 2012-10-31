package forge.card.abilityfactory.effects;

import forge.Singletons;
import forge.card.abilityfactory.SpellEffect;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.SpellAbility;
import forge.card.trigger.Trigger;
import forge.card.trigger.TriggerHandler;

public class DelayedTriggerEffect extends SpellEffect {
    
    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#resolve(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    public String getStackDescription(java.util.Map<String,String> mapParams, SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        if (sa instanceof AbilitySub) {
            sb.append(" ");
        } else {
            sb.append(sa.getSourceCard()).append(" - ");
        }

        if (mapParams.containsKey("SpellDescription")) {
            sb.append(mapParams.get("SpellDescription"));
        } else if (mapParams.containsKey("TriggerDescription")) {
            sb.append(mapParams.get("TriggerDescription"));
        }

        final AbilitySub abSub = sa.getSubAbility();
        if (abSub != null) {
            sb.append(abSub.getStackDescription());
        }

        return sb.toString();

    }

    /**
     * <p>
     * doResolve.
     * </p>
     * 
     * @param abilityFactory
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param spellAbility
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
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