package forge.match.input;

import forge.game.card.Card;
import forge.game.mana.ManaConversionMatrix;
import forge.game.mana.ManaCostBeingPaid;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.model.FModel;
import forge.player.PlayerControllerHuman;
import forge.properties.ForgePreferences;
import forge.util.ITriggerEvent;

public class InputPayManaOfCostPayment extends InputPayMana {
    public InputPayManaOfCostPayment(final PlayerControllerHuman controller, ManaCostBeingPaid cost, SpellAbility spellAbility, Player payer, ManaConversionMatrix matrix) {
        super(controller, spellAbility, payer);
        manaCost = cost;
        extraMatrix = matrix;
        applyMatrix();
    }

    private static final long serialVersionUID = 3467312982164195091L;
    private int phyLifeToLose = 0;
    private ManaConversionMatrix extraMatrix;

    @Override
    protected final void onPlayerSelected(Player selected, final ITriggerEvent triggerEvent) {
        if (player == selected) {
            if (player.canPayLife(this.phyLifeToLose + 2) && manaCost.payPhyrexian()) {
                this.phyLifeToLose += 2;
            }

            this.showMessage();
        }
    }

    @Override
    protected void done() {
        final Card source = saPaidFor.getHostCard();
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
        final String displayMana = manaCost.toString(false, player.getManaPool());
        final StringBuilder msg = new StringBuilder();

        applyMatrix();

        if (messagePrefix != null) {
            msg.append(messagePrefix).append("\n");
        }
        if (FModel.getPreferences().getPrefBoolean(ForgePreferences.FPref.UI_DETAILED_SPELLDESC_IN_PROMPT)) {
            // msg.append(saPaidFor.getStackDescription().replace("(Targeting ERROR)", ""));
	    if (saPaidFor.isSpell()) {
                msg.append(saPaidFor.getStackDescription().replace("(Targeting ERROR)", "")).append("\n\n");
            } else {
                msg.append(saPaidFor.getHostCard()).append(" - ").append(saPaidFor.toString()).append("\n\n");
            }
        }
        msg.append("Pay Mana Cost: ").append(displayMana);
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

    private void applyMatrix() {
        if (extraMatrix == null) {
            return;
        }

        player.getManaPool().applyCardMatrix(extraMatrix);
    }
}
