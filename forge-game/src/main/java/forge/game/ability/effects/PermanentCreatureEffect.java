package forge.game.ability.effects;

import forge.game.card.Card;
import forge.game.spellability.SpellAbility;
import forge.util.CardTranslation;
import forge.util.Localizer;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class PermanentCreatureEffect extends PermanentEffect {

    @Override
    public String getStackDescription(final SpellAbility sa) {
        final Card sourceCard = sa.getHostCard();
        final StringBuilder sb = new StringBuilder();
        sb.append(CardTranslation.getTranslatedName(sourceCard.getName())).append(" - ").append(Localizer.getInstance().getMessage("lblCreature")).append(" ").append(sourceCard.getNetPower());
        sb.append(" / ").append(sourceCard.getNetToughness());
        return sb.toString();
    }
}
