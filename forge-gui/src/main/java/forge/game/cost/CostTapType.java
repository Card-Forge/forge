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

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Predicate;

import forge.ai.ComputerUtil;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates.Presets;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.gui.input.InputSelectCards;
import forge.gui.input.InputSelectCardsFromList;

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

    @Override
    public boolean isRenewable() { return true; }
    
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
        final String desc = this.getDescriptiveType();
        final String type = this.getType();
        
        if (type.contains("sharesCreatureTypeWith")) {
            sb.append("two untapped creatures you control that share a creature type");
        } else if (type.contains("+withTotalPowerGE")) {
            String num = type.split("+withTotalPowerGE")[1];
            sb.append("Tap any number of untapped creatures you control other than CARDNAME with total power " + num + "or greater");
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
        boolean totalPower = false;
        String totalP = "";
        if (type.contains("+withTotalPowerGE")) {
            totalPower = true;
            totalP = type.split("withTotalPowerGE")[1];
            type = type.replace("+withTotalPowerGE" + totalP, "");
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

        if (totalPower) {
            final int i = Integer.parseInt(totalP);
            return CardLists.getTotalPower(typeList) >= i;
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
    public final boolean payHuman(final SpellAbility ability, final Player activator) {
        List<Card> typeList = new ArrayList<Card>(activator.getCardsIn(ZoneType.Battlefield));
        String type = this.getType();
        final String amount = this.getAmount();
        final Card source = ability.getSourceCard();
        Integer c = this.convertAmount();

        boolean sameType = false;
        if (type.contains("sharesCreatureTypeWith")) {
            sameType = true;
            type = type.replace("sharesCreatureTypeWith", "");
        }

        boolean totalPower = false;
        String totalP = "";
        if (type.contains("+withTotalPowerGE")) {
            totalPower = true;
            totalP = type.split("withTotalPowerGE")[1];
            type = type.replace("+withTotalPowerGE" + totalP, "");
        }

        typeList = CardLists.getValidCards(typeList, type.split(";"), activator, ability.getSourceCard());
        typeList = CardLists.filter(typeList, Presets.UNTAPPED);
        if (c == null && !amount.equals("Any")) {
            final String sVar = ability.getSVar(amount);
            // Generalize this
            if (sVar.equals("XChoice")) {
                c = Cost.chooseXValue(source, ability, typeList.size());
            } else {
                c = AbilityUtils.calculateAmount(source, amount, ability);
            }
        }

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
            if (c == 0) return true;
            List<Card> tapped = new ArrayList<Card>();
            while (c > 0) {
                InputSelectCards inp = new InputSelectCardsFromList(1, 1, typeList);
                inp.setMessage("Select one of the cards to tap. Already chosen: " + tapped);
                inp.setCancelAllowed(true);
                inp.showAndWait();
                if (inp.hasCancelled())
                    return false;
                final Card first = inp.getSelected().get(0);
                tapped.add(first);
                typeList = CardLists.filter(typeList, new Predicate<Card>() {
                    @Override
                    public boolean apply(final Card c) {
                        return c.sharesCreatureTypeWith(first);
                    }
                });
                typeList.remove(first);
                c--;
            }
            return executePayment(ability, tapped);
        }       

        if (totalPower) {
            int i = Integer.parseInt(totalP);
            InputSelectCards inp = new InputSelectCardsFromList(0, typeList.size(), typeList);
            inp.setMessage("Select a card to tap.");
            inp.setUnselectAllowed(true);
            inp.setCancelAllowed(true);
            inp.showAndWait();

            if (inp.hasCancelled() || CardLists.getTotalPower(inp.getSelected()) < i) {
                return false;
            } else {
                return executePayment(ability, inp.getSelected());
            }
        }
        
        InputSelectCards inp = new InputSelectCardsFromList(c, c, typeList);
        inp.setMessage("Select a " + getDescriptiveType() + " to tap (%d left)");
        inp.showAndWait();
        if ( inp.hasCancelled() )
            return false;

        return executePayment(ability, inp.getSelected());
    }

    /* (non-Javadoc)
     * @see forge.card.cost.CostPart#decideAIPayment(forge.game.player.AIPlayer, forge.card.spellability.SpellAbility, forge.Card)
     */
    @Override
    public PaymentDecision decideAIPayment(Player ai, SpellAbility ability, Card source) {
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
        if (this.getType().contains("sharesCreatureTypeWith") || this.getType().contains("withTotalPowerGE")) {
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
