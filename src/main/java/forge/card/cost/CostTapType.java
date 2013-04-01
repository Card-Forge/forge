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
import java.util.List;

import com.google.common.base.Predicate;

import forge.Card;
import forge.CardLists;
import forge.CardPredicates;
import forge.CardPredicates.Presets;
import forge.FThreads;
import forge.card.ability.AbilityUtils;
import forge.card.spellability.SpellAbility;
import forge.control.input.InputSelectCards;
import forge.control.input.InputSelectCardsFromList;
import forge.game.GameState;
import forge.game.ai.ComputerUtil;
import forge.game.player.AIPlayer;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

/**
 * The Class CostTapType.
 */
public class CostTapType extends CostPartWithList {

    private final boolean canTapSource;

    /**
     * Instantiates a new cost tap type.
     * 
     * @param amount
     *            the amount
     * @param type
     *            the type
     * @param description
     *            the description
     */
    public CostTapType(final String amount, final String type, final String description, boolean costHasTapSource) {
        super(amount, type, description);
        canTapSource  = !costHasTapSource;
    }

    @Override
    public boolean isReusable() { return true; }


    /**
     * Gets the description.
     * 
     * @return the description
     */
    public final String getDescription() {
        return this.getTypeDescription() == null ? this.getType() : this.getTypeDescription();
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.card.cost.CostPart#toString()
     */
    @Override
    public final String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Tap ");

        final Integer i = this.convertAmount();
        final String desc = this.getDescription();
        final String type = this.getType();
        
        if (type.contains("sharesCreatureTypeWith")) {
            sb.append("two untapped creatures you control that share a creature type");
        } else {
            sb.append(Cost.convertAmountTypeToWords(i, this.getAmount(), "untapped " + desc));
            sb.append(" you control");
        }
        return sb.toString();
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.card.cost.CostPart#refund(forge.Card)
     */
    @Override
    public final void refund(final Card source) {
        for (final Card c : this.getList()) {
            c.setTapped(false);
        }

        this.getList().clear();
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
        
        List<Card> typeList = new ArrayList<Card>(activator.getCardsIn(ZoneType.Battlefield));
        String type = this.getType();
        boolean sameType = false;
        
        if (type.contains("sharesCreatureTypeWith")) {
            sameType = true;
            type = type.replace("sharesCreatureTypeWith", "");
        }
        
        typeList = CardLists.getValidCards(typeList, type.split(";"), activator, source);

        if (!canTapSource) {
            typeList.remove(source);
        }
        typeList = CardLists.filter(typeList, Presets.UNTAPPED);
        
        if (sameType) {
            for (final Card card : typeList) {
                if (CardLists.filter(typeList, new Predicate<Card>() {
                    @Override
                    public boolean apply(final Card c) {
                        return c.sharesCreatureTypeWith(card);
                    }
                }).size() > 1) {
                    return true;
                }
            }
            return false;
        }

        final Integer amount = this.convertAmount();
        if ((typeList.size() == 0) || ((amount != null) && (typeList.size() < amount))) {
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
    public final boolean payHuman(final SpellAbility ability, final GameState game) {
        List<Card> typeList = new ArrayList<Card>(ability.getActivatingPlayer().getCardsIn(ZoneType.Battlefield));
        String type = this.getType();
        boolean sameType = false;
        if (type.contains("sharesCreatureTypeWith")) {
            sameType = true;
            type = type.replace("sharesCreatureTypeWith", "");
        }
        typeList = CardLists.getValidCards(typeList, type.split(";"), ability.getActivatingPlayer(), ability.getSourceCard());
        typeList = CardLists.filter(typeList, Presets.UNTAPPED);
        if (sameType) {
            final List<Card> List2 = typeList;
            typeList = CardLists.filter(typeList, new Predicate<Card>() {
                @Override
                public boolean apply(final Card c) {
                    for (Card card : List2) {
                        if (!card.equals(c) && card.sharesCreatureTypeWith(c)) {
                            return true;
                        }
                    }
                    return false;
                }
            });
        }
        
        final String amount = this.getAmount();
        final Card source = ability.getSourceCard();
        Integer c = this.convertAmount();
        if (c == null) {
            final String sVar = ability.getSVar(amount);
            // Generalize this
            if (sVar.equals("XChoice")) {
                c = CostUtil.chooseXValue(source, ability, typeList.size());
            } else {
                c = AbilityUtils.calculateAmount(source, amount, ability);
            }
        }
        
        
        InputSelectCards inp = new InputSelectCardsFromList(c, c, typeList);
        inp.setMessage("Select a " + getDescription() + " to tap (%d left)");
        FThreads.setInputAndWait(inp);
        if ( inp.hasCancelled() )
            return false;

        return executePayment(ability, inp.getSelected());
    }

    /* (non-Javadoc)
     * @see forge.card.cost.CostPart#decideAIPayment(forge.game.player.AIPlayer, forge.card.spellability.SpellAbility, forge.Card)
     */
    @Override
    public PaymentDecision decideAIPayment(AIPlayer ai, SpellAbility ability, Card source) {
        final String amount = this.getAmount();
        Integer c = this.convertAmount();
        if (c == null) {
            final String sVar = ability.getSVar(amount);
            if (sVar.equals("XChoice")) {
                List<Card> typeList =
                        CardLists.getValidCards(ai.getCardsIn(ZoneType.Battlefield), this.getType().split(";"), ability.getActivatingPlayer(), ability.getSourceCard());
                typeList = CardLists.filter(typeList, Presets.UNTAPPED);
                c = typeList.size();
                source.setSVar("ChosenX", "Number$" + Integer.toString(c));
            } else {
                c = AbilityUtils.calculateAmount(source, amount, ability);
            }
        }
        if (this.getType().contains("sharesCreatureTypeWith")) {
            return null;
        }

        List<Card> totap = ComputerUtil.chooseTapType(ai, this.getType(), source, !canTapSource, c);


        if (totap == null) {
            System.out.println("Couldn't find a valid card to tap for: " + source.getName());
            return null;
        }

        return new PaymentDecision(totap);
    }

    /* (non-Javadoc)
     * @see forge.card.cost.CostPartWithList#executePayment(forge.card.spellability.SpellAbility, forge.Card)
     */
    @Override
    protected void doPayment(SpellAbility ability, Card targetCard) {
        targetCard.tap();
    }

    /* (non-Javadoc)
     * @see forge.card.cost.CostPartWithList#getHashForList()
     */
    @Override
    public String getHashForList() {
        return "Tapped";
    }

    // Inputs

}
