package forge.game.cost;

import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

public class CostBehold extends CostReveal {

    private static final long serialVersionUID = 1L;

    public CostBehold(String amount, String type, String description) {
        super(amount, type, description, "Hand,Battlefield");
    }

    @Override
    public boolean canPay(final SpellAbility ability, final Player payer, final boolean effect) {
        CardCollectionView handList = payer.getCardsIn(revealFrom);
        final int amount = this.getAbilityAmount(ability);
        // currently only creatures (Celestial Reunion)
        if (this.getType().endsWith("ChosenType")) {
            for (final Card card : handList) {
                if (CardLists.count(handList, CardPredicates.sharesCreatureTypeWith(card)) >= amount) {
                    return true;
                }
            }
            return false;
        }
        return super.canPay(ability, payer, effect);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Behold ");

        final Integer i = this.convertAmount();

        final String desc = this.getTypeDescription() == null ? this.getType() : this.getTypeDescription();

        sb.append(Cost.convertAmountTypeToWords(i, this.getAmount(), desc));

        return sb.toString();
    }

    // Inputs
    public <T> T accept(ICostVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
