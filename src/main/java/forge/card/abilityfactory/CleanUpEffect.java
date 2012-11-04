package forge.card.abilityfactory;

import java.util.Map;

import forge.Card;
import forge.Singletons;
import forge.card.spellability.SpellAbility;

public class CleanUpEffect extends SpellEffect { 
    
    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#resolve(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    public void resolve(Map<String, String> params, SpellAbility sa) {
        Card source = sa.getSourceCard();

        if (params.containsKey("ClearRemembered")) {
            source.clearRemembered();
            Singletons.getModel().getGame().getCardState(source).clearRemembered();
        }
        if (params.containsKey("ClearImprinted")) {
            source.clearImprinted();
        }
        if (params.containsKey("ClearChosenX")) {
            source.setSVar("ChosenX", "");
        }
        if (params.containsKey("ClearChosenY")) {
            source.setSVar("ChosenY", "");
        }
        if (params.containsKey("ClearTriggered")) {
            Singletons.getModel().getGame().getTriggerHandler().clearDelayedTrigger(source);
        }
    }

} // end class AbilityFactory_Cleanup