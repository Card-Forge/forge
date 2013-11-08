package forge.gui.input;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import forge.Card;
import forge.card.ColorSet;
import forge.card.mana.ManaCostBeingPaid;
import forge.card.spellability.SpellAbility;
import forge.view.ButtonUtil;

public class InputPayManaX extends InputPayMana {
    private static final long serialVersionUID = -6900234444347364050L;
    private int xPaid = 0;
    private byte colorsPaid;
    private final String manaCostStr;
    private final boolean xCanBe0;
    private boolean canceled = false;


    public InputPayManaX(final SpellAbility sa0, final int amountX, final boolean xCanBe0)
    {
        super(sa0);
        xPaid = 0;
        if (saPaidFor.hasParam("XColor") )
        {
            String xColor = saPaidFor.getParam("XColor");
            if( amountX == 1 )
                manaCostStr = xColor;
            else {
                List<String> list = new ArrayList<String>(amountX);
                for(int i = 0; i < amountX; i++)
                    list.add(xColor);
                manaCostStr = StringUtils.join(list, ' ');
            }
        } else {
            manaCostStr = Integer.toString(amountX);
        }
        manaCost = new ManaCostBeingPaid(manaCostStr);
        
        this.xCanBe0 = xCanBe0;
        colorsPaid = saPaidFor.getSourceCard().getColorsPaid(); // for effects like sunburst
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
    public void showMessage() {
        if( isFinished() ) return;
        
        updateMessage();
    }

    @Override
    protected void updateMessage() { 
        StringBuilder msg = new StringBuilder("Pay X Mana Cost for ");
        msg.append(saPaidFor.getSourceCard().getName()).append("\n").append(this.xPaid);
        msg.append(" Paid so far.");
        if (!xCanBe0) {
            msg.append(" X Can't be 0.");
        }
        // Enable just cancel is full X value hasn't been paid for multiple X values
        // or X is 0, and x can't be 0
        if (!isPaid()) {
            ButtonUtil.enableOnlyCancel();
        } else {
            ButtonUtil.enableAllFocusOk();
        }

        showMessage(msg.toString());
    }

    @Override
    protected void onCardSelected(Card card, boolean isRmb) {
        // don't allow here the cards that produce only wrong colors
        activateManaAbility(card, this.manaCost);
    }
    

    @Override
    protected void onManaAbilityPaid() {
        if (this.manaCost.isPaid()) {
            this.colorsPaid |= manaCost.getColorsPaid();
            this.manaCost = new ManaCostBeingPaid(manaCostStr);
            this.xPaid++;
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
    public void selectManaPool(byte colorCode) {
        useManaFromPool(colorCode, this.manaCost);
    }


    @Override
    protected void done() {
        final Card card = saPaidFor.getSourceCard();
        card.setXManaCostPaid(this.xPaid);
        card.setColorsPaid(this.colorsPaid);
        card.setSunburstValue(ColorSet.fromMask(this.colorsPaid).countColors());
    }
}
