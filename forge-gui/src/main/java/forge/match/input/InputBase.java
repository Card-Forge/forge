/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.match.input;

import java.util.List;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseHandler;
import forge.game.player.Player;
import forge.game.player.PlayerView;
import forge.game.spellability.SpellAbility;
import forge.player.PlayerControllerHuman;
import forge.util.ITriggerEvent;

/**
 * <p>
 * Abstract Input class.
 * </p>
 *
 * @author Forge
 * @version $Id: InputBase.java 24769 2014-02-09 13:56:04Z Hellfish $
 */
public abstract class InputBase implements java.io.Serializable, Input {
    /** Constant <code>serialVersionUID=-2531867688249685076L</code>. */
    private static final long serialVersionUID = -2531867688249685076L;

    private final PlayerControllerHuman controller;
    public InputBase(final PlayerControllerHuman controller0) {
        controller = controller0;
    }
    public final PlayerControllerHuman getController() {
        return controller;
    }
    @Override
    public PlayerView getOwner() {
        final Player owner = getController().getPlayer();
        return owner == null ? null : owner.getView();
    }

    private boolean finished = false;
    protected final boolean isFinished() { return finished; }
    protected final void setFinished() {
        finished = true;

        if (allowAwaitNextInput()) {
            controller.awaitNextInput();
        }
    }

    protected boolean allowAwaitNextInput() {
        return false;
    }

    // showMessage() is always the first method called
    @Override
    public final void showMessageInitial() {
        finished = false;
        controller.cancelAwaitNextInput();
        showMessage();
    }

    protected abstract void showMessage();

    @Override
    public final void selectPlayer(final Player player, final ITriggerEvent triggerEvent) {
        if (isFinished()) { return; }
        onPlayerSelected(player, triggerEvent);
    }

    @Override
    public boolean selectAbility(final SpellAbility ab) {
        return false;
    }

    @Override
    public final void selectButtonCancel() {
        if (isFinished()) { return; }
        onCancel();
    }

    @Override
    public final void selectButtonOK() {
        if (isFinished()) { return; }
        onOk();
    }

    @Override
    public final boolean selectCard(final Card c, final List<Card> otherCardsToSelect, final ITriggerEvent triggerEvent) {
        if (isFinished()) { return false; }
        return onCardSelected(c, otherCardsToSelect, triggerEvent);
    }

    protected boolean onCardSelected(final Card c, final List<Card> otherCardsToSelect, final ITriggerEvent triggerEvent) {
        return false;
    }
    protected void onPlayerSelected(final Player player, final ITriggerEvent triggerEvent) {}
    protected void onCancel() {}
    protected void onOk() {}

    // to remove need for CMatchUI dependence
    protected final void showMessage(final String message) {
        controller.getGui().showPromptMessage(getOwner(), message);
    }

    protected String getTurnPhasePriorityMessage(final Game game) {
        final PhaseHandler ph = game.getPhaseHandler();
        final StringBuilder sb = new StringBuilder();

        sb.append("Priority: ").append(ph.getPriorityPlayer()).append("\n");
        sb.append("Turn ").append(ph.getTurn()).append(" (").append(ph.getPlayerTurn()).append(")\n");
        sb.append("Phase: ").append(ph.getPhase().nameForUi).append("\n");
        sb.append("Stack: ");
        if (!game.getStack().isEmpty()) {
            sb.append(game.getStack().size()).append(" to Resolve.");
        } else {
            sb.append("Empty");
        }
        return sb.toString();
    }
}
