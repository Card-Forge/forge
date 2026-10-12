package forge.gamemodes.match.input;

import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityStackInstance;
import forge.player.PlayerControllerHuman;

import java.util.ArrayList;
import java.util.List;

/**
 * Picks items on the stack (spells, triggered and activated abilities) as targets, by tapping them in the stack view.
 * Items are told apart by stack instance, not by their source card, so two abilities of one permanent stay distinct.
 */
public class InputSelectStackItems extends InputSyncronizedBase {
    private static final long serialVersionUID = 4617306523381946321L;

    private final List<SpellAbilityStackInstance> choices;
    private final List<SpellAbilityStackInstance> selected = new ArrayList<>();
    private final int min;
    private final int max;
    private final SpellAbility sa;
    private boolean allowCancel;
    private boolean cancelled;
    private String message = "Select a target on the stack (%d more)";

    public InputSelectStackItems(final PlayerControllerHuman controller, final int min, final int max,
                                 final List<SpellAbilityStackInstance> choices, final SpellAbility sa) {
        super(controller);
        this.choices = choices;
        this.min = Math.min(min, choices.size());
        this.max = Math.min(max, choices.size());
        this.sa = sa;
        final List<forge.game.spellability.StackItemView> views = new ArrayList<>();
        for (final SpellAbilityStackInstance si : choices) {
            views.add(si.getView());
        }
        getController().getGui().setSelectableStackItems(views);
        // also what earlier steps of this spell (other modes) already chose
        TargetingProgress.showChosen(getController().getGui(), sa);
    }

    public void setMessage(final String message0) { message = message0; }
    public void setCancelAllowed(final boolean allow) { allowCancel = allow; }
    public boolean hasCancelled() { return cancelled; }
    public List<SpellAbilityStackInstance> getSelected() { return selected; }

    @Override
    protected void showMessage() {
        final String text = message.contains("%d") ? String.format(message, max - selected.size()) : message;
        showMessage(text, sa == null ? (forge.game.card.CardView) null : sa.getView().getHostCard());
        getController().getGui().updateButtons(getOwner(), selected.size() >= min, allowCancel, true);
    }

    @Override
    public boolean selectStackItem(final SpellAbilityStackInstance si) {
        if (isFinished() || !choices.contains(si)) {
            return false;
        }
        final boolean wasSelected = selected.remove(si);
        if (!wasSelected) {
            selected.add(si);
        }
        getController().getGui().setStackItemPicked(si.getView(), !wasSelected);
        if (selected.size() >= max) {
            selectButtonOK();
        } else {
            showMessage();
        }
        return true;
    }

    @Override
    protected void onOk() {
        stop();
    }

    @Override
    protected void onCancel() {
        cancelled = true;
        selected.clear();
        stop();
    }

    @Override
    protected void onStop() {
        getController().getGui().clearSelectableStackItems();
        if (!cancelled && sa != null && TargetingProgress.hasLaterTargetingStep(sa)) {
            // more steps follow: keep this step's picks (not yet stored on the ability) marked as chosen
            TargetingProgress.showChosen(getController().getGui(), sa);
            for (final SpellAbilityStackInstance si : selected) {
                getController().getGui().setStackItemPicked(si.getView(), true);
            }
        }
    }

    @Override
    public String getActivateAction(final forge.game.card.Card card) {
        return null;
    }
}
