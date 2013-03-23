package forge.control.input;

import java.util.concurrent.CountDownLatch;

import forge.Card;
import forge.card.cost.CostPartMana;
import forge.card.cost.CostPayment;
import forge.card.mana.ManaCostBeingPaid;
import forge.card.spellability.SpellAbility;
import forge.game.GameState;
import forge.gui.match.CMatchUI;
import forge.view.ButtonUtil;

public class InputPayManaX extends InputPayManaBase {
    private static final long serialVersionUID = -6900234444347364050L;
    private int xPaid = 0;
    private final String colorX;
    private final String strX;
    private String colorsPaid;
    private final CostPartMana costMana;
    private final CostPayment payment;
    private final CountDownLatch cdlFinished;


    public InputPayManaX(final GameState game, final SpellAbility sa0, final CostPayment payment0, final CostPartMana costMana0, final CountDownLatch cdl)
    {
        super(game, sa0);

        payment = payment0;
        xPaid = 0;
        colorX =  saPaidFor.hasParam("XColor") ? saPaidFor.getParam("XColor") : "";
        colorsPaid = saPaidFor.getSourceCard().getColorsPaid();
        costMana = costMana0;
        strX = Integer.toString(costMana.getXMana());
        manaCost = new ManaCostBeingPaid(strX);
        cdlFinished = cdl;
    }

    @Override
    public void showMessage() {
        if (xPaid == 0 && !costMana.canXbe0() || this.colorX.equals("") && !this.manaCost.toString().equals(strX)) {
            ButtonUtil.enableOnlyCancel();
            // only cancel if partially paid an X value
            // or X is 0, and x can't be 0
        } else {
            ButtonUtil.enableAllFocusOk();
        }

        StringBuilder msg = new StringBuilder("Pay X Mana Cost for ");
        msg.append(saPaidFor.getSourceCard().getName()).append("\n").append(this.xPaid);
        msg.append(" Paid so far.");
        if (!costMana.canXbe0()) {
            msg.append(" X Can't be 0.");
        }

        CMatchUI.SINGLETON_INSTANCE.showMessage(msg.toString());
    }

    // selectCard
    @Override
    public void selectCard(final Card card) {
        activateManaAbility(card, this.colorX.isEmpty() ? this.manaCost : new ManaCostBeingPaid(this.colorX));
    }
    

    @Override
    protected void onManaAbilityPaid() {
        if (this.manaCost.isPaid()) {
            if (!this.colorsPaid.contains(this.manaCost.getColorsPaid())) {
                this.colorsPaid += this.manaCost.getColorsPaid();
            }
            this.manaCost = new ManaCostBeingPaid(strX);
            this.xPaid++;
        }
    }

    @Override
    public void selectButtonCancel() {
        this.stop();
        payment.cancelCost();
        cdlFinished.countDown();
    }

    @Override
    public void selectButtonOK() {
        this.stop();
        done();
    }

    @Override
    public void selectManaPool(String color) {
        useManaFromPool(color, this.colorX.isEmpty() ? this.manaCost : new ManaCostBeingPaid(this.colorX));
    }

    /* (non-Javadoc)
     * @see forge.control.input.Input#isClassUpdated()
     */
    @Override
    public void isClassUpdated() {
    }


    @Override
    protected void done() {
        payment.getCard().setXManaCostPaid(this.xPaid);
        payment.setPaidPart(costMana);
        payment.getCard().setColorsPaid(this.colorsPaid);
        payment.getCard().setSunburstValue(this.colorsPaid.length());
        cdlFinished.countDown();
    }
}
