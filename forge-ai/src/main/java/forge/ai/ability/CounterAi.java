package forge.ai.ability;

import java.util.Iterator;

import forge.ai.ComputerUtilCost;
import forge.ai.ComputerUtilMana;
import forge.ai.SpellAbilityAi;
import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardFactoryUtil;
import forge.game.cost.Cost;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityStackInstance;
import forge.game.spellability.TargetRestrictions;
import forge.util.MyRandom;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

public class CounterAi extends SpellAbilityAi {

    @Override
    protected boolean canPlayAI(Player ai, SpellAbility sa) {
        boolean toReturn = true;
        final Cost abCost = sa.getPayCosts();
        final Card source = sa.getHostCard();
        final Game game = ai.getGame();
        if (game.getStack().isEmpty()) {
            return false;
        }

        if (abCost != null) {
            // AI currently disabled for these costs
            if (!ComputerUtilCost.checkSacrificeCost(ai, abCost, source)) {
                return false;
            }
            if (!ComputerUtilCost.checkLifeCost(ai, abCost, source, 4, null)) {
                return false;
            }
        }

        final TargetRestrictions tgt = sa.getTargetRestrictions();
        if (tgt != null) {

            final SpellAbility topSA = findTopSpellAbility(game, sa);
            if (!CardFactoryUtil.isCounterableBy(topSA.getHostCard(), sa) || topSA.getActivatingPlayer() == ai
                    || ai.getAllies().contains(topSA.getActivatingPlayer())) {
                // might as well check for player's friendliness
                return false;
            }
            if (sa.hasParam("AITgts") && (topSA.getHostCard() == null
                    || !topSA.getHostCard().isValid(sa.getParam("AITgts"), sa.getActivatingPlayer(), source, sa))) {
                return false;
            }

            if (sa.hasParam("CounterNoManaSpell") && topSA.getTotalManaSpent() > 0) {
                return false;
            }

            sa.resetTargets();
            if (sa.canTargetSpellAbility(topSA)) {
                sa.getTargets().add(topSA);
            } else {
                return false;
            }
        } else {
            return false;
        }

        String unlessCost = sa.hasParam("UnlessCost") ? sa.getParam("UnlessCost").trim() : null;

        if (unlessCost != null && !unlessCost.endsWith(">")) {
            // Is this Usable Mana Sources? Or Total Available Mana?
            final int usableManaSources = ComputerUtilMana.getAvailableMana(ai.getOpponent(), true).size();
            int toPay = 0;
            boolean setPayX = false;
            if (unlessCost.equals("X") && source.getSVar(unlessCost).equals("Count$xPaid")) {
                setPayX = true;
                toPay = ComputerUtilMana.determineLeftoverMana(sa, ai);
            } else {
                toPay = AbilityUtils.calculateAmount(source, unlessCost, sa);
            }

            if (toPay == 0) {
                return false;
            }

            if (toPay <= usableManaSources) {
                // If this is a reusable Resource, feel free to play it most of
                // the time
                if (!SpellAbilityAi.playReusable(ai,sa)) {
                    return false;
                }
            }

            if (setPayX) {
                source.setSVar("PayX", Integer.toString(toPay));
            }
        }

        // TODO Improve AI

        // Will return true if this spell can counter (or is Reusable and can
        // force the Human into making decisions)

        // But really it should be more picky about how it counters things

        if (sa.hasParam("AILogic")) {
            String logic = sa.getParam("AILogic");
            if ("Never".equals(logic)) {
                return false;
            }
        }



        return toReturn;
    }

    @Override
    public boolean chkAIDrawback(SpellAbility sa, Player aiPlayer) {
        return doTriggerAINoCost(aiPlayer, sa, true);
    }

    @Override
    protected boolean doTriggerAINoCost(Player ai, SpellAbility sa, boolean mandatory) {
        final TargetRestrictions tgt = sa.getTargetRestrictions();
        final Game game = ai.getGame();

        if (tgt != null) {
            if (game.getStack().isEmpty()) {
                return false;
            }

            sa.resetTargets();
            Pair<SpellAbility, Boolean> pair = chooseTargetSpellAbility(game, sa, ai, mandatory);
            SpellAbility tgtSA = pair.getLeft();

            if (tgtSA == null) {
                return false;
            }
            sa.getTargets().add(tgtSA);
            if (!mandatory && !pair.getRight()) {
                // If not mandatory and not preferred, bail out after setting target
                return false;
            }

            String unlessCost = sa.hasParam("UnlessCost") ? sa.getParam("UnlessCost").trim() : null;

            final Card source = sa.getHostCard();
            if (unlessCost != null) {
                // Is this Usable Mana Sources? Or Total Available Mana?
                final int usableManaSources = ComputerUtilMana.getAvailableMana(ai.getOpponent(), true).size();
                int toPay = 0;
                boolean setPayX = false;
                if (unlessCost.equals("X") && source.getSVar(unlessCost).equals("Count$xPaid")) {
                    setPayX = true;
                    toPay = ComputerUtilMana.determineLeftoverMana(sa, ai);
                } else {
                    toPay = AbilityUtils.calculateAmount(source, unlessCost, sa);
                }

                if (!mandatory) {
                    if (toPay == 0) {
                        return false;
                    }

                    if (toPay <= usableManaSources) {
                        // If this is a reusable Resource, feel free to play it most
                        // of the time
                        if (!SpellAbilityAi.playReusable(ai,sa) || (MyRandom.getRandom().nextFloat() < .4)) {
                            return false;
                        }
                    }
                }

                if (setPayX) {
                    source.setSVar("PayX", Integer.toString(toPay));
                }
            }
        }

        // TODO Improve AI

        // Will return true if this spell can counter (or is Reusable and can
        // force the Human into making decisions)

        // But really it should be more picky about how it counters things
        return true;
    }

    public Pair<SpellAbility, Boolean> chooseTargetSpellAbility(Game game, SpellAbility sa, Player ai, boolean mandatory) {
        SpellAbility tgtSA;
        SpellAbility leastBadOption = null;
        SpellAbility bestOption = null;

        Iterator<SpellAbilityStackInstance> it = game.getStack().iterator();
        SpellAbilityStackInstance si = null;
        while(it.hasNext()) {
            si = it.next();
            tgtSA = si.getSpellAbility(true);
            if (!sa.canTargetSpellAbility(tgtSA)) {
                continue;
            }
            if (leastBadOption == null) {
                leastBadOption = tgtSA;
            }

            if (!CardFactoryUtil.isCounterableBy(tgtSA.getHostCard(), sa) ||
                tgtSA.getActivatingPlayer() == ai ||
                !tgtSA.getActivatingPlayer().isOpponentOf(ai)) {
                // Is this a "better" least bad option
                if (leastBadOption.getActivatingPlayer().isOpponentOf(ai)) {
                    // NOOP
                } else if (sa.getActivatingPlayer().isOpponentOf(ai)) {
                    // Target opponents uncounterable stuff, before our own stuff
                    leastBadOption = tgtSA;
                }
                continue;
            }

            if (bestOption == null) {
                bestOption = tgtSA;
            } else {
                // TODO Determine if this option is better than the current best option
                boolean betterThanBest = false;
                if (betterThanBest) {
                    bestOption = tgtSA;
                }
                // Don't really need to keep updating leastBadOption once we have a bestOption
            }
        }

        return new ImmutablePair<>(bestOption != null ? bestOption : leastBadOption, bestOption != null);
    }

    public SpellAbility findTopSpellAbility(Game game, SpellAbility sa) {
        Iterator<SpellAbilityStackInstance> it = game.getStack().iterator();
        SpellAbility tgtSA = it.next().getSpellAbility(true);
        // Grab the topmost spellability that isn't this SA and use that for comparisons
        if (sa.equals(tgtSA) && game.getStack().size() > 1) {
            if (!it.hasNext()) {
                return null;
            }
            tgtSA = it.next().getSpellAbility(true);
        }
        return tgtSA;
    }
}
