package forge.game.ability.effects;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.Lists;

import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.player.Player;
import forge.game.player.PlayerCollection;
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
        final int lifeAmount = redistribute ? 0 : AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParam("LifeAmount"), sa);
        final TargetRestrictions tgt = sa.getTargetRestrictions();
        final List<Integer> lifetotals = new ArrayList<>();
        final PlayerCollection players = getTargetPlayers(sa);

        if (redistribute) {
            for (final Player p : players) {
                if (tgt == null || p.canBeTargetedBy(sa)) {
                    lifetotals.add(p.getLife());
                }
            }
        }

        for (final Player p : players.threadSafeIterable()) {
            if (tgt == null || p.canBeTargetedBy(sa)) {
                if (!redistribute) {
                    p.setLife(lifeAmount, sa.getHostCard());
                } else {
                    List<Integer> validChoices = getDistribution(players, true, lifetotals);
                    int life = sa.getActivatingPlayer().getController().chooseNumber(sa, Localizer.getInstance().getMessage("lblLifeTotal") + ": " + p, validChoices, p);
                    p.setLife(life, sa.getHostCard());
                    lifetotals.remove((Integer) life);
                    players.remove(p);
                }
            }
        }
    }

    private static List<Integer> getDistribution(PlayerCollection players, boolean top, List<Integer> remainingChoices) {
        // distribution was successful
        if (players.isEmpty()) {
            // carry signal back
            remainingChoices.add(1);
            return remainingChoices;
        }
        List<Integer> validChoices = Lists.newArrayList(remainingChoices);
        for (Player p : players) {
            for (Integer choice : remainingChoices) {
                // 119.7/8 illegal choice
                if ((p.getLife() < choice && !p.canGainLife()) || (p.getLife() > choice && !p.canLoseLife())) {
                    if (top) {
                        validChoices.remove(choice);
                    }
                    continue;
                }

                // combination is valid, check next
                PlayerCollection nextPlayers = new PlayerCollection(players);
                nextPlayers.remove(p);
                List<Integer> nextChoices = new ArrayList<>(remainingChoices);
                nextChoices.remove(choice);
                nextChoices = getDistribution(nextPlayers, false, nextChoices);
                if (nextChoices.isEmpty()) {
                    if (top) {
                        // top of recursion stack
                        validChoices.remove(choice);
                    }
                } else if (!top) {
                    return nextChoices;
                }
            }
            if (top) {
                // checking first player is enough
                return validChoices;
            }
        }
        return new ArrayList<Integer>();
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
