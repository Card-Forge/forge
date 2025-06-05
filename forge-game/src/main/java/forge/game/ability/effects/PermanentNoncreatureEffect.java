package forge.game.ability.effects;

import forge.game.spellability.SpellAbility;
import forge.util.CardTranslation;

/**
 * TODO: Write javadoc for this type.
 *
 */
public class PermanentNoncreatureEffect extends PermanentEffect {

    @Override
    public String getStackDescription(final SpellAbility sa) {
        //CardView toString return translated name,don't need call CardTranslation.getTranslatedName in this.
        return CardTranslation.getTranslatedName(sa.getCardState().getName());
    }
}
