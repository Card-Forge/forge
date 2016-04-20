package forge.ai.ability;

import com.google.common.base.Predicate;
import com.google.common.collect.Lists;

import forge.ai.*;
import forge.game.Game;
import forge.game.GameObject;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.card.CounterType;
import forge.game.cost.Cost;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetChoices;
import forge.game.spellability.TargetRestrictions;
import forge.game.zone.ZoneType;
import forge.util.Aggregates;

import java.util.List;

public class DamageDealAi extends DamageAiBase {
    @Override
    public boolean chkAIDrawback(SpellAbility sa, Player ai) {
        final String damage = sa.getParam("NumDmg");
        int dmg = AbilityUtils.calculateAmount(sa.getHostCard(), damage, sa);

        final Card source = sa.getHostCard();

        if (damage.equals("X") && sa.getSVar(damage).equals("Count$xPaid")) {
            // Set PayX here to maximum value.
            dmg = ComputerUtilMana.determineLeftoverMana(sa, ai);
            source.setSVar("PayX", Integer.toString(dmg));
        }
        if (!this.damageTargetAI(ai, sa, dmg, true)) {
            return false;
        }
        return true;
    }

    @Override
    protected boolean canPlayAI(Player ai, SpellAbility sa) {

        final Cost abCost = sa.getPayCosts();
        final Card source = sa.getHostCard();

        final String damage = sa.getParam("NumDmg");
        int dmg = AbilityUtils.calculateAmount(sa.getHostCard(), damage, sa);

        if ((damage.equals("X") && sa.getSVar(damage).equals("Count$xPaid") ||
                sa.getHostCard().getName().equals("Crater's Claws"))) {
            // Set PayX here to maximum value.
            dmg = ComputerUtilMana.determineLeftoverMana(sa, ai);
            source.setSVar("PayX", Integer.toString(dmg));
        }
        if (sa.getHostCard().getName().equals("Crater's Claws") && ai.hasFerocious()) {
            dmg += 2;
        }
        
        String logic = sa.getParam("AILogic");
        if ("DiscardLands".equals(logic)) {
            dmg = 2;
        } else if ("WildHunt".equals(logic)) {
            // This dummy ability will just deal 0 damage, but holds the logic for the AI for Master of Wild Hunt
            List<Card> wolves = CardLists.getValidCards(ai.getCardsIn(ZoneType.Battlefield), "Creature.Wolf+untapped+YouCtrl+Other", ai, source);
            dmg = Aggregates.sum(wolves, CardPredicates.Accessors.fnGetNetPower);
        }
        
        if (source.getName().equals("Sorin, Grim Nemesis")) {
            int loyalty = source.getCounters(CounterType.LOYALTY);
            for (; loyalty > 0; loyalty--) {
                if (this.damageTargetAI(ai, sa, loyalty, false)) {
                    dmg = ComputerUtilCombat.getEnoughDamageToKill(sa.getTargetCard(), loyalty, source, false, false);
                    if (dmg > loyalty || dmg < 1) {
                        continue;   // in case the calculation gets messed up somewhere
                    }
                    source.setSVar("ChosenX", "Number$" + dmg);
                    return true;
                }
            }
            return false;
        }

        if (dmg <= 0) {
            return false;
        }

        // temporarily disabled until better AI
        if (!ComputerUtilCost.checkLifeCost(ai, abCost, source, 4, null)) {
            return false;
        }

        if (!ComputerUtilCost.checkSacrificeCost(ai, abCost, source)) {
            return false;
        }

        if (!ComputerUtilCost.checkRemoveCounterCost(abCost, source)) {
            return false;
        }
        
        if ("DiscardLands".equals(sa.getParam("AILogic")) && !ComputerUtilCost.checkDiscardCost(ai, abCost, source)) {
            return false;
        }

        if (ComputerUtil.preventRunAwayActivations(sa)) {
        	return false;
        }

        if (!this.damageTargetAI(ai, sa, dmg, false)) {
            return false;
        }

        if ((damage.equals("X") && source.getSVar(damage).equals("Count$xPaid")) ||
                sa.getHostCard().getName().equals("Crater's Claws")){
            // If I can kill my target by paying less mana, do it
            if (sa.usesTargeting() && !sa.getTargets().isTargetingAnyPlayer() && !sa.hasParam("DividedAsYouChoose")) {
                int actualPay = 0;
                final boolean noPrevention = sa.hasParam("NoPrevention");
                for (final Card c : sa.getTargets().getTargetCards()) {
                    final int adjDamage = ComputerUtilCombat.getEnoughDamageToKill(c, dmg, source, false, noPrevention);
                    if ((adjDamage > actualPay) && (adjDamage <= dmg)) {
                        actualPay = adjDamage;
                    }
                }
                if (sa.getHostCard().getName().equals("Crater's Claws") && ai.hasFerocious()) {
                    actualPay = actualPay > 2 ? actualPay - 2 : 0;
                }
                source.setSVar("PayX", Integer.toString(actualPay));
            }
        }
        return true;
    }

    /**
     * <p>
     * dealDamageChooseTgtC.
     * </p>
     * 
     * @param d
     *            a int.
     * @param noPrevention
     *            a boolean.
     * @param pl
     *            a {@link forge.game.player.Player} object.
     * @param mandatory
     *            a boolean.
     * @return a {@link forge.game.card.Card} object.
     */
    private Card dealDamageChooseTgtC(final Player ai, final SpellAbility sa, final int d, final boolean noPrevention,
            final Player pl, final boolean mandatory) {

        // wait until stack is empty (prevents duplicate kills)
        if (!sa.isTrigger() && !ai.getGame().getStack().isEmpty()) {
            //TODO:all removal APIs require a check to prevent duplicate kill/bounce/exile/etc.
            //      The original code is a blunt instrument that also blocks all use of removal as interrupts. The issue is
            //      with the AI not having code to consider what occurred previously in the stack thus it has no memory of
            //      removing a target already if something else is placed on top of the stack. A better solution is to place
            //      the checking mechanism after the target is chosen and determine if the topstack invalidates the earlier
            //      removal (shroud effect, pump against damage) so a new removal can/should be applied if possible.
            //return null;
        }
        final TargetRestrictions tgt = sa.getTargetRestrictions();
        final Card source = sa.getHostCard();
        List<Card> hPlay = CardLists.getValidCards(pl.getCardsIn(ZoneType.Battlefield), tgt.getValidTgts(), ai, source, sa);

        final List<GameObject> objects = Lists.newArrayList(sa.getTargets().getTargets());
        if (sa.hasParam("TargetUnique")) {
            objects.addAll(sa.getUniqueTargets());
        }
        for (final Object o : objects) {
            if (o instanceof Card) {
                final Card c = (Card) o;
                if (hPlay.contains(c)) {
                    hPlay.remove(c);
                }
            }
        }
        hPlay = CardLists.getTargetableCards(hPlay, sa);

        final List<Card> killables = CardLists.filter(hPlay, new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                return c.getSVar("Targeting").equals("Dies") 
                        || (ComputerUtilCombat.getEnoughDamageToKill(c, d, source, false, noPrevention) <= d) && !ComputerUtil.canRegenerate(ai, c)
                            && !(c.getSVar("SacMe").length() > 0);
            }
        });

        Card targetCard = null;
        if (pl.isOpponentOf(ai) && !killables.isEmpty()) {
            if (sa.getTargetRestrictions().canTgtPlaneswalker()) {
                targetCard = ComputerUtilCard.getBestPlaneswalkerAI(killables);
            }
            if (targetCard == null) {
                targetCard = ComputerUtilCard.getBestCreatureAI(killables);
            }

            return targetCard;
        }

        if (!mandatory) {
            return null;
        }

        if (!hPlay.isEmpty()) {
            if (pl.isOpponentOf(ai)) {
                if (sa.getTargetRestrictions().canTgtPlaneswalker()) {
                    targetCard = ComputerUtilCard.getBestPlaneswalkerAI(hPlay);
                }
                if (targetCard == null) {
                    targetCard = ComputerUtilCard.getBestCreatureAI(hPlay);
                }
            } else {
                targetCard = ComputerUtilCard.getWorstCreatureAI(hPlay);
            }

            return targetCard;
        }

        return null;
    }

    /**
     * <p>
     * damageTargetAI.
     * </p>
     * 
     * @param saMe
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @param dmg
     *            a int.
     * @return a boolean.
     */
    private boolean damageTargetAI(final Player ai, final SpellAbility saMe, final int dmg, final boolean immediately) {
        final TargetRestrictions tgt = saMe.getTargetRestrictions();
        if ("Atarka's Command".equals(saMe.getHostCard().getName())) {
        	// playReusable in damageChooseNontargeted wrongly assumes that CharmEffect options are re-usable
        	return this.shouldTgtP(ai, saMe, dmg, false);
        }
        if (tgt == null) {
            return this.damageChooseNontargeted(ai, saMe, dmg);
        }

        if (tgt.isRandomTarget()) {
            return false;
        }

        return this.damageChoosingTargets(ai, saMe, tgt, dmg, false, immediately);
    }

    /**
     * <p>
     * damageChoosingTargets.
     * </p>
     * 
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @param tgt
     *            a {@link forge.game.spellability.TargetRestrictions} object.
     * @param dmg
     *            a int.
     * @param mandatory
     *            a boolean.
     * @return a boolean.
     */
    private boolean damageChoosingTargets(final Player ai, final SpellAbility sa, final TargetRestrictions tgt, int dmg,
            final boolean mandatory, boolean immediately) {
        final Card source = sa.getHostCard();
        final boolean noPrevention = sa.hasParam("NoPrevention");
        final Game game = source.getGame();
        final PhaseHandler phase = game.getPhaseHandler();
        final boolean divided = sa.hasParam("DividedAsYouChoose");
        final boolean oppTargetsChoice = sa.hasParam("TargetingPlayer");

        Player enemy = ai.getOpponent();
        
        if ("PowerDmg".equals(sa.getParam("AILogic"))) {
            // check if it is better to target the player instead, the original target is already set in PumpAi.pumpTgtAI()
            if (tgt.canTgtCreatureAndPlayer() && this.shouldTgtP(ai, sa, dmg, noPrevention)){
                sa.resetTargets();
                sa.getTargets().add(enemy);
            }
            return true;
        }

        if (tgt.getMaxTargets(source, sa) <= 0) {
            return false;
        }
        
        immediately |= ComputerUtil.playImmediately(ai, sa);

        sa.resetTargets();
        // target loop
        TargetChoices tcs = sa.getTargets();

        if ("ChoiceBurn".equals(sa.getParam("AILogic"))) {
            // do not waste burns on player if other choices are present
            if (this.shouldTgtP(ai, sa, dmg, noPrevention)) {
                tcs.add(enemy);
                return true;
            } else {
                return false;
            }
        }
        if ("Polukranos".equals(sa.getParam("AILogic"))) {
            int dmgTaken = 0;
            CardCollection humCreatures = ai.getOpponent().getCreaturesInPlay();
            Card lastTgt = null;
            humCreatures = CardLists.getTargetableCards(humCreatures, sa);
            ComputerUtilCard.sortByEvaluateCreature(humCreatures);
            // try to kill things without dying
            for (Card humanCreature : humCreatures) {
                if (FightAi.canKill(humanCreature, source, dmgTaken)) {
                    continue;
                }
                final int assignedDamage = ComputerUtilCombat.getEnoughDamageToKill(humanCreature, dmg, source, false, noPrevention);
                if (assignedDamage <= dmg 
                        && humanCreature.getShieldCount() == 0 && !ComputerUtil.canRegenerate(humanCreature.getController(), humanCreature)) {
                    tcs.add(humanCreature);
                    tgt.addDividedAllocation(humanCreature, assignedDamage);
                    lastTgt = humanCreature;
                    dmg -= assignedDamage;
                }
                if (!source.hasProtectionFrom(humanCreature)) {
                    dmgTaken += humanCreature.getNetPower();
                }
                if (dmg == 0) {
                    return true;
                }
            }
            if (dmg > 0 && lastTgt != null) {
                tgt.addDividedAllocation(lastTgt, tgt.getDividedValue(lastTgt) + dmg);
                dmg = 0;
                return true;
            }
            // get safe target to dump damage
            for (Card humanCreature : humCreatures) {
                if (FightAi.canKill(humanCreature, source, 0)) {
                    continue;
                }
                tcs.add(humanCreature);
                tgt.addDividedAllocation(humanCreature, dmg);
                dmg = 0;
                return true;
            }
        }
        while (tcs.getNumTargeted() < tgt.getMaxTargets(source, sa)) {
            if (oppTargetsChoice && sa.getActivatingPlayer().equals(ai) && !sa.isTrigger()) {
                // canPlayAI (sa activated by ai)
                Player targetingPlayer = AbilityUtils.getDefinedPlayers(source, sa.getParam("TargetingPlayer"), sa).get(0);
                sa.setTargetingPlayer(targetingPlayer);
                return targetingPlayer.getController().chooseTargetsFor(sa);
            }

            if (tgt.canTgtCreatureAndPlayer()) {

                if (this.shouldTgtP(ai, sa, dmg, noPrevention)) {
                    tcs.add(enemy);
                    if (divided) {
                        tgt.addDividedAllocation(enemy, dmg);
                        break;
                    }
                    continue;
                }
                if ("RoundedDown".equals(sa.getParam("DivideEvenly"))) {
                    dmg = dmg * sa.getTargets().getNumTargeted() / (sa.getTargets().getNumTargeted() +1);
                }

                final Card c = this.dealDamageChooseTgtC(ai, sa, dmg, noPrevention, enemy, false);
                if (c != null) {
                    //option to hold removal instead only applies for single targeted removal
                    if (sa.isSpell() && !divided && !immediately && tgt.getMaxTargets(sa.getHostCard(), sa) == 1) {
                        if (!ComputerUtilCard.useRemovalNow(sa, c, dmg, ZoneType.Graveyard)) {
                            return false;
                        }
                    }
                    tcs.add(c);
                    if (divided) {
                        final int assignedDamage = ComputerUtilCombat.getEnoughDamageToKill(c, dmg, source, false, noPrevention);
                        if (assignedDamage <= dmg) {
                            tgt.addDividedAllocation(c, assignedDamage);
                        }
                        dmg = dmg - assignedDamage;
                        if (dmg <= 0) {
                            break;
                        }
                    }
                    continue;
                }

                // When giving priority to targeting Creatures for mandatory
                // triggers
                // feel free to add the Human after we run out of good targets

                // TODO: add check here if card is about to die from something
                // on the stack
                // or from taking combat damage

                boolean freePing = immediately || sa.getPayCosts() == null
                        || sa.getTargets().getNumTargeted() > 0;

                if (!source.isSpell()) {
                    if (phase.is(PhaseType.END_OF_TURN) && sa.isAbility()) {
                        if (phase.getNextTurn().equals(ai))
                            freePing = true;
                    }

                    if (phase.is(PhaseType.MAIN2) && sa.isAbility()) {
                        if (sa.getRestrictions().isPwAbility()
                                || source.hasSVar("EndOfTurnLeavePlay"))
                            freePing = true;
                    }
                }

                if (freePing && sa.canTarget(enemy)) {
                    tcs.add(enemy);
                    if (divided) {
                        tgt.addDividedAllocation(enemy, dmg);
                        break;
                    }
                }
                
            } else if (tgt.canTgtCreature() || tgt.canTgtPlaneswalker()) {
                final Card c = this.dealDamageChooseTgtC(ai, sa, dmg, noPrevention, enemy, mandatory);
                if (c != null) {
                    //option to hold removal instead only applies for single targeted removal
                    if (!immediately && tgt.getMaxTargets(sa.getHostCard(), sa) == 1 && !divided) {
                        if (!ComputerUtilCard.useRemovalNow(sa, c, dmg, ZoneType.Graveyard)) {
                            return false;
                        }
                    }
                    tcs.add(c);
                    if (divided) {
                        final int assignedDamage = ComputerUtilCombat.getEnoughDamageToKill(c, dmg, source, false, noPrevention);
                        if (assignedDamage <= dmg) {
                            tgt.addDividedAllocation(c, assignedDamage);
                        } else {
                            tgt.addDividedAllocation(c, dmg);
                        }
                        dmg = dmg - assignedDamage;
                        if (dmg <= 0) {
                            break;
                        }
                    }
                    continue;
                }
            } else if ("OppAtTenLife".equals(sa.getParam("AILogic"))) {
            	for (final Player p : ai.getOpponents()) {
            		if (sa.canTarget(p) && p.getLife() == 10 && tcs.getNumTargeted() < tgt.getMaxTargets(source, sa)) {
            			tcs.add(p);
            		}
            	}
            }
            // TODO: Improve Damage, we shouldn't just target the player just
            // because we can
            else if (sa.canTarget(enemy)) {
                if ((phase.is(PhaseType.END_OF_TURN) && phase.getNextTurn().equals(ai))
                        || (SpellAbilityAi.isSorcerySpeed(sa) && phase.is(PhaseType.MAIN2))
                        || sa.getPayCosts() == null || immediately
                        || this.shouldTgtP(ai, sa, dmg, noPrevention)) {
                	tcs.add(enemy);
                    if (divided) {
                        tgt.addDividedAllocation(enemy, dmg);
                        break;
                    }
                    continue;
                }
            }
            // fell through all the choices, no targets left?
            if (tcs.getNumTargeted() < tgt.getMinTargets(source, sa) || tcs.getNumTargeted() == 0) {
                if (!mandatory) {
                    sa.resetTargets();
                    return false;
                } else {
                    // If the trigger is mandatory, gotta choose my own stuff now
                    return this.damageChooseRequiredTargets(ai, sa, tgt, dmg, mandatory);
                }
            } else {
                // TODO is this good enough? for up to amounts?
                break;
            }
        }
        return true;
    }

    /**
     * <p>
     * damageChooseNontargeted.
     * </p>
     * @param ai 
     * 
     * @param saMe
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @param dmg
     *            a int.
     * @return a boolean.
     */
    private boolean damageChooseNontargeted(Player ai, final SpellAbility saMe, final int dmg) {
        // TODO: Improve circumstances where the Defined Damage is unwanted
        final List<GameObject> objects = AbilityUtils.getDefinedObjects(saMe.getHostCard(), saMe.getParam("Defined"), saMe);
        boolean urgent = false; // can it wait?
        boolean positive = false;

        for (final Object o : objects) {
            if (o instanceof Card) {
                Card c = (Card) o;
                final int restDamage = ComputerUtilCombat.predictDamageTo(c, dmg, saMe.getHostCard(), false);
                if (!c.hasKeyword("Indestructible") && ComputerUtilCombat.getDamageToKill(c) <= restDamage) {
                    if (c.getController().equals(ai)) {
                        return false;
                    } else {
                        urgent = true;
                    }
                }
                if (c.getController().isOpponentOf(ai) ^ c.getName().equals("Stuffy Doll")) {
                    positive = true;
                }
            } else if (o instanceof Player) {
                final Player p = (Player) o;
                final int restDamage = ComputerUtilCombat.predictDamageTo(p, dmg, saMe.getHostCard(), false);
                if (!p.isOpponentOf(ai) && p.canLoseLife() && restDamage + 3 >= p.getLife() && restDamage > 0) {
                    // from this spell will kill me
                    return false;
                }
                if (p.isOpponentOf(ai) && p.canLoseLife()) {
                    positive = true;
                    if (p.getLife() + 3 <= restDamage) {
                        urgent = true;
                    }
                }
            }
        }
        if (!positive && !(saMe instanceof AbilitySub)) {
            return false;
        }
        if (!urgent && !SpellAbilityAi.playReusable(ai, saMe)) {
            return false;
        }
        return true;
    }

    /**
     * <p>
     * damageChooseRequiredTargets.
     * </p>
     * 
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @param tgt
     *            a {@link forge.game.spellability.TargetRestrictions} object.
     * @param dmg
     *            a int.
     * @param mandatory
     *            a boolean.
     * @return a boolean.
     */
    private boolean damageChooseRequiredTargets(final Player ai, final SpellAbility sa, final TargetRestrictions tgt, final int dmg,
            final boolean mandatory) {
        // this is for Triggered targets that are mandatory
        final boolean noPrevention = sa.hasParam("NoPrevention");
        final boolean divided = sa.hasParam("DividedAsYouChoose");
        final Player opp = ai.getOpponent();
        System.out.println("damageChooseRequiredTargets " + ai + " " + sa);

        while (sa.getTargets().getNumTargeted() < tgt.getMinTargets(sa.getHostCard(), sa)) {
            // TODO: Consider targeting the planeswalker
            if (tgt.canTgtCreature()) {
                final Card c = this.dealDamageChooseTgtC(ai, sa, dmg, noPrevention, ai, mandatory);
                if (c != null) {
                    sa.getTargets().add(c);
                    if (divided) {
                        tgt.addDividedAllocation(c, dmg);
                        break;
                    }
                    continue;
                }
            }

            if (sa.canTarget(opp)) {
                if (sa.getTargets().add(opp)) {
                    if (divided) {
                        tgt.addDividedAllocation(opp, dmg);
                        break;
                    }
                    continue;
                }
            }

            if (sa.canTarget(ai)) {
                if (sa.getTargets().add(ai)) {
                    if (divided) {
                        tgt.addDividedAllocation(ai, dmg);
                        break;
                    }
                    continue;
                }
            }

            // if we get here then there isn't enough targets, this is the only
            // time we can return false
            return false;
        }
        return true;
    }

    @Override
    protected boolean doTriggerAINoCost(Player ai, SpellAbility sa, boolean mandatory) {

        final Card source = sa.getHostCard();
        final String damage = sa.getParam("NumDmg");
        int dmg = AbilityUtils.calculateAmount(source, damage, sa);

        // Remove all damage
        if (sa.hasParam("Remove")) {
            return true;
        }

        if (damage.equals("X") && sa.getSVar(damage).equals("Count$xPaid")) {
            // Set PayX here to maximum value.
            dmg = ComputerUtilMana.determineLeftoverMana(sa, ai);
            source.setSVar("PayX", Integer.toString(dmg));
        }

        final TargetRestrictions tgt = sa.getTargetRestrictions();
        if (tgt == null) {
            // If it's not mandatory check a few things
            if (!mandatory && !this.damageChooseNontargeted(ai, sa, dmg)) {
                return false;
            }
        } else {
            if (!this.damageChoosingTargets(ai, sa, tgt, dmg, mandatory, true) && !mandatory) {
                return false;
            }

            if (damage.equals("X") && source.getSVar(damage).equals("Count$xPaid") && !sa.hasParam("DividedAsYouChoose")) {
                // If I can kill my target by paying less mana, do it
                int actualPay = 0;
                final boolean noPrevention = sa.hasParam("NoPrevention");
                
                //target is a player
                if (!sa.getTargets().isTargetingAnyCard()) {
                    actualPay = dmg;
                }
                for (final Card c : sa.getTargets().getTargetCards()) {
                    final int adjDamage = ComputerUtilCombat.getEnoughDamageToKill(c, dmg, source, false, noPrevention);
                    if (adjDamage > actualPay) {
                        actualPay = adjDamage;
                    }
                }

                source.setSVar("PayX", Integer.toString(actualPay));
            }
        }

        return true;
    }
}
