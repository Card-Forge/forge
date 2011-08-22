package forge;

import forge.card.abilityFactory.AbilityFactory;
import forge.card.cardFactory.CardFactoryUtil;
import forge.card.cost.Cost;
import forge.card.cost.CostUtil;
import forge.card.cost.Cost_Payment;
import forge.card.mana.ManaCost;
import forge.card.mana.ManaPool;
import forge.card.spellability.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;

import static forge.error.ErrorViewer.showError;


/**
 * <p>ComputerUtil class.</p>
 *
 * @author Forge
 * @version $Id$
 */
public class ComputerUtil {

    //if return true, go to next phase
    /**
     * <p>playCards.</p>
     *
     * @return a boolean.
     */
    static public boolean playCards() {
        return playCards(getSpellAbility());
    }

    //if return true, go to next phase
    /**
     * <p>playCards.</p>
     *
     * @param all an array of {@link forge.card.spellability.SpellAbility} objects.
     * @return a boolean.
     */
    static public boolean playCards(SpellAbility[] all) {
        //not sure "playing biggest spell" matters?
        sortSpellAbilityByCost(all);
        //    MyRandom.shuffle(all);

        for (SpellAbility sa : all) {
            // Don't add Counterspells to the "normal" playcard lookupss
            AbilityFactory af = sa.getAbilityFactory();
            if (af != null && af.getAPI().equals("Counter"))
                continue;

            sa.setActivatingPlayer(AllZone.getComputerPlayer());
            if (canBePlayedAndPayedByAI(sa)) //checks everything nescessary
            {
                handlePlayingSpellAbility(sa);

                return false;
            }
        }
        return true;
    }//playCards()

    /**
     * <p>playCards.</p>
     *
     * @param all a {@link java.util.ArrayList} object.
     * @return a boolean.
     */
    static public boolean playCards(ArrayList<SpellAbility> all) {
        SpellAbility[] sas = new SpellAbility[all.size()];
        for (int i = 0; i < sas.length; i++) {
            sas[i] = all.get(i);
        }
        return playCards(sas);
    }//playCards()

    /**
     * <p>handlePlayingSpellAbility.</p>
     *
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     */
    static public void handlePlayingSpellAbility(SpellAbility sa) {
        AllZone.getStack().freezeStack();
        Card source = sa.getSourceCard();

        if (sa.isSpell() && !source.isCopiedSpell())
            AllZone.getGameAction().moveToStack(source);

        Cost cost = sa.getPayCosts();
        Target tgt = sa.getTarget();

        if (cost == null) {
            payManaCost(sa);
            sa.chooseTargetAI();
            sa.getBeforePayManaAI().execute();
            AllZone.getStack().addAndUnfreeze(sa);
        } else {
            if (tgt != null && tgt.doesTarget())
                sa.chooseTargetAI();

            Cost_Payment pay = new Cost_Payment(cost, sa);
            if (pay.payComputerCosts())
                AllZone.getStack().addAndUnfreeze(sa);
        }
    }

    /**
     * <p>counterSpellRestriction.</p>
     *
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @return a int.
     */
    static public int counterSpellRestriction(SpellAbility sa) {
        // Move this to AF?
        // Restriction Level is Based off a handful of factors

        int restrict = 0;

        Card source = sa.getSourceCard();
        Target tgt = sa.getTarget();
        AbilityFactory af = sa.getAbilityFactory();
        HashMap<String, String> params = af.getMapParams();

        // Play higher costing spells first?
        Cost cost = sa.getPayCosts();
        // Convert cost to CMC
        //String totalMana = source.getSVar("PayX"); // + cost.getCMC()

        // Consider the costs here for relative "scoring"
        if (CostUtil.hasDiscardHandCost(cost)) {
            // Null Brooch aid
            restrict -= (AllZoneUtil.getPlayerHand(AllZone.getComputerPlayer()).size() * 20);
        }

        // Abilities before Spells (card advantage)
        if (af.isAbility())
            restrict += 40;

        // TargetValidTargeting gets biggest bonus
        if (tgt.getSAValidTargeting() != null) {
            restrict += 35;
        }

        // Unless Cost gets significant bonus + 10-Payment Amount
        String unless = params.get("UnlessCost");
        if (unless != null) {
            int amount = AbilityFactory.calculateAmount(source, unless, sa);

            int usableManaSources = CardFactoryUtil.getUsableManaSources(AllZone.getHumanPlayer());

            // If the Unless isn't enough, this should be less likely to be used
            if (amount > usableManaSources)
                restrict += 20 - (2 * amount);
            else
                restrict -= (10 - (2 * amount));
        }

        // Then base on Targeting Restriction
        String[] validTgts = tgt.getValidTgts();
        if (validTgts.length != 1 || !validTgts[0].equals("Card"))
            restrict += 10;

        // And lastly give some bonus points to least restrictive TargetType (Spell,Ability,Triggered)
        String tgtType = tgt.getTargetSpellAbilityType();
        restrict -= (5 * tgtType.split(",").length);

        return restrict;
    }

    //if return true, go to next phase
    /**
     * <p>playCounterSpell.</p>
     *
     * @param possibleCounters a {@link java.util.ArrayList} object.
     * @return a boolean.
     */
    static public boolean playCounterSpell(ArrayList<SpellAbility> possibleCounters) {
        SpellAbility bestSA = null;
        int bestRestriction = Integer.MIN_VALUE;

        for (SpellAbility sa : possibleCounters) {
            sa.setActivatingPlayer(AllZone.getComputerPlayer());
            if (canBePlayedAndPayedByAI(sa)) { //checks everything nescessary
                if (bestSA == null) {
                    bestSA = sa;
                    bestRestriction = counterSpellRestriction(sa);
                } else {
                    // Compare bestSA with this SA
                    int restrictionLevel = counterSpellRestriction(sa);

                    if (restrictionLevel > bestRestriction) {
                        bestRestriction = restrictionLevel;
                        bestSA = sa;
                    }
                }
            }
        }

        if (bestSA == null)
            return false;

        // TODO
        // "Look" at Targeted SA and "calculate" the threshold
        // if (bestRestriction < targetedThreshold) return false;

        AllZone.getStack().freezeStack();
        Card source = bestSA.getSourceCard();

        if (bestSA.isSpell() && !source.isCopiedSpell())
            AllZone.getGameAction().moveToStack(source);

        Cost cost = bestSA.getPayCosts();

        if (cost == null) {
            // Honestly Counterspells shouldn't use this branch
            payManaCost(bestSA);
            bestSA.chooseTargetAI();
            bestSA.getBeforePayManaAI().execute();
            AllZone.getStack().addAndUnfreeze(bestSA);
        } else {
            Cost_Payment pay = new Cost_Payment(cost, bestSA);
            if (pay.payComputerCosts())
                AllZone.getStack().addAndUnfreeze(bestSA);
        }

        return true;
    }//playCounterSpell()


    //this is used for AI's counterspells
    /**
     * <p>playStack.</p>
     *
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     */
    final static public void playStack(SpellAbility sa) {
        if (canPayCost(sa)) {
            Card source = sa.getSourceCard();
            if (sa.isSpell() && !source.isCopiedSpell())
                AllZone.getGameAction().moveToStack(source);

            sa.setActivatingPlayer(AllZone.getComputerPlayer());

            payManaCost(sa);

            AllZone.getStack().add(sa);
        }
    }

    /**
     * <p>playStackFree.</p>
     *
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     */
    final static public void playStackFree(SpellAbility sa) {
        sa.setActivatingPlayer(AllZone.getComputerPlayer());

        Card source = sa.getSourceCard();
        if (sa.isSpell() && !source.isCopiedSpell())
            AllZone.getGameAction().moveToStack(source);

        AllZone.getStack().add(sa);
    }

    /**
     * <p>playNoStack.</p>
     *
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     */
    final static public void playNoStack(SpellAbility sa) {
        // TODO: We should really restrict what doesn't use the Stack

        if (canPayCost(sa)) {
            Card source = sa.getSourceCard();
            if (sa.isSpell() && !source.isCopiedSpell())
                AllZone.getGameAction().moveToStack(source);

            sa.setActivatingPlayer(AllZone.getComputerPlayer());
            
            Cost cost = sa.getPayCosts();
            if (cost == null) 
                payManaCost(sa);
            else {
            	Cost_Payment pay = new Cost_Payment(cost, sa);
            	pay.payComputerCosts();
            }

            AbilityFactory.resolve(sa, false);

            //destroys creatures if they have lethal damage, etc..
            AllZone.getGameAction().checkStateEffects();
        }
    }//play()


    //gets Spells of cards in hand and Abilities of cards in play
    //checks to see
    //1. if canPlay() returns true, 2. can pay for mana
    /**
     * <p>getSpellAbility.</p>
     *
     * @return an array of {@link forge.card.spellability.SpellAbility} objects.
     */
    static public SpellAbility[] getSpellAbility() {
        CardList all = new CardList();
        all.addAll(AllZoneUtil.getPlayerCardsInPlay(AllZone.getComputerPlayer()));
        all.addAll(AllZoneUtil.getPlayerHand(AllZone.getComputerPlayer()));
        all.addAll(CardFactoryUtil.getExternalZoneActivationCards(AllZone.getComputerPlayer()));

        CardList humanPlayable = new CardList();
        humanPlayable.addAll(AllZoneUtil.getPlayerCardsInPlay(AllZone.getHumanPlayer()));
        humanPlayable = humanPlayable.filter(new CardListFilter() {
            public boolean addCard(Card c) {
                return (c.canAnyPlayerActivate());
            }
        });

        all.addAll(humanPlayable);

        ArrayList<SpellAbility> spellAbility = new ArrayList<SpellAbility>();
        for (int outer = 0; outer < all.size(); outer++) {
            SpellAbility[] sa = all.get(outer).getSpellAbility();
            for (int i = 0; i < sa.length; i++)
                spellAbility.add(sa[i]);//this seems like it needs to be copied, not sure though
        }

        SpellAbility[] sa = new SpellAbility[spellAbility.size()];
        spellAbility.toArray(sa);
        return sa;
    }

    //This is for playing spells regularly (no Cascade/Ripple etc.)
    /**
     * <p>canBePlayedAndPayedByAI.</p>
     *
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     * @since 1.0.15
     */
    static public boolean canBePlayedAndPayedByAI(SpellAbility sa) {
        return sa.canPlayAI() && sa.canPlay() && canPayCost(sa);
    }

    /**
     * <p>canPayCost.</p>
     *
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    static public boolean canPayCost(SpellAbility sa) {
        return canPayCost(sa, AllZone.getComputerPlayer());
    }//canPayCost()


    /**
     * <p>canPayCost.</p>
     *
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @param player a {@link forge.Player} object.
     * @return a boolean.
     */
    static public boolean canPayCost(SpellAbility sa, Player player) {
        if (!payManaCost(sa, player, true, 0))
            return false;

        return canPayAdditionalCosts(sa, player);
    }//canPayCost()


    /**
     * <p>determineLeftoverMana.</p>
     *
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @return a int.
     */
    static public int determineLeftoverMana(SpellAbility sa) {
        return determineLeftoverMana(sa, AllZone.getComputerPlayer());
    }

    /**
     * <p>determineLeftoverMana.</p>
     *
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @param player a {@link forge.Player} object.
     * @return a int.
     * @since 1.0.15
     */
    static public int determineLeftoverMana(SpellAbility sa, Player player) {

        int xMana = 0;

        for (int i = 1; i < 99; i++) {
            if (!payManaCost(sa, player, true, xMana))
                break;
            xMana = i;
        }

        return xMana;
    }


    /**
     * <p>canPayAdditionalCosts.</p>
     *
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    static public boolean canPayAdditionalCosts(SpellAbility sa) {
        return canPayAdditionalCosts(sa, AllZone.getComputerPlayer());
    }

    /**
     * <p>canPayAdditionalCosts.</p>
     *
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @param player a {@link forge.Player} object.
     * @return a boolean.
     */
    static public boolean canPayAdditionalCosts(SpellAbility sa, Player player) {
        if (sa.getActivatingPlayer() == null){
            System.out.println(sa.getSourceCard() + " in ComputerUtil.canPayAdditionalCosts() without an activating player");
            sa.setActivatingPlayer(player);
        }
        return Cost_Payment.canPayAdditionalCosts(sa.getPayCosts(), sa);
    }

    /**
     * <p>payManaCost.</p>
     *
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     */
    static public void payManaCost(SpellAbility sa) {
        payManaCost(sa, AllZone.getComputerPlayer(), false, 0);
    }

    /**
     * <p>payManaCost.</p>
     *
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @param player a {@link forge.Player} object.
     * @param test (is for canPayCost, if true does not change the game state)
     * @param extraMana a int.
     * @return a boolean.
     * @since 1.0.15
     */
    static public boolean payManaCost(SpellAbility sa, Player player, boolean test, int extraMana) {
        String mana = sa.getPayCosts() != null ? sa.getPayCosts().getTotalMana() : sa.getManaCost();

        ManaCost cost = new ManaCost(mana);

        cost = AllZone.getGameAction().getSpellCostChange(sa, cost);

        ManaPool manapool = AllZone.getComputerManaPool();
        if (player.isHuman()) manapool = AllZone.getManaPool();

        Card card = sa.getSourceCard();
        // Tack xMana Payments into mana here if X is a set value
        if (sa.getPayCosts() != null && cost.getXcounter() > 0) {

            int manaToAdd = 0;
            if (test && extraMana > 0)
                manaToAdd = extraMana * cost.getXcounter();
            else {
                // For Count$xPaid set PayX in the AFs then use that here
                // Else calculate it as appropriate.
                String xSvar = card.getSVar("X").equals("Count$xPaid") ? "PayX" : "X";
                if (!card.getSVar(xSvar).equals("")) {
                    if (xSvar.equals("PayX"))
                        manaToAdd = Integer.parseInt(card.getSVar(xSvar)) * cost.getXcounter(); // X has already been decided
                    else {
                        manaToAdd = AbilityFactory.calculateAmount(card, xSvar, sa) * cost.getXcounter();
                    }
                }
            }

            cost.increaseColorlessMana(manaToAdd);
            if (!test)
                card.setXManaCostPaid(manaToAdd);
        }

        if (cost.isPaid())
            return true;

        ArrayList<String> colors;

        cost = ((ManaPool) manapool).subtractMana(sa, cost);

        CardList manaSources = getAvailableMana();

        //this is to prevent errors for mana sources that have abilities that cost mana.
        manaSources.remove(sa.getSourceCard());

        for (int i = 0; i < manaSources.size(); i++) {
            Card sourceCard = manaSources.get(i);
            ArrayList<Ability_Mana> manaAbilities = sourceCard.getAIPlayableMana();

            boolean used = false; //this is for testing paying mana only

            manaAbilities = sortForNeeded(cost, manaAbilities, player);

            for (Ability_Mana m : manaAbilities) {

                if (used) break; //mana source already used in the test
                m.setActivatingPlayer(player);
                //if the AI can't pay the additional costs skip the mana ability
                if (m.getPayCosts() != null) {
                    if (!canPayAdditionalCosts(m, player))
                        continue;
                } else if (sourceCard.isTapped())
                    continue;

                //don't use abilities with dangerous drawbacks
                if (m.getSubAbility() != null)
                    if (!m.getSubAbility().chkAI_Drawback())
                        continue;

                colors = getProduceableColors(m, player);
                for (int j = 0; j < colors.size(); j++) {
                    if (used) break; //mana source already used in the test

                    if (cost.isNeeded(colors.get(j))) {
                        if (!test) {
                            //Pay additional costs
                            if (m.getPayCosts() != null) {
                                Cost_Payment pay = new Cost_Payment(m.getPayCosts(), m);
                                if (!pay.payComputerCosts()) continue;
                            } else
                                sourceCard.tap();
                        } else used = true; // mana source is now used in the test

                        cost.payMana(colors.get(j));

                        if (!test) {
                            //resolve subabilities
                            AbilityFactory af = m.getAbilityFactory();
                            if (af != null)
                                AbilityFactory.resolveSubAbilities(m);

                            if (sourceCard.getName().equals("Undiscovered Paradise")) {
                                sourceCard.setBounceAtUntap(true);
                            }

                            if (sourceCard.getName().equals("Rainbow Vale")) {
                                sourceCard.addExtrinsicKeyword("An opponent gains control of CARDNAME at the beginning of the next end step.");
                            }

                            //System.out.println("just subtracted " + colors.get(j) + ", cost is now: " + cost.toString());
                            //Run triggers
                            HashMap<String, Object> runParams = new HashMap<String, Object>();

                            runParams.put("Card", sourceCard);
                            runParams.put("Player", player);
                            runParams.put("Produced", colors.get(j)); //can't tell what mana the computer just paid?
                            AllZone.getTriggerHandler().runTrigger("TapsForMana", runParams);
                        }//not a test
                    }
                    if (cost.isPaid()) {
                        //if (sa instanceof Spell_Permanent) // should probably add this
                    	sa.getSourceCard().setColorsPaid(cost.getColorsPaid());
                        sa.getSourceCard().setSunburstValue(cost.getSunburst());
                        manapool.clearPay(sa, test);
                        return true;
                    }
                }
            }

        }

        if (!test) // real payment should not arrive here
            throw new RuntimeException("ComputerUtil : payManaCost() cost was not paid for " + sa.getSourceCard().getName());

        return false;

    }//payManaCost()


    /**
     * <p>getProduceableColors.</p>
     *
     * @param m a {@link forge.card.spellability.Ability_Mana} object.
     * @param player a {@link forge.Player} object.
     * @return a {@link java.util.ArrayList} object.
     * @since 1.0.15
     */
    public static ArrayList<String> getProduceableColors(Ability_Mana m, Player player) {
        ArrayList<String> colors = new ArrayList<String>();

        //if the mana ability is not avaiable move to the next one
        m.setActivatingPlayer(player);
        if (!m.canPlay()) return colors;

        if (!colors.contains(Constant.Color.Black) && m.isBasic() && m.mana().equals("B"))
            colors.add(Constant.Color.Black);
        if (!colors.contains(Constant.Color.White) && m.isBasic() && m.mana().equals("W"))
            colors.add(Constant.Color.White);
        if (!colors.contains(Constant.Color.Green) && m.isBasic() && m.mana().equals("G"))
            colors.add(Constant.Color.Green);
        if (!colors.contains(Constant.Color.Red) && m.isBasic() && m.mana().equals("R"))
            colors.add(Constant.Color.Red);
        if (!colors.contains(Constant.Color.Blue) && m.isBasic() && m.mana().equals("U"))
            colors.add(Constant.Color.Blue);
        if (!colors.contains(Constant.Color.Colorless) && m.isBasic() && m.mana().equals("1"))
            colors.add(Constant.Color.Colorless);

        return colors;
    }

    /**
     * <p>getAvailableMana.</p>
     *
     * @return a {@link forge.CardList} object.
     */
    static public CardList getAvailableMana() {
        return getAvailableMana(AllZone.getComputerPlayer());
    }//getAvailableMana()

    //gets available mana sources and sorts them
    /**
     * <p>getAvailableMana.</p>
     *
     * @param player a {@link forge.Player} object.
     * @return a {@link forge.CardList} object.
     */
    static public CardList getAvailableMana(final Player player) {
        CardList list = AllZoneUtil.getPlayerCardsInPlay(player);
        CardList manaSources = list.filter(new CardListFilter() {
            public boolean addCard(Card c) {
                for (Ability_Mana am : c.getAIPlayableMana()) {
                    am.setActivatingPlayer(player);
                    if (am.canPlay()) return true;
                }

                return false;
            }
        });//CardListFilter

        CardList sortedManaSources = new CardList();
        
        // 1. Use lands that can only produce colorless mana without drawback/cost first
        for (int i = 0; i < manaSources.size(); i++) {
            Card card = manaSources.get(i);

            if (card.isCreature() || card.isEnchanted()) continue; //don't use creatures before other permanents

            int usableManaAbilities = 0;
            boolean needsLimitedResources = false;
            ArrayList<Ability_Mana> manaAbilities = card.getAIPlayableMana();

            for (Ability_Mana m : manaAbilities) {

                Cost cost = m.getPayCosts();
                needsLimitedResources |= !cost.isReusuableResource();

                //if the AI can't pay the additional costs skip the mana ability
                m.setActivatingPlayer(AllZone.getComputerPlayer());
                if (cost != null) {
                    if (!canPayAdditionalCosts(m, player))
                        continue;
                }

                //don't use abilities with dangerous drawbacks
                if (m.getSubAbility() != null) {
                    if (!m.getSubAbility().chkAI_Drawback())
                        continue;
                    needsLimitedResources = true; //TODO: check for good drawbacks (gainLife)
                }
                usableManaAbilities++;
            }
            
            //use lands that can only produce colorless mana first
            if (usableManaAbilities == 1 && !needsLimitedResources && manaAbilities.get(0).mana().equals("1"))
            	sortedManaSources.add(card); 	
        }

        // 2. Search for mana sources that have a certain number of mana abilities (start with 1 and go up to 5) and no drawback/costs
        for (int number = 1; number < 6; number++)
            for (int i = 0; i < manaSources.size(); i++) {
                Card card = manaSources.get(i);

                if (card.isCreature() || card.isEnchanted()) continue; //don't use creatures before other permanents

                int usableManaAbilities = 0;
                boolean needsLimitedResources = false;
                ArrayList<Ability_Mana> manaAbilities = card.getAIPlayableMana();

                for (Ability_Mana m : manaAbilities) {

                    Cost cost = m.getPayCosts();
                    needsLimitedResources |= !cost.isReusuableResource();
                    //if the AI can't pay the additional costs skip the mana ability
                    if (cost != null) {
                        if (!canPayAdditionalCosts(m, player))
                            continue;
                    }

                    //don't use abilities with dangerous drawbacks
                    if (m.getSubAbility() != null) {
                        if (!m.getSubAbility().chkAI_Drawback())
                            continue;
                        needsLimitedResources = true; //TODO: check for good drawbacks (gainLife)
                    }
                    usableManaAbilities++;
                }

                if (usableManaAbilities == number && !needsLimitedResources && !sortedManaSources.contains(card))
                    sortedManaSources.add(card);
            }

        //Add the rest
        for (int j = 0; j < manaSources.size(); j++) {
            if (!sortedManaSources.contains(manaSources.get(j)))
                sortedManaSources.add(manaSources.get(j));
        }

        return sortedManaSources;
    }//getAvailableMana()

    // sorts the most needed mana abilities to come first
    /**
     * <p>sortForNeeded.</p>
     *
     * @param cost a {@link forge.card.mana.ManaCost} object.
     * @param manaAbilities a {@link java.util.ArrayList} object.
     * @param player a {@link forge.Player} object.
     * @return a {@link java.util.ArrayList} object.
     * @since 1.0.15
     */
    static public ArrayList<Ability_Mana> sortForNeeded(ManaCost cost, ArrayList<Ability_Mana> manaAbilities, Player player) {

        ArrayList<String> colors;

        ArrayList<Ability_Mana> res = new ArrayList<Ability_Mana>();

        ManaCost onlyColored = new ManaCost(cost.toString());

        onlyColored.removeColorlessMana();

        for (Ability_Mana am : manaAbilities) {
            colors = getProduceableColors(am, player);
            for (int j = 0; j < colors.size(); j++) {
                if (onlyColored.isNeeded(colors.get(j))) {
                    res.add(am);
                    break;
                }
            }
        }

        for (Ability_Mana am : manaAbilities) {

            if (res.contains(am)) break;

            colors = getProduceableColors(am, player);
            for (int j = 0; j < colors.size(); j++) {
                if (cost.isNeeded(colors.get(j))) {
                    res.add(am);
                    break;
                }
            }
        }

        return res;
    }


    //plays a land if one is available
    /**
     * <p>chooseLandsToPlay.</p>
     *
     * @return a boolean.
     */
    static public boolean chooseLandsToPlay() {
        Player computer = AllZone.getComputerPlayer();
        CardList landList = AllZoneUtil.getPlayerHand(computer);
        landList = landList.filter(AllZoneUtil.lands);


        CardList lands = AllZoneUtil.getPlayerTypeInGraveyard(computer, "Land");
        for (Card crd : lands){
        	if (crd.isLand() && crd.hasKeyword("May be played"))
        		landList.add(crd);
        }

        landList = landList.filter(new CardListFilter() {
            public boolean addCard(Card c) {
                if (c.getSVar("NeedsToPlay").length() > 0) {
                    String needsToPlay = c.getSVar("NeedsToPlay");
                    CardList list = AllZoneUtil.getCardsInPlay();

                    list = list.getValidCards(needsToPlay.split(","), c.getController(), c);
                    if (list.isEmpty()) return false;
                }
                if (c.isType("Legendary")
                        && !c.getName().equals("Flagstones of Trokair")) {
                    CardList list = AllZoneUtil.getPlayerCardsInPlay(AllZone.getComputerPlayer());
                    if (list.containsName(c.getName()))
                        return false;
                }
                
                //don't play the land if it has cycling and enough lands are available
                ArrayList<SpellAbility> spellAbilities = c.getSpellAbilities();
                for (SpellAbility sa : spellAbilities) 
                	if (sa.isCycling()) {
                		CardList hand = AllZoneUtil.getPlayerHand(AllZone.getComputerPlayer());
                        CardList lands = AllZoneUtil.getPlayerCardsInPlay(AllZone.getComputerPlayer());
                        lands.addAll(hand);
                        lands = lands.getType("Land");

                        if (lands.size() >= Math.max(hand.getHighestConvertedManaCost(), 6))
                            return false;
                	}
                		
                
                return true;
            }
        });

        while (!landList.isEmpty() && computer.canPlayLand()) {
            // play as many lands as you can
            int ix = 0;
            while (landList.get(ix).isReflectedLand() && (ix + 1 < landList.size())) {
                // Skip through reflected lands. Choose last if they are all reflected.
                ix++;
            }

            Card land = landList.get(ix);
            landList.remove(ix);
            computer.playLand(land);
            
            if (AllZone.getStack().size() != 0)
            	return true;
        }
        return false;
    }

    /**
     * <p>getCardPreference.</p>
     *
     * @param activate a {@link forge.Card} object.
     * @param pref a {@link java.lang.String} object.
     * @param typeList a {@link forge.CardList} object.
     * @return a {@link forge.Card} object.
     */
    static public Card getCardPreference(Card activate, String pref, CardList typeList) {
        String[] prefValid = activate.getSVar("AIPreference").split("\\$");
        if (prefValid[0].equals(pref)) {
            CardList prefList = typeList.getValidCards(prefValid[1].split(","), activate.getController(), activate);
            if (prefList.size() != 0) {
                prefList.shuffle();
                return prefList.get(0);
            }
        }
        if (pref.contains("SacCost")) { // search for permanents with SacMe
            for (int ip = 0; ip < 9; ip++) {    // priority 0 is the lowest, priority 5 the highest
                final int priority = 9 - ip;
                CardList SacMeList = typeList.filter(new CardListFilter() {
                    public boolean addCard(Card c) {
                        return (!c.getSVar("SacMe").equals("") && Integer.parseInt(c.getSVar("SacMe")) == priority);
                    }
                });
                if (SacMeList.size() != 0) {
                    SacMeList.shuffle();
                    return SacMeList.get(0);
                }
            }
        }
        return null;
    }

    /**
     * <p>chooseSacrificeType.</p>
     *
     * @param type a {@link java.lang.String} object.
     * @param activate a {@link forge.Card} object.
     * @param target a {@link forge.Card} object.
     * @param amount a int.
     * @return a {@link forge.CardList} object.
     */
    static public CardList chooseSacrificeType(String type, Card activate, Card target, int amount) {
        CardList typeList = AllZoneUtil.getPlayerCardsInPlay(AllZone.getComputerPlayer());
        typeList = typeList.getValidCards(type.split(","), activate.getController(), activate);
        if (target != null && target.getController().isComputer() && typeList.contains(target))
            typeList.remove(target);        // don't sacrifice the card we're pumping

        if (typeList.size() < amount)
            return null;

        CardList sacList = new CardList();
        int count = 0;

        while (count < amount) {
            Card prefCard = getCardPreference(activate, "SacCost", typeList);
            if (prefCard != null) {
                sacList.add(prefCard);
                typeList.remove(prefCard);
                count++;
            } else
                break;
        }

        CardListUtil.sortAttackLowFirst(typeList);

        for (int i = count; i < amount; i++) sacList.add(typeList.get(i));
        return sacList;
    }

    /**
     * <p>chooseExileType.</p>
     *
     * @param type a {@link java.lang.String} object.
     * @param activate a {@link forge.Card} object.
     * @param target a {@link forge.Card} object.
     * @param amount a int.
     * @return a {@link forge.CardList} object.
     */
    static public CardList chooseExileType(String type, Card activate, Card target, int amount) {
        return chooseExileFrom(Constant.Zone.Battlefield, type, activate, target, amount);
    }

    /**
     * <p>chooseExileFromHandType.</p>
     *
     * @param type a {@link java.lang.String} object.
     * @param activate a {@link forge.Card} object.
     * @param target a {@link forge.Card} object.
     * @param amount a int.
     * @return a {@link forge.CardList} object.
     */
    static public CardList chooseExileFromHandType(String type, Card activate, Card target, int amount) {
        return chooseExileFrom(Constant.Zone.Hand, type, activate, target, amount);
    }

    /**
     * <p>chooseExileFromGraveType.</p>
     *
     * @param type a {@link java.lang.String} object.
     * @param activate a {@link forge.Card} object.
     * @param target a {@link forge.Card} object.
     * @param amount a int.
     * @return a {@link forge.CardList} object.
     */
    static public CardList chooseExileFromGraveType(String type, Card activate, Card target, int amount) {
        return chooseExileFrom(Constant.Zone.Graveyard, type, activate, target, amount);
    }

    /**
     * <p>chooseExileFrom.</p>
     *
     * @param zone a {@link java.lang.String} object.
     * @param type a {@link java.lang.String} object.
     * @param activate a {@link forge.Card} object.
     * @param target a {@link forge.Card} object.
     * @param amount a int.
     * @return a {@link forge.CardList} object.
     */
    static public CardList chooseExileFrom(String zone, String type, Card activate, Card target, int amount) {
        CardList typeList = AllZoneUtil.getCardsInZone(zone, AllZone.getComputerPlayer());
        typeList = typeList.getValidCards(type.split(","), activate.getController(), activate);
        if (target != null && target.getController().isComputer() && typeList.contains(target))
            typeList.remove(target);    // don't exile the card we're pumping

        if (typeList.size() < amount)
            return null;

        CardListUtil.sortAttackLowFirst(typeList);
        CardList exileList = new CardList();

        for (int i = 0; i < amount; i++) exileList.add(typeList.get(i));
        return exileList;
    }

    /**
     * <p>chooseTapType.</p>
     *
     * @param type a {@link java.lang.String} object.
     * @param activate a {@link forge.Card} object.
     * @param tap a boolean.
     * @param amount a int.
     * @return a {@link forge.CardList} object.
     */
    static public CardList chooseTapType(String type, Card activate, boolean tap, int amount) {
        CardList typeList = AllZoneUtil.getPlayerCardsInPlay(AllZone.getComputerPlayer());
        typeList = typeList.getValidCards(type.split(","), activate.getController(), activate);

        //is this needed?
        typeList = typeList.filter(AllZoneUtil.untapped);

        if (tap)
            typeList.remove(activate);

        if (typeList.size() < amount)
            return null;

        CardListUtil.sortAttackLowFirst(typeList);

        CardList tapList = new CardList();

        for (int i = 0; i < amount; i++) tapList.add(typeList.get(i));
        return tapList;
    }

    /**
     * <p>chooseReturnType.</p>
     *
     * @param type a {@link java.lang.String} object.
     * @param activate a {@link forge.Card} object.
     * @param target a {@link forge.Card} object.
     * @param amount a int.
     * @return a {@link forge.CardList} object.
     */
    static public CardList chooseReturnType(String type, Card activate, Card target, int amount) {
        CardList typeList = AllZoneUtil.getPlayerCardsInPlay(AllZone.getComputerPlayer());
        typeList = typeList.getValidCards(type.split(","), activate.getController(), activate);
        if (target != null && target.getController().isComputer() && typeList.contains(target)) // don't bounce the card we're pumping
            typeList.remove(target);

        if (typeList.size() < amount)
            return null;

        CardListUtil.sortAttackLowFirst(typeList);
        CardList returnList = new CardList();

        for (int i = 0; i < amount; i++) returnList.add(typeList.get(i));
        return returnList;
    }

    /**
     * <p>getPossibleAttackers.</p>
     *
     * @return a {@link forge.CardList} object.
     */
    static public CardList getPossibleAttackers() {
        CardList list = AllZoneUtil.getPlayerCardsInPlay(AllZone.getComputerPlayer());
        list = list.filter(new CardListFilter() {
            public boolean addCard(Card c) {
                return CombatUtil.canAttack(c);
            }
        });
        return list;
    }

    /**
     * <p>getAttackers.</p>
     *
     * @return a {@link forge.Combat} object.
     */
    static public Combat getAttackers() {
        ComputerUtil_Attack2 att = new ComputerUtil_Attack2(AllZoneUtil.getPlayerCardsInPlay(AllZone.getComputerPlayer()),
                AllZoneUtil.getPlayerCardsInPlay(AllZone.getHumanPlayer()), AllZone.getHumanPlayer().getLife());

        return att.getAttackers();
    }

    /**
     * <p>getBlockers.</p>
     *
     * @return a {@link forge.Combat} object.
     */
    static public Combat getBlockers() {
        CardList blockers = AllZoneUtil.getPlayerCardsInPlay(AllZone.getComputerPlayer());

        return ComputerUtil_Block2.getBlockers(AllZone.getCombat(), blockers);
    }

    /**
     * <p>sortSpellAbilityByCost.</p>
     *
     * @param sa an array of {@link forge.card.spellability.SpellAbility} objects.
     */
    static void sortSpellAbilityByCost(SpellAbility sa[]) {
        //sort from highest cost to lowest
        //we want the highest costs first
        Comparator<SpellAbility> c = new Comparator<SpellAbility>() {
            public int compare(SpellAbility a, SpellAbility b) {
                int a1 = CardUtil.getConvertedManaCost(a);
                int b1 = CardUtil.getConvertedManaCost(b);

                //puts creatures in front of spells
                if (a.getSourceCard().isCreature())
                    a1 += 1;

                if (b.getSourceCard().isCreature())
                    b1 += 1;


                return b1 - a1;
            }
        };//Comparator
        Arrays.sort(sa, c);
    }//sortSpellAbilityByCost()

    /**
     * <p>sacrificePermanents.</p>
     *
     * @param amount a int.
     * @param list a {@link forge.CardList} object.
     */
    static public void sacrificePermanents(int amount, CardList list) {
        // used in Annihilator and AF_Sacrifice
        int max = list.size();
        if (max > amount)
            max = amount;

        CardListUtil.sortCMC(list);
        list.reverse();

        for (int i = 0; i < max; i++) {
            // TODO: use getWorstPermanent() would be wayyyy better

            Card c;
            if (list.getNotType("Creature").size() == 0) {
                c = CardFactoryUtil.AI_getWorstCreature(list);
            } else if (list.getNotType("Land").size() == 0) {
                c = CardFactoryUtil.getWorstLand(AllZone.getComputerPlayer());
            } else {
                c = list.get(0);
            }

            ArrayList<Card> auras = c.getEnchantedBy();

            if (auras.size() > 0) {
                // TODO: choose "worst" controlled enchanting Aura
                for (int j = 0; j < auras.size(); j++) {
                    Card aura = auras.get(j);
                    if (aura.getController().isPlayer(c.getController()) && list.contains(aura)) {
                        c = aura;
                        break;
                    }
                }
            }

            list.remove(c);
            AllZone.getGameAction().sacrifice(c);
        }
    }

    /**
     * <p>canRegenerate.</p>
     *
     * @param card a {@link forge.Card} object.
     * @return a boolean.
     */
    public static boolean canRegenerate(Card card) {

        if (card.hasKeyword("CARDNAME can't be regenerated.")) return false;

        Player controller = card.getController();
        CardList l = AllZoneUtil.getPlayerCardsInPlay(controller);
        for (Card c : l)
            for (SpellAbility sa : c.getSpellAbility()){
                //This try/catch should fix the "computer is thinking" bug
                try {
                	AbilityFactory af = sa.getAbilityFactory();
                	
                	if (!sa.isAbility() || af == null || !af.getAPI().equals("Regenerate"))
                		continue;	// Not a Regenerate ability
                	
                	//sa.setActivatingPlayer(controller);
            		if (!(sa.canPlay() && ComputerUtil.canPayCost(sa, controller)))
            			continue;	// Can't play ability
            		
            		HashMap<String, String> mapParams = af.getMapParams();
            		
            		Target tgt = sa.getTarget();
                    if (tgt != null){
                    	if (AllZoneUtil.getCardsInPlay().getValidCards(tgt.getValidTgts(), controller, sa.getSourceCard()).contains(card))
							return true;
                    }
                    else if (AbilityFactory.getDefinedCards(sa.getSourceCard(), mapParams.get("Defined"), sa).contains(card))
                        return true;
                    
                } 
        		catch (Exception ex) {
                    showError(ex, "There is an error in the card code for %s:%n", c.getName(), ex.getMessage());
                }
            }

        return false;
    }

    /**
     * <p>possibleDamagePrevention.</p>
     *
     * @param card a {@link forge.Card} object.
     * @return a int.
     */
    public static int possibleDamagePrevention(Card card) {

        int prevented = 0;

        Player controller = card.getController();
        CardList l = AllZoneUtil.getPlayerCardsInPlay(controller);
        for (Card c : l)
            for (SpellAbility sa : c.getSpellAbility())
                // if SA is from AF_Counter don't add to getPlayable
                //This try/catch should fix the "computer is thinking" bug
                try {
                    if (sa.canPlay() && ComputerUtil.canPayCost(sa, controller) && sa.getAbilityFactory() != null && sa.isAbility()) {
                        AbilityFactory af = sa.getAbilityFactory();
                        HashMap<String, String> mapParams = af.getMapParams();
                        if (mapParams.get("AB").equals("PreventDamage")) {
                            if (AbilityFactory.getDefinedCards(sa.getSourceCard(), mapParams.get("Defined"), sa).contains(card))
                                prevented += AbilityFactory.calculateAmount(af.getHostCard(), mapParams.get("Amount"), sa);
                            Target tgt = sa.getTarget();
                            if (tgt != null) {
                                if (AllZoneUtil.getCardsInPlay().getValidCards(tgt.getValidTgts(), controller, af.getHostCard())
                                        .contains(card))
                                    prevented += AbilityFactory.calculateAmount(af.getHostCard(), mapParams.get("Amount"), sa);

                            }
                        }
                    }
                } catch (Exception ex) {
                    showError(ex, "There is an error in the card code for %s:%n", c.getName(), ex.getMessage());
                }

        return prevented;
    }
}
