package forge.game.ability.effects;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.spellability.SpellAbility;

public class UnattachEffect extends SpellAbilityEffect {
    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#getStackDescription(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
        sb.append("Unattach ");
        final List<Card> targets = getTargetCards(sa);
        sb.append(StringUtils.join(targets, " "));
        return sb.toString();
    }

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#resolve(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    public void resolve(SpellAbility sa) {
        final List<Card> unattachList = getTargetCards(sa);
        for (final Card cardToUnattach : unattachList) {
            if (cardToUnattach.isAura()) {
                //final boolean gainControl = "GainControl".equals(af.parseParams().get("AILogic"));
                //AbilityFactoryAttach.handleUnattachAura(cardToUnattach, c, gainControl);
            } else if (cardToUnattach.isAttachment()) {
                if (cardToUnattach.isAttachedToEntity()) {
                    cardToUnattach.unattachFromEntity(cardToUnattach.getEntityAttachedTo());
                }
            }
        }
    }
}
