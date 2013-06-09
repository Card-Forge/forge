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
package forge.card.cost;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import forge.Card;
import forge.CardLists;
import forge.card.ability.AbilityUtils;
import forge.card.spellability.SpellAbility;
import forge.game.Game;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.gui.GuiChoose;

/**
 * This is for the "ExiledMoveToGrave" Cost.
 */
public class CostExiledMoveToGrave extends CostPartWithList {
    // ExiledMoveToGrave<Num/Type{/TypeDescription}>

    /**
     * Instantiates a new cost CostExiledMoveToGrave.
     *
     * @param amount
     *            the amount
     * @param type
     *            the type
     * @param description
     *            the description
     */
    public CostExiledMoveToGrave(final String amount, final String type, final String description) {
        super(amount, type, description);
    }

    /*
     * (non-Javadoc)
     *
     * @see forge.card.cost.CostPart#toString()
     */
    @Override
    public final String toString() {
        final StringBuilder sb = new StringBuilder();
        final Integer i = this.convertAmount();
        sb.append("Put ");

        final String desc = this.getTypeDescription() == null ? this.getType() : this.getTypeDescription();
        sb.append(Cost.convertAmountTypeToWords(i, this.getAmount(), desc));

        sb.append(" into its owner's graveyard");

        return sb.toString();
    }

    /* (non-Javadoc)
     * @see forge.card.cost.CostPartWithList#getHashForList()
     */
    @Override
    public String getHashForList() {
        return "MovedToGrave";
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * forge.card.cost.CostPart#canPay(forge.card.spellability.SpellAbility,
     * forge.Card, forge.Player, forge.card.cost.Cost)
     */
    @Override
    public final boolean canPay(final SpellAbility ability) {
        final Player activator = ability.getActivatingPlayer();
        final Card source = ability.getSourceCard();

        Integer i = this.convertAmount();

        if (i == null) {
            i = AbilityUtils.calculateAmount(source, this.getAmount(), ability);
        }


        List<Card> typeList = activator.getGame().getCardsIn(ZoneType.Exile);

        typeList = CardLists.getValidCards(typeList, this.getType().split(";"), activator, source);
        if (typeList.size() < i) {
            return false;
        }

        return true;

    }

    /*
     * (non-Javadoc)
     *
     * @see
     * forge.card.cost.CostPart#payHuman(forge.card.spellability.SpellAbility,
     * forge.Card, forge.card.cost.Cost_Payment)
     */
    @Override
    public final boolean payHuman(final SpellAbility ability, final Game game) {
        final String amount = this.getAmount();
        Integer c = this.convertAmount();
        final Card source = ability.getSourceCard();
        final Player activator = ability.getActivatingPlayer();

        List<Card> list = activator.getGame().getCardsIn(ZoneType.Exile);


        if (c == null) {
                c = AbilityUtils.calculateAmount(source, amount, ability);
        }

        list = CardLists.getValidCards(list, this.getType().split(";"), activator, source);

        for (int i = 0; i < c; i++) {
            if (list.isEmpty()) {
                return false;
            }

            final Card card = GuiChoose.oneOrNone("Choose an exiled card to put into graveyard", list);

            if (card != null) {
                list.remove(card);
                executePayment(ability, card);
            } else {
                return false;
            }
        }
        return true;

    }

    /* (non-Javadoc)
     * @see forge.card.cost.CostPartWithList#executePayment(forge.card.spellability.SpellAbility, forge.Card)
     */
    @Override
    protected void doPayment(SpellAbility ability, Card targetCard) {
        ability.getActivatingPlayer().getGame().getAction().moveToGraveyard(targetCard);
    }

    /* (non-Javadoc)
     * @see forge.card.cost.CostPart#decideAIPayment(forge.game.player.AIPlayer, forge.card.spellability.SpellAbility, forge.Card)
     */
    @Override
    public PaymentDecision decideAIPayment(Player ai, SpellAbility ability, Card source) {
        Integer c = this.convertAmount();
        List<Card> chosen = new ArrayList<Card>();

        if (c == null) {
            c = AbilityUtils.calculateAmount(source, this.getAmount(), ability);
        }

        List<Card> typeList = ai.getGame().getCardsIn(ZoneType.Exile);

        typeList = CardLists.getValidCards(typeList, this.getType().split(","), ai, source);

        if (typeList.size() < c) {
            return null;
        }

        CardLists.sortByPowerAsc(typeList);
        Collections.reverse(typeList);

        for (int i = 0; i < c; i++) {
            chosen.add(typeList.get(i));
        }

        return chosen.isEmpty() ? null : new PaymentDecision(chosen);
    }
}
