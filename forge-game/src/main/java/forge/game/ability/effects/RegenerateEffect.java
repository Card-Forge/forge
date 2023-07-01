package forge.game.ability.effects;

import java.util.List;

import forge.game.card.Card;
import forge.game.spellability.SpellAbility;
import forge.util.Lang;

public class RegenerateEffect extends RegenerateBaseEffect {

    /*
     * (non-Javadoc)
     * @see forge.game.ability.SpellAbilityEffect#getStackDescription(forge.game.spellability.SpellAbility)
     */
    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
        final List<Card> tgtCards = getDefinedCardsOrTargeted(sa);

        if (!tgtCards.isEmpty()) {
            sb.append("Regenerate ");
            sb.append(Lang.joinHomogenous(tgtCards));
            sb.append(".");
        }

        return sb.toString();
    }

    /*
     * (non-Javadoc)
     * @see forge.game.ability.SpellAbilityEffect#resolve(forge.game.spellability.SpellAbility)
     */
    @Override
    public void resolve(SpellAbility sa) {
        // create Effect for Regeneration
        createRegenerationEffect(sa, getDefinedCardsOrTargeted(sa));
    }

}
