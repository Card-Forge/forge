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

import com.google.common.collect.Lists;
import forge.card.CardType;
import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.*;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.Lang;
import forge.util.TextUtil;
import forge.util.collect.FCollectionView;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * The Class CostExile.
 */
public class CostExile extends CostPartWithList {
    // Exile<Num/Type{/TypeDescription}>
    // ExileFromHand<Num/Type{/TypeDescription}>
    // ExileFromGrave<Num/Type{/TypeDescription}>
    // ExileFromTop<Num/Type{/TypeDescription}> (of library)
    // ExileSameGrave<Num/Type{/TypeDescription}>

    /**
     * Serializables need a version ID.
     */
    private static final long serialVersionUID = 1L;
    public final List<ZoneType> from = Lists.newArrayList();
    public final int zoneRestriction;

    public final List<ZoneType> getFrom() {
        return this.from;
    }

    public CostExile(final String amount, final String type, final String description, final ZoneType from) {
        this(amount, type, description, from, Lists.newArrayList(), 1);
    }

    public CostExile(final String amount, final String type, final String description, final ZoneType from,
                     final int zoneMode) {
        this(amount, type, description, from, Lists.newArrayList(), zoneMode);
    }

    public CostExile(final String amount, final String type, final String description, final List<ZoneType> froms) {
        this(amount, type, description, null, froms, 1);
    }

    public CostExile(final String amount, final String type, final String description, final ZoneType from,
                     final List<ZoneType> froms, final int zoneMode) {
        super(amount, type, description);
        if (from != null) froms.add(from);
        if (froms.isEmpty()) {
            this.from.add(ZoneType.Battlefield);
        } else {
            this.from.addAll(froms);
        }
        this.zoneRestriction = zoneMode;
    }

    @Override
    public Integer getMaxAmountX(SpellAbility ability, Player payer, final boolean effect) {
        final Card source = ability.getHostCard();
        final Game game = source.getGame();

        CardCollectionView typeList;
        if (zoneRestriction != 1) {
            typeList = game.getCardsIn(this.from);
        } else {
            typeList = payer.getCardsIn(this.from);
        }

        typeList = CardLists.getValidCards(typeList, getType().split(";"), payer, source, ability);

        return typeList.size();
    }

    @Override
    public int paymentOrder() { return 15; }

    @Override
    public final String toString() {
        return toString(0);
    }

    public final String toString(int chosenX) {
        final Integer i = this.convertAmount();
        String desc = this.getDescriptiveType();
        if (this.from.size() == 1) {
            String origin = this.from.get(0).name().toLowerCase();

            if (this.payCostFromSource()) {
                if (!origin.equals("battlefield")) {
                    return String.format("Exile %s from your %s", this.getType(), origin);
                }
                return String.format("Exile %s", this.getType());
            } else if (this.getType().equals("All")) {
                return String.format("Exile all cards from your %s", origin);
            }

            if (origin.equals("battlefield")) {
                String amt;
                if (i == null && this.getAmount().contains("+")) {
                    int needed = Integer.parseInt(this.getAmount().split("\\+")[0]);
                    amt = Lang.getNumeral(needed) + " or more " + desc;
                } else amt = Cost.convertAmountTypeToWords(i, this.getAmount(), desc);
                return "Exile " + amt + (amt.contains("you control") ? "" : " you control");
            }

            if (!desc.equals("Card") && !desc.contains("card")) {
                StringBuilder sb = new StringBuilder();
                sb.append("Exile %s from ");
                if (zoneRestriction == 0) {
                    sb.append("the same");
                } else if (zoneRestriction == -1) {
                    sb.append("a");
                } else {
                    sb.append("your");
                }
                sb.append(" %s");
                return String.format(sb.toString(), Lang.nounWithNumeralExceptOne(this.getAmount(),
                        desc + " card"), origin);
            }

            if (zoneRestriction == 0) {
                return String.format("Exile %s from the same %s",
                        Cost.convertAmountTypeToWords(i, this.getAmount(), desc), origin);
            }

            if (this.getAmount().equals("X")) {
                String x = chosenX > 0 ? Lang.getNumeral(chosenX) : "any number of";
                return String.format ("Exile %s %s from your %s", x, desc, origin);
            }

            return String.format("Exile %s from your %s",
                    Cost.convertAmountTypeToWords(i, this.getAmount(), desc), origin);
        } else {
            return exileMultiZoneCostString(false, chosenX);
        }
    }

    @Override
    public final boolean canPay(final SpellAbility ability, final Player payer, final boolean effect) {
        final Card source = ability.getHostCard();
        final Game game = source.getGame();

        String type = this.getType();
        if (type.equals("All")) {
            return true; // this will always work
        }
        else if (type.contains("FromTopGrave")) {
            type = TextUtil.fastReplace(type, "FromTopGrave", "");
        }

        CardCollection list = new CardCollection(zoneRestriction != 1 ? game.getCardsIn(this.from) :
                payer.getCardsIn(this.from));

        if (this.payCostFromSource()) {
            return list.contains(source);
        }

        boolean totalCMC = false;
        String totalM = "";
        if (type.contains("+withTotalCMCEQ")) {
            totalCMC = true;
            totalM = type.split("withTotalCMCEQ")[1];
            type = TextUtil.fastReplace(type, TextUtil.concatNoSpace("+withTotalCMCEQ", totalM), "");
        }

        boolean sharedType = false;
        if (type.contains("+withSharedCardType")) {
            sharedType = true;
            type = TextUtil.fastReplace(type, "+withSharedCardType", "");
        }

        if (!type.contains("X") || ability.getXManaCostPaid() != null) {
            list = CardLists.getValidCards(list, type.split(";"), payer, source, ability);
        }

        int amount = this.getAbilityAmount(ability);

        if (sharedType) { // will need more logic if cost ever wants more than 2 that share a type
            if (list.size() < amount) return false;
            for (int i = 0; i < list.size(); i++) {
                final Card card1 = list.get(i);
                for (final Card compare : list) {
                    if (!compare.equals(card1) && compare.sharesCardTypeWith(card1)) {
                        return true;
                    }
                }
            }
            return false;
        }

        if (totalCMC) {
            int needed = Integer.parseInt(this.getAmount().split("\\+")[0]);
            if (list.size() < needed) return false;
            if (totalM.equals("X") && ability.getXManaCostPaid() == null) { // X hasn't yet been decided, let it pass
                return true;
            }
            int i = AbilityUtils.calculateAmount(source, totalM, ability);
            return CardLists.cmcCanSumTo(i, list);
        }

        // for Craft: do not count the source card twice (it will be sacrificed)
        if (ability.isCraft()) {
            CostExile firstExileCost = ability.getPayCosts().getCostPartByType(CostExile.class);
            if (firstExileCost != null && firstExileCost.payCostFromSource()) list.remove(ability.getHostCard());
        }

        // for cards like Allosaurus Rider, do not count it
        if (this.from.size() == 1 && this.from.get(0).equals(ZoneType.Hand) && source.isInZone(ZoneType.Hand)
                && list.contains(source)) {
            amount++;
        }

        if (list.size() < amount) {
            return false;
        }

        if (zoneRestriction == 0) {
            boolean foundPayable = false;
            FCollectionView<Player> players = game.getPlayers();
            for (Player p : players) {
                if (CardLists.count(list, CardPredicates.isController(p)) >= amount) {
                    foundPayable = true;
                    break;
                }
            }
            return foundPayable;
        }
        return true;
    }

    @Override
    protected Card doPayment(Player payer, SpellAbility ability, Card targetCard, final boolean effect) {
        final Game game = targetCard.getGame();
        Card newCard = game.getAction().exile(targetCard, null, null);
        SpellAbilityEffect.handleExiledWith(newCard, ability);
        return newCard;
    }

    public String exileMultiZoneCostString(boolean forKW, int xMin) {
        final StringBuilder sb = new StringBuilder();
        sb.append("Exile ");
        String amount = this.getAmount();
        int amt = StringUtils.isNumeric(amount) ? Integer.parseInt(amount) : 0;
        String partType = this.getType();
        //consume .Other from most partTypes
        if (partType.contains(".Other")) partType = partType.replace(".Other", "");
        String singNoun = this.getTypeDescription() != null ? this.getTypeDescription() :
                CardType.CoreType.isValidEnum(partType) || partType.equals("Permanent") ? partType.toLowerCase() :
                        partType;
        String plurNoun = !singNoun.contains(" ") ? Lang.getPlural(singNoun) : singNoun;
        if (!forKW && amt == 0 && xMin > 0) amt = xMin;
        boolean perm = singNoun.equals("permanent");
        if (amt == 1) {
            String aNoun = Lang.nounWithNumeralExceptOne(1, singNoun);
            sb.append(partType.equals("Artifact") || perm ? "another " + singNoun : aNoun);
            sb.append(" you control or ").append(aNoun).append(" card from ");
        } else if (amt > 1) {
            sb.append("the ").append(Lang.getNumeral(amt)).append(" from among ");
            sb.append(perm ? "other " : "").append(plurNoun).append(" you control and/or ").append(singNoun);
            sb.append(" cards in ");
        } else { // currently all non-numeric will use xMin
            sb.append(xMin > 1 ? "the " : "").append(Lang.getNumeral(xMin)).append(forKW ? " or more " : " ");
            if (xMin == 1) {
                sb.append(perm ? "other " : "").append(plurNoun).append(" you control and/or ");
                sb.append(!perm ? singNoun : "").append(" cards from ");
            } else {
                if (this.getFrom().size() > 1) {
                    sb.append("from among ").append(perm ? "other " : "").append(plurNoun);
                    sb.append(" you control and/or cards from ");
                } else {
                    sb.append("from ");
                }
            }
        }
        sb.append("your graveyard");
        return sb.toString();
    }

    public static final String HashLKIListKey = "Exiled";
    public static final String HashCardListKey = "ExiledCards";

    @Override
    public String getHashForLKIList() {
        return HashLKIListKey;
    }
    @Override
    public String getHashForCardList() {
        return HashCardListKey;
    }

    public <T> T accept(ICostVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
