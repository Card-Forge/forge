package forge.card.abilityfactory.effects;


import java.util.ArrayList;

import forge.card.abilityfactory.AbilityFactory;
import forge.card.abilityfactory.SpellEffect;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.player.Player;

public class LifeGainEffect extends SpellEffect {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.AbilityFactoryAlterLife.SpellEffect#getStackDescription(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
        final int amount = AbilityFactory.calculateAmount(sa.getSourceCard(), sa.getParam("LifeAmount"), sa);

        for (final Player player : getTargetPlayers(sa)) {
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
        final int lifeAmount = AbilityFactory.calculateAmount(sa.getSourceCard(), sa.getParam("LifeAmount"), sa);

        final Target tgt = sa.getTarget();
        ArrayList<Player> tgtPlayers = new ArrayList<Player>();

        if (sa.hasParam("Defined")) {
            tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), sa.getParam("Defined"), sa);
        } else if (tgt != null) {
            tgtPlayers = tgt.getTargetPlayers();
        } else {
            tgtPlayers.add(sa.getActivatingPlayer());
        }

        for (final Player p : tgtPlayers) {
            if ((tgt == null) || p.canBeTargetedBy(sa)) {
                p.gainLife(lifeAmount, sa.getSourceCard());
            }
        }
    }

}
