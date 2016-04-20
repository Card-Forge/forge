package forge.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;

import forge.card.CardType;
import forge.card.ColorSet;
import forge.card.MagicColor;
import forge.card.MagicColor.Constant;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.game.Game;
import forge.game.GameObject;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardFactory;
import forge.game.card.CardFactoryUtil;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.card.CardUtil;
import forge.game.card.CounterType;
import forge.game.combat.Combat;
import forge.game.combat.CombatUtil;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.game.staticability.StaticAbility;
import forge.game.zone.MagicStack;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;
import forge.util.Aggregates;
import forge.util.MyRandom;


public class ComputerUtilCard {
    public static Card getMostExpensivePermanentAI(final CardCollectionView list, final SpellAbility spell, final boolean targeted) {
        CardCollectionView all = list;
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
     */
    public static void sortByEvaluateCreature(final CardCollection list) {
        Collections.sort(list, ComputerUtilCard.EvaluateCreatureComparator);
    } // sortByEvaluateCreature()
    
    // The AI doesn't really pick the best artifact, just the most expensive.
    /**
     * <p>
     * getBestArtifactAI.
     * </p>
     * 
     * @param list
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
    
    /**
     * Returns the best Planeswalker from a given list
     * @param list list of cards to evaluate
     * @return best Planeswalker
     */
    public static Card getBestPlaneswalkerAI(final List<Card> list) {
        List<Card> all = CardLists.filter(list, CardPredicates.Presets.PLANEWALKERS);
        if (all.size() == 0) {
            return null;
        }
        // no AI logic, just return most expensive
        return Aggregates.itemWithMax(all, CardPredicates.Accessors.fnGetCmc);
    }

    // The AI doesn't really pick the best enchantment, just the most expensive.
    /**
     * <p>
     * getBestEnchantmentAI.
     * </p>
     * 
     * @param list
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
     * @return a {@link forge.game.card.Card} object.
     */
    public static Card getBestLandAI(final Iterable<Card> list) {
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
     * @param all
     * @param spell
     *            a {@link forge.game.card.Card} object.
     * @param targeted
     *            a boolean.
     * @return a {@link forge.game.card.Card} object.
     */
    public static Card getCheapestPermanentAI(Iterable<Card> all, final SpellAbility spell, final boolean targeted) {
        if (targeted) {
            all = CardLists.filter(all, new Predicate<Card>() {
                @Override
                public boolean apply(final Card c) {
                    return c.canBeTargetedBy(spell);
                }
            });
        }
        if (Iterables.isEmpty(all)) {
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
     * @return a {@link forge.game.card.Card} object.
     */
    public static Card getBestAI(final Iterable<Card> list) {
        // Get Best will filter by appropriate getBest list if ALL of the list
        // is of that type
        if (Iterables.all(list, CardPredicates.Presets.CREATURES)) {
            return ComputerUtilCard.getBestCreatureAI(list);
        }
        if (Iterables.all(list, CardPredicates.Presets.LANDS)) {
            return getBestLandAI(list);
        }
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
    public static Card getBestCreatureAI(final Iterable<Card> list) {
        return Aggregates.itemWithMax(Iterables.filter(list, CardPredicates.Presets.CREATURES), ComputerUtilCard.creatureEvaluator);
    }

    /**
     * <p>
     * getWorstCreatureAI.
     * </p>
     * 
     * @param list
     * @return a {@link forge.game.card.Card} object.
     */
    public static Card getWorstCreatureAI(final Iterable<Card> list) {
        return Aggregates.itemWithMin(Iterables.filter(list, CardPredicates.Presets.CREATURES), ComputerUtilCard.creatureEvaluator);
    }

    // This selection rates tokens higher
    /**
     * <p>
     * getBestCreatureToBounceAI.
     * </p>
     * 
     * @param list
     * @return a {@link forge.game.card.Card} object.
     */
    public static Card getBestCreatureToBounceAI(final CardCollectionView list) {
        final int tokenBonus = 60;
        Card biggest = null;
        int biggestvalue = -1;

        for (Card card : CardLists.filter(list, CardPredicates.Presets.CREATURES)) {
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
     * @return a {@link forge.game.card.Card} object.
     */
    public static Card getWorstAI(final Iterable<Card> list) {
        return ComputerUtilCard.getWorstPermanentAI(list, false, false, false, false);
    }

    /**
     * <p>
     * getWorstPermanentAI.
     * </p>
     * 
     * @param list
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
    public static Card getWorstPermanentAI(final Iterable<Card> list, final boolean biasEnch, final boolean biasLand,
            final boolean biasArt, final boolean biasCreature) {
        if (Iterables.isEmpty(list)) {
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

    public static final Comparator<Card> EvaluateCreatureComparator = new Comparator<Card>() {
        @Override
        public int compare(final Card a, final Card b) {
            return ComputerUtilCard.evaluateCreature(b) - ComputerUtilCard.evaluateCreature(a);
        }
    };

    private static final CreatureEvaluator creatureEvaluator = new CreatureEvaluator();

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
        return creatureEvaluator.evaluateCreature(c);
    }

    public static int evaluatePermanentList(final CardCollectionView list) {
        int value = 0;
        for (int i = 0; i < list.size(); i++) {
            value += list.get(i).getCMC() + 1;
        }
        return value;
    }

    public static int evaluateCreatureList(final CardCollectionView list) {
        return Aggregates.sum(list, creatureEvaluator);
    }

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

    public static boolean canBeKilledByRoyalAssassin(final Player ai, final Card card) {
    	boolean wasTapped = card.isTapped();
    	for (Player opp : ai.getOpponents()) {
    		for (Card c : opp.getCardsIn(ZoneType.Battlefield)) {
    			for (SpellAbility sa : c.getSpellAbilities()) {
                    if (sa.getApi() != ApiType.Destroy) {
                        continue;
                    }
                    if (!ComputerUtilCost.canPayCost(sa, opp)) {
                        continue;
                    }
                    sa.setActivatingPlayer(opp);
                    if (sa.canTarget(card)) {
                    	continue;
                    }
                    // check whether the ability can only target tapped creatures
                	card.setTapped(true);
                    if (!sa.canTarget(card)) {
                    	card.setTapped(wasTapped);
                    	continue;
                    }
                	card.setTapped(wasTapped);
                    return true;
    			}
    		}
    	}
    	return false;
    }
    
    /**
     * Create a mock combat where ai is being attacked and returns the list of likely blockers. 
     * @param ai blocking player
     * @param blockers list of additional blockers to be considered
     * @return list of creatures assigned to block in the simulation
     */
    public static CardCollectionView getLikelyBlockers(final Player ai, final CardCollectionView blockers) {
        AiBlockController aiBlk = new AiBlockController(ai);
        final Player opp = ai.getOpponent();
        Combat combat = new Combat(opp);
        //Use actual attackers if available, else consider all possible attackers
        Combat currentCombat = ai.getGame().getCombat();
        if (currentCombat != null && currentCombat.getAttackingPlayer() != ai) {
            for (Card c : currentCombat.getAttackers()) {
                combat.addAttacker(c, ai);
            }
        } else {
            for (Card c : opp.getCreaturesInPlay()) {
                if (ComputerUtilCombat.canAttackNextTurn(c, ai)) {
                    combat.addAttacker(c, ai);
                }
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
        return getLikelyBlockers(ai, new CardCollection(blocker)).contains(blocker);
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
    public static Card getMostExpensivePermanentAI(final Iterable<Card> all) {
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

    public static String getMostProminentCardName(final CardCollectionView list) {
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
     * @return a {@link java.lang.String} object.
     */
    public static String getMostProminentCreatureType(final CardCollectionView list) {
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
     * @return a {@link java.lang.String} object.
     */
    public static String getMostProminentColor(final Iterable<Card> list) {
        byte colors = CardFactoryUtil.getMostProminentColors(list);
        for(byte c : MagicColor.WUBRG) {
            if ( (colors & c) != 0 )
                return MagicColor.toLongString(c);
        }
        return MagicColor.Constant.WHITE; // no difference, there was no prominent color
    }

    public static String getMostProminentColor(final CardCollectionView list, final List<String> restrictedToColors) {
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
     * @return a {@link forge.game.card.Card} object.
     */
    public static Card getWorstLand(final List<Card> lands) {
        Card worstLand = null;
        int maxScore = 0;
        // first, check for tapped, basic lands
        for (Card tmp : lands) {
            int score = tmp.isTapped() ? 2 : 0;
            score += tmp.isBasicLand() ? 1 : 0;
            score -= tmp.isCreature() ? 4 : 0;
            for (Card aura : tmp.getEnchantedBy(false)) {
            	if (aura.getController().isOpponentOf(tmp.getController())) {
            		score += 5;
            	} else {
            		score -= 5;
            	}
            }
            if (score >= maxScore) {
                worstLand = tmp;
                maxScore = score;
            }
        }
        return worstLand;
    } // end getWorstLand

    public static Card getBestLandToAnimate(final Iterable<Card> lands) {
        Card land = null;
        int maxScore = 0;
        // first, check for tapped, basic lands
        for (Card tmp : lands) {
            // TODO Improve this by choosing basic lands that I have plenty of mana in
            int score = tmp.isTapped() ? 0 : 2;
            score += tmp.isBasicLand() ? 2 : 0;
            score -= tmp.isCreature() ? 4 : 0;
            score -= 5 * tmp.getEnchantedBy(false).size();

            if (score >= maxScore) {
                land = tmp;
                maxScore = score;
            }
        }
        return land;
    } // end getBestLandToAnimate

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
            }
            else if (logic.equals("MostProminentInComputerDeck")) {
                chosen.add(ComputerUtilCard.getMostProminentColor(CardLists.filterControlledBy(game.getCardsInGame(), ai), colorChoices));
            }
            else if (logic.equals("MostProminentDualInComputerDeck")) {
                List<String> prominence = ComputerUtilCard.getColorByProminence(CardLists.filterControlledBy(game.getCardsInGame(), ai));
                chosen.add(prominence.get(0));
                chosen.add(prominence.get(1));
            }
            else if (logic.equals("MostProminentInGame")) {
                chosen.add(ComputerUtilCard.getMostProminentColor(game.getCardsInGame(), colorChoices));
            }
            else if (logic.equals("MostProminentHumanCreatures")) {
                CardCollectionView list = opp.getCreaturesInPlay();
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
                chosen.add(ComputerUtilCard.getMostProminentColor(game.getCardsIn(ZoneType.Battlefield), colorChoices));
            }
            else if (logic.equals("MostProminentAttackers") && game.getPhaseHandler().inCombat()) {
                chosen.add(ComputerUtilCard.getMostProminentColor(game.getCombat().getAttackers(), colorChoices));
            }
            else if (logic.equals("MostProminentInActivePlayerHand")) {
                chosen.add(ComputerUtilCard.getMostProminentColor(game.getPhaseHandler().getPlayerTurn().getCardsIn(ZoneType.Hand), colorChoices));
            }
            else if (logic.equals("MostProminentInComputerDeckButGreen")) {
            	List<String> prominence = ComputerUtilCard.getColorByProminence(CardLists.filterControlledBy(game.getCardsInGame(), ai));
            	if (prominence.get(0) == MagicColor.Constant.GREEN) {
                    chosen.add(prominence.get(1));
            	} else {
                    chosen.add(prominence.get(0));
            	}
            }
            else if (logic.equals("MostExcessOpponentControls")) {
            	int maxExcess = 0;
            	String bestColor = Constant.GREEN;
            	for (byte color : MagicColor.WUBRG) {
            		CardCollectionView ailist = ai.getCardsIn(ZoneType.Battlefield);
            		CardCollectionView opplist = ai.getOpponent().getCardsIn(ZoneType.Battlefield);
            		
            		ailist = CardLists.filter(ailist, CardPredicates.isColor(color));
            		opplist = CardLists.filter(opplist, CardPredicates.isColor(color));

                    int excess = evaluatePermanentList(opplist) - evaluatePermanentList(ailist);
                    if (excess > maxExcess) {
                    	maxExcess = excess;
                    	bestColor = MagicColor.toLongString(color);
                    }
                }
                chosen.add(bestColor);
            }
            else if (logic.equals("MostProminentKeywordInComputerDeck")) {
                CardCollectionView list = ai.getAllCards();
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
        if (chosen.isEmpty()) {
            chosen.add(MagicColor.Constant.GREEN);
        }
        return chosen;
    }
    
    public static boolean useRemovalNow(final SpellAbility sa, final Card c, final int dmg, ZoneType destination) {
        final Player ai = sa.getActivatingPlayer();
        final Player opp = ai.getOpponent();
        final Game game = ai.getGame();
        final PhaseHandler ph = game.getPhaseHandler();
        final PhaseType phaseType = ph.getPhase();

        final int costRemoval = sa.getHostCard().getCMC();
        final int costTarget = c.getCMC();
        
        if (!sa.isSpell()) {
        	return true;
        }
        
        //Check for cards that profit from spells - for example Prowess or Threshold
        if (phaseType == PhaseType.MAIN1 && ComputerUtil.castSpellInMain1(ai, sa)) {
            return true;
        }
        
        //interrupt 1: Check whether a possible blocker will be killed for the AI to make a bigger attack
        if (ph.is(PhaseType.MAIN1) && ph.isPlayerTurn(ai) && c.isCreature()) {
            AiAttackController aiAtk = new AiAttackController(ai);
            final Combat combat = new Combat(ai);
            aiAtk.removeBlocker(c);
            aiAtk.declareAttackers(combat);
        	if (!combat.getAttackers().isEmpty()) {
                AiAttackController aiAtk2 = new AiAttackController(ai);
                final Combat combat2 = new Combat(ai);
                aiAtk2.declareAttackers(combat2);
                if (combat.getAttackers().size() > combat2.getAttackers().size()) {
                	return true;
                }
        	}
        }

        // interrupt 2: remove blocker to save my attacker
        if (ph.is(PhaseType.COMBAT_DECLARE_BLOCKERS) && !ph.isPlayerTurn(ai)) {
            Combat currCombat = game.getCombat();
            if (currCombat != null && !currCombat.getAllBlockers().isEmpty() && currCombat.getAllBlockers().contains(c)) {
                for (Card attacker : currCombat.getAttackersBlockedBy(c)) {
                    if (attacker.getShieldCount() == 0 && ComputerUtilCombat.attackerWouldBeDestroyed(ai, attacker, currCombat)) {
                        CardCollection blockers = currCombat.getBlockers(attacker);
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
        
        // interrupt 3:  two for one = good
        if (c.isEnchanted()) {
            boolean myEnchants = false;
            for (Card enc : c.getEnchantedBy(false)) {
                if (enc.getOwner().equals(ai)) {
                    myEnchants = true;
                    break;
                }
            }
            if (!myEnchants) {
                return true;    //card advantage > tempo
            }
        }
        
        //interrupt 4: opponent pumping target (only works if the pump target is the chosen best target to begin with)
        final MagicStack stack = game.getStack();
        if (!stack.isEmpty()) {
            final SpellAbility topStack = stack.peekAbility();
            if (topStack.getActivatingPlayer().equals(opp) && c.equals(topStack.getTargetCard()) && topStack.isSpell()) {
            	return true;
            }
        }
        
        //burn and curse spells
        float valueBurn = 0;
        if (dmg > 0) {
            if (sa.getDescription().contains("would die, exile it instead")) {
                destination = ZoneType.Exile;
            }
            valueBurn = 1.0f * c.getNetToughness() / dmg;
            valueBurn *= valueBurn;
            if (sa.getTargetRestrictions().canTgtPlayer()) {
                valueBurn /= 2;     //preserve option to burn to the face
            }
            if (valueBurn >= 0.8 && phaseType.isBefore(PhaseType.COMBAT_END)) {
            	return true;
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
        if (!ph.isPlayerTurn(ai) && ph.getPhase().equals(PhaseType.END_OF_TURN)) {
            valueTempo *= 2;    //prefer to cast at opponent EOT
        }
        if (valueTempo >= 0.8 && ph.getPhase().isBefore(PhaseType.COMBAT_END)) {
        	return true;
        }
        
        //evaluate threat of targeted card
        float threat = 0;
        if (c.isCreature()) {
        	// the base value for evaluate creature is 100
        	threat += (-1 + 1.0f * ComputerUtilCard.evaluateCreature(c) / 100) / costRemoval;
        	if (ai.getLife() > 0 && ComputerUtilCombat.canAttackNextTurn(c)) {
        		Combat combat = game.getCombat();
        		threat += 1.0f * ComputerUtilCombat.damageIfUnblocked(c, opp, combat, true) / ai.getLife();
        		//TODO:add threat from triggers and other abilities (ie. Master of Cruelties)
        	}
        	if (ph.isPlayerTurn(ai) && phaseType.isAfter(PhaseType.COMBAT_DECLARE_BLOCKERS)) {
        		threat *= 0.1f;
        	}
        	if (!ph.isPlayerTurn(ai) && 
        			(phaseType.isBefore(PhaseType.COMBAT_BEGIN) || phaseType.isAfter(PhaseType.COMBAT_DECLARE_BLOCKERS))) {
        		threat *= 0.1f;
        	}
        } else if (c.isPlaneswalker()) {
            threat = 1;
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
        if (!c.getManaAbilities().isEmpty()) {
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
     * Decides if the "pump" is worthwhile
     * @param ai casting player
     * @param sa Pump* or CounterPut*
     * @param c target of sa
     * @param toughness +T
     * @param power +P
     * @param keywords additional keywords from sa (only for Pump)
     * @return
     */
    public static boolean shouldPumpCard(final Player ai, final SpellAbility sa, final Card c, final int toughness,
                                         final int power, final List<String> keywords) {
        return shouldPumpCard(ai, sa, c, toughness, power, keywords, false);
    }

    public static boolean shouldPumpCard(final Player ai, final SpellAbility sa, final Card c, final int toughness,
            final int power, final List<String> keywords, boolean immediately) {
        final Game game = ai.getGame();
        final PhaseHandler phase = game.getPhaseHandler();
        final Combat combat = phase.getCombat();
        
        if (!c.canBeTargetedBy(sa)) {
            return false;
        }

        if (c.getNetToughness() + toughness <= 0) {
            return false;
        }

        /* -- currently disabled until better conditions are devised and the spell prediction is made smarter --
        // Determine if some mana sources need to be held for the future spell to cast in Main 2 before determining whether to pump.
        AiController aic = ((PlayerControllerAi)ai.getController()).getAi();
        if (aic.getCardMemory().isMemorySetEmpty(AiCardMemory.MemorySet.HELD_MANA_SOURCES)) {
            // only hold mana sources once
            SpellAbility futureSpell = aic.predictSpellToCastInMain2(ApiType.Pump);
            if (futureSpell != null && futureSpell.getHostCard() != null) {
                aic.reserveManaSourcesForMain2(futureSpell);
            }
        }
        */

        // will the creature attack (only relevant for sorcery speed)?
        if (phase.getPhase().isBefore(PhaseType.COMBAT_DECLARE_ATTACKERS)
                && phase.isPlayerTurn(ai)
                && SpellAbilityAi.isSorcerySpeed(sa)
                && power > 0
                && ComputerUtilCard.doesCreatureAttackAI(ai, c)) {
            return true;
        }

        // buff attacker/blocker using triggered pump
        if (immediately && phase.getPhase().isBefore(PhaseType.COMBAT_DECLARE_ATTACKERS)) {
            if (phase.isPlayerTurn(ai)) {
                if (CombatUtil.canAttack(c)) {
                    return true;
                }
            } else {
                if (CombatUtil.canBlock(c)) {
                    return true;
                }
            }
        }

        final Player opp = ai.getOpponent();
        Card pumped = getPumpedCreature(ai, sa, c, toughness, power, keywords);
        List<Card> oppCreatures = opp.getCreaturesInPlay();
        float chance = 0;
        
        //create and buff attackers
        if (phase.getPhase().isBefore(PhaseType.COMBAT_DECLARE_ATTACKERS) && phase.isPlayerTurn(ai) && opp.getLife() > 0) {
            //1. become attacker for whatever reason
            if (!ComputerUtilCard.doesCreatureAttackAI(ai, c) && ComputerUtilCard.doesSpecifiedCreatureAttackAI(ai, pumped)) {
                float threat = 1.0f * ComputerUtilCombat.damageIfUnblocked(pumped, opp, combat, true) / opp.getLife();
                if (CardLists.filter(oppCreatures, CardPredicates.possibleBlockers(pumped)).isEmpty()) {
                    threat *= 2;
                }
                if (c.getNetPower() == 0 && c == sa.getHostCard() && power > 0 ) {
                    threat *= 4;    //over-value self +attack for 0 power creatures which may be pumped further after attacking 
                }
                chance += threat;
            }
            
            //2. grant haste
            if (keywords.contains("Haste") && c.hasSickness() && !c.isTapped()) {
                chance += 0.5f;
                if (ComputerUtilCard.doesSpecifiedCreatureAttackAI(ai, pumped)) {
                    chance += 0.5f * ComputerUtilCombat.damageIfUnblocked(pumped, opp, combat, true) / opp.getLife();
                }
            }
            
            //3. grant evasive
            if (!CardLists.filter(oppCreatures, CardPredicates.possibleBlockers(c)).isEmpty()) {
                if (CardLists.filter(oppCreatures, CardPredicates.possibleBlockers(pumped)).isEmpty() 
                        && ComputerUtilCard.doesSpecifiedCreatureAttackAI(ai, pumped)) {
                    chance += 0.5f * ComputerUtilCombat.damageIfUnblocked(pumped, opp, combat, true) / opp.getLife();
                }
            }
        }
        
        //combat trickery
        if (phase.is(PhaseType.COMBAT_DECLARE_BLOCKERS)) {
            //clunky code because ComputerUtilCombat.combatantWouldBeDestroyed() does not work for this sort of artificial combat
            Combat pumpedCombat = new Combat(phase.isPlayerTurn(ai) ? ai : opp);
            List<Card> opposing = null;
            boolean pumpedWillDie = false;
            final boolean isAttacking = combat.isAttacking(c);
            if (isAttacking) {
                pumpedCombat.addAttacker(pumped, opp);
                opposing = combat.getBlockers(c);
                for (Card b : opposing) {
                    pumpedCombat.addBlocker(pumped, b);
                }
                if (ComputerUtilCombat.attackerWouldBeDestroyed(ai, pumped, pumpedCombat)) {
                    pumpedWillDie = true;
                }
            } else {
                opposing = combat.getAttackersBlockedBy(c);
                for (Card a : opposing) {
                    pumpedCombat.addAttacker(a, ai);
                    pumpedCombat.addBlocker(a, pumped);
                }
                if (ComputerUtilCombat.blockerWouldBeDestroyed(ai, pumped, pumpedCombat)) {
                    pumpedWillDie = true;
                }
            }
            
            //1. save combatant
            if (ComputerUtilCombat.combatantWouldBeDestroyed(ai, c, combat) && !pumpedWillDie 
                    && !c.hasKeyword("Indestructible")) {   // hack because attackerWouldBeDestroyed() does not
                                                            // check for Indestructible when computing lethal damage
                return true;
            }
            
            //2. kill combatant
            boolean survivor = false;
            for (Card o : opposing) {
                if (!ComputerUtilCombat.combatantWouldBeDestroyed(opp, o, combat)) {
                    survivor = true;
                    break;
                }
            }
            if (survivor) {
                for (Card o : opposing) {
                    if (!ComputerUtilCombat.combatantWouldBeDestroyed(opp, o, combat) 
                    		&& !(o.hasSVar("SacMe") && Integer.parseInt(o.getSVar("SacMe")) > 2)) {
                        if (isAttacking) {
                            if (ComputerUtilCombat.blockerWouldBeDestroyed(opp, o, pumpedCombat)) {
                                return true;
                            }
                        } else {
                            if (ComputerUtilCombat.attackerWouldBeDestroyed(opp, o, pumpedCombat)) {
                                return true;
                            }
                        }
                    }
                }
            }
            
            //3. buff attacker
            if (combat.isAttacking(c) && opp.getLife() > 0) {
                int dmg = ComputerUtilCombat.damageIfUnblocked(c, opp, combat, true);
                int pumpedDmg = ComputerUtilCombat.damageIfUnblocked(pumped, opp, pumpedCombat, true);
                if (combat.isBlocked(c)) {
                    if (!c.hasKeyword("Trample")) {
                        dmg = 0;
                    }
                    if (c.hasKeyword("Trample") || keywords.contains("Trample")) {
                       for (Card b : combat.getBlockers(c)) {
                           pumpedDmg -= ComputerUtilCombat.getDamageToKill(b);
                       }
                    } else {
                        pumpedDmg = 0;
                    }
                }
                if (pumpedDmg >= opp.getLife()) {
                    return true;
                }
                float value = 1.0f * (pumpedDmg - dmg);
                if (c == sa.getHostCard() && power > 0) {
                    int divisor = sa.getPayCosts().getTotalMana().getCMC();
                    if (divisor <= 0) {
                        divisor = 1;
                    }
                    value *= power / divisor;
                } else {
                    value /= opp.getLife();
                }
                chance += value;
            }
            
            //4. lifelink
            if (ai.canGainLife() && ai.getLife() > 0 && !c.hasKeyword("Lifelink") && keywords.contains("Lifelink")
                    && (combat.isAttacking(c) || combat.isBlocking(c))) {
                int dmg = pumped.getNetCombatDamage();
                //The actual dmg inflicted should be the sum of ComputerUtilCombat.predictDamageTo() for opposing creature
                //and trample damage (if any)
                chance += 1.0f * dmg / ai.getLife();
            }
            
            //5. if the life of the computer is in danger, try to pump blockers blocking Tramplers
            if (combat.isBlocking(c) && toughness > 0 ) {
                List<Card> blockedBy = combat.getAttackersBlockedBy(c);
                boolean attackerHasTrample = false;
                for (Card b : blockedBy) {
                    attackerHasTrample |= b.hasKeyword("Trample");
                }
                if (attackerHasTrample && (sa.isAbility() || ComputerUtilCombat.lifeInDanger(ai, combat))) {
                    return true;
                }
            }
        }
        return MyRandom.getRandom().nextFloat() < chance;
    }
    
    /**
     * Apply "pump" ability and return modified creature
     * @param ai casting player
     * @param sa Pump* or CounterPut*
     * @param c target of sa
     * @param toughness +T
     * @param power +P
     * @param keywords additional keywords from sa (only for Pump)
     * @return
     */
    public static Card getPumpedCreature(final Player ai, final SpellAbility sa,
            final Card c, final int toughness, final int power,
            final List<String> keywords) {
        Card pumped = CardFactory.copyCard(c, true);
        pumped.setSickness(c.hasSickness());
        final long timestamp = c.getGame().getNextTimestamp();
        final List<String> kws = new ArrayList<String>();
        for (String kw : keywords) {
            if (kw.startsWith("HIDDEN")) {
                pumped.addHiddenExtrinsicKeyword(kw);
            } else {
                kws.add(kw);
            }
        }
        pumped.addNewPT(c.getCurrentPower(), c.getCurrentToughness(), timestamp);
        pumped.addTempPowerBoost(c.getTempPowerBoost() + power);
        pumped.addTempToughnessBoost(c.getTempToughnessBoost() + toughness);
        pumped.addChangedCardKeywords(kws, new ArrayList<String>(), false, timestamp);
        Set<CounterType> types = c.getCounters().keySet();
        for(CounterType ct : types) {
            pumped.addCounterFireNoEvents(ct, c.getCounters(ct), true);
        }
        //Copies tap-state and extra keywords (auras, equipment, etc.) 
        if (c.isTapped()) {
            pumped.setTapped(true);
        }
        final List<String> copiedKeywords = pumped.getKeywords();
        List<String> toCopy = new ArrayList<String>();
        for (String kw : c.getKeywords()) {
            if (!copiedKeywords.contains(kw)) {
                if (kw.startsWith("HIDDEN")) {
                    pumped.addHiddenExtrinsicKeyword(kw);
                } else {
                    toCopy.add(kw);
                }
            }
        }
        final long timestamp2 = c.getGame().getNextTimestamp(); //is this necessary or can the timestamp be re-used?
        pumped.addChangedCardKeywords(toCopy, new ArrayList<String>(), false, timestamp2);
        ComputerUtilCard.applyStaticContPT(ai.getGame(), pumped, new CardCollection(c));
        return pumped;
    }
    
    /**
     * Applies static continuous Power/Toughness effects to a (virtual) creature.
     * @param game game instance to work with 
     * @param vCard creature to work with
     * @param exclude list of cards to exclude when considering ability sources, accepts null
     */
    public static void applyStaticContPT(final Game game, Card vCard, final CardCollectionView exclude) {
        if (!vCard.isCreature()) {
            return;
        }
        final CardCollection list = new CardCollection(game.getCardsIn(ZoneType.Battlefield));
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
                if (!vCard.isValid(valid, c.getController(), c, null)) {
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
                    vCard.addTempPowerBoost(att);
                }
                if (params.containsKey("AddToughness")) {
                    String addT = params.get("AddToughness");
                    int def = 0;
                    if (addT.equals("AffectedY")) {
                        def = CardFactoryUtil.xCount(vCard, AbilityUtils.getSVar(stAb, addT));
                    } else {
                        def = AbilityUtils.calculateAmount(c, addT, stAb);
                    }
                    vCard.addTempToughnessBoost(def);
                }
            }
        }
    }
    
    /**
     * Evaluate if the ability can save a target against removal
     * @param ai casting player
     * @param sa Pump* or CounterPut*
     * @return
     */
    public static boolean canPumpAgainstRemoval(Player ai, SpellAbility sa) {
        final List<GameObject> objects = ComputerUtil.predictThreatenedObjects(sa.getActivatingPlayer(), sa);
        final CardCollection threatenedTargets = new CardCollection();
        final TargetRestrictions tgt = sa.getTargetRestrictions();

        if (tgt == null) {
            final List<Card> cards = AbilityUtils.getDefinedCards(sa.getHostCard(), sa.getParam("Defined"), sa);
            for (final Card card : cards) {
                if (objects.contains(card)) {
                    return true;
                }
            }
            // For pumps without targeting restrictions, just return immediately until this is fleshed out.
            return false;
        }

        CardCollection targetables = CardLists.getValidCards(ai.getCardsIn(ZoneType.Battlefield), tgt.getValidTgts(), ai, sa.getHostCard(), sa);
        targetables = CardLists.getTargetableCards(targetables, sa);
        targetables = ComputerUtil.getSafeTargets(ai, sa, targetables);
        for (final Card c : targetables) {
            if (objects.contains(c)) {
                threatenedTargets.add(c);
            }
        }
        if (!threatenedTargets.isEmpty()) {
            ComputerUtilCard.sortByEvaluateCreature(threatenedTargets);
            for (Card c : threatenedTargets) {
                sa.getTargets().add(c);
                if (sa.getTargets().getNumTargeted() >= tgt.getMaxTargets(sa.getHostCard(), sa)) {
                    break;
                }
            }
            if (sa.getTargets().getNumTargeted() > tgt.getMaxTargets(sa.getHostCard(), sa)
                    || sa.getTargets().getNumTargeted() < tgt.getMinTargets(sa.getHostCard(), sa)) {
                sa.resetTargets();
                return false;
            }
            return true;
        }
        return false;
    }
}
