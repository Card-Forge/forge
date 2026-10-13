package forge.gamemodes.match.input;

import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityStackInstance;
import forge.game.spellability.StackItemView;
import forge.player.PlayerControllerHuman;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Picks items on the stack (spells, triggered and activated abilities) as targets, by tapping them in the stack view.
 * Items are told apart by stack instance, not by their source card, so two abilities of one permanent stay distinct.
 * A tap in the stack view reaches this input as the entry's ability, through the existing {@link Input#selectAbility}.
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
        final List<StackItemView> views = new ArrayList<>();
        for (final SpellAbilityStackInstance si : choices) {
            views.add(si.getView());
        }
        getController().getGui().setSelectableTargets(views);
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
    public boolean selectAbility(final SpellAbility ab) {
        if (isFinished()) {
            return false;
        }
        for (final SpellAbilityStackInstance si : choices) {
            if (si.getSpellAbility().equals(ab)) {
                return toggle(si);
            }
        }
        return false;
    }

    private boolean toggle(final SpellAbilityStackInstance si) {
        final boolean wasSelected = selected.remove(si);
        if (!wasSelected) {
            selected.add(si);
        }
        getController().getGui().setHighlightedStackItems(Collections.singletonList(si.getView()), !wasSelected);
        if (selected.size() >= max) {
            selectButtonOK();
        } else {
            showMessage();
        }
        return true;
    }

    private void unmarkSelected() {
        final List<StackItemView> views = new ArrayList<>();
        for (final SpellAbilityStackInstance si : selected) {
            views.add(si.getView());
        }
        getController().getGui().setHighlightedStackItems(views, false);
    }

    @Override
    protected void onOk() {
        stop();
    }

    @Override
    protected void onCancel() {
        cancelled = true;
        unmarkSelected();
        selected.clear();
        stop();
    }

    @Override
    protected void onStop() {
        getController().getGui().clearSelectables();
        // the picks stay marked while later steps follow or the spell's cost is still to be paid; otherwise they go with the prompt
        if (cancelled || sa == null
                || !(TargetingProgress.hasLaterTargetingStep(sa) || getController().isKeepingTargetsMarked())) {
            unmarkSelected();
        }
    }

    @Override
    public String getActivateAction(final forge.game.card.Card card) {
        return null;
    }
}
