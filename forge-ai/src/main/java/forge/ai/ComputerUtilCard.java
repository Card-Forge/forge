package forge.ai;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;

import forge.card.CardType;
import forge.card.ColorSet;
import forge.card.MagicColor;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.*;
import forge.game.combat.Combat;
import forge.game.combat.CombatUtil;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbility;
import forge.game.zone.MagicStack;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;
import forge.util.Aggregates;
import forge.util.MyRandom;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.Map.Entry;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class ComputerUtilCard {

    /**
     * <p>
     * getMostExpensivePermanentAI.
     * </p>
     * 
     * @param list
     *            a {@link forge.CardList} object.
     * @param spell
     *            a {@link forge.game.card.Card} object.
     * @param targeted
     *            a boolean.
     * @return a {@link forge.game.card.Card} object.
     */
    public static Card getMostExpensivePermanentAI(final List<Card> list, final SpellAbility spell, final boolean targeted) {
        List<Card> all = list;
        if (targeted) {
            all = CardLists.filter(all, new Predicate<Card>() {
                @Override
                public boolean apply(final Card c) {
                    return c.canBeTargetedBy(spell);
                }
            });
        }
    
        return ComputerUtilCard.getMostExpensivePermanentAI(all);
    }

    
    /**
     * <p>
     * Sorts a List<Card> by "best" using the EvaluateCreature function.
     * the best creatures will be first in the list.
     * </p>
     * 
     * @param list
     *            a {@link forge.CardList} object.
     */
    public static void sortByEvaluateCreature(final List<Card> list) {
        Collections.sort(list, ComputerUtilCard.EvaluateCreatureComparator);
    } // sortByEvaluateCreature()
    
    // The AI doesn't really pick the best artifact, just the most expensive.
    /**
     * <p>
     * getBestArtifactAI.
     * </p>
     * 
     * @param list
     *            a {@link forge.CardList} object.
     * @return a {@link forge.game.card.Card} object.
     */
    public static Card getBestArtifactAI(final List<Card> list) {
        List<Card> all = CardLists.filter(list, CardPredicates.Presets.ARTIFACTS);
        if (all.size() == 0) {
            return null;
        }
        // get biggest Artifact
        return Aggregates.itemWithMax(all, CardPredicates.Accessors.fnGetCmc);
    }

    // The AI doesn't really pick the best enchantment, just the most expensive.
    /**
     * <p>
     * getBestEnchantmentAI.
     * </p>
     * 
     * @param list
     *            a {@link forge.CardList} object.
     * @param spell
     *            a {@link forge.game.card.Card} object.
     * @param targeted
     *            a boolean.
     * @return a {@link forge.game.card.Card} object.
     */
    public static Card getBestEnchantmentAI(final List<Card> list, final SpellAbility spell, final boolean targeted) {
        List<Card> all = CardLists.filter(list, CardPredicates.Presets.ENCHANTMENTS);
        if (targeted) {
            all = CardLists.filter(all, new Predicate<Card>() {
    
                @Override
                public boolean apply(final Card c) {
                    return c.canBeTargetedBy(spell);
                }
            });
        }
    
        // get biggest Enchantment
        return Aggregates.itemWithMax(all, CardPredicates.Accessors.fnGetCmc);
    }

    /**
     * <p>
     * getBestLandAI.
     * </p>
     * 
     * @param list
     *            a {@link forge.CardList} object.
     * @return a {@link forge.game.card.Card} object.
     */
    public static Card getBestLandAI(final Collection<Card> list) {
        final List<Card> land = CardLists.filter(list, CardPredicates.Presets.LANDS);
        if (land.isEmpty()) {
            return null;
        }
    
        // prefer to target non basic lands
        final List<Card> nbLand = CardLists.filter(land, Predicates.not(CardPredicates.Presets.BASIC_LANDS));
    
        if (!nbLand.isEmpty()) {
            // TODO - Rank non basics?
            return Aggregates.random(nbLand);
        }
    
        // if no non-basic lands, target the least represented basic land type
        String sminBL = "";
        int iminBL = 20000; // hopefully no one will ever have more than 20000
                            // lands of one type....
        int n = 0;
        for (String name : MagicColor.Constant.BASIC_LANDS) {
            n = CardLists.getType(land, name).size();
            if ((n < iminBL) && (n > 0)) {
                // if two or more are tied, only the
                // first
                // one checked will be used
                iminBL = n;
                sminBL = name;
            }
        }
        if (iminBL == 20000) {
            return null; // no basic land was a minimum
        }
    
        final List<Card> bLand = CardLists.getType(land, sminBL);
    
        for (Card ut : Iterables.filter(bLand, CardPredicates.Presets.UNTAPPED)) {
            return ut;
        }
    
    
        return Aggregates.random(bLand); // random tapped land of least represented type
    }

    /**
     * <p>
     * getCheapestPermanentAI.
     * </p>
     * 
     * @param list
     *            a {@link forge.CardList} object.
     * @param spell
     *            a {@link forge.game.card.Card} object.
     * @param targeted
     *            a boolean.
     * @return a {@link forge.game.card.Card} object.
     */
    public static Card getCheapestPermanentAI(Collection<Card> all, final SpellAbility spell, final boolean targeted) {
        if (targeted) {
            all = CardLists.filter(all, new Predicate<Card>() {
                @Override
                public boolean apply(final Card c) {
                    return c.canBeTargetedBy(spell);
                }
            });
        }
        if (all.isEmpty()) {
            return null;
        }
    
        // get cheapest card:
        Card cheapest = null;
    
        for (Card c : all) {
            if (cheapest == null || cheapest.getManaCost().getCMC() <= cheapest.getManaCost().getCMC()) {
                cheapest = c;
            }
        }
    
        return cheapest;
    
    }

    // returns null if list.size() == 0
    /**
     * <p>
     * getBestAI.
     * </p>
     * 
     * @param list
     *            a {@link forge.CardList} object.
     * @return a {@link forge.game.card.Card} object.
     */
    public static Card getBestAI(final Collection<Card> list) {
        // Get Best will filter by appropriate getBest list if ALL of the list
        // is of that type
        if (Iterables.all(list, CardPredicates.Presets.CREATURES))
            return ComputerUtilCard.getBestCreatureAI(list);
    
        if (Iterables.all(list, CardPredicates.Presets.LANDS))
            return getBestLandAI(list);
    
        // TODO - Once we get an EvaluatePermanent this should call
        // getBestPermanent()
        return ComputerUtilCard.getMostExpensivePermanentAI(list);
    }

    /**
     * getBestCreatureAI.
     * 
     * @param list
     *            the list
     * @return the card
     */
    public static Card getBestCreatureAI(final Collection<Card> list) {
        return Aggregates.itemWithMax(Iterables.filter(list, CardPredicates.Presets.CREATURES), ComputerUtilCard.fnEvaluateCreature);
    }

    /**
     * <p>
     * getWorstCreatureAI.
     * </p>
     * 
     * @param list
     *            a {@link forge.CardList} object.
     * @return a {@link forge.game.card.Card} object.
     */
    public static Card getWorstCreatureAI(final Collection<Card> list) {
        return Aggregates.itemWithMin(Iterables.filter(list, CardPredicates.Presets.CREATURES), ComputerUtilCard.fnEvaluateCreature);
    }

    // This selection rates tokens higher
    /**
     * <p>
     * getBestCreatureToBounceAI.
     * </p>
     * 
     * @param list
     *            a {@link forge.CardList} object.
     * @return a {@link forge.game.card.Card} object.
     */
    public static Card getBestCreatureToBounceAI(final List<Card> list) {
        final int tokenBonus = 60;
        Card biggest = null;
        int biggestvalue = -1;

        for(Card card : CardLists.filter(list, CardPredicates.Presets.CREATURES)) {
            int newvalue = ComputerUtilCard.evaluateCreature(card);
            newvalue += card.isToken() ? tokenBonus : 0; // raise the value of tokens

            if (biggestvalue < newvalue) {
                biggest = card;
                biggestvalue = newvalue;
            }
        }
        return biggest;
    }

    /**
     * <p>
     * getWorstAI.
     * </p>
     * 
     * @param list
     *            a {@link forge.CardList} object.
     * @return a {@link forge.game.card.Card} object.
     */
    public static Card getWorstAI(final Collection<Card> list) {
        return ComputerUtilCard.getWorstPermanentAI(list, false, false, false, false);
    }

    /**
     * <p>
     * getWorstPermanentAI.
     * </p>
     * 
     * @param list
     *            a {@link forge.CardList} object.
     * @param biasEnch
     *            a boolean.
     * @param biasLand
     *            a boolean.
     * @param biasArt
     *            a boolean.
     * @param biasCreature
     *            a boolean.
     * @return a {@link forge.game.card.Card} object.
     */
    public static Card getWorstPermanentAI(final Collection<Card> list, final boolean biasEnch, final boolean biasLand,
            final boolean biasArt, final boolean biasCreature) {
        if (list.size() == 0) {
            return null;
        }
        
        final boolean hasEnchantmants = Iterables.any(list, CardPredicates.Presets.ENCHANTMENTS);
        if (biasEnch && hasEnchantmants) {
            return getCheapestPermanentAI(CardLists.filter(list, CardPredicates.Presets.ENCHANTMENTS), null, false);
        }
    
        final boolean hasArtifacts = Iterables.any(list, CardPredicates.Presets.ARTIFACTS); 
        if (biasArt && hasArtifacts) {
            return getCheapestPermanentAI(CardLists.filter(list, CardPredicates.Presets.ARTIFACTS), null, false);
        }

        if (biasLand && Iterables.any(list, CardPredicates.Presets.LANDS)) {
            return ComputerUtilCard.getWorstLand(CardLists.filter(list, CardPredicates.Presets.LANDS));
        }
    
        final boolean hasCreatures = Iterables.any(list, CardPredicates.Presets.CREATURES);
        if (biasCreature && hasCreatures) {
            return getWorstCreatureAI(CardLists.filter(list, CardPredicates.Presets.CREATURES));
        }
    
        List<Card> lands = CardLists.filter(list, CardPredicates.Presets.LANDS);
        if (lands.size() > 6) {
            return ComputerUtilCard.getWorstLand(lands);
        }
    
        if (hasEnchantmants || hasArtifacts) {
            final List<Card> ae = CardLists.filter(list, Predicates.<Card>or(CardPredicates.Presets.ARTIFACTS, CardPredicates.Presets.ENCHANTMENTS));
            return getCheapestPermanentAI(ae, null, false);
        }
    
        if (hasCreatures) {
            return getWorstCreatureAI(CardLists.filter(list, CardPredicates.Presets.CREATURES));
        }
    
        // Planeswalkers fall through to here, lands will fall through if there
        // aren't very many
        return getCheapestPermanentAI(list, null, false);
    }

    public static final Function<Card, Integer> fnEvaluateCreature = new Function<Card, Integer>() {
        @Override
        public Integer apply(Card a) {
            return ComputerUtilCard.evaluateCreature(a);
        }
    };
    public static final Comparator<Card> EvaluateCreatureComparator = new Comparator<Card>() {
        @Override
        public int compare(final Card a, final Card b) {
            return ComputerUtilCard.evaluateCreature(b) - ComputerUtilCard.evaluateCreature(a);
        }
    };
    /**
     * <p>
     * evaluateCreature.
     * </p>
     * 
     * @param c
     *            a {@link forge.game.card.Card} object.
     * @return a int.
     */
    public static int evaluateCreature(final Card c) {
    
        int value = 100;
        if (c.isToken()) {
            value = 80; // tokens should be worth less than actual cards
        }
        int power = c.getNetCombatDamage();
        final int toughness = c.getNetDefense();
        for (String keyword : c.getKeyword()) {
            if (keyword.equals("Prevent all combat damage that would be dealt by CARDNAME.")
                    || keyword.equals("Prevent all damage that would be dealt by CARDNAME.")
                    || keyword.equals("Prevent all combat damage that would be dealt to and dealt by CARDNAME.")
                    || keyword.equals("Prevent all damage that would be dealt to and dealt by CARDNAME.")) {
                power = 0;
                break;
            }
        }
        value += power * 15;
        value += toughness * 10;
        value += c.getCMC() * 5;
    
        // Evasion keywords
        if (c.hasKeyword("Flying")) {
            value += power * 10;
        }
        if (c.hasKeyword("Horsemanship")) {
            value += power * 10;
        }
        if (c.hasKeyword("Unblockable")) {
            value += power * 10;
        } else {
            if (c.hasKeyword("You may have CARDNAME assign its combat damage as though it weren't blocked.")) {
                value += power * 6;
            }
            if (c.hasKeyword("Fear")) {
                value += power * 6;
            }
            if (c.hasKeyword("Intimidate")) {
                value += power * 6;
            }
            if (c.hasStartOfKeyword("CantBeBlockedBy")) {
                value += power * 3;
            }
        }
    
        // Other good keywords
        if (power > 0) {
            if (c.hasKeyword("Double Strike")) {
                value += 10 + (power * 15);
            } else if (c.hasKeyword("First Strike")) {
                value += 10 + (power * 5);
            }
            if (c.hasKeyword("Deathtouch")) {
                value += 25;
            }
            if (c.hasKeyword("Lifelink")) {
                value += power * 10;
            }
            if (power > 1 && c.hasKeyword("Trample")) {
                value += (power - 1) * 5;
            }
            if (c.hasKeyword("Vigilance")) {
                value += (power * 5) + (toughness * 5);
            }
            if (c.hasKeyword("Wither")) {
                value += power * 10;
            }
            if (c.hasKeyword("Infect")) {
                value += power * 15;
            }
            value += c.getKeywordMagnitude("Rampage");
        }
    
        value += c.getKeywordMagnitude("Bushido") * 16;
        value += c.getAmountOfKeyword("Flanking") * 15;
        value += c.getAmountOfKeyword("Exalted") * 15;
        value += c.getKeywordMagnitude("Annihilator") * 50;
    
    
        // Defensive Keywords
        if (c.hasKeyword("Reach") && !c.hasKeyword("Flying")) {
            value += 5;
        }
        if (c.hasKeyword("CARDNAME can block creatures with shadow as though they didn't have shadow.")) {
            value += 3;
        }
    
        // Protection
        if (c.hasKeyword("Indestructible")) {
            value += 70;
        }
        if (c.hasKeyword("Prevent all damage that would be dealt to CARDNAME.")) {
            value += 60;
        } else if (c.hasKeyword("Prevent all combat damage that would be dealt to CARDNAME.")) {
            value += 50;
        }
        if (c.hasKeyword("Hexproof")) {
            value += 35;
        } else if (c.hasKeyword("Shroud")) {
            value += 30;
        }
        if (c.hasStartOfKeyword("Protection")) {
            value += 20;
        }
        if (c.hasStartOfKeyword("PreventAllDamageBy")) {
            value += 10;
        }
        value += c.getKeywordMagnitude("Absorb") * 11;
    
        // Bad keywords
        if (c.hasKeyword("Defender") || c.hasKeyword("CARDNAME can't attack.")) {
            value -= (power * 9) + 40;
        } else if (c.getSVar("SacrificeEndCombat").equals("True")) {
            value -= 40;
        }
        if (c.hasKeyword("CARDNAME can't block.")) {
            value -= 10;
        } else if (c.hasKeyword("CARDNAME attacks each turn if able.")
                || c.hasKeyword("CARDNAME attacks each combat if able.")) {
            value -= 10;
        } else if (c.hasStartOfKeyword("CARDNAME attacks specific player each combat if able")) {
            value -= 10;
        } else if (c.hasKeyword("CARDNAME can block only creatures with flying.")) {
            value -= toughness * 5;
        }
    
        if (c.hasSVar("DestroyWhenDamaged")) {
            value -= (toughness - 1) * 9;
        }
    
        if (c.hasKeyword("CARDNAME can't attack or block.")) {
            value = 50 + (c.getCMC() * 5); // reset everything - useless
        }
        if (c.hasKeyword("CARDNAME doesn't untap during your untap step.")) {
            if (c.isTapped()) {
                value = 50 + (c.getCMC() * 5); // reset everything - useless
            } else {
                value -= 50;
            }
        }
        if (c.hasSVar("EndOfTurnLeavePlay")) {
            value -= 50;
        } else if (c.hasStartOfKeyword("Cumulative upkeep")) {
            value -= 30;
        } else if (c.hasStartOfKeyword("At the beginning of your upkeep, sacrifice CARDNAME unless you pay")) {
            value -= 20;
        } else if (c.hasStartOfKeyword("(Echo unpaid)")) {
            value -= 10;
        }
    
        if (c.hasStartOfKeyword("At the beginning of your upkeep, CARDNAME deals")) {
            value -= 20;
        } 
        if (c.hasStartOfKeyword("Fading")) {
            value -= 20;
        }
        if (c.hasStartOfKeyword("Vanishing")) {
            value -= 20;
        }
        if (c.getSVar("Targeting").equals("Dies")) {
            value -= 25;
        }
    
        for (final SpellAbility sa : c.getSpellAbilities()) {
            if (sa.isAbility()) {
                value += 10;
            }
        }
        if (!c.getManaAbility().isEmpty()) {
            value += 10;
        }
    
        if (c.isUntapped()) {
            value += 1;
        }
    
        // paired creatures are more valuable because they grant a bonus to the other creature
        if (c.isPaired()) {
            value += 14;
        }
        
        if (!c.getEncoded().isEmpty()) {
            value += 24;
        }
    
        return value;
    
    } // evaluateCreature

    /**
     * <p>
     * evaluatePermanentList.
     * </p>
     * 
     * @param list
     *            a {@link forge.CardList} object.
     * @return a int.
     */
    public static int evaluatePermanentList(final List<Card> list) {
        int value = 0;
        for (int i = 0; i < list.size(); i++) {
            value += list.get(i).getCMC() + 1;
        }
    
        return value;
    }

    /**
     * <p>
     * evaluateCreatureList.
     * </p>
     * 
     * @param list
     *            a {@link forge.CardList} object.
     * @return a int.
     */
    public static int evaluateCreatureList(final List<Card> list) {
        return Aggregates.sum(list, fnEvaluateCreature);
    }

    /**
     * <p>
     * doesCreatureAttackAI.
     * </p>
     * 
     * @param ai
     *            the AI player
     * @param card
     *            a {@link forge.game.card.Card} object.
     * @return a boolean.
     */
    public static boolean doesCreatureAttackAI(final Player ai, final Card card) {
        AiAttackController aiAtk = new AiAttackController(ai);
        Combat combat = new Combat(ai);
        aiAtk.declareAttackers(combat);
        return combat.isAttacking(card);
    }
    
    /**
     * Extension of doesCreatureAttackAI() for "virtual" creatures that do not actually exist on the battlefield yet
     * such as unanimated manlands.
     * @param ai controller of creature 
     * @param card creature to be evaluated
     * @return creature will be attack
     */
    public static boolean doesSpecifiedCreatureAttackAI(final Player ai, final Card card) {
        AiAttackController aiAtk = new AiAttackController(ai, card);
        Combat combat = new Combat(ai);
        aiAtk.declareAttackers(combat);
        return combat.isAttacking(card);
    }
    
    /**
     * Create a mock combat and returns the list of likely blockers. 
     * @param ai blocking player
     * @param blockers list of additional blockers to be considered
     * @return list of creatures assigned to block in the simulation
     */
    public static List<Card> getLikelyBlockers(final Player ai, final List<Card> blockers) {
        AiBlockController aiBlk = new AiBlockController(ai);
        final Player opp = ai.getOpponent();
        Combat combat = new Combat(opp);
        //Use actual attackers if available, else consider all possible attackers
        if (ai.getGame().getCombat() == null) {
            for (Card c : opp.getCreaturesInPlay()) {
                if (CombatUtil.canAttackNextTurn(c, ai)) {
                    combat.addAttacker(c, ai);
                }
            }
        } else {
            for (Card c : ai.getGame().getCombat().getAttackers()) {
                combat.addAttacker(c, ai);
            }
        }
        if (blockers == null || blockers.isEmpty()) {
            aiBlk.assignBlockersForCombat(combat);
        } else {
            aiBlk.assignAdditionalBlockers(combat, blockers);
        }
        return combat.getAllBlockers();
    }
    
    /**
     * Decide if a creature is going to be used as a blocker.
     * @param ai controller of creature 
     * @param blocker creature to be evaluated
     * @return creature will be a blocker
     */
    public static boolean doesSpecifiedCreatureBlock(final Player ai, Card blocker) {
        List<Card> blockers = new ArrayList<Card>();
        blockers.add(blocker);
        return getLikelyBlockers(ai, blockers).contains(blocker);
    }

    /**
     * Check if an attacker can be blocked profitably (ie. kill attacker)
     * @param ai controller of attacking creature
     * @param attacker attacking creature to evaluate
     * @return attacker will die
     */
    public static boolean canBeBlockedProfitably(final Player ai, Card attacker) {
        AiBlockController aiBlk = new AiBlockController(ai);
        Combat combat = new Combat(ai);
        combat.addAttacker(attacker, ai);
        final List<Card> attackers = new ArrayList<Card>();
        attackers.add(attacker);
        aiBlk.assignBlockersGivenAttackers(combat, attackers);
        return ComputerUtilCombat.attackerWouldBeDestroyed(ai, attacker, combat);
    }
    
    /**
     * getMostExpensivePermanentAI.
     * 
     * @param all
     *            the all
     * @return the card
     */
    public static Card getMostExpensivePermanentAI(final Collection<Card> all) {
        Card biggest = null;
    
        int bigCMC = -1;
        for (final Card card : all) {
            int curCMC = card.getCMC();
    
            // Add all cost of all auras with the same controller
            if (card.isEnchanted()) {
                final List<Card> auras = CardLists.filterControlledBy(card.getEnchantedBy(false), card.getController());
                curCMC += Aggregates.sum(auras, CardPredicates.Accessors.fnGetCmc) + auras.size();
            }
    
            if (curCMC >= bigCMC) {
                bigCMC = curCMC;
                biggest = card;
            }
        }
    
        return biggest;
    }

    /**
     * <p>
     * getMostProminentCardName.
     * </p>
     * 
     * @param list
     *            a {@link forge.CardList} object.
     * @return a {@link java.lang.String} object.
     */
    public static String getMostProminentCardName(final List<Card> list) {
    
        if (list.size() == 0) {
            return "";
        }
    
        final Map<String, Integer> map = new HashMap<String, Integer>();
    
        for (final Card c : list) {
            final String name = c.getName();
            Integer currentCnt = map.get(name);
            map.put(name, currentCnt == null ? Integer.valueOf(1) : Integer.valueOf(1 + currentCnt));
        } // for
    
        int max = 0;
        String maxName = "";
    
        for (final Entry<String, Integer> entry : map.entrySet()) {
            final String type = entry.getKey();
            // Log.debug(type + " - " + entry.getValue());
    
            if (max < entry.getValue()) {
                max = entry.getValue();
                maxName = type;
            }
        }
        return maxName;
    }

    /**
     * <p>
     * getMostProminentCreatureType.
     * </p>
     * 
     * @param list
     *            a {@link forge.CardList} object.
     * @return a {@link java.lang.String} object.
     */
    public static String getMostProminentCreatureType(final List<Card> list) {
    
        if (list.size() == 0) {
            return "";
        }
    
        final Map<String, Integer> map = new HashMap<String, Integer>();
    
        for (final Card c : list) {
            for (final String var : c.getType()) {
                if (CardType.isACreatureType(var)) {
                    if (!map.containsKey(var)) {
                        map.put(var, 1);
                    } else {
                        map.put(var, map.get(var) + 1);
                    }
                }
            }
        } // for
    
        int max = 0;
        String maxType = "";
    
        for (final Entry<String, Integer> entry : map.entrySet()) {
            final String type = entry.getKey();
            // Log.debug(type + " - " + entry.getValue());
    
            if (max < entry.getValue()) {
                max = entry.getValue();
                maxType = type;
            }
        }
    
        return maxType;
    }

    /**
     * <p>
     * getMostProminentColor.
     * </p>
     * 
     * @param list
     *            a {@link forge.CardList} object.
     * @return a {@link java.lang.String} object.
     */
    public static String getMostProminentColor(final List<Card> list) {
        byte colors = CardFactoryUtil.getMostProminentColors(list);
        for(byte c : MagicColor.WUBRG) {
            if ( (colors & c) != 0 )
                return MagicColor.toLongString(c);
        }
        return MagicColor.Constant.WHITE; // no difference, there was no prominent color
    }

    public static String getMostProminentColor(final List<Card> list, final List<String> restrictedToColors) {
        byte colors = CardFactoryUtil.getMostProminentColorsFromList(list, restrictedToColors);
        for (byte c : MagicColor.WUBRG) {
            if ((colors & c) != 0) {
                return MagicColor.toLongString(c);
            }
        }
        return restrictedToColors.get(0); // no difference, there was no prominent color
    }

    public static List<String> getColorByProminence(final List<Card> list) {
        int cntColors = MagicColor.WUBRG.length;
        final List<Pair<Byte,Integer>> map = new ArrayList<Pair<Byte,Integer>>();
        for(int i = 0; i < cntColors; i++) {
            map.add(MutablePair.of(MagicColor.WUBRG[i], 0));
        }

        for (final Card crd : list) {
            ColorSet color = CardUtil.getColors(crd);
            if (color.hasWhite()) map.get(0).setValue(Integer.valueOf(map.get(0).getValue()+1));
            if (color.hasBlue()) map.get(1).setValue(Integer.valueOf(map.get(1).getValue()+1));
            if (color.hasBlack()) map.get(2).setValue(Integer.valueOf(map.get(2).getValue()+1));
            if (color.hasRed()) map.get(3).setValue(Integer.valueOf(map.get(3).getValue()+1));
            if (color.hasGreen()) map.get(4).setValue(Integer.valueOf(map.get(4).getValue()+1));
        } // for

        Collections.sort(map, new Comparator<Pair<Byte,Integer>>() {
            @Override public int compare(Pair<Byte, Integer> o1, Pair<Byte, Integer> o2) {
                return o2.getValue() - o1.getValue();
            }
        });
    
        // will this part be once dropped?
        List<String> result = new ArrayList<String>(cntColors);
        for(Pair<Byte, Integer> idx : map) { // fetch color names in the same order
            result.add(MagicColor.toLongString(idx.getKey()));
        }
        // reverse to get indices for most prominent colors first.
        return result;
    }


    /**
     * <p>
     * getWorstLand.
     * </p>
     * 
     * @param lands
     *            a {@link forge.CardList} object.
     * @return a {@link forge.game.card.Card} object.
     */
    public static Card getWorstLand(final List<Card> lands) {
        Card worstLand = null;
        int maxScore = 0;
        // first, check for tapped, basic lands
        for (Card tmp : lands) {
            int score = tmp.isTapped() ? 2 : 0;
            score += tmp.isBasicLand() ? 1 : 0;
            score += tmp.isCreature() ? 4 : 0;
            if (score >= maxScore) {
                worstLand = tmp;
                maxScore = score;
            }
        }
        return worstLand;
    } // end getWorstLand

    public static final Predicate<Deck> AI_KNOWS_HOW_TO_PLAY_ALL_CARDS = new Predicate<Deck>() {
        @Override
        public boolean apply(Deck d) {
            for(Entry<DeckSection, CardPool> cp: d) {
                for(Entry<PaperCard, Integer> e : cp.getValue()) {
                    if ( e.getKey().getRules().getAiHints().getRemAIDecks() )
                        return false;
                }
            }
            return true;
        }
    };
    public static List<String> chooseColor(SpellAbility sa, int min, int max, List<String> colorChoices) {
        List<String> chosen = new ArrayList<String>();
        Player ai = sa.getActivatingPlayer();
        final Game game = ai.getGame();
        Player opp = ai.getOpponent();
        if (sa.hasParam("AILogic")) {
            final String logic = sa.getParam("AILogic");
            if (logic.equals("MostProminentInHumanDeck")) {
                chosen.add(ComputerUtilCard.getMostProminentColor(CardLists.filterControlledBy(game.getCardsInGame(), opp), colorChoices));
            } else if (logic.equals("MostProminentInComputerDeck")) {
                chosen.add(ComputerUtilCard.getMostProminentColor(CardLists.filterControlledBy(game.getCardsInGame(), ai), colorChoices));
            } else if (logic.equals("MostProminentDualInComputerDeck")) {
                List<String> prominence = ComputerUtilCard.getColorByProminence(CardLists.filterControlledBy(game.getCardsInGame(), ai));
                chosen.add(prominence.get(0));
                chosen.add(prominence.get(1));
            }
            else if (logic.equals("MostProminentInGame")) {
                chosen.add(ComputerUtilCard.getMostProminentColor(game.getCardsInGame(), colorChoices));
            }
            else if (logic.equals("MostProminentHumanCreatures")) {
                List<Card> list = opp.getCreaturesInPlay();
                if (list.isEmpty()) {
                    list = CardLists.filter(CardLists.filterControlledBy(game.getCardsInGame(), opp), CardPredicates.Presets.CREATURES);
                }
                chosen.add(ComputerUtilCard.getMostProminentColor(list, colorChoices));
            }
            else if (logic.equals("MostProminentComputerControls")) {
                chosen.add(ComputerUtilCard.getMostProminentColor(ai.getCardsIn(ZoneType.Battlefield), colorChoices));
            }
            else if (logic.equals("MostProminentHumanControls")) {
                chosen.add(ComputerUtilCard.getMostProminentColor(ai.getOpponent().getCardsIn(ZoneType.Battlefield), colorChoices));
            }
            else if (logic.equals("MostProminentPermanent")) {
                final List<Card> list = game.getCardsIn(ZoneType.Battlefield);
                chosen.add(ComputerUtilCard.getMostProminentColor(list, colorChoices));
            }
            else if (logic.equals("MostProminentAttackers") && game.getPhaseHandler().inCombat()) {
                chosen.add(ComputerUtilCard.getMostProminentColor(game.getCombat().getAttackers(), colorChoices));
            }
            else if (logic.equals("MostProminentInActivePlayerHand")) {
                chosen.add(ComputerUtilCard.getMostProminentColor(game.getPhaseHandler().getPlayerTurn().getCardsIn(ZoneType.Hand), colorChoices));
            }
            else if (logic.equals("MostProminentKeywordInComputerDeck")) {
                List<Card> list = ai.getAllCards();
                int m1 = 0;
                String chosenColor = MagicColor.Constant.WHITE;

                for (final String c : MagicColor.Constant.ONLY_COLORS) {
                    final int cmp = CardLists.filter(list, CardPredicates.containsKeyword(c)).size();
                    if (cmp > m1) {
                        m1 = cmp;
                        chosenColor = c;
                    }
                }
                chosen.add(chosenColor);
            }
        }
        if (chosen.size() == 0) {
            chosen.add(MagicColor.Constant.GREEN);
        }
        return chosen;
    }
    
    public static boolean useRemovalNow(final SpellAbility sa, final Card c, final int dmg, ZoneType destination) {
        final Player ai = sa.getActivatingPlayer();
        final Player opp = ai.getOpponent();
        final Game game = ai.getGame();
        final PhaseHandler ph = game.getPhaseHandler();

        final int costRemoval = sa.getHostCard().getCMC();
        final int costTarget = c.getCMC();
        
        //interrupt 1:remove blocker to save my attacker
        if (ph.is(PhaseType.COMBAT_DECLARE_BLOCKERS)) {
            Combat currCombat = game.getCombat();
            if (currCombat != null && !currCombat.getAllBlockers().isEmpty() && currCombat.getAllBlockers().contains(c)) {
                for (Card attacker : currCombat.getAttackersBlockedBy(c)) {
                    if (attacker.getShieldCount() == 0 && ComputerUtilCombat.attackerWouldBeDestroyed(ai, attacker, currCombat)) {
                        List<Card> blockers = currCombat.getBlockers(attacker);
                        ComputerUtilCard.sortByEvaluateCreature(blockers);
                        Combat combat = new Combat(ai);
                        combat.addAttacker(attacker, opp);
                        for (Card blocker : blockers) {
                            if (blocker == c) {
                                continue;
                            }
                            combat.addBlocker(attacker, blocker);
                        }
                        if (!ComputerUtilCombat.attackerWouldBeDestroyed(ai, attacker, combat)) {
                            return true;
                        }
                    }
                }
            }
        }
        
        //burn and curse spells
        float valueBurn = 0;
        if (dmg > 0) {
            if (sa.getDescription().contains("would die, exile it instead")) {
                destination = ZoneType.Exile;
            }
            valueBurn = 1.0f * c.getNetDefense() / dmg;
            valueBurn *= valueBurn;
            if (sa.getTargetRestrictions().canTgtPlayer()) {
                valueBurn /= 2;     //preserve option to burn to the face
            }
        }
        
        //evaluate tempo gain
        float valueTempo = Math.max(0.1f * costTarget / costRemoval, valueBurn);
        if (c.isEquipped()) {
            valueTempo *= 2;
        }
        if (SpellAbilityAi.isSorcerySpeed(sa)) {
            valueTempo *= 2;    //sorceries have less usage opportunities
        }
        if (!c.canBeDestroyed()) {
            valueTempo *= 2;    //deal with annoying things
        }
        if (!destination.equals(ZoneType.Graveyard) &&  //TODO:boat-load of "when blah dies" triggers
                c.hasKeyword("Persist") || c.hasKeyword("Undying") || c.hasKeyword("Modular")) {
            valueTempo *= 2;
        }
        if (destination.equals(ZoneType.Hand) && !c.isToken()) {
            valueTempo /= 2;    //bouncing non-tokens for tempo is less valuable
        }
        if (c.isLand()) {
            valueTempo += 0.5f / opp.getLandsInPlay().size();   //set back opponent's mana
        }
        if (c.isEnchanted()) {
            boolean myEnchants = false;
            for (Card enc : c.getEnchantedBy(false)) {
                if (enc.getOwner().equals(ai)) {
                    myEnchants = true;
                    break;
                }
            }
            if (!myEnchants) {
                valueTempo += 1;    //card advantage > tempo
            }
        }
        if (!ph.isPlayerTurn(ai) && ph.getPhase().equals(PhaseType.END_OF_TURN)) {
            valueTempo *= 2;    //prefer to cast at opponent EOT
        }
        
        //interrupt 2:opponent pumping target (only works if the pump target is the chosen best target to begin with)
        final MagicStack stack = ai.getGame().getStack();
        if (!stack.isEmpty()) {
            final SpellAbility topStack = stack.peekAbility();
            if (topStack.getActivatingPlayer().equals(opp) && c.equals(topStack.getTargetCard()) && topStack.isSpell()) {
                valueTempo += 1;
            }
        }
        
        //evaluate threat of targeted card
        float threat = 0;
        if (c.isCreature()) {
            Combat combat = ai.getGame().getCombat();
            threat = 1.0f * ComputerUtilCombat.damageIfUnblocked(c, opp, combat, true) / ai.getLife();
            //TODO:add threat from triggers and other abilities (ie. Master of Cruelties)
        } else {
            for (final StaticAbility stAb : c.getStaticAbilities()) {
                final Map<String, String> params = stAb.getMapParams();
                //continuous buffs
                if (params.get("Mode").equals("Continuous") && "Creature.YouCtrl".equals(params.get("Affected"))) {
                    int bonusPT = 0;
                    if (params.containsKey("AddPower")) {
                        bonusPT += AbilityUtils.calculateAmount(c, params.get("AddPower"), stAb);
                    }
                    if (params.containsKey("AddToughness")) {
                        bonusPT += AbilityUtils.calculateAmount(c, params.get("AddPower"), stAb);
                    }
                    String kws = params.get("AddKeyword");
                    if (kws != null) {
                        bonusPT += 4 * (1 + StringUtils.countMatches(kws, "&"));    //treat each added keyword as a +2/+2 for now
                    }
                    if (bonusPT > 0) {
                        threat = bonusPT * (1 + opp.getCreaturesInPlay().size()) / 10.0f;
                    }
                }
            }
            //TODO:add threat from triggers and other abilities (ie. Bident of Thassa)
        }
        if (!c.getManaAbility().isEmpty()) {
            threat += 0.5f * costTarget / opp.getLandsInPlay().size();   //set back opponent's mana
        }
        
        final float valueNow = Math.max(valueTempo, threat);
        if (valueNow < 0.2) {   //hard floor to reduce ridiculous odds for instants over time
            return false;
        } else {
            final float chance = MyRandom.getRandom().nextFloat();
            return chance < valueNow;
        }
    }
    
    /**
     * Applies static continuous Power/Toughness effects to a (virtual) creature.
     * @param game game instance to work with 
     * @param vCard creature to work with
     * @param exclude list of cards to exclude when considering ability sources, accepts null
     */
    public static void applyStaticContPT(final Game game, Card vCard, final List<Card> exclude) {
        if (!vCard.isCreature()) {
            return;
        }
        final List<Card> list = game.getCardsIn(ZoneType.Battlefield);
        list.addAll(game.getCardsIn(ZoneType.Command));
        if (exclude != null) {
            list.removeAll(exclude);
        }
        for (final Card c : list) {
            for (final StaticAbility stAb : c.getStaticAbilities()) {
                final Map<String, String> params = stAb.getMapParams();
                if (!params.get("Mode").equals("Continuous")) {
                    continue;
                }
                if (!params.containsKey("Affected")) {
                    continue;
                }
                final String valid = params.get("Affected");
                if (!vCard.isValid(valid, c.getController(), c)) {
                    continue;
                }
                if (params.containsKey("AddPower")) {
                    String addP = params.get("AddPower");
                    int att = 0;
                    if (addP.equals("AffectedX")) {
                        att = CardFactoryUtil.xCount(vCard, AbilityUtils.getSVar(stAb, addP));
                    } else {
                        att = AbilityUtils.calculateAmount(c, addP, stAb);
                    }
                    vCard.addTempAttackBoost(att);
                }
                if (params.containsKey("AddToughness")) {
                    String addT = params.get("AddToughness");
                    int def = 0;
                    if (addT.equals("AffectedY")) {
                        def = CardFactoryUtil.xCount(vCard, AbilityUtils.getSVar(stAb, addT));
                    } else {
                        def = AbilityUtils.calculateAmount(c, addT, stAb);
                    }
                    vCard.addTempDefenseBoost(def);
                }
            }
        }
    }
    
}
