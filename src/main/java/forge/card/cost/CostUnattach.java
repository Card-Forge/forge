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

import java.util.List;

import forge.Card;
import forge.CardLists;
import forge.card.spellability.SpellAbility;
import forge.game.GameState;
import forge.game.player.AIPlayer;
import forge.game.player.Player;
import forge.gui.GuiDialog;

/**
 * The Class CostUnattach.
 */
public class CostUnattach extends CostPartWithList {
    // Unattach<CARDNAME> if ability is on the Equipment
    // Unattach<Card.Attached+namedHeartseeker/Equipped Heartseeker> if equipped creature has the ability

    /**
     * Instantiates a new cost unattach.
     */
    public CostUnattach(final String type, final String desc) {
        super("1", type, desc);
    }

    @Override
    public boolean isUndoable() { return false; }

    @Override
    public boolean isReusable() { return false; }

    /*
     * (non-Javadoc)
     * 
     * @see forge.card.cost.CostPart#toString()
     */
    @Override
    public final String toString() {
        return String.format("Unattach %s", this.getTypeDescription());
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
        final String type = this.getType();
        if (type.equals("CARDNAME")) {
            if (source.isEquipping()) {
                return true;
            }
        } else if (type.equals("OriginalHost")) {
            Card originalEquipment = ability.getOriginalHost();
            if (originalEquipment.isEquipping()) {
                return true;
            }
        } else {
            List<Card> equipped = source.getEquippedBy();
            if (CardLists.getValidCards(equipped, type, activator, source).size() > 0) {
                return true;
            }
        }

        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.card.cost.CostPart#payAI(forge.card.spellability.SpellAbility,
     * forge.Card, forge.card.cost.Cost_Payment)
     */
    @Override
    public final void payAI(final AIPlayer ai, final SpellAbility ability, final Card source, final CostPayment payment, final GameState game) {
        Card cardToUnattach = findCardToUnattach(source, (Player) ai, ability);
        if (cardToUnattach == null) {
            // We really shouldn't be able to get here if there's nothing to unattach
            return;
        }

        Card equippingCard = cardToUnattach.getEquipping().get(0);
        cardToUnattach.unEquipCard(equippingCard);
        this.addToList(cardToUnattach);
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
        this.resetList();
        Player activator = ability.getActivatingPlayer();
        Card cardToUnattach = findCardToUnattach(source, activator, ability);
        if (cardToUnattach != null && GuiDialog.confirm(source, String.format("Unattach %s?", cardToUnattach.toString()))) {
            Card equippingCard = cardToUnattach.getEquipping().get(0);
            cardToUnattach.unEquipCard(equippingCard);
            this.addToList(cardToUnattach);
            payment.setPaidManaPart(this);
        } else {
            payment.setCancel(true);
            payment.getRequirements().finishPaying();
            return false;
        }

        return true;
    }

    private Card findCardToUnattach(final Card source, Player activator, SpellAbility ability) {
        if (getType().equals("CARDNAME")) {
            if (source.isEquipping()) {
                return source;
            }
        } else if (getType().equals("OriginalHost")) {
            Card originalEquipment = ability.getOriginalHost();
            if (originalEquipment.isEquipping()) {
                return originalEquipment;
            }
        } else {
            List<Card> attachees = CardLists.getValidCards(source.getEquippedBy(), this.getType(), activator, source);
            if (attachees.size() > 0) {
                // Just pick the first one, although maybe give a dialog
                return attachees.get(0);
            }
        }
        return null;
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
        return true;
    }
}
