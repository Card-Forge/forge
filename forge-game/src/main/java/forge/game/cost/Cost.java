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
package forge.game.cost;

import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import forge.card.CardType;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Lists;

import forge.card.mana.ManaCost;
import forge.card.mana.ManaCostParser;
import forge.game.CardTraitBase;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.card.CounterType;
import forge.game.mana.ManaCostBeingPaid;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.Lang;
import forge.util.TextUtil;

/**
 * <p>
 * Cost class.
 * </p>
 *
 * @author Forge
 * @version $Id$
 */
public class Cost implements Serializable {
    /**
     * Serializables need a version ID.
     */
    private static final long serialVersionUID = 1L;
    private boolean isAbility = true;
    private final List<CostPart> costParts = Lists.newArrayList();
    private boolean isMandatory = false;

    // Primarily used for Summoning Sickness awareness
    private boolean tapCost = false;

    public final boolean hasTapCost() {
        return this.tapCost;
    }

    private void cacheTapCost() {
        tapCost = hasSpecificCostType(CostTap.class);
    }

    public final boolean hasNoManaCost() {
        return this.getCostMana() == null;
    }

    public final boolean hasManaCost() {
        return !this.hasNoManaCost();
    }

    public final boolean hasSpecificCostType(Class<? extends CostPart> costType) {
        for (CostPart p : getCostParts()) {
            if (costType.isInstance(p)) {
                return true;
            }
        }
        return false;
    }

    public final boolean hasOnlySpecificCostType(Class<? extends CostPart> costType) {
        for (CostPart p : getCostParts()) {
            if (!costType.isInstance(p)) {
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    public <T extends CostPart> T getCostPartByType(Class<T> costType) {
        for (CostPart p : getCostParts()) {
            if (costType.isInstance(p)) {
                return (T)p;
            }
        }
        return null;
    }

    /**
     * Gets the cost parts.
     *
     * @return the cost parts
     */
    public final List<CostPart> getCostParts() {
        return this.costParts;
    }

    public void sort() {
        // Things that need to happen first should be 0-4 (Tap, PayMana)
        // Things that happen that are generally undoable 5 (Pretty much everything)
        // Things that are annoying to undo 6-10 (PayLife, GainControl)
        // Things that are hard to undo 11+ (Zone Changing things)
        // Things that are pretty much happen at the end (Untap) 16+
        // Things that NEED to happen last 100+

        Collections.sort(this.costParts, new Comparator<CostPart>() {
            @Override
            public int compare(CostPart o1, CostPart o2) {
                return ObjectUtils.compare(o1.paymentOrder(), o2.paymentOrder());
            }
        });
    }

    /**
     * Get the cost parts, always including a mana cost part (which may be
     * zero).
     *
     * @return the cost parts, possibly with an extra zero mana {@link
     * CostPartMana}.
     */
    public final List<CostPart> getCostPartsWithZeroMana() {
        if (this.hasManaCost()) {
            return this.costParts;
        }
        final List<CostPart> newCostParts = Lists.newArrayListWithCapacity(this.costParts.size() + 1);
        newCostParts.addAll(this.costParts);
        newCostParts.add(new CostPartMana(ManaCost.ZERO, null));
        return newCostParts;
    }

    /**
     * <p>
     * isOnlyManaCost.
     * </p>
     *
     * @return a boolean.
     */
    public final boolean isOnlyManaCost() {
        // used by Morph, Equip and some string builders
        for (final CostPart part : this.costParts) {
            if (!(part instanceof CostPartMana)) {
                return false;
            }
        }

        return true;
    }

    /**
     * <p>
     * getTotalMana.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public final ManaCost getTotalMana() {
        CostPartMana manapart = getCostMana();
        return manapart == null ? ManaCost.ZERO : manapart.getMana();
    }

    /**
     * <p>
     * isMandatory
     * </p>
     *
     * @return boolean
     */
    public final boolean isMandatory() {
        return this.isMandatory;
    }
    public final void setMandatory(boolean b) {
        isMandatory = b;
    }

    public final boolean isAbility() {
        return this.isAbility;
    }

    private Cost() {

    }

    private Cost(int genericMana) {
        costParts.add(new CostPartMana(ManaCost.get(genericMana), null));
    }

    // Parsing Strings

    public Cost(ManaCost cost, final boolean bAbility) {
        this.isAbility = bAbility;
        costParts.add(new CostPartMana(cost, null));
    }

    public Cost(String parse, final boolean bAbility) {
        this(parse, bAbility, true);
    }

    /**
     * <p>
     * Constructor for Cost.
     * </p>
     * @param parse
     *            a {@link java.lang.String} object.
     * @param bAbility
     *            a boolean.
     */
    public Cost(String parse, final boolean bAbility, final boolean intrinsic) {
        this.isAbility = bAbility;
        // when adding new costs for cost string, place them here

        boolean xCantBe0 = false;
        boolean untapCost = false;

        StringBuilder manaParts = new StringBuilder();
        String[] parts = TextUtil.splitWithParenthesis(parse, ' ', '<', '>');

        // make this before parse so that classes that need it get data in their constructor
        for (String part : parts) {
            if (part.equals("T") || part.equals("Tap"))
                this.tapCost = true;
            if (part.equals("Q") || part.equals("Untap"))
                untapCost = true;
        }

        CostPartMana parsedMana = null;
        for (String part : parts) {
            if ("XCantBe0".equals(part)) {
                xCantBe0 = true;
            } else if ("Mandatory".equals(part)) {
                this.isMandatory = true;
            } else {
                CostPart cp = parseCostPart(part, tapCost, untapCost);
                if (null != cp)
                    if (cp instanceof CostPartMana) {
                        parsedMana = (CostPartMana) cp;
                    } else {
                        if (cp instanceof CostPartWithList) {
                            ((CostPartWithList)cp).setIntrinsic(intrinsic);
                        }
                        this.costParts.add(cp);
                    }
                else
                    manaParts.append(part).append(" ");
            }
        }

        if (parsedMana == null && (manaParts.length() > 0 || xCantBe0)) {
            parsedMana = new CostPartMana(new ManaCost(new ManaCostParser(manaParts.toString())), xCantBe0 ? "XCantBe0" : null);
        }
        if (parsedMana != null) {
            costParts.add(parsedMana);
        }

        // technically the user might pay the costs in any order
        // but needs to activate mana ability first
        sort();
    }

    private static CostPart parseCostPart(String parse, boolean tapCost, boolean untapCost) {
        if (parse.startsWith("Mana<")) {
            final String[] splitStr = TextUtil.split(abCostParse(parse, 1)[0], '\\');
            final String restriction = splitStr.length > 1 ? splitStr[1] : null;
            return new CostPartMana(new ManaCost(new ManaCostParser(splitStr[0])), restriction);
        }

        if (parse.startsWith("tapXType<")) {
            final String[] splitStr = abCostParse(parse, 3);
            final String description = splitStr.length > 2 ? splitStr[2] : null;
            return new CostTapType(splitStr[0], splitStr[1], description, tapCost);
        }

        if (parse.startsWith("untapYType<")) {
            final String[] splitStr = abCostParse(parse, 3);
            final String description = splitStr.length > 2 ? splitStr[2] : null;
            return new CostUntapType(splitStr[0], splitStr[1], description, untapCost);
        }

        if (parse.startsWith("SubCounter<")) {
            // SubCounter<NumCounters/CounterType/{Type/Description/Zone}>
            final String[] splitStr = abCostParse(parse, 5);
            final String type = splitStr.length > 2 ? splitStr[2] : "CARDNAME";
            final String description = splitStr.length > 3 ? splitStr[3] : null;
            final ZoneType zone = splitStr.length > 4 ? ZoneType.smartValueOf(splitStr[4]) : ZoneType.Battlefield;
            boolean oneOrMore = false;
            if (splitStr[0].equals("X1+")) {
                oneOrMore = true;
                splitStr[0] = "X";
            }

            return new CostRemoveCounter(splitStr[0], CounterType.getType(splitStr[1]), type, description, zone, oneOrMore);
        }

        if (parse.startsWith("AddCounter<")) {
            // AddCounter<NumCounters/CounterType>
            final String[] splitStr = abCostParse(parse, 4);
            final String target = splitStr.length > 2 ? splitStr[2] : "CARDNAME";
            final String description = splitStr.length > 3 ? splitStr[3] : null;
            return new CostPutCounter(splitStr[0], CounterType.getType(splitStr[1]), target, description);
        }

        // While no card has "PayLife<2> PayLife<3> there might be a card that
        // Changes Cost by adding a Life Payment
        if (parse.startsWith("PayLife<")) {
            // PayLife<LifeCost>
            final String[] splitStr = abCostParse(parse, 2);
            final String description = splitStr.length > 1 ? splitStr[1] : null;
            return new CostPayLife(splitStr[0], description);
        }

        if (parse.startsWith("PayEnergy<")) {
            // Payenergy<EnergyCost>
            final String[] splitStr = abCostParse(parse, 1);
            return new CostPayEnergy(splitStr[0]);
        }
        if (parse.startsWith("PayShards<")) { //Adventure specific energy-esque tokens
            // Payshards<ShardCost>
            final String[] splitStr = abCostParse(parse, 1);
            return new CostPayShards(splitStr[0]);
        }

        if (parse.startsWith("GainLife<")) {
            // PayLife<LifeCost>
            final String[] splitStr = abCostParse(parse, 3);
            int cnt = splitStr.length > 2 ? "*".equals(splitStr[2]) ? Integer.MAX_VALUE : Integer.parseInt(splitStr[2]) : 1;
            return new CostGainLife(splitStr[0], splitStr[1], cnt);
        }

        if (parse.startsWith("GainControl<")) {
            final String[] splitStr = abCostParse(parse, 3);
            final String description = splitStr.length > 2 ? splitStr[2] : null;
            return new CostGainControl(splitStr[0], splitStr[1], description);
        }

        if (parse.startsWith("Unattach<")) {
            // Unattach<Type/Desc>
            final String[] splitStr = abCostParse(parse, 2);
            final String description = splitStr.length > 1 ? splitStr[1] : null;
            return new CostUnattach(splitStr[0], description);
        }

        if (parse.startsWith("ChooseColor<")) {
            // ChooseColor<NumToChoose>
            //TODO expand this to set off different UI for Specialize
            final String[] splitStr = abCostParse(parse, 1);
            return new CostChooseColor(splitStr[0]);
        }

        if (parse.startsWith("ChooseCreatureType<")) {
            final String[] splitStr = abCostParse(parse, 1);
            return new CostChooseCreatureType(splitStr[0]);
        }

        if (parse.startsWith("DamageYou<")) {
            // Damage<NumDmg>
            final String[] splitStr = abCostParse(parse, 1);
            return new CostDamage(splitStr[0]);
        }

        if (parse.startsWith("Mill<")) {
            // Mill<NumCards>
            final String[] splitStr = abCostParse(parse, 1);
            return new CostMill(splitStr[0]);
        }

        if (parse.startsWith("FlipCoin<")) {
            // FlipCoin<NumCoins>
            final String[] splitStr = abCostParse(parse, 1);
            return new CostFlipCoin(splitStr[0]);
        }

        if (parse.startsWith("RollDice<")) {
            // RollDice<NumDice/Sides/ResultSVar>
            final String[] splitStr = abCostParse(parse, 4);
            final String description = splitStr.length > 3 ? splitStr[3] : null;
            return new CostRollDice(splitStr[0], splitStr[1], splitStr[2], description);
        }

        if (parse.startsWith("Discard<")) {
            // Discard<NumCards/Type>
            final String[] splitStr = abCostParse(parse, 3);
            final String description = splitStr.length > 2 ? splitStr[2] : null;
            return new CostDiscard(splitStr[0], splitStr[1], description);
        }

        if (parse.startsWith("AddMana<")) {
            // AddMana<Num/Type>
            final String[] splitStr = abCostParse(parse, 3);
            final String description = splitStr.length > 2 ? splitStr[2] : null;
            return new CostAddMana(splitStr[0], splitStr[1], description);
        }

        if (parse.startsWith("Sac<")) {
            final String[] splitStr = abCostParse(parse, 3);
            final String description = splitStr.length > 2 ? splitStr[2] : null;
            return new CostSacrifice(splitStr[0], splitStr[1], description);
        }

        if (parse.startsWith("RemoveAnyCounter<")) {
            final String[] splitStr = abCostParse(parse, 4);
            final String description = splitStr.length > 3 ? splitStr[3] : null;
            boolean oneOrMore = false;
            if (splitStr[0].equals("X1+")) {
                oneOrMore = true;
                splitStr[0] = "X";
            }
            return new CostRemoveAnyCounter(splitStr[0], CounterType.getType(splitStr[1]), splitStr[2], description, oneOrMore);
        }

        if (parse.startsWith("Exile<")) {
            final String[] splitStr = abCostParse(parse, 3);
            final String description = splitStr.length > 2 ? splitStr[2] : null;
            return new CostExile(splitStr[0], splitStr[1], description, ZoneType.Battlefield);
        }

        if (parse.startsWith("ExileFromHand<")) {
            final String[] splitStr = abCostParse(parse, 3);
            final String description = splitStr.length > 2 ? splitStr[2] : null;
            return new CostExile(splitStr[0], splitStr[1], description, ZoneType.Hand);
        }

        if (parse.startsWith("ExileFromGrave<")) {
            final String[] splitStr = abCostParse(parse, 3);
            final String description = splitStr.length > 2 ? splitStr[2] : null;
            return new CostExile(splitStr[0], splitStr[1], description, ZoneType.Graveyard);
        }

        if (parse.startsWith("ExileFromStack<")) {
            final String[] splitStr = abCostParse(parse, 3);
            final String description = splitStr.length > 2 ? splitStr[2] : null;
            return new CostExileFromStack(splitStr[0], splitStr[1], description);
        }

        if (parse.startsWith("ExileFromTop<")) {
            final String[] splitStr = abCostParse(parse, 3);
            final String description = splitStr.length > 2 ? splitStr[2] : null;
            return new CostExile(splitStr[0], splitStr[1], description, ZoneType.Library);
        }

        if (parse.startsWith("ExileAnyGrave<")) {
            final String[] splitStr = abCostParse(parse, 3);
            final String description = splitStr.length > 2 ? splitStr[2] : null;
            return new CostExile(splitStr[0], splitStr[1], description, ZoneType.Graveyard, -1);
        }

        if (parse.startsWith("ExileSameGrave<")) {
            final String[] splitStr = abCostParse(parse, 3);
            final String description = splitStr.length > 2 ? splitStr[2] : null;
            return new CostExile(splitStr[0], splitStr[1], description, ZoneType.Graveyard, 0);
        }

        if (parse.startsWith("Return<")) {
            final String[] splitStr = abCostParse(parse, 3);
            final String description = splitStr.length > 2 ? splitStr[2] : null;
            return new CostReturn(splitStr[0], splitStr[1], description);
        }

        if (parse.startsWith("Reveal<")) {
            final String[] splitStr = abCostParse(parse, 3);
            final String description = splitStr.length > 2 ? splitStr[2] : null;
            return new CostReveal(splitStr[0], splitStr[1], description);
        }

        if (parse.startsWith("RevealFromExile<")) {
            final String[] splitStr = abCostParse(parse, 3);
            final String description = splitStr.length > 2 ? splitStr[2] : null;
            return new CostReveal(splitStr[0], splitStr[1], description, "Exile");
        }

        if (parse.startsWith("RevealOrChoose<")) {
            final String[] splitStr = abCostParse(parse, 3);
            final String description = splitStr.length > 2 ? splitStr[2] : null;
            return new CostReveal(splitStr[0], splitStr[1], description, "Hand,Battlefield");
        }

        if (parse.startsWith("ExiledMoveToGrave<")) {
            final String[] splitStr = abCostParse(parse, 3);
            final String description = splitStr.length > 2 ? splitStr[2] : null;
            return new CostExiledMoveToGrave(splitStr[0], splitStr[1], description);
        }

        if (parse.startsWith("Draw<")) {
            final String[] splitStr = abCostParse(parse, 2);
            return new CostDraw(splitStr[0], splitStr[1]);
        }

        if (parse.startsWith("PutCardToLibFromHand<")) {
            final String[] splitStr = abCostParse(parse, 4);
            final String description = splitStr.length > 3 ? splitStr[3] : null;
            return new CostPutCardToLib(splitStr[0], splitStr[1], splitStr[2], description, ZoneType.Hand);
        }

        if (parse.startsWith("PutCardToLibFromGrave<")) {
            final String[] splitStr = abCostParse(parse, 4);
            final String description = splitStr.length > 3 ? splitStr[3] : null;
            return new CostPutCardToLib(splitStr[0], splitStr[1], splitStr[2], description, ZoneType.Graveyard);
        }

        if (parse.startsWith("PutCardToLibFromSameGrave<")) {
            final String[] splitStr = abCostParse(parse, 4);
            final String description = splitStr.length > 3 ? splitStr[3] : null;
            return new CostPutCardToLib(splitStr[0], splitStr[1], splitStr[2], description, ZoneType.Graveyard, true);
        }

        if (parse.startsWith("PutCardToLibFromBattlefield<")) {
            final String[] splitStr = abCostParse(parse, 4);
            final String description = splitStr.length > 3 ? splitStr[3] : null;
            return new CostPutCardToLib(splitStr[0], splitStr[1], splitStr[2], description, ZoneType.Battlefield);
        }

        if (parse.startsWith("Exert<")) {
            final String[] splitStr = abCostParse(parse, 3);
            final String description = splitStr.length > 2 ? splitStr[2] : null;
            return new CostExert(splitStr[0], splitStr[1], description);
        }

        if (parse.startsWith("Enlist<")) {
            final String[] splitStr = abCostParse(parse, 3);
            final String description = splitStr.length > 2 ? splitStr[2] : null;
            return new CostEnlist(splitStr[0], splitStr[1], description);
        }

        if (parse.equals("RevealChosenPlayer")) {
            return new CostRevealChosenPlayer();
        }

        // These won't show up with multiples
        if (parse.equals("Untap") || parse.equals("Q")) {
            return new CostUntap();
        }

        if (parse.equals("T")) {
            return new CostTap();
        }
        return null;
    }

    /**
     * <p>
     * abCostParse.
     * </p>
     *
     * @param parse
     *            a {@link java.lang.String} object.
     * @param numParse
     *            a int.
     * @return an array of {@link java.lang.String} objects.
     */
    private static String[] abCostParse(final String parse, final int numParse) {
        final int startPos = 1 + parse.indexOf("<");
        final int endPos = parse.indexOf(">", startPos);
        String str = parse.substring(startPos, endPos);
        final String[] splitStr = TextUtil.split(str, '/', numParse);
        return splitStr;
    }

    public final Cost copy() {
        Cost toRet = new Cost();
        toRet.isAbility = this.isAbility;
        toRet.isMandatory = this.isMandatory;
        for (CostPart cp : this.costParts) {
            toRet.costParts.add(cp.copy());
        }
        toRet.cacheTapCost();
        return toRet;
    }

    public final Cost copyWithNoMana() {
        Cost toRet = new Cost(0);
        toRet.isAbility = this.isAbility;
        for (CostPart cp : this.costParts) {
            if (!(cp instanceof CostPartMana))
                toRet.costParts.add(cp.copy());
        }
        toRet.cacheTapCost();
        return toRet;
    }

    public final Cost copyWithDefinedMana(String manaCost) {
        Cost toRet = copyWithNoMana();
        toRet.costParts.add(new CostPartMana(new ManaCost(new ManaCostParser(manaCost)), null));
        toRet.cacheTapCost();
        return toRet;
    }

    public final CostPartMana getCostMana() {
        for (final CostPart part : this.costParts) {
            if (part instanceof CostPartMana) {
                return (CostPartMana) part;
            }
        }
        return null;
    }

    public final CostPayEnergy getCostEnergy() {
        for (final CostPart part : this.costParts) {
            if (part instanceof CostPayEnergy) {
                return (CostPayEnergy) part;
            }
        }
        return null;
    }

    /**
     * <p>
     * refundPaidCost.
     * </p>
     *
     * @param source
     *            a {@link forge.game.card.Card} object.
     */
    public final void refundPaidCost(final Card source) {
        // prereq: isUndoable is called first
        for (final CostPart part : this.costParts) {
            part.refund(source);
        }
    }

    /**
     * <p>
     * isUndoable.
     * </p>
     *
     * @return a boolean.
     */
    public final boolean isUndoable() {
        for (final CostPart part : this.costParts) {
            if (!part.isUndoable()) {
                return false;
            }
        }

        return true;
    }

    /**
     * <p>
     * isReusuableResource.
     * </p>
     *
     * @return a boolean.
     */
    public final boolean isReusuableResource() {
        for (final CostPart part : this.costParts) {
            if (!part.isReusable()) {
                return false;
            }
        }

        return this.isAbility;
    }

    /**
     * <p>
     * isRenewableResource.
     * </p>
     *
     * @return a boolean.
     */
    public final boolean isRenewableResource() {
        for (final CostPart part : this.costParts) {
            if (!part.isRenewable()) {
                return false;
            }
        }

        return this.isAbility;
    }

    /**
     * <p>
     * toString.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    @Override
    public final String toString() {
        if (this.isAbility) {
            return this.abilityToString();
        } else {
            return this.spellToString(true);
        }
    }

    // maybe add a conversion method that turns the amounts into words 1=a(n),
    // 2=two etc.

    /**
     * <p>
     * toStringAlt.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public final String toStringAlt() {
        return this.spellToString(false);
    }

    /**
     * <p>
     * toSimpleString.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public final String toSimpleString() {
        final StringBuilder cost = new StringBuilder();
        boolean first = true;
        for (final CostPart part : this.costParts) {
            if (!first) {
                cost.append(", ");
            }
            cost.append(part.toString());
            first = false;
        }
        return cost.toString();
    }

    /**
     * <p>
     * spellToString.
     * </p>
     *
     * @param bFlag
     *            a boolean.
     * @return a {@link java.lang.String} object.
     */
    private String spellToString(final boolean bFlag) {
        final StringBuilder cost = new StringBuilder();
        boolean first = true;

        if (bFlag) {
            cost.append("As an additional cost to cast this spell, ");
        } else {
            // usually no additional mana cost for spells
            // only three Alliances cards have additional mana costs, but they
            // are basically kicker/multikicker
            /*
             * if (!getTotalMana().equals("0")) {
             * cost.append("pay ").append(getTotalMana()); first = false; }
             */
        }

        for (final CostPart part : this.costParts) {
            if (part instanceof CostPartMana) {
                continue;
            }
            if (!first) {
                cost.append(" and ");
            }
            if (bFlag) {
                cost.append(StringUtils.uncapitalize(part.toString()));
            } else {
                cost.append(part.toString());
            }
            first = false;
        }

        if (first) {
            return "";
        }

        if (bFlag) {
            cost.append(".").append("\n");
        }

        return cost.toString();
    }

    /**
     * <p>
     * abilityToString.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    private String abilityToString() {
        final StringBuilder cost = new StringBuilder();
        boolean first = true;

        for (final CostPart part : this.costParts) {
            boolean append = true;
            if (!first) {
                if (part instanceof CostPartMana) {
                    cost.insert(0, ", ").insert(0, part.toString());
                    append = false;
                } else {
                    cost.append(", ");
                }
            }
            if (append) {
                cost.append(part.toString());
            }
            first = false;
        }

        if (first) {
            cost.append("0");
        }

        return cost.toString();
    }

    // TODO: If a Cost needs to pay more than 10 of something, fill this array
    // as appropriate
    /**
     * Constant.
     * <code>numNames="{zero, a, two, three, four, five, six, "{trunked}</code>
     */
    private static final String[] NUM_NAMES = { "zero", "a", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten" };

    /**
     * Convert amount type to words.
     *
     * @param i
     *            the i
     * @param amount
     *            the amount
     * @param type
     *            the type
     * @return the string
     */
    public static String convertAmountTypeToWords(final Integer i, final String amount, final String type) {
        if (i == null) {
            return Cost.convertAmountTypeToWords(amount, type);
        }

        return Cost.convertIntAndTypeToWords(i.intValue(), type);
    }

    /**
     * <p>
     * convertIntAndTypeToWords.
     * </p>
     *
     * @param i
     *            a int.
     * @param type
     *            a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public static String convertIntAndTypeToWords(final int i, String type) {
        if (i == 1 && type.startsWith("another")) {
            return type; //prevent returning "an another"
        }

        final StringBuilder sb = new StringBuilder();

        if (i >= Cost.NUM_NAMES.length) {
            sb.append(i);
        }
        else if (i == 1 && Lang.startsWithVowel(type)) {
            sb.append("an");
        }
        else {
            sb.append(Cost.NUM_NAMES[i]);
        }

        sb.append(" ");
        if (1 != i) {
            String [] typewords = type.split(" ");
            String lastWord = typewords[typewords.length - 1];
            sb.append(CardType.isASubType(lastWord) ? type.replace(lastWord, CardType.getPluralType(lastWord))
                    : type + "s");
        } else {
            sb.append(type);
        }

        return sb.toString();
    }

    /**
     * Convert amount type to words.
     *
     * @param amount
     *            the amount
     * @param type
     *            the type
     * @return the string
     */
    public static String convertAmountTypeToWords(final String amount, final String type) {
        final StringBuilder sb = new StringBuilder();

        sb.append(amount);
        sb.append(" ");
        sb.append(type);

        return sb.toString();
    }

    public void mergeTo(Cost source, int amt, final SpellAbility sa) {
        // multiply to create the full cost
        if (amt > 1) {
            // to double itself we need to work on a copy
            Cost sourceCpy = source.copy();
            for (int i = 1; i < amt; ++i) {
                // in theory setAmount could be used instead but it depends on the cost complexity (probably not worth trying to determine that first)
                source.add(sourceCpy);
            }
        }
        // combine costs (these shouldn't mix together)
        this.add(source, false, sa);
    }

    public Cost add(Cost cost1) {
        return add(cost1, true);
    }
    public Cost add(Cost cost1, boolean mergeAdditional) {
        return add(cost1, mergeAdditional, null);
    }
    public Cost add(Cost cost1, boolean mergeAdditional, final SpellAbility sa) {
        CostPartMana costPart2 = this.getCostMana();
        List<CostPart> toRemove = Lists.newArrayList();
        for (final CostPart part : cost1.getCostParts()) {
            if (part instanceof CostPartMana && ((CostPartMana) part).getMana().isZero()) {
                continue; // do not add Zero Mana
            } else if (part instanceof CostPartMana && costPart2 != null) {
                CostPartMana mPart = (CostPartMana) part;
                ManaCostBeingPaid oldManaCost = new ManaCostBeingPaid(mPart.getMana());
                oldManaCost.addManaCost(costPart2.getMana());
                costParts.remove(costPart2);
                boolean XCantBe0 = !mPart.canXbe0() || !costPart2.canXbe0();
                if (mPart.isExiledCreatureCost() || mPart.isEnchantedCreatureCost() || XCantBe0) {
                    // FIXME: something was amiss when trying to add the cost since the mana cost is either \EnchantedCost or \Exiled but the
                    // restriction no longer marks it as such. Therefore, we need to explicitly copy the ExiledCreatureCost/EnchantedCreatureCost
                    // to make cards like Merseine or Back from the Brink work.
                    costParts.add(0, new CostPartMana(oldManaCost.toManaCost(), mPart.isExiledCreatureCost(), mPart.isEnchantedCreatureCost(), XCantBe0));
                } else {
                    costParts.add(0, new CostPartMana(oldManaCost.toManaCost(), null));
                }
            } else if (part instanceof CostPutCounter || (mergeAdditional && // below usually not desired because they're from different causes
                    (part instanceof CostDiscard || part instanceof CostDraw ||
                    part instanceof CostAddMana || part instanceof CostPayLife ||
                    part instanceof CostSacrifice || part instanceof CostTapType ||
                    part instanceof CostExile))) {
                boolean alreadyAdded = false;
                for (final CostPart other : costParts) {
                    Integer otherAmount = other.convertAmount();
                    // support X loyalty
                    if (otherAmount == null && sa != null && sa.isPwAbility()) {
                        otherAmount = other.getAbilityAmount(sa);
                    }
                    if ((other.getClass().equals(part.getClass()) || (part instanceof CostPutCounter && ((CostPutCounter)part).getCounter().is(CounterEnumType.LOYALTY))) &&
                            part.getType().equals(other.getType()) &&
                            StringUtils.isNumeric(part.getAmount()) &&
                            otherAmount != null) {
                        String amount = String.valueOf(part.convertAmount() + otherAmount);
                        if (part instanceof CostPutCounter) { // CR 606.5 path for Carth
                            if (other instanceof CostPutCounter && ((CostPutCounter)other).getCounter().equals(((CostPutCounter) part).getCounter())) {
                                costParts.add(new CostPutCounter(amount, ((CostPutCounter) part).getCounter(), part.getType(), part.getTypeDescription()));
                            } else if (other instanceof CostRemoveCounter && ((CostRemoveCounter)other).counter.is(CounterEnumType.LOYALTY)) {
                                Integer counters = otherAmount - part.convertAmount();
                                // the cost can turn positive if multiple Carth raise it
                                if (counters < 0) {
                                    costParts.add(new CostPutCounter(String.valueOf(counters *-1), CounterType.get(CounterEnumType.LOYALTY), part.getType(), part.getTypeDescription()));
                                } else {
                                    costParts.add(new CostRemoveCounter(String.valueOf(counters), CounterType.get(CounterEnumType.LOYALTY), part.getType(), part.getTypeDescription(), ZoneType.Battlefield, false));
                                }
                            } else {
                                continue;
                            }
                        } else if (part instanceof CostSacrifice) {
                            costParts.add(new CostSacrifice(amount, part.getType(), part.getTypeDescription()));
                        } else if (part instanceof CostDiscard) {
                            costParts.add(new CostDiscard(amount, part.getType(), part.getTypeDescription()));
                        } else if (part instanceof CostDraw) {
                            costParts.add(new CostDraw(amount, part.getType()));
                        } else if (part instanceof CostTapType) {
                            CostTapType tappart = (CostTapType)part;
                            costParts.add(new CostTapType(amount, part.getType(), part.getTypeDescription(), !tappart.canTapSource));
                        } else if (part instanceof CostAddMana) {
                            costParts.add(new CostAddMana(amount, part.getType(), part.getTypeDescription()));
                        } else if (part instanceof CostPayLife) {
                            costParts.add(new CostPayLife(amount, part.getTypeDescription()));
                        } else if (part instanceof CostExile) {
                            costParts.add(new CostExile(amount, part.getType(), part.getTypeDescription(), ((CostExile) part).getFrom()));
                        }
                        toRemove.add(other);
                        alreadyAdded = true;
                        break;
                    }
                }
                if (!alreadyAdded) {
                    costParts.add(part);
                }
            } else {
                costParts.add(part);
            }
        }
        costParts.removeAll(toRemove);
        this.sort();
        return this;
    }

    public final void applyTextChangeEffects(final CardTraitBase trait) {
        for (final CostPart part : this.getCostParts()) {
            part.applyTextChangeEffects(trait);
        }
    }

    public boolean canPay(SpellAbility sa, final boolean effect) {
        return canPay(sa, sa.getActivatingPlayer(), effect);
    }
    public boolean canPay(SpellAbility sa, Player payer, final boolean effect) {
        for (final CostPart part : this.getCostParts()) {
            if (!part.canPay(sa, payer, effect)) {
                return false;
            }
        }

        return true;
    }

    public boolean hasXInAnyCostPart() {
        boolean xCost = false;
        for (CostPart p : this.getCostParts()) {
            if (p instanceof CostPartMana) {
                if (((CostPartMana) p).getAmountOfX() > 0) {
                    xCost = true;
                    break;
                }
            } else if (p.getAmount().equals("X")) {
                xCost = true;
                break;
            }
        }
        return xCost;
    }

    public Integer getMaxForNonManaX(final SpellAbility ability, final Player payer, final boolean effect) {
        Integer val = null;
        for (CostPart p : getCostParts()) {
            if (!p.getAmount().equals("X")) {
                continue;
            }

            val = ObjectUtils.min(val, p.getMaxAmountX(ability, payer, effect));
        }
        // extra 0 check
        if (val != null && val <= 0 && hasManaCost() && !getCostMana().canXbe0()) {
            val = null;
        }
        return val;
    }
    public static final Cost Zero = new Cost(0);
}
