package forge.ai.ability;

import java.util.List;
import java.util.Map;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import forge.ai.ComputerUtilAbility;
import forge.ai.ComputerUtilCard;
import forge.ai.ComputerUtilCost;
import forge.ai.SpecialCardAi;
import forge.ai.SpellAbilityAi;
import forge.ai.SpellApiToAi;
import forge.card.MagicColor;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates.Presets;
import forge.game.card.CardUtil;
import forge.game.card.CounterEnumType;
import forge.game.combat.Combat;
import forge.game.combat.CombatUtil;
import forge.game.cost.Cost;
import forge.game.keyword.Keyword;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.Aggregates;
import forge.util.collect.FCollection;


public class ChooseGenericEffectAi extends SpellAbilityAi {

    @Override
    protected boolean checkAiLogic(final Player ai, final SpellAbility sa, final String aiLogic) {
        if ("Khans".equals(aiLogic) || "Dragons".equals(aiLogic)) {
            return true;
        } else if (aiLogic.startsWith("Fabricate") || "Riot".equals(aiLogic)) {
            return true;
        } else if ("Pump".equals(aiLogic) || "BestOption".equals(aiLogic)) {
            for (AbilitySub sb : sa.getAdditionalAbilityList("Choices")) {
                if (SpellApiToAi.Converter.get(sb.getApi()).canPlayAIWithSubs(ai, sb)) {
                    return true;
                }
            }
        } else if ("GideonBlackblade".equals(aiLogic)) {
            return SpecialCardAi.GideonBlackblade.consider(ai, sa);
        } else if ("SoulEcho".equals(aiLogic)) {
            return doTriggerAINoCost(ai, sa, true);
        } else if ("Always".equals(aiLogic)) {
            return true;
        }
        return false;
    }

    @Override
    protected boolean checkApiLogic(final Player ai, final SpellAbility sa) {
        return sa.hasParam("AILogic");
    }

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#chkAIDrawback(java.util.Map, forge.card.spellability.SpellAbility, forge.game.player.Player)
     */
    @Override
    public boolean chkAIDrawback(SpellAbility sa, Player aiPlayer) {
        return checkApiLogic(aiPlayer, sa);
    }

    @Override
    protected boolean doTriggerAINoCost(final Player aiPlayer, final SpellAbility sa, final boolean mandatory) {
        if ("CombustibleGearhulk".equals(sa.getParam("AILogic")) || "SoulEcho".equals(sa.getParam("AILogic"))) {
            for (final Player p : aiPlayer.getOpponents()) {
                if (p.canBeTargetedBy(sa)) {
                    sa.resetTargets();
                    sa.getTargets().add(p);
                    return true;
                }
            }
            return true; // perhaps the opponent(s) had Sigarda, Heron's Grace or another effect giving hexproof in play, still play the creature as 6/6
        }

        return super.doTriggerAINoCost(aiPlayer, sa, mandatory);
    }

    @Override
    public SpellAbility chooseSingleSpellAbility(Player player, SpellAbility sa, List<SpellAbility> spells,
            Map<String, Object> params) {
        Card host = sa.getHostCard();
        final String sourceName = ComputerUtilAbility.getAbilitySourceName(sa);
        final Game game = host.getGame();
        final Combat combat = game.getCombat();
        final String logic = sa.getParam("AILogic");
        if (logic == null) {
            return spells.get(0);
        } else if ("Random".equals(logic)) {
            return Aggregates.random(spells);
        } else if ("GideonBlackblade".equals(logic)) {
            return SpecialCardAi.GideonBlackblade.chooseSpellAbility(player, sa, spells);
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
                if (ComputerUtilCost.willPayUnlessCost(sp, player, unless, false, new FCollection<>(player))
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
        } else if ("SelfOthers".equals(logic)) {
            SpellAbility self = null, others = null;
            for (final SpellAbility sp : spells) {
                if (sp.getDescription().equals("Self")) {
                    self = sp;
                } else {
                    others = sp;
                }
            }
            String hostname = host.getName();
            if (hostname.equals("May Civilization Collapse")) {
                if (player.getLandsInPlay().isEmpty()) {
                    return self;
                }
            } else if (hostname.equals("Feed the Machine")) {
                if (player.getCreaturesInPlay().isEmpty()) {
                    return self;
                }
            } else if (hostname.equals("Surrender Your Thoughts")) {
                if (player.getCardsIn(ZoneType.Hand).isEmpty()) {
                    return self;
                }
            } else if (hostname.equals("The Fate of the Flammable")) {
                if (!player.canLoseLife()) {
                    return self;
                }
            }
            return others;
        } else if ("Counters".equals(logic)) {
            // TODO: this code will need generalization if this logic is used for cards other
            // than Elspeth Conquers Death with different choice parameters
            SpellAbility p1p1 = null, loyalty = null;
            for (final SpellAbility sp : spells) {
                if (("P1P1").equals(sp.getParam("CounterType"))) {
                    p1p1 = sp;
                } else {
                    loyalty = sp;
                }
            }
            if (sa.getParent().getTargetCard() != null && sa.getParent().getTargetCard().getType().isPlaneswalker()) {
                return loyalty;
            } else {
                return p1p1;
            }
        } else if ("Fatespinner".equals(logic)) {
            SpellAbility skipDraw = null, /*skipMain = null,*/ skipCombat = null;
            for (final SpellAbility sp : spells) {
                if (sp.getDescription().equals("FatespinnerSkipDraw")) {
                    skipDraw = sp;
                } else if (sp.getDescription().equals("FatespinnerSkipMain")) {
                    //skipMain = sp;
                } else {
                    skipCombat = sp;
                }
            }
            // FatespinnerSkipDraw,FatespinnerSkipMain,FatespinnerSkipCombat
            if (game.getReplacementHandler().wouldPhaseBeSkipped(player, "Draw")) {
                return skipDraw;
            }
            if (game.getReplacementHandler().wouldPhaseBeSkipped(player, "BeginCombat")) {
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
            if (owner.isCardInCommand("Emblem - Tamiyo, the Moon Sage")) {
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
        } else if (logic.startsWith("Fabricate")) {
            final int n = Integer.valueOf(logic.substring("Fabricate".length()));
            if(spells.size() < 2) {
				// If the creature is no longer on the battlefield, the option
				// to add counters is already removed at this point. Return the
				// only available option: create servo tokens.
            	return spells.get(0);
            }
            SpellAbility counterSA = spells.get(0), tokenSA = spells.get(1);

            // check for something which might prevent the counters to be placed on host
            if (!host.canReceiveCounters(CounterEnumType.P1P1)) {
                return tokenSA;
            }

            // if host would leave the play or if host is useless, create tokens
            if (host.hasSVar("EndOfTurnLeavePlay") || ComputerUtilCard.isUselessCreature(player, host)) {
                return tokenSA;
            }

            // need a copy for one with extra +1/+1 counter boost,
            // without causing triggers to run
            final Card copy = CardUtil.getLKICopy(host);
            copy.setCounters(CounterEnumType.P1P1, copy.getCounters(CounterEnumType.P1P1) + n);
            copy.setZone(host.getZone());

            // if host would put into the battlefield attacking
            if (combat != null && combat.isAttacking(host)) {
                final Player defender = combat.getDefenderPlayerByAttacker(host);
                if (defender.canLoseLife() && !ComputerUtilCard.canBeBlockedProfitably(defender, copy)) {
                    return counterSA;
                }
                return tokenSA;
            }

            // if the host has haste and can attack
            if (CombatUtil.canAttack(copy)) {
                for (final Player opp : player.getOpponents()) {
                    if (CombatUtil.canAttack(copy, opp) &&
                            opp.canLoseLife() &&
                            !ComputerUtilCard.canBeBlockedProfitably(opp, copy))
                        return counterSA;
                }
            }

            // TODO check for trigger to turn token ETB into +1/+1 counter for host
            // TODO check for trigger to turn token ETB into damage or life loss for opponent
            // in this cases Token might be prefered even if they would not survive
            final Card tokenCard = TokenAi.spawnToken(player, tokenSA);

            // Token would not survive
            if (!tokenCard.isCreature() || tokenCard.getNetToughness() < 1) {
                return counterSA;
            }

            // Special Card logic, this one try to median its power with the number of artifacts
            if ("Marionette Master".equals(sourceName)) {
                CardCollection list = CardLists.filter(player.getCardsIn(ZoneType.Battlefield), Presets.ARTIFACTS);
                return list.size() >= copy.getNetPower() ? counterSA : tokenSA;
            } else if ("Cultivator of Blades".equals(sourceName)) {
                // Cultivator does try to median with number of Creatures
                CardCollection list = player.getCreaturesInPlay();
                return list.size() >= copy.getNetPower() ? counterSA : tokenSA;
            }

            // evaluate Creature with +1/+1
            int evalCounter = ComputerUtilCard.evaluateCreature(copy);

            final CardCollection tokenList = new CardCollection(host);
            for (int i = 0; i < n; ++i) {
                tokenList.add(TokenAi.spawnToken(player, tokenSA));
            }

            // evaluate Host with Tokens
            int evalToken = ComputerUtilCard.evaluateCreatureList(tokenList);

            return evalToken >= evalCounter ? tokenSA : counterSA;
        } else if ("CombustibleGearhulk".equals(logic)) {
            Player controller = sa.getActivatingPlayer();
            List<ZoneType> zones = ZoneType.listValueOf("Graveyard, Battlefield, Exile");
            int life = player.getLife();
            CardCollectionView revealedCards = controller.getCardsIn(zones);

            if (revealedCards.size() < 5) {
                // Not very many revealed cards, just guess based on lifetotal
                return life < 7 ? spells.get(0) : spells.get(1);
            }

            int totalCMC = 0;
            for(Card c : revealedCards) {
                totalCMC += c.getCMC();
            }

            int bestGuessDamage = totalCMC * 3 / revealedCards.size();
            return life <= bestGuessDamage ? spells.get(0) : spells.get(1);
        }  else if ("SoulEcho".equals(logic)) {
            Player target = sa.getTargetingPlayer();
            int life = target.getLife();
            return life < 10 ? spells.get(0) : Aggregates.random(spells);
        } else if ("Pump".equals(logic) || "BestOption".equals(logic)) {
            List<SpellAbility> filtered = Lists.newArrayList();
            // filter first for the spells which can be done
            for (SpellAbility sp : spells) {
                if (SpellApiToAi.Converter.get(sp.getApi()).canPlayAIWithSubs(player, sp)) {
                    filtered.add(sp);
                }
            }

            // TODO find better way to check
            if (!filtered.isEmpty()) {
                return filtered.get(0);
            }
        } else if ("Riot".equals(logic)) {
            SpellAbility counterSA = spells.get(0), hasteSA = spells.get(1);
            return preferHasteForRiot(sa, player) ? hasteSA : counterSA;
        }
        return spells.get(0);   // return first choice if no logic found
    }

    public static boolean preferHasteForRiot(SpellAbility sa, Player player) {
        // returning true means preferring Haste, returning false means preferring a +1/+1 counter
        final Card host = sa.getHostCard();
        final Game game = host.getGame();
        final Card copy = CardUtil.getLKICopy(host);
        copy.setLastKnownZone(player.getZone(ZoneType.Battlefield));

        // check state it would have on the battlefield
        CardCollection preList = new CardCollection(copy);
        game.getAction().checkStaticAbilities(false, Sets.newHashSet(copy), preList);
        // reset again?
        game.getAction().checkStaticAbilities(false);

        // can't gain counters, use Haste
        if (!copy.canReceiveCounters(CounterEnumType.P1P1)) {
            return true;
        }

        // already has Haste, use counter
        if (copy.hasKeyword(Keyword.HASTE)) {
            return false;
        }

        // not AI turn
        if (!game.getPhaseHandler().isPlayerTurn(player)) {
            return false;
        }

        // not before Combat
        if (!game.getPhaseHandler().getPhase().isBefore(PhaseType.COMBAT_DECLARE_ATTACKERS)) {
            return false;
        }

        // TODO check other opponents too if able
        final Player opp = player.getWeakestOpponent();
        if (opp != null) {
            // TODO add predict Combat Damage?
            return opp.getLife() < copy.getNetPower();
        }

        // haste might not be good enough?
        return false;
    }
}
