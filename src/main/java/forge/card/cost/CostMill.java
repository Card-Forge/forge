package forge.card.cost;

import java.util.Iterator;

import forge.AllZone;
import forge.Card;
import forge.CardList;
import forge.Constant;
import forge.Constant.Zone;
import forge.GameActionUtil;
import forge.Player;
import forge.PlayerZone;
import forge.card.abilityFactory.AbilityFactory;
import forge.card.spellability.SpellAbility;

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
        this.amount = amount;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * forge.card.cost.CostPart#canPay(forge.card.spellability.SpellAbility,
     * forge.Card, forge.Player, forge.card.cost.Cost)
     */
    @Override
    public final boolean canPay(final SpellAbility ability, final Card source, final Player activator, final Cost cost) {
        PlayerZone zone = activator.getZone(Constant.Zone.Library);

        Integer i = convertAmount();

        if (i == null) {
            String sVar = source.getSVar(amount);
            if (sVar.equals("XChoice")) {
                return true;
            }

            i = AbilityFactory.calculateAmount(source, amount, ability);
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
    public final boolean decideAIPayment(final SpellAbility ability, final Card source, final Cost_Payment payment) {
        resetList();

        Integer c = convertAmount();
        if (c == null) {
            String sVar = source.getSVar(amount);
            // Generalize this
            if (sVar.equals("XChoice")) {
                return false;
            }

            c = AbilityFactory.calculateAmount(source, amount, ability);
        }

        list = AllZone.getComputerPlayer().getCardsIn(Zone.Library, c);

        if (list == null || list.size() < c) {
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
    public final void payAI(final SpellAbility ability, final Card source, final Cost_Payment payment) {
        for (Card c : list)
            AllZone.getGameAction().moveToGraveyard(c);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * forge.card.cost.CostPart#payHuman(forge.card.spellability.SpellAbility,
     * forge.Card, forge.card.cost.Cost_Payment)
     */
    @Override
    public final boolean payHuman(final SpellAbility ability, final Card source, final Cost_Payment payment) {
        String amount = getAmount();
        Integer c = convertAmount();
        Player activator = ability.getActivatingPlayer();

        if (c == null) {
            String sVar = source.getSVar(amount);
            // Generalize this
            if (sVar.equals("XChoice")) {
                c = CostUtil.chooseXValue(source, list.size());
            } else {
                c = AbilityFactory.calculateAmount(source, amount, ability);
            }
        }
        CardList list = activator.getCardsIn(Zone.Library, c);

        if (list == null || list.size() > c) {
            // I don't believe this is possible
            payment.cancelCost();
            return false;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Mill ").append(c).append(" cards from your library?");

        boolean doMill = GameActionUtil.showYesNoDialog(source, sb.toString());
        if (doMill) {
            resetList();
            Iterator<Card> itr = list.iterator();
            while (itr.hasNext()) {
                Card card = (Card) itr.next();
                addToList(card);
                AllZone.getGameAction().moveToGraveyard(card);
            }
            addListToHash(ability, "Milled");
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
        StringBuilder sb = new StringBuilder();
        Integer i = convertAmount();
        sb.append("Put the top ");

        if (i != null) {
            sb.append(i);
        } else {
            sb.append(amount);
        }

        sb.append(" card");
        if (i == null || i > 1) {
            sb.append("s");
        }
        sb.append(" from the top of your library into your graveyard");

        return sb.toString();
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.card.cost.CostPart#refund(forge.Card)
     */
    @Override
    public void refund(final Card source) {
    }
}
