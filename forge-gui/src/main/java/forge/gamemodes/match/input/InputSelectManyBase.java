package forge.gamemodes.match.input;

import java.util.Collection;

import com.google.common.collect.Iterables;

import forge.game.GameEntity;
import forge.game.card.Card;
import forge.game.card.CardView;
import forge.game.spellability.SpellAbility;
import forge.localinstance.properties.ForgePreferences;
import forge.model.FModel;
import forge.player.PlayerControllerHuman;

public abstract class InputSelectManyBase<T extends GameEntity> extends InputSyncronizedBase {
    private static final long serialVersionUID = -2305549394512889450L;

    protected boolean bCancelled = false;
    protected final int min;
    protected final int max;
    protected boolean allowCancel = false;
    protected SpellAbility sa = null;
    protected CardView card;

    protected String message = "Source-Card-Name - Select %d more card(s)";

    protected InputSelectManyBase(final PlayerControllerHuman controller, final int min, final int max) {
        super(controller);
        if (min > max) {
            throw new IllegalArgumentException("Min must not be greater than Max");
        }
        this.min = min;
        this.max = max;
    }
    
    protected InputSelectManyBase(final PlayerControllerHuman controller, final int min, final int max, final SpellAbility sa0) {
    	this(controller,min,max);
    	this.sa = sa0;
        if (sa0 != null) {
            this.card = sa0.getView().getHostCard();
        }
    }

    protected InputSelectManyBase(final PlayerControllerHuman controller, final int min, final int max, final CardView card0) {
    	this(controller,min,max);
    	this.sa = null;
    	this.card = card0;
    }

    protected void refresh() {
        if (hasAllTargets()) {
            selectButtonOK();
        } else {
            this.showMessage();
        }
    }

    protected abstract boolean hasEnoughTargets();
    protected abstract boolean hasAllTargets();

    protected abstract String getMessage();

    @Override
    public final void showMessage() {
        if ( FModel.getPreferences().getPrefBoolean(ForgePreferences.FPref.UI_DETAILED_SPELLDESC_IN_PROMPT) &&
             (card!=null) ) {
            final StringBuilder sb = new StringBuilder();
            sb.append(card.toString());
            if ( (sa != null) && (!sa.toString().isEmpty()) ) { // some spell abilities have no useful string value
                sb.append(" - ").append(sa.toString());
            }
            sb.append("\n\n").append(getMessage());
            showMessage(sb.toString(), card);
        } else {
            if (card != null) { 
                showMessage(getMessage(), card);
            } else {
                showMessage(getMessage());
            }
        }
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
        this.allowCancel = allow;
    }
}
