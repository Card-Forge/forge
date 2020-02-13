package forge.game.ability.effects;

import forge.game.ability.SpellAbilityEffect;
import forge.game.card.CardDamageMap;
import forge.game.spellability.SpellAbility;

public class DamageResolveEffect extends SpellAbilityEffect {

    public DamageResolveEffect() {
        // TODO Auto-generated constructor stub
    }

    /*
     * (non-Javadoc)
     * @see forge.game.ability.SpellAbilityEffect#resolve(forge.game.spellability.SpellAbility)
     */
    @Override
    public void resolve(SpellAbility sa) {
        CardDamageMap damageMap = sa.getDamageMap();
        CardDamageMap preventMap = sa.getPreventMap();
        
        if (preventMap != null) {
            preventMap.triggerPreventDamage(false);
            preventMap.clear();
        }
        // non combat damage cause lifegain there
        if (damageMap != null) {
            damageMap.triggerDamageDoneOnce(false, sa.getHostCard().getGame(), sa);
            damageMap.clear();
        }
    }

    /* (non-Javadoc)
     * @see forge.game.ability.SpellAbilityEffect#getStackDescription(forge.game.spellability.SpellAbility)
     */
    @Override
    protected String getStackDescription(SpellAbility sa) {
        return "";
    }

}
