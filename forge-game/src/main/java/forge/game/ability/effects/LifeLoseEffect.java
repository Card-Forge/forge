package forge.game.ability.effects;


import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.ability.SpellAbilityEffect;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.util.Lang;

public class LifeLoseEffect extends SpellAbilityEffect {

    /* (non-Javadoc)
     * @see forge.game.ability.SpellAbilityEffect#getStackDescription(forge.game.spellability.SpellAbility)
     */
    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
        final int amount = AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParam("LifeAmount"), sa);

        int affected = getTargetPlayers(sa).size();
        sb.append(Lang.joinHomogenous(getTargetPlayers(sa)));

        sb.append(affected > 1 ? " each lose " : " loses ");
        sb.append(amount).append(" life.");

        return sb.toString();
    }

    /* (non-Javadoc)
     * @see forge.game.ability.SpellAbilityEffect#resolve(forge.game.spellability.SpellAbility)
     */
    @Override
    public void resolve(SpellAbility sa) {
        int lifeLost = 0;

        final int lifeAmount = AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParam("LifeAmount"), sa);

        for (final Player p : getTargetPlayers(sa)) {
            if (!sa.usesTargeting() || p.canBeTargetedBy(sa)) {
                lifeLost += p.loseLife(lifeAmount);
            }
        }
        sa.getHostCard().setSVar("AFLifeLost", "Number$" + lifeLost);

        // Exceptional case for Extort: must propagate the amount of life lost to subability, 
        // otherwise the first Extort trigger per game won't work
        if (sa.getSubAbility() != null && ApiType.GainLife.equals(sa.getSubAbility().getApi())) {
            sa.getSubAbility().setSVar("AFLifeLost", "Number$" + lifeLost);
        }
        
    }

}
