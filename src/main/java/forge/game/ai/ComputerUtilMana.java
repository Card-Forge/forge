package forge.game.ai;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import com.google.common.base.Predicate;
import forge.Card;
import forge.CardLists;
import forge.CardUtil;
import forge.Constant;
import forge.FThreads;
import forge.card.MagicColor;
import forge.card.ability.AbilityUtils;
import forge.card.ability.ApiType;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.cost.Cost;
import forge.card.cost.CostPayment;
import forge.card.mana.ManaAtom;
import forge.card.mana.ManaCost;
import forge.card.mana.ManaCostBeingPaid;
import forge.card.mana.ManaCostShard;
import forge.card.mana.ManaPool;
import forge.card.spellability.Ability;
import forge.card.spellability.AbilityManaPart;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.SpellAbility;
import forge.game.GameActionUtil;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.util.maps.CollectionSuppliers;
import forge.util.maps.EnumMapOfLists;
import forge.util.maps.MapOfLists;
import forge.util.maps.TreeMapOfLists;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class ComputerUtilMana {

    private final static boolean DEBUG_MANA_PAYMENT = false; 
    
    public static boolean canPayManaCost(final SpellAbility sa, final Player ai, final int extraMana) {
        return payManaCost(sa, ai, true, extraMana, true);
    }

    // Does not check if mana sources can be used right now, just checks for potential chance.
    public static boolean hasEnoughManaSourcesToCast(final SpellAbility sa, final Player ai) {
        return payManaCost(sa, ai, true, 0, false);
    }    
    
    public static boolean payManaCost(final Player ai, final SpellAbility sa) {
        return payManaCost(sa, ai, false, 0, true);
    }

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
    private static boolean payManaCost(final SpellAbility sa, final Player ai, final boolean test, final int extraMana, boolean checkPlayable) {
        ManaCostBeingPaid cost = ComputerUtilMana.calculateManaCost(sa, test, extraMana);
        
        final Card card = sa.getSourceCard();

        adjustManaCostToAvoidNegEffects(cost, card);

        final ManaPool manapool = ai.getManaPool();
        List<ManaCostShard> unpaidShards = cost.getUnpaidShards();
        Collections.sort(unpaidShards); // most difficult shards must come first
        for(ManaCostShard part : unpaidShards) {
            if( part != ManaCostShard.X)
                manapool.payManaFromPool(sa, cost, part);
        }

        if (cost.isPaid()) {
            // refund any mana taken from mana pool when test
            manapool.clearManaPaid(sa, test);
            handleOfferingsAI(sa, test, cost.isPaid());
            return true;
        }

        // arrange all mana abilities by color produced.
        final MapOfLists<Integer, SpellAbility> manaAbilityMap = ComputerUtilMana.groupSourcesByManaColor(ai, checkPlayable);
        if ( manaAbilityMap.isEmpty() ) {
            manapool.clearManaPaid(sa, test);
            handleOfferingsAI(sa, test, cost.isPaid());
            return false;
        }
        
        // select which abilities may be used for each shard
        MapOfLists<ManaCostShard, SpellAbility> sourcesForShards = ComputerUtilMana.groupAndOrderToPayShards(ai, manaAbilityMap, cost);
        
        // Loop over mana needed
        
        if ( DEBUG_MANA_PAYMENT ) {
            System.out.println((test ? "test -- " : "PROD -- ") + FThreads.debugGetStackTraceItem(5, true));
            for(Entry<ManaCostShard, Collection<SpellAbility>> src : sourcesForShards.entrySet()) {
                System.out.println("\t" +src.getKey() + " : " + src.getValue().size() + " source(s)");
                for(SpellAbility sss : src.getValue()) {
                    System.out.printf("\t\t%s - %s%n", sss.getSourceCard(), sss);
                }
            }
        }
        
        List<String> paymentPlan = new ArrayList<String>();

        String originalCost = cost.toString(false);
        ManaCostShard toPay = null;
        while (!cost.isPaid()) {
            toPay = getNextShardToPay(cost, sourcesForShards);

            Collection<SpellAbility> saList = sourcesForShards.get(toPay);
            SpellAbility saPayment = null;
            if( saList != null  ) { 
                for (final SpellAbility ma : saList) { 
                    if(ma.getSourceCard() == sa.getSourceCard())
                        continue;
                    if( canPayShardWithSpellAbility(toPay, ai, ma, sa, checkPlayable || !test ) ) {
                        saPayment = ma;
                        break;
                    }
                }
            }
            
            if( DEBUG_MANA_PAYMENT )
                paymentPlan.add(String.format("%s : (%s) %s", toPay, saPayment == null ? "LIFE" : saPayment.getSourceCard(), saPayment));
            
            if( saPayment == null ) {
                if(!toPay.isPhyrexian() || !ai.canPayLife(2))
                    break; // cannot pay
                
                cost.payPhyrexian();
                if( !test )
                    ai.payLife(2, sa.getSourceCard());
                continue;
            } 

            setExpressColorChoice(sa, ai, cost, toPay, saPayment);

            if ( test ) {
                String manaProduced = toPay.isSnow() ? "S" : GameActionUtil.generatedMana(saPayment);
                //System.out.println(manaProduced);
                /* String remainder = */ cost.payMultipleMana(manaProduced);
                // add it to mana pool?
                
                // remove from available lists
                for(Collection<SpellAbility> kv : sourcesForShards.values()) {
                    Iterator<SpellAbility> itSa = kv.iterator();
                    while(itSa.hasNext()) {
                        SpellAbility srcSa = itSa.next();
                        if( srcSa.getSourceCard().equals(saPayment.getSourceCard()) )
                            itSa.remove();
                    }
                }
            } else {
                if (saPayment.getPayCosts() != null) {
                    final CostPayment pay = new CostPayment(saPayment.getPayCosts(), saPayment);
                    if (!pay.payComputerCosts(ai, ai.getGame())) {
                        continue;
                    }
                } else {
                    System.err.println("Ability " + saPayment + " from " + saPayment.getSourceCard() + "  had NULL as payCost");
                    saPayment.getSourceCard().tap();
                }

                AbilityUtils.resolve(saPayment, false);
                // subtract mana from mana pool
                manapool.payManaFromAbility(sa, cost, saPayment);

                // no need to remove abilities from resource map, 
                // once their costs are paid and consume resources, they can not be used again
            }
        }

        manapool.clearManaPaid(sa, test);
        handleOfferingsAI(sa, test, cost.isPaid());

        if( DEBUG_MANA_PAYMENT )
            System.err.printf("%s > [%s] payment has %s (%s +%d) for (%s) %s:%n\t%s%n%n", FThreads.debugGetCurrThreadId(), test ? "test" : "PROD", cost.isPaid() ? "*PAID*" : "failed", originalCost, extraMana, sa.getSourceCard(), sa.toUnsuppressedString(), StringUtils.join(paymentPlan, "\n\t") );
        
        if(!cost.isPaid()) {
            if( test )
                return false;
            else 
                throw new RuntimeException("ComputerUtil : payManaCost() cost was not paid for " + sa.getSourceCard().getName() + ". Didn't find what to pay for " + toPay);
        }
                
        
        // if (sa instanceof Spell_Permanent) // should probably add this
        sa.getSourceCard().setColorsPaid(cost.getColorsPaid());
        sa.getSourceCard().setSunburstValue(cost.getSunburst());
        return true;
    } // payManaCost()


    private static void setExpressColorChoice(final SpellAbility sa, final Player ai, ManaCostBeingPaid cost,
            ManaCostShard toPay, SpellAbility saPayment) {
        
        AbilityManaPart m = saPayment.getManaPart();
        if ( m.isComboMana() )
            getComboManaChoice(ai, saPayment, sa, cost);
        else if (saPayment.getApi() == ApiType.ManaReflected) {
            System.out.println("Evaluate reflected mana of: " + saPayment.getSourceCard());
            Set<String> reflected = CardUtil.getReflectableManaColors(saPayment);

            for(byte c : MagicColor.WUBRG) {
                if (toPay.canBePaidWithManaOfColor(c) && reflected.contains(MagicColor.toLongString(c))) {
                    m.setExpressChoice(MagicColor.toShortString(c));
                    return;
                }
            }
        } else if ( m.isAnyMana()) {
            byte colorChoice = 0;
            if (toPay.isOr2Colorless())
                colorChoice = toPay.getColorMask();
            else for( byte c : MagicColor.WUBRG ) {
                if ( toPay.canBePaidWithManaOfColor(c)) {
                    colorChoice = c;
                    break;
                }
            }
            m.setExpressChoice(MagicColor.toShortString(colorChoice));
        }
    }

    private static boolean canPayShardWithSpellAbility(ManaCostShard toPay, Player ai, SpellAbility ma, SpellAbility sa, boolean checkCosts) {
        final Card sourceCard = ma.getSourceCard();
        
        if (toPay.isSnow() && !sourceCard.isSnow() ) return false;

        AbilityManaPart m = ma.getManaPart();
        if (!m.meetsManaRestrictions(sa)) {
            return false;
        }

        if ( checkCosts ) {
            // Check if AI can still play this mana ability
            ma.setActivatingPlayer(ai);
            if (ma.getPayCosts() != null ) { // if the AI can't pay the additional costs skip the mana ability
                if (!CostPayment.canPayAdditionalCosts(ma.getPayCosts(), ma)) {
                    return false;
                }
            } else if (sourceCard.isTapped() ) {
                return false;
            }
        }

        if (m.isComboMana()) {
            for(String s : m.getComboColors().split(" ")) {
                if ( "Any".equals(s) || toPay.canBePaidWithManaOfColor(MagicColor.fromName(s)))
                    return true;
            }
            return false;

        } else if (ma.getApi() == ApiType.ManaReflected) {
            Set<String> reflected = CardUtil.getReflectableManaColors(ma);

            for(byte c : MagicColor.WUBRG) {
                if (toPay.canBePaidWithManaOfColor(c) && reflected.contains(MagicColor.toLongString(c))) {
                    m.setExpressChoice(MagicColor.toShortString(c));
                    return true;
                }
            }
            return false;
        }
        return true;
    }


    private static ManaCostShard getNextShardToPay(ManaCostBeingPaid cost, Map<ManaCostShard, Collection<SpellAbility>> sourcesForShards) {
        // mind the priorities
        // * Pay mono-colored first,
        // * Pay 2/C with matching colors
        // * pay hybrids 
        // * pay phyrexian, keep mana for colorless
        // * pay colorless
        
        for(ManaCostShard s : cost.getDistinctShards()) { // should check in which order EnumMap enumerates keys. If it's same as enum member declaration, nothing else needs to be done.
            return s; 
        }
        return null;
    }

    private static void adjustManaCostToAvoidNegEffects(ManaCostBeingPaid cost, final Card card) {
        // Make mana needed to avoid negative effect a mandatory cost for the AI
        for (String manaPart : card.getSVar("ManaNeededToAvoidNegativeEffect").split(",")) {
            // convert long color strings to short color strings
            byte mask = MagicColor.fromName(manaPart);

            // make mana mandatory for AI
            if (!cost.needsColor(mask) && cost.getColorlessManaAmount() > 0) {
                ManaCostShard shard = ManaCostShard.valueOf(mask);
                cost.increaseShard(shard, 1);
                cost.decreaseColorlessMana(1);
            }
        }
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
    private static void getComboManaChoice(final Player ai, final SpellAbility manaAb, final SpellAbility saRoot, final ManaCostBeingPaid cost) {
    
        final StringBuilder choiceString = new StringBuilder();
        final Card source = manaAb.getSourceCard();
        final AbilityManaPart abMana = manaAb.getManaPart();
    
        if (abMana.isComboMana()) {
            int amount = manaAb.hasParam("Amount") ? AbilityUtils.calculateAmount(source, manaAb.getParam("Amount"), saRoot) : 1;
            final ManaCostBeingPaid testCost = new ManaCostBeingPaid(cost.toString().replace("X ", ""));
            final String[] comboColors = abMana.getComboColors().split(" ");
            for (int nMana = 1; nMana <= amount; nMana++) {
                String choice = "";
                // Use expressChoice first
                if (!abMana.getExpressChoice().isEmpty()) {
                    choice = abMana.getExpressChoice();
                    abMana.clearExpressChoice();
                    byte colorMask = MagicColor.fromName(choice);
                    if (abMana.canProduce(choice) && testCost.isAnyPartPayableWith(colorMask)) {
                        choiceString.append(choice);
                        testCost.payMultipleMana(choice);
                        continue;
                    }
                }
                // check colors needed for cost
                if (!testCost.isPaid()) {
                    // Loop over combo colors
                    for (String color : comboColors) {
                        if (testCost.isAnyPartPayableWith(MagicColor.fromName(color))) {
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
                String commonColor = ComputerUtilCard.getMostProminentColor(ai.getCardsIn(
                        ZoneType.Hand));
                if (!commonColor.isEmpty() && abMana.getComboColors().contains(MagicColor.toShortString(commonColor))) {
                    choice = MagicColor.toShortString(commonColor);
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

        abMana.setExpressChoice(choiceString.toString());
    }


    // TODO: this code is disconnected now, it was moved here from MagicStack, where X cost is not processed any more
    public static void computerPayX(final SpellAbility sa, Player player, int xCost) {
        final int neededDamage = CardFactoryUtil.getNeededXDamage(sa);
        final Ability ability = new Ability(sa.getSourceCard(), ManaCost.get(xCost)) {
            @Override
            public void resolve() {
                sa.getSourceCard().addXManaCostPaid(1);
            }
        };
        
        while (ComputerUtilCost.canPayCost(ability, player) && (neededDamage != sa.getSourceCard().getXManaCostPaid())) {
            ComputerUtil.playNoStack(player, ability, player.getGame());
        }

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
    private static MapOfLists<ManaCostShard, SpellAbility> groupAndOrderToPayShards(final Player ai, final MapOfLists<Integer, SpellAbility> manaAbilityMap, final ManaCostBeingPaid cost) {
        MapOfLists<ManaCostShard, SpellAbility> res = new EnumMapOfLists<ManaCostShard, SpellAbility>(ManaCostShard.class, CollectionSuppliers.<SpellAbility>hashSets());

        // loop over cost parts
        for (ManaCostShard shard : cost.getDistinctShards() ) {
            if ( shard == ManaCostShard.S ) {
                res.put(shard, manaAbilityMap.get(ManaAtom.IS_SNOW));
                continue;
            }

            if (shard.isOr2Colorless()) {
                Integer colorKey = Integer.valueOf(shard.getColorMask());
                if (manaAbilityMap.containsKey(colorKey) )
                    res.addAll(shard, manaAbilityMap.get(colorKey));
                if (manaAbilityMap.containsKey(ManaAtom.COLORLESS) )
                    res.addAll(shard, manaAbilityMap.get(ManaAtom.COLORLESS));
                continue;
            }

            for(Entry<Integer, Collection<SpellAbility>> kv : manaAbilityMap.entrySet()) {
                if( shard.canBePaidWithManaOfColor(kv.getKey().byteValue()) )
                    res.addAll(shard, kv.getValue());
            }
        }

        if (cost.getColorlessManaAmount() > 0 && manaAbilityMap.containsKey(ManaAtom.COLORLESS))
            res.addAll(ManaCostShard.COLORLESS, manaAbilityMap.get(ManaAtom.COLORLESS));

        return res;
    }

    /**
     * Calculate the ManaCost for the given SpellAbility.
     * @param sa
     * @param test
     * @param extraMana
     * @return ManaCost
     */
    private static ManaCostBeingPaid calculateManaCost(final SpellAbility sa, final boolean test, final int extraMana) {
        final ManaCost mana = sa.getPayCosts() != null ? sa.getPayCosts().getTotalMana() : ManaCost.NO_COST;
    
        ManaCostBeingPaid cost = new ManaCostBeingPaid(mana);
        cost.applySpellCostChange(sa);
    
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
                        manaToAdd = AbilityUtils.calculateAmount(card, xSvar, sa) * cost.getXcounter();
                    }
                }
            }
    
            String manaXColor = sa.getParam("XColor");
            ManaCostShard shardToGrow = ManaCostShard.parseNonGeneric(manaXColor == null ? "1" : manaXColor);
            cost.increaseShard(shardToGrow, manaToAdd);
            
            if (!test) {
                card.setXManaCostPaid(manaToAdd / cost.getXcounter());
            }
        }
    
        return cost;
    }

    //This method is currently used by AI to estimate available mana
    public static List<Card> getAvailableMana(final Player ai, final boolean checkPlayable) {
        final List<Card> list = ai.getCardsIn(ZoneType.Battlefield);
        list.addAll(ai.getCardsIn(ZoneType.Hand));
        final List<Card> manaSources = CardLists.filter(list, new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                for (final SpellAbility am : getAIPlayableMana(c)) {
                    am.setActivatingPlayer(ai);
                    if (!checkPlayable || am.canPlay()) {
                        return true;
                    }
                }
                return false;
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
        for (Card card : manaSources) {
    
            if (card.isCreature() || card.isEnchanted()) {
                otherManaSources.add(card);
                continue; // don't use creatures before other permanents
            }
    
            int usableManaAbilities = 0;
            boolean needsLimitedResources = false;
            boolean producesAnyColor = false;
            final ArrayList<SpellAbility> manaAbilities = getAIPlayableMana(card);
    
            for (final SpellAbility m : manaAbilities) {
    
                if (m.getManaPart().isAnyMana()) {
                    producesAnyColor = true;
                }
    
                final Cost cost = m.getPayCosts();
                if (cost != null) {
                    needsLimitedResources |= !cost.isReusuableResource();
                }
    
                // if the AI can't pay the additional costs skip the mana ability
                if (cost != null) {
                    m.setActivatingPlayer(ai);
                    if (!CostPayment.canPayAdditionalCosts(m.getPayCosts(), m)) {
                        continue;
                    }
                }
    
                // don't use abilities with dangerous drawbacks
                AbilitySub sub = m.getSubAbility(); 
                if (sub != null && !card.getName().equals("Pristine Talisman") && !card.getName().equals("Zhur-Taa Druid")) {
                    if (!sub.getAi().chkDrawbackWithSubs(ai, sub)) {
                        continue;
                    }
                    needsLimitedResources = true; // TODO: check for good drawbacks (gainLife)
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

    
    //This method is currently used by AI to estimate mana available
    private static MapOfLists<Integer, SpellAbility> groupSourcesByManaColor(final Player ai, boolean checkPlayable) {
        final MapOfLists<Integer, SpellAbility> manaMap = new TreeMapOfLists<Integer, SpellAbility>(CollectionSuppliers.<SpellAbility>arrayLists());

        // Loop over all current available mana sources
        for (final Card sourceCard : getAvailableMana(ai, checkPlayable)) {
            for (final SpellAbility m : getAIPlayableMana(sourceCard)) {
                m.setActivatingPlayer(ai);
                if (checkPlayable && !m.canPlay()) {
                    continue;
                }
    
                // don't use abilities with dangerous drawbacks
                AbilitySub sub = m.getSubAbility(); 
                if (sub != null) {
                    if (!sub.getAi().chkDrawbackWithSubs(ai, sub)) {
                        continue;
                    }
                }
                
                manaMap.add(ManaAtom.COLORLESS, m); // add to colorless source list
                AbilityManaPart mp = m.getManaPart();

                Set<String> reflectedColors = CardUtil.getReflectableManaColors(m);
                // find possible colors
                if (mp.canProduce("W") || reflectedColors.contains(Constant.Color.WHITE)) {
                    manaMap.add(ManaAtom.WHITE, m);
                }
                if (mp.canProduce("U") || reflectedColors.contains(Constant.Color.BLUE)) {
                    manaMap.add(ManaAtom.BLUE, m);
                }
                if (mp.canProduce("B") || reflectedColors.contains(Constant.Color.BLACK)) {
                    manaMap.add(ManaAtom.BLACK, m);
                }
                if (mp.canProduce("R") || reflectedColors.contains(Constant.Color.RED)) {
                    manaMap.add(ManaAtom.RED, m);
                }
                if (mp.canProduce("G") || reflectedColors.contains(Constant.Color.GREEN)) {
                    manaMap.add(ManaAtom.GREEN, m);
                }
                if (mp.isSnow()) {
                    manaMap.add(ManaAtom.IS_SNOW, m);
                }
            } // end of mana abilities loop
        } // end of mana sources loop

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
        for (int i = 1; i < 100; i++)
            if (!canPayManaCost(sa, player, i))
                return i - 1;

        return 99;
    }

    // Returns basic mana abilities plus "reflected mana" abilities
    /**
     * <p>
     * getAIPlayableMana.
     * </p>
     * 
     * @return a {@link java.util.ArrayList} object.
     */
    private static final ArrayList<SpellAbility> getAIPlayableMana(Card c) {
        final ArrayList<SpellAbility> res = new ArrayList<SpellAbility>();
        for (final SpellAbility a : c.getManaAbility()) {
    
            // if a mana ability has a mana cost the AI will miscalculate
            // if there is a parent ability the AI can't use it
            final Cost cost = a.getPayCosts();
            if (!cost.hasNoManaCost() || (a.getApi() != ApiType.Mana && a.getApi() != ApiType.ManaReflected)) {
                continue;
            }
    
            if (!res.contains(a)) {
                res.add(a);
            }
    
        }
        return res;
    }
    private static void handleOfferingsAI(final SpellAbility sa, boolean test, boolean costIsPaid) {
        if (sa.isOffering() && sa.getSacrificedAsOffering() != null) {
            final Card offering = sa.getSacrificedAsOffering();
            offering.setUsedToPay(false);
            if (costIsPaid && !test) {
                sa.getSourceCard().getController().getGame().getAction().sacrifice(offering, sa);
            }
            sa.resetSacrificedAsOffering();
        }
    }

}
