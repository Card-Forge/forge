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

import java.util.List;

import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.util.TextUtil;

/**
 * The Class CostUnattach.
 */
public class CostUnattach extends CostPartWithList {
    // Unattach<CARDNAME> if ability is on the Equipment
    // Unattach<Card.Attached+namedHeartseeker/Equipped Heartseeker> if equipped creature has the ability

    /**
     * Serializables need a version ID.
     */
    private static final long serialVersionUID = 1L;


    /**
     * Instantiates a new cost unattach.
     */
    public CostUnattach(final String type, final String desc) {
        super("1", type, desc);
    }

    @Override
    public boolean isUndoable() { return false; }

    @Override
    public boolean isReusable() { return true; }

    /*
     * (non-Javadoc)
     * 
     * @see forge.card.cost.CostPart#toString()
     */
    @Override
    public final String toString() {
        return TextUtil.concatWithSpace("Unattach", this.getTypeDescription());
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
        return findCardToUnattach(ability.getHostCard(), payer, ability) != null;
    }

    public Card findCardToUnattach(final Card source, Player activator, SpellAbility ability) {
        if (payCostFromSource()) {
            if (source.isEquipping()) {
                return source;
            }
        } else if (getType().equals("OriginalHost")) {
            Card originalEquipment = ability.getOriginalHost();
            if (originalEquipment.isEquipping()) {
                return originalEquipment;
            }
        } else {
            List<Card> attachees = CardLists.getValidCards(source.getEquippedBy(), this.getType(), activator, source, ability);
            if (attachees.size() > 0) {
                // Just pick the first one, although maybe give a dialog
                return attachees.get(0);
            }
        }
        return null;
    }

    /* (non-Javadoc)
     * @see forge.card.cost.CostPartWithList#executePayment(forge.card.spellability.SpellAbility, forge.Card)
     */
    @Override
    protected Card doPayment(Player payer, SpellAbility ability, Card targetCard, final boolean effect) {
        targetCard.unattachFromEntity(targetCard.getEntityAttachedTo());
        return targetCard;
    }

    @Override
    public String getHashForLKIList() {
        return "Unattached";
    }
    @Override
    public String getHashForCardList() {
    	return "UnattachedCards";
    }

    public <T> T accept(ICostVisitor<T> visitor) {
        return visitor.visit(this);
    }

}
