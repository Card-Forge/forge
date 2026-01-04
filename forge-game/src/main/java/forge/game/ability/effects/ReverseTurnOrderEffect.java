package forge.game.ability.effects;

import forge.game.ability.SpellAbilityEffect;
import forge.game.spellability.SpellAbility;

public class ReverseTurnOrderEffect extends SpellAbilityEffect {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#resolve(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    public void resolve(final SpellAbility sa) {
        sa.getHostCard().getGame().reverseTurnOrder();
    }

}
