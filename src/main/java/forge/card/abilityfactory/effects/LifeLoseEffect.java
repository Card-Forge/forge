package forge.card.abilityfactory.effects;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import forge.Card;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.abilityfactory.ApiType;
import forge.card.abilityfactory.SpellEffect;
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
        final int amount = AbilityFactory.calculateAmount(sa.getSourceCard(), sa.getParam("LifeAmount"), sa);

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

        final int lifeAmount = AbilityFactory.calculateAmount(sa.getSourceCard(), sa.getParam("LifeAmount"), sa);

        final Target tgt = sa.getTarget();
        for (final Player p : getTargetPlayers(sa)) {
            if ((tgt == null) || p.canBeTargetedBy(sa)) {
                lifeLost += p.loseLife(lifeAmount);
            }
        }
        sa.getSourceCard().setSVar("AFLifeLost", "Number$" + Integer.toString(lifeLost));
    }

}
