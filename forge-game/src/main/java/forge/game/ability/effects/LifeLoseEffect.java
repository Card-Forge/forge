package forge.game.ability.effects;


import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;

public class LifeLoseEffect extends SpellAbilityEffect {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.AbilityFactoryAlterLife.SpellEffect#getStackDescription(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected String getStackDescription(SpellAbility sa) {

        final StringBuilder sb = new StringBuilder();
        final int amount = AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParam("LifeAmount"), sa);

        int affected = getTargetPlayers(sa).size();
        for (int i = 0; i < affected; i++) {
            final Player player = getTargetPlayers(sa).get(i);
            sb.append(player);
            sb.append(i < (affected - 2) ? ", " : i == (affected - 2) ? " and " : " ");
        }

        sb.append(affected > 1 ? "each lose " : "loses ");
        sb.append(amount).append(" life.");

        return sb.toString();
    }

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.AbilityFactoryAlterLife.SpellEffect#resolve(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    public void resolve(SpellAbility sa) {

        int lifeLost = 0;

        final int lifeAmount = AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParam("LifeAmount"), sa);

        final TargetRestrictions tgt = sa.getTargetRestrictions();
        for (final Player p : getTargetPlayers(sa)) {
            if ((tgt == null) || p.canBeTargetedBy(sa)) {
                lifeLost += p.loseLife(lifeAmount);
            }
        }
        sa.getHostCard().setSVar("AFLifeLost", "Number$" + Integer.toString(lifeLost));

        // Exceptional case for Extort: must propagate the amount of life lost to subability, 
        // otherwise the first Extort trigger per game won't work
        if (sa.getHostCard().hasKeyword("Extort") && sa.getSubAbility() != null) {
            sa.getSubAbility().setSVar("AFLifeLost", "Number$" + Integer.toString(lifeLost));
        }
        
    }

}
