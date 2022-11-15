package forge.game.ability.effects;

import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.spellability.SpellAbility;

public class ChooseSectorEffect extends SpellAbilityEffect {

    @Override
    public void resolve(SpellAbility sa) {
        final Card card = sa.getHostCard();
        final String chosen = card.getController().getController().chooseSector(null, sa.getParamOrDefault("AILogic", ""));
        card.setChosenSector(chosen);
    }
}
