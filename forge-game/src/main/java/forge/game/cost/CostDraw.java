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

import java.util.Map;

import forge.game.ability.AbilityKey;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.player.PlayerCollection;
import forge.game.spellability.SpellAbility;

/**
 * The Class CostDraw.
 */
public class CostDraw extends CostPart {

    private static final long serialVersionUID = 1L;

    /**
     * CostDraw.
     * @param amount
     * @param playerSelector
     */
    public CostDraw(final String amount, final String playerSelector) {
        super(amount, playerSelector, null);
    }

    @Override
    public int paymentOrder() { return 15; }

    /*
     * (non-Javadoc)
     * 
     * @see forge.card.cost.CostPart#toString()
     */
    @Override
    public final String toString() {
        final StringBuilder sb = new StringBuilder();
        final Integer i = this.convertAmount();
        sb.append("Draw ").append(Cost.convertAmountTypeToWords(i, this.getAmount(), "Card"));
        return sb.toString();
    }

    /** 
     * getPotentialPlayers.
     * @param payer
     * @param source
     */
    public PlayerCollection getPotentialPlayers(final Player payer, final SpellAbility ability) {
        PlayerCollection res = new PlayerCollection();
        String type = this.getType();
        final Card source = ability.getHostCard();

        int c = this.getAbilityAmount(ability);

        for (Player p : payer.getGame().getPlayers()) {
            if (p.isValid(type, payer, source, ability) && p.canDrawAmount(c)) {
                res.add(p);
            }
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
    public final boolean canPay(final SpellAbility ability, final Player payer, final boolean effect) {
        return !getPotentialPlayers(payer, ability).isEmpty();
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.card.cost.CostPart#payAI(forge.card.spellability.SpellAbility,
     * forge.Card, forge.card.cost.Cost_Payment)
     */
    @Override
    public final boolean payAsDecided(final Player ai, final PaymentDecision decision, SpellAbility ability, final boolean effect) {
        Map<AbilityKey, Object> moveParams = AbilityKey.newMap();
        moveParams.put(AbilityKey.LastStateBattlefield, ability.getLastStateBattlefield());
        moveParams.put(AbilityKey.LastStateGraveyard, ability.getLastStateGraveyard());
        for (final Player p : decision.players) {
            p.drawCards(decision.c, ability, moveParams);
        }
        return true;
    }

    @Override
    public <T> T accept(ICostVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
