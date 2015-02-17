package forge.match.input;

import java.util.Collection;

import com.google.common.collect.Iterables;

import forge.game.GameEntity;
import forge.game.card.Card;
import forge.game.card.CardView;
import forge.player.PlayerControllerHuman;

public abstract class InputSelectManyBase<T extends GameEntity> extends InputSyncronizedBase {
    private static final long serialVersionUID = -2305549394512889450L;

    protected boolean bCancelled = false;
    protected final int min;
    protected final int max;
    protected boolean allowCancel = false;

    protected String message = "Source-Card-Name - Select %d more card(s)";

    protected InputSelectManyBase(final PlayerControllerHuman controller, final int min, final int max) {
        super(controller);
        if (min > max) {
            throw new IllegalArgumentException("Min must not be greater than Max");
        }
        this.min = min;
        this.max = max;
    }

    protected void refresh() {
        if (hasAllTargets()) {
            selectButtonOK();
        }
        else {
            this.showMessage();
        }
    }

    protected abstract boolean hasEnoughTargets();
    protected abstract boolean hasAllTargets();

    protected abstract String getMessage();

    @Override
    public final void showMessage() {
        showMessage(getMessage());
        getController().getGui().updateButtons(getOwner(), hasEnoughTargets(), allowCancel, true);
    }

    @Override
    protected final void onCancel() {
        bCancelled = true;
        resetUsedToPay();
        this.getSelected().clear();
        this.stop();
    }

    public final boolean hasCancelled() {
        return bCancelled;
    }

    public abstract Collection<T> getSelected();
    public T getFirstSelected() { return Iterables.getFirst(getSelected(), null); }

    @Override
    protected final void onOk() {
        resetUsedToPay();
        this.stop();
    }

    public void setMessage(String message0) {
        this.message = message0;
    }

    protected void onSelectStateChanged(final GameEntity c, final boolean newState) {
        if (c instanceof Card) {
            getController().getGui().setUsedToPay(CardView.get((Card) c), newState); // UI supports card highlighting though this abstraction-breaking mechanism
        }
    }

    private void resetUsedToPay() {
        for (final GameEntity c : getSelected()) {
            if (c instanceof Card) {
                getController().getGui().setUsedToPay(CardView.get((Card) c), false);
            }
        }
    }

    public final void setCancelAllowed(boolean allow) {
        this.allowCancel = allow ;
    }
}
