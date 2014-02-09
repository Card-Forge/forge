package forge.game.ability.effects;


import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;

import java.util.List;

public class LifeGainEffect extends SpellAbilityEffect {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.AbilityFactoryAlterLife.SpellEffect#getStackDescription(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
        final int amount = AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParam("LifeAmount"), sa);

        for (final Player player : getDefinedPlayersOrTargeted(sa)) {
            sb.append(player).append(" ");
        }

        sb.append("gains ").append(amount).append(" life.");

        return sb.toString();
    }

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.AbilityFactoryAlterLife.SpellEffect#resolve(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    public void resolve(SpellAbility sa) {
        final int lifeAmount = AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParam("LifeAmount"), sa);

        final TargetRestrictions tgt = sa.getTargetRestrictions();
        List<Player> tgtPlayers = getDefinedPlayersOrTargeted(sa);
        if( tgtPlayers.isEmpty() ) {
            tgtPlayers.add(sa.getActivatingPlayer());
        }

        for (final Player p : tgtPlayers) {
            if ((tgt == null) || p.canBeTargetedBy(sa)) {
                p.gainLife(lifeAmount, sa.getHostCard());
            }
        }
    }

}
