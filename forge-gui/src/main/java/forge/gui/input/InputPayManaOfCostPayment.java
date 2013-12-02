package forge.gui.input;

import forge.game.card.Card;
import forge.game.mana.ManaCostBeingPaid;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

public class InputPayManaOfCostPayment extends InputPayMana {
    public InputPayManaOfCostPayment(ManaCostBeingPaid cost, SpellAbility spellAbility) {
        super(spellAbility);
        manaCost = cost;
    }

    private static final long serialVersionUID = 3467312982164195091L;
    private int phyLifeToLose = 0;

    @Override
    public void selectPlayer(final Player selectedPlayer) {
        if (player == selectedPlayer) {
            if (player.canPayLife(this.phyLifeToLose + 2) && manaCost.payPhyrexian()) {
                this.phyLifeToLose += 2;
            }

            this.showMessage();
        }
    }

    @Override
    protected void done() {
        final Card source = saPaidFor.getSourceCard();
        if (this.phyLifeToLose > 0) {
            player.payLife(this.phyLifeToLose, source);
        }
    }

    @Override
    protected void onCancel() {
        stop();
    }

    @Override
    protected String getMessage() { 
        final String displayMana = manaCost.toString(false);

        final StringBuilder msg = new StringBuilder("Pay Mana Cost: " + displayMana);
        if (this.phyLifeToLose > 0) {
            msg.append(" (");
            msg.append(this.phyLifeToLose);
            msg.append(" life paid for phyrexian mana)");
        }

        if (manaCost.containsPhyrexianMana()) {
            msg.append("\n(Click on your life total to pay life for phyrexian mana.)");
        }
        
        return msg.toString();
    }
}
