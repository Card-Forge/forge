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
package forge.game.card;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import forge.card.mana.ManaAtom;
import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;

import forge.GameCommand;
import forge.card.CardStateName;
import forge.card.CardType;
import forge.card.ColorSet;
import forge.card.MagicColor;
import forge.card.mana.ManaCost;
import forge.card.mana.ManaCostParser;
import forge.card.mana.ManaCostShard;
import forge.game.Game;
import forge.game.GameEntity;
import forge.game.GameLogEntryType;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.Card.SplitCMCMode;
import forge.game.card.CardPredicates.Presets;
import forge.game.cost.Cost;
import forge.game.cost.CostPayment;
import forge.game.event.GameEventCardStatsChanged;
import forge.game.keyword.Keyword;
import forge.game.keyword.KeywordsChange;
import forge.game.phase.PhaseHandler;
import forge.game.player.Player;
import forge.game.replacement.ReplacementEffect;
import forge.game.replacement.ReplacementHandler;
import forge.game.replacement.ReplacementLayer;
import forge.game.spellability.Ability;
import forge.game.spellability.AbilityStatic;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.OptionalCost;
import forge.game.spellability.Spell;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityRestriction;
import forge.game.spellability.SpellPermanent;
import forge.game.spellability.TargetRestrictions;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerHandler;
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;
import forge.util.Aggregates;
import forge.util.collect.FCollectionView;
import forge.util.Lang;

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
     * abilityMorphDown.
     * </p>
     * 
     * @param sourceCard
     *            a {@link forge.game.card.Card} object.
     * @return a {@link forge.game.spellability.SpellAbility} object.
     */
    public static SpellAbility abilityMorphDown(final Card sourceCard) {
        final Spell morphDown = new Spell(sourceCard, new Cost(ManaCost.THREE, false)) {
            private static final long serialVersionUID = -1438810964807867610L;

            @Override
            public void resolve() {
                Card c = sourceCard.getGame().getAction().moveToPlay(sourceCard);
                c.setPreFaceDownState(CardStateName.Original);
            }

            @Override
            public boolean canPlay() {
                CardStateName stateBackup = sourceCard.getCurrentStateName();
                sourceCard.setState(CardStateName.FaceDown, false);
                boolean success = super.canPlay();
                sourceCard.setState(stateBackup, false);
                return success;
            }
        };

        morphDown.setDescription("(You may cast this card face down as a 2/2 creature for {3}.)");
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
     *            a {@link forge.game.card.Card} object.
     * @param cost
     *            a {@link forge.game.cost.Cost} object.
     * @return a {@link forge.game.spellability.AbilityActivated} object.
     */
    public static AbilityStatic abilityMorphUp(final Card sourceCard, final Cost cost, final boolean mega) {
        final AbilityStatic morphUp = new AbilityStatic(sourceCard, cost, null) {

            @Override
            public void resolve() {
                if (sourceCard.turnFaceUp()) {
                    String sb = this.getActivatingPlayer() + " has unmorphed " + sourceCard.getName();
                    sourceCard.getGame().getGameLog().add(GameLogEntryType.STACK_RESOLVE, sb);
                    sourceCard.getGame().fireEvent(new GameEventCardStatsChanged(sourceCard));
                }
                if (mega) {
                	sourceCard.addCounter(CounterType.P1P1, 1, true);
                }
            }

            @Override
            public boolean canPlay() {
                return sourceCard.getController().equals(this.getActivatingPlayer()) && sourceCard.isFaceDown()
                        && sourceCard.isInPlay() && CostPayment.canPayAdditionalCosts(cost, this);
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
        morphUp.setIsMorphUp(true);

        return morphUp;
    }

    public static AbilityStatic abilityManifestFaceUp(final Card sourceCard, final ManaCost manaCost) {
        final Cost cost = new Cost(manaCost, true);

        final AbilityStatic manifestUp = new AbilityStatic(sourceCard, cost, null) {

            @Override
            public void resolve() {
                if (sourceCard.turnFaceUp(true, true)) {
                    String sb = this.getActivatingPlayer() + " has unmanifested " + sourceCard.getName();
                    sourceCard.getGame().getGameLog().add(GameLogEntryType.STACK_RESOLVE, sb);
                    sourceCard.getGame().fireEvent(new GameEventCardStatsChanged(sourceCard));
                }
            }

            @Override
            public boolean canPlay() {
                return sourceCard.getController().equals(this.getActivatingPlayer()) && sourceCard.isFaceDown()
                        && sourceCard.isInPlay() && sourceCard.isManifested();
            }

        }; // manifest_up

        String costDesc = cost.toString();
        // get rid of the ": " at the end
        costDesc = costDesc.substring(0, costDesc.length() - 2);
        final StringBuilder sb = new StringBuilder();
        sb.append("Unmanifest");
        if (!cost.isOnlyManaCost()) {
            sb.append(" -");
        }
        sb.append(" ").append(costDesc).append(" (Turn this face up any time for its mana cost.)");
        manifestUp.setDescription(sb.toString());

        final StringBuilder sbStack = new StringBuilder();
        sbStack.append(sourceCard.getName()).append(" - turn this card face up.");
        manifestUp.setStackDescription(sbStack.toString());
        manifestUp.setIsManifestUp(true);

        return manifestUp;
    }

    public static boolean handleHiddenAgenda(Player player, Card card) {
        SpellAbility sa = new SpellAbility.EmptySa(card);
        sa.getMapParams().put("AILogic", card.getSVar("AgendaLogic"));
        Predicate<PaperCard> cpp = Predicates.alwaysTrue();
        //Predicate<Card> pc = Predicates.in(player.getAllCards());
        // TODO This would be better to send in the player's deck, not all cards
        String name = player.getController().chooseCardName(sa, cpp, "Card", "Name a card for " + card.getName());
        if (name == null || name.isEmpty()) {
            return false;
        }

        card.setNamedCard(name);
        card.turnFaceDown();
        // Hopefully face down also hides the named card?
        card.addSpellAbility(abilityRevealHiddenAgenda(card));
        return true;
    }

    public static AbilityStatic abilityRevealHiddenAgenda(final Card sourceCard) {
        final AbilityStatic revealAgenda = new AbilityStatic(sourceCard, Cost.Zero, null) {

            @Override
            public void resolve() {
                if (sourceCard.turnFaceUp()) {
                    String sb = this.getActivatingPlayer() + " has revealed " + sourceCard.getName() + " with the chosen name " + sourceCard.getNamedCard();
                    sourceCard.getGame().getGameLog().add(GameLogEntryType.STACK_RESOLVE, sb);
                    sourceCard.getGame().fireEvent(new GameEventCardStatsChanged(sourceCard));
                }
            }

            @Override
            public boolean canPlay() {
                return sourceCard.getController().equals(this.getActivatingPlayer()) && sourceCard.isFaceDown()
                        && sourceCard.isInZone(ZoneType.Command);
            }

            // TODO When should the AI activate this?

        }; // reveal hidden agenda

        revealAgenda.setDescription("Reveal this Hidden Agenda at any time. ");
        return revealAgenda;
    }

    /**
     * <p>
     * abilityCycle.
     * </p>
     * 
     * @param sourceCard
     *            a {@link forge.game.card.Card} object.
     * @param cycleCost
     *            a {@link java.lang.String} object.
     * @return a {@link forge.game.spellability.SpellAbility} object.
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
     *            a {@link forge.game.card.Card} object.
     * @param cycleCost
     *            a {@link java.lang.String} object.
     * @param type
     *            a {@link java.lang.String} object.
     * @return a {@link forge.game.spellability.SpellAbility} object.
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
     * abilitySuspendStatic.
     * </p>
     * 
     * @param sourceCard
     *            a {@link forge.game.card.Card} object.
     * @param suspendCost
     *            a {@link java.lang.String} object.
     * @param timeCounters
     *            a int.
     * @return a {@link forge.game.spellability.SpellAbility} object.
     */
    public static SpellAbility abilitySuspendStatic(final Card sourceCard, final String suspendCost, final String timeCounters) {
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
            public void resolve() {
                final Game game = sourceCard.getGame();
                final Card c = game.getAction().exile(sourceCard);

                int counters = AbilityUtils.calculateAmount(c, timeCounters, this);
                c.addCounter(CounterType.TIME, counters, true);
                
                String sb = String.format("%s has suspended %s with %d time counters on it.", this.getActivatingPlayer(), c.getName(), counters);
                game.getGameLog().add(GameLogEntryType.STACK_RESOLVE, sb);
            }
        };
        final StringBuilder sbDesc = new StringBuilder();
        sbDesc.append("Suspend ").append(timeCounters).append(" - ").append(cost.toSimpleString());
        sbDesc.append(" (Rather than cast CARDNAME from your hand, you may pay cost and exile it with three time counters on it. At the beginning of your upkeep, remove a time counter. When the last is removed, cast it without paying its mana cost.)");
        suspend.setDescription(sbDesc.toString());

        String svar = "X"; // emulate "References X" here
        suspend.setSVar(svar, sourceCard.getSVar(svar));

        final StringBuilder sbStack = new StringBuilder();
        sbStack.append(sourceCard.getName()).append(" suspending for ").append(timeCounters).append(" turns.)");
        suspend.setStackDescription(sbStack.toString());

        suspend.getRestrictions().setZone(ZoneType.Hand);
        return suspend;
    } // abilitySuspendStatic()

    public static void addSuspendUpkeepTrigger(Card card) {
        //upkeep trigger
        StringBuilder upkeepTrig = new StringBuilder();
        String triggerSvar = "SuspendTrigSV"; // FIXME: the SVars were previously random UUIDs (which caused the keyword not to work). 
        String removeCounterSvar = "SuspendRemoveCtrSV";

        upkeepTrig.append("Mode$ Phase | Phase$ Upkeep | ValidPlayer$ You | TriggerZones$ Exile | CheckSVar$ ");
        upkeepTrig.append(triggerSvar);
        upkeepTrig.append(" | SVarCompare$ GE1 | References$ ");
        upkeepTrig.append(triggerSvar);
        upkeepTrig.append(" | Execute$ ");
        upkeepTrig.append(removeCounterSvar);
        // Mark this trigger as Secondary, so it's not displayed twice
        upkeepTrig.append(" | Secondary$ True | TriggerDescription$ At the beginning of your upkeep, if this card is suspended, remove a time counter from it");

        card.setSVar(removeCounterSvar, "DB$ RemoveCounter | Defined$ Self | CounterType$ TIME | CounterNum$ 1");
        card.setSVar(triggerSvar,"Count$ValidExile Card.Self+suspended");

        final Trigger parsedUpkeepTrig = TriggerHandler.parseTrigger(upkeepTrig.toString(), card, true);
        card.addTrigger(parsedUpkeepTrig);
    }

    public static void addSuspendPlayTrigger(Card card) {
        //play trigger
        StringBuilder playTrig = new StringBuilder();
        String playSvar = "SuspendPlaySV"; // FIXME: the SVars were previously random UUIDs (which caused the keyword not to work).

        playTrig.append("Mode$ CounterRemoved | TriggerZones$ Exile | ValidCard$ Card.Self | CounterType$ TIME | NewCounterAmount$ 0 | Secondary$ True | Execute$ ");
        playTrig.append(playSvar);
        playTrig.append(" | TriggerDescription$ When the last time counter is removed from this card, if it's exiled, play it without paying its mana cost if able.  ");
        playTrig.append("If you can't, it remains exiled. If you cast a creature spell this way, it gains haste until you lose control of the spell or the permanent it becomes.");

        StringBuilder playWithoutCost = new StringBuilder();
        playWithoutCost.append("DB$ Play | Defined$ Self | WithoutManaCost$ True | SuspendCast$ True");

        final Trigger parsedPlayTrigger = TriggerHandler.parseTrigger(playTrig.toString(), card, true);
        card.addTrigger(parsedPlayTrigger);

        card.setSVar(playSvar, playWithoutCost.toString());
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
            // cost starts with "generic" number cost
            int cost = Integer.parseInt(tokenized[0]);
            cost = multiplier * cost;
            tokenized[0] = "" + cost;
            sb.append(tokenized[0]);
        }
        else {
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
            }
            else {
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
            }
            else {
                for (int j = 0; j < multiplier; j++) {
                    // tokenized[i] = tokenized[i] + " " + tokenized[i];
                    sb.append((" "));
                    sb.append(tokenized[i]);
                }
            }
        }

        String result = sb.toString();
        //System.out.println("result: " + result);
        result = result.trim();
        return result;
    }

    /**
     * <p>
     * isTargetStillValid.
     * </p>
     * 
     * @param ability
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @param target
     *            a {@link forge.game.card.Card} object.
     * @return a boolean.
     */
    public static boolean isTargetStillValid(final SpellAbility ability, final Card target) {
        Zone zone = target.getGame().getZoneOf(target);
        if (zone == null) {
            return false; // for tokens that disappeared
        }

        final Card source = ability.getHostCard();
        final TargetRestrictions tgt = ability.getTargetRestrictions();
        if (tgt != null) {
            // Reconfirm the Validity of a TgtValid, or if the Creature is still
            // a Creature
            if (tgt.doesTarget()
                    && !target.isValid(tgt.getValidTgts(), ability.getActivatingPlayer(), ability.getHostCard(), ability)) {
                return false;
            }

            // Check if the target is in the zone it needs to be in to be targeted
            if (!tgt.getZone().contains(zone.getZoneType())) {
                return false;
            }
        }
        else {
            // If an Aura's target is removed before it resolves, the Aura
            // fizzles
            if (source.isAura() && !target.isInZone(ZoneType.Battlefield)) {
                return false;
            }
        }

        // Make sure it's still targetable as well
        return ability.canTarget(target);
    }

    // does "target" have protection from "card"?
    /**
     * <p>
     * hasProtectionFrom.
     * </p>
     * 
     * @param card
     *            a {@link forge.game.card.Card} object.
     * @param target
     *            a {@link forge.game.card.Card} object.
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
     *            a {@link forge.game.card.Card} object.
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
     *            a {@link forge.game.card.Card} object.
     * @param sa
     *            the sa
     * @return a boolean.
     */
    public static boolean isCounterableBy(final Card c, final SpellAbility sa) {
        if (!isCounterable(c)) {
            return false;
        }
        // Autumn's Veil
        if (c.hasKeyword("CARDNAME can't be countered by blue or black spells.") && sa.isSpell() 
                && (sa.getHostCard().isBlack() || sa.getHostCard().isBlue())) {
            return false;
        }
        return true;
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
     * @param expression
     *            a {@link java.lang.String} object.
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
     * @param objects
     *            a {@link java.util.ArrayList} object.
     * @param s
     *            a {@link java.lang.String} object.
     * @param source
     *            a {@link forge.game.card.Card} object.
     * @return a int.
     */
    public static int objectXCount(final List<?> objects, final String s, final Card source) {
        if (objects.isEmpty()) {
            return 0;
        }

        if (s.startsWith("Valid")) {
            final CardCollection cards = new CardCollection();
            for (Object o : objects) {
                if (o instanceof Card) {
                    cards.add((Card) o);
                }
            }
            return CardFactoryUtil.handlePaid(cards, s, source);
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
     *            a {@link forge.game.card.Card} object.
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
                final int current = playerXProperty(player, s.replace("Highest", ""), source);
                if (current > n) {
                    n = current;
                }
            }

            return doXMath(n, m, source);
        }

        if (l[0].startsWith("Lowest")) {
            n = 99999; // if no players have fewer than 99999 valids, the game is frozen anyway
            for (final Player player : players) {
                final int current = playerXProperty(player, s.replace("Lowest", ""), source);
                if (current < n) {
                    n = current;
                }
            }
            return doXMath(n, m, source);
        }


        final String[] sq;
        sq = l[0].split("\\.");

        // the number of players passed in
        if (sq[0].equals("Amount")) {
            return doXMath(players.size(), m, source);
        }

        if (sq[0].startsWith("HasProperty")) {
            int totPlayer = 0;
            String property = sq[0].substring(11);
            for (Player p : players) {
                if (p.hasProperty(property, source.getController(), source, null)) {
                    totPlayer++;
                }
            }
            return doXMath(totPlayer, m, source);
        }

        if (sq[0].contains("DamageThisTurn")) {
            int totDmg = 0;
            for (Player p : players) {
                totDmg += p.getAssignedDamage();
            }
            return doXMath(totDmg, m, source);
        }

        if (players.size() > 0) {
            return playerXProperty(players.get(0), s, source);
        }

        return doXMath(n, m, source);
    }

    public static int playerXProperty(final Player player, final String s, final Card source) {
        final String[] l = s.split("/");
        final String m = extractOperators(s);
        
        final Game game = player.getGame();
        
        // count valid cards in any specified zone/s
        if (l[0].startsWith("Valid") && !l[0].contains("Valid ")) {
            String[] lparts = l[0].split(" ", 2);
            final List<ZoneType> vZone = ZoneType.listValueOf(lparts[0].split("Valid")[1]);
            String restrictions = l[0].replace(lparts[0] + " ", "");
            final String[] rest = restrictions.split(",");
            CardCollection cards = CardLists.getValidCards(game.getCardsIn(vZone), rest, player, source, null);
            return doXMath(cards.size(), m, source);
        }

        // count valid cards on the battlefield
        if (l[0].startsWith("Valid ")) {
            final String restrictions = l[0].substring(6);
            final String[] rest = restrictions.split(",");
            CardCollection cardsonbattlefield = CardLists.getValidCards(game.getCardsIn(ZoneType.Battlefield), rest, player, source, null);
            return doXMath(cardsonbattlefield.size(), m, source);
        }

        final String[] sq = l[0].split("\\.");
        final String value = sq[0];

        if (value.contains("CardsInHand")) {
            return doXMath(player.getCardsIn(ZoneType.Hand).size(), m, source);
        }

        if (value.contains("NumPowerSurgeLands")) {
            return doXMath(player.getNumPowerSurgeLands(), m, source);
        }

        if (value.contains("DomainPlayer")) {
            int n = 0;
            final CardCollectionView someCards = player.getCardsIn(ZoneType.Battlefield);
            final List<String> basic = MagicColor.Constant.BASIC_LANDS;

            for (int i = 0; i < basic.size(); i++) {
                if (!CardLists.getType(someCards, basic.get(i)).isEmpty()) {
                    n++;
                }
            }
            return doXMath(n, m, source);
        }

        if (value.contains("CardsInLibrary")) {
            return doXMath(player.getCardsIn(ZoneType.Library).size(), m, source);
        }

        if (value.contains("CardsInGraveyard")) {
            return doXMath(player.getCardsIn(ZoneType.Graveyard).size(), m, source);
        }
        if (value.contains("LandsInGraveyard")) {
            return doXMath(CardLists.getType(player.getCardsIn(ZoneType.Graveyard), "Land").size(), m, source);
        }

        if (value.contains("CreaturesInPlay")) {
            return doXMath(player.getCreaturesInPlay().size(), m, source);
        }

        if (value.contains("CardsInPlay")) {
            return doXMath(player.getCardsIn(ZoneType.Battlefield).size(), m, source);
        }

        if (value.contains("LifeTotal")) {
            return doXMath(player.getLife(), m, source);
        }

        if (value.contains("LifeLostThisTurn")) {
            return doXMath(player.getLifeLostThisTurn(), m, source);
        }

        if (value.contains("LifeLostLastTurn")) {
            return doXMath(player.getLifeLostLastTurn(), m, source);
        }

        if (value.contains("LifeGainedThisTurn")) {
            return doXMath(player.getLifeGainedThisTurn(), m, source);
        }

        if (value.contains("PoisonCounters")) {
            return doXMath(player.getPoisonCounters(), m, source);
        }

        if (value.contains("TopOfLibraryCMC")) {
            return doXMath(Aggregates.sum(player.getCardsIn(ZoneType.Library, 1), CardPredicates.Accessors.fnGetCmc), m, source);
        }

        if (value.contains("LandsPlayed")) {
            return doXMath(player.getLandsPlayedThisTurn(), m, source);
        }

        if (value.contains("CardsDrawn")) {
            return doXMath(player.getNumDrawnThisTurn(), m, source);
        }
        
        if (value.contains("CardsDiscardedThisTurn")) {
            return doXMath(player.getNumDiscardedThisTurn(), m, source);
        }

        if (value.contains("AttackersDeclared")) {
            return doXMath(player.getAttackersDeclaredThisTurn(), m, source);
        }

        if (value.equals("DamageDoneToPlayerBy")) {
            return doXMath(source.getDamageDoneToPlayerBy(player.getName()), m, source);
        }

        if (value.contains("DamageToOppsThisTurn")) {
            int oppDmg = 0;
            for (Player opp : player.getOpponents()) {
                oppDmg += opp.getAssignedDamage();
            }
            return doXMath(oppDmg, m, source);
        }
        
        return doXMath(0, m, source);
    }

    /**
     * <p>
     * Parse non-mana X variables.
     * </p>
     * 
     * @param c
     *            a {@link forge.game.card.Card} object.
     * @param expression
     *            a {@link java.lang.String} object.
     * @return a int.
     */
    public static int xCount(final Card c, final String expression) {
        if (StringUtils.isBlank(expression)) {
            return 0;
        }
        if (StringUtils.isNumeric(expression)) {
            return Integer.parseInt(expression);
        }

        final Player cc = c.getController();
        final Game game = c.getGame();
        final Player activePlayer = game.getPhaseHandler().getPlayerTurn();

        final String[] l = expression.split("/");
        final String m = extractOperators(expression);

        // accept straight numbers
        if (l[0].startsWith("Number$")) {
            final String number = l[0].substring(7);
            if (number.equals("ChosenNumber")) {
                return doXMath(c.getChosenNumber(), m, c);
            }
            return doXMath(Integer.parseInt(number), m, c);
        }

        if (l[0].startsWith("Count$")) {
            l[0] = l[0].substring(6);
        }

        if (l[0].startsWith("SVar$")) {
            return doXMath(xCount(c, c.getSVar(l[0].substring(5))), m, c);
        }

        if (l[0].startsWith("Controller$")) {
            return playerXProperty(cc, l[0].substring(11), c);
        }

        // Manapool
        if (l[0].startsWith("ManaPool")) {
            final String color = l[0].split(":")[1];
            if (color.equals("All")) {
                return cc.getManaPool().totalMana();
            }
            return cc.getManaPool().getAmountOfColor(ManaAtom.fromName(color));
        }

        // count valid cards in any specified zone/s
        if (l[0].startsWith("Valid")) {
            String[] lparts = l[0].split(" ", 2);
            final String[] rest = lparts[1].split(",");

            final CardCollectionView cardsInZones = lparts[0].length() > 5 
                ? game.getCardsIn(ZoneType.listValueOf(lparts[0].substring(5)))
                : game.getCardsIn(ZoneType.Battlefield);

            CardCollection cards = CardLists.getValidCards(cardsInZones, rest, cc, c, null);
            return doXMath(cards.size(), m, c);
        }

        if (l[0].startsWith("ImprintedCardManaCost") && !c.getImprintedCards().isEmpty()) {
            return c.getImprintedCards().get(0).getCMC();
        }

        if (l[0].startsWith("GreatestPower_")) {
            final String restriction = l[0].substring(14);
            final String[] rest = restriction.split(",");
            CardCollection list = CardLists.getValidCards(cc.getGame().getCardsIn(ZoneType.Battlefield), rest, cc, c, null);
            int highest = 0;
            for (final Card crd : list) {
                if (crd.getNetPower() > highest) {
                    highest = crd.getNetPower();
                }
            }
            return highest;
        }

        if (l[0].startsWith("GreatestToughness_")) {
            final String restriction = l[0].substring(18);
            final String[] rest = restriction.split(",");
            CardCollection list = CardLists.getValidCards(cc.getGame().getCardsIn(ZoneType.Battlefield), rest, cc, c, null);
            int highest = 0;
            for (final Card crd : list) {
                if (crd.getNetToughness() > highest) {
                    highest = crd.getNetToughness();
                }
            }
            return highest;
        }

        if (l[0].startsWith("HighestCMC_")) {
            final String restriction = l[0].substring(11);
            final String[] rest = restriction.split(",");
            CardCollection list = CardLists.getValidCards(cc.getGame().getCardsInGame(), rest, cc, c, null);
            int highest = 0;
            for (final Card crd : list) {
                if (crd.isSplitCard()) {
                    if (crd.getCMC(Card.SplitCMCMode.LeftSplitCMC) > highest) {
                        highest = crd.getCMC(Card.SplitCMCMode.LeftSplitCMC);
                    }
                    if (crd.getCMC(Card.SplitCMCMode.RightSplitCMC) > highest) {
                        highest = crd.getCMC(Card.SplitCMCMode.RightSplitCMC);
                    }
                }
                else {
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
            CardCollection list = CardLists.getValidCards(cc.getGame().getCardsInGame(), rest, cc, c, null);
            for (final Card card : list) {
                if (!crdname.contains(card.getName())) {
                    crdname.add(card.getName());
                }
            }
            return doXMath(crdname.size(), m, c);
        }

        if (l[0].startsWith("RememberedSize")) {
            return doXMath(c.getRememberedCount(), m, c);
        }

        if (l[0].startsWith("RememberedNumber")) {
            int num = 0;
            for (final Object o : c.getRemembered()) {
                if (o instanceof Integer) {
                    num += (Integer) o;
                }
            }
            return doXMath(num, m, c);
        }

        // Count$CountersAdded <CounterType> <ValidSource>
        if (l[0].startsWith("CountersAdded")) {
            final String[] components = l[0].split(" ", 3);
            final CounterType counterType = CounterType.valueOf(components[1]);
            String restrictions = components[2];
            final String[] rest = restrictions.split(",");
            CardCollection candidates = CardLists.getValidCards(game.getCardsInGame(), rest, cc, c, null);

            int added = 0;
            for (final Card counterSource : candidates) {
                added += c.getCountersAddedBy(counterSource, counterType);
            }
            return doXMath(added, m, c);
        }

        if (l[0].startsWith("CommanderCastFromCommandZone")) {
            // Read SVar CommanderCostRaise from Commander Effect
            Card commeff = CardLists.filter(cc.getCardsIn(ZoneType.Command),
                    CardPredicates.nameEquals("Commander Effect")).get(0);
            return doXMath(xCount(commeff, commeff.getSVar("CommanderCostRaise")), "DivideEvenlyDown.2", c);
        }
        
        if (l[0].startsWith("MostProminentCreatureType")) {
            String restriction = l[0].split(" ")[1];
            CardCollection list = CardLists.getValidCards(game.getCardsIn(ZoneType.Battlefield), restriction, cc, c);
            return doXMath(getMostProminentCreatureTypeSize(list), m, c);
        }

        if (l[0].startsWith("SecondMostProminentColor")) {
            String restriction = l[0].split(" ")[1];
            CardCollection list = CardLists.getValidCards(game.getCardsIn(ZoneType.Battlefield), restriction, cc, c);
            int[] colorSize = SortColorsFromList(list);
            return doXMath(colorSize[colorSize.length - 2], m, c);
        }

        if (l[0].startsWith("RolledThisTurn")) {
            return game.getPhaseHandler().getPlanarDiceRolledthisTurn();
        }

        //SacrificedThisTurn <type>
        if (l[0].startsWith("SacrificedThisTurn")) {
            CardCollectionView list = cc.getSacrificedThisTurn();
            if (l[0].contains(" ")) {
                final String[] components = l[0].split(" ", 2);
                list = CardLists.getValidCards(list, components[1], cc, c);
            }
            return list.size();
        }

        final String[] sq;
        sq = l[0].split("\\.");

        if (sq[0].contains("xPaid")) {
            return doXMath(c.getXManaCostPaid(), m, c);
        }

        if (sq[0].contains("xColorPaid")) {
            String[] attrs = sq[0].split(" ");
            String colors = "";
            for (int i = 1; i < attrs.length; i++) {
                colors += attrs[i];
            }
            return doXMath(c.getXManaCostPaidCount(colors), m, c);
        }


        if (sq[0].equals("YouDrewThisTurn")) {
            return doXMath(c.getController().getNumDrawnThisTurn(), m, c);
        }

        if (sq[0].equals("FirstSpellTotalManaSpent")) {
            try{
                return doXMath(c.getFirstSpellAbility().getTotalManaSpent(), m, c);
            } catch (NullPointerException e) {
                // This spell was not cast
                return 0;
            }

        }
        if (sq[0].equals("StormCount")) {
            return doXMath(game.getStack().getSpellsCastThisTurn().size() - 1, m, c);
        }
        if (sq[0].equals("DamageDoneThisTurn")) {
            return doXMath(c.getDamageDoneThisTurn(), m, c);
        }
        if (sq[0].equals("BloodthirstAmount")) {
            return doXMath(c.getController().getBloodthirstAmount(), m, c);
        }
        if (sq[0].equals("RegeneratedThisTurn")) {
            return doXMath(c.getRegeneratedThisTurn(), m, c);
        }
        
        // TriggeringObjects
        if (sq[0].startsWith("Triggered")) {
            return doXMath(xCount((Card) c.getTriggeringObject("Card"), sq[0].substring(9)), m, c);
        }

        if (sq[0].contains("YourStartingLife")) {
            return doXMath(cc.getStartingLife(), m, c);
        }

        if (sq[0].contains("YourLifeTotal")) {
            return doXMath(cc.getLife(), m, c);
        }
        if (sq[0].contains("OppGreatestLifeTotal")) {
            return doXMath(cc.getOpponentsGreatestLifeTotal(), m, c);
        }
        if (sq[0].contains("OppsAtLifeTotal")) {
        	final int lifeTotal = xCount(c, sq[1]);
        	int number = 0;
        	for (final Player opp : cc.getOpponents()) {
        		if (opp.getLife() == lifeTotal) {
        			number++;
        		}
        	}
        	return doXMath(number, m, c);
        }

        //  Count$TargetedLifeTotal (targeted player's life total)
        if (sq[0].contains("TargetedLifeTotal")) {
            // This doesn't work in some circumstances, since the active SA isn't passed through
            for (final SpellAbility sa : c.getCurrentState().getNonManaAbilities()) {
                final SpellAbility saTargeting = sa.getSATargetingPlayer();
                if (saTargeting != null) {
                    for (final Player tgtP : saTargeting.getTargets().getTargetPlayers()) {
                        return doXMath(tgtP.getLife(), m, c);
                    }
                }
            }
        }

        if (sq[0].contains("LifeYouLostThisTurn")) {
            return doXMath(cc.getLifeLostThisTurn(), m, c);
        }
        if (sq[0].contains("LifeYouGainedThisTurn")) {
            return doXMath(cc.getLifeGainedThisTurn(), m, c);
        }
        if (sq[0].contains("LifeOppsLostThisTurn")) {
            int lost = 0;
            for (Player opp : cc.getOpponents()) {
                lost += opp.getLifeLostThisTurn();
            }
            return doXMath(lost, m, c);
        }

        if (sq[0].equals("TotalDamageDoneByThisTurn")) {
            return doXMath(c.getTotalDamageDoneBy(), m, c);
        }
        if (sq[0].equals("TotalDamageReceivedThisTurn")) {
            return doXMath(c.getTotalDamageRecievedThisTurn(), m, c);
        }

        if (sq[0].startsWith("YourCounters")) {
            // "YourCountersExperience" or "YourCountersPoison"
            String counterType = sq[0].substring(12);
            return doXMath(cc.getCounters(CounterType.getType(counterType)), m, c);
        }

        if (sq[0].contains("YourPoisonCounters")) {
            return doXMath(cc.getPoisonCounters(), m, c);
        }
        if (sq[0].contains("TotalOppPoisonCounters")) {
            return doXMath(cc.getOpponentsTotalPoisonCounters(), m, c);
        }

        if (sq[0].contains("YourDamageThisTurn")) {
            return doXMath(cc.getAssignedDamage(), m, c);
        }
        if (sq[0].contains("TotalOppDamageThisTurn")) {
            return doXMath(cc.getOpponentsAssignedDamage(), m, c);
        }
        if (sq[0].contains("MaxOppDamageThisTurn")) {
            return doXMath(cc.getMaxOpponentAssignedDamage(), m, c);
        }

        // Count$YourTypeDamageThisTurn Type
        if (sq[0].contains("YourTypeDamageThisTurn")) {
            return doXMath(cc.getAssignedDamage(sq[0].split(" ")[1]), m, c);
        }
        if (sq[0].contains("YourDamageSourcesThisTurn")) {
            Iterable<Card> allSrc = cc.getAssignedDamageSources();
            String restriction = sq[0].split(" ")[1];
            CardCollection filtered = CardLists.getValidCards(allSrc, restriction, cc, c);
            return doXMath(filtered.size(), m, c);
        }
        
        if (sq[0].contains("YourLandsPlayed")) {
            return doXMath(cc.getLandsPlayedThisTurn(), m, c);
        }

        // Count$TopOfLibraryCMC
        if (sq[0].contains("TopOfLibraryCMC")) {
            final Card topCard = cc.getCardsIn(ZoneType.Library).getFirst();
            return doXMath(topCard == null ? 0 : topCard.getCMC(), m, c);
        }
        
        //Count$TopOfLibraryEachFaceCMC - this version accounts for each of the two split card faces individually
        if (sq[0].contains("TopOfLibraryEachFaceCMC")) {
            final Card topCard = cc.getCardsIn(ZoneType.Library).getFirst();

            if (topCard == null) {
                return 0;
            }

            if (topCard.isSplitCard()) {
                // encode two CMC values so they can be processed individually
                // TODO: devise a better mechanism for this?
                int cmcLeft = doXMath(topCard.getCMC(SplitCMCMode.LeftSplitCMC), m, c);
                int cmcRight = doXMath(topCard.getCMC(SplitCMCMode.RightSplitCMC), m, c);
                int dualCMC = cmcLeft + Card.SPLIT_CMC_ENCODE_MAGIC_NUMBER * cmcRight;
                return dualCMC;
            }

            return doXMath(topCard.getCMC(), m, c);
        }

        // Count$EnchantedControllerCreatures
        if (sq[0].contains("EnchantedControllerCreatures")) {
            if (c.getEnchantingCard() != null) {
                return CardLists.count(c.getEnchantingCard().getController().getCardsIn(ZoneType.Battlefield), CardPredicates.Presets.CREATURES);
            }
            return 0;
        }

        // Count$MonstrosityMagnitude
        if (sq[0].contains("MonstrosityMagnitude")) {
            return doXMath(c.getMonstrosityNum(), m, c);
        }

        // Count$Chroma.<color name>
        // Count$Devotion.<color name>
        if (sq[0].contains("Chroma") || sq[0].equals("Devotion")) {
            ZoneType sourceZone = sq[0].contains("ChromaInGrave") ?  ZoneType.Graveyard : ZoneType.Battlefield;
            String colorName = sq[1];
            if (colorName.contains("Chosen")) {
                colorName = MagicColor.toShortString(c.getChosenColor());
            }
            final CardCollectionView cards;
            if (sq[0].contains("ChromaSource")) { // Runs Chroma for passed in Source card
                cards = new CardCollection(c);
            }
            else {
                cards = cc.getCardsIn(sourceZone);
            }

            int colorOcurrencices = 0;
            byte colorCode = ManaAtom.fromName(colorName);
            for (Card c0 : cards) {
                for (ManaCostShard sh : c0.getManaCost()){
                    if ((sh.getColorMask() & colorCode) != 0) 
                        colorOcurrencices++;
                }
            }
            return doXMath(colorOcurrencices, m, c);
        }
        // Count$DevotionDual.<color name>.<color name>
        if (sq[0].contains("DevotionDual")) {
            int colorOcurrencices = 0;
            byte color1 = ManaAtom.fromName(sq[1]);
            byte color2 = ManaAtom.fromName(sq[2]);
            for (Card c0 : cc.getCardsIn(ZoneType.Battlefield)) {
                for (ManaCostShard sh : c0.getManaCost()) {
                    if ((sh.getColorMask() & (color1 | color2)) != 0) {
                        colorOcurrencices++;
                    }
                }
            }
            return doXMath(colorOcurrencices, m, c);
        }

        if (sq[0].contains("Hellbent")) {
            return doXMath(Integer.parseInt(sq[cc.hasHellbent() ? 1 : 2]), m, c);
        }
        if (sq[0].contains("Metalcraft")) {
            return doXMath(Integer.parseInt(sq[cc.hasMetalcraft() ? 1 : 2]), m, c);
        }
        if (sq[0].contains("Delirium")) {
            return doXMath(Integer.parseInt(sq[cc.hasDelirium() ? 1 : 2]), m, c);
        }
        if (sq[0].contains("FatefulHour")) {
            return doXMath(Integer.parseInt(sq[cc.getLife() <= 5 ? 1 : 2]), m, c);
        }

        if (sq[0].contains("Landfall")) {
            return doXMath(Integer.parseInt(sq[cc.hasLandfall() ? 1 : 2]), m, c);
        }
        if (sq[0].contains("Threshold")) {
            return doXMath(Integer.parseInt(sq[cc.hasThreshold() ? 1 : 2]), m, c);
        }
        if (sq[0].startsWith("Kicked")) {
            return doXMath(Integer.parseInt(sq[c.getKickerMagnitude() > 0 ? 1 : 2]), m, c);
        }
        if (sq[0].startsWith("AltCost")) {
            return doXMath(Integer.parseInt(sq[c.isOptionalCostPaid(OptionalCost.AltCost) ? 1 : 2]), m, c);
        }

        // Count$wasCastFrom<Zone>.<true>.<false>
        if (sq[0].startsWith("wasCastFrom")) {
            boolean zonesMatch = c.getCastFrom() == ZoneType.smartValueOf(sq[0].substring(11)); 
            return doXMath(Integer.parseInt(sq[zonesMatch ? 1 : 2]), m, c);
        }

        if (sq[0].startsWith("Devoured")) {
            final String validDevoured = l[0].split(" ")[1];
            CardCollection cl = CardLists.getValidCards(c.getDevoured(), validDevoured.split(","), cc, c, null);
            return doXMath(cl.size(), m, c);
        }

        if (sq[0].contains("CardPower")) {
            return doXMath(c.getNetPower(), m, c);
        }
        if (sq[0].contains("CardToughness")) {
            return doXMath(c.getNetToughness(), m, c);
        }
        if (sq[0].contains("CardSumPT")) {
            return doXMath((c.getNetPower() + c.getNetToughness()), m, c);
        }

        // Count$SumPower_valid
        if (sq[0].contains("SumPower")) {
            final String[] restrictions = l[0].split("_");
            final String[] rest = restrictions[1].split(",");
            CardCollection filteredCards = CardLists.getValidCards(cc.getGame().getCardsIn(ZoneType.Battlefield), rest, cc, c, null);
            return doXMath(Aggregates.sum(filteredCards, CardPredicates.Accessors.fnGetNetPower), m, c);
        }
        // Count$CardManaCost
        if (sq[0].contains("CardManaCost")) {
            Card ce;
            if (sq[0].contains("Equipped") && c.isEquipping()) {
                ce = c.getEquipping();
            }
            else if (sq[0].contains("Remembered")) {
                ce = (Card) c.getFirstRemembered();
            }
            else {
                ce = c;
            }

            return doXMath(ce == null ? 0 : ce.getCMC(), m, c);
        }
        // Count$SumCMC_valid
        if (sq[0].contains("SumCMC")) {
            final String[] restrictions = l[0].split("_");
            final String[] rest = restrictions[1].split(",");
            CardCollectionView cardsonbattlefield = game.getCardsIn(ZoneType.Battlefield);
            CardCollection filteredCards = CardLists.getValidCards(cardsonbattlefield, rest, cc, c, null);
            return Aggregates.sum(filteredCards, CardPredicates.Accessors.fnGetCmc);
        }

        if (sq[0].contains("CardNumColors")) {
            return doXMath(CardUtil.getColors(c).countColors(), m, c);
        }
        if (sq[0].contains("ChosenNumber")) {
            return doXMath(c.getChosenNumber(), m, c);
        }
        if (sq[0].contains("CardCounters")) {
            // CardCounters.ALL to be used for Kinsbaile Borderguard and anything that cares about all counters
            int count = 0;
            if (sq[1].equals("ALL")) {
                for (Integer i : c.getCounters().values()) {
                    if (i != null && i > 0) {
                        count += i;
                    }
                }
            }
            else {
                count = c.getCounters(CounterType.getType(sq[1]));
            }
            return doXMath(count, m, c);
        }

        // Count$TotalCounters.<counterType>_<valid>
        if (sq[0].contains("TotalCounters")) {
            final String[] restrictions = l[0].split("_");
            final CounterType cType = CounterType.getType(restrictions[1]);
            final String[] validFilter = restrictions[2].split(",");
            CardCollectionView validCards = game.getCardsIn(ZoneType.Battlefield);
            validCards = CardLists.getValidCards(validCards, validFilter, cc, c, null);
            int cCount = 0;
            for (final Card card : validCards) {
                cCount += card.getCounters(cType);
            }
            return doXMath(cCount, m, c);
        }

        if (sq[0].contains("CardControllerTypes")) {
            return doXMath(getCardTypesFromList(cc.getCardsIn(ZoneType.smartValueOf(sq[1]))), m, c);
        }

        if (sq[0].contains("CardTypes")) {
            return doXMath(getCardTypesFromList(game.getCardsIn(ZoneType.smartValueOf(sq[1]))), m, c);
        }

        if (sq[0].contains("BushidoPoint")) {
            return doXMath(c.getKeywordMagnitude("Bushido"), m, c);
        }
        if (sq[0].contains("TimesKicked")) {
            return doXMath(c.getKickerMagnitude(), m, c);
        }
        if (sq[0].contains("TimesPseudokicked")) {
            return doXMath(c.getPseudoKickerMagnitude(), m, c);
        }

        // Count$IfMainPhase.<numMain>.<numNotMain> // 7/10
        if (sq[0].contains("IfMainPhase")) {
            final PhaseHandler cPhase = cc.getGame().getPhaseHandler();
            final boolean isMyMain = cPhase.getPhase().isMain() && cPhase.getPlayerTurn().equals(cc);
            return doXMath(Integer.parseInt(sq[isMyMain ? 1 : 2]), m, c);
        }

        // Count$ThisTurnEntered <ZoneDestination> [from <ZoneOrigin>] <Valid>
        if (sq[0].contains("ThisTurnEntered")) {
            final String[] workingCopy = l[0].split("_");
            
            ZoneType destination = ZoneType.smartValueOf(workingCopy[1]);
            final boolean hasFrom = workingCopy[2].equals("from");
            ZoneType origin = hasFrom ? ZoneType.smartValueOf(workingCopy[3]) : null;
            String validFilter = workingCopy[hasFrom ? 4 : 2] ;

            final CardCollection res = CardUtil.getThisTurnEntered(destination, origin, validFilter, c);
            if (origin == null) { // Remove cards on the battlefield that changed controller
                CardCollectionView sameDest = CardUtil.getThisTurnEntered(destination, destination, validFilter, c);
                res.removeAll(sameDest);
            }
            return doXMath(res.size(), m, c);
        }

        // Count$LastTurnEntered <ZoneDestination> [from <ZoneOrigin>] <Valid>
        if (sq[0].contains("LastTurnEntered")) {
            final String[] workingCopy = l[0].split("_");
            
            ZoneType destination = ZoneType.smartValueOf(workingCopy[1]);
            final boolean hasFrom = workingCopy[2].equals("from");
            ZoneType origin = hasFrom ? ZoneType.smartValueOf(workingCopy[3]) : null;
            String validFilter = workingCopy[hasFrom ? 4 : 2] ;

            final CardCollection res = CardUtil.getLastTurnEntered(destination, origin, validFilter, c);
            if (origin == null) { // Remove cards on the battlefield that changed controller
                CardCollectionView sameDest = CardUtil.getLastTurnEntered(destination, destination, validFilter, c);
                res.removeAll(sameDest);
            }
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

            List<Card> res = new ArrayList<>();
            if (workingCopy[0].contains("This")) {
                res = CardUtil.getThisTurnCast(validFilter, c);
            }
            else {
                res = CardUtil.getLastTurnCast(validFilter, c);
            }

            final int ret = doXMath(res.size(), m, c);
            return ret;
        }

        // Count$Morbid.<True>.<False>
        if (sq[0].startsWith("Morbid")) {
            final CardCollection res = CardUtil.getThisTurnEntered(ZoneType.Graveyard, ZoneType.Battlefield, "Creature", c, true);
            if (res.size() > 0) {
                return doXMath(Integer.parseInt(sq[1]), m, c);
            }
            return doXMath(Integer.parseInt(sq[2]), m, c);
        }

        // Count$Madness.<True>.<False>
        if (sq[0].startsWith("Madness")) {
            if (c.isMadness()) {
                return doXMath(StringUtils.isNumeric(sq[1]) ? Integer.parseInt(sq[1]) : xCount(c, c.getSVar(sq[1])), m, c);
            }
            return doXMath(StringUtils.isNumeric(sq[2]) ? Integer.parseInt(sq[2]) : xCount(c, c.getSVar(sq[2])), m, c);
        }

        if (sq[0].equals("YourTurns")) {
            return doXMath(cc.getTurn(), m, c);
        }

        if (sq[0].equals("TotalTurns")) {
            // Sorry for the Singleton use, replace this once this function has game passed into it
            return doXMath(game.getPhaseHandler().getTurn(), m, c);
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
            CardCollection someCards = CardLists.filter(neededPlayer.getCardsIn(ZoneType.Battlefield), Presets.LANDS);
            for (String basic : MagicColor.Constant.BASIC_LANDS) {
                if (!CardLists.getType(someCards, basic).isEmpty()) {
                    n++;
                }
            }
            return doXMath(n, m, c);
        }
        // Count$Converge
        if (sq[0].contains("Converge")) {
            return doXMath(c.getSunburstValue(), m, c);
        }
        // Count$ColoredCreatures *a DOMAIN for creatures*
        if (sq[0].contains("ColoredCreatures")) {
            int mask = 0;
            CardCollection someCards = CardLists.filter(cc.getCardsIn(ZoneType.Battlefield), Presets.CREATURES);
            for (final Card crd : someCards) {
                mask |= CardUtil.getColors(crd).getColor();
            }
            return doXMath(ColorSet.fromMask(mask).countColors(), m, c);
        }
        
        // Count$CardMulticolor.<numMC>.<numNotMC>
        if (sq[0].contains("CardMulticolor")) {
            final boolean isMulti = CardUtil.getColors(c).isMulticolor(); 
            return doXMath(Integer.parseInt(sq[isMulti ? 1 : 2]), m, c);
        }
        
        // Complex counting methods
        CardCollectionView someCards = getCardListForXCount(c, cc, sq);
        
        // 1/10 - Count$MaxCMCYouCtrl
        if (sq[0].contains("MaxCMC")) {
            int mmc = Aggregates.max(someCards, CardPredicates.Accessors.fnGetCmc);
            return doXMath(mmc, m, c);
        }

        return doXMath(someCards.size(), m, c);
    }

    private static CardCollectionView getCardListForXCount(final Card c, final Player cc, final String[] sq) {
    	final List<Player> opps = cc.getOpponents();
        CardCollection someCards = new CardCollection();
        final Game game = c.getGame();
        
        // Generic Zone-based counting
        // Count$QualityAndZones.Subquality

        // build a list of cards in each possible specified zone

        if (sq[0].contains("YouCtrl")) {
            someCards.addAll(cc.getCardsIn(ZoneType.Battlefield));
        }

        if (sq[0].contains("InYourYard")) {
            someCards.addAll(cc.getCardsIn(ZoneType.Graveyard));
        }

        if (sq[0].contains("InYourLibrary")) {
            someCards.addAll(cc.getCardsIn(ZoneType.Library));
        }

        if (sq[0].contains("InYourHand")) {
            someCards.addAll(cc.getCardsIn(ZoneType.Hand));
        }

        if (sq[0].contains("InYourSideboard")) {
            someCards.addAll(cc.getCardsIn(ZoneType.Sideboard));
        }

        if (sq[0].contains("OppCtrl")) {
        	for (final Player p : opps) {
        		someCards.addAll(p.getZone(ZoneType.Battlefield).getCards());
        	}
        }

        if (sq[0].contains("InOppYard")) {
        	for (final Player p : opps) {
        		someCards.addAll(p.getCardsIn(ZoneType.Graveyard));
        	}
        }

        if (sq[0].contains("InOppHand")) {
        	for (final Player p : opps) {
        		someCards.addAll(p.getCardsIn(ZoneType.Hand));
        	}
        }

        if (sq[0].contains("InChosenHand")) {
        	if (c.getChosenPlayer() != null) {
        		someCards.addAll(c.getChosenPlayer().getCardsIn(ZoneType.Hand));
        	}
        }

        if (sq[0].contains("InChosenYard")) {
        	if (c.getChosenPlayer() != null) {
        		someCards.addAll(c.getChosenPlayer().getCardsIn(ZoneType.Graveyard));
        	}
        }

        if (sq[0].contains("OnBattlefield")) {
        	someCards.addAll(game.getCardsIn(ZoneType.Battlefield));
        }

        if (sq[0].contains("InAllYards")) {
        	someCards.addAll(game.getCardsIn(ZoneType.Graveyard));
        }

        if (sq[0].contains("SpellsOnStack")) {
            someCards.addAll(game.getCardsIn(ZoneType.Stack));
        }

        if (sq[0].contains("InAllHands")) {
        	someCards.addAll(game.getCardsIn(ZoneType.Hand));
        }

        //  Count$InTargetedHand (targeted player's cards in hand)
        if (sq[0].contains("InTargetedHand")) {
            for (final SpellAbility sa : c.getCurrentState().getNonManaAbilities()) {
                final SpellAbility saTargeting = sa.getSATargetingPlayer();
                if (saTargeting != null) {
                    for (final Player tgtP : saTargeting.getTargets().getTargetPlayers()) {
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
        // if (sq[0].contains("Untapped")) { someCards = CardLists.filter(someCards, Presets.UNTAPPED); }

        // if (sq[0].contains("Tapped")) { someCards = CardLists.filter(someCards, Presets.TAPPED); }

//        String sq0 = sq[0].toLowerCase();
//        for (String color : MagicColor.Constant.ONLY_COLORS) {
//            if (sq0.contains(color))
//                someCards = someCards.filter(CardListFilter.WHITE);
//        }
        // "White Creatures" - Count$WhiteTypeYouCtrl.Creature
        // if (sq[0].contains("White")) someCards = CardLists.filter(someCards, CardPredicates.isColor(MagicColor.WHITE));
        // if (sq[0].contains("Blue"))  someCards = CardLists.filter(someCards, CardPredicates.isColor(MagicColor.BLUE));
        // if (sq[0].contains("Black")) someCards = CardLists.filter(someCards, CardPredicates.isColor(MagicColor.BLACK));
        // if (sq[0].contains("Red"))   someCards = CardLists.filter(someCards, CardPredicates.isColor(MagicColor.RED));
        // if (sq[0].contains("Green")) someCards = CardLists.filter(someCards, CardPredicates.isColor(MagicColor.GREEN));

        if (sq[0].contains("Multicolor")) {
            someCards = CardLists.filter(someCards, new Predicate<Card>() {
                @Override
                public boolean apply(final Card c) {
                    return CardUtil.getColors(c).isMulticolor();
                }
            });
        }

        if (sq[0].contains("Monocolor")) {
            someCards = CardLists.filter(someCards, new Predicate<Card>() {
                @Override
                public boolean apply(final Card c) {
                    return CardUtil.getColors(c).isMonoColor();
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
        } else if (s[0].contains("Abs")) {
            return Math.abs(num);
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
     *            a {@link forge.game.card.CardCollectionView} object.
     * @param string
     *            a {@link java.lang.String} object.
     * @param source
     *            a {@link forge.game.card.Card} object.
     * @return a int.
     */
    public static int handlePaid(final CardCollectionView paidList, final String string, final Card source) {
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
            final CardCollection list = CardLists.getValidCards(paidList, valid, source.getController(), source);
            return doXMath(list.size(), splitString.length > 1 ? splitString[1] : null, source);
        }

        String filteredString = string;
        CardCollection filteredList = new CardCollection(paidList);
        final String[] filter = filteredString.split("_");

        if (string.startsWith("FilterControlledBy")) {
            final String pString = filter[0].substring(18);
            FCollectionView<Player> controllers = AbilityUtils.getDefinedPlayers(source, pString, null);
            filteredList = CardLists.filterControlledBy(filteredList, controllers);
            filteredString = filteredString.replace(pString, "");
            filteredString = filteredString.replace("FilterControlledBy_", "");
        }

        int tot = 0;
        for (final Card c : filteredList) {
            tot += xCount(c, filteredString);
        }

        return tot;
    }

    /**
     * <p>
     * isMostProminentColor.
     * </p>
     * 
     * @param list
     *            a {@link Iterable<Card>} object.
     * @return a boolean.
     */
    public static byte getMostProminentColors(final Iterable<Card> list) {
        int cntColors = MagicColor.WUBRG.length;
        final Integer[] map = new Integer[cntColors];
        for (int i = 0; i < cntColors; i++) {
            map[i] = 0;
        }

        for (final Card crd : list) {
            ColorSet color = CardUtil.getColors(crd);
            for (int i = 0; i < cntColors; i++) {
                if (color.hasAnyColor(MagicColor.WUBRG[i]))
                    map[i]++;
            }
        } // for

        byte mask = 0;
        int nMax = -1;
        for (int i = 0; i < cntColors; i++) { 
            if (map[i] > nMax)
                mask = MagicColor.WUBRG[i];
            else if (map[i] == nMax)
                mask |= MagicColor.WUBRG[i];
            else 
                continue;
            nMax = map[i];
        }
        return mask;
    }

    /**
     * <p>
     * SortColorsFromList.
     * </p>
     * 
     * @param list
     *            a {@link forge.game.card.CardCollection} object.
     * @return a List.
     */
    public static int[] SortColorsFromList(final CardCollection list) {
        int cntColors = MagicColor.WUBRG.length;
        final int[] map = new int[cntColors];
        for (int i = 0; i < cntColors; i++) {
            map[i] = 0;
        }

        for (final Card crd : list) {
            ColorSet color = CardUtil.getColors(crd);
            for (int i = 0; i < cntColors; i++) {
                if (color.hasAnyColor(MagicColor.WUBRG[i]))
                    map[i]++;
            }
        } // for
        Arrays.sort(map);
        return map;
    }

    /**
     * <p>
     * getMostProminentColorsFromList.
     * </p>
     * 
     * @param list
     *            a {@link forge.game.card.CardCollectionView} object.
     * @return a boolean.
     */
    public static byte getMostProminentColorsFromList(final CardCollectionView list, final List<String> restrictedToColors) {
        List<Byte> colorRestrictions = new ArrayList<Byte>();
        for (final String col : restrictedToColors) {
            colorRestrictions.add(MagicColor.fromName(col));
        }
        int cntColors = colorRestrictions.size();
        final Integer[] map = new Integer[cntColors];
        for (int i = 0; i < cntColors; i++) {
            map[i] = 0;
        }

        for (final Card crd : list) {
            ColorSet color = CardUtil.getColors(crd);
            for (int i = 0; i < cntColors; i++) {
                if (color.hasAnyColor(colorRestrictions.get(i))) {
                    map[i]++;
                }
            }
        }

        byte mask = 0;
        int nMax = -1;
        for (int i = 0; i < cntColors; i++) { 
            if (map[i] > nMax)
                mask = colorRestrictions.get(i);
            else if (map[i] == nMax)
                mask |= colorRestrictions.get(i);
            else 
                continue;
            nMax = map[i];
        }
        return mask;
    }

    /**
     * <p>
     * getMostProminentCreatureType.
     * </p>
     * 
     * @param list
     *            a {@link forge.game.card.CardCollection} object.
     * @return an int.
     */
    public static int getMostProminentCreatureTypeSize(final CardCollection list) {
        if (list.isEmpty()) {
            return 0;
        }
        int allCreatureType = 0;

        final Map<String, Integer> map = new HashMap<String, Integer>();
        for (final Card c : list) {
            // Remove Duplicated types
            final Set<String> creatureTypes = c.getType().getCreatureTypes();
            for (String creatureType : creatureTypes) {
                if (creatureType.equals("AllCreatureTypes")) {
                    allCreatureType++;
                }
                else {
                    Integer count = map.get(creatureType);
                    map.put(creatureType, count == null ? 1 : count + 1);
                }
            }
        }

        int max = 0;
        for (final Entry<String, Integer> entry : map.entrySet()) {
            if (max < entry.getValue()) {
                max = entry.getValue();
            }
        }
        return max + allCreatureType;
    }

    /**
     * <p>
     * sharedKeywords.
     * </p>
     * 
     * @param kw
     *            a  String arry.
     * @return a List<String>.
     */
    public static List<String> sharedKeywords(final String[] kw, final String[] restrictions,
            final List<ZoneType> zones, final Card host) {
        final List<String> filteredkw = new ArrayList<String>();
        final Player p = host.getController();
        CardCollection cardlist = new CardCollection(p.getGame().getCardsIn(zones));
        final List<String> landkw = new ArrayList<String>();
        final List<String> protectionkw = new ArrayList<String>();
        final List<String> allkw = new ArrayList<String>();
        
        cardlist = CardLists.getValidCards(cardlist, restrictions, p, host, null);
        for (Card c : cardlist) {
            for (String k : c.getKeywords()) {
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

    public static int getCardTypesFromList(final CardCollectionView list) {
        EnumSet<CardType.CoreType> types = EnumSet.noneOf(CardType.CoreType.class);
        for (Card c1 : list) {
            Iterables.addAll(types, c1.getType().getCoreTypes());
        }
        return types.size();
    }

    public static List<SpellAbility> getBushidoEffects(final Card c) {
        final List<SpellAbility> list = new ArrayList<SpellAbility>();
        for (final String kw : c.getKeywords()) {
            if (kw.contains("Bushido")) {
                final String[] parse = kw.split(" ");
                final String s = parse[1];
                final int magnitude = Integer.parseInt(s);

                String description = String.format("Bushido %d (When this blocks or becomes blocked, it gets +%d/+%d until end of turn).", magnitude, magnitude, magnitude);
                String regularPart = String.format("AB$ Pump | Cost$ 0 | Defined$ CardUID_%d | NumAtt$ +%d | NumDef$ +%d | StackDescription$ %s", c.getId(), magnitude, magnitude, description);
                
                SpellAbility ability = AbilityFactory.getAbility(regularPart, c);
                ability.setDescription(ability.getStackDescription());
                ability.setTrigger(true); // can be copied by Strionic Resonator
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
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @return a int.
     */
    public static int getNeededXDamage(final SpellAbility ability) {
        // when targeting a creature, make sure the AI won't overkill on X
        // damage
        final Card target = ability.getTargetCard();
        int neededDamage = -1;

        if ((target != null)) {
            neededDamage = target.getNetToughness() - target.getDamage();
        }

        return neededDamage;
    }

    public static void correctAbilityChainSourceCard(final SpellAbility sa, final Card card) {
        sa.setHostCard(card);

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
            final SpellAbility intrinsicAbility = AbilityFactory.getAbility(rawAbility, card);
            card.addSpellAbility(intrinsicAbility);
            intrinsicAbility.setIntrinsic(true);
        }
    }
    /*
    public static final void addCommanderAbilities(final Card cmd) {
        ReplacementEffect re = ReplacementHandler.parseReplacement(
                "Event$ Moved | Destination$ Graveyard,Exile | ValidCard$ Card.Self | Secondary$ True | Optional$ True | OptionalDecider$ You | ReplaceWith$ CommanderMoveReplacement | " +
                "Description$ If a commander would be put into its owner's graveyard or exile from anywhere, that player may put it into the command zone instead.",
                cmd, true);
        cmd.addReplacementEffect(re);
        if (StringUtils.isBlank(cmd.getSVar("CommanderCostRaise"))) // why condition check is needed?
            cmd.setSVar("CommanderCostRaise", "Number$0");

        String cmdManaCost = cmd.getManaCost().toString();
        cmd.setSVar("CommanderMoveReplacement", "DB$ ChangeZone | Origin$ Battlefield,Graveyard,Exile,Library | Destination$ Command | Defined$ ReplacedCard");
        cmd.setSVar("DBCommanderIncCast", "DB$ StoreSVar | SVar$ CommanderCostRaise | Type$ CountSVar | Expression$ CommanderCostRaise/Plus.2");
        SpellAbility sa = AbilityFactory.getAbility("SP$ PermanentCreature | SorcerySpeed$ True | ActivationZone$ Command | SubAbility$ DBCommanderIncCast | Cost$ " + cmdManaCost, cmd);
        cmd.addSpellAbility(sa);

        cmd.addIntrinsicAbility("SP$ PermanentCreature | SorcerySpeed$ True | ActivationZone$ Command | SubAbility$ DBCommanderIncCast | Cost$ " + cmdManaCost);
        cmd.addStaticAbility("Mode$ RaiseCost | Amount$ CommanderCostRaise | Type$ Spell | ValidCard$ Card.Self+wasCastFromCommand | EffectZone$ All | AffectedZone$ Stack");
    }
    */

    /**
     * <p>
     * postFactoryKeywords.
     * </p>
     * 
     * @param card
     *            a {@link forge.game.card.Card} object.
     */
    public static void setupKeywordedAbilities(final Card card) {
        // this function should handle any keywords that need to be added after
        // a spell goes through the factory
        // Cards with Cycling abilities
        // -1 means keyword "Cycling" not found

        for (String keyword : card.getKeywords()) {
            if (keyword.startsWith("Multikicker")) {
                final String[] k = keyword.split("kicker ");
                String mkCost = k[1].split(":")[0];
                final SpellAbility sa = card.getFirstSpellAbility();
                sa.setMultiKickerManaCost(new ManaCost(new ManaCostParser(mkCost)));
                if (k[1].endsWith("Generic")) {
                    sa.addAnnounceVar("Pseudo-multikicker");
                } else {
                    sa.addAnnounceVar("Multikicker");
                }
            }
            else if (keyword.startsWith("Replicate")) {
                card.getFirstSpellAbility().addAnnounceVar("Replicate");
            }
            else if (keyword.startsWith("Fuse")) {
                card.getState(CardStateName.Original).addNonManaAbility(AbilityFactory.buildFusedAbility(card));
            }
            else if (keyword.startsWith("Evoke")) {
                card.addSpellAbility(makeEvokeSpell(card, keyword));
            }
            else if (keyword.startsWith("Dash")) {
                card.addSpellAbility(makeDashSpell(card, keyword));
            }
            else if (keyword.startsWith("Awaken")) {
                card.addSpellAbility(makeAwakenSpell(card, keyword));
            }
            else if (keyword.startsWith("Surge")) {
                card.addSpellAbility(makeSurgeSpell(card, keyword));
            }
            else if (keyword.startsWith("Monstrosity")) {
                final String[] k = keyword.split(":");
                final String magnitude = k[0].substring(12);
                final String manacost = k[1];
                card.removeIntrinsicKeyword(keyword);

                String ref = "X".equals(magnitude) ? " | References$ X" : "";
                String counters = StringUtils.isNumeric(magnitude) 
                        ? Lang.nounWithNumeral(Integer.parseInt(magnitude), "+1/+1 counter"): "X +1/+1 counters";
                String effect = "AB$ PutCounter | Cost$ " + manacost + " | ConditionPresent$ " +
                        "Card.Self+IsNotMonstrous | Monstrosity$ True | CounterNum$ " +
                        magnitude + " | CounterType$ P1P1 | SpellDescription$ Monstrosity " +
                        magnitude + " (If this creature isn't monstrous, put " + 
                        counters + " on it and it becomes monstrous.) | StackDescription$ SpellDescription" + ref;

                card.addSpellAbility(AbilityFactory.getAbility(effect, card));
                // add ability to instrinic strings so copies/clones create the ability also
                card.getCurrentState().addUnparsedAbility(effect);
            }
            else if (keyword.startsWith("Unearth")) {
                card.removeIntrinsicKeyword(keyword);

                final String[] k = keyword.split(":");
                final String manacost = k[1];

                String effect = "AB$ ChangeZone | Cost$ " + manacost + " | Defined$ Self" +
                        " | Origin$ Graveyard | Destination$ Battlefield | SorcerySpeed$" +
                        " True | ActivationZone$ Graveyard | Unearth$ True | SubAbility$" +
                        " UnearthPumpSVar | PrecostDesc$ Unearth | StackDescription$ " +
                        "Unearth: Return CARDNAME to the battlefield. | SpellDescription$" +
                        " (" + ManaCostParser.parse(manacost) + ": Return CARDNAME to the " +
                        "battlefield. It gains haste. Exile it at the beginning of the next" +
                        " end step or if it would leave the battlefield. Unearth only as a sorcery.)";
                String dbpump = "DB$ Pump | Defined$ Self | KW$ Haste & HIDDEN If CARDNAME" +
                        " would leave the battlefield, exile it instead of putting it " +
                        "anywhere else. | Permanent$ True | SubAbility$ UnearthDelayTrigger";
                String delTrig = "DB$ DelayedTrigger | Mode$ Phase | Phase$ End of Turn" +
                        " | Execute$ UnearthTrueDeath | TriggerDescription$ Exile " +
                        "CARDNAME at the beginning of the next end step.";
                String truedeath = "DB$ ChangeZone | Defined$ Self | Origin$ Battlefield" +
                        " | Destination$ Exile";
                card.setSVar("UnearthPumpSVar", dbpump);
                card.setSVar("UnearthDelayTrigger", delTrig);
                card.setSVar("UnearthTrueDeath", truedeath);
                card.addSpellAbility(AbilityFactory.getAbility(effect, card));
                // add ability to instrinic strings so copies/clones create the ability also
                card.getCurrentState().addUnparsedAbility(effect);
            }
            else if (keyword.startsWith("Level up")) {
                final String strMaxLevel = card.getSVar("maxLevel");
                card.removeIntrinsicKeyword(keyword);

                final String[] k = keyword.split(":");
                final String manacost = k[1];

                String effect = "AB$ PutCounter | Cost$ " + manacost + " | " +
                        "SorcerySpeed$ True | LevelUp$ True | CounterNum$ 1" +
                        " | CounterType$ LEVEL | PrecostDesc$ Level Up | MaxLevel$ " +
                        strMaxLevel + " | SpellDescription$ (Put a level counter on" +
                        " this permanent. Activate this ability only any time you" +
                        " could cast a sorcery.)";

                card.addSpellAbility(AbilityFactory.getAbility(effect, card));
                // add ability to instrinic strings so copies/clones create the ability also
                card.getCurrentState().addUnparsedAbility(effect);
            }
            else if (keyword.startsWith("Cycling")) {
                card.removeIntrinsicKeyword(keyword);

                final String[] k = keyword.split(":");
                final String manacost = k[1];

                card.addSpellAbility(abilityCycle(card, manacost));
            }
            else if (keyword.startsWith("TypeCycling")) {
                card.removeIntrinsicKeyword(keyword);

                final String[] k = keyword.split(":");
                final String type = k[1];
                final String manacost = k[2];

                card.addSpellAbility(abilityTypecycle(card, manacost, type));
            }
            else if (keyword.startsWith("Transmute")) {
                card.removeIntrinsicKeyword(keyword);
                final String manacost = keyword.split(":")[1];
                final String sbTransmute = "AB$ ChangeZone | Cost$ " + manacost + " Discard<1/CARDNAME>"
                        + " | CostDesc$ Transmute " + ManaCostParser.parse(manacost)+ " | ActivationZone$ Hand"
                        + " | Origin$ Library | Destination$ Hand | ChangeType$ Card.cmcEQ" + card.getManaCost().getCMC()
                        + " | ChangeNum$ 1 | SorcerySpeed$ True | References$ TransmuteX | SpellDescription$ ("
                        + ManaCostParser.parse(manacost) + ", Discard this card: Search your library for a card "
                        + "with the same converted mana cost as the discarded card, reveal that card, "
                        + "and put it into your hand. Then shuffle your library. Activate this ability "
                        + "only any time you could cast a sorcery.)";
                final SpellAbility abTransmute = AbilityFactory.getAbility(sbTransmute, card);
                card.addSpellAbility(abTransmute);
                card.getCurrentState().addUnparsedAbility(sbTransmute);
            }
            else if (keyword.startsWith("Soulshift")) {
                final String[] k = keyword.split(" ");
                final int manacost = Integer.parseInt(k[1]);

                final String actualTrigger = "Mode$ ChangesZone | Origin$ Battlefield | Destination$ Graveyard"
                        + "| OptionalDecider$ You | ValidCard$ Card.Self | Execute$ SoulshiftAbility"
                        + "| TriggerController$ TriggeredCardController | TriggerDescription$ " + keyword
                        + " (When this creature dies, you may return target Spirit card with converted mana cost "
                        + manacost + " or less from your graveyard to your hand.)";
                final String abString = "DB$ ChangeZone | Origin$ Graveyard | Destination$ Hand"
                        + "| ValidTgts$ Spirit.YouOwn+cmcLE" + manacost;
                final Trigger parsedTrigger = TriggerHandler.parseTrigger(actualTrigger, card, true);
                card.addTrigger(parsedTrigger);
                card.setSVar("SoulshiftAbility", abString);
            }
            else if (keyword.startsWith("Champion")) {
                card.removeIntrinsicKeyword(keyword);

                final String[] k = keyword.split(":");
                final String[] valid = k[1].split(",");
                String desc = k.length > 2 ? k[2] : k[1];
                String article = Lang.startsWithVowel(desc) ? "an" : "a";
                if (desc.equals("Creature")) {
                    desc = "creature"; //use lowercase for "Champion a creature"
                }

                StringBuilder changeType = new StringBuilder();
                for (String v : valid) {
                    if (changeType.length() != 0) {
                        changeType.append(",");
                    }
                    changeType.append(v).append(".YouCtrl+Other");
                }

                StringBuilder trig = new StringBuilder();
                trig.append("Mode$ ChangesZone | Origin$ Any | Destination$ Battlefield | ValidCard$ Card.Self | ");
                trig.append("Execute$ ChampionAbility | TriggerDescription$ Champion ").append(article + " ");
                trig.append(desc).append(" (When this enters the battlefield, sacrifice it unless you exile another ");
                trig.append(desc).append(" you control. When this leaves the battlefield, that card returns to the battlefield.)");

                StringBuilder trigReturn = new StringBuilder();
                trigReturn.append("Mode$ ChangesZone | Origin$ Battlefield | Destination$ Any | ValidCard$ Card.Self | ");
                trigReturn.append("Execute$ ChampionReturn | Secondary$ True | TriggerDescription$ When this leaves the battlefield, that card returns to the battlefield.");

                StringBuilder ab = new StringBuilder();
                ab.append("DB$ ChangeZone | Origin$ Battlefield | Destination$ Exile | RememberChanged$ True | Champion$ True | ");
                ab.append("Hidden$ True | Optional$ True | SubAbility$ ChampionSacrifice | ChangeType$ ").append(changeType);

                StringBuilder subAb = new StringBuilder();
                subAb.append("DB$ Sacrifice | Defined$ Card.Self | ConditionDefined$ Remembered | ConditionPresent$ Card | ConditionCompare$ EQ0");

                String returnChampion = "DB$ ChangeZone | Defined$ Remembered | Origin$ Exile | Destination$ Battlefield | SubAbility$ ChampionCleanup";
                final Trigger parsedTrigger = TriggerHandler.parseTrigger(trig.toString(), card, true);
                final Trigger parsedTrigReturn = TriggerHandler.parseTrigger(trigReturn.toString(), card, true);
                card.addTrigger(parsedTrigger);
                card.addTrigger(parsedTrigReturn);
                card.setSVar("ChampionAbility", ab.toString());
                card.setSVar("ChampionReturn", returnChampion);
                card.setSVar("ChampionCleanup", "DB$ Cleanup | ClearRemembered$ True");
                card.setSVar("ChampionSacrifice", subAb.toString());
            }
            else if (keyword.startsWith("If CARDNAME would be put into a graveyard "
                    + "from anywhere, reveal CARDNAME and shuffle it into its owner's library instead.")) {
                String replacement = "Event$ Moved | Destination$ Graveyard | ValidCard$ Card.Self | ReplaceWith$ GraveyardToLibrary";
                String ab =  "DB$ ChangeZone | Hidden$ True | Origin$ All | Destination$ Library | Defined$ ReplacedCard | Reveal$ True | Shuffle$ True";

                card.addReplacementEffect(ReplacementHandler.parseReplacement(replacement, card, true));
                card.setSVar("GraveyardToLibrary", ab);
            }
            else if (keyword.startsWith("Echo")) {
                final String[] k = keyword.split(":");
                final String cost = k[1];
                
                String upkeepTrig = "Mode$ Phase | Phase$ Upkeep | ValidPlayer$ You | TriggerZones$ Battlefield " +
                        " | Execute$ TrigUpkeepEcho | IsPresent$ Card.Self+cameUnderControlSinceLastUpkeep | Secondary$ True | " +
                        "TriggerDescription$ Echo: At the beginning of your upkeep, if CARDNAME came under your control since the " +
                        "beginning of your last upkeep, sacrifice it unless you pay the Echo cost";
                String ref = "X".equals(cost) ? " | References$ X" : "";
                card.setSVar("TrigUpkeepEcho", "AB$ Sacrifice | Cost$ 0 | SacValid$ Self | "
                        + "Echo$ " + cost + ref);
 
                final Trigger parsedUpkeepTrig = TriggerHandler.parseTrigger(upkeepTrig, card, true);
                card.addTrigger(parsedUpkeepTrig);
            }
            else if (keyword.startsWith("Suspend")) {
                card.removeIntrinsicKeyword(keyword);
                card.setSuspend(true);
                final String[] k = keyword.split(":");

                final String timeCounters = k[1];
                final String cost = k[2];
                card.addSpellAbility(abilitySuspendStatic(card, cost, timeCounters));
                addSuspendUpkeepTrigger(card);
                addSuspendPlayTrigger(card);
            }
            else if (keyword.startsWith("Fading")) {
                final String[] k = keyword.split(":");

                card.addIntrinsicKeyword("etbCounter:FADE:" + k[1] + ":no Condition:no desc");

                String upkeepTrig = "Mode$ Phase | Phase$ Upkeep | ValidPlayer$ You | TriggerZones$ Battlefield " +
                        " | Execute$ TrigUpkeepFading | Secondary$ True | TriggerDescription$ At the beginning of " +
                        "your upkeep, remove a fade counter from CARDNAME. If you can't, sacrifice CARDNAME.";

                card.setSVar("TrigUpkeepFading", "AB$ RemoveCounter | Cost$ 0 | Defined$ Self | CounterType$ FADE" +
                        " | CounterNum$ 1 | RememberRemoved$ True | SubAbility$ DBUpkeepFadingSac");
                card.setSVar("DBUpkeepFadingSac","DB$ Sacrifice | SacValid$ Self | ConditionCheckSVar$ FadingCheckSVar" +
                        " | ConditionSVarCompare$ EQ0 | References$ FadingCheckSVar | SubAbility$ FadingCleanup");
                card.setSVar("FadingCleanup","DB$ Cleanup | ClearRemembered$ True");
                card.setSVar("FadingCheckSVar","Count$RememberedSize");
                final Trigger parsedUpkeepTrig = TriggerHandler.parseTrigger(upkeepTrig, card, true);
                card.addTrigger(parsedUpkeepTrig);
            }
            else if (keyword.startsWith("Renown")) {
                final String[] k = keyword.split(" ");
                final String suffix = !k[1].equals("1") ? "s" : "";
                card.removeIntrinsicKeyword(keyword);
                String renownTrig = "Mode$ DamageDone | ValidSource$ Card.Self | ValidTarget$ Player"
                		+ " | IsPresent$ Card.Self+IsNotRenowned | CombatDamage$ True | Execute$"
                		+ " TrigBecomeRenown | TriggerDescription$ Renown " + k[1] +" (When this "
                		+ "creature deals combat damage to a player, if it isn't renowned, put " 
                		+ k[1] + " +1/+1 counter" + suffix + " on it and it becomes renowned.) ";
                card.setSVar("TrigBecomeRenown", "AB$ PutCounter | Cost$ 0 | Defined$ Self | "
                		+ "CounterType$ P1P1 | CounterNum$ " + k[1] + " | Renown$ True");
                final Trigger parseRenownTrig = TriggerHandler.parseTrigger(renownTrig, card, true);
                card.addTrigger(parseRenownTrig);
            }
            else if (keyword.startsWith("Vanishing")) {
                final String[] k = keyword.split(":");
                // etbcounter
                card.addIntrinsicKeyword("etbCounter:TIME:" + k[1] + ":no Condition:no desc");
                // Remove Time counter trigger
                String upkeepTrig = "Mode$ Phase | Phase$ Upkeep | ValidPlayer$ You | " +
                        "TriggerZones$ Battlefield | IsPresent$ Card.Self+counters_GE1_TIME" +
                        " | Execute$ TrigUpkeepVanishing | TriggerDescription$ At the " +
                        "beginning of your upkeep, if CARDNAME has a time counter on it, " +
                        "remove a time counter from it. | Secondary$ True";
                card.setSVar("TrigUpkeepVanishing", "AB$ RemoveCounter | Cost$ 0 | Defined$ Self" +
                        " | CounterType$ TIME | CounterNum$ 1");
                final Trigger parsedUpkeepTrig = TriggerHandler.parseTrigger(upkeepTrig, card, true);
                card.addTrigger(parsedUpkeepTrig);
                // sacrifice trigger
                String sacTrig = "Mode$ CounterRemoved | TriggerZones$ Battlefield | ValidCard$" +
                        " Card.Self | NewCounterAmount$ 0 | Secondary$ True | CounterType$ TIME |" +
                        " Execute$ TrigVanishingSac | TriggerDescription$ When the last time " +
                        "counter is removed from CARDNAME, sacrifice it.";
                card.setSVar("TrigVanishingSac", "AB$ Sacrifice | Cost$ 0 | SacValid$ Self");

                final Trigger parsedSacTrigger = TriggerHandler.parseTrigger(sacTrig, card, true);
                card.addTrigger(parsedSacTrigger);
            }
            else if (keyword.equals("Delve")) {
                card.getSpellAbilities().getFirst().setDelve(true);
            }
            else if (keyword.startsWith("Haunt")) {
                setupHauntSpell(card);
            }
            else if (keyword.equals("Provoke")) {
                final String actualTrigger = "Mode$ Attacks | ValidCard$ Card.Self | "
                        + "OptionalDecider$ You | Execute$ ProvokeAbility | Secondary$ True | TriggerDescription$ "
                        + "When this attacks, you may have target creature defending player "
                        + "controls untap and block it if able.";
                final String abString = "DB$ MustBlock | ValidTgts$ Creature.DefenderCtrl | "
                        + "TgtPrompt$ Select target creature defending player controls | SubAbility$ ProvokeUntap";
                final String dbString = "DB$ Untap | Defined$ Targeted";
                final Trigger parsedTrigger = TriggerHandler.parseTrigger(actualTrigger, card, true);
                card.addTrigger(parsedTrigger);
                card.setSVar("ProvokeAbility", abString);
                card.setSVar("ProvokeUntap", dbString);
            }
            else if (keyword.equals("Myriad")) {
                final String actualTrigger = "Mode$ Attacks | ValidCard$ Card.Self | Execute$ "
                        + "MyriadAbility | Secondary$ True | TriggerDescription$ Myriad (When this "
                        + "creature attacks, for each opponent other than defending player, you may"
                        + " put a token that's a copy of this creature onto the battlefield tapped "
                        + "and attacking that player or a planeswalker he or she controls. Exile the"
                        + " tokens at end of combat.)";
                final String abString = "DB$ RepeatEach | RepeatPlayers$ OpponentsOtherThanDefendingPlayer"
                        + " | RepeatSubAbility$ MyriadCopy | SubAbility$ MyriadDelTrig";
                final String dbString1 = "DB$ CopyPermanent | Defined$ Self | Tapped$ True | "
                        + "Optional$ True | CopyAttacking$ Remembered | ChoosePlayerOrPlaneswalker$"
                        + " True | ImprintCopied$ True";
                final String dbString2 = "DB$ DelayedTrigger | Mode$ Phase | Phase$ EndCombat | "
                        + "Execute$ MyriadExile | RememberObjects$ Imprinted | TriggerDescription$"
                        + " Exile the tokens at end of combat. | SubAbility$ MyriadCleanup";
                final String dbString3 = "DB$ ChangeZone | Defined$ DelayTriggerRemembered | Origin$"
                        + " Battlefield | Destination$ Exile";
                final String dbString4 = "DB$ Cleanup | ClearImprinted$ True";
                final Trigger parsedTrigger = TriggerHandler.parseTrigger(actualTrigger, card, true);
                card.addTrigger(parsedTrigger);
                card.setSVar("MyriadAbility", abString);
                card.setSVar("MyriadCopy", dbString1);
                card.setSVar("MyriadDelTrig", dbString2);
                card.setSVar("MyriadExile", dbString3);
                card.setSVar("MyriadCleanup", dbString4);
            }
            else if (keyword.equals("Living Weapon")) {
                card.removeIntrinsicKeyword(keyword);

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
            else if (keyword.equals("Epic")) {
                makeEpic(card);
            }
            else if (keyword.equals("Soulbond")) {
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
            else if (keyword.equals("Exalted")) {
                addTriggerAbility(keyword, card, null);
            }
            else if (keyword.equals("Extort")) {
                addTriggerAbility(keyword, card, null);
            }
            else if (keyword.equals("Evolve")) {
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
            else if (keyword.startsWith("Dredge")) {
                final int dredgeAmount = card.getKeywordMagnitude("Dredge");

                final String actualRep = "Event$ Draw | ActiveZones$ Graveyard | ValidPlayer$ You | "
                        + "ReplaceWith$ DredgeCards | Secondary$ True | Optional$ True | CheckSVar$ "
                        + "DredgeCheckLib | SVarCompare$ GE" + dredgeAmount + " | References$ "
                        + "DredgeCheckLib | AICheckDredge$ True | Description$ " + card.getName()
                        +  " - Dredge " + dredgeAmount;
                final String abString = "DB$ Mill | Defined$ You | NumCards$ " + dredgeAmount + " | "
                        + "SubAbility$ DredgeMoveToPlay";
                final String moveToPlay = "DB$ ChangeZone | Origin$ Graveyard | Destination$ Hand | "
                        + "Defined$ Self";
                final String checkSVar = "Count$ValidLibrary Card.YouOwn";
                card.setSVar("DredgeCards", abString);
                card.setSVar("DredgeMoveToPlay", moveToPlay);
                card.setSVar("DredgeCheckLib", checkSVar);
                card.addReplacementEffect(ReplacementHandler.parseReplacement(actualRep, card, true));
            }
            else if (keyword.startsWith("Tribute")) {
                final int tributeAmount = card.getKeywordMagnitude("Tribute");

                final String actualRep = "Event$ Moved | Destination$ Battlefield | ValidCard$ Card.Self |"
                        + " ReplaceWith$ TributeAddCounter | Secondary$ True | Description$ Tribute "
                        + tributeAmount + " (As this creature enters the battlefield, an opponent of your"
                        + " choice may place " + tributeAmount + " +1/+1 counter on it.)";
                final String abString = "DB$ PutCounter | Defined$ ReplacedCard | Tribute$ True | "
                        + "CounterType$ P1P1 | CounterNum$ " + tributeAmount
                        + " | ETB$ True | SubAbility$ TributeMoveToPlay";
                final String moveToPlay = "DB$ ChangeZone | Origin$ All | Destination$ Battlefield | "
                        + "Defined$ ReplacedCard | Hidden$ True";
                card.setSVar("TributeAddCounter", abString);
                card.setSVar("TributeMoveToPlay", moveToPlay);
                card.addReplacementEffect(ReplacementHandler.parseReplacement(actualRep, card, true));
            }
            else if (keyword.startsWith("Amplify")) {
                // find position of Amplify keyword
                final int ampPos = card.getKeywordPosition("Amplify");
                final String[] ampString = card.getKeywords().get(ampPos).split(":");
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
                // Setup ETB replacement effects
                final String actualRep = "Event$ Moved | Destination$ Battlefield | ValidCard$ Card.Self |"
                        + " ReplaceWith$ AmplifyReveal | Secondary$ True | Description$ As this creature "
                        + "enters the battlefield, put " + amplifyMagnitude + " +1/+1 counter" + suffix 
                        + " on it for each " + ampTypes.replace(",", " and/or ") 
                        + " card you reveal in your hand.)";
                final String abString = "DB$ Reveal | AnyNumber$ True | RevealValid$ "
                        + types.toString() + " | RememberRevealed$ True | SubAbility$ Amplify";
                final String dbString = "DB$ PutCounter | Defined$ ReplacedCard | CounterType$ P1P1 | "
                        + "CounterNum$ AmpMagnitude | References$ Revealed,AmpMagnitude | SubAbility$"
                        + " AmplifyMoveToPlay | ETB$ True";
                final String moveToPlay = "DB$ ChangeZone | Origin$ All | Destination$ Battlefield | "
                        + "Defined$ ReplacedCard | Hidden$ True | SubAbility$ DBCleanup";
                card.addReplacementEffect(ReplacementHandler.parseReplacement(actualRep, card, true));
                card.setSVar("AmplifyReveal", abString);
                card.setSVar("AmplifyMoveToPlay", moveToPlay);
                card.setSVar("Amplify", dbString);
                card.setSVar("DBCleanup", "DB$ Cleanup | ClearRemembered$ True");
                card.setSVar("AmpMagnitude", "SVar$Revealed/Times." + amplifyMagnitude);
                card.setSVar("Revealed", "Remembered$Amount");
            }
            else if (keyword.startsWith("Equip")) {
                // Check for additional params such as preferred AI targets
                final String equipString = keyword.substring(5);
                final String[] equipExtras = equipString.contains("|") ? equipString.split("\\|", 2) : null;
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
                abilityStr.append("| PrecostDesc$ Equip ");
                Cost cost = new Cost(equipCost, true);
                if (!cost.isOnlyManaCost()) { //Something other than a mana cost
                    abilityStr.append("- ");
                }
                abilityStr.append("| CostDesc$ " + cost.toSimpleString() + " ");
                abilityStr.append("| SpellDescription$ (" + cost.toSimpleString() + ": Attach to target creature you control. Equip only as a sorcery.)");
                // instantiate attach ability
                final SpellAbility sa = AbilityFactory.getAbility(abilityStr.toString(), card);
                card.addSpellAbility(sa);
                // add ability to instrinic strings so copies/clones create the ability also
                card.getCurrentState().addUnparsedAbility(abilityStr.toString());
            }
            else if (keyword.startsWith("Outlast")) {
                final String outlastString = keyword.substring(7);
                final String[] outlastExtras = outlastString.contains("|") ? outlastString.split("\\|", 2) : null;
                // Get cost string
                String outlastCost = "";
                if (outlastExtras != null) {
                    outlastCost = outlastExtras[0].trim();
                } else {
                    outlastCost = outlastString.trim();
                }
                // Create outlast ability string
                final StringBuilder abilityStr = new StringBuilder();
                abilityStr.append("AB$ PutCounter | Cost$ ");
                abilityStr.append(outlastCost);
                abilityStr.append(" T | Defined$ Self | CounterType$ P1P1 | CounterNum$ 1 ");
                abilityStr.append("| SorcerySpeed$ True | Outlast$ True ");
                if (outlastExtras != null) {
                    abilityStr.append("| ").append(outlastExtras[1]).append(" ");
                }
                abilityStr.append("| PrecostDesc$ Outlast ");
                Cost cost = new Cost(outlastCost, true);
                if (!cost.isOnlyManaCost()) { //Something other than a mana cost
                    abilityStr.append("- ");
                }
                abilityStr.append("| CostDesc$ " + cost.toSimpleString() + " ");
                abilityStr.append("| SpellDescription$ (" + cost.toSimpleString() + ", {T}: Put a +1/+1 counter on this creature. Outlast only as a sorcery.)");
                final SpellAbility sa = AbilityFactory.getAbility(abilityStr.toString(), card);
                card.addSpellAbility(sa);
                // add ability to instrinic strings so copies/clones create the ability also
                card.getCurrentState().addUnparsedAbility(abilityStr.toString());
            }
            else if (keyword.startsWith("Fortify")) {
                final String equipString = keyword.substring(7);
                final String[] equipExtras = equipString.contains("|") ? equipString.split("\\|", 2) : null;
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
                abilityStr.append(" | ValidTgts$ Land.YouCtrl | TgtPrompt$ Select target land you control ");
                abilityStr.append("| SorcerySpeed$ True | AILogic$ Pump | IsPresent$ Card.Self+nonCreature ");
                if (equipExtras != null) {
                    abilityStr.append("| ").append(equipExtras[1]).append(" ");
                }
                abilityStr.append("| PrecostDesc$ Fortify ");
                Cost cost = new Cost(equipCost, true);
                if (!cost.isOnlyManaCost()) { //Something other than a mana cost
                    abilityStr.append("- ");
                }
                abilityStr.append("| CostDesc$ " + cost.toSimpleString() + " ");
                abilityStr.append("| SpellDescription$ (" + cost.toSimpleString() + ": Attach to target land you control. Fortify only as a sorcery.)");
     
                // instantiate attach ability
                final SpellAbility sa = AbilityFactory.getAbility(abilityStr.toString(), card);
                card.addSpellAbility(sa);
                // add ability to intrinsic strings so copies/clones create the ability also
                card.getCurrentState().addUnparsedAbility(abilityStr.toString());
            }
            else if (keyword.startsWith("Bestow")) {
                final String[] params = keyword.split(":");
                final String cost = params[1];
                card.removeIntrinsicKeyword(keyword);       

                final StringBuilder sbAttach = new StringBuilder();
                sbAttach.append("SP$ Attach | Cost$ ");
                sbAttach.append(cost);
                sbAttach.append(" | AILogic$ ").append(params.length > 2 ? params[2] : "Pump");
                sbAttach.append(" | Bestow$ True | ValidTgts$ Creature");
                final SpellAbility bestow = AbilityFactory.getAbility(sbAttach.toString(), card);

                bestow.setDescription("Bestow " + ManaCostParser.parse(cost) + " (If you cast this"
                        + " card for its bestow cost, it's an Aura spell with enchant creature. It"
                        + " becomes a creature again if it's not attached to a creature.)");
                bestow.setStackDescription("Bestow - " + card.getName());
                bestow.setBasicSpell(false);
                card.addSpellAbility(bestow);
                card.getCurrentState().addUnparsedAbility(sbAttach.toString());
            }
            else if (keyword.equals("Hideaway")) {
                card.getCurrentState().addIntrinsicKeyword("CARDNAME enters the battlefield tapped.");

                final Trigger hideawayTrigger = TriggerHandler.parseTrigger("Mode$ ChangesZone | Destination$ Battlefield | ValidCard$ Card.Self | Execute$ TrigHideawayDig | TriggerDescription$ When CARDNAME enters the battlefield, look at the top four cards of your library, exile one face down, then put the rest on the bottom of your library.", card, true);
                card.addTrigger(hideawayTrigger);
                card.setSVar("TrigHideawayDig", "DB$ Dig | Defined$ You | DigNum$ 4 | DestinationZone$ Exile | ExileFaceDown$ True | RememberChanged$ True | SubAbility$ DBHideawayEffect");
                final Trigger gainControlTrigger = TriggerHandler.parseTrigger("Mode$ ChangesController | ValidCard$ Card.Self | Execute$ DBHideawayEffect | Static$ True", card, true);
                card.addTrigger(gainControlTrigger);
                card.setSVar("DBHideawayEffect", "DB$ Effect | StaticAbilities$ STHideawayEffectLookAtCard | Triggers$ THideawayEffectCleanup | SVars$ DBHideawayEffectExileSelf | ImprintOnHost$ True | Duration$ Permanent | SubAbility$ DBHideawayRemember");
                card.setSVar("STHideawayEffectLookAtCard", "Mode$ Continuous | Affected$ Card.IsRemembered | MayLookAt$ True | EffectZone$ Command | AffectedZone$ Exile | Description$ You may look at the exiled card.");
                card.setSVar("THideawayEffectCleanup", "Mode$ ChangesZone | ValidCard$ Card.IsRemembered | Origin$ Exile | Destination$ Any | TriggerZone$ Command | Execute$ DBHideawayEffectExileSelf | Static$ True");
                card.setSVar("DBHideawayEffectExileSelf", "DB$ ChangeZone | Defined$ Self | Origin$ Command | Destination$ Exile");
                final Trigger changeZoneTrigger = TriggerHandler.parseTrigger("Mode$ ChangesZone | ValidCard$ Card.IsRemembered | Origin$ Exile | Destination$ Any | TriggerZone$ Command | Execute$ DBHideawayCleanup | Static$ True", card, true);
                card.addTrigger(changeZoneTrigger);
                card.setSVar("DBHideawayRemember", "DB$ Animate | Defined$ Imprinted | RememberObjects$ Remembered | Permanent$ True");
                card.setSVar("DBHideawayCleanup", "DB$ Cleanup | ClearRemembered$ True");
            }
        }

        // Prowess
        final int prowess = card.getAmountOfKeyword("Prowess");
        card.removeIntrinsicKeyword("Prowess");
        final StringBuilder trigProwess = new StringBuilder(
                "Mode$ SpellCast | ValidCard$ Card.nonCreature | ValidActivatingPlayer$ You | "
                        + "Execute$ ProwessPump | TriggerZones$ Battlefield | TriggerDescription$ "
                        + "Prowess (Whenever you cast a noncreature spell, this creature gets +1/+1 "
                        + "until end of turn.)");

        final String abStringProwess = "DB$ Pump | Defined$ Self | NumAtt$ +1 | NumDef$ +1";
        card.getCurrentState().setSVar("ProwessPump", abStringProwess);
        final Trigger prowessTrigger = TriggerHandler.parseTrigger(trigProwess.toString(), card, true);
        for (int i = 0; i < prowess; i++) {
            card.getCurrentState().addTrigger(prowessTrigger);
            card.getCurrentState().setSVar("BuffedBy", "Card.nonCreature+nonLand"); // for the AI
        } // Prowess

        // AddCost
        if (card.hasSVar("FullCost")) {
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

        setupEtbKeywords(card);
    }

    /**
     * TODO: Write javadoc for this method.
     * @param card
     */
    private static void setupEtbKeywords(final Card card) {
        for (String kw : card.getKeywords()) {

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

                ReplacementEffect re = ReplacementHandler.parseReplacement(repEffsb.toString(), card, true);
                re.setLayer(layer);
                re.setOverridingAbility(repAb);

                card.addReplacementEffect(re);
            } else if (kw.startsWith("etbCounter")) {
                makeEtbCounter(kw, card, true);
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

                ReplacementEffect re = ReplacementHandler.parseReplacement(repeffstr, card, true);
                re.setLayer(ReplacementLayer.Other);

                card.addReplacementEffect(re);
            }
        }
    }


    private static ReplacementEffect makeEtbCounter(final String kw, final Card card, final boolean intrinsic)
    {
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
        String abStr = "DB$ PutCounter | Defined$ Self | CounterType$ " + splitkw[1]
                + " | ETB$ True | CounterNum$ " + amount +  " | SubAbility$ ETBCounterDBSVar";
        String dbStr = "DB$ ChangeZone | Hidden$ True | Origin$ All | Destination$ Battlefield"
                + "| Defined$ ReplacedCard";
        try {
            Integer.parseInt(amount);
        }
        catch (NumberFormatException ignored) {
            abStr += " | References$ " + amount;
        }
        card.setSVar("ETBCounterSVar", abStr);
        card.setSVar("ETBCounterDBSVar", dbStr);

        String repeffstr = "Event$ Moved | ValidCard$ Card.Self | Destination$ Battlefield "
                + "| ReplaceWith$ ETBCounterSVar | Description$ " + desc + (!extraparams.equals("") ? " | " + extraparams : "");

        ReplacementEffect re = ReplacementHandler.parseReplacement(repeffstr, card, intrinsic);
        re.setLayer(ReplacementLayer.Other);

        return card.addReplacementEffect(re);
    }
    
    public static void addTriggerAbility(final String keyword, final Card card, final KeywordsChange kws) {
        final boolean intrinsic = kws == null;

        if (keyword.equals("Exalted")) {
	        final StringBuilder trigExalted = new StringBuilder(
	                "Mode$ Attacks | ValidCard$ Creature.YouCtrl | Alone$ True | "
	                        + "Execute$ ExaltedPump | TriggerZones$ Battlefield | Secondary$ True | TriggerDescription$ "
	                        + "Exalted (" + Keyword.getInstance(keyword).getReminderText() + ")");

	        final String abStringExalted = "DB$ Pump | Defined$ TriggeredAttacker | NumAtt$ +1 | NumDef$ +1";
	        card.setSVar("ExaltedPump", abStringExalted);
	        final Trigger exaltedTrigger = TriggerHandler.parseTrigger(trigExalted.toString(), card, intrinsic);
	        final Trigger cardTrigger = card.addTrigger(exaltedTrigger);
            if (!intrinsic) {
                kws.addTrigger(cardTrigger);
            }
    	} else if (keyword.equals("Extort")) {
            final String extortTrigger = "Mode$ SpellCast | ValidCard$ Card | ValidActivatingPlayer$ You | "
                    + "TriggerZones$ Battlefield | Execute$ ExtortOpps | Secondary$ True"
                    + " | TriggerDescription$ Extort ("+ Keyword.getInstance(keyword).getReminderText() +")";
            final String abString = "AB$ LoseLife | Cost$ WB | Defined$ Player.Opponent | "
                    + "LifeAmount$ 1 | SubAbility$ ExtortGainLife";
            final String dbString = "DB$ GainLife | Defined$ You | LifeAmount$ AFLifeLost | References$ AFLifeLost";
            final Trigger parsedTrigger = TriggerHandler.parseTrigger(extortTrigger, card, intrinsic);
            final Trigger cardTrigger = card.addTrigger(parsedTrigger);
            card.setSVar("ExtortOpps", abString);
            card.setSVar("ExtortGainLife", dbString);
            card.setSVar("AFLifeLost", "Number$0");
            if (!intrinsic) {
                kws.addTrigger(cardTrigger);
            }
        }
    }

    public static void addReplacementEffect(final String keyword, final Card card, final KeywordsChange kws) {

    }

    public static void addSpellAbility(final String keyword, final Card card, final KeywordsChange kws) {

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
        
        final SpellAbility origSA = card.getSpellAbilities().getFirst();
        
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
        final String[] splitKeyword = card.getKeywords().get(hauntPos).split(":");
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

        card.getKeywords().remove(hauntPos);

        // First, create trigger that runs when the haunter goes to the
        // graveyard
        final StringBuilder sbHaunter = new StringBuilder();
        sbHaunter.append("Mode$ ChangesZone | Origin$ Battlefield | ");
        sbHaunter.append("Destination$ Graveyard | ValidCard$ Card.Self | ");
        sbHaunter.append("Static$ True | Secondary$ True | TriggerDescription$ Blank");

        final Trigger haunterDies = TriggerHandler.parseTrigger(sbHaunter.toString(), card, true);

        final Ability haunterDiesWork = new Ability(card, ManaCost.ZERO) {
            @Override
            public void resolve() {
                this.getTargets().getFirstTargetedCard().addHauntedBy(card);
                card.getGame().getAction().exile(card);
            }
        };
        haunterDiesWork.setDescription(hauntDescription);
        haunterDiesWork.setTargetRestrictions(new TargetRestrictions(null, new String[]{"Creature"}, "1", "1")); // not null to make stack preserve targets set

        final Ability haunterDiesSetup = new Ability(card, ManaCost.ZERO) {
            @Override
            public void resolve() {
                final Game game = card.getGame();
                this.setActivatingPlayer(card.getController());
                haunterDiesWork.setActivatingPlayer(card.getController());
                CardCollection allCreatures = CardLists.filter(game.getCardsIn(ZoneType.Battlefield), Presets.CREATURES);
                final CardCollection creats = CardLists.getTargetableCards(allCreatures, haunterDiesWork);
                if (creats.isEmpty()) {
                    return;
                }

                final Card toHaunt = card.getController().getController().chooseSingleEntityForEffect(creats, new SpellAbility.EmptySa(ApiType.InternalHaunt, card), "Choose target creature to haunt.");
                haunterDiesWork.setTargetCard(toHaunt);
                haunterDiesWork.setActivatingPlayer(card.getController());
                game.getStack().add(haunterDiesWork);
            }
        };

        haunterDies.setOverridingAbility(haunterDiesSetup);

        // Second, create the trigger that runs when the haunted creature dies
        final StringBuilder sbDies = new StringBuilder();
        sbDies.append("Mode$ ChangesZone | Origin$ Battlefield | Destination$ Graveyard | ");
        sbDies.append("ValidCard$ Creature.HauntedBy | Execute$ ").append(hauntSVarName);
        sbDies.append(" | TriggerDescription$ ").append(hauntDescription);

        final Trigger hauntedDies = forge.game.trigger.TriggerHandler.parseTrigger(sbDies.toString(), card, true);

        // Third, create the trigger that runs when the haunting creature
        // enters the battlefield
        final StringBuilder sbETB = new StringBuilder();
        sbETB.append("Mode$ ChangesZone | Destination$ Battlefield | ValidCard$ Card.Self | Execute$ ");
        sbETB.append(hauntSVarName).append(" | Secondary$ True | TriggerDescription$ ");
        sbETB.append(hauntDescription);

        final Trigger haunterETB = forge.game.trigger.TriggerHandler.parseTrigger(sbETB.toString(), card, true);

        // Fourth, create a trigger that removes the haunting status if the
        // haunter leaves the exile
        final StringBuilder sbUnExiled = new StringBuilder();
        sbUnExiled.append("Mode$ ChangesZone | Origin$ Exile | Destination$ Any | ");
        sbUnExiled.append("ValidCard$ Card.Self | Static$ True | Secondary$ True | ");
        sbUnExiled.append("TriggerDescription$ Blank");

        final Trigger haunterUnExiled = forge.game.trigger.TriggerHandler.parseTrigger(sbUnExiled.toString(), card,
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
                    + " | SpellDescription$ " + abilityDescription;

            final SpellAbility sa = AbilityFactory.getAbility(abString, card);
            sa.setPayCosts(new Cost(card.getManaCost(), false));
            card.addSpellAbility(sa);
        }

        card.addTrigger(hauntedDies);
        card.addTrigger(haunterUnExiled);
    }

    /**
     * TODO: Write javadoc for this method.
     * @param card
     * @param altCost
     * @param sa
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
        if (params.containsKey("References")) {
            for (String svar : params.get("References").split(",")) {
            	altCostSA.setSVar(svar, card.getSVar(svar));
            }
        }
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
        
        final SpellAbility evokedSpell = new SpellPermanent(card) {
            private static final long serialVersionUID = -1598664196463358630L;

            @Override
            public void resolve() {
                final Game game = card.getGame();
                card.setEvoked(true);
                game.getAction().moveToPlay(card);
            }
        };
        card.removeIntrinsicKeyword(evokeKeyword);
        final StringBuilder desc = new StringBuilder();
        desc.append("Evoke ").append(evokedCost.toSimpleString());
        desc.append(" (You may cast this spell for its evoke cost. ");
        desc.append("If you do, when it enters the battlefield, sacrifice it.)");

        evokedSpell.setDescription(desc.toString());

        final StringBuilder sb = new StringBuilder();
        sb.append(card.getName()).append(" (Evoked)");
        evokedSpell.setStackDescription(sb.toString());
        evokedSpell.setBasicSpell(false);
        evokedSpell.setPayCosts(evokedCost);
        return evokedSpell;
    }

    private static final Map<String,String> emptyMap = new TreeMap<String,String>();
    public static void setupETBReplacementAbility(SpellAbility sa) {
        sa.appendSubAbility(new AbilitySub(ApiType.InternalEtbReplacement, sa.getHostCard(), null, emptyMap));
        // ETBReplacementMove(sa.getHostCard(), null));
    }

    /**
     * make Dash keyword
     * @param card
     * @param dashKeyword
     * @return
     */
    private static SpellAbility makeDashSpell(final Card card, final String dashKeyword) {
        final String[] k = dashKeyword.split(":");
        final Cost dashCost = new Cost(k[1], false);
        card.removeIntrinsicKeyword(dashKeyword);
        final String dashString = "SP$ PermanentCreature | Cost$ " + k[1] + " | SubAbility$"
                + " DashPump";
        final String dbHaste = "DB$ Pump | Defined$ Self | KW$ Haste | Permanent$ True"
                + " | SubAbility$ DashDelayedTrigger";
        final String dbDelayTrigger = "DB$ DelayedTrigger | Mode$ Phase | Phase$"
                + " End of Turn | Execute$ DashReturnSelf | RememberObjects$ Self"
                + " | TriggerDescription$ Return CARDNAME from the battlefield to"
                + " its owner's hand.";
        final String dbReturn = "DB$ ChangeZone | Origin$ Battlefield | Destination$ Hand"
                + " | Defined$ DelayTriggerRemembered";
        card.setSVar("DashPump", dbHaste);
        card.setSVar("DashDelayedTrigger", dbDelayTrigger);
        card.setSVar("DashReturnSelf", dbReturn);

        final SpellAbility dashSpell = AbilityFactory.getAbility(dashString, card);
        String desc = "Dash " + dashCost.toSimpleString() + " (You may cast this "
                + "spell for its dash cost. If you do, it gains haste, and it's "
                + "returned from the battlefield to its owner's hand at the beginning"
                + " of the next end step.)";
        dashSpell.setStackDescription(card.getName() + " (Dash)");
        dashSpell.setDescription(desc);
        dashSpell.setBasicSpell(false);
        dashSpell.setPayCosts(dashCost);
        dashSpell.setDash(true);
        return dashSpell;
    }

    /**
     * make Awaken keyword
     * @param card
     * @param awakenKeyword
     * @return
     */
    private static SpellAbility makeAwakenSpell(final Card card, final String awakenKeyword) {
        final String[] k = awakenKeyword.split(":");
        final String counters = k[1];
        final String suffix = !counters.equals("1") ? "s" : "";
        final Cost awakenCost = new Cost(k[2], false);
        // Leave intrinsic Keyword for retrieval by Halimar Tidecaller
        //card.removeIntrinsicKeyword(awakenKeyword);
        
        final SpellAbility awakenSpell = card.getFirstSpellAbility().copy();

        // get the last subability of the spell to attach awaken in the end
        SpellAbility lastSub = awakenSpell;
        while (lastSub.getSubAbility() != null) {
            final AbilitySub copySubSA = ((AbilitySub) lastSub.getSubAbility()).getCopy();
            lastSub.setSubAbility(copySubSA);
            lastSub = copySubSA;
        }

        final String awaken = "DB$ PutCounter | CounterType$ P1P1 | CounterNum$ "+ counters + " | "
                + "ValidTgts$ Land.YouCtrl | TgtPrompt$ Select target land you control | SubAbility$"
                + " AwakenAnimate";
        final String dbAnimate = "DB$ Animate | Defined$ Targeted | Power$ 0 | Toughness$ 0 | Types$"
                + " Creature,Elemental | Permanent$ True | Keywords$ Haste";
        card.setSVar("AwakenAnimate", dbAnimate);
        final AbilitySub awakenSub = (AbilitySub) AbilityFactory.getAbility(awaken, card);
        lastSub.setSubAbility(awakenSub);
        String desc = "Awaken " + counters + " - " + awakenCost.toSimpleString() + " (If you cast "
                + "this spell for " + awakenCost.toSimpleString() + ", also put " + counters
                + " +1/+1 counter"+ suffix + " on target land you control and it becomes a 0/0 "
                + "Elemental creature with haste. It's still a land.)";
        awakenSpell.setDescription(desc);
        awakenSpell.setBasicSpell(false);
        awakenSpell.setPayCosts(awakenCost);
        return awakenSpell;
    }

    /**
     * make Surge keyword
     * @param card
     * @param surgeKeyword
     * @return
     */
    private static SpellAbility makeSurgeSpell(final Card card, final String surgeKeyword) {
        final String[] k = surgeKeyword.split(":");
        final Cost surgeCost = new Cost(k[1], false);
        card.removeIntrinsicKeyword(surgeKeyword);
        final SpellAbility surgeSpell = card.getFirstSpellAbility().copy();

        surgeSpell.setPayCosts(surgeCost);
        surgeSpell.setBasicSpell(false);
        surgeSpell.addOptionalCost(OptionalCost.Surge);

        final SpellAbilityRestriction restriction = new SpellAbilityRestriction();
        restriction.setVariables(card.getFirstSpellAbility().getRestrictions());
        restriction.setSurge(true);
        surgeSpell.setRestrictions(restriction);
        String desc = "Surge " + surgeCost.toSimpleString() + " (You may cast this spell for its "
                + "surge cost if you or a teammate has cast another spell this turn.)";
        surgeSpell.setDescription(desc);
        return surgeSpell;
    }

    /**
     * <p>
     * hasKeyword.
     * </p>
     * 
     * @param c
     *            a {@link forge.game.card.Card} object.
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
     *            a {@link forge.game.card.Card} object.
     * @param k
     *            a {@link java.lang.String} object.
     * @param startPos
     *            a int.
     * @return a int.
     */
    private static final int hasKeyword(final Card c, final String k, final int startPos) {
        final List<String> a = c.getKeywords();
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
     *            a {@link forge.game.card.Card} object.
     * @param cardName
     *            a {@link java.lang.String} object.
     * 
     */
    public static final void parseKeywords(final Card card, final String cardName) {
        if (hasKeyword(card, "Sunburst") != -1) {
            final GameCommand sunburstCIP = new GameCommand() {
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

            final GameCommand sunburstLP = new GameCommand() {
                private static final long serialVersionUID = -7564420917490677427L;

                @Override
                public void run() {
                    card.setSunburstValue(0);
                }
            };

            card.addComesIntoPlayCommand(sunburstCIP);
            card.addLeavesPlayCommand(sunburstLP);
        }

        if (hasKeyword(card, "Devoid") != -1) {
            card.setColor("1");
        }

        if (hasKeyword(card, "Morph") != -1) {
            final int n = hasKeyword(card, "Morph");
            if (n != -1) {

                final String parse = card.getKeywords().get(n).toString();
                Map<String, String> sVars = card.getSVars();

                final String[] k = parse.split(":");
                final Cost cost = new Cost(k[1], true);

                card.addSpellAbility(abilityMorphDown(card));

                card.setState(CardStateName.FaceDown, false);

                card.addSpellAbility(abilityMorphUp(card, cost, false));
                card.setSVars(sVars); // for Warbreak Trumpeter.

                card.setState(CardStateName.Original, false);
            }

        } // Morph
        if (hasKeyword(card, "Megamorph") != -1) {
            final int n = hasKeyword(card, "Megamorph");
            if (n != -1) {

                final String parse = card.getKeywords().get(n).toString();
                Map<String, String> sVars = card.getSVars();

                final String[] k = parse.split(":");
                final Cost cost = new Cost(k[1], true);

                card.addSpellAbility(abilityMorphDown(card));

                card.setState(CardStateName.FaceDown, false);

                card.addSpellAbility(abilityMorphUp(card, cost, true));
                card.setSVars(sVars);
                card.setState(CardStateName.Original, false);
            }
        } // Megamorph

        if (hasKeyword(card, "Madness") != -1) {
            final int n = hasKeyword(card, "Madness");
            if (n != -1) {
                // Set Madness Replacement effects
                String repeffstr = "Event$ Discard | ActiveZones$ Hand | ValidCard$ Card.Self | " +
                		"ReplaceWith$ DiscardMadness | Secondary$ True | Description$ If you" +
                		" discard this card, discard it into exile.";
                ReplacementEffect re = ReplacementHandler.parseReplacement(repeffstr, card, true);
                card.addReplacementEffect(re);
                String sVarMadness = "DB$ Discard | Defined$ ReplacedPlayer" +
                		" | Mode$ Defined | DefinedCards$ ReplacedCard | Madness$ True";
                card.setSVar("DiscardMadness", sVarMadness);

                // Set Madness Triggers
                final String parse = card.getKeywords().get(n).toString();
                // card.removeIntrinsicKeyword(parse);
                final String[] k = parse.split(":");
                String trigStr = "Mode$ Discarded | ValidCard$ Card.Self | IsMadness$ True | " +
                		"Execute$ TrigPlayMadness | Secondary$ True | TriggerDescription$ " +
                		"Play Madness - " + card.getName();
                final Trigger myTrigger = TriggerHandler.parseTrigger(trigStr, card, true);
                card.addTrigger(myTrigger);
                String playMadness = "AB$ Play | Cost$ 0 | Defined$ Self | PlayMadness$ " + k[1] + 
                		" | Optional$ True | SubAbility$ DBWasNotPlayMadness | RememberPlayed$ True";
                String moveToYard = "DB$ ChangeZone | Defined$ Self | Origin$ Exile | " +
                		"Destination$ Graveyard | ConditionDefined$ Remembered | ConditionPresent$" +
                		" Card | ConditionCompare$ EQ0 | SubAbility$ DBMadnessCleanup";
                String cleanUp = "DB$ Cleanup | ClearRemembered$ True";
                card.setSVar("TrigPlayMadness", playMadness);
                card.setSVar("DBWasNotPlayMadness", moveToYard);
                card.setSVar("DBMadnessCleanup", cleanUp);
            }
        } // madness

        if (hasKeyword(card, "Miracle") != -1) {
            final int n = hasKeyword(card, "Miracle");
            if (n != -1) {
                final String parse = card.getKeywords().get(n).toString();
                // card.removeIntrinsicKeyword(parse);

                final String[] k = parse.split(":");
                card.setMiracleCost(new Cost(k[1], false));
            }
        } // miracle

        if (hasKeyword(card, "Devour") != -1) {
            final int n = hasKeyword(card, "Devour");
            if (n != -1) {

                final String parse = card.getKeywords().get(n).toString();
                // card.removeIntrinsicKeyword(parse);

                final String[] k = parse.split(":");
                final String magnitude = k[1];

                String abStr = "DB$ ChangeZone | Hidden$ True | Origin$ All | Destination$ Battlefield"
                        + " | Defined$ ReplacedCard | SubAbility$ DevourCleanup";
                String dbStr = "DB$ Sacrifice | Defined$ You | Amount$ DevourSacX | "
                        + "References$ DevourSacX | SacValid$ Creature.Other | SacMessage$ creature | "
                        + "RememberSacrificed$ True | Optional$ True | "
                        + "Devour$ True | SubAbility$ DevourCounters";
                String counterStr = "DB$ PutCounter | Defined$ Self | CounterType$ P1P1 | CounterNum$ DevourX"
                        + " | ETB$ True | References$ DevourX,DevourSize | SubAbility$ DevourETB";

                card.setSVar("DevourETB", abStr);
                card.setSVar("DevourSac", dbStr);
                card.setSVar("DevourSacX", "Count$Valid Creature.YouCtrl+Other");
                card.setSVar("DevourCounters", counterStr);
                card.setSVar("DevourX", "SVar$DevourSize/Times." + magnitude);
                card.setSVar("DevourSize", "Count$RememberedSize");
                card.setSVar("DevourCleanup", "DB$ Cleanup | ClearRemembered$ True");

                String repeffstr = "Event$ Moved | ValidCard$ Card.Self | Destination$ Battlefield | ReplaceWith$ DevourSac";

                ReplacementEffect re = ReplacementHandler.parseReplacement(repeffstr, card, true);
                re.setLayer(ReplacementLayer.Other);
                card.addReplacementEffect(re);
            }
        } // Devour

        if (hasKeyword(card, "Modular") != -1) {
            final int n = hasKeyword(card, "Modular");
            if (n != -1) {
                final String parse = card.getKeywords().get(n).toString();
                card.getKeywords().remove(parse);

                final int m = Integer.parseInt(parse.substring(8));

                card.addIntrinsicKeyword("etbCounter:P1P1:" + m + ":no Condition: " +
                        "Modular " + m + " (This enters the battlefield with " + m + " +1/+1 counters on it. When it's put into a graveyard, " +
                        "you may put its +1/+1 counters on target artifact creature.)");

                final String abStr = "AB$ PutCounter | Cost$ 0 | References$ ModularX | ValidTgts$ Artifact.Creature | " +
                        "TgtPrompt$ Select target artifact creature | CounterType$ P1P1 | CounterNum$ ModularX";
                card.setSVar("ModularTrig", abStr);
                card.setSVar("ModularX", "TriggeredCard$CardCounters.P1P1");
                
                String trigStr = "Mode$ ChangesZone | ValidCard$ Card.Self | Origin$ Battlefield | Destination$ Graveyard" +
                        " | OptionalDecider$ TriggeredCardController | TriggerController$ TriggeredCardController | Execute$ ModularTrig | " +
                        "Secondary$ True | TriggerDescription$ When CARDNAME is put into a graveyard from the battlefield, " +
                        "you may put a +1/+1 counter on target artifact creature for each +1/+1 counter on CARDNAME";
                final Trigger myTrigger = TriggerHandler.parseTrigger(trigStr, card, true);
                card.addTrigger(myTrigger);
            }
        } // Modular
        

        /*
         * WARNING: must keep this keyword processing before etbCounter keyword
         * processing.
         */
        final int graft = hasKeyword(card, "Graft");
        if (graft != -1) {
            final String parse = card.getKeywords().get(graft).toString();

            final int m = Integer.parseInt(parse.substring(6));
            final String abStr = "AB$ MoveCounter | Cost$ 0 | Source$ Self | "
                    + "Defined$ TriggeredCardLKICopy | CounterType$ P1P1 | CounterNum$ 1";
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
            final String numCounters = card.getKeywords().get(bloodthirst).split(" ")[1];
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

        final int storm = card.getAmountOfKeyword("Storm");
        for (int i = 0; i < storm; i++) {
            final StringBuilder trigScript = new StringBuilder(
                    "Mode$ SpellCast | ValidCard$ Card.Self | Execute$ Storm "
                            + "| TriggerDescription$ Storm (When you cast this spell, "
                            + "copy it for each spell cast before it this turn.)");

            card.setSVar("Storm", "AB$ CopySpellAbility | Cost$ 0 | Defined$ TriggeredSpellAbility | Amount$ StormCount | References$ StormCount");
            card.setSVar("StormCount", "TriggerCount$CurrentStormCount/Minus.1");
            final Trigger stormTrigger = TriggerHandler.parseTrigger(trigScript.toString(), card, true);

            card.addTrigger(stormTrigger);
        } // Storm
        final int cascade = card.getAmountOfKeyword("Cascade");
        for (int i = 0; i < cascade; i++) {
            final StringBuilder trigScript = new StringBuilder(
                    "Mode$ SpellCast | ValidCard$ Card.Self | Execute$ TrigCascade | Secondary$ " +
                    "True | TriggerDescription$ Cascade - CARDNAME");

            final String abString = "AB$ DigUntil | Cost$ 0 | Defined$ You | Amount$ 1 | Valid$ "
                    + "Card.nonLand+cmcLTCascadeX | FoundDestination$ Exile | RevealedDestination$"
                    + " Exile | References$ CascadeX | ImprintRevealed$ True | RememberFound$ True"
                    + " | SubAbility$ CascadeCast";
            final String dbCascadeCast = "DB$ Play | Defined$ Remembered | WithoutManaCost$ True | "
                    + "Optional$ True | SubAbility$ CascadeMoveToLib";
            final String dbMoveToLib = "DB$ ChangeZoneAll | ChangeType$ Card.IsRemembered,Card.IsImprinted"
                    + " | Origin$ Exile | Destination$ Library | RandomOrder$ True | LibraryPosition$ -1"
                    + " | SubAbility$ CascadeCleanup";
            card.setSVar("TrigCascade", abString);
            card.setSVar("CascadeCast", dbCascadeCast);
            card.setSVar("CascadeMoveToLib", dbMoveToLib);
            card.setSVar("CascadeX", "Count$CardManaCost");
            card.setSVar("CascadeCleanup", "DB$ Cleanup | ClearRemembered$ True | ClearImprinted$ True");
            final Trigger cascadeTrigger = TriggerHandler.parseTrigger(trigScript.toString(), card, true);

            card.addTrigger(cascadeTrigger);
        } // Cascade

        if (hasKeyword(card, "Recover") != -1) {
            final String recoverCost = card.getKeywords().get(card.getKeywordPosition("Recover")).split(":")[1];
            final String abStr = "AB$ ChangeZone | Cost$ 0 | Defined$ Self"
            		+ " | Origin$ Graveyard | Destination$ Hand | UnlessCost$ "
                    + recoverCost + " | UnlessPayer$ You | UnlessSwitched$ True"
                    + " | UnlessResolveSubs$ WhenNotPaid | SubAbility$ RecoverExile";
            card.setSVar("RecoverTrig", abStr);
            card.setSVar("RecoverExile", "DB$ ChangeZone | Defined$ Self"
            		+ " | Origin$ Graveyard | Destination$ Exile");
            String trigObject = card.isCreature() ? "Creature.Other+YouOwn" : "Creature.YouOwn";
            String trigArticle = card.isCreature() ? "another" : "a";
            String trigStr = "Mode$ ChangesZone | ValidCard$ " + trigObject
            		+ " | Origin$ Battlefield | Destination$ Graveyard | "
            		+ "TriggerZones$ Graveyard | Execute$ RecoverTrig | "
            		+ "TriggerDescription$ When " + trigArticle + " creature is "
            		+ "put into your graveyard from the battlefield, you "
            		+ "may pay " + recoverCost + ". If you do, return "
            		+ "CARDNAME from your graveyard to your hand. Otherwise,"
            		+ " exile CARDNAME. | Secondary$ True";
            final Trigger myTrigger = TriggerHandler.parseTrigger(trigStr, card, true);
            card.addTrigger(myTrigger);
        } // Recover

        int ripplePos = hasKeyword(card, "Ripple");
        while (ripplePos != -1) {
            final int n = ripplePos;
            final String parse = card.getKeywords().get(n);
            final String[] k = parse.split(":");
            final int num = Integer.parseInt(k[1]);
            final String triggerSvar = "DBRipple";

            final String actualTrigger = "Mode$ SpellCast | ValidCard$ Card.Self | " +
                    "Execute$ " + triggerSvar + " | Secondary$ True | TriggerDescription$" +
                    " Ripple " + num + " - CARDNAME | OptionalDecider$ You";
            final String abString = "AB$ Dig | Cost$ 0 | NoMove$ True | DigNum$ " + num +
                    " | Reveal$ True | RememberRevealed$ True | SubAbility$ DBCastRipple";
            final String dbCast = "DB$ Play | Valid$ Card.IsRemembered+sameName | " +
                    "ValidZone$ Library | WithoutManaCost$ True | Optional$ True | " +
                    "Amount$ All | SubAbility$ RippleMoveToBottom";

            card.setSVar(triggerSvar.toString(), abString);
            card.setSVar("DBCastRipple", dbCast);
            card.setSVar("RippleMoveToBottom", "DB$ ChangeZoneAll | ChangeType$ " +
                    "Card.IsRemembered | Origin$ Library | Destination$ Library | " +
                    "LibraryPosition$ -1 | SubAbility$ RippleCleanup");
            card.setSVar("RippleCleanup", "DB$ Cleanup | ClearRemembered$ True");

            final Trigger parsedTrigger = TriggerHandler.parseTrigger(actualTrigger, card, true);
            card.addTrigger(parsedTrigger);
            
            ripplePos = hasKeyword(card, "Ripple", n + 1);
        } // Ripple

        final int dethrone = card.getAmountOfKeyword("Dethrone");
        card.removeIntrinsicKeyword("Dethrone");
        for (int i = 0; i < dethrone; i++) {
            final StringBuilder trigScript = new StringBuilder(
                    "Mode$ Attacks | ValidCard$ Card.Self | Attacked$ Player.withMostLife | "
                    + "TriggerZones$ Battlefield | Execute$ DethroneCounters | TriggerDescription$"
                    + " Dethrone (Whenever this creature attacks the player with the most life or "
                    + "tied for the most life, put a +1/+1 counter on it.)");

            final String abString = "DB$ PutCounter | Defined$ Self | CounterType$ P1P1 | "
                    + "CounterNum$ 1";
            card.setSVar("DethroneCounters", abString);
            final Trigger cascadeTrigger = TriggerHandler.parseTrigger(trigScript.toString(), card, true);
            card.addTrigger(cascadeTrigger);
        } // Dethrone

        final int exploit = card.getAmountOfKeyword("Exploit");
        card.removeIntrinsicKeyword("Exploit");
        final StringBuilder trigExploit = new StringBuilder(
                "Mode$ ChangesZone | ValidCard$ Card.Self | Origin$ Any | Destination$ Battlefield"
                + " | Execute$ ExploitSac | TriggerDescription$ Exploit (When this creature enters"
                + " the battlefield, you may sacrifice a creature.)");
        final String abStringExploit = "DB$ Sacrifice | SacValid$ Creature | Exploit$ True | Optional$ True";
        card.setSVar("ExploitSac", abStringExploit);
        final Trigger exploitTrigger = TriggerHandler.parseTrigger(trigExploit.toString(), card, true);
        for (int i = 0; i < exploit; i++) {
            card.addTrigger(exploitTrigger);
        } // Exploit
        final int ingest = card.getAmountOfKeyword("Ingest");
        card.removeIntrinsicKeyword("Ingest");
        final StringBuilder trigIngest = new StringBuilder(
                "Mode$ DamageDone | ValidSource$ Card.Self | ValidTarget$ Player | CombatDamage$ True"
                + " | Execute$ IngestExile | TriggerZones$ Battlefield | TriggerDescription$ Ingest "
                + "(Whenever this creature deals combat damage to a player, that player exiles the "
                + "top card of his or her library.)");
        final String abStringIngest = "DB$ Mill | NumCards$ 1 | Destination$ Exile | Defined$ TriggeredTarget";
        card.setSVar("IngestExile", abStringIngest);
        final Trigger ingestTrigger = TriggerHandler.parseTrigger(trigIngest.toString(), card, true);
        for (int i = 0; i < ingest; i++) {
            card.addTrigger(ingestTrigger);
        } // Ingest
    }

    public final static void refreshTotemArmor(Card c) {
        boolean hasKw = c.hasKeyword("Totem armor");

        CardState state = c.getCurrentState();
        FCollectionView<ReplacementEffect> res = state.getReplacementEffects();
        for (int ix = 0; ix < res.size(); ix++) {
            ReplacementEffect re = res.get(ix);
            if (re.getMapParams().containsKey("TotemArmor")) {
                if (hasKw) { return; } // has re and kw - nothing to do here
                state.removeReplacementEffect(re);
                ix--;
            }
        }

        if (hasKw) { 
            ReplacementEffect re = ReplacementHandler.parseReplacement("Event$ Destroy | ActiveZones$ Battlefield | ValidCard$ Card.EnchantedBy | ReplaceWith$ RegenTA | Secondary$ True | TotemArmor$ True | Description$ Totem armor - " + c, c, true);
            c.getSVars().put("RegenTA", "AB$ DealDamage | Cost$ 0 | Defined$ ReplacedCard | Remove$ All | SubAbility$ DestroyMe");
            c.getSVars().put("DestroyMe", "DB$ Destroy | Defined$ Self");
            state.addReplacementEffect(re);
        }
    }
}
