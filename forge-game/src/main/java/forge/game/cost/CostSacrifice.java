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

import forge.card.CardType;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

import forge.game.Game;
import forge.game.ability.AbilityKey;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.Lang;

/**
 * The Class CostSacrifice.
 */
public class CostSacrifice extends CostPartWithList {

    private static final long serialVersionUID = 1L;

    /**
     * Instantiates a new cost sacrifice.
     *
     * @param amount
     *            the amount
     * @param type
     *            the type
     * @param description
     *            the description
     */
    public CostSacrifice(final String amount, final String type, final String description) {
        super(amount, type, description);
    }

    @Override
    public int paymentOrder() { return 15; }

    @Override
    public Integer getMaxAmountX(SpellAbility ability, Player payer, final boolean effect) {
        final Card source = ability.getHostCard();

        String type = getType();
        boolean differentNames = false;
        if (type.contains("+WithDifferentNames")) {
            type = type.replace("+WithDifferentNames", "");
            differentNames = true;
        }

        CardCollectionView typeList = payer.getCardsIn(ZoneType.Battlefield);
        typeList = CardLists.getValidCards(typeList, type.split(";"), payer, source, ability);
        typeList = CardLists.filter(typeList, CardPredicates.canBeSacrificedBy(ability, effect));
        if (differentNames) {
            // TODO rewrite with sharesName to respect Spy Kit
            final Set<String> crdname = Sets.newHashSet();
            for (final Card card : typeList) {
                String name = card.getName();
                // CR 201.2b Those objects have different names only if each of them has at least one name and no two objects in that group have a name in common
                if (!card.hasNoName()) {
                    crdname.add(name);
                }
            }
            return crdname.size();
        }
        return typeList.size();
    }

    /*
     * (non-Javadoc)
     *
     * @see forge.card.cost.CostPart#toString()
     */
    @Override
    public final String toString() {
        final StringBuilder sb = new StringBuilder();
        if (getAmount().equals("X")) {
            sb.append("You may sacrifice ");
        } else {
            sb.append("Sacrifice ");
        }

        if (payCostFromSource()) {
            sb.append(getTypeDescription() == null || !getTypeDescription().startsWith("this")
                    ? getType() : getTypeDescription());
        } else if (getAmount().equals("X")) {
            String typeDesc = getType().toLowerCase().replace(";","s and/or ");
            sb.append("any number of ").append(typeDesc).append("s");
        } else {
            String desc;
            if (this.getTypeDescription() == null) {
                final String typeS = this.getType();
                desc = typeS.equals("Permanent") || CardType.CoreType.isValidEnum(typeS) ? typeS.toLowerCase() : typeS;
            } else {
                desc = this.getTypeDescription();
            }

            if (desc.startsWith("another")) sb.append(desc);
            else sb.append(convertAmount() == null ? Lang.nounWithNumeralExceptOne(getAmount(), desc)
                    : Lang.nounWithNumeralExceptOne(convertAmount(), desc));
        }
        return sb.toString();
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * forge.card.cost.CostPart#canPay(forge.card.spellability.SpellAbility,
     * forge.Card, forge.Player, forge.card.cost.Cost)
     */
    @Override
    public final boolean canPay(final SpellAbility ability, final Player activator, final boolean effect) {
        final Card source = ability.getHostCard();

        if (getType().equals("OriginalHost")) {
            Card originalEquipment = ability.getOriginalHost();
            return originalEquipment.isEquipping() && originalEquipment.canBeSacrificedBy(ability, effect);
        }

        if (payCostFromSource()) {
            return source.canBeSacrificedBy(ability, effect);
        }

        // You can always sac all
        if ("All".equalsIgnoreCase(getAmount())) {
            CardCollectionView typeList = activator.getCardsIn(ZoneType.Battlefield);
            typeList = CardLists.getValidCards(typeList, getType().split(";"), activator, source, ability);
            // it needs to check if everything can be sacrificed
            return Iterables.all(typeList, CardPredicates.canBeSacrificedBy(ability, effect));
        }

        int amount = getAbilityAmount(ability);

        // If amount is null, it's either "ALL" or "X"
        // if X is defined, it needs to be calculated and checked, if X is
        // choice, it can be Paid even if it's 0
        return getMaxAmountX(ability, activator, effect) >= amount;
    }

    @Override
    protected Card doPayment(Player payer, SpellAbility ability, Card targetCard, final boolean effect) {
        final Game game = targetCard.getGame();
        // no table there, it is already handled by CostPartWithList
        Map<AbilityKey, Object> moveParams = AbilityKey.newMap();
        AbilityKey.addCardZoneTableParams(moveParams, table);

        return game.getAction().sacrifice(targetCard, ability, effect, moveParams);
    }

    /* (non-Javadoc)
     * @see forge.card.cost.CostPartWithList#getHashForList()
     */
    @Override
    public String getHashForLKIList() {
        return "Sacrificed";
    }
    @Override
    public String getHashForCardList() {
    	return "SacrificedCards";
    }

    // Inputs
    public <T> T accept(ICostVisitor<T> visitor) {
        return visitor.visit(this);
    }

}
