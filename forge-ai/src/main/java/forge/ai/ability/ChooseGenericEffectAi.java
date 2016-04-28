package forge.ai.ability;

import forge.ai.ComputerUtilCombat;
import forge.ai.ComputerUtilCost;
import forge.ai.SpellAbilityAi;
import forge.game.card.Card;
import forge.game.combat.Combat;
import forge.game.combat.CombatUtil;
import forge.game.cost.Cost;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.util.Aggregates;
import forge.util.collect.FCollection;

import java.util.List;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

/**
 * TODO: Write javadoc for this type.
 *
 */
public class ChooseGenericEffectAi extends SpellAbilityAi {

    @Override
    protected boolean canPlayAI(Player aiPlayer, SpellAbility sa) {
        if ("Khans".equals(sa.getParam("AILogic")) || "Dragons".equals(sa.getParam("AILogic"))) {
            return true;
        }
        return false;
    }

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#chkAIDrawback(java.util.Map, forge.card.spellability.SpellAbility, forge.game.player.Player)
     */
    @Override
    public boolean chkAIDrawback(SpellAbility sa, Player aiPlayer) {
        return canPlayAI(aiPlayer, sa);
    }

    @Override
    public SpellAbility chooseSingleSpellAbility(Player player, SpellAbility sa, List<SpellAbility> spells) {
        Card host = sa.getHostCard();
        final String logic = sa.getParam("AILogic");
        if ("Random".equals(logic)) {
            return Aggregates.random(spells);
        } else if ("Phasing".equals(logic)) { // Teferi's Realm : keep aggressive 
            List<SpellAbility> filtered = Lists.newArrayList(Iterables.filter(spells, new Predicate<SpellAbility>() {
                @Override
                public boolean apply(final SpellAbility sp) {
                    return !sp.getDescription().contains("Creature") && !sp.getDescription().contains("Land");
                }
            }));
            return Aggregates.random(filtered);
        } else if ("PayUnlessCost".equals(logic)) {
            for (final SpellAbility sp : spells) {
                String unlessCost = sp.getParam("UnlessCost");
                sp.setActivatingPlayer(sa.getActivatingPlayer());
                Cost unless = new Cost(unlessCost, false);
                SpellAbility paycost = new SpellAbility.EmptySa(sa.getHostCard(), player);
                paycost.setPayCosts(unless);
                if (ComputerUtilCost.willPayUnlessCost(sp, player, unless, false, new FCollection<Player>(player))
                        && ComputerUtilCost.canPayCost(paycost, player)) {
                    return sp;
                }
            }
            return spells.get(0);
        } else if ("Khans".equals(logic) || "Dragons".equals(logic)) { // Fate Reforged sieges
            for (final SpellAbility sp : spells) {
                if (sp.getDescription().equals(logic)) {
                    return sp;
                }
            }
        } else if ("Fatespinner".equals(logic)) {
            SpellAbility skipDraw = null, skipMain = null, skipCombat = null;
            for (final SpellAbility sp : spells) {
                if (sp.getDescription().equals("FatespinnerSkipDraw")) {
                    skipDraw = sp;
                } else if (sp.getDescription().equals("FatespinnerSkipMain")) {
                    skipMain = sp;
                } else {
                    skipCombat = sp;
                }
            }
            // FatespinnerSkipDraw,FatespinnerSkipMain,FatespinnerSkipCombat
            if (player.hasKeyword("Skip your draw step.")) {
                return skipDraw;
            }
            if (player.hasKeyword("Skip your next combat phase.")) {
                return skipCombat;
            }

            // TODO If combat is poor, Skip Combat
            // Todo if hand is empty or mostly empty, skip main phase
            // Todo if hand has gas, skip draw
            return Aggregates.random(spells);

        } else if ("SinProdder".equals(logic)) {
            SpellAbility allow = null, deny = null;
            for (final SpellAbility sp : spells) {
                if (sp.getDescription().equals("Allow")) {
                    allow = sp;
                } else {
                    deny = sp;
                }
            }

            Card imprinted = host.getImprintedCards().getFirst();
            int dmg = imprinted.getCMC();
            if (dmg == 0) {
                // If CMC = 0, mill it!
                return deny;
            } else if (dmg + 3 > player.getLife()) {
                // if low on life, do nothing.
                return allow;
            } else if (player.getLife() - dmg > 15) {
                // TODO Check "danger" level of card
                // If lots of life, and card might be dangerous? Mill it!
                return deny;
            }
            // if unsure, random?
            return Aggregates.random(spells);
        }
        return spells.get(0);   // return first choice if no logic found
    }
}