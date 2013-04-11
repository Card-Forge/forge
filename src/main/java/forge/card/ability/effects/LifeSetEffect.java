package forge.card.ability.effects;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import forge.card.ability.AbilityUtils;
import forge.card.ability.SpellAbilityEffect;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.player.HumanPlayer;
import forge.game.player.Player;
import forge.gui.GuiChoose;

public class LifeSetEffect extends SpellAbilityEffect {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.AbilityFactoryAlterLife.SpellEffect#resolve(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    public void resolve(SpellAbility sa) {
        final boolean redistribute = sa.hasParam("Redistribute");
        final int lifeAmount = redistribute ? 20 : AbilityUtils.calculateAmount(sa.getSourceCard(), sa.getParam("LifeAmount"), sa);
        final Target tgt = sa.getTarget();
        final List<Integer> lifetotals = new ArrayList<Integer>();
        
        if (redistribute) {
            for (final Player p : getTargetPlayers(sa)) {
                if ((tgt == null) || p.canBeTargetedBy(sa)) {
                    lifetotals.add(p.getLife());
                }
            }
        }

        for (final Player p : getTargetPlayers(sa)) {
            if ((tgt == null) || p.canBeTargetedBy(sa)) {
                if (!redistribute) {
                    p.setLife(lifeAmount, sa.getSourceCard());
                } else {
                    int life;
                    if (sa.getActivatingPlayer() instanceof HumanPlayer) {
                        life = GuiChoose.one("Life Total: " + p, lifetotals);
                    } else {//AI
                        if (p.equals(sa.getSourceCard().getController())) {
                            life = Collections.max(lifetotals);
                        } else if (p.isOpponentOf(sa.getSourceCard().getController())) {
                            life = Collections.min(lifetotals);
                        } else {
                            life = lifetotals.get(0);
                        }
                    }
                    p.setLife(life, sa.getSourceCard());
                    lifetotals.remove((Integer) life);
                }
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
        final boolean redistribute = sa.hasParam("Redistribute");

        List<Player> tgtPlayers = getTargetPlayers(sa);
        if (!redistribute) {
            for (final Player player : tgtPlayers) {
                sb.append(player).append(" ");
            }
            sb.append("life total becomes ").append(amount).append(".");
        } else {
            sb.append("Redistribute ");
            for (final Player player : tgtPlayers) {
                sb.append(player).append(" ");
            }
            sb.append("life totals.");
        }
        return sb.toString();
    }

}
