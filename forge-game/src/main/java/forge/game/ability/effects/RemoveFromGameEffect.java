package forge.game.ability.effects;

import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.spellability.SpellAbility;

public class RemoveFromGameEffect extends SpellAbilityEffect {

    @Override
    public void resolve(SpellAbility sa) {

        for (final Card tgtC : getTargetCards(sa)) {
            tgtC.getGame().getAction().ceaseToExist(tgtC, true);
        }

    }
}
