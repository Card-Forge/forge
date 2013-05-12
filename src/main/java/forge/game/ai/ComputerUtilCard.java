package forge.game.ai;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;

import forge.Card;
import forge.CardLists;
import forge.CardPredicates;
import forge.CardUtil;
import forge.Constant;
import forge.card.CardType;
import forge.card.ColorSet;
import forge.card.MagicColor;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.spellability.SpellAbility;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.item.CardPrinted;
import forge.util.Aggregates;
import forge.util.MyRandom;

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
     *            a {@link forge.Card} object.
     * @param targeted
     *            a boolean.
     * @return a {@link forge.Card} object.
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

    // The AI doesn't really pick the best artifact, just the most expensive.
    /**
     * <p>
     * getBestArtifactAI.
     * </p>
     * 
     * @param list
     *            a {@link forge.CardList} object.
     * @return a {@link forge.Card} object.
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
     *            a {@link forge.Card} object.
     * @param targeted
     *            a boolean.
     * @return a {@link forge.Card} object.
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
     * @return a {@link forge.Card} object.
     */
    public static Card getBestLandAI(final List<Card> list) {
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
        for (String name : Constant.Color.BASIC_LANDS) {
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
     *            a {@link forge.Card} object.
     * @param targeted
     *            a boolean.
     * @return a {@link forge.Card} object.
     */
    public static Card getCheapestPermanentAI(final List<Card> list, final SpellAbility spell, final boolean targeted) {
        List<Card> all = list;
        if (targeted) {
            all = CardLists.filter(all, new Predicate<Card>() {
                @Override
                public boolean apply(final Card c) {
                    return c.canBeTargetedBy(spell);
                }
            });
        }
        if (all.size() == 0) {
            return null;
        }
    
        // get cheapest card:
        Card cheapest = null;
        cheapest = all.get(0);
    
        for (int i = 0; i < all.size(); i++) {
            if (cheapest.getManaCost().getCMC() <= cheapest.getManaCost().getCMC()) {
                cheapest = all.get(i);
            }
        }
    
        return cheapest;
    
    }

    // for Sarkhan the Mad
    /**
     * <p>
     * getCheapestCreatureAI.
     * </p>
     * 
     * @param list
     *            a {@link forge.CardList} object.
     * @param spell
     *            a {@link forge.Card} object.
     * @param targeted
     *            a boolean.
     * @return a {@link forge.Card} object.
     */
    public static Card getCheapestCreatureAI(List<Card> list, final SpellAbility spell, final boolean targeted) {
        list = CardLists.filter(list, new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                return c.isCreature();
            }
        });
        return getCheapestPermanentAI(list, spell, targeted);
    }

    // returns null if list.size() == 0
    /**
     * <p>
     * getBestAI.
     * </p>
     * 
     * @param list
     *            a {@link forge.CardList} object.
     * @return a {@link forge.Card} object.
     */
    public static Card getBestAI(final List<Card> list) {
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
    public static Card getBestCreatureAI(final List<Card> list) {
        List<Card> all = CardLists.filter(list, CardPredicates.Presets.CREATURES);
        return Aggregates.itemWithMax(all, ComputerUtilCard.fnEvaluateCreature);
    }

    // This selection rates tokens higher
    /**
     * <p>
     * getBestCreatureToBounceAI.
     * </p>
     * 
     * @param list
     *            a {@link forge.CardList} object.
     * @return a {@link forge.Card} object.
     */
    public static Card getBestCreatureToBounceAI(final List<Card> list) {
        final int tokenBonus = 40;
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
     * @return a {@link forge.Card} object.
     */
    public static Card getWorstAI(final List<Card> list) {
        return ComputerUtilCard.getWorstPermanentAI(list, false, false, false, false);
    }

    // returns null if list.size() == 0
    /**
     * <p>
     * getWorstCreatureAI.
     * </p>
     * 
     * @param list
     *            a {@link forge.CardList} object.
     * @return a {@link forge.Card} object.
     */
    public static Card getWorstCreatureAI(final List<Card> list) {
        List<Card> all = CardLists.filter(list, CardPredicates.Presets.CREATURES);
        // get smallest creature
        return Aggregates.itemWithMin(all, ComputerUtilCard.fnEvaluateCreature);
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
     * @return a {@link forge.Card} object.
     */
    public static Card getWorstPermanentAI(final List<Card> list, final boolean biasEnch, final boolean biasLand,
            final boolean biasArt, final boolean biasCreature) {
        if (list.size() == 0) {
            return null;
        }
    
        if (biasEnch && Iterables.any(list, CardPredicates.Presets.ENCHANTMENTS)) {
            return getCheapestPermanentAI(CardLists.filter(list, CardPredicates.Presets.ENCHANTMENTS), null, false);
        }
    
        if (biasArt && Iterables.any(list, CardPredicates.Presets.ARTIFACTS)) {
            return getCheapestPermanentAI(CardLists.filter(list, CardPredicates.Presets.ARTIFACTS), null, false);
        }
    
        if (biasLand && Iterables.any(list, CardPredicates.Presets.LANDS)) {
            return ComputerUtilCard.getWorstLand(CardLists.filter(list, CardPredicates.Presets.LANDS));
        }
    
        if (biasCreature && Iterables.any(list, CardPredicates.Presets.CREATURES)) {
            return getWorstCreatureAI(CardLists.filter(list, CardPredicates.Presets.CREATURES));
        }
    
        List<Card> lands = CardLists.filter(list, CardPredicates.Presets.LANDS);
        if (lands.size() > 6) {
            return ComputerUtilCard.getWorstLand(lands);
        }
    
        if ((CardLists.getType(list, "Artifact").size() > 0) || (CardLists.getType(list, "Enchantment").size() > 0)) {
            return getCheapestPermanentAI(CardLists.filter(list, new Predicate<Card>() {
                @Override
                public boolean apply(final Card c) {
                    return c.isArtifact() || c.isEnchantment();
                }
            }), null, false);
        }
    
        if (CardLists.getType(list, "Creature").size() > 0) {
            return getWorstCreatureAI(CardLists.getType(list, "Creature"));
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
     *            a {@link forge.Card} object.
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
            if (c.hasStartOfKeyword("CARDNAME can't be blocked except by")) {
                value += power * 5;
            }
            if (c.hasStartOfKeyword("CARDNAME can't be blocked by")) {
                value += power * 2;
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
            if (c.hasKeyword("Whenever a creature dealt damage by CARDNAME this turn is "
                    + "put into a graveyard, put a +1/+1 counter on CARDNAME.")) {
                value += 2;
            }
            if (c.hasKeyword("Whenever a creature dealt damage by CARDNAME this turn is "
                    + "put into a graveyard, put a +2/+2 counter on CARDNAME.")) {
                value += 3;
            }
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
        } else if (c.hasKeyword("CARDNAME attacks each turn if able.")) {
            value -= 10;
        } else if (c.hasKeyword("CARDNAME can block only creatures with flying.")) {
            value -= toughness * 5;
        }
    
        if (c.hasStartOfKeyword("When CARDNAME is dealt damage, destroy it.")) {
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
        if (c.hasKeyword("At the beginning of the end step, destroy CARDNAME.")
               || c.hasKeyword("At the beginning of the end step, exile CARDNAME.")
               || c.hasKeyword("At the beginning of the end step, sacrifice CARDNAME.")) {
            value -= 50;
        } else if (c.hasStartOfKeyword("Cumulative upkeep")) {
            value -= 30;
        } else if (c.hasStartOfKeyword("At the beginning of your upkeep, destroy CARDNAME unless you pay")
                || c.hasStartOfKeyword("At the beginning of your upkeep, sacrifice CARDNAME unless you pay")
                || c.hasStartOfKeyword("Upkeep:")) {
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
     *            a {@link forge.Card} object.
     * @return a boolean.
     */
    public static boolean doesCreatureAttackAI(final Player ai, final Card card) {
        final List<Card> att = new AiAttackController(ai, ai.getOpponent()).getAttackers().getAttackers();
    
        return att.contains(card);
    }

    /**
     * getMostExpensivePermanentAI.
     * 
     * @param all
     *            the all
     * @return the card
     */
    public static Card getMostExpensivePermanentAI(final List<Card> all) {
        Card biggest = null;
    
        int bigCMC = -1;
        for (final Card card : all) {
            int curCMC = card.getCMC();
    
            // Add all cost of all auras with the same controller
            final List<Card> auras = CardLists.filterControlledBy(card.getEnchantedBy(), card.getController());
            curCMC += Aggregates.sum(auras, CardPredicates.Accessors.fnGetCmc) + auras.size();
    
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
            final ArrayList<String> typeList = c.getType();
    
            for (final String var : typeList) {
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
        return Constant.Color.WHITE; // no difference, there was no prominent color
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
     * getUsableManaSources.
     * </p>
     * 
     * @param player
     *            a {@link forge.game.player.Player} object.
     * @return a int.
     */
    public static int getUsableManaSources(final Player player) {
        List<Card> list = CardLists.filter(player.getCardsIn(ZoneType.Battlefield), new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                for (final SpellAbility am : ComputerUtilMana.getAIPlayableMana(c)) {
                    am.setActivatingPlayer(player);
                    if (am.canPlay()) {
                        return true;
                    }
                }
                return false;
            }
        });
    
        return list.size();
    }

    /**
     * <p>
     * getWorstLand.
     * </p>
     * 
     * @param lands
     *            a {@link forge.CardList} object.
     * @return a {@link forge.Card} object.
     */
    public static Card getWorstLand(final List<Card> lands) {
        Card worstLand = null;
        int maxScore = 0;
        // first, check for tapped, basic lands
        for (Card tmp : lands) {
            int score = tmp.isTapped() ? 2 : 0;
            score += tmp.isBasicLand() ? 1 : 0;
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
                for(Entry<CardPrinted, Integer> e : cp.getValue()) {
                    if ( e.getKey().getRules().getAiHints().getRemAIDecks() )
                        return false;
                }
            }
            return true;
        }
    };

}
