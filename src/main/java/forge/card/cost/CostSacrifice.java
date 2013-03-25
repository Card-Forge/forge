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
import forge.CardLists;
import forge.FThreads;
import forge.card.ability.AbilityUtils;
import forge.card.spellability.SpellAbility;
import forge.control.input.InputPayment;
import forge.game.GameState;
import forge.game.ai.ComputerUtil;
import forge.game.player.AIPlayer;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.gui.GuiDialog;
import forge.gui.match.CMatchUI;
import forge.view.ButtonUtil;

/**
 * The Class CostSacrifice.
 */
public class CostSacrifice extends CostPartWithList {

    /** 
     * TODO: Write javadoc for this type.
     *
     */
    public static final class InputPayCostSacrificeFromList extends InputPayCostBase {
        private final CostSacrifice part;
        private final SpellAbility sa;
        private final int nNeeded;
        private final List<Card> typeList;
        private static final long serialVersionUID = 2685832214519141903L;
        private int nSacrifices = 0;

        /**
         * TODO: Write javadoc for Constructor.
         * @param part
         * @param sa
         * @param nNeeded
         * @param payment
         * @param typeList
         */
        public InputPayCostSacrificeFromList(CostSacrifice part, SpellAbility sa, int nNeeded, List<Card> typeList) {
            this.part = part;
            this.sa = sa;
            this.nNeeded = nNeeded;
            this.typeList = typeList;
        }

        @Override
        public void showMessage() {
            if (nNeeded == 0) {
                this.done();
            }

            final StringBuilder msg = new StringBuilder("Sacrifice ");
            final int nLeft = nNeeded - this.nSacrifices;
            msg.append(nLeft).append(" ");
            msg.append(part.getDescriptiveType());
            if (nLeft > 1) {
                msg.append("s");
            }

            CMatchUI.SINGLETON_INSTANCE.showMessage(msg.toString());
            ButtonUtil.enableOnlyCancel();
        }

        @Override
        public void selectCard(final Card card) {
            if (typeList.contains(card)) {
                this.nSacrifices++;
                part.executePayment(sa, card);
                typeList.remove(card);
                // in case nothing else to sacrifice
                if (this.nSacrifices == nNeeded) {
                    this.done();
                } else if (typeList.isEmpty()) {
                    // happen
                    this.cancel();
                } else {
                    this.showMessage();
                }
            }
        }
    }

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

    /*
     * (non-Javadoc)
     * 
     * @see forge.card.cost.CostPart#toString()
     */
    @Override
    public final String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Sacrifice ");

        final Integer i = this.convertAmount();

        if (this.payCostFromSource()) {
            sb.append(this.getType());
        } else {
            final String desc = this.getTypeDescription() == null ? this.getType() : this.getTypeDescription();
            if (i != null) {
                sb.append(Cost.convertIntAndTypeToWords(i, desc));
            } else {
                sb.append(Cost.convertAmountTypeToWords(this.getAmount(), desc));
            }
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
    public final boolean canPay(final SpellAbility ability, final Card source, final Player activator, final Cost cost, final GameState game) {
        // You can always sac all
        if (!this.payCostFromSource()) {
            // If the sacrificed type is dependant on an annoucement, can't necesarily rule out the CanPlay call
            boolean needsAnnoucement = ability.hasParam("Announce") && this.getType().contains(ability.getParam("Announce"));
            
            List<Card> typeList = new ArrayList<Card>(activator.getCardsIn(ZoneType.Battlefield));
            typeList = CardLists.getValidCards(typeList, this.getType().split(";"), activator, source);
            final Integer amount = this.convertAmount();

            if (activator.hasKeyword("You can't sacrifice creatures to cast spells or activate abilities.")) {
                typeList = CardLists.getNotType(typeList, "Creature");
            }

            if (!needsAnnoucement && (amount != null) && (typeList.size() < amount)) {
                return false;
            }

            // If amount is null, it's either "ALL" or "X"
            // if X is defined, it needs to be calculated and checked, if X is
            // choice, it can be Paid even if it's 0
        }
        else {
            if (!source.isInPlay()) {
                return false;
            }
            else if (source.isCreature() && activator.hasKeyword("You can't sacrifice creatures to cast spells or activate abilities.")) {
                return false;
            }
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
        this.addListToHash(ability, "Sacrificed");
        for (final Card c : this.getList()) {
            executePayment(ability, c);
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
    public final boolean payHuman(final SpellAbility ability, final GameState game) {
        final String amount = this.getAmount();
        final Card source = ability.getSourceCard();
        final String type = this.getType();
        final Player activator = ability.getActivatingPlayer();
        List<Card> list = new ArrayList<Card>(activator.getCardsIn(ZoneType.Battlefield));
        list = CardLists.getValidCards(list, type.split(";"), activator, source);
        if (activator.hasKeyword("You can't sacrifice creatures to cast spells or activate abilities.")) {
            list = CardLists.getNotType(list, "Creature");
        }

        if (this.payCostFromSource()) {
            if (source.getController() == ability.getActivatingPlayer() && source.isInPlay()) {
                if (!GuiDialog.confirm(source, source.getName() + " - Sacrifice?")) 
                    return false;
                executePayment(ability, source);
                return true;
            }
        } else if (amount.equals("All")) {
            this.setList(list);
            // TODO Ask First
            for (final Card card : list) {
                executePayment(ability, card);
            }
            return true;
        } else {
            Integer c = this.convertAmount();
            if (c == null) {
                // Generalize this
                if (ability.getSVar(amount).equals("XChoice")) {
                    c = CostUtil.chooseXValue(source, ability, list.size());
                } else {
                    c = AbilityUtils.calculateAmount(source, amount, ability);
                }
            }
            if (0 == c.intValue()) {
                return true;
            }
            InputPayment inp = new InputPayCostSacrificeFromList(this, ability, c, list);
            FThreads.setInputAndWait(inp);
            return inp.isPaid();
        }
        return false;
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
        final Player activator = ability.getActivatingPlayer();
        if (this.payCostFromSource()) {
            this.getList().add(source);
        } else if (this.getAmount().equals("All")) {
            /*List<Card> typeList = new ArrayList<Card>(activator.getCardsIn(ZoneType.Battlefield));
            typeList = CardLists.getValidCards(typeList, this.getType().split(";"), activator, source);
            if (activator.hasKeyword("You can't sacrifice creatures to cast spells or activate abilities.")) {
                typeList = CardLists.getNotType(typeList, "Creature");
            }*/
            // Does the AI want to use Sacrifice All?
            return false;
        } else {
            Integer c = this.convertAmount();
            if (c == null) {
                if (ability.getSVar(this.getAmount()).equals("XChoice")) {
                    return false;
                }

                c = AbilityUtils.calculateAmount(source, this.getAmount(), ability);
            }
            this.setList(ComputerUtil.chooseSacrificeType(activator, this.getType(), source, ability.getTargetCard(), c));
            if (this.getList() == null) {
                return false;
            }
        }
        return true;
    }

    /* (non-Javadoc)
     * @see forge.card.cost.CostPartWithList#executePayment(forge.card.spellability.SpellAbility, forge.Card)
     */
    @Override
    public void executePayment(SpellAbility ability, Card targetCard) {
        this.addToList(targetCard);
        ability.getActivatingPlayer().getGame().getAction().sacrifice(targetCard, ability);
    }

    /* (non-Javadoc)
     * @see forge.card.cost.CostPartWithList#getHashForList()
     */
    @Override
    public String getHashForList() {
        return "Sacrificed";
    }

    // Inputs

}
