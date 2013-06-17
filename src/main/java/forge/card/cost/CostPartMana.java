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

import forge.Card;
import forge.Singletons;
import forge.card.MagicColor;
import forge.card.ability.AbilityUtils;
import forge.card.mana.ManaCost;
import forge.card.mana.ManaCostBeingPaid;
import forge.card.mana.ManaCostShard;
import forge.card.spellability.SpellAbility;
import forge.game.Game;
import forge.game.ai.ComputerUtilMana;
import forge.game.player.Player;
import forge.gui.input.InputPayMana;
import forge.gui.input.InputPayManaOfCostPayment;
import forge.gui.input.InputPayManaX;

/**
 * The mana component of any spell or ability cost
 */
public class CostPartMana extends CostPart {
    // "Leftover"
    private final ManaCost cost;
    private boolean xCantBe0 = false;
    private final String restriction;

    /**
     * Instantiates a new cost mana.
     * 
     * @param mana
     *            the mana
     * @param amount
     *            the amount
     * @param xCantBe0 TODO
     */
    public CostPartMana(final ManaCost cost, String restriction, boolean xCantBe0) {
        this.cost = cost;
        this.xCantBe0 = xCantBe0;
        this.restriction = restriction;
    }

    /**
     * Gets the mana.
     * 
     * @return the mana
     */
    public final ManaCost getMana() {
        // Only used for Human to pay for non-X cost first
        return this.cost;
    }

    public final int getAmountOfX() {
        return this.cost.getShardCount(ManaCostShard.X);
    }

    /**
     * @return the xCantBe0
     */
    public boolean canXbe0() {
        return !xCantBe0;
    }

    /**
     * Gets the mana to pay.
     * 
     * @return the mana to pay
     */
    public final ManaCost getManaToPay() {
        return cost;
    }
    
    @Override
    public boolean isReusable() { return true; }

    @Override
    public boolean isUndoable() { return true; }
    

    @Override
    public final String toString() {
        return cost.toString();
    }


    @Override
    public final boolean canPay(final SpellAbility ability) {
        // For now, this will always return true. But this should probably be
        // checked at some point
        return true;
    }

    @Override
    public final boolean payHuman(final SpellAbility ability, final Game game) {
        final Card source = ability.getSourceCard();
        ManaCostBeingPaid toPay = new ManaCostBeingPaid(getManaToPay(), restriction);

        boolean xWasBilled = false;
        String xInCard = source.getSVar("X");
        if (this.getAmountOfX() > 0 && !"Count$xPaid".equals(xInCard)) { // announce X will overwrite whatever was in card script
            // this currently only works for things about Targeted object
            int xCost = AbilityUtils.calculateAmount(source, "X", ability) * this.getAmountOfX();
            byte xColor = MagicColor.fromName(ability.hasParam("XColor") ? ability.getParam("XColor") : "1"); 
            toPay.increaseShard(ManaCostShard.valueOf(xColor), xCost);
            xWasBilled = true;
        }
        int timesMultikicked = ability.getSourceCard().getKickerMagnitude();
        if ( timesMultikicked > 0 && ability.isAnnouncing("Multikicker")) {
            ManaCost mkCost = ability.getMultiKickerManaCost();
            for(int i = 0; i < timesMultikicked; i++)
                toPay.combineManaCost(mkCost);
        }

        InputPayMana inpPayment;
        toPay.applySpellCostChange(ability, false);
        if (ability.isOffering() && ability.getSacrificedAsOffering() == null) {
            System.out.println("Sacrifice input for Offering cancelled");
            return false;
        }
        if (!toPay.isPaid()) {
            inpPayment = new InputPayManaOfCostPayment(toPay, ability);
            Singletons.getControl().getInputQueue().setInputAndWait(inpPayment);
            if (!inpPayment.isPaid()) {
                return handleOfferingAndConvoke(ability, true, false);
            }

            source.setColorsPaid(toPay.getColorsPaid());
            source.setSunburstValue(toPay.getSunburst());
        }
        if (this.getAmountOfX() > 0) {
            if (!ability.isAnnouncing("X") && !xWasBilled) {
                source.setXManaCostPaid(0);
                inpPayment = new InputPayManaX(ability, this.getAmountOfX(), this.canXbe0());
                Singletons.getControl().getInputQueue().setInputAndWait(inpPayment);
                if (!inpPayment.isPaid()) {
                    return false;
                }
            } else {
                int x = AbilityUtils.calculateAmount(source, "X", ability);
                source.setXManaCostPaid(x);
            }
        }

        // Handle convoke and offerings
        if (ability.isOffering() && ability.getSacrificedAsOffering() != null) {
            System.out.println("Finishing up Offering");
            final Card offering = ability.getSacrificedAsOffering();
            offering.setUsedToPay(false);
            game.getAction().sacrifice(offering, ability);
            ability.resetSacrificedAsOffering();
        }
        if (ability.getTappedForConvoke() != null) {
            for (final Card c : ability.getTappedForConvoke()) {
                c.setTapped(false);
                c.tap();
            }
            ability.clearTappedForConvoke();
        }
        return handleOfferingAndConvoke(ability, false, true);

    }

    /* (non-Javadoc)
     * @see forge.card.cost.CostPart#payAI(forge.card.cost.PaymentDecision, forge.game.player.AIPlayer, forge.card.spellability.SpellAbility, forge.Card)
     */
    @Override
    public void payAI(PaymentDecision decision, Player ai, SpellAbility ability, Card source) {
        ComputerUtilMana.payManaCost(ai, ability);
    }


    /* (non-Javadoc)
     * @see forge.card.cost.CostPart#decideAIPayment(forge.game.player.AIPlayer, forge.card.spellability.SpellAbility, forge.Card)
     */
    @Override
    public PaymentDecision decideAIPayment(Player ai, SpellAbility ability, Card source) {
        return new PaymentDecision(0);
    }

    /**
     * TODO: Write javadoc for this method.
     * @return
     */
    public String getRestiction() {
        // TODO Auto-generated method stub
        return restriction;
    }

    private boolean handleOfferingAndConvoke(final SpellAbility ability, boolean manaInputCancelled, boolean isPaid) {
        boolean done = !manaInputCancelled && isPaid;
        if (ability.isOffering() && ability.getSacrificedAsOffering() != null) {
            final Card offering = ability.getSacrificedAsOffering();
            offering.setUsedToPay(false);
            if (done) {
                ability.getSourceCard().getGame().getAction().sacrifice(offering, ability);
            }
            ability.resetSacrificedAsOffering();
        }
        if (ability.getTappedForConvoke() != null) {
            for (final Card c : ability.getTappedForConvoke()) {
                c.setTapped(false);
                if (done) {
                    c.tap();
                }
            }
            ability.clearTappedForConvoke();
        }
        return done;
    }
}
