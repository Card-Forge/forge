package forge.gamemodes.match.input;

import forge.card.mana.ManaAtom;
import forge.card.mana.ManaCostShard;
import forge.game.card.Card;
import forge.game.mana.ManaConversionMatrix;
import forge.game.mana.ManaCostBeingPaid;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.localinstance.properties.ForgePreferences;
import forge.model.FModel;
import forge.player.PlayerControllerHuman;
import forge.util.ITriggerEvent;
import forge.util.Localizer;

public class InputPayManaOfCostPayment extends InputPayMana {
    public InputPayManaOfCostPayment(final PlayerControllerHuman controller, ManaCostBeingPaid cost, SpellAbility spellAbility, Player payer, ManaConversionMatrix matrix) {
        super(controller, spellAbility, payer);
        manaCost = cost;
        extraMatrix = matrix;
        applyMatrix();

        // Set Mana cost being paid for SA to be able to reference it later
        player.pushPaidForSA(saPaidFor);
        saPaidFor.setManaCostBeingPaid(manaCost);
    }

    private static final long serialVersionUID = 3467312982164195091L;
    //private int phyLifeToLose = 0;
    private ManaConversionMatrix extraMatrix;

    @Override
    protected final void onPlayerSelected(Player selected, final ITriggerEvent triggerEvent) {
        if (player == selected) {
            if (player.canPayLife(this.phyLifeToLose + 2)) {
                if (manaCost.payPhyrexian()) {
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
