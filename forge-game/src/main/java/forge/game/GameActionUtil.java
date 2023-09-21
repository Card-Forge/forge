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
import forge.game.card.*;
import forge.game.staticability.StaticAbility;
import forge.util.Aggregates;
import org.apache.commons.lang3.StringUtils;

import forge.card.MagicColor;
import forge.card.mana.ManaCost;
import forge.card.mana.ManaCostParser;
import forge.game.ability.AbilityFactory;
import forge.game.ability.ApiType;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.CardPlayOption.PayManaCost;
import forge.game.cost.Cost;
import forge.game.cost.CostPayment;
import forge.game.keyword.Keyword;
import forge.game.keyword.KeywordInterface;
import forge.game.player.Player;
import forge.game.player.PlayerCollection;
import forge.game.player.PlayerController;
import forge.game.replacement.ReplacementEffect;
import forge.game.replacement.ReplacementHandler;
import forge.game.replacement.ReplacementLayer;
import forge.game.spellability.*;
import forge.game.staticability.StaticAbilityLayer;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerType;
import forge.game.zone.Zone;
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

        if (sa.isSpell() && source.isInPlay()) {
            return alternatives;
        }

        if (sa.isSpell()) {
            boolean lkicheck = false;

            Card newHost = ((Spell)sa).getAlternateHost(source);
            if (newHost != null) {
                source = newHost;
                lkicheck = true;
            }

            // 601.3e
            if (lkicheck) {
                // double freeze tracker, so it doesn't update view
                game.getTracker().freeze();
                source.clearStaticChangedCardKeywords(false);
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

                if (o.getAbility().hasParam("ValidAfterStack")) {
                    newSA.getMapParams().put("ValidAfterStack", o.getAbility().getParam("ValidAfterStack"));
                }

                final SpellAbilityRestriction sar = newSA.getRestrictions();
                if (o.isWithFlash()) {
                    sar.setInstantSpeed(true);
                }
                sar.setZone(null);
                newSA.setMayPlay(o);

                if (changedManaCost) {
                    if ("0".equals(sa.getParam("ActivationLimit")) && sa.getHostCard().getManaCost().isNoCost()) {
                        sar.setLimitToCheck(null);
                    }
                }

                final StringBuilder sb = new StringBuilder(sa.getDescription());
                if (!source.equals(host)) {
                    sb.append(" by ");
                    if (host.isImmutable() && host.getEffectSource() != null) {
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

                    if (keyword.startsWith("Escape")) {
                        if (!source.isInZone(ZoneType.Graveyard)) {
                            continue;
                        }

                        final String[] k = keyword.split(":");
                        final Cost escapeCost = new Cost(k[1], true);

                        final SpellAbility newSA = sa.copyWithManaCostReplaced(activator, escapeCost);

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
                        newSA.setIntrinsic(inst.isIntrinsic());

                        alternatives.add(newSA);
                    } else if (keyword.startsWith("Flashback")) {
                        if (!source.isInZone(ZoneType.Graveyard)) {
                            continue;
                        }

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
                        flashback.setKeyword(inst);
                        flashback.setIntrinsic(inst.isIntrinsic());
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
                if (source.isForetoldCostByEffect() && source.isInZone(ZoneType.Exile) && activator.equals(source.getOwner())
                        && source.isForetold() && !source.isForetoldThisTurn() && !source.getManaCost().isNoCost()) {
                    // Its foretell cost is equal to its mana cost reduced by {2}.
                    final SpellAbility foretold = sa.copy(activator);
                    Integer reduced = Math.min(2, sa.getPayCosts().getCostMana().getMana().getGenericCost());
                    foretold.putParam("ReduceCost", reduced.toString());
                    foretold.setAlternativeCost(AlternativeCost.Foretold);
                    foretold.getRestrictions().setZone(ZoneType.Exile);
                    foretold.putParam("AfterDescription", "(Foretold)");
                    alternatives.add(foretold);
                }

                // some needs to check after ability was put on the stack
                // Currently this is only checked for Toolbox and that only cares about creature spells
                if (source.isCreature() && game.getAction().hasStaticAbilityAffectingZone(ZoneType.Stack, StaticAbilityLayer.ABILITIES)) {
                    Zone oldZone = source.getLastKnownZone();
                    Card blitzCopy = source;
                    if (!source.isLKI()) {
                        blitzCopy = CardUtil.getLKICopy(source);
                    }
                    blitzCopy.setLastKnownZone(game.getStackZone());
                    lkicheck = true;

                    blitzCopy.clearStaticChangedCardKeywords(false);
                    CardCollection preList = new CardCollection(blitzCopy);
                    game.getAction().checkStaticAbilities(false, Sets.newHashSet(blitzCopy), preList);

                    // currently only for Keyword BLitz, but should affect Dash probably too
                    for (final KeywordInterface inst : blitzCopy.getKeywords(Keyword.BLITZ)) {
                        // TODO with mana value 4 or greater has blitz.
                        for (SpellAbility iSa : inst.getAbilities()) {
                            // do only non intrinsic
                            if (!iSa.isIntrinsic()) {
                                alternatives.add(iSa);
                            }
                        }
                    }
                    // need to reset to Old Zone, or canPlay would fail
                    blitzCopy.setLastKnownZone(oldZone);
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
        } else {
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
            if (sa.isEquip() && activator.hasKeyword("You may pay 0 rather than pay equip costs.")) {
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
        }
        return alternatives;
    }

    public static List<OptionalCostValue> getOptionalCostValues(final SpellAbility sa) {
        final List<OptionalCostValue> costs = Lists.newArrayList();
        if (sa == null || !sa.isSpell()) {
            return costs;
        }

        sa.clearPipsToReduce();

        Card source = sa.getHostCard();
        final Game game = source.getGame();
        boolean lkicheck = false;

        Card newHost = ((Spell)sa).getAlternateHost(source);
        if (newHost != null) {
            source = newHost;
            lkicheck = true;
        }

        // 601.3e
        if (lkicheck) {
            // double freeze tracker, so it doesn't update view
            game.getTracker().freeze();
            source.clearStaticChangedCardKeywords(false);
            CardCollection preList = new CardCollection(source);
            game.getAction().checkStaticAbilities(false, Sets.newHashSet(source), preList);
        }

        final CardCollection costSources = new CardCollection(source);
        costSources.addAll(game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES));
        for (final Card ca : costSources) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (!stAb.checkConditions("OptionalCost")) {
                    continue;
                }

                if (!stAb.matchesValidParam("ValidCard", source)) {
                    continue;
                }
                if (!stAb.matchesValidParam("ValidSA", sa)) {
                    continue;
                }
                if (!stAb.matchesValidParam("Activator", sa.getActivatingPlayer())) {
                    continue;
                }

                final Cost cost = new Cost(stAb.getParam("Cost"), false);
                if (stAb.hasParam("ReduceColor")) {
                    if (stAb.getParam("ReduceColor").equals("W")) {
                        costs.add(new OptionalCostValue(OptionalCost.ReduceW, cost));
                    } else if (stAb.getParam("ReduceColor").equals("U")) {
                        costs.add(new OptionalCostValue(OptionalCost.ReduceU, cost));
                    } else if (stAb.getParam("ReduceColor").equals("B")) {
                        costs.add(new OptionalCostValue(OptionalCost.ReduceB, cost));
                    } else if (stAb.getParam("ReduceColor").equals("R")) {
                        costs.add(new OptionalCostValue(OptionalCost.ReduceR, cost));
                    } else if (stAb.getParam("ReduceColor").equals("G")) {
                        costs.add(new OptionalCostValue(OptionalCost.ReduceG, cost));
                    }
                } else {
                    costs.add(new OptionalCostValue(OptionalCost.Generic, cost));
                }
            }
        }

        for (KeywordInterface inst : source.getKeywords()) {
            final String keyword = inst.getOriginal();
            if (keyword.equals("Bargain")) {
                final Cost cost = new Cost("Sac<1/Artifact;Enchantment;Card.token/artifact or enchantment or token>", false);
                costs.add(new OptionalCostValue(OptionalCost.Bargain, cost));
            } else if (keyword.startsWith("Buyback")) {
                final Cost cost = new Cost(keyword.substring(8), false);
                costs.add(new OptionalCostValue(OptionalCost.Buyback, cost));
            } else if (keyword.startsWith("Entwine")) {
                String[] k = keyword.split(":");
                final Cost cost = new Cost(k[1], false);
                costs.add(new OptionalCostValue(OptionalCost.Entwine, cost));
            } else if (keyword.startsWith("Kicker")) {
                String[] sCosts = TextUtil.split(keyword.substring(6), ':');
                int numKickers = sCosts.length;
                for (int j = 0; j < numKickers; j++) {
                    final Cost cost = new Cost(sCosts[j], false);
                    OptionalCost type = null;
                    type = j == 0 ? OptionalCost.Kicker1 : OptionalCost.Kicker2;
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

        // reset static abilities
        if (lkicheck) {
            game.getAction().checkStaticAbilities(false);
            // clear delayed changes, this check should not have updated the view
            game.getTracker().clearDelayed();
            // need to unfreeze tracker
            game.getTracker().unfreeze();
        }

        return costs;
    }

    public static SpellAbility addOptionalCosts(final SpellAbility sa, List<OptionalCostValue> list) {
        if (sa == null || list.isEmpty()) {
            return sa;
        }
        final SpellAbility result = sa.copy();
        if (sa.hasParam("ReduceCost")) {
            result.putParam("ReduceCost", sa.getParam("ReduceCost"));
        }
        if (sa.hasParam("RaiseCost")) {
            result.putParam("RaiseCost", sa.getParam("RaiseCost"));
        }
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
        if (sa.isSpell()) {
            final Card source = sa.getHostCard();
            for (KeywordInterface inst : source.getKeywords()) {
                final String keyword = inst.getOriginal();
                if (keyword.startsWith("AlternateAdditionalCost")) {
                    abilities.clear();

                    for (String s : keyword.split(":", 2)[1].split(":")) {
                        final SpellAbility newSA = sa.copy();
                        newSA.setBasicSpell(false);

                        final Cost cost = new Cost(s, false);
                        newSA.setDescription(sa.getDescription() + " (Additional cost: " + cost.toSimpleString() + ")");
                        newSA.setPayCosts(cost.add(sa.getPayCosts()));
                        if (newSA.canPlay()) {
                            abilities.add(newSA);
                        }
                    }
                }
            }
        } else if (sa.isActivatedAbility() && sa.hasParam("AlternateCost")) {
            // need to be handled there because it needs to rebuilt the description for the original ability

            abilities.clear();

            SpellAbility newSA = sa.copy();
            newSA.removeParam("AlternateCost");
            newSA.rebuiltDescription();
            if (newSA.canPlay()) {
                abilities.add(newSA);
            }

            // set the cost to this directly to bypass non mana cost
            Cost alternateCost = new Cost(sa.getParam("AlternateCost"), sa.isAbility());
            SpellAbility newSA2 = sa.copyWithDefinedCost(alternateCost);
            newSA2.removeParam("AlternateCost");
            newSA2.rebuiltDescription();
            if (newSA2.canPlay()) {
                abilities.add(newSA2);
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
            if (o.startsWith("Casualty")) {
                Trigger tr = Iterables.getFirst(ki.getTriggers(), null);
                if (tr != null) {
                    String n = o.split(":")[1];
                    if (host.wasCast() && n.equals("X")) {
                        CardCollectionView creatures = activator.getCreaturesInPlay();
                        int max = Aggregates.max(creatures, CardPredicates.Accessors.fnGetNetPower);
                        n = Integer.toString(pc.chooseNumber(sa, "Choose X for Casualty", 0, max));
                    }
                    final String casualtyCost = "Sac<1/Creature.powerGE" + n + "/creature with power " + n +
                            " or greater>";
                    final Cost cost = new Cost(casualtyCost, false);
                    String str = "Pay for Casualty? " + cost.toSimpleString();
                    boolean v = pc.addKeywordCost(sa, cost, ki, str);

                    tr.setSVar("CasualtyPaid", v ? "1" : "0");
                    tr.getOverridingAbility().setSVar("CasualtyPaid", v ? "1" : "0");
                    tr.setSVar("Casualty", v ? n : "0");
                    tr.getOverridingAbility().setSVar("Casualty", v ? n : "0");

                    if (v) {
                        if (result == null) {
                            result = sa.copy();
                        }
                        result.getPayCosts().add(cost);
                        reset = true;
                    }
                }
            } else if (o.equals("Conspire")) {
                Trigger tr = Iterables.getFirst(ki.getTriggers(), null);
                if (tr != null) {
                    final String conspireCost = "tapXType<2/Creature.SharesColorWith/" +
                        "creature that shares a color with " + host.getName() + ">";
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
            } else if (o.startsWith("Squad")) {
                Trigger tr = Iterables.getFirst(ki.getTriggers(), null);
                if (tr != null) {
                    String costStr = o.split(":")[1];
                    final Cost cost = new Cost(costStr, false);

                    String str = "Choose amount for Squad: " + cost.toSimpleString();

                    int v = pc.chooseNumberForKeywordCost(sa, cost, ki, str, Integer.MAX_VALUE);

                    tr.setSVar("SquadAmount", String.valueOf(v));
                    tr.getOverridingAbility().setSVar("SquadAmount", String.valueOf(v));

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
        eff.setName(sourceCard + "'s Effect");
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

        String desc = "It enters the battlefield with ";
        desc += Lang.nounWithNumeral(amount, CounterType.getType(counter).getName() + " counter");
        desc += " on it.";

        String repeffstr = "Event$ Moved | ValidCard$ Card.IsRemembered | Destination$ Battlefield | ReplacementResult$ Updated | Description$ " + desc;

        ReplacementEffect re = ReplacementHandler.parseReplacement(repeffstr, eff, true);
        re.setLayer(ReplacementLayer.Other);
        re.setOverridingAbility(sa);

        eff.addReplacementEffect(re);

        SpellAbilityEffect.addForgetOnMovedTrigger(eff, "Stack");

        eff.updateStateForView();

        // TODO: Add targeting to the effect so it knows who it's dealing with
        game.getTriggerHandler().suppressMode(TriggerType.ChangesZone);
        game.getAction().moveTo(ZoneType.Command, eff, null, null);
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
        AbilityManaPart abMana = sa.getManaPart();
        if (abMana == null) {
            return "";
        }

        String baseMana;
        int amount = sa.amountOfManaGenerated(false);

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
            baseMana = abMana.mana(sa);
        }

        if (sa.getSubAbility() != null) {
            // Mark SAs with subAbilities as undoable. These are generally things like damage, and other stuff
            // that's hard to track and remove
            sa.setUndoable(false);
        } else if (sa.hasParam("Amount") && !StringUtils.isNumeric(sa.getParam("Amount"))) {
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
        PlayerCollection players = new PlayerCollection(game.getPlayers());
        // CR 613.7m use APNAP
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
        c.getGame().getAction().checkStaticAbilities(false);

        c.updateKeywords();

        c.getGame().getTriggerHandler().resetActiveTriggers();
    }

    public static void rollbackAbility(SpellAbility ability, final Zone fromZone, final int zonePosition, CostPayment payment, Card oldCard) {
        // cancel ability during target choosing
        final Game game = ability.getActivatingPlayer().getGame();

        if (fromZone != null) { // and not a copy
            // might have been an alternative lki host
            oldCard = ability.getCardState().getCard();
 
            oldCard.setCastSA(null);
            oldCard.setCastFrom(null);
            // add back to where it came from, hopefully old state
            // skip GameAction
            oldCard.getZone().remove(oldCard);
            // in some rare cases the old position no longer exists (Panglacial Wurm + Selvala)
            Integer newPosition = zonePosition >= 0 ? Math.min(Integer.valueOf(zonePosition), fromZone.size()) : null;
            fromZone.add(oldCard, newPosition);
            ability.setHostCard(oldCard);
            ability.setXManaCostPaid(null);
            ability.setSpendPhyrexianMana(false);
            ability.clearPipsToReduce();
            ability.setPaidLife(0);
            if (ability.hasParam("Announce")) {
                for (final String aVar : ability.getParam("Announce").split(",")) {
                    final String varName = aVar.trim();
                    if (!varName.equals("X")) {
                        ability.setSVar(varName, "0");
                    }
                }
            }
            // better safe than sorry approach in case rolled back ability was copy (from addExtraKeywordCost)
            for (SpellAbility sa : oldCard.getSpells()) {
                sa.setHostCard(oldCard);
            }
            //for Chorus of the Conclave
            ability.rollback();

            oldCard.setBackSide(false);
            oldCard.setState(oldCard.getFaceupCardStateName(), true);
            oldCard.unanimateBestow();
            if (ability.isDisturb() || ability.hasParam("CastTransformed")) {
                oldCard.undoIncrementTransformedTimestamp();
            }

            if (ability.hasParam("Prototype")) {
                oldCard.removeCloneState(oldCard.getPrototypeTimestamp());
            }
        }

        ability.clearTargets();

        ability.resetOnceResolved();
        payment.refundPayment();
        game.getStack().clearFrozen();
        game.getTriggerHandler().clearWaitingTriggers();
    }

}
