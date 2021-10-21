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

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.collect.*;
import org.apache.commons.lang3.StringUtils;

import forge.card.MagicColor;
import forge.card.mana.ManaCost;
import forge.card.mana.ManaCostParser;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardFactoryUtil;
import forge.game.card.CardPlayOption;
import forge.game.card.CardPlayOption.PayManaCost;
import forge.game.card.CounterType;
import forge.game.cost.Cost;
import forge.game.keyword.Keyword;
import forge.game.keyword.KeywordInterface;
import forge.game.keyword.KeywordsChange;
import forge.game.player.Player;
import forge.game.player.PlayerCollection;
import forge.game.player.PlayerController;
import forge.game.replacement.ReplacementEffect;
import forge.game.replacement.ReplacementHandler;
import forge.game.replacement.ReplacementLayer;
import forge.game.spellability.AbilityManaPart;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.AlternativeCost;
import forge.game.spellability.OptionalCost;
import forge.game.spellability.OptionalCostValue;
import forge.game.spellability.Spell;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityRestriction;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerHandler;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;
import forge.util.Lang;
import forge.util.TextUtil;


/**
 * <p>
 * GameActionUtil class.
 * </p>
 *
 * @author Forge
 * @version $Id$
 */
public final class GameActionUtil {

    private GameActionUtil() {
        throw new AssertionError();
    }

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
        final List<SpellAbility> alternatives = Lists.newArrayList();

        Card source = sa.getHostCard();
        final Game game = source.getGame();

        if (sa.isSpell() && !source.isInZone(ZoneType.Battlefield)) {
            boolean lkicheck = false;

            Card newHost = ((Spell)sa).getAlternateHost(source);
            if (newHost != null) {
                source = newHost;
                lkicheck = true;
            }

            if (lkicheck) {
                // double freeze tracker, so it doesn't update view
                game.getTracker().freeze();
                source.clearChangedCardKeywords(false);
                CardCollection preList = new CardCollection(source);
                game.getAction().checkStaticAbilities(false, Sets.newHashSet(source), preList);
            }

            for (CardPlayOption o : source.mayPlay(activator)) {
                // do not appear if it can be cast with SorcerySpeed
                if (o.getAbility().hasParam("MayPlayNotSorcerySpeed") && activator.couldCastSorcery(sa)) {
                    continue;
                }
                // non basic are only allowed if PayManaCost is yes
                if ((!sa.isBasicSpell() || (sa.costHasManaX() && !sa.getPayCosts().getCostMana().canXbe0())) && o.getPayManaCost() == PayManaCost.NO) {
                    continue;
                }
                final Card host = o.getHost();

                SpellAbility newSA = null;

                boolean changedManaCost = false;
                if (o.getPayManaCost() == PayManaCost.NO) {
                    newSA = sa.copyWithNoManaCost(activator);
                    newSA.setBasicSpell(false);
                    changedManaCost = true;
                } else if (o.getAltManaCost() != null) {
                    newSA = sa.copyWithManaCostReplaced(activator, o.getAltManaCost());
                    newSA.setBasicSpell(false);
                    changedManaCost = true;
                } else {
                    newSA = sa.copy(activator);
                }
                final SpellAbilityRestriction sar = newSA.getRestrictions();
                if (o.isWithFlash()) {
                    sar.setInstantSpeed(true);
                }
                sar.setZone(null);
                newSA.setMayPlay(o.getAbility());

                if (changedManaCost) {
                    if ("0".equals(sa.getParam("ActivationLimit")) && sa.getHostCard().getManaCost().isNoCost()) {
                        sar.setLimitToCheck(null);
                    }
                }

                final StringBuilder sb = new StringBuilder(sa.getDescription());
                if (!source.equals(host)) {
                    sb.append(" by ");
                    if ((host.isImmutable()) && host.getEffectSource() != null) {
                        sb.append(host.getEffectSource());
                    } else {
                        sb.append(host);
                    }
                }
                if (o.getAbility().hasParam("MayPlayText")) {
                    sb.append(" (").append(o.getAbility().getParam("MayPlayText")).append(")");
                }
                sb.append(o.toString(false));
                newSA.setDescription(sb.toString());
                alternatives.add(newSA);
            }

            // need to be done there before static abilities does reset the card
            if (sa.isBasicSpell()) {
                for (final KeywordInterface inst : source.getKeywords()) {
                    final String keyword = inst.getOriginal();

                    if (keyword.startsWith("Disturb")) {
                        final String[] k = keyword.split(":");
                        final Cost disturbCost = new Cost(k[1], true);

                        final SpellAbility newSA = sa.copyWithManaCostReplaced(activator, disturbCost);
                        newSA.setActivatingPlayer(activator);

                        newSA.putParam("PrecostDesc", "Disturb —");
                        newSA.putParam("CostDesc", disturbCost.toString());

                        // makes new SpellDescription
                        final StringBuilder desc = new StringBuilder();
                        desc.append(newSA.getCostDescription());
                        desc.append("(").append(inst.getReminderText()).append(")");
                        newSA.setDescription(desc.toString());
                        newSA.putParam("AfterDescription", "(Disturbed)");

                        newSA.setAlternativeCost(AlternativeCost.Disturb);
                        newSA.getRestrictions().setZone(ZoneType.Graveyard);
                        newSA.setCardState(source.getAlternateState());

                        alternatives.add(newSA);
                    } else if (keyword.startsWith("Escape")) {
                        final String[] k = keyword.split(":");
                        final Cost escapeCost = new Cost(k[1], true);

                        final SpellAbility newSA = sa.copyWithManaCostReplaced(activator, escapeCost);
                        newSA.setActivatingPlayer(activator);

                        newSA.putParam("PrecostDesc", "Escape—");
                        newSA.putParam("CostDesc", escapeCost.toString());

                        // makes new SpellDescription
                        final StringBuilder desc = new StringBuilder();
                        desc.append(newSA.getCostDescription());
                        desc.append("(").append(inst.getReminderText()).append(")");
                        newSA.setDescription(desc.toString());
                        newSA.putParam("AfterDescription", "(Escaped)");

                        newSA.setAlternativeCost(AlternativeCost.Escape);
                        newSA.getRestrictions().setZone(ZoneType.Graveyard);

                        alternatives.add(newSA);
                    } else if (keyword.startsWith("Flashback")) {
                        // if source has No Mana cost, and flashback doesn't have own one,
                        // flashback can't work
                        if (keyword.equals("Flashback") && source.getManaCost().isNoCost()) {
                            continue;
                        }

                        SpellAbility flashback = null;

                        // there is a flashback cost (and not the cards cost)
                        if (keyword.contains(":")) { // K:Flashback:Cost:ExtraParams:ExtraDescription
                            final String[] k = keyword.split(":");
                            flashback = sa.copyWithManaCostReplaced(activator, new Cost(k[1], false));
                            String extraParams =  k.length > 2 ? k[2] : "";
                            if (!extraParams.isEmpty()) {
                                for (Map.Entry<String, String> param : AbilityFactory.getMapParams(extraParams).entrySet()) {
                                    flashback.putParam(param.getKey(), param.getValue());
                                }
                            }
                        } else { // same cost as original (e.g. Otaria plane)
                            flashback = sa.copy(activator);
                        }
                        flashback.setAlternativeCost(AlternativeCost.Flashback);
                        flashback.getRestrictions().setZone(ZoneType.Graveyard);
                        alternatives.add(flashback);
                    } else if (keyword.startsWith("Foretell")) {
                        // Foretell cast only from Exile
                        if (!source.isInZone(ZoneType.Exile) || !source.isForetold() || source.isForetoldThisTurn() ||
                                !activator.equals(source.getOwner())) {
                            continue;
                        }
                        // skip this part for foretell by external source
                        if (keyword.equals("Foretell")) {
                            continue;
                        }

                        final SpellAbility foretold = sa.copy(activator);
                        foretold.setAlternativeCost(AlternativeCost.Foretold);
                        foretold.getRestrictions().setZone(ZoneType.Exile);
                        foretold.putParam("AfterDescription", "(Foretold)");

                        final String[] k = keyword.split(":");
                        foretold.setPayCosts(new Cost(k[1], false));

                        alternatives.add(foretold);
                    }
                }

                // foretell by external source
                if (source.isForetoldByEffect() && source.isInZone(ZoneType.Exile) && activator.equals(source.getOwner())
                        && source.isForetold() && !source.isForetoldThisTurn() && !source.getManaCost().isNoCost()) {
                    // Its foretell cost is equal to its mana cost reduced by {2}.
                    final SpellAbility foretold = sa.copy(activator);
                    foretold.putParam("ReduceCost", "2");
                    foretold.setAlternativeCost(AlternativeCost.Foretold);
                    foretold.getRestrictions().setZone(ZoneType.Exile);
                    foretold.putParam("AfterDescription", "(Foretold)");
                    alternatives.add(foretold);
                }
            }

            // reset static abilities
            if (lkicheck) {
                game.getAction().checkStaticAbilities(false);
                // clear delayed changes, this check should not have updated the view
                game.getTracker().clearDelayed();
                // need to unfreeze tracker
                game.getTracker().unfreeze();
            }
        }

        if (sa.isManaAbility() && sa.isActivatedAbility() && activator.hasKeyword("Piracy") && source.isLand() && source.isInPlay() && !activator.equals(source.getController()) && sa.getPayCosts().hasTapCost()) {
            SpellAbility newSA = sa.copy(activator);
            // to bypass Activator restriction, set Activator to Player
            newSA.getRestrictions().setActivator("Player");

            // extra Mana restriction to only Spells
            for (AbilityManaPart mp : newSA.getAllManaParts()) {
                mp.setExtraManaRestriction("Spell");
            }
            alternatives.add(newSA);
        }

        // below are for some special cases of activated abilities
        if (sa.isCycling() && activator.hasKeyword("CyclingForZero")) {
            for (final KeywordInterface inst : source.getKeywords()) {
                // need to find the correct Keyword from which this Ability is from
                if (!inst.getAbilities().contains(sa)) {
                    continue;
                }

                // set the cost to this directly to bypass non mana cost
                final SpellAbility newSA = sa.copyWithDefinedCost("Discard<1/CARDNAME>");
                newSA.setActivatingPlayer(activator);
                newSA.putParam("CostDesc", ManaCostParser.parse("0"));

                // need to build a new Keyword to get better Reminder Text
                String data[] = inst.getOriginal().split(":");
                data[1] = "0";
                KeywordInterface newKi = Keyword.getInstance(StringUtils.join(data, ":"));

                // makes new SpellDescription
                final StringBuilder sb = new StringBuilder();
                sb.append(newSA.getCostDescription());
                sb.append("(").append(newKi.getReminderText()).append(")");
                newSA.setDescription(sb.toString());

                alternatives.add(newSA);
            }
        }
        if (sa.hasParam("Equip") && activator.hasKeyword("You may pay 0 rather than pay equip costs.")) {
            for (final KeywordInterface inst : source.getKeywords()) {
                // need to find the correct Keyword from which this Ability is from
                if (!inst.getAbilities().contains(sa)) {
                    continue;
                }

                // set the cost to this directly to bypass non mana cost
                SpellAbility newSA = sa.copyWithDefinedCost("0");
                newSA.setActivatingPlayer(activator);
                newSA.putParam("CostDesc", ManaCostParser.parse("0"));

                // need to build a new Keyword to get better Reminder Text
                String data[] = inst.getOriginal().split(":");
                data[1] = "0";
                KeywordInterface newKi = Keyword.getInstance(StringUtils.join(data, ":"));

                // makes new SpellDescription
                final StringBuilder sb = new StringBuilder();
                sb.append(newSA.getCostDescription());
                sb.append("(").append(newKi.getReminderText()).append(")");
                newSA.setDescription(sb.toString());

                alternatives.add(newSA);
            }
        }

        return alternatives;
    }

    public static List<OptionalCostValue> getOptionalCostValues(final SpellAbility sa) {
        final List<OptionalCostValue> costs = Lists.newArrayList();
        if (sa == null || !sa.isSpell()) {
            return costs;
        }
        final Card source = sa.getHostCard();
        for (KeywordInterface inst : source.getKeywords()) {
            final String keyword = inst.getOriginal();
            if (keyword.startsWith("Buyback")) {
                final Cost cost = new Cost(keyword.substring(8), false);
                costs.add(new OptionalCostValue(OptionalCost.Buyback, cost));
            } else if (keyword.startsWith("Entwine")) {
                String[] k = keyword.split(":");
                final Cost cost = new Cost(k[1], false);
                costs.add(new OptionalCostValue(OptionalCost.Entwine, cost));
            } else if (keyword.startsWith("Kicker")) {
                String[] sCosts = TextUtil.split(keyword.substring(6), ':');
                boolean generic = "Generic".equals(sCosts[sCosts.length - 1]);
                // If this is a "generic kicker" (Undergrowth), ignore value for kicker creations
                int numKickers = sCosts.length - (generic ? 1 : 0);
                for (int j = 0; j < numKickers; j++) {
                    final Cost cost = new Cost(sCosts[j], false);
                    OptionalCost type = null;
                    if (!generic) {
                        type = j == 0 ? OptionalCost.Kicker1 : OptionalCost.Kicker2;
                    } else {
                        type = OptionalCost.Generic;
                    }
                    costs.add(new OptionalCostValue(type, cost));
                }
            } else if (keyword.equals("Retrace")) {
                if (source.isInZone(ZoneType.Graveyard)) {
                    final Cost cost = new Cost("Discard<1/Land>", false);
                    costs.add(new OptionalCostValue(OptionalCost.Retrace, cost));
                }
            } else if (keyword.equals("Jump-start")) {
                if (source.isInZone(ZoneType.Graveyard)) {
                    final Cost cost = new Cost("Discard<1/Card>", false);
                    costs.add(new OptionalCostValue(OptionalCost.Jumpstart, cost));
                }
            } else if (keyword.startsWith("MayFlashCost")) {
                String[] k = keyword.split(":");
                final Cost cost = new Cost(k[1], false);
                costs.add(new OptionalCostValue(OptionalCost.Flash, cost));
            }

            // Surge while having OptionalCost is none of them
        }
        return costs;
    }

    public static SpellAbility addOptionalCosts(final SpellAbility sa, List<OptionalCostValue> list) {
        if (sa == null || list.isEmpty()) {
            return sa;
        }
        final SpellAbility result = sa.copy();
        for (OptionalCostValue v : list) {
            result.getPayCosts().add(v.getCost());
            result.addOptionalCost(v.getType());

            // add some extra logic, try to move it to other parts
            switch (v.getType()) {
            case Retrace:
            case Jumpstart:
                result.getRestrictions().setZone(ZoneType.Graveyard);
                break;
            case Flash:
                result.getRestrictions().setInstantSpeed(true);
                break;
            default:
                break;
            }
        }
        return result;
    }

    public static List<SpellAbility> getAdditionalCostSpell(final SpellAbility sa) {
        final List<SpellAbility> abilities = Lists.newArrayList(sa);
        if (!sa.isSpell()) {
            return abilities;
        }
        final Card source = sa.getHostCard();
        for (KeywordInterface inst : source.getKeywords()) {
            final String keyword = inst.getOriginal();
            if (keyword.startsWith("AlternateAdditionalCost")) {
                final List<SpellAbility> newAbilities = Lists.newArrayList();
                String[] costs = TextUtil.split(keyword, ':');

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
                    newAbilities.add(newSA2);
                }

                abilities.clear();
                abilities.addAll(newAbilities);
            }
        }
        return abilities;
    }

    public static SpellAbility addExtraKeywordCost(final SpellAbility sa) {
        if (!sa.isSpell() || sa.isCopied()) {
            return sa;
        }
        SpellAbility result = null;
        final Card host = sa.getHostCard();
        final Game game = host.getGame();
        final Player activator = sa.getActivatingPlayer();
        final PlayerController pc = activator.getController();

        game.getAction().checkStaticAbilities(false);

        boolean reset = false;

        for (KeywordInterface ki : host.getKeywords()) {
            final String o = ki.getOriginal();
            if (o.equals("Conspire")) {
                Trigger tr = Iterables.getFirst(ki.getTriggers(), null);
                if (tr != null) {
                    final String conspireCost = "tapXType<2/Creature.SharesColorWith/" +
                        "untapped creature you control that shares a color with " + host.getName() + ">";
                    final Cost cost = new Cost(conspireCost, false);
                    String str = "Pay for Conspire? " + cost.toSimpleString();

                    boolean v = pc.addKeywordCost(sa, cost, ki, str);
                    tr.setSVar("Conspire", v ? "1" : "0");

                    if (v) {
                        if (result == null) {
                            result = sa.copy();
                        }
                        result.getPayCosts().add(cost);
                        reset = true;
                    }
                }
            } else if (o.startsWith("Replicate")) {
                Trigger tr = Iterables.getFirst(ki.getTriggers(), null);
                if (tr != null) {
                    String costStr = o.split(":")[1];
                    final Cost cost = new Cost(costStr, false);

                    String str = "Choose Amount for Replicate: " + cost.toSimpleString();

                    int v = pc.chooseNumberForKeywordCost(sa, cost, ki, str, Integer.MAX_VALUE);

                    tr.setSVar("ReplicateAmount", String.valueOf(v));
                    tr.getOverridingAbility().setSVar("ReplicateAmount", String.valueOf(v));

                    for (int i = 0; i < v; i++) {
                        if (result == null) {
                            result = sa.copy();
                        }
                        result.getPayCosts().add(cost);
                        reset = true;
                    }
                }
            }
        }

        if (host.isCreature()) {
            String kw = "As an additional cost to cast creature spells," +
                    " you may pay any amount of mana. If you do, that creature enters " +
                    "the battlefield with that many additional +1/+1 counters on it.";

            for (final Card c : activator.getZone(ZoneType.Battlefield)) {
                for (KeywordInterface ki : c.getKeywords()) {
                    if (kw.equals(ki.getOriginal())) {
                        final Cost cost = new Cost(ManaCost.ONE, false);
                        String str = "Choose Amount for " + c.getName() + ": " + cost.toSimpleString();

                        int v = pc.chooseNumberForKeywordCost(sa, cost, ki, str, Integer.MAX_VALUE);

                        if (v > 0) {
                            Card eff = createETBCountersEffect(c, host, activator, "P1P1", String.valueOf(v));

                            if (result == null) {
                                result = sa.copy();
                            }
                            result.addRollbackEffect(eff);
                            for (int i = 0; i < v; i++) {
                                result.getPayCosts().add(cost);
                            }
                        }
                    }
                }
            }
        }

        // reset active Trigger
        if (reset) {
            host.getGame().getTriggerHandler().resetActiveTriggers(false);
        }

        return result != null ? result : sa;
    }

    public static Card createETBCountersEffect(Card sourceCard, Card c, Player controller, String counter, String amount) {
        final Game game = sourceCard.getGame();
        final Card eff = new Card(game.nextCardId(), game);
        eff.setTimestamp(game.getNextTimestamp());
        eff.setName(sourceCard.getName() + "'s Effect");
        eff.setOwner(controller);

        eff.setImageKey(sourceCard.getImageKey());
        eff.setColor(MagicColor.COLORLESS);
        eff.setImmutable(true);
        // try to get the SpellAbility from the mana ability
        //eff.setEffectSource((SpellAbility)null);

        eff.addRemembered(c);

        String abStr = "DB$ PutCounter | Defined$ ReplacedCard | CounterType$ " + counter
                + " | ETB$ True | CounterNum$ " + amount;

        SpellAbility sa = AbilityFactory.getAbility(abStr, c);
        if (!StringUtils.isNumeric(amount)) {
            sa.setSVar(amount, sourceCard.getSVar(amount));
        }
        CardFactoryUtil.setupETBReplacementAbility(sa);

        String desc = "It enters the battlefield with ";
        desc += Lang.nounWithNumeral(amount, CounterType.getType(counter).getName() + " counter");
        desc += " on it.";

        String repeffstr = "Event$ Moved | ValidCard$ Card.IsRemembered | Destination$ Battlefield | Description$ " + desc;

        ReplacementEffect re = ReplacementHandler.parseReplacement(repeffstr, eff, true);
        re.setLayer(ReplacementLayer.Other);
        re.setOverridingAbility(sa);

        eff.addReplacementEffect(re);

        // Forgot Trigger
        String trig = "Mode$ ChangesZone | ValidCard$ Card.IsRemembered | Origin$ Stack | Destination$ Any | TriggerZones$ Command | Static$ True";
        String forgetEffect = "DB$ Pump | ForgetObjects$ TriggeredCard";
        String exileEffect = "DB$ ChangeZone | Defined$ Self | Origin$ Command | Destination$ Exile"
                + " | ConditionDefined$ Remembered | ConditionPresent$ Card | ConditionCompare$ EQ0";

        SpellAbility saForget = AbilityFactory.getAbility(forgetEffect, eff);
        AbilitySub saExile = (AbilitySub) AbilityFactory.getAbility(exileEffect, eff);
        saForget.setSubAbility(saExile);

        final Trigger parsedTrigger = TriggerHandler.parseTrigger(trig, eff, true);
        parsedTrigger.setOverridingAbility(saForget);
        eff.addTrigger(parsedTrigger);
        eff.updateStateForView();

        // TODO: Add targeting to the effect so it knows who it's dealing with
        game.getTriggerHandler().suppressMode(TriggerType.ChangesZone);
        game.getAction().moveTo(ZoneType.Command, eff, null);
        game.getTriggerHandler().clearSuppression(TriggerType.ChangesZone);
        
        return eff;
    }

    public static String generatedTotalMana(final SpellAbility sa) {
        StringBuilder sb = new StringBuilder();
        SpellAbility tail = sa;
        while (tail != null) {
            String value = generatedMana(tail);
            if (!value.isEmpty() && !"0".equals(value)) {
                sb.append(value).append(" ");
            }
            tail = tail.getSubAbility();
        }
        return sb.toString().trim();
    }

    public static String generatedMana(final SpellAbility sa) {
        int amount = sa.amountOfManaGenerated(false);
        AbilityManaPart abMana = sa.getManaPart();
        if (abMana == null) {
            return "";
        }
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
        } else if ((sa.getParam("Amount") != null) && (amount != AbilityUtils.calculateAmount(sa.getHostCard(),sa.getParam("Amount"), sa))) {
            sa.setUndoable(false);
        }

        final StringBuilder sb = new StringBuilder();
        if (amount <= 0) {
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

    public static CardCollectionView orderCardsByTheirOwners(Game game, CardCollectionView list, ZoneType dest, SpellAbility sa) {
        if (list.size() <= 1) {
            return list;
        }
        CardCollection completeList = new CardCollection();
        PlayerCollection players = game.getPlayers();
        // CR 613.7k use APNAP
        int indexAP = players.indexOf(game.getPhaseHandler().getPlayerTurn());
        if (indexAP != -1) {
            Collections.rotate(players, - indexAP);
        }
        for (Player p : players) {
            CardCollection subList = new CardCollection();
            for (Card c : list) {
                Player decider = dest == ZoneType.Battlefield ? c.getController() : c.getOwner();
                if (decider.equals(p)) {
                    subList.add(c);
                }
            }
            CardCollectionView subListView = subList;
            if (subList.size() > 1) {
                subListView = p.getController().orderMoveToZoneList(subList, dest, sa);
            }
            completeList.addAll(subListView);
        }
        return completeList;
    }

    public static void checkStaticAfterPaying(Card c) {
        Table<Long, Long, KeywordsChange> oldKW = TreeBasedTable.create((TreeBasedTable<Long, Long, KeywordsChange>) c.getChangedCardKeywords());
        // this should be the last time checkStaticAbilities is called before SpellCast triggers to
        // - setup Cascade dependent on high enough X (Imoti)
        // - remove Replicate if Djinn Illuminatus gets sacrificed as payment
        // because this will remove the payment SVars for Replicate we need to restore them
        c.getGame().getAction().checkStaticAbilities(false);

        Table<Long, Long, KeywordsChange> updatedKW = c.getChangedCardKeywords();
        for (Table.Cell<Long, Long, KeywordsChange> entry : oldKW.cellSet()) {
            for (KeywordInterface ki : entry.getValue().getKeywords()) {
                // check if this keyword existed previously
                if ((ki.getOriginal().startsWith("Replicate") || ki.getOriginal().startsWith("Conspire")) && updatedKW.get(entry.getRowKey(), entry.getColumnKey()) != null) {
                    updatedKW.put(entry.getRowKey(), entry.getColumnKey(), oldKW.get(entry.getRowKey(), entry.getColumnKey()));
                }
            }
        }
        c.updateKeywords();

        c.getGame().getTriggerHandler().resetActiveTriggers();
    }

}
