package forge.game.ability.effects;

import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.spellability.SpellAbility;
import forge.util.Lang;

public class UnattachEffect extends SpellAbilityEffect {
    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#getStackDescription(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
        sb.append("Unattach ");
        sb.append(Lang.joinHomogenous(getTargetCards(sa)));
        return sb.toString();
    }

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#resolve(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    public void resolve(SpellAbility sa) {
        for (final Card cardToUnattach : getTargetCards(sa)) {
            if (cardToUnattach.isAttachment() && cardToUnattach.isAttachedToEntity()) {
                cardToUnattach.unattachFromEntity(cardToUnattach.getEntityAttachedTo());
            }
        }
    }
}
