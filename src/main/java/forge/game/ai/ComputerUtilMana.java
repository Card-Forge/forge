package forge.game.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Predicate;

import forge.Card;
import forge.CardLists;
import forge.GameActionUtil;
import forge.Singletons;
import forge.card.SpellManaCost;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.cost.Cost;
import forge.card.cost.CostPayment;
import forge.card.mana.ManaCostBeingPaid;
import forge.card.mana.ManaCostShard;
import forge.card.mana.ManaPool;
import forge.card.spellability.AbilityManaPart;
import forge.card.spellability.SpellAbility;
import forge.control.input.InputPayManaCostUtil;
import forge.game.GameState;
import forge.game.player.AIPlayer;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class ComputerUtilMana {

    /**
     * <p>
     * payManaCost.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param ai
     *            a {@link forge.game.player.Player} object.
     * @param test
     *            (is for canPayCost, if true does not change the game state)
     * @param extraMana
     *            a int.
     * @param checkPlayable
     *            should we check if playable? use for hypothetical "can AI play this"
     * @return a boolean.
     * @since 1.0.15
     */
    public static boolean payManaCost(final SpellAbility sa, final Player ai, final boolean test, final int extraMana, boolean checkPlayable) {
        ManaCostBeingPaid cost = ComputerUtilMana.calculateManaCost(sa, test, extraMana);
    
        final GameState game = Singletons.getModel().getGame();
        final ManaPool manapool = ai.getManaPool();
    
        cost = manapool.payManaFromPool(sa, cost);
    
        if (cost.isPaid()) {
            // refund any mana taken from mana pool when test
            manapool.clearManaPaid(sa, test);
            return true;
        }
    
        // get map of mana abilities
        final Map<String, List<SpellAbility>> manaAbilityMap = ComputerUtilMana.mapManaSources((AIPlayer) ai, checkPlayable);
        // initialize ArrayList list for mana needed
        final List<List<SpellAbility>> partSources = new ArrayList<List<SpellAbility>>();
        final List<Integer> partPriority = new ArrayList<Integer>();
        final String[] costParts = cost.toString().replace("X ", "").split(" ");
        boolean foundAllSources = ComputerUtilMana.findManaSources(ai, manaAbilityMap, partSources, partPriority, costParts);
        if (!foundAllSources) {
            if (!test) {
                // real payment should not arrive here
                throw new RuntimeException("ComputerUtil : payManaCost() cost was not paid for " + sa.getSourceCard());
            }
            manapool.clearManaPaid(sa, test); // refund any mana taken from mana pool
            return false;
        }
    
        // Create array to keep track of sources used
        final ArrayList<Card> usedSources = new ArrayList<Card>();
        // this is to prevent errors for mana sources that have abilities that
        // cost mana.
        usedSources.add(sa.getSourceCard());
        // Loop over mana needed
        int nPriority = 0;
        while (nPriority < partPriority.size()) {
            final int nPart = partPriority.get(nPriority);
            final ManaCostBeingPaid costPart = new ManaCostBeingPaid(costParts[nPart]);
            // Loop over mana abilities that can be used to current mana cost part
            for (final SpellAbility ma : partSources.get(nPart)) {
                final Card sourceCard = ma.getSourceCard();
    
                // Check if source has already been used
                if (usedSources.contains(sourceCard)) {
                    continue;
                }
    
                // Check if AI can still play this mana ability
                ma.setActivatingPlayer(ai);
                // if the AI can't pay the additional costs skip the mana ability
                if (ma.getPayCosts() != null && checkPlayable) {
                    if (!ComputerUtilCost.canPayAdditionalCosts(ma, ai, game)) {
                        continue;
                    }
                } else if (sourceCard.isTapped() && checkPlayable) {
                    continue;
                }
    
                AbilityManaPart m = ma.getManaPart();
                // Check for mana restrictions
                if (!m.meetsManaRestrictions(sa)) {
                    continue;
                }
    
                String manaProduced;
                // Check if paying snow mana
                if ("S".equals(costParts[nPart])) {
                    manaProduced = "S";
                } else {
                    if (m.isComboMana()) {
                        String colorChoice = costParts[nPart];
                        m.setExpressChoice(colorChoice);
                        colorChoice = ComputerUtilMana.getComboManaChoice(ai, ma, sa, cost);
                        m.setExpressChoice(colorChoice);
                    }
                    // check if ability produces any color
                    else if (m.isAnyMana()) {
                        String colorChoice = costParts[nPart];
                        final ArrayList<String> negEffect = cost.getManaNeededToAvoidNegativeEffect();
                        final ArrayList<String> negEffectPaid = cost.getManaPaidToAvoidNegativeEffect();
                        // Check for
                        // 1) Colorless
                        // 2) Split e.g. 2/G
                        // 3) Hybrid e.g. UG
                        if (costParts[nPart].matches("[0-9]+")) {
                            colorChoice = "W";
                            for (int n = 0; n < negEffect.size(); n++) {
                                if (!negEffectPaid.contains(negEffect.get(n))) {
                                    colorChoice = negEffect.get(n);
                                    break;
                                }
                            }
                        } else if (costParts[nPart].contains("/")) {
                            colorChoice = costParts[nPart].replace("2/", "");
                        } else if (costParts[nPart].length() > 1) {
                            colorChoice = costParts[nPart].substring(0, 1);
                            for (int n = 0; n < negEffect.size(); n++) {
                                if (costParts[nPart].contains(negEffect.get(n))
                                        && !negEffectPaid.contains(negEffect.get(n))) {
                                    colorChoice = negEffect.get(n);
                                    break;
                                }
                            }
                        }
                        m.setExpressChoice(colorChoice);
                    }
                    // get produced mana
                    manaProduced = GameActionUtil.generatedMana(ma);
                    if (manaProduced.matches("0")) {
                        continue;
                    }
                }
    
                // add source card to used list
                usedSources.add(sourceCard);
    
                costPart.payMultipleMana(manaProduced);
    
                if (!test) {
                    // Pay additional costs
                    if (ma.getPayCosts() != null) {
                        final CostPayment pay = new CostPayment(ma.getPayCosts(), ma, game);
                        if (!pay.payComputerCosts((AIPlayer)ai, game)) {
                            continue;
                        }
                    } else {
                        sourceCard.tap();
                    }
                    // resolve mana ability
                    //ma.resolve();
                    AbilityFactory.resolve(ma, false);
                    // subtract mana from mana pool
                    cost = manapool.payManaFromAbility(sa, cost, ma);
                } else {
                    cost.payMultipleMana(manaProduced);
                }
                // check if cost part is paid
                if (costPart.isPaid() || cost.isPaid()) {
                    break;
                }
            } // end of mana ability loop
    
            if (!costPart.isPaid() || cost.isPaid()) {
                break;
            } else {
                nPriority++;
            }
        } // end of cost parts loop
    
        //check for phyrexian mana
        if (!cost.isPaid() && cost.containsPhyrexianMana() && ai.getLife() > 5 && ai.canPayLife(2)) {
            cost.payPhyrexian();
            if (!test) {
                ai.payLife(2, sa.getSourceCard());
            }
        }
    
        manapool.clearManaPaid(sa, test);
        // check if paid
        if (cost.isPaid()) {
            // if (sa instanceof Spell_Permanent) // should probably add this
            sa.getSourceCard().setColorsPaid(cost.getColorsPaid());
            sa.getSourceCard().setSunburstValue(cost.getSunburst());
            return true;
        }
    
        if (!test) {
            final StringBuilder sb = new StringBuilder();
            sb.append("ComputerUtil : payManaCost() cost was not paid for ");
            sb.append(sa.getSourceCard().getName());
            throw new RuntimeException(sb.toString());
        }
    
        return false;
    
    } // payManaCost()

    /**
     * <p>
     * payManaCost.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    public static boolean payManaCost(final Player ai, final SpellAbility sa) {
        return payManaCost(sa, ai, false, 0, true);
    }

    /**
     * Find all mana sources.
     * @param manaAbilityMap
     * @param partSources
     * @param partPriority
     * @param costParts
     * @param foundAllSources
     * @return Were all mana sources found?
     */
    private static boolean findManaSources(final Player ai, final Map<String, List<SpellAbility>> manaAbilityMap,
            final List<List<SpellAbility>> partSources, final List<Integer> partPriority,
            final String[] costParts) {
        final String[] shortColors = { "W", "U", "B", "R", "G" };
        boolean foundAllSources;
        if (manaAbilityMap.isEmpty()) {
            foundAllSources = false;
        } else {
            foundAllSources = true;
            // loop over cost parts
            for (int nPart = 0; nPart < costParts.length; nPart++) {
                final List<SpellAbility> srcFound = new ArrayList<SpellAbility>();
                // Test for:
                // 1) Colorless
                // 2) Split e.g. 2/G
                // 3) Hybrid e.g. U/G
                // defaults to single short color
                if (costParts[nPart].matches("[0-9]+")) { // Colorless
                    srcFound.addAll(manaAbilityMap.get("1"));
                } else if (costParts[nPart].contains("2/")) { // Split
                    final String colorKey = costParts[nPart].replace("2/", "");
                    // add specified color sources first
                    if (manaAbilityMap.containsKey(colorKey)) {
                        srcFound.addAll(manaAbilityMap.get(colorKey));
                    }
                    // add other available colors
                    for (final String color : shortColors) {
                        if (!colorKey.contains(color)) {
                            // Is source available?
                            if (manaAbilityMap.containsKey(color)) {
                                srcFound.addAll(manaAbilityMap.get(color));
                            }
                        }
                    }
                } else if (costParts[nPart].contains("P")) { // Phyrexian
                    String newPart = costParts[nPart].replace("/P", "");
                    if (manaAbilityMap.containsKey(newPart)) {
                        srcFound.addAll(manaAbilityMap.get(newPart));
                    } else if (ai.getLife() > 8) { //Pay with life
                        partSources.add(nPart, srcFound);
                        continue;
                    }
                } else if (costParts[nPart].length() > 1) { // Hybrid
                    final String firstColor = costParts[nPart].substring(0, 1);
                    final String secondColor = costParts[nPart].substring(2);
                    final boolean foundFirst = manaAbilityMap.containsKey(firstColor);
                    final boolean foundSecond = manaAbilityMap.containsKey(secondColor);
                    if (foundFirst || foundSecond) {
                        if (!foundFirst) {
                            srcFound.addAll(manaAbilityMap.get(secondColor));
                        } else if (!foundSecond) {
                            srcFound.addAll(manaAbilityMap.get(firstColor));
                        } else if (manaAbilityMap.get(firstColor).size() > manaAbilityMap.get(secondColor).size()) {
                            srcFound.addAll(manaAbilityMap.get(firstColor));
                            srcFound.addAll(manaAbilityMap.get(secondColor));
                        } else {
                            srcFound.addAll(manaAbilityMap.get(secondColor));
                            srcFound.addAll(manaAbilityMap.get(firstColor));
                        }
                    }
                } else { // single color
                    if (manaAbilityMap.containsKey(costParts[nPart])) {
                        srcFound.addAll(manaAbilityMap.get(costParts[nPart]));
                    }
                }
    
                // add sources to array lists
                partSources.add(nPart, srcFound);
                // add to sorted priority list
                if (srcFound.size() > 0) {
                    int i;
                    for (i = 0; i < partPriority.size(); i++) {
                        if (srcFound.size() <= partSources.get(i).size()) {
                            break;
                        }
                    }
                    partPriority.add(i, nPart);
                } else {
                    foundAllSources = false;
                    break;
                }
            }
        }
        return foundAllSources;
    }

    private static void increaseManaCostByX(SpellAbility sa, ManaCostBeingPaid cost, int xValue) {
            String manaXColor = sa.hasParam("XColor") ? sa.getParam("XColor") : "";
            if (manaXColor.isEmpty()) {
                cost.increaseColorlessMana(xValue);
            } else {
                if (manaXColor.equals("B")) {
                    cost.increaseShard(ManaCostShard.BLACK, xValue);
                } else if (manaXColor.equals("G")) {
                    cost.increaseShard(ManaCostShard.GREEN, xValue);
                } else if (manaXColor.equals("R")) {
                    cost.increaseShard(ManaCostShard.RED, xValue);
                } else if (manaXColor.equals("U")) {
                    cost.increaseShard(ManaCostShard.BLUE, xValue);
                } else if (manaXColor.equals("W")) {
                    cost.increaseShard(ManaCostShard.WHITE, xValue);
                    
    /* Max: Never seen these options in real cards
                } else if (manaXColor.contains("B") && manaXColor.contains("G")) {
                    cost.increaseShard(ManaCostShard.BG, xValue);
                } else if (manaXColor.contains("B") && manaXColor.contains("R")) {
                    cost.increaseShard(ManaCostShard.BR, xValue);
                } else if (manaXColor.contains("R") && manaXColor.contains("G")) {
                    cost.increaseShard(ManaCostShard.RG, xValue);
                } else if (manaXColor.contains("U") && manaXColor.contains("B")) {
                    cost.increaseShard(ManaCostShard.UB, xValue);
                } else if (manaXColor.contains("U") && manaXColor.contains("G")) {
                    cost.increaseShard(ManaCostShard.UG, xValue);
                } else if (manaXColor.contains("U") && manaXColor.contains("R")) {
                    cost.increaseShard(ManaCostShard.UR, xValue);
                } else if (manaXColor.contains("W") && manaXColor.contains("B")) {
                    cost.increaseShard(ManaCostShard.WB, xValue);
                } else if (manaXColor.contains("W") && manaXColor.contains("G")) {
                    cost.increaseShard(ManaCostShard.WG, xValue);
                } else if (manaXColor.contains("W") && manaXColor.contains("R")) {
                    cost.increaseShard(ManaCostShard.WR, xValue);
                } else if (manaXColor.contains("W") && manaXColor.contains("U")) {
                    cost.increaseShard(ManaCostShard.WU, xValue);
    */
                }
            }
        }

    /**
     * Calculate the ManaCost for the given SpellAbility.
     * @param sa
     * @param test
     * @param extraMana
     * @return ManaCost
     */
    private static ManaCostBeingPaid calculateManaCost(final SpellAbility sa, final boolean test, final int extraMana) {
        final SpellManaCost mana = sa.getPayCosts() != null ? sa.getPayCosts().getTotalMana() : sa.getManaCost();
    
        ManaCostBeingPaid cost = new ManaCostBeingPaid(mana);
    
        cost = Singletons.getModel().getGame().getAction().getSpellCostChange(sa, cost);
    
        final Card card = sa.getSourceCard();
        // Tack xMana Payments into mana here if X is a set value
        if ((sa.getPayCosts() != null) && (cost.getXcounter() > 0 || extraMana > 0)) {
    
            int manaToAdd = 0;
            if (test && extraMana > 0) {
                final int multiplicator = Math.max(cost.getXcounter(), 1);
                manaToAdd = extraMana * multiplicator;
            } else {
                // For Count$xPaid set PayX in the AFs then use that here
                // Else calculate it as appropriate.
                final String xSvar = card.getSVar("X").startsWith("Count$xPaid") ? "PayX" : "X";
                if (!card.getSVar(xSvar).equals("")) {
                    if (xSvar.equals("PayX")) {
                        manaToAdd = Integer.parseInt(card.getSVar(xSvar)) * cost.getXcounter(); // X
                    } else {
                        manaToAdd = AbilityFactory.calculateAmount(card, xSvar, sa) * cost.getXcounter();
                    }
                }
            }
    
            increaseManaCostByX(sa, cost, manaToAdd);
            
            if (!test) {
                card.setXManaCostPaid(manaToAdd / cost.getXcounter());
            }
        }
    
        // Make mana needed to avoid negative effect a mandatory cost for the AI
        if (!card.getSVar("ManaNeededToAvoidNegativeEffect").equals("")) {
            final String[] negEffects = card.getSVar("ManaNeededToAvoidNegativeEffect").split(",");
            int amountAdded = 0;
            for (int nStr = 0; nStr < negEffects.length; nStr++) {
                // convert long color strings to short color strings
                if (negEffects[nStr].length() > 1) {
                    negEffects[nStr] = InputPayManaCostUtil.getShortColorString(negEffects[nStr]);
                }
                // make mana mandatory for AI
                if (!cost.isColor(negEffects[nStr])) {
                    cost.combineManaCost(negEffects[nStr]);
                    amountAdded++;
                }
            }
            cost.setManaNeededToAvoidNegativeEffect(negEffects);
            // TODO: should it be an error condition if amountAdded is greater
            // than the colorless in the original cost? (ArsenalNut - 120102)
            // adjust colorless amount to account for added mana
            cost.decreaseColorlessMana(amountAdded);
        }
        return cost;
    }

    /**
     * <p>
     * getAvailableMana.
     * </p>
     * 
     * @param ai
     *            a {@link forge.game.player.Player} object.
     * @param checkPlayable
     * @return a {@link forge.CardList} object.
     */
    private static List<Card> getAvailableMana(final AIPlayer ai, final boolean checkPlayable) {
        final GameState game = Singletons.getModel().getGame();
        final List<Card> list = ai.getCardsIn(ZoneType.Battlefield);
        final List<Card> manaSources = CardLists.filter(list, new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                if (checkPlayable) {
                    for (final SpellAbility am : c.getAIPlayableMana()) {
                        am.setActivatingPlayer(ai);
                        if (am.canPlay()) {
                            return true;
                        }
                    }
    
                    return false;
                } else {
                    return true;
                }
            }
        }); // CardListFilter
    
        final List<Card> sortedManaSources = new ArrayList<Card>();
        final List<Card> otherManaSources = new ArrayList<Card>();
        final List<Card> colorlessManaSources = new ArrayList<Card>();
        final List<Card> oneManaSources = new ArrayList<Card>();
        final List<Card> twoManaSources = new ArrayList<Card>();
        final List<Card> threeManaSources = new ArrayList<Card>();
        final List<Card> fourManaSources = new ArrayList<Card>();
        final List<Card> fiveManaSources = new ArrayList<Card>();
        final List<Card> anyColorManaSources = new ArrayList<Card>();
    
        // Sort mana sources
        // 1. Use lands that can only produce colorless mana without
        // drawback/cost first
        // 2. Search for mana sources that have a certain number of abilities
        // 3. Use lands that produce any color many
        // 4. all other sources (creature, costs, drawback, etc.)
        for (int i = 0; i < manaSources.size(); i++) {
            final Card card = manaSources.get(i);
    
            if (card.isCreature() || card.isEnchanted()) {
                otherManaSources.add(card);
                continue; // don't use creatures before other permanents
            }
    
            int usableManaAbilities = 0;
            boolean needsLimitedResources = false;
            boolean producesAnyColor = false;
            final ArrayList<SpellAbility> manaAbilities = card.getAIPlayableMana();
    
            for (final SpellAbility m : manaAbilities) {
    
                if (m.getManaPart().isAnyMana()) {
                    producesAnyColor = true;
                }
    
                final Cost cost = m.getPayCosts();
                if (cost != null) {
                    needsLimitedResources |= !cost.isReusuableResource();
                }
    
                // if the AI can't pay the additional costs skip the mana
                // ability
                m.setActivatingPlayer(ai);
                if (cost != null) {
                    if (!ComputerUtilCost.canPayAdditionalCosts(m, ai, game)) {
                        continue;
                    }
                }
    
                // don't use abilities with dangerous drawbacks
                if (m.getSubAbility() != null && !card.getName().equals("Pristine Talisman")) {
                    if (!m.getSubAbility().chkAIDrawback(ai)) {
                        continue;
                    }
                    needsLimitedResources = true; // TODO: check for good
                                                  // drawbacks (gainLife)
                }
                usableManaAbilities++;
            }
    
            if (needsLimitedResources) {
                otherManaSources.add(card);
            } else if (producesAnyColor) {
                anyColorManaSources.add(card);
            } else if (usableManaAbilities == 1) {
                if (manaAbilities.get(0).getManaPart().mana().equals("1")) {
                    colorlessManaSources.add(card);
                } else {
                    oneManaSources.add(card);
                }
            } else if (usableManaAbilities == 2) {
                twoManaSources.add(card);
            } else if (usableManaAbilities == 3) {
                threeManaSources.add(card);
            } else if (usableManaAbilities == 4) {
                fourManaSources.add(card);
            } else {
                fiveManaSources.add(card);
            }
    
        }
        sortedManaSources.addAll(colorlessManaSources);
        sortedManaSources.addAll(oneManaSources);
        sortedManaSources.addAll(twoManaSources);
        sortedManaSources.addAll(threeManaSources);
        sortedManaSources.addAll(fourManaSources);
        sortedManaSources.addAll(fiveManaSources);
        sortedManaSources.addAll(anyColorManaSources);
        //use better creatures later
        CardLists.sortByEvaluateCreature(otherManaSources);
        Collections.reverse(otherManaSources);
        sortedManaSources.addAll(otherManaSources);
        return sortedManaSources;
    } // getAvailableMana()

    /**
     * <p>
     * mapManaSources.
     * </p>
     * 
     * @param ai
     *            a {@link forge.game.player.Player} object.
     * @param checkPlayable TODO
     * @return HashMap<String, List<Card>>
     */
    private static Map<String, List<SpellAbility>> mapManaSources(final AIPlayer ai, boolean checkPlayable) {
        final Map<String, List<SpellAbility>> manaMap = new HashMap<String, List<SpellAbility>>();
    
        final List<SpellAbility> whiteSources = new ArrayList<SpellAbility>();
        final List<SpellAbility> blueSources = new ArrayList<SpellAbility>();
        final List<SpellAbility> blackSources = new ArrayList<SpellAbility>();
        final List<SpellAbility> redSources = new ArrayList<SpellAbility>();
        final List<SpellAbility> greenSources = new ArrayList<SpellAbility>();
        final List<SpellAbility> colorlessSources = new ArrayList<SpellAbility>();
        final List<SpellAbility> snowSources = new ArrayList<SpellAbility>();
    
        // Get list of current available mana sources
        final List<Card> manaSources = getAvailableMana(ai, checkPlayable);
    
        // Loop over all mana sources
        for (int i = 0; i < manaSources.size(); i++) {
            final Card sourceCard = manaSources.get(i);
            final ArrayList<SpellAbility> manaAbilities = sourceCard.getAIPlayableMana();
    
            // Loop over all mana abilities for a source
            for (final SpellAbility m : manaAbilities) {
                m.setActivatingPlayer(ai);
                if (!m.canPlay() && checkPlayable) {
                    continue;
                }
    
                // don't use abilities with dangerous drawbacks
                if (m.getSubAbility() != null) {
                    if (!m.getSubAbility().chkAIDrawback(ai)) {
                        continue;
                    }
                }
    
                // add to colorless source list
                colorlessSources.add(m);
    
                // find possible colors
                if (m.getManaPart().canProduce("W")) {
                    whiteSources.add(m);
                }
                if (m.getManaPart().canProduce("U")) {
                    blueSources.add(m);
                }
                if (m.getManaPart().canProduce("B")) {
                    blackSources.add(m);
                }
                if (m.getManaPart().canProduce("R")) {
                    redSources.add(m);
                }
                if (m.getManaPart().canProduce("G")) {
                    greenSources.add(m);
                }
                if (m.getManaPart().isSnow()) {
                    snowSources.add(m);
                }
            } // end of mana abilities loop
        } // end of mana sources loop
    
        // Add sources
        if (!whiteSources.isEmpty()) {
            manaMap.put("W", whiteSources);
        }
        if (!blueSources.isEmpty()) {
            manaMap.put("U", blueSources);
        }
        if (!blackSources.isEmpty()) {
            manaMap.put("B", blackSources);
        }
        if (!redSources.isEmpty()) {
            manaMap.put("R", redSources);
        }
        if (!greenSources.isEmpty()) {
            manaMap.put("G", greenSources);
        }
        if (!colorlessSources.isEmpty()) {
            manaMap.put("1", colorlessSources);
        }
        if (!snowSources.isEmpty()) {
            manaMap.put("S", snowSources);
        }
    
        return manaMap;
    }

    /**
     * <p>
     * determineLeftoverMana.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param player
     *            a {@link forge.game.player.Player} object.
     * @return a int.
     * @since 1.0.15
     */
    public static int determineLeftoverMana(final SpellAbility sa, final Player player) {
    
        int xMana = 0;
    
        for (int i = 1; i < 99; i++) {
            if (!payManaCost(sa, player, true, i, true)) {
                break;
            }
            xMana = i;
        }
    
        return xMana;
    }

    /**
     * <p>
     * getComboManaChoice.
     * </p>
     * 
     * @param abMana
     *            a {@link forge.card.spellability.AbilityMana} object.
     * @param saRoot
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param cost
     *            a {@link forge.card.mana.ManaCostBeingPaid} object.
     * @return String
     */
    public static String getComboManaChoice(final Player ai, final SpellAbility manaAb, final SpellAbility saRoot, final ManaCostBeingPaid cost) {
    
        final StringBuilder choiceString = new StringBuilder();
    
    
        final Card source = manaAb.getSourceCard();
        final AbilityManaPart abMana = manaAb.getManaPart();
    
        if (abMana.isComboMana()) {
            int amount = manaAb.hasParam("Amount") ? AbilityFactory.calculateAmount(source, manaAb.getParam("Amount"), saRoot) : 1;
            final ManaCostBeingPaid testCost = new ManaCostBeingPaid(cost.toString().replace("X ", ""));
            final String[] comboColors = abMana.getComboColors().split(" ");
            for (int nMana = 1; nMana <= amount; nMana++) {
                String choice = "";
                // Use expressChoice first
                if (!abMana.getExpressChoice().isEmpty()) {
                    choice = abMana.getExpressChoice();
                    abMana.clearExpressChoice();
                    if (abMana.canProduce(choice) && testCost.isNeeded(choice)) {
                        choiceString.append(choice);
                        testCost.payMultipleMana(choice);
                        continue;
                    }
                }
                // check colors needed for cost
                if (!testCost.isPaid()) {
                    // Loop over combo colors
                    for (String color :  comboColors) {
                        if (testCost.isNeeded(color)) {
                            testCost.payMultipleMana(color);
                            if (nMana != 1) {
                                choiceString.append(" ");
                            }
                            choiceString.append(color);
                            choice = color;
                            break;
                        }
                    }
                    if (!choice.isEmpty()) {
                        continue;
                    }
                }
                // check if combo mana can produce most common color in hand
                String commonColor = CardFactoryUtil.getMostProminentColor(ai.getCardsIn(
                        ZoneType.Hand));
                if (!commonColor.isEmpty() && abMana.getComboColors().contains(InputPayManaCostUtil.getShortColorString(commonColor))) {
                    choice = InputPayManaCostUtil.getShortColorString(commonColor);
                }
                else {
                    // default to first color
                    choice = comboColors[0];
                }
                if (nMana != 1) {
                    choiceString.append(" ");
                }
                choiceString.append(choice);
            }
        }
        if (choiceString.toString().isEmpty()) {
            choiceString.append("0");
        }
        return choiceString.toString();
    }

}
