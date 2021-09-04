package forge.game.ability.effects;

import java.util.List;

import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.util.Lang;

public class LifeGainEffect extends SpellAbilityEffect {

    /* (non-Javadoc)
     * @see forge.game.ability.SpellAbilityEffect#getStackDescription(forge.game.spellability.SpellAbility)
     */
    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
        final String amountStr = sa.getParam("LifeAmount");

        sb.append(Lang.joinHomogenous(getDefinedPlayersOrTargeted(sa)));

        if (!amountStr.equals("AFLifeLost") || sa.hasSVar(amountStr)) {
            final int amount = AbilityUtils.calculateAmount(sa.getHostCard(), amountStr, sa);

            sb.append(" gains ").append(amount).append(" life.");
        } else {
            sb.append(" gains life equal to the life lost this way.");
        }

        return sb.toString();
    }

    /* (non-Javadoc)
     * @see forge.game.ability.SpellAbilityEffect#resolve(forge.game.spellability.SpellAbility)
     */
    @Override
    public void resolve(SpellAbility sa) {
        final int lifeAmount = AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParam("LifeAmount"), sa);

        List<Player> tgtPlayers = getDefinedPlayersOrTargeted(sa);
        if (tgtPlayers.isEmpty()) {
            tgtPlayers.add(sa.getActivatingPlayer());
        }

        for (final Player p : tgtPlayers) {
            if (!sa.usesTargeting() || p.canBeTargetedBy(sa)) {
                p.gainLife(lifeAmount, sa.getHostCard(), sa);
            }
        }
    }

}
