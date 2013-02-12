package forge.card.ability.effects;


import forge.card.ability.AbilityUtils;
import forge.card.ability.SpellEffect;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.player.Player;

public class LifeLoseEffect extends SpellEffect {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.AbilityFactoryAlterLife.SpellEffect#getStackDescription(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected String getStackDescription(SpellAbility sa) {

        final StringBuilder sb = new StringBuilder();
        final int amount = AbilityUtils.calculateAmount(sa.getSourceCard(), sa.getParam("LifeAmount"), sa);

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

        final int lifeAmount = AbilityUtils.calculateAmount(sa.getSourceCard(), sa.getParam("LifeAmount"), sa);

        final Target tgt = sa.getTarget();
        for (final Player p : getTargetPlayers(sa)) {
            if ((tgt == null) || p.canBeTargetedBy(sa)) {
                lifeLost += p.loseLife(lifeAmount);
            }
        }
        sa.getSourceCard().setSVar("AFLifeLost", "Number$" + Integer.toString(lifeLost));
    }

}
