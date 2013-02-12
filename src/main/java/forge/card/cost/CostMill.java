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
import java.util.Iterator;
import java.util.List;

import forge.Card;

import forge.Singletons;
import forge.card.ability.AbilityUtils;
import forge.card.spellability.SpellAbility;
import forge.game.GameState;
import forge.game.player.AIPlayer;
import forge.game.player.Player;
import forge.game.zone.PlayerZone;
import forge.game.zone.ZoneType;
import forge.gui.GuiDialog;

/**
 * This is for the "Mill" Cost. Putting cards from the top of your library into
 * your graveyard as a cost. This Cost doesn't appear on very many cards, but
 * might appear in more in the future. This will show up in the form of Mill<1>
 */
public class CostMill extends CostPartWithList {

    /**
     * Instantiates a new cost mill.
     * 
     * @param amount
     *            the amount
     */
    public CostMill(final String amount) {
        this.setAmount(amount);
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
        final PlayerZone zone = activator.getZone(ZoneType.Library);

        Integer i = this.convertAmount();

        if (i == null) {
            final String sVar = ability.getSVar(this.getAmount());
            if (sVar.equals("XChoice")) {
                return true;
            }

            i = AbilityUtils.calculateAmount(source, this.getAmount(), ability);
        }

        return i < zone.size();
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
        this.resetList();

        Integer c = this.convertAmount();
        if (c == null) {
            final String sVar = ability.getSVar(this.getAmount());
            // Generalize this
            if (sVar.equals("XChoice")) {
                return false;
            }

            c = AbilityUtils.calculateAmount(source, this.getAmount(), ability);
        }

        this.setList(new ArrayList<Card>(ai.getCardsIn(ZoneType.Library, c)));

        if ((this.getList() == null) || (this.getList().size() < c)) {
            return false;
        }

        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.card.cost.CostPart#payAI(forge.card.spellability.SpellAbility,
     * forge.Card, forge.card.cost.Cost_Payment)
     */
    @Override
    public final void payAI(final AIPlayer ai, final SpellAbility ability, final Card source, final CostPayment payment, final GameState game) {
        for (final Card c : this.getList()) {
            Singletons.getModel().getGame().getAction().moveToGraveyard(c);
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
        Integer c = this.convertAmount();
        final Player activator = ability.getActivatingPlayer();

        if (c == null) {
            final String sVar = ability.getSVar(amount);
            // Generalize this
            if (sVar.equals("XChoice")) {
                c = CostUtil.chooseXValue(source, ability, this.getList().size());
            } else {
                c = AbilityUtils.calculateAmount(source, amount, ability);
            }
        }
        final List<Card> list = activator.getCardsIn(ZoneType.Library, c);

        if ((list == null) || (list.size() > c)) {
            // I don't believe this is possible
            payment.cancelCost();
            return false;
        }

        final StringBuilder sb = new StringBuilder();
        sb.append("Mill ").append(c).append(" cards from your library?");

        final boolean doMill = GuiDialog.confirm(source, sb.toString());
        if (doMill) {
            this.resetList();
            final Iterator<Card> itr = list.iterator();
            while (itr.hasNext()) {
                final Card card = itr.next();
                this.addToList(card);
                Singletons.getModel().getGame().getAction().moveToGraveyard(card);
            }
            this.addListToHash(ability, "Milled");
            payment.paidCost(this);
            return false;
        } else {
            payment.cancelCost();
            return false;
        }
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
        sb.append("Put the top ");

        if (i != null) {
            sb.append(i);
        } else {
            sb.append(this.getAmount());
        }

        sb.append(" card");
        if ((i == null) || (i > 1)) {
            sb.append("s");
        }
        sb.append(" from the top of your library into your graveyard");

        return sb.toString();
    }

}
