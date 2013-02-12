package forge.card.ability.effects;

import java.util.List;

import forge.card.ability.AbilityUtils;
import forge.card.ability.SpellEffect;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.player.Player;

public class LifeSetEffect extends SpellEffect {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.AbilityFactoryAlterLife.SpellEffect#resolve(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    public void resolve(SpellAbility sa) {
        final int lifeAmount = AbilityUtils.calculateAmount(sa.getSourceCard(), sa.getParam("LifeAmount"), sa);
        final Target tgt = sa.getTarget();

        for (final Player p : getTargetPlayers(sa)) {
            if ((tgt == null) || p.canBeTargetedBy(sa)) {
                p.setLife(lifeAmount, sa.getSourceCard());
            }
        }
    }

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.AbilityFactoryAlterLife.SpellEffect#getStackDescription(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
        final int amount = AbilityUtils.calculateAmount(sa.getSourceCard(), sa.getParam("LifeAmount"), sa);

        List<Player> tgtPlayers = getTargetPlayers(sa);

        for (final Player player : tgtPlayers) {
            sb.append(player).append(" ");
        }

        sb.append("life total becomes ").append(amount).append(".");

        return sb.toString();
    }

}
