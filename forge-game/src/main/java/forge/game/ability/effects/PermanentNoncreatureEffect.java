package forge.game.ability.effects;

import forge.game.card.Card;
import forge.game.spellability.SpellAbility;
import forge.util.CardTranslation;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class PermanentNoncreatureEffect extends PermanentEffect {

    @Override
    public String getStackDescription(final SpellAbility sa) {
        final Card sourceCard = sa.getHostCard();
        //CardView toString return translated name,don't need call CardTranslation.getTranslatedName in this.
        return CardTranslation.getTranslatedName(sourceCard.getName());
    }
}
