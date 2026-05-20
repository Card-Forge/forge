package forge.game.ability.effects;

import forge.game.card.CardState;
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
        final CardState source = sa.getCardState();
        final StringBuilder sb = new StringBuilder();
        sb.append(CardTranslation.getTranslatedName(source.getName())).append(" - ").append(Localizer.getInstance().getMessage("lblCreature")).append(" ");
        sb.append(sa.getParamOrDefault("SetPower", source.getBasePowerString()));
        sb.append(" / ").append(sa.getParamOrDefault("SetToughness", source.getBaseToughnessString()));
        return sb.toString();
    }
}
