package forge.match.input;

import forge.card.ColorSet;
import forge.card.mana.ManaCost;
import forge.card.mana.ManaCostParser;
import forge.game.card.Card;
import forge.game.mana.Mana;
import forge.game.mana.ManaCostBeingPaid;
import forge.game.spellability.SpellAbility;
import forge.util.ITriggerEvent;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class InputPayManaX extends InputPayMana {
    private static final long serialVersionUID = -6900234444347364050L;
    private int xPaid = 0;
    private ArrayList<Mana> xPaidByColor = new ArrayList<>();
    private byte colorsPaid;
    private final ManaCost manaCostPerX;
    private final boolean xCanBe0;
    private boolean canceled = false;

    public InputPayManaX(final SpellAbility sa0, final int amountX, final boolean xCanBe0) {
        super(sa0, sa0.getActivatingPlayer());
        xPaid = 0;
        
        if (saPaidFor.hasParam("XColor")) {
            String xColor = saPaidFor.getParam("XColor");
            if (amountX == 1) {
                manaCostPerX = new ManaCost(new ManaCostParser(xColor));
            }
            else {
                List<String> list = new ArrayList<String>(amountX);
                for (int i = 0; i < amountX; i++) {
                    list.add(xColor);
                }
                manaCostPerX = new ManaCost(new ManaCostParser(StringUtils.join(list, ' ')));
            }
        }
        else {
            manaCostPerX = ManaCost.get(amountX);
        }
        manaCost = new ManaCostBeingPaid(manaCostPerX);

        this.xCanBe0 = xCanBe0;
        colorsPaid = saPaidFor.getHostCard().getColorsPaid(); // for effects like sunburst
    }

    /* (non-Javadoc)
     * @see forge.control.input.InputPayManaBase#isPaid()
     */
    @Override
    public boolean isPaid() {
        //return !( xPaid == 0 && !costMana.canXbe0() || this.colorX.equals("") && !this.manaCost.toString().equals(strX) );
        // return !( xPaid == 0 && !costMana.canXbe0()) && !(this.colorX.equals("") && !this.manaCost.toString().equals(strX));
        return !canceled && (xPaid > 0 || xCanBe0);
    }

    @Override
    protected boolean supportAutoPay() {
        return false;
    }

    @Override
    public void showMessage() {
        if (isFinished()) { return; }

        updateMessage();
    }

    @Override
    protected String getMessage() {
        StringBuilder msg = new StringBuilder("Pay X Mana Cost for ");
        msg.append(saPaidFor.getHostCard().getName()).append("\n").append(this.xPaid);
        msg.append(" Paid so far.");
        if (!xCanBe0) {
            msg.append(" X Can't be 0.");
        }
        // Enable just cancel is full X value hasn't been paid for multiple X values
        // or X is 0, and x can't be 0
        if (!isPaid()) {
            ButtonUtil.enableOnlyCancel();
        }
        else {
            ButtonUtil.enableAllFocusOk();
        }

        return msg.toString();
    }

    @Override
    protected boolean onCardSelected(final Card card, final ITriggerEvent triggerEvent) {
        // don't allow here the cards that produce only wrong colors
        return activateManaAbility(card, this.manaCost);
    }

    @Override
    protected void onManaAbilityPaid() {
        if (this.manaCost.isPaid()) {
            this.colorsPaid |= manaCost.getColorsPaid();
            this.manaCost = new ManaCostBeingPaid(manaCostPerX);
            this.xPaid++;
            this.xPaidByColor.add(saPaidFor.getPayingMana().get(0));
        }
    }

    @Override
    protected final void onCancel() {
        // If you hit cancel, isPaid needs to return false
        this.canceled = true;
        this.stop();
    }

    @Override
    protected final void onOk() {
        done();
        this.stop();
    }

    @Override
    protected void done() {
        final Card card = saPaidFor.getHostCard();
        card.setXManaCostPaid(this.xPaid);
        card.setXManaCostPaidByColor(this.xPaidByColor);
        card.setColorsPaid(this.colorsPaid);
        card.setSunburstValue(ColorSet.fromMask(this.colorsPaid).countColors());
    }
}
