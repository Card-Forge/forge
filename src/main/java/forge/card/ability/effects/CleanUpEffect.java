package forge.card.ability.effects;


import forge.Card;
import forge.Singletons;
import forge.card.ability.AbilityUtils;
import forge.card.ability.SpellAbilityEffect;
import forge.card.spellability.SpellAbility;

public class CleanUpEffect extends SpellAbilityEffect {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#resolve(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    public void resolve(SpellAbility sa) {
        Card source = sa.getSourceCard();

        if (sa.hasParam("ClearRemembered")) {
            source.clearRemembered();
            Singletons.getModel().getGame().getCardState(source).clearRemembered();
        }
        if (sa.hasParam("ForgetDefined")) {
            for (final Card card : AbilityUtils.getDefinedCards(source, sa.getParam("ForgetDefined"), sa)) {
                source.getRemembered().remove(card);
            }
        }
        if (sa.hasParam("ClearImprinted")) {
            source.clearImprinted();
        }
        if (sa.hasParam("ClearChosenX")) {
            source.setSVar("ChosenX", "");
        }
        if (sa.hasParam("ClearChosenY")) {
            source.setSVar("ChosenY", "");
        }
        if (sa.hasParam("ClearTriggered")) {
            Singletons.getModel().getGame().getTriggerHandler().clearDelayedTrigger(source);
        }
    }

}
