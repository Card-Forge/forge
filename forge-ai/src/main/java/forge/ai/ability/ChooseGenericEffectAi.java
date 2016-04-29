package forge.ai.ability;

import forge.ai.ComputerUtilCombat;
import forge.ai.ComputerUtilCost;
import forge.ai.SpellAbilityAi;
import forge.card.MagicColor;
import forge.game.card.Card;
import forge.game.card.CardUtil;
import forge.game.combat.Combat;
import forge.game.combat.CombatUtil;
import forge.game.cost.Cost;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
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
            Player owner = imprinted.getOwner();
            
            //useless cards in hand
            if (imprinted.getName().equals("Bridge from Below") ||
            		imprinted.getName().equals("Haakon, Stromgald Scourge")) {
            	return allow;
            }
                        
            //bad cards when are thrown from the library to the graveyard, but Yixlid can prevent that
            if (!player.getGame().isCardInPlay("Yixlid Jailer") && (
            		imprinted.getName().equals("Gaea's Blessing") ||
            		imprinted.getName().equals("Narcomoeba"))) {
            	return allow;
            }
            
            // milling against Tamiyo is pointless
            if (owner.isCardInCommand("Tamiyo, the Moon Sage emblem")) {
            	return allow;
            }
            
            // milling a land against Gitrog result in card draw
            if (imprinted.isLand() && owner.isCardInPlay("The Gitrog Monster")) {
            	// try to mill owner
            	if (owner.getCardsIn(ZoneType.Library).size() < 5) {
            		return deny;
            	}
            	return allow;
            }
            
            // milling a creature against Sidisi result in more creatures
            if (imprinted.isCreature() && owner.isCardInPlay("Sidisi, Brood Tyrant")) {
            	return allow;
            }

            //if Iona does prevent from casting, allow it to draw 
            for (final Card io : player.getCardsIn(ZoneType.Battlefield, "Iona, Shield of Emeria")) {
            	if (CardUtil.getColors(imprinted).hasAnyColor(MagicColor.fromName(io.getChosenColor()))) {
                	return allow;
            	}
            }
            
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