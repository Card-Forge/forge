package forge.control.input;

import forge.Card;
import forge.Singletons;
import forge.card.cost.CostMana;
import forge.card.cost.CostPayment;
import forge.card.mana.ManaCost;
import forge.card.spellability.SpellAbility;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.gui.match.CMatchUI;
import forge.view.ButtonUtil;

public class InputPayManaCost2 extends InputPayMana {
    
    private ManaCost manaCost;
    private final CostMana costMana;
    // I would kill the one who made 2 classes like above
    private final String originalManaCost;
    private final SpellAbility sa; 
    private final int manaToAdd;
    private final CostPayment payment;
    
    public InputPayManaCost2(CostMana costMana, SpellAbility spellAbility, final CostPayment payment, int toAdd) {
        manaCost = new ManaCost(costMana.getManaToPay());
        manaCost.increaseColorlessMana(toAdd);

        this.costMana = costMana;
        originalManaCost = costMana.getMana();
        sa = spellAbility;
        manaToAdd = toAdd;
        this.payment = payment;
               
    }

    private static final long serialVersionUID = 3467312982164195091L;

    

    private int phyLifeToLose = 0;

    private void resetManaCost() {
        this.manaCost = new ManaCost(this.originalManaCost);
        this.phyLifeToLose = 0;
    }

    @Override
    public void selectCard(final Card card) {
        // prevent cards from tapping themselves if ability is a
        // tapability, although it should already be tapped
        this.manaCost = InputPayManaCostUtil.activateManaAbility(sa, card, this.manaCost);

        if (this.manaCost.isPaid()) {
            this.done();
        } else if (Singletons.getModel().getMatch().getInput().getInput() == this) {
            this.showMessage();
        }
    }

    @Override
    public void selectPlayer(final Player player) {
        if (player.isHuman()) {
            if (player.canPayLife(this.phyLifeToLose + 2) && manaCost.payPhyrexian()) {
                this.phyLifeToLose += 2;
            }

            this.showMessage();
        }
    }

    private void done() {
        final Card source = sa.getSourceCard();
        if (this.phyLifeToLose > 0) {
            Singletons.getControl().getPlayer().payLife(this.phyLifeToLose, source);
        }
        source.setColorsPaid(this.manaCost.getColorsPaid());
        source.setSunburstValue(this.manaCost.getSunburst());
        this.resetManaCost();
        this.stop();

        if (costMana.hasNoXManaCost() || (manaToAdd > 0)) {
            payment.paidCost(costMana);
        } else {
            source.setXManaCostPaid(0);
            final Input inp = new InputPayManaX(sa, payment, costMana);
            Singletons.getModel().getMatch().getInput().setInputInterrupt(inp);
        }

        // If this is a spell with convoke, re-tap all creatures used
        // for it.
        // This is done to make sure Taps triggers go off at the right
        // time
        // (i.e. AFTER cost payment, they are tapped previously as well
        // so that
        // any mana tapabilities can't be used in payment as well as
        // being tapped for convoke)

        if (sa.getTappedForConvoke() != null) {
            for (final Card c : sa.getTappedForConvoke()) {
                c.setTapped(false);
                c.tap();
            }
            sa.clearTappedForConvoke();
        }

    }

    @Override
    public void selectButtonCancel() {
        // If we're paying for a spell with convoke, untap all creatures
        // used for it.
        if (sa.getTappedForConvoke() != null) {
            for (final Card c : sa.getTappedForConvoke()) {
                c.setTapped(false);
            }
            sa.clearTappedForConvoke();
        }

        this.stop();
        this.resetManaCost();
        payment.cancelCost();
        Singletons.getControl().getPlayer().getZone(ZoneType.Battlefield).updateObservers();
    }

    @Override
    public void showMessage() {
        ButtonUtil.enableOnlyCancel();
        final String displayMana = manaCost.toString().replace("X", "").trim();
        CMatchUI.SINGLETON_INSTANCE.showMessage("Pay Mana Cost: " + displayMana);

        final StringBuilder msg = new StringBuilder("Pay Mana Cost: " + displayMana);
        if (this.phyLifeToLose > 0) {
            msg.append(" (");
            msg.append(this.phyLifeToLose);
            msg.append(" life paid for phyrexian mana)");
        }

        if (manaCost.containsPhyrexianMana()) {
            msg.append("\n(Click on your life total to pay life for phyrexian mana.)");
        }

        CMatchUI.SINGLETON_INSTANCE.showMessage(msg.toString());
        if (manaCost.isPaid()) {
            this.done();
        }
    }

    @Override
    public void selectManaPool(String color) {
        manaCost = InputPayManaCostUtil.activateManaAbility(color, sa, this.manaCost);

        if (this.manaCost.isPaid()) {
            this.done();
        } else if (Singletons.getModel().getMatch().getInput().getInput() == this) {
            this.showMessage();
        }
    }
    
    @Override public void isClassUpdated() {}
}