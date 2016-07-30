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
package forge.game;

import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import forge.card.MagicColor;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityFactory.AbilityRecordType;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.card.CardPlayOption;
import forge.game.card.CardPredicates;
import forge.game.card.CardPlayOption.PayManaCost;
import forge.game.cost.Cost;
import forge.game.mana.ManaCostBeingPaid;
import forge.game.player.Player;
import forge.game.spellability.*;
import forge.game.zone.ZoneType;
import forge.util.TextUtil;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * <p>
 * GameActionUtil class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public final class GameActionUtil {
    // Cache these instead of generating them on the fly, to avoid excessive allocations every time
    // static abilities are checked.
    @SuppressWarnings("unchecked")
    private static final Map<String, String>[] BASIC_LAND_ABILITIES_PARAMS = new Map[MagicColor.WUBRG.length];
    private static final AbilityRecordType[] BASIC_LAND_ABILITIES_TYPES = new AbilityRecordType[MagicColor.WUBRG.length];
    static {
        for (int i = 0; i < MagicColor.WUBRG.length; i++ ) {
            String color = MagicColor.toShortString(MagicColor.WUBRG[i]);
            String abString  = "AB$ Mana | Cost$ T | Produced$ " + color +
                    " | SpellDescription$ Add {" + color + "} to your mana pool.";
            Map<String, String> mapParams = AbilityFactory.getMapParams(abString);
            BASIC_LAND_ABILITIES_PARAMS[i] = mapParams;
            BASIC_LAND_ABILITIES_TYPES[i] = AbilityRecordType.getRecordType(mapParams);
        }
    }

    private GameActionUtil() {
        throw new AssertionError();
    }

    // restricted to combat damage, restricted to players
    /**
     * <p>
     * executeCombatDamageToPlayerEffects.
     * </p>
     * 
     * @param player
     *            a {@link forge.game.player.Player} object.
     * @param c
     *            a {@link forge.game.card.Card} object.
     * @param damage
     *            a int.
     */
    public static void executeCombatDamageToPlayerEffects(final Player player, final Card c, final int damage) {

        if (damage <= 0) {
            return;
        }

        for (final String key : c.getKeywords()) {
            if (!key.startsWith("Poisonous ")) continue;
            final String[] k = key.split(" ", 2);
            final int poison = Integer.parseInt(k[1]);
            // Now can be copied by Strionic Resonator
            String effect = "AB$ Poison | Cost$ 0 | Defined$ PlayerNamed_" + player.getName() + " | Num$ " + k[1];
            SpellAbility ability = AbilityFactory.getAbility(effect, c);

            final StringBuilder sb = new StringBuilder();
            sb.append(c);
            sb.append(" - Poisonous: ");
            sb.append(player);
            sb.append(" gets ").append(poison).append(" poison counter");
            if (poison != 1) {
                sb.append("s");
            }
            sb.append(".");

            ability.setActivatingPlayer(c.getController());
            ability.setDescription(sb.toString());
            ability.setStackDescription(sb.toString());
            ability.setTrigger(true);

            player.getGame().getStack().addSimultaneousStackEntry(ability);

        }

        c.getDamageHistory().registerCombatDamage(player);
    } // executeCombatDamageToPlayerEffects

    /**
     * Gets the st land mana abilities.
     * @param game
     * 
     * @return the stLandManaAbilities
     */
    public static void grantBasicLandsManaAbilities(List<Card> lands) {
        // remove all abilities granted by this Command
        for (final Card land : lands) {
            List<SpellAbility> origManaAbs = Lists.newArrayList(land.getManaAbilities());
            // will get comodification exception without a different list
            for (final SpellAbility sa : origManaAbs) {
                if (sa.isBasicLandAbility()) {
                    land.getCurrentState().removeManaAbility(sa);
                }
            }
        }

        // add all appropriate mana abilities based on current types
        for (int i = 0; i < MagicColor.WUBRG.length; i++ ) {
            String landType = MagicColor.Constant.BASIC_LANDS.get(i);
            Map<String, String> mapParams = BASIC_LAND_ABILITIES_PARAMS[i];
            AbilityRecordType type = BASIC_LAND_ABILITIES_TYPES[i];
            for (final Card land : lands) {
                if (land.getType().hasSubtype(landType)) {
                    final SpellAbility sa = AbilityFactory.getAbility(mapParams, type, land);
                    sa.setBasicLandAbility(true);
                    land.getCurrentState().addManaAbility(sa);
                }
            }
        }
    } // stLandManaAbilities

    /**
     * <p>
     * Find the alternative costs to a {@link SpellAbility}.
     * </p>
     * 
     * @param sa
     *            a {@link SpellAbility}.
     * @param activator
     *            the {@link Player} for which to calculate available
     * @return a {@link List} of {@link SpellAbility} objects, each representing
     *         a possible alternative cost the provided activator can use to pay
     *         the provided {@link SpellAbility}.
     */
    public static final List<SpellAbility> getAlternativeCosts(final SpellAbility sa, final Player activator) {
        final List<SpellAbility> alternatives = new ArrayList<SpellAbility>();

        final Card source = sa.getHostCard();

        if (sa.isSpell()) {
            for (CardPlayOption o : source.mayPlay(activator)) {
                // non basic are only allowed if PayManaCost is yes
                if (!sa.isBasicSpell() && o.getPayManaCost() == PayManaCost.NO) {
                    continue;
                }
                final Card host = o.getHost();

                final SpellAbility newSA = sa.copy();
                final SpellAbilityRestriction sar = new SpellAbilityRestriction();
                sar.setVariables(sa.getRestrictions());
                if (o.isWithFlash()) {
                	sar.setInstantSpeed(true);
                }
                sar.setZone(null);
                newSA.setRestrictions(sar);
                newSA.setMayPlayHost(host);
                if (o.getPayManaCost() == PayManaCost.NO) {
                    newSA.setBasicSpell(false);
                    newSA.setPayCosts(newSA.getPayCosts().copyWithNoMana());
                }

                final StringBuilder sb = new StringBuilder(sa.getDescription());
                if (!source.equals(host)) {
                    sb.append(" by ");
                    if (host.isEmblem() || host.getType().hasSubtype("Effect")) {
                        sb.append(host.getEffectSource());
                    } else {
                        sb.append(host);
                    }
                }
                sb.append(o.toString(false));
                newSA.setDescription(sb.toString());
                alternatives.add(newSA);
            }
        }

        if (!sa.isBasicSpell()) {
            return alternatives;
        }

        for (final String keyword : source.getKeywords()) {
            if (sa.isSpell() && keyword.startsWith("Flashback")) {
                final SpellAbility flashback = sa.copy();
                flashback.setFlashBackAbility(true);
                SpellAbilityRestriction sar = new SpellAbilityRestriction();
                sar.setVariables(sa.getRestrictions());
                sar.setZone(ZoneType.Graveyard);
                flashback.setRestrictions(sar);

                // there is a flashback cost (and not the cards cost)
                if (!keyword.equals("Flashback")) {
                    flashback.setPayCosts(new Cost(keyword.substring(10), false));
                }
                alternatives.add(flashback);
            }
            if (sa.isSpell() && keyword.startsWith("Alternative Cost")) {
                final SpellAbility newSA = sa.copy();
                newSA.setBasicSpell(false);
                String kw = keyword;
                if (keyword.contains("ConvertedManaCost")) {
                    final String cmc = Integer.toString(sa.getHostCard().getCMC());
                    kw = keyword.replace("ConvertedManaCost", cmc);
                }
                final Cost cost = new Cost(kw.substring(17), false).add(newSA.getPayCosts().copyWithNoMana());
                newSA.setPayCosts(cost);
                newSA.setDescription(sa.getDescription() + " (by paying " + cost.toSimpleString() + " instead of its mana cost)");
                alternatives.add(newSA);
            }
            if (sa.isSpell() && keyword.equals("You may cast CARDNAME as though it had flash if you pay 2 more to cast it.")) {
                final SpellAbility newSA = sa.copy();
                newSA.setBasicSpell(false);
                ManaCostBeingPaid newCost = new ManaCostBeingPaid(source.getManaCost());
                newCost.increaseGenericMana(2);
                final Cost actualcost = new Cost(newCost.toManaCost(), false);
                newSA.setPayCosts(actualcost);
                SpellAbilityRestriction sar = new SpellAbilityRestriction();
                sar.setVariables(sa.getRestrictions());
                sar.setInstantSpeed(true);
                newSA.setRestrictions(sar);
                newSA.setDescription(sa.getDescription() + " (by paying " + actualcost.toSimpleString() + " instead of its mana cost)");
                alternatives.add(newSA);
            }
            if (sa.isSpell() && keyword.endsWith(" offering")) {
                final String offeringType = keyword.split(" ")[0];
                List<Card> canOffer = CardLists.filter(sa.getHostCard().getController().getCardsIn(ZoneType.Battlefield),
                        CardPredicates.isType(offeringType));
                if (source.getController().hasKeyword("You can't sacrifice creatures to cast spells or activate abilities.")) {
                    canOffer = CardLists.getNotType(canOffer, "Creature");
                }
                if (!canOffer.isEmpty()) {
                    final SpellAbility newSA = sa.copy();
                    SpellAbilityRestriction sar = new SpellAbilityRestriction();
                    sar.setVariables(sa.getRestrictions());
                    sar.setInstantSpeed(true);
                    newSA.setRestrictions(sar);
                    newSA.setBasicSpell(false);
                    newSA.setIsOffering(true);
                    newSA.setPayCosts(sa.getPayCosts());
                    newSA.setDescription(sa.getDescription() + " (" + offeringType + " offering)");
                    alternatives.add(newSA);
                }
            }
            if (sa.isSpell() && keyword.startsWith("Emerge")) {
                List<Card> canEmerge = sa.getHostCard().getController().getCreaturesInPlay();
                if (source.getController().hasKeyword("You can't sacrifice creatures to cast spells or activate abilities.")) {
                    continue;
                }
                if (!canEmerge.isEmpty()) {
                    final SpellAbility newSA = sa.copy();
                    SpellAbilityRestriction sar = new SpellAbilityRestriction();
                    sar.setVariables(sa.getRestrictions());
                    newSA.setRestrictions(sar);
                    newSA.setBasicSpell(false);
                    newSA.setIsEmerge(true);
                    newSA.setPayCosts(new Cost(keyword.substring(7), false));
                    newSA.setDescription(sa.getDescription() + " (Emerge)");
                    alternatives.add(newSA);
                }
            }
            if (sa.hasParam("Equip") && sa instanceof AbilityActivated && keyword.equals("EquipInstantSpeed")) {
                final SpellAbility newSA = ((AbilityActivated) sa).getCopy();
                SpellAbilityRestriction sar = new SpellAbilityRestriction();
                sar.setVariables(sa.getRestrictions());
                sar.setSorcerySpeed(false);
                sar.setInstantSpeed(true);
                newSA.setRestrictions(sar);
                newSA.setDescription(sa.getDescription() + " (you may activate any time you could cast an instant )");
                alternatives.add(newSA);
            }
        }
        return alternatives;
    }

    /**
     * get optional additional costs.
     * 
     * @param original
     *            the original sa
     * @return an ArrayList<SpellAbility>.
     */
    public static List<SpellAbility> getOptionalCosts(final SpellAbility original) {
        final List<SpellAbility> abilities = new ArrayList<SpellAbility>();

        final Card source = original.getHostCard();
        abilities.add(original);
        if (!original.isSpell()) {
            return abilities;
        }

        // Buyback, Kicker
        for (String keyword : source.getKeywords()) {
            if (keyword.startsWith("AlternateAdditionalCost")) {
                final List<SpellAbility> newAbilities = new ArrayList<SpellAbility>();
                String[] costs = TextUtil.split(keyword, ':');
                for (SpellAbility sa : abilities) {
                    final SpellAbility newSA = sa.copy();
                    newSA.setBasicSpell(false);

                    final Cost cost1 = new Cost(costs[1], false);
                    newSA.setDescription(sa.getDescription() + " (Additional cost " + cost1.toSimpleString() + ")");
                    newSA.setPayCosts(cost1.add(sa.getPayCosts()));
                    if (newSA.canPlay()) {
                        newAbilities.add(newSA);
                    }

                    //second option
                    final SpellAbility newSA2 = sa.copy();
                    newSA2.setBasicSpell(false);

                    final Cost cost2 = new Cost(costs[2], false);
                    newSA2.setDescription(sa.getDescription() + " (Additional cost " + cost2.toSimpleString() + ")");
                    newSA2.setPayCosts(cost2.add(sa.getPayCosts()));
                    if (newSA2.canPlay()) {
                        newAbilities.add(newAbilities.size(), newSA2);
                    }
                }
                abilities.clear();
                abilities.addAll(newAbilities);
            } else if (keyword.startsWith("Buyback")) {
                for (int i = 0; i < abilities.size(); i++) {
                    final SpellAbility newSA = abilities.get(i).copy();
                    newSA.setBasicSpell(false);
                    newSA.setPayCosts(new Cost(keyword.substring(8), false).add(newSA.getPayCosts()));
                    newSA.setDescription(newSA.getDescription() + " (with Buyback)");
                    newSA.addOptionalCost(OptionalCost.Buyback);
                    if (newSA.canPlay()) {
                        abilities.add(i, newSA);
                        i++;
                    }
                }
            } else if (keyword.startsWith("Entwine")) {
                for (int i = 0; i < abilities.size(); i++) {
                	final SpellAbility newSA = abilities.get(i).copy();
            		SpellAbility entwine = AbilityFactory.buildEntwineAbility(newSA);
            		entwine.setPayCosts(new Cost(keyword.substring(8), false).add(newSA.getPayCosts()));
            		entwine.addOptionalCost(OptionalCost.Entwine);
                	if (newSA.canPlay()) {
                        abilities.add(i, entwine);
                        i++;
                    }
                }
            } else if (keyword.startsWith("Kicker")) {
                for (int i = 0; i < abilities.size(); i++) {
                    String[] sCosts = TextUtil.split(keyword.substring(7), ':');
                    boolean generic = sCosts[sCosts.length - 1].trim().equals("Generic");
                    int iUnKicked = i;
                    // If this is a "generic kicker" (Undergrowth), ignore value for kicker creations
                    int numKickers = sCosts.length - (generic ? 1 : 0);
                    for (int j = 0; j < numKickers; j++) {
                        final SpellAbility newSA = abilities.get(iUnKicked).copy();
                        newSA.setBasicSpell(false);
                        final Cost cost = new Cost(sCosts[j], false);
                        newSA.setPayCosts(cost.add(newSA.getPayCosts()));
                        if (!generic) {
                            newSA.setDescription(newSA.getDescription() + " (Kicker " + cost.toSimpleString() + ")");
                            newSA.addOptionalCost(j == 0 ? OptionalCost.Kicker1 : OptionalCost.Kicker2);
                        } else {
                            newSA.setDescription(newSA.getDescription() + " (Optional " + cost.toSimpleString() + ")");
                            newSA.addOptionalCost(OptionalCost.Generic);
                        }
                        if (newSA.canPlay()) {
                            abilities.add(i, newSA);
                            i++;
                            iUnKicked++;
                        }
                    }
                    if (numKickers == 2) { // case for both kickers - it's hardcoded since they never have more than 2 kickers
                        final SpellAbility newSA = abilities.get(iUnKicked).copy();
                        newSA.setBasicSpell(false);
                        final Cost cost1 = new Cost(sCosts[0], false);
                        final Cost cost2 = new Cost(sCosts[1], false);
                        newSA.setDescription(newSA.getDescription() + String.format(" (Both kickers: %s and %s)", cost1.toSimpleString(), cost2.toSimpleString()));
                        newSA.setPayCosts(cost2.add(cost1.add(newSA.getPayCosts())));
                        newSA.addOptionalCost(OptionalCost.Kicker1);
                        newSA.addOptionalCost(OptionalCost.Kicker2);
                        if (newSA.canPlay()) {
                            abilities.add(i, newSA);
                            i++;
                        }
                    }
                }
            }
        }

        if (source.hasKeyword("Conspire")) {
            int amount = source.getAmountOfKeyword("Conspire");
            for (int kwInstance = 1; kwInstance <= amount; kwInstance++) {
                for (int i = 0; i < abilities.size(); i++) {
                    final SpellAbility newSA = abilities.get(i).copy();
                    newSA.setBasicSpell(false);
                    final String conspireCost = "tapXType<2/Creature.SharesColorWith/untapped creature you control that shares a color with " + source.getName() + ">";
                    newSA.setPayCosts(new Cost(conspireCost, false).add(newSA.getPayCosts()));
                    final String tag = kwInstance > 1 ? " (Conspire " + kwInstance + ")" : " (Conspire)";
                    newSA.setDescription(newSA.getDescription() + tag);
                    newSA.addOptionalCost(OptionalCost.Conspire);
                    newSA.addConspireInstance();
                    if (newSA.canPlay()) {
                        abilities.add(++i, newSA);
                    }
                }
            }
        }

        // Splice
        final List<SpellAbility> newAbilities = new ArrayList<SpellAbility>();
        for (SpellAbility sa : abilities) {
            if (sa.isSpell() && sa.getHostCard().getType().hasStringType("Arcane") && sa.getApi() != null ) {
                newAbilities.addAll(GameActionUtil.getSpliceAbilities(sa));
            }
        }
        abilities.addAll(newAbilities);
        return abilities;
    }

    /**
     * <p>
     * getSpliceAbilities.
     * </p>
     * 
     * @param sa
     *            a SpellAbility.
     * @return an ArrayList<SpellAbility>.
     * get abilities with all Splice options
     */
    private  static final List<SpellAbility> getSpliceAbilities(SpellAbility sa) {
        List<SpellAbility> newSAs = new ArrayList<SpellAbility>();
        List<SpellAbility> allSaCombinations = new ArrayList<SpellAbility>();
        allSaCombinations.add(sa);
        Card source = sa.getHostCard();

        for (Card c : sa.getActivatingPlayer().getCardsIn(ZoneType.Hand)) {
            if (c.equals(source)) {
                continue;
            }

            String spliceKwCost = null;
            for (String keyword : c.getKeywords()) {
                if (keyword.startsWith("Splice")) {
                    spliceKwCost = keyword.substring(19);
                    break;
                }
            }

            if (spliceKwCost == null)
                continue;

            Map<String, String> params = AbilityFactory.getMapParams(c.getCurrentState().getFirstUnparsedAbility());
            AbilityRecordType rc = AbilityRecordType.getRecordType(params);
            ApiType api = rc.getApiTypeOf(params);
            AbilitySub subAbility = (AbilitySub) AbilityFactory.getAbility(AbilityRecordType.SubAbility, api, params, null, c);

            // Add the subability to all existing variants
            for (int i = 0; i < allSaCombinations.size(); ++i) {
                //create a new spell copy
                final SpellAbility newSA = allSaCombinations.get(i).copy();
                newSA.setBasicSpell(false);
                newSA.setPayCosts(new Cost(spliceKwCost, false).add(newSA.getPayCosts()));
                newSA.setDescription(newSA.getDescription() + " (Splicing " + c + " onto it)");
                newSA.addSplicedCards(c);

                // copy all subAbilities
                SpellAbility child = newSA;
                while (child.getSubAbility() != null) {
                    AbilitySub newChild = child.getSubAbility().getCopy();
                    child.setSubAbility(newChild);
                    child.setActivatingPlayer(newSA.getActivatingPlayer());
                    child = newChild;
                }

                //add the spliced ability to the end of the chain
                child.setSubAbility(subAbility);

                //set correct source and activating player to all the spliced abilities
                child = subAbility;
                while (child != null) {
                    child.setHostCard(source);
                    child.setActivatingPlayer(newSA.getActivatingPlayer());
                    child = child.getSubAbility();
                }
                newSAs.add(newSA);
                allSaCombinations.add(++i, newSA);
            }
        }
        return newSAs;
    }

    private static boolean hasUrzaLands(final Player p) {
        final CardCollectionView landsControlled = p.getCardsIn(ZoneType.Battlefield);
        return Iterables.any(landsControlled, Predicates.and(CardPredicates.isType("Urza's"), CardPredicates.isType("Mine")))
                && Iterables.any(landsControlled, Predicates.and(CardPredicates.isType("Urza's"), CardPredicates.isType("Power-Plant")))
                && Iterables.any(landsControlled, Predicates.and(CardPredicates.isType("Urza's"), CardPredicates.isType("Tower")));
    }

    public static int amountOfManaGenerated(final SpellAbility sa, boolean multiply) {
        // Calculate generated mana here for stack description and resolving

        int amount = sa.hasParam("Amount") ? AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParam("Amount"), sa) : 1;
        AbilityManaPart abMana = sa.getManaPartRecursive();

        if (sa.hasParam("Bonus")) {
            // For mana abilities that get a bonus
            // Bonus currently MULTIPLIES the base amount. Base Amounts should
            // ALWAYS be Base
            int bonus = 0;
            if (sa.getParam("Bonus").equals("UrzaLands")) {
                if (hasUrzaLands(sa.getActivatingPlayer())) {
                    bonus = Integer.parseInt(sa.getParam("BonusProduced"));
                }
            }

            amount += bonus;
        }

        if (!multiply || abMana.isAnyMana() || abMana.isComboMana() || abMana.isSpecialMana()) {
            return amount;
        } else {
            // For cards that produce like {C}{R} vs cards that produce {R}{R}.
            return abMana.mana().split(" ").length * amount;
        }
    }


    public static String generatedMana(final SpellAbility sa) {
        int amount = amountOfManaGenerated(sa, false);
        AbilityManaPart abMana = sa.getManaPart();
        String baseMana;

        if (abMana.isComboMana()) {
            baseMana = abMana.getExpressChoice();
            if (baseMana.isEmpty()) {
                baseMana = abMana.getOrigProduced();
            }
        } else if (abMana.isAnyMana()) {
            baseMana = abMana.getExpressChoice();
            if (baseMana.isEmpty()) {
                baseMana = "Any";
            }
        } else if (sa.getApi() == ApiType.ManaReflected) {
            baseMana = abMana.getExpressChoice();
        } else if (abMana.isSpecialMana()) {
            baseMana = abMana.getExpressChoice();
        } else {
            baseMana = abMana.mana();
        }

        if (sa.getSubAbility() != null) {
            // Mark SAs with subAbilities as undoable. These are generally things like damage, and other stuff
            // that's hard to track and remove
            sa.setUndoable(false);
        } else {
            try {
                if ((sa.getParam("Amount") != null) && (amount != Integer.parseInt(sa.getParam("Amount")))) {
                    sa.setUndoable(false);
                }
            } catch (final NumberFormatException n) {
                sa.setUndoable(false);
            }
        }

        final StringBuilder sb = new StringBuilder();
        if (amount == 0) {
            sb.append("0");
        } else if (abMana.isComboMana()) {
            // amount is already taken care of in resolve method for combination mana, just append baseMana
            sb.append(baseMana);
        } else {
            if (StringUtils.isNumeric(baseMana)) {
                sb.append(amount * Integer.parseInt(baseMana));
            } else {
                sb.append(baseMana);
                for (int i = 1; i < amount; i++) {
                    sb.append(" ").append(baseMana);
                }
            }
        }
        return sb.toString();
    }
} // end class GameActionUtil
