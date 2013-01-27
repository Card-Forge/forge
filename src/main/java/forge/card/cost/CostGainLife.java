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

import forge.Card;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.spellability.SpellAbility;
import forge.game.GameState;
import forge.game.player.AIPlayer;
import forge.game.player.Player;
import forge.gui.GuiChoose;

/**
 * The Class CostGainLife.
 */
public class CostGainLife extends CostPart {
    private final int cntPlayers; // MAX_VALUE means ALL/EACH PLAYERS
    private int lastPaidAmount = 0;

    /**
     * Gets the last paid amount.
     * 
     * @return the last paid amount
     */
    public final int getLastPaidAmount() {
        return this.lastPaidAmount;
    }

    /**
     * Sets the last paid amount.
     * 
     * @param paidAmount
     *            the new last paid amount
     */
    public final void setLastPaidAmount(final int paidAmount) {
        this.lastPaidAmount = paidAmount;
    }

    /**
     * Instantiates a new cost gain life.
     * 
     * @param amount
     *            the amount
     */
    public CostGainLife(final String amount, String playerSelector, int qty) {
        super(amount, playerSelector, null);
        cntPlayers = qty;
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.card.cost.CostPart#toString()
     */
    @Override
    public final String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Have an opponent gain ").append(this.getAmount()).append(" life");
        return sb.toString();
    }
    
    private List<Player> getPotentialTargets(final GameState game, final Player payer, final Card source)
    {
        List<Player> res = new ArrayList<Player>();
        for(Player p : game.getPlayers())
        {
            if(p.isValid(getType(), payer, source))
                res.add(p);
        }
        return res;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * forge.card.cost.CostPart#canPay(forge.card.spellability.SpellAbility,
     * forge.Card, forge.Player, forge.card.cost.Cost)
     */
    @Override
    public final boolean canPay(final SpellAbility ability, final Card source, final Player activator, final Cost cost, final GameState game) {
        final Integer amount = this.convertAmount();
        if ( amount == null ) return false;

        int cntAbleToGainLife = 0;
        List<Player> possibleTargets = getPotentialTargets(game, activator, source);

        for (final Player opp : possibleTargets) {
            if (opp.canGainLife()) {
                cntAbleToGainLife++;
            }
        }

        return cntPlayers < Integer.MAX_VALUE ? cntAbleToGainLife >= cntPlayers : cntAbleToGainLife == possibleTargets.size();
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.card.cost.CostPart#payAI(forge.card.spellability.SpellAbility,
     * forge.Card, forge.card.cost.Cost_Payment)
     */
    @Override
    public final void payAI(final AIPlayer ai, final SpellAbility ability, final Card source, final CostPayment payment, final GameState game) {
        int playersLeft = cntPlayers;
        for (final Player opp : getPotentialTargets(game, ai, source)) {
            if (opp.canGainLife() && playersLeft > 0) {
                playersLeft--;
                opp.gainLife(this.getLastPaidAmount(), null);
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * forge.card.cost.CostPart#payHuman(forge.card.spellability.SpellAbility,
     * forge.Card, forge.card.cost.Cost_Payment)
     */
    @Override
    public final boolean payHuman(final SpellAbility ability, final Card source, final CostPayment payment, final GameState game) {
        final String amount = this.getAmount();
        final Player activator = ability.getActivatingPlayer();
        final int life = activator.getLife();

        Integer c = this.convertAmount();
        if (c == null) {
            final String sVar = ability.getSVar(amount);
            // Generalize this
            if (sVar.equals("XChoice")) {
                c = CostUtil.chooseXValue(source, ability,  life);
            } else {
                c = AbilityFactory.calculateAmount(source, amount, ability);
            }
        }

        final List<Player> oppsThatCanGainLife = new ArrayList<Player>();
        for (final Player opp : getPotentialTargets(game, activator, source)) {
            if (opp.canGainLife()) {
                oppsThatCanGainLife.add(opp);
            }
        }

        if(cntPlayers == Integer.MAX_VALUE) { // applied to all players who can gain
            for(Player opp: oppsThatCanGainLife)
                opp.gainLife(c, null);
            payment.setPaidManaPart(this);
            return true;
        }
            
        final StringBuilder sb = new StringBuilder();
        sb.append(source.getName()).append(" - Choose an opponent to gain ").append(c).append(" life:");

        
        for(int playersLeft = cntPlayers; playersLeft > 0; playersLeft--) {
            final Player chosenToGain = GuiChoose.oneOrNone(sb.toString(), oppsThatCanGainLife);
            if (null == chosenToGain) {
                payment.setCancel(true);
                payment.getRequirements().finishPaying();
                return false;
            } else {
                final Player chosen = chosenToGain;
                chosen.gainLife(c, null);
            }
        }
        
        payment.setPaidManaPart(this);
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * forge.card.cost.CostPart#decideAIPayment(forge.card.spellability.SpellAbility
     * , forge.Card, forge.card.cost.Cost_Payment)
     */
    @Override
    public final boolean decideAIPayment(final AIPlayer ai, final SpellAbility ability, final Card source, final CostPayment payment) {


        Integer c = this.convertAmount();
        if (c == null) {
            final String sVar = ability.getSVar(this.getAmount());
            // Generalize this
            if (sVar.equals("XChoice")) {
                return false;
            } else {
                c = AbilityFactory.calculateAmount(source, this.getAmount(), ability);
            }
        }

        final List<Player> oppsThatCanGainLife = new ArrayList<Player>();
        for (final Player opp : ai.getOpponents()) {
            if (opp.canGainLife()) {
                oppsThatCanGainLife.add(opp);
            }
        }

        if (oppsThatCanGainLife.size() == 0) {
            return false;
        }
        this.setLastPaidAmount(c);
        return true;
    }
}
