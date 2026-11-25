package forge.game.ability.effects;

import java.util.List;

import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.spellability.SpellAbility;

/**
 * "At the beginning of the next end step, sacrifice it" (or return, exile ...): the delayed trigger for the cards the
 * ability defines or targets, named by {@code AtEOT$}. Scripts spelled this "Pump | AtEOT$ Sacrifice".
 */
public class EndOfTurnEffect extends SpellAbilityEffect {
    @Override
    protected String getStackDescription(final SpellAbility sa) {
        return sa.getParamOrDefault("StackDescription", "");
    }

    @Override
    public void resolve(final SpellAbility sa) {
        final List<Card> cards = getCardsfromTargets(sa);
        if (!cards.isEmpty()) {
            registerDelayedTrigger(sa, sa.getParam("AtEOT"), cards);
        }
    }
}
