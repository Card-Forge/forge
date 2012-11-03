package forge.card.abilityfactory.effects;

import java.util.HashMap;
import java.util.Map;

import forge.Singletons;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.abilityfactory.SpellEffect;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.SpellAbility;
import forge.card.trigger.TriggerType;

public class ClashEffect extends SpellEffect {
    
    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#getStackDescription(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected String getStackDescription(Map<String, String> params, SpellAbility sa) {
        return sa.getSourceCard().getName() + " - Clash with an opponent.";
    }
    
    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#resolve(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    public void resolve(Map<String, String> params, SpellAbility sa) {
        final AbilityFactory afOutcomes = new AbilityFactory();
        final boolean victory = sa.getSourceCard().getController().clashWithOpponent(sa.getSourceCard());

        // Run triggers
        final HashMap<String, Object> runParams = new HashMap<String, Object>();
        runParams.put("Player", sa.getSourceCard().getController());

        if (victory) {
            if (params.containsKey("WinSubAbility")) {
                final SpellAbility win = afOutcomes.getAbility(
                        sa.getSourceCard().getSVar(params.get("WinSubAbility")), sa.getSourceCard());
                win.setActivatingPlayer(sa.getSourceCard().getController());
                ((AbilitySub) win).setParent(sa);

                AbilityFactory.resolve(win, false);
            }
            runParams.put("Won", "True");
        } else {
            if (params.containsKey("OtherwiseSubAbility")) {
                final SpellAbility otherwise = afOutcomes.getAbility(
                        sa.getSourceCard().getSVar(params.get("OtherwiseSubAbility")), sa.getSourceCard());
                otherwise.setActivatingPlayer(sa.getSourceCard().getController());
                ((AbilitySub) otherwise).setParent(sa);

                AbilityFactory.resolve(otherwise, false);
            }
            runParams.put("Won", "False");
        }

        Singletons.getModel().getGame().getTriggerHandler().runTrigger(TriggerType.Clashed, runParams);
    }

}