package forge.game.ability.effects;

import java.util.ArrayList;
import java.util.List;

import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.util.Localizer;

public class LifeSetEffect extends SpellAbilityEffect {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.AbilityFactoryAlterLife.SpellEffect#resolve(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    public void resolve(SpellAbility sa) {
        final boolean redistribute = sa.hasParam("Redistribute");
        final int lifeAmount = redistribute ? 20 : AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParam("LifeAmount"), sa);
        final TargetRestrictions tgt = sa.getTargetRestrictions();
        final List<Integer> lifetotals = new ArrayList<>();
        
        if (redistribute) {
            for (final Player p : getTargetPlayers(sa)) {
                if (tgt == null || p.canBeTargetedBy(sa)) {
                    lifetotals.add(p.getLife());
                }
            }
        }

        for (final Player p : getTargetPlayers(sa)) {
            if (tgt == null || p.canBeTargetedBy(sa)) {
                if (!redistribute) {
                    p.setLife(lifeAmount, sa.getHostCard());
                } else {
                    int life = sa.getActivatingPlayer().getController().chooseNumber(sa, Localizer.getInstance().getMessage("lblLifeTotal") + ": " + p, lifetotals, p);
                    p.setLife(life, sa.getHostCard());
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
        final int amount = AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParam("LifeAmount"), sa);
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
