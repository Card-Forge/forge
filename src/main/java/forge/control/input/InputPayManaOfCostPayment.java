package forge.control.input;

import forge.Card;
import forge.Singletons;
import forge.card.cost.CostPartMana;
import forge.card.mana.ManaCostBeingPaid;
import forge.card.spellability.SpellAbility;
import forge.game.GameState;
import forge.game.player.Player;
import forge.view.ButtonUtil;

public class InputPayManaOfCostPayment extends InputPayManaBase {

    public InputPayManaOfCostPayment(final GameState game, CostPartMana costMana, SpellAbility spellAbility, int toAdd) {
        super(spellAbility);
        manaCost = new ManaCostBeingPaid(costMana.getManaToPay());
        manaCost.increaseColorlessMana(toAdd);
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
            Singletons.getControl().getPlayer().payLife(this.phyLifeToLose, source);
        }
        source.setColorsPaid(this.manaCost.getColorsPaid());
        source.setSunburstValue(this.manaCost.getSunburst());

        // If this is a spell with convoke, re-tap all creatures used  for it.
        // This is done to make sure Taps triggers go off at the right time
        // (i.e. AFTER cost payment, they are tapped previously as well so that
        // any mana tapabilities can't be used in payment as well as being tapped for convoke)

        handleConvokedCards(false);
        stop();
    }
    
    @Override
    public void selectButtonCancel() {
        handleConvokedCards(true);
        stop();
    }

    @Override
    public void showMessage() {
        ButtonUtil.enableOnlyCancel();
        final String displayMana = manaCost.toString().replace("X", "").trim();

        final StringBuilder msg = new StringBuilder("Pay Mana Cost: " + displayMana);
        if (this.phyLifeToLose > 0) {
            msg.append(" (");
            msg.append(this.phyLifeToLose);
            msg.append(" life paid for phyrexian mana)");
        }

        if (manaCost.containsPhyrexianMana()) {
            msg.append("\n(Click on your life total to pay life for phyrexian mana.)");
        }

        showMessage(msg.toString());
        checkIfAlredyPaid();
    }
}
