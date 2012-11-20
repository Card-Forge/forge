package forge.card.abilityfactory.effects;


import forge.Card;
import forge.Singletons;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.abilityfactory.SpellEffect;
import forge.card.spellability.SpellAbility;

public class CleanUpEffect extends SpellEffect {

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
            System.out.println(AbilityFactory.getDefinedCards(source, sa.getParam("ForgetDefined"), sa));
            for (final Card card : AbilityFactory.getDefinedCards(source, sa.getParam("ForgetDefined"), sa)) {
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
