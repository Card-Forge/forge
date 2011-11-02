package forge;

import static forge.error.ErrorViewer.showError;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;

import forge.Constant.Zone;
import forge.card.abilityFactory.AbilityFactory;
import forge.card.cardFactory.CardFactoryUtil;
import forge.card.cost.Cost;
import forge.card.cost.CostUtil;
import forge.card.cost.Cost_Payment;
import forge.card.mana.ManaCost;
import forge.card.mana.ManaPool;
import forge.card.spellability.Ability_Mana;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;

/**
 * <p>
 * ComputerUtil class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class ComputerUtil {

    // if return true, go to next phase
    /**
     * <p>
     * playCards.
     * </p>
     * 
     * @return a boolean.
     */
    public static boolean playCards() {
        return playCards(getSpellAbility());
    }

    // if return true, go to next phase
    /**
     * <p>
     * playCards.
     * </p>
     * 
     * @param all
     *            an array of {@link forge.card.spellability.SpellAbility}
     *            objects.
     * @return a boolean.
     */
    public static boolean playCards(final SpellAbility[] all) {
        // not sure "playing biggest spell" matters?
        sortSpellAbilityByCost(all);
        // MyRandom.shuffle(all);

        for (SpellAbility sa : all) {
            // Don't add Counterspells to the "normal" playcard lookupss
            AbilityFactory af = sa.getAbilityFactory();
            if (af != null && af.getAPI().equals("Counter")) {
                continue;
            }

            sa.setActivatingPlayer(AllZone.getComputerPlayer());
            if (canBePlayedAndPayedByAI(sa)) {
                handlePlayingSpellAbility(sa);

                return false;
            }
        }
        return true;
    } // playCards()

    /**
     * <p>
     * playCards.
     * </p>
     * 
     * @param all
     *            a {@link java.util.ArrayList} object.
     * @return a boolean.
     */
    public static boolean playCards(final ArrayList<SpellAbility> all) {
        SpellAbility[] sas = new SpellAbility[all.size()];
        for (int i = 0; i < sas.length; i++) {
            sas[i] = all.get(i);
        }
        return playCards(sas);
    } // playCards()

    /**
     * <p>
     * handlePlayingSpellAbility.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    public static void handlePlayingSpellAbility(final SpellAbility sa) {
        AllZone.getStack().freezeStack();
        Card source = sa.getSourceCard();

        if (sa.isSpell() && !source.isCopiedSpell()) {
            sa.setSourceCard(AllZone.getGameAction().moveToStack(source));
        }

        Cost cost = sa.getPayCosts();
        Target tgt = sa.getTarget();

        if (cost == null) {
            payManaCost(sa);
            sa.chooseTargetAI();
            sa.getBeforePayManaAI().execute();
            AllZone.getStack().addAndUnfreeze(sa);
        } else {
            if (tgt != null && tgt.doesTarget()) {
                sa.chooseTargetAI();
            }

            Cost_Payment pay = new Cost_Payment(cost, sa);
            if (pay.payComputerCosts()) {
                AllZone.getStack().addAndUnfreeze(sa);
            }
        }
    }

    /**
     * <p>
     * counterSpellRestriction.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a int.
     */
    public static int counterSpellRestriction(final SpellAbility sa) {
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
        // String totalMana = source.getSVar("PayX"); // + cost.getCMC()

        // Consider the costs here for relative "scoring"
        if (CostUtil.hasDiscardHandCost(cost)) {
            // Null Brooch aid
            restrict -= (AllZone.getComputerPlayer().getCardsIn(Zone.Hand).size() * 20);
        }

        // Abilities before Spells (card advantage)
        if (af.isAbility()) {
            restrict += 40;
        }

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
            if (amount > usableManaSources) {
                restrict += 20 - (2 * amount);
            } else {
                restrict -= (10 - (2 * amount));
            }
        }

        // Then base on Targeting Restriction
        String[] validTgts = tgt.getValidTgts();
        if (validTgts.length != 1 || !validTgts[0].equals("Card")) {
            restrict += 10;
        }

        // And lastly give some bonus points to least restrictive TargetType
        // (Spell,Ability,Triggered)
        String tgtType = tgt.getTargetSpellAbilityType();
        restrict -= (5 * tgtType.split(",").length);

        return restrict;
    }

    // if return true, go to next phase
    /**
     * <p>
     * playCounterSpell.
     * </p>
     * 
     * @param possibleCounters
     *            a {@link java.util.ArrayList} object.
     * @return a boolean.
     */
    public static boolean playCounterSpell(final ArrayList<SpellAbility> possibleCounters) {
        SpellAbility bestSA = null;
        int bestRestriction = Integer.MIN_VALUE;

        for (SpellAbility sa : possibleCounters) {
            sa.setActivatingPlayer(AllZone.getComputerPlayer());
            if (canBePlayedAndPayedByAI(sa)) { // checks everything nescessary
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

        if (bestSA == null) {
            return false;
        }

        // TODO
        // "Look" at Targeted SA and "calculate" the threshold
        // if (bestRestriction < targetedThreshold) return false;

        AllZone.getStack().freezeStack();
        Card source = bestSA.getSourceCard();

        if (bestSA.isSpell() && !source.isCopiedSpell()) {
            bestSA.setSourceCard(AllZone.getGameAction().moveToStack(source));
        }

        Cost cost = bestSA.getPayCosts();

        if (cost == null) {
            // Honestly Counterspells shouldn't use this branch
            payManaCost(bestSA);
            bestSA.chooseTargetAI();
            bestSA.getBeforePayManaAI().execute();
            AllZone.getStack().addAndUnfreeze(bestSA);
        } else {
            Cost_Payment pay = new Cost_Payment(cost, bestSA);
            if (pay.payComputerCosts()) {
                AllZone.getStack().addAndUnfreeze(bestSA);
            }
        }

        return true;
    } // playCounterSpell()

    // this is used for AI's counterspells
    /**
     * <p>
     * playStack.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    public static final void playStack(final SpellAbility sa) {
        if (canPayCost(sa)) {
            Card source = sa.getSourceCard();
            if (sa.isSpell() && !source.isCopiedSpell()) {
                sa.setSourceCard(AllZone.getGameAction().moveToStack(source));
            }

            sa.setActivatingPlayer(AllZone.getComputerPlayer());

            payManaCost(sa);

            AllZone.getStack().add(sa);
        }
    }

    /**
     * <p>
     * playStackFree.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    public static final void playStackFree(final SpellAbility sa) {
        sa.setActivatingPlayer(AllZone.getComputerPlayer());

        Card source = sa.getSourceCard();
        if (sa.isSpell() && !source.isCopiedSpell()) {
            sa.setSourceCard(AllZone.getGameAction().moveToStack(source));
        }

        AllZone.getStack().add(sa);
    }

    /**
     * <p>
     * playNoStack.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    public static final void playNoStack(final SpellAbility sa) {
        // TODO: We should really restrict what doesn't use the Stack

        if (canPayCost(sa)) {
            Card source = sa.getSourceCard();
            if (sa.isSpell() && !source.isCopiedSpell()) {
                sa.setSourceCard(AllZone.getGameAction().moveToStack(source));
            }

            sa.setActivatingPlayer(AllZone.getComputerPlayer());

            Cost cost = sa.getPayCosts();
            if (cost == null) {
                payManaCost(sa);
            } else {
                Cost_Payment pay = new Cost_Payment(cost, sa);
                pay.payComputerCosts();
            }

            AbilityFactory.resolve(sa, false);

            // destroys creatures if they have lethal damage, etc..
            AllZone.getGameAction().checkStateEffects();
        }
    } // play()

    // gets Spells of cards in hand and Abilities of cards in play
    // checks to see
    // 1. if canPlay() returns true, 2. can pay for mana
    /**
     * <p>
     * getSpellAbility.
     * </p>
     * 
     * @return an array of {@link forge.card.spellability.SpellAbility} objects.
     */
    public static SpellAbility[] getSpellAbility() {
        CardList all = new CardList();
        all.addAll(AllZone.getComputerPlayer().getCardsIn(Zone.Battlefield));
        all.addAll(AllZone.getComputerPlayer().getCardsIn(Zone.Hand));
        all.addAll(CardFactoryUtil.getExternalZoneActivationCards(AllZone.getComputerPlayer()));

        CardList humanPlayable = new CardList();
        humanPlayable.addAll(AllZone.getHumanPlayer().getCardsIn(Zone.Battlefield));
        humanPlayable = humanPlayable.filter(new CardListFilter() {
            public boolean addCard(final Card c) {
                return (c.canAnyPlayerActivate());
            }
        });

        all.addAll(humanPlayable);

        ArrayList<SpellAbility> spellAbility = new ArrayList<SpellAbility>();
        for (int outer = 0; outer < all.size(); outer++) {
            SpellAbility[] sa = all.get(outer).getSpellAbility();
            for (int i = 0; i < sa.length; i++) {
                spellAbility.add(sa[i]); // this seems like it needs to be
                                        // copied, not sure though
            }
        }

        SpellAbility[] sa = new SpellAbility[spellAbility.size()];
        spellAbility.toArray(sa);
        return sa;
    }

    // This is for playing spells regularly (no Cascade/Ripple etc.)
    /**
     * <p>
     * canBePlayedAndPayedByAI.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     * @since 1.0.15
     */
    public static boolean canBePlayedAndPayedByAI(final SpellAbility sa) {
        return sa.canPlay() && sa.canPlayAI() && canPayCost(sa);
    }

    /**
     * <p>
     * canPayCost.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    public static boolean canPayCost(final SpellAbility sa) {
        return canPayCost(sa, AllZone.getComputerPlayer());
    } // canPayCost()

    /**
     * <p>
     * canPayCost.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param player
     *            a {@link forge.Player} object.
     * @return a boolean.
     */
    public static boolean canPayCost(final SpellAbility sa, final Player player) {
        if (!payManaCost(sa, player, true, 0)) {
            return false;
        }

        return canPayAdditionalCosts(sa, player);
    } // canPayCost()

    /**
     * <p>
     * determineLeftoverMana.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a int.
     */
    public static int determineLeftoverMana(final SpellAbility sa) {
        return determineLeftoverMana(sa, AllZone.getComputerPlayer());
    }

    /**
     * <p>
     * determineLeftoverMana.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param player
     *            a {@link forge.Player} object.
     * @return a int.
     * @since 1.0.15
     */
    public static int determineLeftoverMana(final SpellAbility sa, final Player player) {

        int xMana = 0;

        for (int i = 1; i < 99; i++) {
            if (!payManaCost(sa, player, true, xMana)) {
                break;
            }
            xMana = i;
        }

        return xMana;
    }

    /**
     * <p>
     * canPayAdditionalCosts.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    public static boolean canPayAdditionalCosts(final SpellAbility sa) {
        return canPayAdditionalCosts(sa, AllZone.getComputerPlayer());
    }

    /**
     * <p>
     * canPayAdditionalCosts.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param player
     *            a {@link forge.Player} object.
     * @return a boolean.
     */
    public static boolean canPayAdditionalCosts(final SpellAbility sa, final Player player) {
        if (sa.getActivatingPlayer() == null) {
            System.out.println(sa.getSourceCard()
                    + " in ComputerUtil.canPayAdditionalCosts() without an activating player");
            sa.setActivatingPlayer(player);
        }
        return Cost_Payment.canPayAdditionalCosts(sa.getPayCosts(), sa);
    }

    /**
     * <p>
     * payManaCost.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    public static void payManaCost(final SpellAbility sa) {
        payManaCost(sa, AllZone.getComputerPlayer(), false, 0);
    }

    /**
     * <p>
     * payManaCost.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param player
     *            a {@link forge.Player} object.
     * @param test
     *            (is for canPayCost, if true does not change the game state)
     * @param extraMana
     *            a int.
     * @return a boolean.
     * @since 1.0.15
     */
    public static boolean payManaCost(final SpellAbility sa, final Player player, final boolean test,
            final int extraMana) {
        String mana = sa.getPayCosts() != null ? sa.getPayCosts().getTotalMana() : sa.getManaCost();

        ManaCost cost = new ManaCost(mana);

        cost = AllZone.getGameAction().getSpellCostChange(sa, cost);

        ManaPool manapool = player.getManaPool();

        Card card = sa.getSourceCard();
        // Tack xMana Payments into mana here if X is a set value
        if (sa.getPayCosts() != null && cost.getXcounter() > 0) {

            int manaToAdd = 0;
            if (test && extraMana > 0) {
                manaToAdd = extraMana * cost.getXcounter();
            } else {
                // For Count$xPaid set PayX in the AFs then use that here
                // Else calculate it as appropriate.
                String xSvar = card.getSVar("X").equals("Count$xPaid") ? "PayX" : "X";
                if (!card.getSVar(xSvar).equals("")) {
                    if (xSvar.equals("PayX")) {
                        manaToAdd = Integer.parseInt(card.getSVar(xSvar)) * cost.getXcounter(); // X
                    } else {
                        manaToAdd = AbilityFactory.calculateAmount(card, xSvar, sa) * cost.getXcounter();
                    }
                }
            }

            cost.increaseColorlessMana(manaToAdd);
            if (!test) {
                card.setXManaCostPaid(manaToAdd);
            }
        }

        if (cost.isPaid()) {
            return true;
        }

        ArrayList<String> colors;

        cost = ((ManaPool) manapool).subtractMana(sa, cost);
        if (card.getSVar("ManaNeededToAvoidNegativeEffect") != "") {
            cost.setManaNeededToAvoidNegativeEffect(card.getSVar("ManaNeededToAvoidNegativeEffect").split(","));
        }

        CardList manaSources = getAvailableMana();

        // this is to prevent errors for mana sources that have abilities that
        // cost mana.
        manaSources.remove(sa.getSourceCard());

        for (int i = 0; i < manaSources.size(); i++) {
            Card sourceCard = manaSources.get(i);
            ArrayList<Ability_Mana> manaAbilities = sourceCard.getAIPlayableMana();

            boolean used = false; // this is for testing paying mana only

            manaAbilities = sortForNeeded(cost, manaAbilities, player);

            for (Ability_Mana m : manaAbilities) {

                if (used) {
                    break; // mana source already used in the test
                }
                m.setActivatingPlayer(player);
                // if the AI can't pay the additional costs skip the mana
                // ability
                if (m.getPayCosts() != null) {
                    if (!canPayAdditionalCosts(m, player)) {
                        continue;
                    }
                } else if (sourceCard.isTapped()) {
                    continue;
                }

                // don't use abilities with dangerous drawbacks
                if (m.getSubAbility() != null) {
                    if (!m.getSubAbility().chkAIDrawback()) {
                        continue;
                    }
                }

                colors = getProduceableColors(m, player);
                for (int j = 0; j < colors.size(); j++) {
                    if (used) {
                        break; // mana source already used in the test
                    }

                    if (cost.isNeeded(colors.get(j))) {
                        if (!test) {
                            // Pay additional costs
                            if (m.getPayCosts() != null) {
                                Cost_Payment pay = new Cost_Payment(m.getPayCosts(), m);
                                if (!pay.payComputerCosts()) {
                                    continue;
                                }
                            } else {
                                sourceCard.tap();
                            }
                        } else {
                            used = true; // mana source is now used in the test
                        }

                        cost.payMana(colors.get(j));

                        if (!test) {
                            // resolve subabilities
                            AbilityFactory af = m.getAbilityFactory();
                            if (af != null) {
                                AbilityFactory.resolveSubAbilities(m);
                            }

                            if (sourceCard.getName().equals("Undiscovered Paradise")) {
                                sourceCard.setBounceAtUntap(true);
                            }

                            if (sourceCard.getName().equals("Rainbow Vale")) {
                                sourceCard
                                        .addExtrinsicKeyword("An opponent gains control of CARDNAME "
                                + "at the beginning of the next end step.");
                            }

                            // System.out.println("just subtracted " +
                            // colors.get(j) + ", cost is now: " +
                            // cost.toString());
                            // Run triggers
                            HashMap<String, Object> runParams = new HashMap<String, Object>();

                            runParams.put("Card", sourceCard);
                            runParams.put("Player", player);
                            runParams.put("Produced", colors.get(j)); // can't
                                                                      // tell
                                                                      // what
                                                                      // mana
                                                                      // the
                                                                      // computer
                                                                      // just
                                                                      // paid?
                            AllZone.getTriggerHandler().runTrigger("TapsForMana", runParams);
                        } // not a test
                    }
                    if (cost.isPaid()) {
                        // if (sa instanceof Spell_Permanent) // should probably
                        // add this
                        sa.getSourceCard().setColorsPaid(cost.getColorsPaid());
                        sa.getSourceCard().setSunburstValue(cost.getSunburst());
                        manapool.clearPay(sa, test);
                        return true;
                    }
                }
            }

        }

        if (!test) {
            throw new RuntimeException("ComputerUtil : payManaCost() cost was not paid for "
                    + sa.getSourceCard().getName());
        }

        return false;

    } // payManaCost()

    /**
     * <p>
     * getProduceableColors.
     * </p>
     * 
     * @param m
     *            a {@link forge.card.spellability.Ability_Mana} object.
     * @param player
     *            a {@link forge.Player} object.
     * @return a {@link java.util.ArrayList} object.
     * @since 1.0.15
     */
    public static ArrayList<String> getProduceableColors(final Ability_Mana m, final Player player) {
        ArrayList<String> colors = new ArrayList<String>();

        // if the mana ability is not avaiable move to the next one
        m.setActivatingPlayer(player);
        if (!m.canPlay()) {
            return colors;
        }

        if (!colors.contains(Constant.Color.BLACK) && m.isBasic() && m.mana().equals("B")) {
            colors.add(Constant.Color.BLACK);
        }
        if (!colors.contains(Constant.Color.WHITE) && m.isBasic() && m.mana().equals("W")) {
            colors.add(Constant.Color.WHITE);
        }
        if (!colors.contains(Constant.Color.GREEN) && m.isBasic() && m.mana().equals("G")) {
            colors.add(Constant.Color.GREEN);
        }
        if (!colors.contains(Constant.Color.RED) && m.isBasic() && m.mana().equals("R")) {
            colors.add(Constant.Color.RED);
        }
        if (!colors.contains(Constant.Color.BLUE) && m.isBasic() && m.mana().equals("U")) {
            colors.add(Constant.Color.BLUE);
        }
        if (!colors.contains(Constant.Color.COLORLESS) && m.isBasic() && m.mana().equals("1")) {
            colors.add(Constant.Color.COLORLESS);
        }

        return colors;
    }

    /**
     * <p>
     * getAvailableMana.
     * </p>
     * 
     * @return a {@link forge.CardList} object.
     */
    public static CardList getAvailableMana() {
        return getAvailableMana(AllZone.getComputerPlayer());
    } // getAvailableMana()

    // gets available mana sources and sorts them
    /**
     * <p>
     * getAvailableMana.
     * </p>
     * 
     * @param player
     *            a {@link forge.Player} object.
     * @return a {@link forge.CardList} object.
     */
    public static CardList getAvailableMana(final Player player) {
        CardList list = player.getCardsIn(Zone.Battlefield);
        CardList manaSources = list.filter(new CardListFilter() {
            public boolean addCard(final Card c) {
                for (Ability_Mana am : c.getAIPlayableMana()) {
                    am.setActivatingPlayer(player);
                    if (am.canPlay()) {
                        return true;
                    }
                }

                return false;
            }
        }); // CardListFilter

        CardList sortedManaSources = new CardList();

        // 1. Use lands that can only produce colorless mana without
        // drawback/cost first
        for (int i = 0; i < manaSources.size(); i++) {
            Card card = manaSources.get(i);

            if (card.isCreature() || card.isEnchanted()) {
                continue; // don't use creatures before other permanents
            }

            int usableManaAbilities = 0;
            boolean needsLimitedResources = false;
            ArrayList<Ability_Mana> manaAbilities = card.getAIPlayableMana();

            for (Ability_Mana m : manaAbilities) {

                Cost cost = m.getPayCosts();
                needsLimitedResources |= !cost.isReusuableResource();

                // if the AI can't pay the additional costs skip the mana
                // ability
                m.setActivatingPlayer(AllZone.getComputerPlayer());
                if (cost != null) {
                    if (!canPayAdditionalCosts(m, player)) {
                        continue;
                    }
                }

                // don't use abilities with dangerous drawbacks
                if (m.getSubAbility() != null) {
                    if (!m.getSubAbility().chkAIDrawback()) {
                        continue;
                    }
                    needsLimitedResources = true; // TODO: check for good
                                                  // drawbacks (gainLife)
                }
                usableManaAbilities++;
            }

            // use lands that can only produce colorless mana first
            if (usableManaAbilities == 1 && !needsLimitedResources && manaAbilities.get(0).mana().equals("1")) {
                sortedManaSources.add(card);
            }
        }

        // 2. Search for mana sources that have a certain number of mana
        // abilities (start with 1 and go up to 5) and no drawback/costs
        for (int number = 1; number < 6; number++) {
            for (int i = 0; i < manaSources.size(); i++) {
                Card card = manaSources.get(i);

                if (card.isCreature() || card.isEnchanted()) {
                    continue; // don't use creatures before other permanents
                }

                int usableManaAbilities = 0;
                boolean needsLimitedResources = false;
                ArrayList<Ability_Mana> manaAbilities = card.getAIPlayableMana();

                for (Ability_Mana m : manaAbilities) {

                    Cost cost = m.getPayCosts();
                    needsLimitedResources |= !cost.isReusuableResource();
                    // if the AI can't pay the additional costs skip the mana
                    // ability
                    if (cost != null) {
                        if (!canPayAdditionalCosts(m, player)) {
                            continue;
                        }
                    }

                    // don't use abilities with dangerous drawbacks
                    if (m.getSubAbility() != null) {
                        if (!m.getSubAbility().chkAIDrawback()) {
                            continue;
                        }
                        needsLimitedResources = true; // TODO: check for good
                                                      // drawbacks (gainLife)
                    }
                    usableManaAbilities++;
                }

                if (usableManaAbilities == number && !needsLimitedResources && !sortedManaSources.contains(card)) {
                    sortedManaSources.add(card);
                }
            }
        }

        // Add the rest
        for (int j = 0; j < manaSources.size(); j++) {
            if (!sortedManaSources.contains(manaSources.get(j))) {
                sortedManaSources.add(manaSources.get(j));
            }
        }

        return sortedManaSources;
    } // getAvailableMana()

    // sorts the most needed mana abilities to come first
    /**
     * <p>
     * sortForNeeded.
     * </p>
     * 
     * @param cost
     *            a {@link forge.card.mana.ManaCost} object.
     * @param manaAbilities
     *            a {@link java.util.ArrayList} object.
     * @param player
     *            a {@link forge.Player} object.
     * @return a {@link java.util.ArrayList} object.
     * @since 1.0.15
     */
    public static ArrayList<Ability_Mana> sortForNeeded(final ManaCost cost,
            final ArrayList<Ability_Mana> manaAbilities, final Player player) {

        ArrayList<String> colors;

        ArrayList<String> colorsNeededToAvoidNegativeEffect = cost.getManaNeededToAvoidNegativeEffect();

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
                for (String col : colorsNeededToAvoidNegativeEffect) {
                    if (col.equalsIgnoreCase(colors.get(j))
                            || CardUtil.getShortColor(col).equalsIgnoreCase(colors.get(j))) {
                        res.add(am);
                    }
                }
            }
        }

        for (Ability_Mana am : manaAbilities) {

            if (res.contains(am)) {
                break;
            }

            colors = getProduceableColors(am, player);
            for (int j = 0; j < colors.size(); j++) {
                if (cost.isNeeded(colors.get(j))) {
                    res.add(am);
                    break;
                }
                for (String col : colorsNeededToAvoidNegativeEffect) {
                    if (col.equalsIgnoreCase(colors.get(j))
                            || CardUtil.getShortColor(col).equalsIgnoreCase(colors.get(j))) {
                        res.add(am);
                    }
                }
            }
        }

        return res;
    }

    // plays a land if one is available
    /**
     * <p>
     * chooseLandsToPlay.
     * </p>
     * 
     * @return a boolean.
     */
    public static boolean chooseLandsToPlay() {
        Player computer = AllZone.getComputerPlayer();
        CardList landList = computer.getCardsIn(Zone.Hand);
        landList = landList.filter(CardListFilter.LANDS);

        CardList lands = computer.getCardsIn(Zone.Graveyard).getType("Land");
        for (Card crd : lands) {
            if (crd.isLand() && crd.hasStartOfKeyword("May be played")) {
                landList.add(crd);
            }
        }

        landList = landList.filter(new CardListFilter() {
            public boolean addCard(final Card c) {
                if (c.getSVar("NeedsToPlay").length() > 0) {
                    String needsToPlay = c.getSVar("NeedsToPlay");
                    CardList list = AllZoneUtil.getCardsIn(Zone.Battlefield);

                    list = list.getValidCards(needsToPlay.split(","), c.getController(), c);
                    if (list.isEmpty()) {
                        return false;
                    }
                }
                if (c.isType("Legendary") && !c.getName().equals("Flagstones of Trokair")) {
                    CardList list = AllZone.getComputerPlayer().getCardsIn(Zone.Battlefield);
                    if (list.containsName(c.getName())) {
                        return false;
                    }
                }

                // don't play the land if it has cycling and enough lands are
                // available
                ArrayList<SpellAbility> spellAbilities = c.getSpellAbilities();
                for (SpellAbility sa : spellAbilities) {
                    if (sa.isCycling()) {
                        CardList hand = AllZone.getComputerPlayer().getCardsIn(Zone.Hand);
                        CardList lands = AllZone.getComputerPlayer().getCardsIn(Zone.Battlefield);
                        lands.addAll(hand);
                        lands = lands.getType("Land");

                        if (lands.size() >= Math.max(hand.getHighestConvertedManaCost(), 6)) {
                            return false;
                        }
                    }
                }
                return true;
            }
        });

        while (!landList.isEmpty() && computer.canPlayLand()) {
            // play as many lands as you can
            int ix = 0;
            while (landList.get(ix).isReflectedLand() && (ix + 1 < landList.size())) {
                // Skip through reflected lands. Choose last if they are all
                // reflected.
                ix++;
            }

            Card land = landList.get(ix);
            landList.remove(ix);
            computer.playLand(land);

            if (AllZone.getStack().size() != 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * <p>
     * getCardPreference.
     * </p>
     * 
     * @param activate
     *            a {@link forge.Card} object.
     * @param pref
     *            a {@link java.lang.String} object.
     * @param typeList
     *            a {@link forge.CardList} object.
     * @return a {@link forge.Card} object.
     */
    public static Card getCardPreference(final Card activate, final String pref, final CardList typeList) {

        if (activate != null) {
            String[] prefValid = activate.getSVar("AIPreference").split("\\$");
            if (prefValid[0].equals(pref)) {
                CardList prefList = typeList.getValidCards(prefValid[1].split(","), activate.getController(), activate);
                if (prefList.size() != 0) {
                    prefList.shuffle();
                    return prefList.get(0);
                }
            }
        }
        if (pref.contains("SacCost")) { // search for permanents with SacMe
            for (int ip = 0; ip < 9; ip++) { // priority 0 is the lowest,
                                             // priority 5 the highest
                final int priority = 9 - ip;
                CardList sacMeList = typeList.filter(new CardListFilter() {
                    public boolean addCard(final Card c) {
                        return (!c.getSVar("SacMe").equals("") && Integer.parseInt(c.getSVar("SacMe")) == priority);
                    }
                });
                if (sacMeList.size() != 0) {
                    sacMeList.shuffle();
                    return sacMeList.get(0);
                }
            }
        }

        if (pref.contains("DiscardCost")) { // search for permanents with
                                            // DiscardMe
            for (int ip = 0; ip < 9; ip++) { // priority 0 is the lowest,
                                             // priority 5 the highest
                final int priority = 9 - ip;
                CardList sacMeList = typeList.filter(new CardListFilter() {
                    public boolean addCard(final Card c) {
                        return (!c.getSVar("DiscardMe").equals("")
                                && Integer.parseInt(c.getSVar("DiscardMe")) == priority);
                    }
                });
                if (sacMeList.size() != 0) {
                    sacMeList.shuffle();
                    return sacMeList.get(0);
                }
            }
        }

        return null;
    }

    /**
     * <p>
     * chooseSacrificeType.
     * </p>
     * 
     * @param type
     *            a {@link java.lang.String} object.
     * @param activate
     *            a {@link forge.Card} object.
     * @param target
     *            a {@link forge.Card} object.
     * @param amount
     *            a int.
     * @return a {@link forge.CardList} object.
     */
    public static CardList chooseSacrificeType(final String type, final Card activate, final Card target,
            final int amount) {
        CardList typeList = AllZone.getComputerPlayer().getCardsIn(Zone.Battlefield);
        typeList = typeList.getValidCards(type.split(","), activate.getController(), activate);
        if (target != null && target.getController().isComputer() && typeList.contains(target)) {
            typeList.remove(target); // don't sacrifice the card we're pumping
        }

        if (typeList.size() < amount) {
            return null;
        }

        CardList sacList = new CardList();
        int count = 0;

        while (count < amount) {
            Card prefCard = getCardPreference(activate, "SacCost", typeList);
            if (prefCard != null) {
                sacList.add(prefCard);
                typeList.remove(prefCard);
                count++;
            } else {
                break;
            }
        }

        CardListUtil.sortAttackLowFirst(typeList);

        for (int i = count; i < amount; i++) {
            sacList.add(typeList.get(i));
        }
        return sacList;
    }

    /**
     * <p>
     * AI_discardNumType.
     * </p>
     * 
     * @param numDiscard
     *            a int.
     * @param uTypes
     *            an array of {@link java.lang.String} objects. May be null for
     *            no restrictions.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a CardList of discarded cards.
     */
    public static CardList discardNumTypeAI(final int numDiscard, final String[] uTypes, final SpellAbility sa) {
        CardList hand = AllZone.getComputerPlayer().getCardsIn(Zone.Hand);
        Card sourceCard = null;

        if (uTypes != null && sa != null) {
            hand = hand.getValidCards(uTypes, sa.getActivatingPlayer(), sa.getSourceCard());
        }
        if (sa != null) {
            sourceCard = sa.getSourceCard();
        }

        if (hand.size() < numDiscard) {
            return null;
        }

        CardList discardList = new CardList();
        int count = 0;

        // look for good discards
        while (count < numDiscard) {
            Card prefCard = getCardPreference(sourceCard, "DiscardCost", hand);
            if (prefCard != null) {
                discardList.add(prefCard);
                hand.remove(prefCard);
                count++;
            } else {
                break;
            }
        }

        int discardsLeft = numDiscard - count;

        // chose rest
        for (int i = 0; i < discardsLeft; i++) {
            if (hand.size() <= 0) {
                continue;
            }
            CardList landsInPlay = AllZone.getComputerPlayer().getCardsIn(Zone.Battlefield).getType("Land");
            if (landsInPlay.size() > 5) {
                CardList landsInHand = hand.getType("Land");
                if (landsInHand.size() > 0) { // discard lands
                    discardList.add(landsInHand.get(0));
                    hand.remove(landsInHand.get(0));
                } else { // discard low costed stuff
                    CardListUtil.sortCMC(hand);
                    hand.reverse();
                    discardList.add(hand.get(0));
                    hand.remove(hand.get(0));
                }
            } else { // discard high costed stuff
                CardListUtil.sortCMC(hand);
                discardList.add(hand.get(0));
                hand.remove(hand.get(0));
            }
        }

        return discardList;
    }

    /**
     * <p>
     * chooseExileType.
     * </p>
     * 
     * @param type
     *            a {@link java.lang.String} object.
     * @param activate
     *            a {@link forge.Card} object.
     * @param target
     *            a {@link forge.Card} object.
     * @param amount
     *            a int.
     * @return a {@link forge.CardList} object.
     */
    public static CardList chooseExileType(final String type,
            final Card activate, final Card target, final int amount) {
        return chooseExileFrom(Constant.Zone.Battlefield, type, activate, target, amount);
    }

    /**
     * <p>
     * chooseExileFromHandType.
     * </p>
     * 
     * @param type
     *            a {@link java.lang.String} object.
     * @param activate
     *            a {@link forge.Card} object.
     * @param target
     *            a {@link forge.Card} object.
     * @param amount
     *            a int.
     * @return a {@link forge.CardList} object.
     */
    public static CardList chooseExileFromHandType(final String type, final Card activate, final Card target,
            final int amount) {
        return chooseExileFrom(Constant.Zone.Hand, type, activate, target, amount);
    }

    /**
     * <p>
     * chooseExileFromGraveType.
     * </p>
     * 
     * @param type
     *            a {@link java.lang.String} object.
     * @param activate
     *            a {@link forge.Card} object.
     * @param target
     *            a {@link forge.Card} object.
     * @param amount
     *            a int.
     * @return a {@link forge.CardList} object.
     */
    public static CardList chooseExileFromGraveType(final String type, final Card activate, final Card target,
            final int amount) {
        return chooseExileFrom(Constant.Zone.Graveyard, type, activate, target, amount);
    }

    /**
     * <p>
     * chooseExileFrom.
     * </p>
     * 
     * @param zone
     *            a {@link java.lang.String} object.
     * @param type
     *            a {@link java.lang.String} object.
     * @param activate
     *            a {@link forge.Card} object.
     * @param target
     *            a {@link forge.Card} object.
     * @param amount
     *            a int.
     * @return a {@link forge.CardList} object.
     */
    public static CardList chooseExileFrom(final Constant.Zone zone, final String type, final Card activate,
            final Card target, final int amount) {
        CardList typeList = AllZone.getComputerPlayer().getCardsIn(zone);
        typeList = typeList.getValidCards(type.split(","), activate.getController(), activate);
        if (target != null && target.getController().isComputer() && typeList.contains(target)) {
            typeList.remove(target); // don't exile the card we're pumping
        }

        if (typeList.size() < amount) {
            return null;
        }

        CardListUtil.sortAttackLowFirst(typeList);
        CardList exileList = new CardList();

        for (int i = 0; i < amount; i++) {
            exileList.add(typeList.get(i));
        }
        return exileList;
    }

    /**
     * <p>
     * chooseTapType.
     * </p>
     * 
     * @param type
     *            a {@link java.lang.String} object.
     * @param activate
     *            a {@link forge.Card} object.
     * @param tap
     *            a boolean.
     * @param amount
     *            a int.
     * @return a {@link forge.CardList} object.
     */
    public static CardList chooseTapType(final String type, final Card activate, final boolean tap, final int amount) {
        CardList typeList = AllZone.getComputerPlayer().getCardsIn(Zone.Battlefield);
        typeList = typeList.getValidCards(type.split(","), activate.getController(), activate);

        // is this needed?
        typeList = typeList.filter(CardListFilter.UNTAPPED);

        if (tap) {
            typeList.remove(activate);
        }

        if (typeList.size() < amount) {
            return null;
        }

        CardListUtil.sortAttackLowFirst(typeList);

        CardList tapList = new CardList();

        for (int i = 0; i < amount; i++) {
            tapList.add(typeList.get(i));
        }
        return tapList;
    }

    /**
     * <p>
     * chooseReturnType.
     * </p>
     * 
     * @param type
     *            a {@link java.lang.String} object.
     * @param activate
     *            a {@link forge.Card} object.
     * @param target
     *            a {@link forge.Card} object.
     * @param amount
     *            a int.
     * @return a {@link forge.CardList} object.
     */
    public static CardList chooseReturnType(final String type,
            final Card activate, final Card target, final int amount) {
        CardList typeList = AllZone.getComputerPlayer().getCardsIn(Zone.Battlefield);
        typeList = typeList.getValidCards(type.split(","), activate.getController(), activate);
        if (target != null && target.getController().isComputer() && typeList.contains(target)) {
            // bounce
            // the
            // card
            // we're
            // pumping
            typeList.remove(target);
        }

        if (typeList.size() < amount) {
            return null;
        }

        CardListUtil.sortAttackLowFirst(typeList);
        CardList returnList = new CardList();

        for (int i = 0; i < amount; i++) {
            returnList.add(typeList.get(i));
        }
        return returnList;
    }

    /**
     * <p>
     * getPossibleAttackers.
     * </p>
     * 
     * @return a {@link forge.CardList} object.
     */
    public static CardList getPossibleAttackers() {
        CardList list = AllZone.getComputerPlayer().getCardsIn(Zone.Battlefield);
        list = list.filter(new CardListFilter() {
            public boolean addCard(final Card c) {
                return CombatUtil.canAttack(c);
            }
        });
        return list;
    }

    /**
     * <p>
     * getAttackers.
     * </p>
     * 
     * @return a {@link forge.Combat} object.
     */
    public static Combat getAttackers() {
        ComputerUtilAttack2 att = new ComputerUtilAttack2(AllZone.getComputerPlayer().getCardsIn(Zone.Battlefield),
                AllZone.getHumanPlayer().getCardsIn(Zone.Battlefield), AllZone.getHumanPlayer().getLife());

        return att.getAttackers();
    }

    /**
     * <p>
     * getBlockers.
     * </p>
     * 
     * @return a {@link forge.Combat} object.
     */
    public static Combat getBlockers() {
        CardList blockers = AllZone.getComputerPlayer().getCardsIn(Zone.Battlefield);

        return ComputerUtilBlock2.getBlockers(AllZone.getCombat(), blockers);
    }

    /**
     * <p>
     * sortSpellAbilityByCost.
     * </p>
     * 
     * @param sa
     *            an array of {@link forge.card.spellability.SpellAbility}
     *            objects.
     */
    static void sortSpellAbilityByCost(final SpellAbility[] sa) {
        // sort from highest cost to lowest
        // we want the highest costs first
        Comparator<SpellAbility> c = new Comparator<SpellAbility>() {
            public int compare(final SpellAbility a, final SpellAbility b) {
                int a1 = CardUtil.getConvertedManaCost(a);
                int b1 = CardUtil.getConvertedManaCost(b);

                // puts creatures in front of spells
                if (a.getSourceCard().isCreature()) {
                    a1 += 1;
                }

                if (b.getSourceCard().isCreature()) {
                    b1 += 1;
                }

                // sort planeswalker abilities for ultimate
                if (a.getRestrictions().getPlaneswalker() && b.getRestrictions().getPlaneswalker()) {
                    if (a.getAbilityFactory() != null && a.getAbilityFactory().getMapParams().containsKey("Ultimate")) {
                        a1 += 1;
                    } else if (b.getAbilityFactory() != null
                            && b.getAbilityFactory().getMapParams().containsKey("Ultimate")) {
                        b1 += 1;
                    }
                }

                return b1 - a1;
            }
        }; // Comparator
        Arrays.sort(sa, c);
    } // sortSpellAbilityByCost()

    /**
     * <p>
     * sacrificePermanents.
     * </p>
     * 
     * @param amount
     *            a int.
     * @param list
     *            a {@link forge.CardList} object.
     * @return the card list
     */
    public static CardList sacrificePermanents(final int amount, final CardList list) {
        CardList sacList = new CardList();
        // used in Annihilator and AF_Sacrifice
        int max = list.size();
        if (max > amount) {
            max = amount;
        }

        CardListUtil.sortCMC(list);
        list.reverse();

        for (int i = 0; i < max; i++) {
            // TODO: use getWorstPermanent() would be wayyyy better

            Card c;
            if (list.getNotType("Creature").size() == 0) {
                c = CardFactoryUtil.getWorstCreatureAI(list);
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
            sacList.add(c);
            AllZone.getGameAction().sacrifice(c);
        }
        return sacList;
    }

    /**
     * <p>
     * canRegenerate.
     * </p>
     * 
     * @param card
     *            a {@link forge.Card} object.
     * @return a boolean.
     */
    public static boolean canRegenerate(final Card card) {

        if (card.hasKeyword("CARDNAME can't be regenerated.")) {
            return false;
        }

        Player controller = card.getController();
        CardList l = controller.getCardsIn(Zone.Battlefield);
        for (Card c : l) {
            for (SpellAbility sa : c.getSpellAbility()) {
                // This try/catch should fix the "computer is thinking" bug
                try {
                    AbilityFactory af = sa.getAbilityFactory();

                    if (!sa.isAbility() || af == null || !af.getAPI().equals("Regenerate")) {
                        continue; // Not a Regenerate ability
                    }

                    // sa.setActivatingPlayer(controller);
                    if (!(sa.canPlay() && ComputerUtil.canPayCost(sa, controller))) {
                        continue; // Can't play ability
                    }

                    HashMap<String, String> mapParams = af.getMapParams();

                    Target tgt = sa.getTarget();
                    if (tgt != null) {
                        if (AllZoneUtil.getCardsIn(Zone.Battlefield)
                                .getValidCards(tgt.getValidTgts(), controller, sa.getSourceCard()).contains(card)) {
                            return true;
                        }
                    } else if (AbilityFactory.getDefinedCards(sa.getSourceCard(), mapParams.get("Defined"), sa)
                            .contains(card)) {
                        return true;
                    }

                } catch (Exception ex) {
                    showError(ex, "There is an error in the card code for %s:%n", c.getName(), ex.getMessage());
                }
            }
        }
        return false;
    }

    /**
     * <p>
     * possibleDamagePrevention.
     * </p>
     * 
     * @param card
     *            a {@link forge.Card} object.
     * @return a int.
     */
    public static int possibleDamagePrevention(final Card card) {

        int prevented = 0;

        Player controller = card.getController();
        CardList l = controller.getCardsIn(Zone.Battlefield);
        for (Card c : l) {
            for (SpellAbility sa : c.getSpellAbility()) {
                // if SA is from AF_Counter don't add to getPlayable
                // This try/catch should fix the "computer is thinking" bug
                try {
                    if (sa.getAbilityFactory() != null && sa.isAbility()) {
                        AbilityFactory af = sa.getAbilityFactory();
                        HashMap<String, String> mapParams = af.getMapParams();
                        if (mapParams.get("AB").equals("PreventDamage") && sa.canPlay()
                                && ComputerUtil.canPayCost(sa, controller)) {
                            if (AbilityFactory.getDefinedCards(sa.getSourceCard(), mapParams.get("Defined"), sa)
                                    .contains(card)) {
                                prevented += AbilityFactory.calculateAmount(af.getHostCard(), mapParams.get("Amount"),
                                        sa);
                            }
                            Target tgt = sa.getTarget();
                            if (tgt != null) {
                                if (AllZoneUtil.getCardsIn(Zone.Battlefield)
                                        .getValidCards(tgt.getValidTgts(),
                                                controller, af.getHostCard()).contains(card)) {
                                    prevented += AbilityFactory.calculateAmount(af.getHostCard(),
                                            mapParams.get("Amount"), sa);
                                }

                            }
                        }
                    }
                } catch (Exception ex) {
                    showError(ex, "There is an error in the card code for %s:%n", c.getName(), ex.getMessage());
                }
        }
        }
        return prevented;
    }
}
