package forge.gamemodes.match.input;

import java.util.ArrayList;
import java.util.List;

import forge.card.mana.ManaAtom;
import forge.card.mana.ManaCostShard;
import forge.game.mana.Mana;
import forge.game.mana.ManaConversionMatrix;
import forge.game.mana.ManaCostBeingPaid;
import forge.game.mana.ManaPool;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.localinstance.properties.ForgePreferences;
import forge.model.FModel;
import forge.player.PlayerControllerHuman;
import forge.util.ITriggerEvent;
import forge.util.Localizer;

public class InputPayManaOfCostPayment extends InputPayMana {

    public InputPayManaOfCostPayment(final PlayerControllerHuman controller, ManaCostBeingPaid cost, SpellAbility spellAbility, Player payer, ManaConversionMatrix matrix, boolean effect) {
        super(controller, spellAbility, payer, effect);
        manaCost = cost;
        extraMatrix = matrix;
        applyMatrix();

        // CR 118.3c forced cast must use pool mana
        // TODO this introduces a small risk for illegal payments if the human "wastes" enough mana for abilities like Doubling Cube
        if (spellAbility.getPayCosts().isMandatory()) {
            List<Mana> refund = new ArrayList<>();
            mandatory = ManaPool.payManaCostFromPool(new ManaCostBeingPaid(cost), spellAbility, payer, true, refund);
            ManaPool.refundMana(refund, payer, spellAbility);
        }

        // Set Mana cost being paid for SA to be able to reference it later
        player.pushPaidForSA(saPaidFor);
        saPaidFor.setManaCostBeingPaid(manaCost);
    }

    private static final long serialVersionUID = 3467312982164195091L;
    private ManaConversionMatrix extraMatrix;

    @Override
    protected final void onPlayerSelected(Player selected, final ITriggerEvent triggerEvent) {
        if (player == selected) {
            if (player.canPayLife(this.phyLifeToLose + 2, this.effect, saPaidFor)) {
                if (manaCost.payPhyrexian()) {
                    saPaidFor.setSpendPhyrexianMana(true);
                    this.phyLifeToLose += 2;
                } else {
                    if (player.hasKeyword("PayLifeInsteadOf:B") && manaCost.hasAnyKind(ManaAtom.BLACK)) {
                        manaCost.decreaseShard(ManaCostShard.BLACK, 1);
                        this.phyLifeToLose += 2;
                    }
                }
            }

            this.showMessage();
        }
    }

    @Override
    protected void done() {
        if (this.phyLifeToLose > 0) {
            player.payLife(this.phyLifeToLose, saPaidFor, this.effect);
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
        final Localizer localizer = Localizer.getInstance();

        applyMatrix();

        if (messagePrefix != null) {
            msg.append(messagePrefix).append("\n");
        }
        if (FModel.getPreferences().getPrefBoolean(ForgePreferences.FPref.UI_DETAILED_SPELLDESC_IN_PROMPT)) {
	    if (saPaidFor.isSpell()) {
                msg.append(saPaidFor.getStackDescription().replace("(Targeting ERROR)", "")).append("\n\n");
            } else {
                msg.append(saPaidFor.getHostCard()).append(" - ").append(saPaidFor.toString()).append("\n\n");
            }
        }
        msg.append(localizer.getMessage("lblPayManaCost")).append(" ").append(displayMana);
        if (this.phyLifeToLose > 0) {
            msg.append(" ").append(String.format(localizer.getMessage("lblLifePaidForPhyrexianMana"), this.phyLifeToLose));
        }

        boolean isLifeInsteadBlack = player.hasKeyword("PayLifeInsteadOf:B") && manaCost.hasAnyKind(ManaAtom.BLACK);

        if (manaCost.containsPhyrexianMana() || isLifeInsteadBlack) {
            StringBuilder sb = new StringBuilder();
            if (manaCost.containsPhyrexianMana() && !isLifeInsteadBlack) {
                sb.append(localizer.getMessage("lblClickOnYourLifeTotalToPayLifeForPhyrexianMana"));
            } else if (!manaCost.containsPhyrexianMana() && isLifeInsteadBlack) {
                sb.append(localizer.getMessage("lblClickOnYourLifeTotalToPayLifeForBlackMana"));
            } else if (manaCost.containsPhyrexianMana() && isLifeInsteadBlack) {
                sb.append(localizer.getMessage("lblClickOnYourLifeTotalToPayLifeForPhyrexianOrBlackMana"));
            }
            msg.append("\n(").append(sb).append(")");
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
