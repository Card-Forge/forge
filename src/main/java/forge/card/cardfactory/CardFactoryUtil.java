/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.card.cardfactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import forge.Card;
import forge.CardCharacteristicName;
import forge.CardLists;
import forge.CardPredicates;
import forge.CardPredicates.Presets;
import forge.CardUtil;
import forge.Command;
import forge.Constant;
import forge.CounterType;
import forge.FThreads;
import forge.GameEntity;
import forge.Singletons;
import forge.card.ability.AbilityFactory;
import forge.card.ability.AbilityUtils;
import forge.card.ability.ApiType;
import forge.card.cost.Cost;
import forge.card.mana.ManaCost;
import forge.card.mana.ManaCostParser;
import forge.card.replacement.ReplacementEffect;
import forge.card.replacement.ReplacementHandler;
import forge.card.replacement.ReplacementLayer;
import forge.card.spellability.Ability;
import forge.card.spellability.AbilityActivated;
import forge.card.spellability.AbilityStatic;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.OptionalCost;
import forge.card.spellability.Spell;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.SpellAbilityRestriction;
import forge.card.spellability.SpellPermanent;
import forge.card.spellability.Target;
import forge.card.trigger.Trigger;
import forge.card.trigger.TriggerHandler;
import forge.card.trigger.TriggerType;
import forge.control.input.InputSelectCards;
import forge.control.input.InputSelectCardsFromList;
import forge.game.ai.ComputerUtilCard;
import forge.game.ai.ComputerUtilCost;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.AIPlayer;
import forge.game.player.Player;
import forge.game.zone.PlayerZone;
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;
import forge.gui.GuiChoose;
import forge.util.Aggregates;

/**
 * <p>
 * CardFactoryUtil class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class CardFactoryUtil {

    /**
     * <p>
     * abilityUnearth.
     * </p>
     * 
     * @param sourceCard
     *            a {@link forge.Card} object.
     * @param manaCost
     *            a {@link java.lang.String} object.
     * @return a {@link forge.card.spellability.AbilityActivated} object.
     */
    public static AbilityActivated abilityUnearth(final Card sourceCard, final String manaCost) {

        final Cost cost = new Cost(manaCost, true);
        class AbilityUnearth extends AbilityActivated {
            public AbilityUnearth(final Card ca, final Cost co, final Target t) {
                super(ca, co, t);
            }

            @Override
            public AbilityActivated getCopy() {
                AbilityActivated res = new AbilityUnearth(getSourceCard(),
                        getPayCosts(), getTarget() == null ? null : new Target(getTarget()));
                CardFactory.copySpellAbility(this, res);
                final SpellAbilityRestriction restrict = new SpellAbilityRestriction();
                restrict.setZone(ZoneType.Graveyard);
                restrict.setSorcerySpeed(true);
                res.setRestrictions(restrict);
                return res;
            }

            private static final long serialVersionUID = -5633945565395478009L;

            @Override
            public void resolve() {
                final Card card = Singletons.getModel().getGame().getAction().moveToPlay(sourceCard);

                card.addIntrinsicKeyword("At the beginning of the end step, exile CARDNAME.");
                card.addIntrinsicKeyword("Haste");
                card.setUnearthed(true);
            }

            @Override
            public boolean canPlayAI() {
                PhaseHandler phase = Singletons.getModel().getGame().getPhaseHandler();
                if (phase.getPhase().isAfter(PhaseType.MAIN1) || !phase.isPlayerTurn(getActivatingPlayer())) {
                    return false;
                }
                return ComputerUtilCost.canPayCost(this, getActivatingPlayer());
            }
        }
        final AbilityActivated unearth = new AbilityUnearth(sourceCard, cost, null);

        final SpellAbilityRestriction restrict = new SpellAbilityRestriction();
        restrict.setZone(ZoneType.Graveyard);
        restrict.setSorcerySpeed(true);
        unearth.setRestrictions(restrict);

        final StringBuilder sbStack = new StringBuilder();
        sbStack.append("Unearth: ").append(sourceCard.getName());
        unearth.setStackDescription(sbStack.toString());

        return unearth;
    } // abilityUnearth()

    /**
     * <p>
     * abilityMorphDown.
     * </p>
     * 
     * @param sourceCard
     *            a {@link forge.Card} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility abilityMorphDown(final Card sourceCard) {
        final Spell morphDown = new Spell(sourceCard, new Cost(ManaCost.THREE, false)) {
            private static final long serialVersionUID = -1438810964807867610L;

            @Override
            public void resolve() {
                Card c = Singletons.getModel().getGame().getAction().moveToPlay(sourceCard);
                c.setPreFaceDownCharacteristic(CardCharacteristicName.Original);
            }

            @Override
            public boolean canPlay() {
                //Lands do not have SpellPermanents.
                if (sourceCard.isLand()) {
                    return (Singletons.getModel().getGame().getZoneOf(sourceCard).is(ZoneType.Hand) || sourceCard.hasKeyword("May be played"))
                            && sourceCard.getController().canCastSorcery();
                }
                else {
                    return sourceCard.getSpellPermanent().canPlay();
                }
            }
        };

        morphDown.setDescription("(You may cast this face down as a 2/2 creature for 3.)");
        morphDown.setStackDescription("Morph - Creature 2/2");
        morphDown.setCastFaceDown(true);

        return morphDown;
    }

    /**
     * <p>
     * abilityMorphUp.
     * </p>
     * 
     * @param sourceCard
     *            a {@link forge.Card} object.
     * @param cost
     *            a {@link forge.card.cost.Cost} object.
     * @return a {@link forge.card.spellability.AbilityActivated} object.
     */
    public static AbilityStatic abilityMorphUp(final Card sourceCard, final Cost cost) {
        final AbilityStatic morphUp = new AbilityStatic(sourceCard, cost, null) {

            @Override
            public void resolve() {
                if (sourceCard.turnFaceUp()) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(this.getActivatingPlayer()).append(" has unmorphed ");
                    sb.append(sourceCard.getName());
                    Singletons.getModel().getGame().getGameLog().add("ResolveStack", sb.toString(), 2);
                }
            }

            @Override
            public boolean canPlay() {
                return sourceCard.getController().equals(this.getActivatingPlayer()) && sourceCard.isFaceDown()
                        && sourceCard.isInPlay();
            }

        }; // morph_up

        String costDesc = cost.toString();
        // get rid of the ": " at the end
        costDesc = costDesc.substring(0, costDesc.length() - 2);
        final StringBuilder sb = new StringBuilder();
        sb.append("Morph");
        if (!cost.isOnlyManaCost()) {
            sb.append(" -");
        }
        sb.append(" ").append(costDesc).append(" (Turn this face up any time for its morph cost.)");
        morphUp.setDescription(sb.toString());

        final StringBuilder sbStack = new StringBuilder();
        sbStack.append(sourceCard.getName()).append(" - turn this card face up.");
        morphUp.setStackDescription(sbStack.toString());

        return morphUp;
    }

    /**
     * <p>
     * abilityCycle.
     * </p>
     * 
     * @param sourceCard
     *            a {@link forge.Card} object.
     * @param cycleCost
     *            a {@link java.lang.String} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility abilityCycle(final Card sourceCard, String cycleCost) {
        StringBuilder sb = new StringBuilder();
        sb.append("AB$ Draw | Cost$ ");
        sb.append(cycleCost);
        sb.append(" Discard<1/CARDNAME> | ActivationZone$ Hand | PrecostDesc$ Cycling ");
        sb.append("| SpellDescription$ Draw a card.");

        SpellAbility cycle = AbilityFactory.getAbility(sb.toString(), sourceCard);
        cycle.setIsCycling(true);

        return cycle;
    } // abilityCycle()

    /**
     * <p>
     * abilityTypecycle.
     * </p>
     * 
     * @param sourceCard
     *            a {@link forge.Card} object.
     * @param cycleCost
     *            a {@link java.lang.String} object.
     * @param type
     *            a {@link java.lang.String} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility abilityTypecycle(final Card sourceCard, String cycleCost, final String type) {
        StringBuilder sb = new StringBuilder();
        sb.append("AB$ ChangeZone | Cost$ ").append(cycleCost);

        String desc = type;
        if (type.equals("Basic")) {
            desc = "Basic land";
        }

        sb.append(" Discard<1/CARDNAME> | ActivationZone$ Hand | PrecostDesc$ ").append(desc).append("cycling ");
        sb.append("| Origin$ Library | Destination$ Hand |");
        sb.append("ChangeType$ ").append(type);
        sb.append(" | SpellDescription$ Search your library for a ").append(desc).append(" card, reveal it,");
        sb.append(" and put it into your hand. Then shuffle your library.");

        SpellAbility cycle = AbilityFactory.getAbility(sb.toString(), sourceCard);
        cycle.setIsCycling(true);

        return cycle;
    } // abilityTypecycle()

    /**
     * <p>
     * abilityTransmute.
     * </p>
     * 
     * @param sourceCard
     *            a {@link forge.Card} object.
     * @param transmuteCost
     *            a {@link java.lang.String} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility abilityTransmute(final Card sourceCard, String transmuteCost) {
        transmuteCost += " Discard<1/CARDNAME>";
        final Cost abCost = new Cost(transmuteCost, true);
        class AbilityTransmute extends AbilityActivated {
            public AbilityTransmute(final Card ca, final Cost co, final Target t) {
                super(ca, co, t);
            }

            @Override
            public AbilityActivated getCopy() {
                AbilityActivated res = new AbilityTransmute(getSourceCard(),
                        getPayCosts(), getTarget() == null ? null : new Target(getTarget()));
                CardFactory.copySpellAbility(this, res);
                res.getRestrictions().setZone(ZoneType.Hand);
                return res;
            }

            private static final long serialVersionUID = -4960704261761785512L;

            @Override
            public boolean canPlayAI() {
                return false;
            }

            @Override
            public boolean canPlay() {
                return super.canPlay() && sourceCard.getController().canCastSorcery();
            }

            @Override
            public void resolve() {
                final List<Card> cards = sourceCard.getController().getCardsIn(ZoneType.Library);
                final List<Card> sameCost = new ArrayList<Card>();

                for (Card c : cards) {
                    if (c.isSplitCard() && c.getCurState() == CardCharacteristicName.Original) {
                        if (c.getState(CardCharacteristicName.LeftSplit).getManaCost().getCMC() == sourceCard.getManaCost().getCMC() ||
                                c.getState(CardCharacteristicName.RightSplit).getManaCost().getCMC() == sourceCard.getManaCost().getCMC()) {
                            sameCost.add(c);
                        }
                    }
                    else if (c.getManaCost().getCMC() == sourceCard.getManaCost().getCMC()) {
                        sameCost.add(c);
                    }
                }

                if (sameCost.size() == 0) {
                    return;
                }

                final Card o = GuiChoose.oneOrNone("Select a card", sameCost);
                if (o != null) {
                    // ability.setTargetCard((Card)o);

                    sourceCard.getController().discard(sourceCard, this);
                    final Card c1 = o;

                    Singletons.getModel().getGame().getAction().moveToHand(c1);

                }
                sourceCard.getController().shuffle();
            }
        }
        final SpellAbility transmute = new AbilityTransmute(sourceCard, abCost, null);

        final StringBuilder sbDesc = new StringBuilder();
        sbDesc.append("Transmute (").append(abCost.toString());
        sbDesc.append("Search your library for a card with the same converted mana cost as this card, reveal it, ");
        sbDesc.append("and put it into your hand. Then shuffle your library. Transmute only as a sorcery.)");
        transmute.setDescription(sbDesc.toString());

        final StringBuilder sbStack = new StringBuilder();
        sbStack.append(sourceCard).append(" Transmute: Search your library ");
        sbStack.append("for a card with the same converted mana cost.)");
        transmute.setStackDescription(sbStack.toString());

        transmute.getRestrictions().setZone(ZoneType.Hand);
        return transmute;
    } // abilityTransmute()

    /**
     * <p>
     * abilitySuspend.
     * </p>
     * 
     * @param sourceCard
     *            a {@link forge.Card} object.
     * @param suspendCost
     *            a {@link java.lang.String} object.
     * @param timeCounters
     *            a int.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility abilitySuspend(final Card sourceCard, final String suspendCost, final String timeCounters) {
        // be careful with Suspend ability, it will not hit the stack
        Cost cost = new Cost(suspendCost, true);
        final SpellAbility suspend = new AbilityStatic(sourceCard, cost, null) {
            @Override
            public boolean canPlay() {
                if (!(this.getRestrictions().canPlay(sourceCard, this))) {
                    return false;
                }

                if (sourceCard.isInstant() || sourceCard.hasKeyword("Flash")) {
                    return true;
                }

                return sourceCard.getOwner().canCastSorcery();
            }

            @Override
            public boolean canPlayAI() {
                return true;
                // Suspend currently not functional for the AI,
                // seems to be an issue with regaining Priority after Suspension
            }

            @Override
            public void resolve() {
                final Card c = Singletons.getModel().getGame().getAction().exile(sourceCard);

                int counters = AbilityUtils.calculateAmount(c, timeCounters, this);
                c.addCounter(CounterType.TIME, counters, true);

                StringBuilder sb = new StringBuilder();
                sb.append(this.getActivatingPlayer()).append(" has suspended ");
                sb.append(c.getName()).append("with ");
                sb.append(counters).append(" time counters on it.");
                Singletons.getModel().getGame().getGameLog().add("ResolveStack", sb.toString(), 2);
            }
        };
        final StringBuilder sbDesc = new StringBuilder();
        sbDesc.append("Suspend ").append(timeCounters).append(" - ").append(cost.toSimpleString());
        suspend.setDescription(sbDesc.toString());

        String svar = "X"; // emulate "References X" here
        suspend.setSVar(svar, sourceCard.getSVar(svar));

        final StringBuilder sbStack = new StringBuilder();
        sbStack.append(sourceCard.getName()).append(" suspending for ").append(timeCounters).append(" turns.)");
        suspend.setStackDescription(sbStack.toString());

        suspend.getRestrictions().setZone(ZoneType.Hand);
        return suspend;
    } // abilitySuspend()

    /**
     * <p>
     * entersBattleFieldWithCounters.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @param type
     *            a {@link forge.CounterType} object.
     * @param n
     *            a int.
     * @return a {@link forge.Command} object.
     */
    public static Command entersBattleFieldWithCounters(final Card c, final CounterType type, final int n) {
        final Command addCounters = new Command() {
            private static final long serialVersionUID = 4825430555490333062L;

            @Override
            public void run() {
                c.addCounter(type, n, true);
            }
        };
        return addCounters;
    }

    /**
     * <p>
     * fading.
     * </p>
     * 
     * @param sourceCard
     *            a {@link forge.Card} object.
     * @param power
     *            a int.
     * @return a {@link forge.Command} object.
     */
    public static Command fading(final Card sourceCard, final int power) {
        return entersBattleFieldWithCounters(sourceCard, CounterType.FADE, power);
    } // fading

    /**
     * <p>
     * vanishing.
     * </p>
     * 
     * @param sourceCard
     *            a {@link forge.Card} object.
     * @param power
     *            a int.
     * @return a {@link forge.Command} object.
     */
    public static Command vanishing(final Card sourceCard, final int power) {
        return entersBattleFieldWithCounters(sourceCard, CounterType.TIME, power);
    } // vanishing

    /**
     * <p>
     * getNumberOfManaSymbolsControlledByColor.
     * </p>
     * 
     * @param colorAbb
     *            a {@link java.lang.String} object.
     * @param player
     *            a {@link forge.game.player.Player} object.
     * @return a int.
     */
    public static int getNumberOfManaSymbolsControlledByColor(final String colorAbb, final Player player) {
        final List<Card> cards = player.getCardsIn(ZoneType.Battlefield);
        return getNumberOfManaSymbolsByColor(colorAbb, cards);
    }

    /**
     * <p>
     * getNumberOfManaSymbolsByColor.
     * </p>
     * 
     * @param colorAbb
     *            a {@link java.lang.String} object.
     * @param cards
     *            a {@link forge.List<Card>} object.
     * @return a int.
     */
    public static int getNumberOfManaSymbolsByColor(final String colorAbb, final List<Card> cards) {
        int count = 0;
        for (Card c : cards) {
            // Certain tokens can have mana cost, so don't skip them
            count += getNumberOfManaSymbolsByColor(colorAbb, c);
        }
        return count;
    }

    /**
     * <p>
     * getNumberOfManaSymbolsByColor.
     * </p>
     * 
     * @param colorAbb
     *            a {@link java.lang.String} object.
     * @param card
     *            a {@link forge.Card} object.
     * @return a int.
     */
    public static int getNumberOfManaSymbolsByColor(final String colorAbb, final Card card) {
        return countOccurrences(card.getManaCost().toString().trim(), colorAbb);
    }

    /**
     * <p>
     * multiplyCost.
     * </p>
     * 
     * @param manacost
     *            a {@link java.lang.String} object.
     * @param multiplier
     *            a int.
     * @return a {@link java.lang.String} object.
     */
    public static String multiplyCost(final String manacost, final int multiplier) {
        if (multiplier == 0) {
            return "";
        }
        if (multiplier == 1) {
            return manacost;
        }

        final String[] tokenized = manacost.split("\\s");
        final StringBuilder sb = new StringBuilder();

        if (Character.isDigit(tokenized[0].charAt(0))) {
            // cost starts with "colorless" number cost
            int cost = Integer.parseInt(tokenized[0]);
            cost = multiplier * cost;
            tokenized[0] = "" + cost;
            sb.append(tokenized[0]);
        } else {
            if (tokenized[0].contains("<")) {
                final String[] advCostPart = tokenized[0].split("<");
                final String costVariable = advCostPart[1].split(">")[0];
                final String[] advCostPartValid = costVariable.split("\\/", 2);
                // multiply the number part of the cost object
                int num = Integer.parseInt(advCostPartValid[0]);
                num = multiplier * num;
                tokenized[0] = advCostPart[0] + "<" + num;
                if (advCostPartValid.length > 1) {
                    tokenized[0] = tokenized[0] + "/" + advCostPartValid[1];
                }
                tokenized[0] = tokenized[0] + ">";
                sb.append(tokenized[0]);
            } else {
                for (int i = 0; i < multiplier; i++) {
                    // tokenized[0] = tokenized[0] + " " + tokenized[0];
                    sb.append((" "));
                    sb.append(tokenized[0]);
                }
            }
        }

        for (int i = 1; i < tokenized.length; i++) {
            if (tokenized[i].contains("<")) {
                final String[] advCostParts = tokenized[i].split("<");
                final String costVariables = advCostParts[1].split(">")[0];
                final String[] advCostPartsValid = costVariables.split("\\/", 2);
                // multiply the number part of the cost object
                int num = Integer.parseInt(advCostPartsValid[0]);
                num = multiplier * num;
                tokenized[i] = advCostParts[0] + "<" + num;
                if (advCostPartsValid.length > 1) {
                    tokenized[i] = tokenized[i] + "/" + advCostPartsValid[1];
                }
                tokenized[i] = tokenized[i] + ">";
                sb.append((" "));
                sb.append(tokenized[i]);
            } else {
                for (int j = 0; j < multiplier; j++) {
                    // tokenized[i] = tokenized[i] + " " + tokenized[i];
                    sb.append((" "));
                    sb.append(tokenized[i]);
                }
            }
        }

        String result = sb.toString();
        System.out.println("result: " + result);
        result = result.trim();
        return result;
    }

    /**
     * <p>
     * isTargetStillValid.
     * </p>
     * 
     * @param ability
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param target
     *            a {@link forge.Card} object.
     * @return a boolean.
     */
    public static boolean isTargetStillValid(final SpellAbility ability, final Card target) {

        if (Singletons.getModel().getGame().getZoneOf(target) == null) {
            return false; // for tokens that disappeared
        }

        final Card source = ability.getSourceCard();
        final Target tgt = ability.getTarget();
        if (tgt != null) {
            // Reconfirm the Validity of a TgtValid, or if the Creature is still
            // a Creature
            if (tgt.doesTarget()
                    && !target.isValid(tgt.getValidTgts(), ability.getActivatingPlayer(), ability.getSourceCard())) {
                return false;
            }

            // Check if the target is in the zone it needs to be in to be
            // targeted
            if (!Singletons.getModel().getGame().getZoneOf(target).is(tgt.getZone())) {
                return false;
            }
        } else {
            // If an Aura's target is removed before it resolves, the Aura
            // fizzles
            if (source.isAura() && !target.isInZone(ZoneType.Battlefield)) {
                return false;
            }
        }

        // Make sure it's still targetable as well
        return target.canBeTargetedBy(ability);
    }

    // does "target" have protection from "card"?
    /**
     * <p>
     * hasProtectionFrom.
     * </p>
     * 
     * @param card
     *            a {@link forge.Card} object.
     * @param target
     *            a {@link forge.Card} object.
     * @return a boolean.
     */
    public static boolean hasProtectionFrom(final Card card, final Card target) {
        if (target == null) {
            return false;
        }

        return target.hasProtectionFrom(card);
    }

    /**
     * <p>
     * isCounterable.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @return a boolean.
     */
    public static boolean isCounterable(final Card c) {
        if (c.hasKeyword("CARDNAME can't be countered.") || !c.getCanCounter()) {
            return false;
        }

        return true;
    }

    /**
     * <p>
     * isCounterableBy.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @param sa
     *            the sa
     * @return a boolean.
     */
    public static boolean isCounterableBy(final Card c, final SpellAbility sa) {
        if (!isCounterable(c)) {
            return false;
        }
        //TODO: Add code for Autumn's Veil here

        return true;
    }

    /**
     * <p>
     * getExternalZoneActivationCards.
     * </p>
     * 
     * @param activator
     *            a {@link forge.game.player.Player} object.
     * @return a {@link forge.CardList} object.
     */
    public static List<Card> getExternalZoneActivationCards(final Player activator) {
        final List<Card> cl = new ArrayList<Card>();

        cl.addAll(getActivateablesFromZone(activator.getZone(ZoneType.Graveyard), activator));
        cl.addAll(getActivateablesFromZone(activator.getZone(ZoneType.Exile), activator));
        cl.addAll(getActivateablesFromZone(activator.getZone(ZoneType.Library), activator));
        cl.addAll(getActivateablesFromZone(activator.getZone(ZoneType.Command), activator));

        //External activatables from all opponents
        for (final Player opponent : activator.getOpponents()) {
            cl.addAll(getActivateablesFromZone(opponent.getZone(ZoneType.Exile), activator));
            cl.addAll(getActivateablesFromZone(opponent.getZone(ZoneType.Graveyard), activator));
            cl.addAll(getActivateablesFromZone(opponent.getZone(ZoneType.Library), activator));
            if (opponent.hasKeyword("Play with your hand revealed.")) {
                cl.addAll(getActivateablesFromZone(opponent.getZone(ZoneType.Hand), activator));
            }
        }

        return cl;
    }

    /**
     * <p>
     * getActivateablesFromZone.
     * </p>
     * 
     * @param zone
     *            a PlayerZone object.
     * @param activator
     *            a {@link forge.game.player.Player} object.
     * @return a boolean.
     */
    public static List<Card> getActivateablesFromZone(final PlayerZone zone, final Player activator) {

        Iterable<Card> cl = zone.getCards();

        // Only check the top card of the library
        if (zone.is(ZoneType.Library)) {
            cl = Iterables.limit(cl, 1);
        }

        if (activator.equals(zone.getPlayer())) {
            cl = Iterables.filter(cl, new Predicate<Card>() {
                @Override
                public boolean apply(final Card c) {
                    if (c.hasKeyword("You may look at this card.")) {
                        return true;
                    }

                    if (c.isLand()
                            && (c.hasKeyword("May be played") || c.hasKeyword("May be played without paying its mana cost"))) {
                        return true;
                    }

                    for (final SpellAbility sa : c.getSpellAbilities()) {
                        final ZoneType restrictZone = sa.getRestrictions().getZone();
                        if (zone.is(restrictZone)) {
                            return true;
                        }

                        if (sa.isSpell()
                                && (c.hasKeyword("May be played") || c.hasKeyword("May be played without paying its mana cost")
                                        || (c.hasStartOfKeyword("Flashback") && zone.is(ZoneType.Graveyard)))
                                && restrictZone.equals(ZoneType.Hand)) {
                            return true;
                        }
                    }
                    return false;
                }
            });
        } else {
            // the activator is not the owner of the card
            cl = Iterables.filter(cl, new Predicate<Card>() {
                @Override
                public boolean apply(final Card c) {

                    if (c.hasStartOfKeyword("May be played by your opponent")
                            || c.hasKeyword("Your opponent may look at this card.")) {
                        return true;
                    }
                    return false;
                }
            });
        }
        return Lists.newArrayList(cl);
    }

    /**
     * <p>
     * countOccurrences.
     * </p>
     * 
     * @param arg1
     *            a {@link java.lang.String} object.
     * @param arg2
     *            a {@link java.lang.String} object.
     * @return a int.
     */
    public static int countOccurrences(final String arg1, final String arg2) {

        int count = 0;
        int index = 0;
        while ((index = arg1.indexOf(arg2, index)) != -1) {
            ++index;
            ++count;
        }
        return count;
    }

    /**
     * <p>
     * parseMath.
     * </p>
     * 
     * @param l
     *            an array of {@link java.lang.String} objects.
     * @return an array of {@link java.lang.String} objects.
     */
    public static String extractOperators(final String expression) {
        String[] l = expression.split("/");
        return l.length > 1 ? l[1] : null;
    }

    /**
     * <p>
     * Parse player targeted X variables.
     * </p>
     * 
     * @param players
     *            a {@link java.util.ArrayList} object.
     * @param s
     *            a {@link java.lang.String} object.
     * @param source
     *            a {@link forge.Card} object.
     * @return a int.
     */
    public static int objectXCount(final ArrayList<Object> objects, final String s, final Card source) {
        if (objects.isEmpty()) {
            return 0;
        }

        int n = s.startsWith("Amount") ? objects.size() : 0;
        return doXMath(n, extractOperators(s), source);
    }

    /**
     * <p>
     * Parse player targeted X variables.
     * </p>
     * 
     * @param players
     *            a {@link java.util.ArrayList} object.
     * @param s
     *            a {@link java.lang.String} object.
     * @param source
     *            a {@link forge.Card} object.
     * @return a int.
     */
    public static int playerXCount(final List<Player> players, final String s, final Card source) {
        if (players.size() == 0) {
            return 0;
        }

        final String[] l = s.split("/");
        final String m = extractOperators(s);

        int n = 0;

        // methods for getting the highest/lowest playerXCount from a range of players
        if (l[0].startsWith("Highest")) {
            for (final Player player : players) {
                final ArrayList<Player> temp = new ArrayList<Player>();
                temp.add(player);
                final int current = playerXCount(temp, s.replace("Highest", ""), source);
                if (current > n) {
                    n = current;
                }
            }

            return doXMath(n, m, source);
        } else if (l[0].startsWith("Lowest")) {
            n = 99999; // if no players have fewer than 99999 valids, the game is frozen anyway
            for (final Player player : players) {
                final ArrayList<Player> temp = new ArrayList<Player>();
                temp.add(player);
                final int current = playerXCount(temp, s.replace("Lowest", ""), source);
                if (current < n) {
                    n = current;
                }
            }

            return doXMath(n, m, source);
        }

        // count valid cards in any specified zone/s
        if (l[0].startsWith("Valid") && !l[0].contains("Valid ")) {
            String[] lparts = l[0].split(" ", 2);
            final List<ZoneType> vZone = ZoneType.listValueOf(lparts[0].split("Valid")[1]);
            String restrictions = l[0].replace(lparts[0] + " ", "");
            final String[] rest = restrictions.split(",");
            List<Card> cards = Singletons.getModel().getGame().getCardsIn(vZone);
            cards = CardLists.getValidCards(cards, rest, players.get(0), source);

            n = cards.size();

            return doXMath(n, m, source);
        }
        // count valid cards on the battlefield
        if (l[0].startsWith("Valid ")) {
            final String restrictions = l[0].substring(6);
            final String[] rest = restrictions.split(",");
            List<Card> cardsonbattlefield = Singletons.getModel().getGame().getCardsIn(ZoneType.Battlefield);
            cardsonbattlefield = CardLists.getValidCards(cardsonbattlefield, rest, players.get(0), source);

            n = cardsonbattlefield.size();

            return doXMath(n, m, source);
        }

        final String[] sq;
        sq = l[0].split("\\.");

        // the number of players passed in
        if (sq[0].equals("Amount")) {
            return doXMath(players.size(), m, source);
        }

        if (sq[0].contains("CardsInHand")) {
            if (players.size() > 0) {
                return doXMath(players.get(0).getCardsIn(ZoneType.Hand).size(), m, source);
            }
        }

        if (sq[0].contains("DomainPlayer")) {
            final List<Card> someCards = new ArrayList<Card>();
            someCards.addAll(players.get(0).getCardsIn(ZoneType.Battlefield));
            final String[] basic = { "Forest", "Plains", "Mountain", "Island", "Swamp" };

            for (int i = 0; i < basic.length; i++) {
                if (!CardLists.getType(someCards, basic[i]).isEmpty()) {
                    n++;
                }
            }
            return doXMath(n, m, source);
        }

        if (sq[0].contains("CardsInLibrary")) {
            if (players.size() > 0) {
                return doXMath(players.get(0).getCardsIn(ZoneType.Library).size(), m, source);
            }
        }

        if (sq[0].contains("CardsInGraveyard")) {
            if (players.size() > 0) {
                return doXMath(players.get(0).getCardsIn(ZoneType.Graveyard).size(), m, source);
            }
        }
        if (sq[0].contains("LandsInGraveyard")) {
            if (players.size() > 0) {
                return doXMath(CardLists.getType(players.get(0).getCardsIn(ZoneType.Graveyard), "Land").size(), m,
                        source);
            }
        }

        if (sq[0].contains("CreaturesInPlay")) {
            if (players.size() > 0) {
                return doXMath(players.get(0).getCreaturesInPlay().size(), m, source);
            }
        }

        if (sq[0].contains("CardsInPlay")) {
            if (players.size() > 0) {
                return doXMath(players.get(0).getCardsIn(ZoneType.Battlefield).size(), m, source);
            }
        }

        if (sq[0].contains("LifeTotal")) {
            if (players.size() > 0) {
                return doXMath(players.get(0).getLife(), m, source);
            }
        }

        if (sq[0].contains("LifeLostThisTurn")) {
            if (players.size() > 0) {
                return doXMath(players.get(0).getLifeLostThisTurn(), m, source);
            }
        }

        if (sq[0].contains("PoisonCounters")) {
            if (players.size() > 0) {
                return doXMath(players.get(0).getPoisonCounters(), m, source);
            }
        }

        if (sq[0].contains("TopOfLibraryCMC")) {
            if (players.size() > 0) {
                return doXMath(Aggregates.sum(players.get(0).getCardsIn(ZoneType.Library, 1), CardPredicates.Accessors.fnGetCmc),
                        m, source);
            }
        }

        if (sq[0].contains("LandsPlayed")) {
            if (players.size() > 0) {
                return doXMath(players.get(0).getNumLandsPlayed(), m, source);
            }
        }

        if (sq[0].contains("CardsDrawn")) {
            if (players.size() > 0) {
                return doXMath(players.get(0).getNumDrawnThisTurn(), m, source);
            }
        }
        
        if (sq[0].contains("CardsDiscardedThisTurn")) {
            if (players.size() > 0) {
                return doXMath(players.get(0).getNumDiscardedThisTurn(), m, source);
            }
        }

        if (sq[0].contains("AttackersDeclared")) {
            if (players.size() > 0) {
                return doXMath(players.get(0).getAttackersDeclaredThisTurn(), m, source);
            }
        }

        if (sq[0].equals("DamageDoneToPlayerBy")) {
            if (players.size() > 0) {
                return doXMath(source.getDamageDoneToPlayerBy(players.get(0).getName()), m, source);
            }
        }

        if (sq[0].contains("DamageToOppsThisTurn")) {
            if (players.size() > 0) {
                int oppDmg = 0;
                for (Player opp : players.get(0).getOpponents()) {
                    oppDmg += opp.getAssignedDamage();
                }
                return doXMath(oppDmg, m, source);
            }
        }

        if (sq[0].contains("DamageThisTurn")) {
            if (players.size() > 0) {
                int totDmg = 0;
                for (Player p : players) {
                    totDmg += p.getAssignedDamage();
                }
                return doXMath(totDmg, m, source);
            }
        }

        return doXMath(n, m, source);
    }

    /**
     * <p>
     * Parse non-mana X variables.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @param expression
     *            a {@link java.lang.String} object.
     * @return a int.
     */
    public static int xCount(final Card c, final String expression) {
        if (StringUtils.isBlank(expression)) return 0;
        if (StringUtils.isNumeric(expression)) return Integer.parseInt(expression);

        final Player cc = c.getController();
        final Player ccOpp = cc.getOpponent();
        final Player activePlayer = cc.getGame().getPhaseHandler().getPlayerTurn();

        final String[] l = expression.split("/");
        final String m = extractOperators(expression);

        // accept straight numbers
        if (l[0].startsWith("Number$")) {
            final String number = l[0].substring(7);
            if (number.equals("ChosenNumber")) {
                return doXMath(c.getChosenNumber(), m, c);
            } else {
                return doXMath(Integer.parseInt(number), m, c);
            }
        }

        if (l[0].startsWith("Count$")) {
            l[0] = l[0].substring(6);
        }

        if (l[0].startsWith("SVar$")) {
            return doXMath(xCount(c, c.getSVar(l[0].substring(5))), m, c);
        }

        // Manapool
        if (l[0].startsWith("ManaPool")) {
            final String color = l[0].split(":")[1];
            if (color.equals("All")) {
                return c.getController().getManaPool().totalMana();
            } else {
                return c.getController().getManaPool().getAmountOfColor(color);
            }
        }

        // count valid cards in any specified zone/s
        if (l[0].startsWith("Valid")) {
            String[] lparts = l[0].split(" ", 2);
            final String[] rest = lparts[1].split(",");

            final List<Card> cardsInZones = lparts[0].length() > 5 
                ? cc.getGame().getCardsIn(ZoneType.listValueOf(lparts[0].substring(5)))
                : cc.getGame().getCardsIn(ZoneType.Battlefield);

            List<Card> cards = CardLists.getValidCards(cardsInZones, rest, cc, c);
            return doXMath(cards.size(), m, c);
        }


        if (l[0].startsWith("ImprintedCardPower") && !c.getImprinted().isEmpty())       return c.getImprinted().get(0).getNetAttack();
        if (l[0].startsWith("ImprintedCardToughness") && !c.getImprinted().isEmpty())   return c.getImprinted().get(0).getNetDefense();
        if (l[0].startsWith("ImprintedCardManaCost") && !c.getImprinted().isEmpty())    return c.getImprinted().get(0).getCMC();

        if (l[0].startsWith("GreatestPowerYouControl")) {
            int highest = 0;
            for (final Card crd : c.getController().getCreaturesInPlay()) {
                if (crd.getNetAttack() > highest) {
                    highest = crd.getNetAttack();
                }
            }
            return highest;
        }

        if (l[0].startsWith("GreatestPowerYouDontControl")) {
            int highest = 0;
            for (final Card crd : c.getController().getGame().getCardsIn(ZoneType.Battlefield)) {
                if (!crd.isCreature() || crd.getController() == c.getController())
                    continue;
                if (crd.getNetAttack() > highest) {
                    highest = crd.getNetAttack();
                }
            }
            return highest;
        }

        if (l[0].startsWith("HighestCMCRemembered")) {
            final List<Card> list = new ArrayList<Card>();
            int highest = 0;
            for (final Object o : c.getRemembered()) {
                if (o instanceof Card) {
                    list.add(Singletons.getModel().getGame().getCardState((Card) o));
                }
            }
            for (final Card crd : list) {
                if (crd.isSplitCard()) {
                    if (crd.getCMC(Card.SplitCMCMode.LeftSplitCMC) > highest) {
                        highest = crd.getCMC(Card.SplitCMCMode.LeftSplitCMC);
                    }
                    if (crd.getCMC(Card.SplitCMCMode.RightSplitCMC) > highest) {
                        highest = crd.getCMC(Card.SplitCMCMode.RightSplitCMC);
                    }
                } else {
                    if (crd.getCMC() > highest) {
                        highest = crd.getCMC();
                    }
                }
            }
            return highest;
        }

        if (l[0].startsWith("DifferentCardNames_")) {
            final List<String> crdname = new ArrayList<String>();
            final String restriction = l[0].substring(19);
            final String[] rest = restriction.split(",");
            List<Card> list = cc.getGame().getCardsInGame();
            list = CardLists.getValidCards(list, rest, cc, c);
            for (final Card card : list) {
                if (!crdname.contains(card.getName())) {
                    crdname.add(card.getName());
                }
            }
            return doXMath(crdname.size(), m, c);
        }

        if (l[0].startsWith("RememberedSize")) {
            return doXMath(c.getRemembered().size(), m, c);
        }

        // Count$CountersAdded <CounterType> <ValidSource>
        if (l[0].startsWith("CountersAdded")) {
            final String[] components = l[0].split(" ", 3);
            final CounterType counterType = CounterType.valueOf(components[1]);
            String restrictions = components[2];
            final String[] rest = restrictions.split(",");
            List<Card> candidates = Singletons.getModel().getGame().getCardsInGame();
            candidates = CardLists.getValidCards(candidates, rest, cc, c);

            int added = 0;
            for (final Card counterSource : candidates) {
                added += c.getCountersAddedBy(counterSource, counterType);
            }
            return doXMath(added, m, c);
        }

        if (l[0].startsWith("RolledThisTurn")) {
            return Singletons.getModel().getGame().getPhaseHandler().getPlanarDiceRolledthisTurn();
        }

        final String[] sq;
        sq = l[0].split("\\.");

        if (sq[0].contains("xPaid"))            return doXMath(c.getXManaCostPaid(), m, c);


        if (sq[0].equals("YouDrewThisTurn"))    return doXMath(c.getController().getNumDrawnThisTurn(), m, c);
        if (sq[0].equals("OppDrewThisTurn"))    return doXMath(c.getController().getOpponent().getNumDrawnThisTurn(), m, c);
        

        if (sq[0].equals("StormCount"))         return doXMath(Singletons.getModel().getGame().getStack().getCardsCastThisTurn().size() - 1, m, c);
        if (sq[0].equals("DamageDoneThisTurn")) return doXMath(c.getDamageDoneThisTurn(), m, c);
        if (sq[0].equals("BloodthirstAmount"))  return doXMath(c.getController().getBloodthirstAmount(), m, c);
        if (sq[0].equals("RegeneratedThisTurn")) return doXMath(c.getRegeneratedThisTurn(), m, c);
        
        // TriggeringObjects
        if (sq[0].startsWith("Triggered"))      return doXMath((Integer) c.getTriggeringObject(sq[0].substring(9)), m, c);

        if (sq[0].contains("YourStartingLife"))     return doXMath(cc.getStartingLife(), m, c);
        //if (sq[0].contains("OppStartingLife"))    return doXMath(oppController.getStartingLife(), m, c); // found no cards using it
        

        if (sq[0].contains("YourLifeTotal"))        return doXMath(cc.getLife(), m, c);
        if (sq[0].contains("OppLifeTotal"))         return doXMath(ccOpp.getLife(), m, c);

        //  Count$TargetedLifeTotal (targeted player's life total)
        if (sq[0].contains("TargetedLifeTotal")) {
            for (final SpellAbility sa : c.getCharacteristics().getSpellAbility()) {
                final SpellAbility saTargeting = sa.getSATargetingPlayer();
                if (saTargeting != null) {
                    for (final Player tgtP : saTargeting.getTarget().getTargetPlayers()) {
                        return doXMath(tgtP.getLife(), m, c);
                    }
                }
            }
        }

        if (sq[0].contains("LifeYouLostThisTurn"))  return doXMath(cc.getLifeLostThisTurn(), m, c);
        if (sq[0].contains("LifeOppsLostThisTurn")) {
            int lost = 0;
            for (Player opp : cc.getOpponents()) {
                lost += opp.getLifeLostThisTurn();
            }
            return doXMath(lost, m, c);
        }

        if (sq[0].equals("TotalDamageDoneByThisTurn"))      return doXMath(c.getTotalDamageDoneBy(), m, c);
        if (sq[0].equals("TotalDamageReceivedThisTurn"))    return doXMath(c.getTotalDamageRecievedThisTurn(), m, c);

        if (sq[0].contains("YourPoisonCounters"))           return doXMath(cc.getPoisonCounters(), m, c);
        if (sq[0].contains("OppPoisonCounters"))            return doXMath(ccOpp.getPoisonCounters(), m, c);

        if (sq[0].contains("OppDamageThisTurn"))            return doXMath(cc.getOpponent().getAssignedDamage(), m, c);
        if (sq[0].contains("YourDamageThisTurn"))           return doXMath(cc.getAssignedDamage(), m, c);

        // Count$YourTypeDamageThisTurn Type
        if (sq[0].contains("YourTypeDamageThisTurn"))       return doXMath(cc.getAssignedDamage(sq[0].split(" ")[1]), m, c);
        if (sq[0].contains("YourLandsPlayed"))              return doXMath(cc.getNumLandsPlayed(), m, c);


        // Count$HighestLifeTotal
        if (sq[0].contains("HighestLifeTotal")) {
            return doXMath(Aggregates.max(cc.getGame().getPlayers(), Player.Accessors.FN_GET_LIFE), m, c);
        }

        // Count$LowestLifeTotal
        if (sq[0].contains("LowestLifeTotal")) {
            final String[] playerType = sq[0].split(" ");
            final boolean onlyOpponents = playerType.length > 1 && playerType[1].equals("Opponent");
            List<Player> checked = onlyOpponents ? cc.getOpponents() : cc.getGame().getPlayers();
            return doXMath(Aggregates.min(checked, Player.Accessors.FN_GET_LIFE), m, c);
        }

        // Count$TopOfLibraryCMC
        if (sq[0].contains("TopOfLibraryCMC")) {
            final List<Card> library = cc.getCardsIn(ZoneType.Library);
            return doXMath(library.isEmpty() ? 0 : library.get(0).getCMC(), m, c);
        }

        // Count$EnchantedControllerCreatures
        if (sq[0].contains("EnchantedControllerCreatures")) {
            List<Card> enchantedControllerInPlay = new ArrayList<Card>();
            if (c.getEnchantingCard() != null) {
                enchantedControllerInPlay = c.getEnchantingCard().getController().getCardsIn(ZoneType.Battlefield);
                enchantedControllerInPlay = CardLists.filter(enchantedControllerInPlay, CardPredicates.Presets.CREATURES);
            }
            return enchantedControllerInPlay.size();
        }

        // Count$LowestLibrary
        if (sq[0].contains("LowestLibrary")) {
            return Aggregates.min(cc.getGame().getPlayers(), Player.Accessors.countCardsInZone(ZoneType.Library));
        }

        // Count$Chroma.<mana letter>
        if (sq[0].contains("Chroma")) {
            if (sq[0].contains("ChromaSource")) {
                // Runs Chroma for passed in Source card
                List<Card> chromaList = CardLists.createCardList(c);
                return doXMath(getNumberOfManaSymbolsByColor(sq[1], chromaList), m, c);
            }
            else {
                return doXMath(getNumberOfManaSymbolsControlledByColor(sq[1], cc), m, c);
            }
        }

        if (sq[0].contains("Hellbent"))         return doXMath(Integer.parseInt(sq[cc.hasHellbent() ? 1 : 2]), m, c);
        if (sq[0].contains("Metalcraft"))       return doXMath(Integer.parseInt(sq[cc.hasMetalcraft() ? 1 : 2]), m, c);
        if (sq[0].contains("FatefulHour"))      return doXMath(Integer.parseInt(sq[cc.getLife() <= 5 ? 1 : 2]), m, c);

        if (sq[0].contains("Landfall"))         return doXMath(Integer.parseInt(sq[cc.hasLandfall() ? 1 : 2]), m, c);
        if (sq[0].contains("Threshold"))        return doXMath(Integer.parseInt(sq[cc.hasThreshold() ? 1 : 2]), m, c);
        if (sq[0].startsWith("Kicked"))         return doXMath(Integer.parseInt(sq[c.getKickerMagnitude() > 0 ? 1 : 2]), m, c);
        if (sq[0].startsWith("AltCost"))        return doXMath(Integer.parseInt(sq[c.isOptionalCostPaid(OptionalCost.AltCost) ? 1 : 2]), m, c);

        // Count$wasCastFrom<Zone>.<true>.<false>
        if (sq[0].startsWith("wasCastFrom")) {
            boolean zonesMatch = c.getCastFrom() == ZoneType.smartValueOf(sq[0].substring(11)); 
            return doXMath(Integer.parseInt(sq[zonesMatch ? 1 : 2]), m, c);
        }

        if (sq[0].contains("GraveyardWithGE20Cards")) {
            final boolean hasBigGrave = Aggregates.max(cc.getGame().getPlayers(), Player.Accessors.countCardsInZone(ZoneType.Graveyard)) >= 20;
            return doXMath(Integer.parseInt(sq[ hasBigGrave ? 1 : 2]), m, c);
        }

        if (sq[0].startsWith("Devoured")) {
            final String validDevoured = l[0].split(" ")[1];
            List<Card> cl = CardLists.getValidCards(c.getDevoured(), validDevoured.split(","), cc, c);
            return doXMath(cl.size(), m, c);
        }

        if (sq[0].contains("CardPower"))        return doXMath(c.getNetAttack(), m, c);
        if (sq[0].contains("CardToughness"))    return doXMath(c.getNetDefense(), m, c);
        if (sq[0].contains("CardSumPT"))        return doXMath((c.getNetAttack() + c.getNetDefense()), m, c);

        // Count$SumPower_valid
        if (sq[0].contains("SumPower")) {
            final String[] restrictions = l[0].split("_");
            final String[] rest = restrictions[1].split(",");
            List<Card> filteredCards = CardLists.getValidCards(cc.getGame().getCardsIn(ZoneType.Battlefield), rest, cc, c);
            return doXMath(Aggregates.sum(filteredCards, CardPredicates.Accessors.fnGetCmc), m, c);
        }
        // Count$CardManaCost
        if (sq[0].contains("CardManaCost")) {
            Card ce = sq[0].contains("Equipped") && c.isEquipping() ? c.getEquipping().get(0) : c;
            return doXMath(ce.getCMC(), m, c);
        }
        // Count$SumCMC_valid
        if (sq[0].contains("SumCMC")) {
            final String[] restrictions = l[0].split("_");
            final String[] rest = restrictions[1].split(",");
            List<Card> cardsonbattlefield = Singletons.getModel().getGame().getCardsIn(ZoneType.Battlefield);
            List<Card> filteredCards = CardLists.getValidCards(cardsonbattlefield, rest, cc, c);
            return Aggregates.sum(filteredCards, CardPredicates.Accessors.fnGetCmc);
        }

        if (sq[0].contains("CardNumColors"))    return doXMath(CardUtil.getColors(c).size(), m, c);
        if (sq[0].contains("ChosenNumber"))     return doXMath(c.getChosenNumber(), m, c);
        if (sq[0].contains("CardCounters"))     return doXMath(c.getCounters(CounterType.getType(sq[1])), m, c);

        // Count$TotalCounters.<counterType>_<valid>
        if (sq[0].contains("TotalCounters")) {
            final String[] restrictions = l[0].split("_");
            final CounterType cType = CounterType.getType(restrictions[1]);
            final String[] validFilter = restrictions[2].split(",");
            List<Card> validCards = Singletons.getModel().getGame().getCardsIn(ZoneType.Battlefield);
            validCards = CardLists.getValidCards(validCards, validFilter, cc, c);
            int cCount = 0;
            for (final Card card : validCards) {
                cCount += card.getCounters(cType);
            }
            return doXMath(cCount, m, c);
        }
        

        if (sq[0].contains("BushidoPoint"))     return doXMath(c.getKeywordMagnitude("Bushido"), m, c);
        if (sq[0].contains("TimesKicked"))      return doXMath(c.getKickerMagnitude(), m, c);
        if (sq[0].contains("NumCounters"))      return doXMath(c.getCounters(CounterType.getType(sq[1])), m, c);


        // Count$IfMainPhase.<numMain>.<numNotMain> // 7/10
        if (sq[0].contains("IfMainPhase")) {
            final PhaseHandler cPhase = cc.getGame().getPhaseHandler();
            final boolean isMyMain = cPhase.getPhase().isMain() && cPhase.getPlayerTurn().equals(cc);
            return doXMath(Integer.parseInt(sq[isMyMain ? 1 : 2]), m, c);
        }

        // Count$M12Empires.<numIf>.<numIfNot>
        if (sq[0].contains("AllM12Empires")) {
            boolean has = c.getController().isCardInPlay("Crown of Empires");
            has &= c.getController().isCardInPlay("Scepter of Empires");
            has &= c.getController().isCardInPlay("Throne of Empires");
            return doXMath(Integer.parseInt(sq[has ? 1 : 2]), m, c);
        }

        // Count$ThisTurnEntered <ZoneDestination> [from <ZoneOrigin>] <Valid>
        if (sq[0].contains("ThisTurnEntered")) {
            final String[] workingCopy = l[0].split("_");
            
            ZoneType destination = ZoneType.smartValueOf(workingCopy[1]);
            final boolean hasFrom = workingCopy[2].equals("from");
            ZoneType origin = hasFrom ? ZoneType.smartValueOf(workingCopy[3]) : null;
            String validFilter = workingCopy[hasFrom ? 4 : 2] ;

            final List<Card> res = CardUtil.getThisTurnEntered(destination, origin, validFilter, c);
            return doXMath(res.size(), m, c);
        }

        // Count$AttackersDeclared
        if (sq[0].contains("AttackersDeclared")) {
            return doXMath(cc.getAttackersDeclaredThisTurn(), m, c);
        }

        // Count$ThisTurnCast <Valid>
        // Count$LastTurnCast <Valid>
        if (sq[0].contains("ThisTurnCast") || sq[0].contains("LastTurnCast")) {

            final String[] workingCopy = l[0].split("_");
            final String validFilter = workingCopy[1];

            List<Card> res;

            if (workingCopy[0].contains("This")) {
                res = CardUtil.getThisTurnCast(validFilter, c);
            } else {
                res = CardUtil.getLastTurnCast(validFilter, c);
            }

            final int ret = doXMath(res.size(), m, c);
            return ret;
        }

        // Count$Morbid.<True>.<False>
        if (sq[0].startsWith("Morbid")) {
            final List<Card> res = CardUtil.getThisTurnEntered(ZoneType.Graveyard, ZoneType.Battlefield, "Creature", c);
            if (res.size() > 0) {
                return doXMath(Integer.parseInt(sq[1]), m, c);
            } else {
                return doXMath(Integer.parseInt(sq[2]), m, c);
            }
        }

        if (sq[0].equals("YourTurns")) {
            return doXMath(cc.getTurn(), m, c);
        }

        if (sq[0].equals("TotalTurns")) {
            // Sorry for the Singleton use, replace this once this function has game passed into it
            return doXMath(Singletons.getModel().getGame().getPhaseHandler().getTurn(), m, c);
        }
        
        //Count$Random.<Min>.<Max>
        if (sq[0].equals("Random")) {
            int min = StringUtils.isNumeric(sq[1]) ? Integer.parseInt(sq[1]) : xCount(c, c.getSVar(sq[1]));
            int max = StringUtils.isNumeric(sq[2]) ? Integer.parseInt(sq[2]) : xCount(c, c.getSVar(sq[2]));

            return forge.util.MyRandom.getRandom().nextInt(1+max-min) + min;
        }


        // Count$Domain
        if (sq[0].startsWith("Domain")) {
            int n = 0;
            Player neededPlayer = sq[0].equals("DomainActivePlayer") ? activePlayer : cc;
            List<Card> someCards = CardLists.filter(neededPlayer.getCardsIn(ZoneType.Battlefield), Presets.LANDS);
            for (String basic : Constant.Color.BASIC_LANDS) {
                if (!CardLists.getType(someCards, basic).isEmpty()) {
                    n++;
                }
            }
            return doXMath(n, m, c);
        }

        // Count$ColoredCreatures *a DOMAIN for creatures*
        if (sq[0].contains("ColoredCreatures")) {
            int n = 0;
            List<Card> someCards = CardLists.filter(cc.getCardsIn(ZoneType.Battlefield), Presets.CREATURES);
            for (final String color : Constant.Color.ONLY_COLORS) {
                if (!CardLists.getColor(someCards, color).isEmpty()) {
                    n++;
                }
            }
            return doXMath(n, m, c);
        }
        
        // Count$CardMulticolor.<numMC>.<numNotMC>
        if (sq[0].contains("CardMulticolor")) {
            final boolean isMulti = CardUtil.getColors(c).size() > 1; 
            return doXMath(Integer.parseInt(sq[isMulti ? 1 : 2]), m, c);
        }

        
        // Complex counting methods
        List<Card> someCards = getCardListForXCount(c, cc, ccOpp, sq);
        
        // 1/10 - Count$MaxCMCYouCtrl
        if (sq[0].contains("MaxCMC")) {
            int mmc = Aggregates.max(someCards, CardPredicates.Accessors.fnGetCmc);
            return doXMath(mmc, m, c);
        }

        return doXMath(someCards.size(), m, c);
    }

    private static List<Card> getCardListForXCount(final Card c, final Player cc, final Player ccOpp, final String[] sq) {
        List<Card> someCards = new ArrayList<Card>();
        
        // Generic Zone-based counting
        // Count$QualityAndZones.Subquality

        // build a list of cards in each possible specified zone

        // if a card was ever written to count two different zones,
        // make sure they don't get added twice.
        boolean mf = false, my = false, mh = false;
        boolean of = false, oy = false, oh = false;

        if (sq[0].contains("YouCtrl") && !mf) {
            someCards.addAll(cc.getCardsIn(ZoneType.Battlefield));
            mf = true;
        }

        if (sq[0].contains("InYourYard") && !my) {
            someCards.addAll(cc.getCardsIn(ZoneType.Graveyard));
            my = true;
        }

        if (sq[0].contains("InYourLibrary") && !my) {
            someCards.addAll(cc.getCardsIn(ZoneType.Library));
            my = true;
        }

        if (sq[0].contains("InYourHand") && !mh) {
            someCards.addAll(cc.getCardsIn(ZoneType.Hand));
            mh = true;
        }

        if (sq[0].contains("InYourSideboard") && !mh) {
            someCards.addAll(cc.getCardsIn(ZoneType.Sideboard));
            mh = true;
        }

        if (sq[0].contains("OppCtrl")) {
            if (!of) {
                someCards.addAll(ccOpp.getCardsIn(ZoneType.Battlefield));
                of = true;
            }
        }

        if (sq[0].contains("InOppYard")) {
            if (!oy) {
                someCards.addAll(ccOpp.getCardsIn(ZoneType.Graveyard));
                oy = true;
            }
        }

        if (sq[0].contains("InOppHand")) {
            if (!oh) {
                someCards.addAll(ccOpp.getCardsIn(ZoneType.Hand));
                oh = true;
            }
        }

        if (sq[0].contains("InChosenHand")) {
            if (!oh) {
                if (c.getChosenPlayer() != null) {
                    someCards.addAll(c.getChosenPlayer().getCardsIn(ZoneType.Hand));
                }
                oh = true;
            }
        }

        if (sq[0].contains("InChosenYard")) {
            if (!oh) {
                if (c.getChosenPlayer() != null) {
                    someCards.addAll(c.getChosenPlayer().getCardsIn(ZoneType.Graveyard));
                }
                oh = true;
            }
        }

        if (sq[0].contains("OnBattlefield")) {
            if (!mf) {
                someCards.addAll(cc.getCardsIn(ZoneType.Battlefield));
            }
            if (!of) {
                someCards.addAll(ccOpp.getCardsIn(ZoneType.Battlefield));
            }
        }

        if (sq[0].contains("InAllYards")) {
            if (!my) {
                someCards.addAll(cc.getCardsIn(ZoneType.Graveyard));
            }
            if (!oy) {
                someCards.addAll(ccOpp.getCardsIn(ZoneType.Graveyard));
            }
        }

        if (sq[0].contains("SpellsOnStack")) {
            someCards.addAll(Singletons.getModel().getGame().getCardsIn(ZoneType.Stack));
        }

        if (sq[0].contains("InAllHands")) {
            if (!mh) {
                someCards.addAll(cc.getCardsIn(ZoneType.Hand));
            }
            if (!oh) {
                someCards.addAll(ccOpp.getCardsIn(ZoneType.Hand));
            }
        }

        //  Count$InTargetedHand (targeted player's cards in hand)
        if (sq[0].contains("InTargetedHand")) {
            for (final SpellAbility sa : c.getCharacteristics().getSpellAbility()) {
                final SpellAbility saTargeting = sa.getSATargetingPlayer();
                if (saTargeting != null) {
                    for (final Player tgtP : saTargeting.getTarget().getTargetPlayers()) {
                        someCards.addAll(tgtP.getCardsIn(ZoneType.Hand));
                    }
                }
            }
        }

        //  Count$InTargetedHand (targeted player's cards in hand)
        if (sq[0].contains("InEnchantedHand")) {
            GameEntity o = c.getEnchanting();
            Player controller = null;
            if (o instanceof Card) {
                controller = ((Card) o).getController();
            }
            else {
                controller = (Player) o;
            }
            if (controller != null) {
                someCards.addAll(controller.getCardsIn(ZoneType.Hand));
            }
        }
        if (sq[0].contains("InEnchantedYard")) {
            GameEntity o = c.getEnchanting();
            Player controller = null;
            if (o instanceof Card) {
                controller = ((Card) o).getController();
            }
            else {
                controller = (Player) o;
            }
            if (controller != null) {
                someCards.addAll(controller.getCardsIn(ZoneType.Graveyard));
            }
        }
        // filter lists based on the specified quality

        // "Clerics you control" - Count$TypeYouCtrl.Cleric
        if (sq[0].contains("Type")) {
            someCards = CardLists.filter(someCards, CardPredicates.isType(sq[1]));
        }

        // "Named <CARDNAME> in all graveyards" - Count$NamedAllYards.<CARDNAME>

        if (sq[0].contains("Named")) {
            if (sq[1].equals("CARDNAME")) {
                sq[1] = c.getName();
            }
            someCards = CardLists.filter(someCards, CardPredicates.nameEquals(sq[1]));
        }

        // Refined qualities

        // "Untapped Lands" - Count$UntappedTypeYouCtrl.Land
        if (sq[0].contains("Untapped")) {
            someCards = CardLists.filter(someCards, Presets.UNTAPPED);
        }

        if (sq[0].contains("Tapped")) {
            someCards = CardLists.filter(someCards, Presets.TAPPED);
        }

//        String sq0 = sq[0].toLowerCase();
//        for(String color : Constant.Color.ONLY_COLORS) {
//            if( sq0.contains(color) )
//                someCards = someCards.filter(CardListFilter.WHITE);
//        }
        // "White Creatures" - Count$WhiteTypeYouCtrl.Creature
        if (sq[0].contains("White")) {
            someCards = CardLists.filter(someCards, Presets.WHITE);
        }

        if (sq[0].contains("Blue")) {
            someCards = CardLists.filter(someCards, Presets.BLUE);
        }

        if (sq[0].contains("Black")) {
            someCards = CardLists.filter(someCards, Presets.BLACK);
        }

        if (sq[0].contains("Red")) {
            someCards = CardLists.filter(someCards, Presets.RED);
        }

        if (sq[0].contains("Green")) {
            someCards = CardLists.filter(someCards, Presets.GREEN);
        }

        if (sq[0].contains("Multicolor")) {
            someCards = CardLists.filter(someCards, new Predicate<Card>() {
                @Override
                public boolean apply(final Card c) {
                    return CardUtil.getColors(c).size() > 1;
                }
            });
        }

        if (sq[0].contains("Monocolor")) {
            someCards = CardLists.filter(someCards, new Predicate<Card>() {
                @Override
                public boolean apply(final Card c) {
                    return (CardUtil.getColors(c).size() == 1);
                }
            });
        }
        return someCards;
    }

    public static int doXMath(final int num, final String operators, final Card c) {
        if (operators == null || operators.equals("none")) {
            return num;
        }

        final String[] s = operators.split("\\.");
        int secondaryNum = 0;

        try {
            if (s.length == 2) {
                secondaryNum = Integer.parseInt(s[1]);
            }
        } catch (final Exception e) {
            secondaryNum = xCount(c, c.getSVar(s[1]));
        }

        if (s[0].contains("Plus")) {
            return num + secondaryNum;
        } else if (s[0].contains("NMinus")) {
            return secondaryNum - num;
        } else if (s[0].contains("Minus")) {
            return num - secondaryNum;
        } else if (s[0].contains("Twice")) {
            return num * 2;
        } else if (s[0].contains("Thrice")) {
            return num * 3;
        } else if (s[0].contains("HalfUp")) {
            return (int) (Math.ceil(num / 2.0));
        } else if (s[0].contains("HalfDown")) {
            return (int) (Math.floor(num / 2.0));
        } else if (s[0].contains("ThirdUp")) {
            return (int) (Math.ceil(num / 3.0));
        } else if (s[0].contains("ThirdDown")) {
            return (int) (Math.floor(num / 3.0));
        } else if (s[0].contains("Negative")) {
            return num * -1;
        } else if (s[0].contains("Times")) {
            return num * secondaryNum;
        } else if (s[0].contains("DivideEvenlyDown")) {
            if (secondaryNum == 0) {
                return 0;
            } else {
                return num / secondaryNum;
            }
        } else if (s[0].contains("Mod")) {
            return num % secondaryNum;
        } else if (s[0].contains("LimitMax")) {
            if (num < secondaryNum) {
                return num;
            } else {
                return secondaryNum;
            }
        } else if (s[0].contains("LimitMin")) {
            if (num > secondaryNum) {
                return num;
            } else {
                return secondaryNum;
            }

        } else {
            return num;
        }
    }

    /**
     * <p>
     * handlePaid.
     * </p>
     * 
     * @param paidList
     *            a {@link forge.CardList} object.
     * @param string
     *            a {@link java.lang.String} object.
     * @param source
     *            a {@link forge.Card} object.
     * @return a int.
     */
    public static int handlePaid(final List<Card> paidList, final String string, final Card source) {
        if (paidList == null) {
            if (string.contains(".")) {
                final String[] splitString = string.split("\\.", 2);
                return doXMath(0, splitString[1], source);
            } else {
                return 0;
            }
        }
        if (string.startsWith("Amount")) {
            if (string.contains(".")) {
                final String[] splitString = string.split("\\.", 2);
                return doXMath(paidList.size(), splitString[1], source);
            } else {
                return paidList.size();
            }

        }
        if (string.startsWith("Valid")) {
            
            final String[] splitString = string.split("/", 2);
            String valid = splitString[0].substring(6);
            final List<Card> list = CardLists.getValidCards(paidList, valid, source.getController(), source);
            return doXMath(list.size(), splitString.length > 1 ? splitString[1] : null, source);
        }

        int tot = 0;
        for (final Card c : paidList) {
            tot += xCount(c, string);
        }

        return tot;
    }

    /**
     * <p>
     * isMostProminentColor.
     * </p>
     * 
     * @param list
     *            a {@link forge.CardList} object.
     * @return a boolean.
     */
    public static boolean isMostProminentColor(final List<Card> list, final String color) {

        final Map<String, Integer> map = new HashMap<String, Integer>();

        for (final Card c : list) {
            for (final String color2 : CardUtil.getColors(c)) {
                if (color2.equals("colorless")) {
                    // nothing to do
                } else if (!map.containsKey(color2)) {
                    map.put(color2, 1);
                } else {
                    map.put(color2, map.get(color2) + 1);
                }
            }
        } // for

        if (map.isEmpty() || !map.containsKey(color)) {
            return false;
        }

        int num = map.get(color);

        for (final Entry<String, Integer> entry : map.entrySet()) {

            if (num < entry.getValue()) {
                return false;
            }
        }

        return true;
    }
    
    /**
     * <p>
     * sharedKeywords.
     * </p>
     * 
     * @param kw
     *            a {@link forge.CardList} object.
     * @return a List<String>.
     */
    public static List<String> sharedKeywords(final String[] kw, final String[] restrictions,
            final List<ZoneType> zones, final Card host) {
        final List<String> filteredkw = new ArrayList<String>();
        final Player p = host.getController();
        List<Card> cardlist = new ArrayList<Card>(p.getGame().getCardsIn(zones));
        final List<String> landkw = new ArrayList<String>();
        final List<String> protectionkw = new ArrayList<String>();
        final List<String> allkw = new ArrayList<String>();
        
        cardlist = CardLists.getValidCards(cardlist, restrictions, p, host);
        for (Card c : cardlist) {
            for (String k : c.getKeyword()) {
                if (k.endsWith("walk")) {
                    if (!landkw.contains(k)) {
                        landkw.add(k);
                    }
                } else if (k.startsWith("Protection")) {
                    if (!protectionkw.contains(k)) {
                        protectionkw.add(k);
                    }
                }
                if (!allkw.contains(k)) {
                    allkw.add(k);
                }
            }
        }
        for (String keyword : kw) {
            if (keyword.equals("Protection")) {
                filteredkw.addAll(protectionkw);
            } else if (keyword.equals("Landwalk")) {
                filteredkw.addAll(landkw);
            } else if (allkw.contains(keyword)) {
                filteredkw.add(keyword);
            }
        }
        return filteredkw;
    }

    /**
     * <p>
     * getBushidoEffects.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @return a {@link java.util.ArrayList} object.
     */
    public static ArrayList<Ability> getBushidoEffects(final Card c) {
        final ArrayList<Ability> list = new ArrayList<Ability>();

        final Card crd = c;

        for (final String kw : c.getKeyword()) {
            if (kw.contains("Bushido")) {
                final String[] parse = kw.split(" ");
                final String s = parse[1];
                final int magnitude = Integer.parseInt(s);

                final Ability ability = new Ability(c, ManaCost.ZERO) {
                    @Override
                    public void resolve() {
                        final Command untilEOT = new Command() {

                            private static final long serialVersionUID = 3014846051064254493L;

                            @Override
                            public void run() {
                                if (crd.isInPlay()) {
                                    crd.addTempAttackBoost(-1 * magnitude);
                                    crd.addTempDefenseBoost(-1 * magnitude);
                                }
                            }
                        };

                        Singletons.getModel().getGame().getEndOfTurn().addUntil(untilEOT);

                        crd.addTempAttackBoost(magnitude);
                        crd.addTempDefenseBoost(magnitude);
                    }
                };
                final StringBuilder sb = new StringBuilder();
                sb.append(c);
                sb.append(" - (Bushido) gets +");
                sb.append(magnitude);
                sb.append("/+");
                sb.append(magnitude);
                sb.append(" until end of turn.");
                ability.setStackDescription(sb.toString());

                list.add(ability);
            }
        }
        return list;
    }

    /**
     * <p>
     * getNeededXDamage.
     * </p>
     * 
     * @param ability
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a int.
     */
    public static int getNeededXDamage(final SpellAbility ability) {
        // when targeting a creature, make sure the AI won't overkill on X
        // damage
        final Card target = ability.getTargetCard();
        int neededDamage = -1;

        if ((target != null)) {
            neededDamage = target.getNetDefense() - target.getDamage();
        }

        return neededDamage;
    }


    /**
     * <p>
     * playLandEffects.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     */
    public static void playLandEffects(final Card c) {
        final Player player = c.getController();

        // > 0 because land amount isn't incremented until after playLandEffects
        final boolean extraLand = player.getNumLandsPlayed() > 0;

        if (extraLand) {
            final List<Card> fastbonds = player.getCardsIn(ZoneType.Battlefield, "Fastbond");
            for (final Card f : fastbonds) {
                final SpellAbility ability = new Ability(f, ManaCost.ZERO) {
                    @Override
                    public void resolve() {
                        f.getController().addDamage(1, f);
                    }
                };
                ability.setStackDescription("Fastbond - Deals 1 damage to you.");

                Singletons.getModel().getGame().getStack().addSimultaneousStackEntry(ability);

            }
        }
    }

    /**
     * <p>
     * isNegativeCounter.
     * </p>
     * 
     * @param c
     *            a {@link forge.CounterType} object.
     * @return a boolean.
     */
    public static boolean isNegativeCounter(final CounterType c) {
        return (c == CounterType.AGE) || (c == CounterType.BLAZE) || (c == CounterType.BRIBERY) || (c == CounterType.DOOM)
                || (c == CounterType.ICE) || (c == CounterType.M1M1) || (c == CounterType.M0M2) || (c == CounterType.M0M1)
                || (c == CounterType.TIME);
    }

    public static void correctAbilityChainSourceCard(final SpellAbility sa, final Card card) {

        sa.setSourceCard(card);

        if (sa.getSubAbility() != null) {
            correctAbilityChainSourceCard(sa.getSubAbility(), card);
        }
    }

    /**
     * Adds the ability factory abilities.
     * 
     * @param card
     *            the card
     */
    public static final void addAbilityFactoryAbilities(final Card card) {
        // **************************************************
        // AbilityFactory cards
        for (String rawAbility : card.getUnparsedAbilities()) {
            card.addSpellAbility(AbilityFactory.getAbility(rawAbility, card));
        }
    }

    /**
     * <p>
     * postFactoryKeywords.
     * </p>
     * 
     * @param card
     *            a {@link forge.Card} object.
     */
    public static void setupKeywordedAbilities(final Card card) {
        // this function should handle any keywords that need to be added after
        // a spell goes through the factory
        // Cards with Cycling abilities
        // -1 means keyword "Cycling" not found

        // TODO - certain cards have two different kicker types, kicker will
        // need
        // to be written differently to handle this
        // TODO - kicker costs can only be mana right now i think?
        // TODO - this kicker only works for pemanents. maybe we can create an
        // optional cost class for buyback, kicker, that type of thing

        if (hasKeyword(card, "Multikicker") != -1) {
            final int n = hasKeyword(card, "Multikicker");
            if (n != -1) {
                final String parse = card.getKeyword().get(n).toString();
                final String[] k = parse.split("kicker ");

                final SpellAbility sa = card.getFirstSpellAbility();
                sa.setMultiKickerManaCost(new ManaCost(new ManaCostParser(k[1])));
            }
        }

        if (hasKeyword(card, "Replicate") != -1) {
            final int n = hasKeyword(card, "Replicate");
            if (n != -1) {
                final String parse = card.getKeyword().get(n).toString();
                final String[] k = parse.split("cate ");

                final SpellAbility sa = card.getFirstSpellAbility();
                sa.setIsReplicate(true);
                sa.setReplicateManaCost(new ManaCost(new ManaCostParser(k[1])));
            }
        }
        
        if(hasKeyword(card, "Fuse") != -1) {
            card.getState(CardCharacteristicName.Original).getSpellAbility().add(AbilityFactory.buildFusedAbility(card));
        }

        final int evokePos = hasKeyword(card, "Evoke");
        if (evokePos != -1) {
            card.addSpellAbility(makeEvokeSpell(card, card.getKeyword().get(evokePos)));
        }

        if (hasKeyword(card, "Cycling") != -1) {
            final int n = hasKeyword(card, "Cycling");
            if (n != -1) {
                final String parse = card.getKeyword().get(n).toString();
                card.removeIntrinsicKeyword(parse);

                final String[] k = parse.split(":");
                final String manacost = k[1];

                card.addSpellAbility(abilityCycle(card, manacost));
            }
        } // Cycling

        while (hasKeyword(card, "TypeCycling") != -1) {
            final int n = hasKeyword(card, "TypeCycling");
            if (n != -1) {
                final String parse = card.getKeyword().get(n).toString();
                card.removeIntrinsicKeyword(parse);

                final String[] k = parse.split(":");
                final String type = k[1];
                final String manacost = k[2];

                card.addSpellAbility(abilityTypecycle(card, manacost, type));
            }
        } // TypeCycling

        if (hasKeyword(card, "Transmute") != -1) {
            final int n = hasKeyword(card, "Transmute");
            if (n != -1) {
                final String parse = card.getKeyword().get(n);
                card.removeIntrinsicKeyword(parse);
                final String manacost = parse.split(":")[1];

                card.addSpellAbility(abilityTransmute(card, manacost));
            }
        } // transmute

        int shiftPos = hasKeyword(card, "Soulshift");
        while (shiftPos != -1) {
            final int n = shiftPos;
            final String parse = card.getKeyword().get(n);
            final String[] k = parse.split(" ");
            final int manacost = Integer.parseInt(k[1]);

            final String actualTrigger = "Mode$ ChangesZone | Origin$ Battlefield | Destination$ Graveyard"
                    + "| OptionalDecider$ You | ValidCard$ Card.Self | Execute$ SoulshiftAbility"
                    + "| TriggerController$ TriggeredCardController | TriggerDescription$ " + parse
                    + " (When this creature dies, you may return target Spirit card with converted mana cost "
                    + manacost + " or less from your graveyard to your hand.)";
            final String abString = "DB$ ChangeZone | Origin$ Graveyard | Destination$ Hand"
                    + "| ValidTgts$ Spirit.YouOwn+cmcLE" + manacost;
            final Trigger parsedTrigger = TriggerHandler.parseTrigger(actualTrigger, card, true);
            card.addTrigger(parsedTrigger);
            card.setSVar("SoulshiftAbility", abString);
            shiftPos = hasKeyword(card, "Soulshift", n + 1);
        } // Soulshift

        final int championPos = hasKeyword(card, "Champion");
        if (championPos != -1) {
            String parse = card.getKeyword().get(championPos);
            card.removeIntrinsicKeyword(parse);

            final String[] k = parse.split(":");
            final String[] valid = k[1].split(",");
            String desc = k.length > 2 ? k[2] : k[1];

            StringBuilder changeType = new StringBuilder();
            for (String v : valid) {
                if (changeType.length() != 0) {
                    changeType.append(",");
                }
                changeType.append(v).append(".YouCtrl+Other");
            }

            StringBuilder trig = new StringBuilder();
            trig.append("Mode$ ChangesZone | Origin$ Any | Destination$ Battlefield | ValidCard$ Card.Self | ");
            trig.append("Execute$ ChampionAbility | TriggerDescription$ Champion a(n) ");
            trig.append(desc).append(" (When this enters the battlefield, sacrifice it unless you exile another ");
            trig.append(desc).append(" you control. When this leaves the battlefield, that card returns to the battlefield.)");

            StringBuilder trigReturn = new StringBuilder();
            trigReturn.append("Mode$ ChangesZone | Origin$ Battlefield | Destination$ Any | ValidCard$ Card.Self | ");
            trigReturn.append("Execute$ ChampionReturn | Secondary$ True | TriggerDescription$ When this leaves the battlefield, that card returns to the battlefield.");

            StringBuilder ab = new StringBuilder();
            ab.append("DB$ ChangeZone | Origin$ Battlefield | Destination$ Exile | RememberChanged$ True | Champion$ True | ");
            ab.append("Hidden$ True | Optional$ True | SubAbility$ DBSacrifice | ChangeType$ ").append(changeType);

            StringBuilder subAb = new StringBuilder();
            subAb.append("DB$ Sacrifice | Defined$ Card.Self | ConditionDefined$ Remembered | ConditionPresent$ Card | ConditionCompare$ EQ0");

            String returnChampion = "DB$ ChangeZone | Defined$ Remembered | Origin$ Exile | Destination$ Battlefield";
            final Trigger parsedTrigger = TriggerHandler.parseTrigger(trig.toString(), card, true);
            final Trigger parsedTrigReturn = TriggerHandler.parseTrigger(trigReturn.toString(), card, true);
            card.addTrigger(parsedTrigger);
            card.addTrigger(parsedTrigReturn);
            card.setSVar("ChampionAbility", ab.toString());
            card.setSVar("ChampionReturn", returnChampion);
            card.setSVar("DBSacrifice", subAb.toString());
        }

        final int echoPos = hasKeyword(card, "Echo");
        if (echoPos != -1) {
            // card.removeIntrinsicKeyword(parse);
            final String[] k = card.getKeyword().get(echoPos).split(":");
            final String manacost = k[1];

            card.setEchoCost(manacost);

            final Command intoPlay = new Command() {

                private static final long serialVersionUID = -7913835645603984242L;

                @Override
                public void run() {
                    card.addExtrinsicKeyword("(Echo unpaid)");
                }
            };
            card.addComesIntoPlayCommand(intoPlay);

        } // echo

        if (hasKeyword(card, "Suspend") != -1) {
            // Suspend:<TimeCounters>:<Cost>
            final int n = hasKeyword(card, "Suspend");
            if (n != -1) {
                final String parse = card.getKeyword().get(n);
                card.removeIntrinsicKeyword(parse);
                card.setSuspend(true);
                final String[] k = parse.split(":");

                final String timeCounters = k[1];
                final String cost = k[2];
                card.addSpellAbility(abilitySuspend(card, cost, timeCounters));
            }
        } // Suspend

        if (hasKeyword(card, "Fading") != -1) {
            final int n = hasKeyword(card, "Fading");
            if (n != -1) {
                final String[] k = card.getKeyword().get(n).split(":");
                final int power = Integer.parseInt(k[1]);

                card.addComesIntoPlayCommand(fading(card, power));
            }
        } // Fading

        if (hasKeyword(card, "Vanishing") != -1) {
            final int n = hasKeyword(card, "Vanishing");
            if (n != -1) {
                final String[] k = card.getKeyword().get(n).split(":");
                final int power = Integer.parseInt(k[1]);

                card.addComesIntoPlayCommand(vanishing(card, power));
            }
        } // Vanishing

        // AddCost
        if (!card.getSVar("FullCost").equals("")) {
            final SpellAbility sa1 = card.getFirstSpellAbility();
            if (sa1 != null && sa1.isSpell()) {
                sa1.setPayCosts(new Cost(card.getSVar("FullCost"), sa1.isAbility()));
            }
        }

        // AltCost
        String altCost = card.getSVar("AltCost");
        if (StringUtils.isNotBlank(altCost)) {
            final SpellAbility sa1 = card.getFirstSpellAbility();
            if (sa1 != null && sa1.isSpell()) {
                card.addSpellAbility(makeAltCostAbility(card, altCost, sa1));
            }
        }

        if (card.hasKeyword("Delve")) {
            card.getSpellAbilities().get(0).setDelve(true);
        }

        if (card.hasStartOfKeyword("Haunt")) {
            setupHauntSpell(card);
        }

        if (card.hasKeyword("Provoke")) {
            final String actualTrigger = "Mode$ Attacks | ValidCard$ Card.Self | "
                    + "OptionalDecider$ You | Execute$ ProvokeAbility | Secondary$ True | TriggerDescription$ "
                    + "When this attacks, you may have target creature defending player "
                    + "controls untap and block it if able.";
            final String abString = "DB$ MustBlock | ValidTgts$ Creature.DefenderCtrl | "
                    + "TgtPrompt$ Select target creature defending player controls | SubAbility$ DBUntap";
            final String dbString = "DB$ Untap | Defined$ Targeted";
            final Trigger parsedTrigger = TriggerHandler.parseTrigger(actualTrigger, card, true);
            card.addTrigger(parsedTrigger);
            card.setSVar("ProvokeAbility", abString);
            card.setSVar("DBUntap", dbString);
        }

        if (card.hasKeyword("Living Weapon")) {
            card.removeIntrinsicKeyword("Living Weapon");

            final StringBuilder sbTrig = new StringBuilder();
            sbTrig.append("Mode$ ChangesZone | Origin$ Any | Destination$ Battlefield | ");
            sbTrig.append("ValidCard$ Card.Self | Execute$ TrigGerm | TriggerDescription$ ");
            sbTrig.append("Living Weapon (When this Equipment enters the battlefield, ");
            sbTrig.append("put a 0/0 black Germ creature token onto the battlefield, then attach this to it.)");

            final StringBuilder sbGerm = new StringBuilder();
            sbGerm.append("DB$ Token | TokenAmount$ 1 | TokenName$ Germ | TokenTypes$ Creature,Germ | RememberTokens$ True | ");
            sbGerm.append("TokenOwner$ You | TokenColors$ Black | TokenPower$ 0 | TokenToughness$ 0 | TokenImage$ B 0 0 Germ | SubAbility$ DBGermAttach");

            final StringBuilder sbAttach = new StringBuilder();
            sbAttach.append("DB$ Attach | Defined$ Remembered | SubAbility$ DBGermClear");

            final StringBuilder sbClear = new StringBuilder();
            sbClear.append("DB$ Cleanup | ClearRemembered$ True");

            card.setSVar("TrigGerm", sbGerm.toString());
            card.setSVar("DBGermAttach", sbAttach.toString());
            card.setSVar("DBGermClear", sbClear.toString());

            final Trigger etbTrigger = TriggerHandler.parseTrigger(sbTrig.toString(), card, true);
            card.addTrigger(etbTrigger);
        }

        if (card.hasKeyword("Epic")) {
            makeEpic(card);
        }

        if (card.hasKeyword("Soulbond")) {
            // Setup ETB trigger for card with Soulbond keyword
            final String actualTriggerSelf = "Mode$ ChangesZone | Origin$ Any | Destination$ Battlefield | "
                    + "ValidCard$ Card.Self | Execute$ TrigBondOther | OptionalDecider$ You | "
                    + "IsPresent$ Creature.Other+YouCtrl+NotPaired | Secondary$ True | "
                    + "TriggerDescription$ When CARDNAME enters the battlefield, "
                    + "you may pair CARDNAME with another unpaired creature you control";
            final String abStringSelf = "AB$ Bond | Cost$ 0 | Defined$ Self | ValidCards$ Creature.Other+YouCtrl+NotPaired";
            final Trigger parsedTriggerSelf = TriggerHandler.parseTrigger(actualTriggerSelf, card, true);
            card.addTrigger(parsedTriggerSelf);
            card.setSVar("TrigBondOther", abStringSelf);
            // Setup ETB trigger for other creatures you control
            final String actualTriggerOther = "Mode$ ChangesZone | Origin$ Any | Destination$ Battlefield | "
                    + "ValidCard$ Creature.Other+YouCtrl | TriggerZones$ Battlefield | OptionalDecider$ You | "
                    + "Execute$ TrigBondSelf | IsPresent$ Creature.Self+NotPaired | Secondary$ True | "
                    + " TriggerDescription$ When another unpaired creature you control enters the battlefield, "
                    + "you may pair it with CARDNAME";
            final String abStringOther = "AB$ Bond | Cost$ 0 | Defined$ TriggeredCard | ValidCards$ Creature.Self+NotPaired";
            final Trigger parsedTriggerOther = TriggerHandler.parseTrigger(actualTriggerOther, card, true);
            card.addTrigger(parsedTriggerOther);
            card.setSVar("TrigBondSelf", abStringOther);
        }

        if (card.hasKeyword("Extort")) {
            final String extortTrigger = "Mode$ SpellCast | ValidCard$ Card | ValidActivatingPlayer$ You | "
                    + "TriggerZones$ Battlefield | Execute$ ExtortOpps | Secondary$ True"
                    + " | TriggerDescription$ Extort (Whenever you cast a spell, you may pay WB. If you do, "
                    + "each opponent loses 1 life and you gain that much life.)";
            final String abString = "AB$ LoseLife | Cost$ WB | Defined$ Player.Opponent | "
                    + "LifeAmount$ 1 | SubAbility$ DBGainLife";
            final String dbString = "DB$ GainLife | Defined$ You | LifeAmount$ AFLifeLost";
            final Trigger parsedTrigger = TriggerHandler.parseTrigger(extortTrigger, card, true);
            card.addTrigger(parsedTrigger);
            card.setSVar("ExtortOpps", abString);
            card.setSVar("DBGainLife", dbString);
            card.setSVar("AFLifeLost", "Number$0");
        }

        if (card.hasKeyword("Evolve")) {
            final String evolveTrigger = "Mode$ ChangesZone | Origin$ Any | Destination$ Battlefield | "
                    + " ValidCard$ Creature.YouCtrl+Other | EvolveCondition$ True | "
                    + "TriggerZones$ Battlefield | Execute$ EvolveAddCounter | Secondary$ True | "
                    + "TriggerDescription$ Evolve (Whenever a creature enters the battlefield under your "
                    + "control, if that creature has greater power or toughness than this creature, put a "
                    + "+1/+1 counter on this creature.)";
            final String abString = "AB$ PutCounter | Cost$ 0 | Defined$ Self | CounterType$ P1P1 | "
                    + "CounterNum$ 1 | Evolve$ True";
            final Trigger parsedTrigger = TriggerHandler.parseTrigger(evolveTrigger, card, true);
            card.addTrigger(parsedTrigger);
            card.setSVar("EvolveAddCounter", abString);
        }

        if (card.hasStartOfKeyword("Amplify")) {
            // find position of Amplify keyword
            final int equipPos = card.getKeywordPosition("Amplify");
            final String[] ampString = card.getKeyword().get(equipPos).split(":");
            final String amplifyMagnitude = ampString[1];
            final String suffix = !amplifyMagnitude.equals("1") ? "s" : "";
            final String ampTypes = ampString[2];
            String[] refinedTypes = ampTypes.split(",");
            final StringBuilder types = new StringBuilder();
            for (int i = 0; i < refinedTypes.length; i++) {
                types.append("Card.").append(refinedTypes[i]).append("+YouCtrl");
                if (i + 1 != refinedTypes.length) {
                    types.append(",");
                }
            }
            // Setup ETB trigger for card with Amplify keyword
            final String actualTrigger = "Mode$ ChangesZone | Origin$ Any | Destination$ Battlefield | "
                    + "ValidCard$ Card.Self | Execute$ AmplifyReveal | Static$ True | Secondary$ True | "
                    + "TriggerDescription$ As this creature enters the battlefield, put "
                    + amplifyMagnitude + " +1/+1 counter" + suffix + " on it for each "
                    + ampTypes.replace(",", " and/or ") + " card you reveal in your hand.)";
            final String abString = "AB$ Reveal | Cost$ 0 | AnyNumber$ True | RevealValid$ "
                    + types.toString() + " | RememberRevealed$ True | SubAbility$ Amplify";
            final String dbString = "DB$ PutCounter | Defined$ Self | CounterType$ P1P1 | "
                    + "CounterNum$ AmpMagnitude | References$ Revealed,AmpMagnitude | SubAbility$ DBCleanup";
            final Trigger parsedTrigger = TriggerHandler.parseTrigger(actualTrigger, card, true);
            card.addTrigger(parsedTrigger);
            card.setSVar("AmplifyReveal", abString);
            card.setSVar("Amplify", dbString);
            card.setSVar("DBCleanup", "DB$ Cleanup | ClearRemembered$ True");
            card.setSVar("AmpMagnitude", "SVar$Revealed/Times." + amplifyMagnitude);
            card.setSVar("Revealed", "Remembered$Amount");
        }

        if (card.hasStartOfKeyword("Equip")) {
            // find position of Equip keyword
            final int equipPos = card.getKeywordPosition("Equip");
            // Check for additional params such as preferred AI targets
            final String equipString = card.getKeyword().get(equipPos).substring(5);
            final String[] equipExtras = equipString.contains("\\|") ? equipString.split("\\|", 2) : null;
            // Get cost string
            String equipCost = "";
            if (equipExtras != null) {
                equipCost = equipExtras[0].trim();
            } else {
                equipCost = equipString.trim();
            }
           // Create attach ability string
            final StringBuilder abilityStr = new StringBuilder();
            abilityStr.append("AB$ Attach | Cost$ ");
            abilityStr.append(equipCost);
            abilityStr.append(" | ValidTgts$ Creature.YouCtrl | TgtPrompt$ Select target creature you control ");
            abilityStr.append("| SorcerySpeed$ True | Equip$ True | AILogic$ Pump | IsPresent$ Card.Self+nonCreature ");
            if (equipExtras != null) {
                abilityStr.append("| ").append(equipExtras[1]).append(" ");
            }
            if (equipCost.matches(".+<.+>")) { //Something other than a mana cost
                abilityStr.append("| PrecostDesc$ Equip - | SpellDescription$ (Attach to target creature you control. Equip only as a sorcery.)");
            }
            else {
                abilityStr.append("| PrecostDesc$ Equip | SpellDescription$ (Attach to target creature you control. Equip only as a sorcery.)");
            }
            // instantiate attach ability
            final SpellAbility sa = AbilityFactory.getAbility(abilityStr.toString(), card);
            card.addSpellAbility(sa);
            // add ability to instrinic strings so copies/clones create the ability also
            card.getUnparsedAbilities().add(abilityStr.toString());
        }

        setupEtbKeywords(card);
    }

    /**
     * TODO: Write javadoc for this method.
     * @param card
     */
    private static void setupEtbKeywords(final Card card) {
        for (String kw : card.getKeyword()) {

            if (kw.startsWith("ETBReplacement")) {
                String[] splitkw = kw.split(":");
                ReplacementLayer layer = ReplacementLayer.smartValueOf(splitkw[1]);
                SpellAbility repAb = AbilityFactory.getAbility(card.getSVar(splitkw[2]), card);
                String desc = repAb.getDescription();
                setupETBReplacementAbility(repAb);

                final String valid = splitkw.length >= 6 ? splitkw[5] : "Card.Self";

                StringBuilder repEffsb = new StringBuilder();
                repEffsb.append("Event$ Moved | ValidCard$ ").append(valid);
                repEffsb.append(" | Destination$ Battlefield | Description$ ").append(desc);
                if (splitkw.length >= 4) {
                    if (splitkw[3].contains("Optional")) {
                        repEffsb.append(" | Optional$ True");
                    }
                }
                if (splitkw.length >= 5) {
                    if (!splitkw[4].isEmpty()) {
                        repEffsb.append(" | ActiveZones$ " + splitkw[4]);
                    }
                }

                ReplacementEffect re = ReplacementHandler.parseReplacement(repEffsb.toString(), card);
                re.setLayer(layer);
                re.setOverridingAbility(repAb);

                card.addReplacementEffect(re);
            } else if (kw.startsWith("etbCounter")) {
                String parse = kw;
                card.removeIntrinsicKeyword(parse);

                String[] splitkw = parse.split(":");

                String desc = "CARDNAME enters the battlefield with " + splitkw[2] + " "
                        + CounterType.valueOf(splitkw[1]).getName() + " counters on it.";
                String extraparams = "";
                String amount = splitkw[2];
                if (splitkw.length > 3) {
                    if (!splitkw[3].equals("no Condition")) {
                        extraparams = splitkw[3];
                    }
                }
                if (splitkw.length > 4) {
                    desc = !splitkw[4].equals("no desc") ? splitkw[4] : "";
                }
                String abStr = "AB$ ChangeZone | Cost$ 0 | Hidden$ True | Origin$ All | Destination$ Battlefield"
                        + "| Defined$ ReplacedCard | SubAbility$ ETBCounterDBSVar";
                String dbStr = "DB$ PutCounter | Defined$ Self | CounterType$ " + splitkw[1] + " | CounterNum$ " + amount;
                try {
                    Integer.parseInt(amount);
                }
                catch (NumberFormatException ignored) {
                    dbStr += " | References$ " + amount;
                }
                card.setSVar("ETBCounterSVar", abStr);
                card.setSVar("ETBCounterDBSVar", dbStr);

                String repeffstr = "Event$ Moved | ValidCard$ Card.Self | Destination$ Battlefield "
                        + "| ReplaceWith$ ETBCounterSVar | Description$ " + desc + (!extraparams.equals("") ? " | " + extraparams : "");

                ReplacementEffect re = ReplacementHandler.parseReplacement(repeffstr, card);
                re.setLayer(ReplacementLayer.Other);

                card.addReplacementEffect(re);
            } else if (kw.equals("CARDNAME enters the battlefield tapped.")) {
                String parse = kw;
                card.removeIntrinsicKeyword(parse);

                String abStr = "AB$ Tap | Cost$ 0 | Defined$ Self | ETB$ True | SubAbility$ MoveETB";
                String dbStr = "DB$ ChangeZone | Hidden$ True | Origin$ All | Destination$ Battlefield"
                        + "| Defined$ ReplacedCard";

                card.setSVar("ETBTappedSVar", abStr);
                card.setSVar("MoveETB", dbStr);

                String repeffstr = "Event$ Moved | ValidCard$ Card.Self | Destination$ Battlefield "
                        + "| ReplaceWith$ ETBTappedSVar | Description$ CARDNAME enters the battlefield tapped.";

                ReplacementEffect re = ReplacementHandler.parseReplacement(repeffstr, card);
                re.setLayer(ReplacementLayer.Other);

                card.addReplacementEffect(re);
            }
        }
    }

    /**
     * TODO: Write javadoc for this method.
     * @param card
     * @return
     */
    private static void makeEpic(final Card card) {

        // Add the Epic effect as a subAbility
        String dbStr = "DB$ Effect | Triggers$ EpicTrigger | SVars$ EpicCopy | StaticAbilities$ EpicCantBeCast | Duration$ Permanent | Unique$ True";
        
        final AbilitySub newSA = (AbilitySub) AbilityFactory.getAbility(dbStr.toString(), card);
        
        card.setSVar("EpicCantBeCast", "Mode$ CantBeCast | ValidCard$ Card | Caster$ You | EffectZone$ Command | Description$ For the rest of the game, you can't cast spells.");
        card.setSVar("EpicTrigger", "Mode$ Phase | Phase$ Upkeep | ValidPlayer$ You | Execute$ EpicCopy | TriggerDescription$ "
                + "At the beginning of each of your upkeeps, copy " + card.toString() + " except for its epic ability.");
        card.setSVar("EpicCopy", "DB$ CopySpellAbility | Defined$ EffectSource");
        
        final SpellAbility origSA = card.getSpellAbilities().get(0);
        
        SpellAbility child = origSA;
        while (child.getSubAbility() != null) {
            child = child.getSubAbility();
        }
        child.setSubAbility(newSA);
        newSA.setParent(child);
    }

    /**
     * TODO: Write javadoc for this method.
     * @param card
     */
    private static void setupHauntSpell(final Card card) {
        final int hauntPos = card.getKeywordPosition("Haunt");
        final String[] splitKeyword = card.getKeyword().get(hauntPos).split(":");
        final String hauntSVarName = splitKeyword[1];
        final String abilityDescription = splitKeyword[2];
        final String hauntAbilityDescription = abilityDescription.substring(0, 1).toLowerCase()
                + abilityDescription.substring(1);
        String hauntDescription;
        if (card.isCreature()) {
            final StringBuilder sb = new StringBuilder();
            sb.append("When ").append(card.getName());
            sb.append(" enters the battlefield or the creature it haunts dies, ");
            sb.append(hauntAbilityDescription);
            hauntDescription = sb.toString();
        } else {
            final StringBuilder sb = new StringBuilder();
            sb.append("When the creature ").append(card.getName());
            sb.append(" haunts dies, ").append(hauntAbilityDescription);
            hauntDescription = sb.toString();
        }

        card.getKeyword().remove(hauntPos);

        // First, create trigger that runs when the haunter goes to the
        // graveyard
        final StringBuilder sbHaunter = new StringBuilder();
        sbHaunter.append("Mode$ ChangesZone | Origin$ Battlefield | ");
        sbHaunter.append("Destination$ Graveyard | ValidCard$ Card.Self | ");
        sbHaunter.append("Static$ True | Secondary$ True | TriggerDescription$ Blank");

        final Trigger haunterDies = forge.card.trigger.TriggerHandler
                .parseTrigger(sbHaunter.toString(), card, true);

        final Ability haunterDiesWork = new Ability(card, ManaCost.ZERO) {
            @Override
            public void resolve() {
                this.getTargetCard().addHauntedBy(card);
                Singletons.getModel().getGame().getAction().exile(card);
            }
        };
        haunterDiesWork.setDescription(hauntDescription);

        final Ability haunterDiesSetup = new Ability(card, ManaCost.ZERO) {
            @Override
            public void resolve() {
                this.setActivatingPlayer(card.getController());
                final List<Card> creats = CardLists.filter(Singletons.getModel().getGame().getCardsIn(ZoneType.Battlefield), Presets.CREATURES);
                for (int i = 0; i < creats.size(); i++) {
                    if (!creats.get(i).canBeTargetedBy(this)) {
                        creats.remove(i);
                        i--;
                    }
                }
                if (creats.isEmpty()) {
                    return;
                }

                // need to do it this way because I don't know quite how to
                // make TriggerHandler respect BeforePayMana.
                if (card.getController().isHuman()) {
                    
                    final InputSelectCards target = new InputSelectCards(1, 1) {
                        private static final long serialVersionUID = 1981791992623774490L;
                        @Override
                        protected boolean isValidChoice(Card c) {
                            Zone zone = Singletons.getModel().getGame().getZoneOf(c);
                            if (!zone.is(ZoneType.Battlefield) || !c.isCreature()) {
                                return false;
                            }
                            return c.canBeTargetedBy(haunterDiesWork);
                        }
                    };
                    target.setMessage("Choose target creature to haunt.");
                    
                    FThreads.setInputAndWait(target);
                    if (!target.hasCancelled()) {
                        haunterDiesWork.setTargetCard(target.getSelected().get(0));
                        Singletons.getModel().getGame().getStack().add(haunterDiesWork);
                    }
                } else {
                    // AI choosing what to haunt
                    final List<Card> oppCreats = CardLists.filterControlledBy(creats, card.getController().getOpponent());
                    if (!oppCreats.isEmpty()) {
                        haunterDiesWork.setTargetCard(ComputerUtilCard.getWorstCreatureAI(oppCreats));
                    } else {
                        haunterDiesWork.setTargetCard(ComputerUtilCard.getWorstCreatureAI(creats));
                    }
                    Singletons.getModel().getGame().getStack().add(haunterDiesWork);
                }
            }
        };

        haunterDies.setOverridingAbility(haunterDiesSetup);

        // Second, create the trigger that runs when the haunted creature dies
        final StringBuilder sbDies = new StringBuilder();
        sbDies.append("Mode$ ChangesZone | Origin$ Battlefield | Destination$ Graveyard | ");
        sbDies.append("ValidCard$ Creature.HauntedBy | Execute$ ").append(hauntSVarName);
        sbDies.append(" | TriggerDescription$ ").append(hauntDescription);

        final Trigger hauntedDies = forge.card.trigger.TriggerHandler.parseTrigger(sbDies.toString(), card, true);

        // Third, create the trigger that runs when the haunting creature
        // enters the battlefield
        final StringBuilder sbETB = new StringBuilder();
        sbETB.append("Mode$ ChangesZone | Destination$ Battlefield | ValidCard$ Card.Self | Execute$ ");
        sbETB.append(hauntSVarName).append(" | Secondary$ True | TriggerDescription$ ");
        sbETB.append(hauntDescription);

        final Trigger haunterETB = forge.card.trigger.TriggerHandler.parseTrigger(sbETB.toString(), card, true);

        // Fourth, create a trigger that removes the haunting status if the
        // haunter leaves the exile
        final StringBuilder sbUnExiled = new StringBuilder();
        sbUnExiled.append("Mode$ ChangesZone | Origin$ Exile | Destination$ Any | ");
        sbUnExiled.append("ValidCard$ Card.Self | Static$ True | Secondary$ True | ");
        sbUnExiled.append("TriggerDescription$ Blank");

        final Trigger haunterUnExiled = forge.card.trigger.TriggerHandler.parseTrigger(sbUnExiled.toString(), card,
                true);

        final Ability haunterUnExiledWork = new Ability(card, ManaCost.ZERO) {
            @Override
            public void resolve() {
                if (card.getHaunting() != null) {
                    card.getHaunting().removeHauntedBy(card);
                    card.setHaunting(null);
                }
            }
        };

        haunterUnExiled.setOverridingAbility(haunterUnExiledWork);

        // Fifth, add all triggers and abilities to the card.
        if (card.isCreature()) {
            card.addTrigger(haunterETB);
            card.addTrigger(haunterDies);
        } else {
            final String abString = card.getSVar(hauntSVarName).replace("AB$", "SP$")
                    .replace("Cost$ 0", "Cost$ " + card.getManaCost())
                    + " | SpellDescription$ " + abilityDescription;

            final SpellAbility sa = AbilityFactory.getAbility(abString, card);
            card.addSpellAbility(sa);
        }

        card.addTrigger(hauntedDies);
        card.addTrigger(haunterUnExiled);
    }

    /**
     * TODO: Write javadoc for this method.
     * @param card
     * @param abilities
     * @return 
     */
    private static SpellAbility makeAltCostAbility(final Card card, final String altCost, final SpellAbility sa) {
        final Map<String, String> params = AbilityFactory.getMapParams(altCost);

        final SpellAbility altCostSA = sa.copy();
        final Cost abCost = new Cost(params.get("Cost"), altCostSA.isAbility());
        altCostSA.setPayCosts(abCost);
        altCostSA.setBasicSpell(false);
        altCostSA.addOptionalCost(OptionalCost.AltCost);

        final SpellAbilityRestriction restriction = new SpellAbilityRestriction();
        restriction.setRestrictions(params);
        if (!params.containsKey("ActivationZone")) {
            restriction.setZone(ZoneType.Hand);
        }
        altCostSA.setRestrictions(restriction);

        final String costDescription = params.containsKey("Description") ? params.get("Description") 
                : String.format("You may %s rather than pay %s's mana cost.", abCost.toStringAlt(), card.getName());
        
        altCostSA.setDescription(costDescription);
        return altCostSA;
    }

    /**
     * TODO: Write javadoc for this method.
     * @param card
     * @param evokeKeyword
     * @return
     */
    private static SpellAbility makeEvokeSpell(final Card card, final String evokeKeyword) {
        final String[] k = evokeKeyword.split(":");
        final Cost evokedCost = new Cost(k[1], false);
        
        final SpellAbility evokedSpell = new Spell(card, evokedCost) {
            private static final long serialVersionUID = -1598664196463358630L;

            @Override
            public void resolve() {
                card.setEvoked(true);
                Singletons.getModel().getGame().getAction().moveToPlay(card);
            }

            @Override
            public boolean canPlayAI() {
                if (!SpellPermanent.checkETBEffects(card, (AIPlayer) this.getActivatingPlayer())) {
                    return false;
                }
                return super.canPlayAI();
            }
        };
        card.removeIntrinsicKeyword(evokeKeyword);
        final StringBuilder desc = new StringBuilder();
        desc.append("Evoke ").append(evokedCost);
        desc.append(" (You may cast this spell for its evoke cost. ");
        desc.append("If you do, when it enters the battlefield, sacrifice it.)");

        evokedSpell.setDescription(desc.toString());

        final StringBuilder sb = new StringBuilder();
        sb.append(card.getName()).append(" (Evoked)");
        evokedSpell.setStackDescription(sb.toString());
        evokedSpell.setBasicSpell(false);
        return evokedSpell;
    }

    private static final Map<String,String> emptyMap = new TreeMap<String,String>();
    public static void setupETBReplacementAbility(SpellAbility sa) {
        sa.appendSubAbility(new AbilitySub(ApiType.InternalEtbReplacement, sa.getSourceCard(), null, emptyMap));
        // ETBReplacementMove(sa.getSourceCard(), null));
    }

    /**
     * <p>
     * hasKeyword.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @param k
     *            a {@link java.lang.String} object.
     * @return a int.
     */
    public static final int hasKeyword(final Card c, final String k) {
        return hasKeyword(c, k, 0);
    }

    /**
     * <p>
     * hasKeyword.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @param k
     *            a {@link java.lang.String} object.
     * @param startPos
     *            a int.
     * @return a int.
     */
    private static final int hasKeyword(final Card c, final String k, final int startPos) {
        final List<String> a = c.getKeyword();
        for (int i = startPos; i < a.size(); i++) {
            if (a.get(i).startsWith(k)) {
                return i;
            }
        }

        return -1;
    }

    /**
     * <p>
     * parseKeywords.
     * </p>
     * Pulling out the parsing of keywords so it can be used by the token
     * generator
     * 
     * @param card
     *            a {@link forge.Card} object.
     * @param cardName
     *            a {@link java.lang.String} object.
     * 
     */
    public static final void parseKeywords(final Card card, final String cardName) {
        if (card.hasKeyword("CARDNAME enters the battlefield tapped unless you control two or fewer other lands.")) {
            card.addComesIntoPlayCommand(new Command() {
                private static final long serialVersionUID = 6436821515525468682L;

                @Override
                public void run() {
                    final List<Card> lands = card.getController().getLandsInPlay();
                    lands.remove(card);
                    if (!(lands.size() <= 2)) {
                        // it enters the battlefield this way, and should not
                        // fire triggers
                        card.setTapped(true);
                    }
                }
            });
        }
        if (hasKeyword(card, "CARDNAME enters the battlefield tapped unless you control a") != -1) {
            final int n = hasKeyword(card,
                    "CARDNAME enters the battlefield tapped unless you control a");
            final String parse = card.getKeyword().get(n).toString();

            String splitString;
            if (parse.contains(" or a ")) {
                splitString = " or a ";
            } else {
                splitString = " or an ";
            }

            final String[] types = parse.substring(60, parse.length() - 1).split(splitString);

            card.addComesIntoPlayCommand(new Command() {
                private static final long serialVersionUID = 403635232455049834L;

                @Override
                public void run() {
                    final List<Card> clICtrl = card.getOwner().getCardsIn(ZoneType.Battlefield);

                    boolean fnd = false;

                    for (int i = 0; i < clICtrl.size(); i++) {
                        final Card c = clICtrl.get(i);
                        for (final String type : types) {
                            if (c.isType(type.trim())) {
                                fnd = true;
                            }
                        }
                    }

                    if (!fnd) {
                        // it enters the battlefield this way, and should not
                        // fire triggers
                        card.setTapped(true);
                    }
                }
            });
        }
        if (hasKeyword(card, "Sunburst") != -1) {
            final Command sunburstCIP = new Command() {
                private static final long serialVersionUID = 1489845860231758299L;

                @Override
                public void run() {
                    if (card.isCreature()) {
                        card.addCounter(CounterType.P1P1, card.getSunburstValue(), true);
                    } else {
                        card.addCounter(CounterType.CHARGE, card.getSunburstValue(), true);
                    }

                }
            };

            final Command sunburstLP = new Command() {
                private static final long serialVersionUID = -7564420917490677427L;

                @Override
                public void run() {
                    card.setSunburstValue(0);
                }
            };

            card.addComesIntoPlayCommand(sunburstCIP);
            card.addLeavesPlayCommand(sunburstLP);
        }

        // Enforce the "World rule"
        if (card.isType("World")) {
            final Command intoPlay = new Command() {
                private static final long serialVersionUID = 6536398032388958127L;

                @Override
                public void run() {
                    final List<Card> cardsInPlay = CardLists.getType(Singletons.getModel().getGame().getCardsIn(ZoneType.Battlefield), "World");
                    cardsInPlay.remove(card);
                    for (int i = 0; i < cardsInPlay.size(); i++) {
                        Singletons.getModel().getGame().getAction().sacrificeDestroy(cardsInPlay.get(i));
                    }
                } // execute()
            }; // Command
            card.addComesIntoPlayCommand(intoPlay);
        }

        if (hasKeyword(card, "Morph") != -1) {
            final int n = hasKeyword(card, "Morph");
            if (n != -1) {

                final String parse = card.getKeyword().get(n).toString();
                Map<String, String> sVars = card.getSVars();

                final String[] k = parse.split(":");
                final Cost cost = new Cost(k[1], true);

                card.addSpellAbility(abilityMorphDown(card));

                card.turnFaceDown();

                card.addSpellAbility(abilityMorphUp(card, cost));
                card.setSVars(sVars); // for Warbreak Trumpeter.

                card.setState(CardCharacteristicName.Original);
            }
        } // Morph

        if (hasKeyword(card, "Unearth") != -1) {
            final int n = hasKeyword(card, "Unearth");
            if (n != -1) {
                final String parse = card.getKeyword().get(n).toString();
                // card.removeIntrinsicKeyword(parse);

                final String[] k = parse.split(":");

                final String manacost = k[1];

                card.addSpellAbility(abilityUnearth(card, manacost));
            }
        } // unearth

        if (hasKeyword(card, "Madness") != -1) {
            final int n = hasKeyword(card, "Madness");
            if (n != -1) {
                final String parse = card.getKeyword().get(n).toString();
                // card.removeIntrinsicKeyword(parse);

                final String[] k = parse.split(":");
                card.setMadnessCost(k[1]);
            }
        } // madness

        if (hasKeyword(card, "Miracle") != -1) {
            final int n = hasKeyword(card, "Miracle");
            if (n != -1) {
                final String parse = card.getKeyword().get(n).toString();
                // card.removeIntrinsicKeyword(parse);

                final String[] k = parse.split(":");
                card.setMiracleCost(k[1]);
            }
        } // miracle

        if (hasKeyword(card, "Devour") != -1) {
            final int n = hasKeyword(card, "Devour");
            if (n != -1) {

                final String parse = card.getKeyword().get(n).toString();
                // card.removeIntrinsicKeyword(parse);

                final String[] k = parse.split(":");
                final String magnitude = k[1];

                // final String player = card.getController();
                final int[] numCreatures = new int[1];

                final Command intoPlay = new Command() {
                    private static final long serialVersionUID = -7530312713496897814L;

                    @Override
                    public void run() {
                        final List<Card> creats = card.getController().getCreaturesInPlay();
                        creats.remove(card);
                        // System.out.println("Creats size: " + creats.size());

                        card.clearDevoured();
                        if (card.getController().isHuman()) {
                            if (creats.size() > 0) {
                                final List<Card> selection = GuiChoose.order("Devour", "Devouring", -1, creats, null, card);
                                numCreatures[0] = selection.size();

                                for (Object o : selection) {
                                    Card dinner = (Card) o;
                                    card.addDevoured(dinner);
                                    Singletons.getModel().getGame().getAction().sacrifice(dinner, null);
                                    final HashMap<String, Object> runParams = new HashMap<String, Object>();
                                    runParams.put("Devoured", dinner);
                                    card.getController().getGame().getTriggerHandler()
                                    .runTrigger(TriggerType.Devoured, runParams, false);
                                }
                            }
                        } // human
                        else {
                            int count = 0;
                            for (int i = 0; i < creats.size(); i++) {
                                final Card c = creats.get(i);
                                if ((c.getNetAttack() <= 1) && ((c.getNetAttack() + c.getNetDefense()) <= 3)) {
                                    card.addDevoured(c);
                                    Singletons.getModel().getGame().getAction().sacrifice(c, null);
                                    final HashMap<String, Object> runParams = new HashMap<String, Object>();
                                    runParams.put("Devoured", c);
                                    card.getController().getGame().getTriggerHandler()
                                    .runTrigger(TriggerType.Devoured, runParams, false);
                                    count++;
                                }
                            }
                            numCreatures[0] = count;
                        }
                        final int multiplier = magnitude.equals("X") ? AbilityUtils.calculateAmount(card, magnitude, null)
                                : Integer.parseInt(magnitude);
                        final int totalCounters = numCreatures[0] * multiplier;

                        card.addCounter(CounterType.P1P1, totalCounters, true);

                    }
                };
                card.addComesIntoPlayCommand(intoPlay);
            }
        } // Devour

        if (hasKeyword(card, "Modular") != -1) {
            final int n = hasKeyword(card, "Modular");
            if (n != -1) {
                final String parse = card.getKeyword().get(n).toString();
                card.getKeyword().remove(parse);

                final int m = Integer.parseInt(parse.substring(8));

                card.addIntrinsicKeyword("etbCounter:P1P1:" + m + ":no Condition:"
                        + "Modular " + m + " (This enters the battlefield with " + m + " +1/+1 counters on it. When it's put into a graveyard, you may put its +1/+1 counters on target artifact creature.)");

                final SpellAbility ability = new Ability(card, ManaCost.ZERO) {
                    @Override
                    public void resolve() {
                        final Card card2 = this.getTargetCard();
                        card2.addCounter(CounterType.P1P1, this.getSourceCard().getCounters(CounterType.P1P1), true);
                    } // resolve()
                };

                card.addDestroyCommand(new Command() {
                    private static final long serialVersionUID = 304026662487997331L;

                    @Override
                    public void run() {
                        final Player modularPlayer =  card.getController();
                        final List<Card> choices = Lists.newArrayList();
                        for(Card c : modularPlayer.getGame().getCardsIn(ZoneType.Battlefield)) {
                            if( c.isCreature() && c.isArtifact() && c.canBeTargetedBy(ability))
                                choices.add(c);
                        }

                        Card card2 = null;
                        // Target as Modular is Destroyed
                        if (modularPlayer.isComputer()) {
                            final List<Card> aiChoices = CardLists.filterControlledBy(choices, modularPlayer);
                            if (!aiChoices.isEmpty()) {
                                card2 = ComputerUtilCard.getBestCreatureAI(aiChoices);
                            }
                        } else {
                            InputSelectCards inp = new InputSelectCardsFromList(1, 1, choices);
                            inp.setCancelAllowed(true);
                            inp.setMessage("Select target artifact creature to give it +1/+1 counters from the dead " + card);
                            FThreads.setInputAndWait(inp);
                            if( !inp.hasCancelled() ) {
                                card2 = inp.getSelected().get(0);
                            }
                        }
                        ability.setTargetCard(card2);
                        if ( null != card2 ) {
                            String desc = String.format("Put %d +1/+1 counter/s from %s on %s", card.getCounters(CounterType.P1P1), card, card2);
                            ability.setStackDescription(desc);
                            modularPlayer.getGame().getStack().addSimultaneousStackEntry(ability);
                        }
                    }
                });
            }
        } // Modular

        /*
         * WARNING: must keep this keyword processing before etbCounter keyword
         * processing.
         */
        final int graft = hasKeyword(card, "Graft");
        if (graft != -1) {
            final String parse = card.getKeyword().get(graft).toString();

            final int m = Integer.parseInt(parse.substring(6));
            final String abStr = "AB$ MoveCounter | Cost$ 0 | Source$ Self | "
                    + "Defined$ TriggeredCard | CounterType$ P1P1 | CounterNum$ 1";
            card.setSVar("GraftTrig", abStr);

            String trigStr = "Mode$ ChangesZone | ValidCard$ Creature.Other | "
                + "Origin$ Any | Destination$ Battlefield"
                + " | TriggerZones$ Battlefield | OptionalDecider$ You | "
                + "IsPresent$ Card.Self+counters_GE1_P1P1 | "
                + "Execute$ GraftTrig | TriggerDescription$ "
                + "Whenever another creature enters the battlefield, you "
                + "may move a +1/+1 counter from this creature onto it.";
            final Trigger myTrigger = TriggerHandler.parseTrigger(trigStr, card, true);
            card.addTrigger(myTrigger);

            card.addIntrinsicKeyword("etbCounter:P1P1:" + m);
        }

        final int bloodthirst = hasKeyword(card, "Bloodthirst");
        if (bloodthirst != -1) {
            final String numCounters = card.getKeyword().get(bloodthirst).split(" ")[1];
            String desc = "Bloodthirst "
                    + numCounters + " (If an opponent was dealt damage this turn, this creature enters the battlefield with "
                    + numCounters + " +1/+1 counters on it.)";
            if (numCounters.equals("X")) {
                desc = "Bloodthirst X (This creature enters the battlefield with X +1/+1 counters on it, "
                        + "where X is the damage dealt to your opponents this turn.)";
                card.setSVar("X", "Count$BloodthirstAmount");
            }

            card.addIntrinsicKeyword("etbCounter:P1P1:" + numCounters + ":Bloodthirst$ True:" + desc);
        } // bloodthirst

        final int storm = card.getKeywordAmount("Storm");
        for (int i = 0; i < storm; i++) {
            final StringBuilder trigScript = new StringBuilder(
                    "Mode$ SpellCast | ValidCard$ Card.Self | Execute$ Storm "
                            + "| TriggerDescription$ Storm (When you cast this spell, "
                            + "copy it for each spell cast before it this turn.)");

            card.setSVar("Storm", "AB$ CopySpellAbility | Cost$ 0 | Defined$ TriggeredSpellAbility | Amount$ StormCount");
            card.setSVar("StormCount", "Count$StormCount");
            final Trigger stormTrigger = TriggerHandler.parseTrigger(trigScript.toString(), card, true);

            card.addTrigger(stormTrigger);
        } // Storm
    }

} // end class CardFactoryUtil
