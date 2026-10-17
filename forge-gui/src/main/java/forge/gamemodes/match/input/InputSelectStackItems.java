package forge.gamemodes.match.input;

import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityStackInstance;
import forge.gamemodes.match.Selection;
import forge.player.PlayerControllerHuman;
import forge.trackable.TrackableObject;

import java.util.ArrayList;
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
    }

    public void setMessage(final String message0) { message = message0; }
    public void setCancelAllowed(final boolean allow) { allowCancel = allow; }
    public boolean hasCancelled() { return cancelled; }
    public List<SpellAbilityStackInstance> getSelected() { return selected; }

    @Override
    protected Selection getSelection() {
        final List<TrackableObject> offered = new ArrayList<>();
        for (final SpellAbilityStackInstance si : choices) {
            offered.add(si.getView());
        }
        final List<TrackableObject> chosen = sa == null ? new ArrayList<>() : TargetingProgress.chosenTargets(sa);
        for (final SpellAbilityStackInstance si : selected) {
            chosen.add(si.getView());
        }
        return new Selection(List.of(), 0, 0, offered, chosen);
    }

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
        showSelection();
        if (selected.size() >= max) {
            selectButtonOK();
        } else {
            showMessage();
        }
        return true;
    }

    @Override
    protected void onOk() {
        // The button is disabled until enough is chosen, but a remote client can still send the press
        if (selected.size() < min) {
            return;
        }
        stop();
    }

    @Override
    protected void onCancel() {
        cancelled = true;
        selected.clear();
        stop();
    }

    @Override
    public String getActivateAction(final forge.game.card.Card card) {
        return null;
    }
}
