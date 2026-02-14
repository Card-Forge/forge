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
package forge.gamemodes.match.input;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CardView;
import forge.game.phase.PhaseHandler;
import forge.game.player.Player;
import forge.game.player.PlayerView;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityView;
import forge.gui.GuiBase;
import forge.localinstance.properties.ForgePreferences;
import forge.model.FModel;
import forge.player.PlayerControllerHuman;
import forge.util.ITriggerEvent;
import forge.util.Localizer;

import java.util.List;

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
    protected final void showMessage(final String message, final SpellAbilityView sav) {
        if (GuiBase.isNetworkplay(controller.getGui())) //todo additional check to pass this
            controller.getGui().showPromptMessage(getOwner(), message);
        else
            controller.getGui().showCardPromptMessage(getOwner(), message, sav.getHostCard());
    }
    protected final void showMessage(final String message, final CardView card) {
        if (GuiBase.isNetworkplay(controller.getGui())) //todo additional check to pass this
            controller.getGui().showPromptMessage(getOwner(), message);
        else
            controller.getGui().showCardPromptMessage(getOwner(), message, card);
    }

    protected String getTurnPhasePriorityMessage(final Game game) {
        final PhaseHandler ph = game.getPhaseHandler();
        final StringBuilder sb = new StringBuilder();
        Localizer localizer = Localizer.getInstance();
        sb.append(localizer.getMessage("lblPriority")).append(": ").append(ph.getPriorityPlayer()).append("\n");
        sb.append(localizer.getMessage("lblTurn")).append(": ").append(ph.getTurn()).append(" (").append(ph.getPlayerTurn()).append(")");

        if (!game.isNeitherDayNorNight()) {
            sb.append("  [");

            String dayLabel = game.isDay() ? "Day" : "Night";

            sb.append(Localizer.getInstance().getMessage("lbl" + dayLabel));
            sb.append("]");
        }

        sb.append("\n");
        sb.append(localizer.getMessage("lblPhase")).append(": ").append(ph.getPhase().nameForUi).append("\n");
        sb.append(localizer.getMessage("lblStack")).append(": ");
        if (!game.getStack().isEmpty()) {
            sb.append(game.getStack().size()).append(" ").append(localizer.getMessage("lbltoResolve"));
        } else {
            sb.append(localizer.getMessage("lblEmpty"));
        }
        if (FModel.getPreferences().getPrefBoolean(ForgePreferences.FPref.UI_SHOW_STORM_COUNT_IN_PROMPT)) {
            int stormCount = game.getView().getStormCount();
            if (stormCount > 0) {
                sb.append("\n").append(localizer.getMessage("lblStormCount")).append(": ").append(stormCount);
            }
        }

        if (controller.macros() != null) {
            boolean isRecording = controller.macros().isRecording();
            String pbText = controller.macros().playbackText();
            if (pbText != null) {
                sb.append("\n");
                if (isRecording) {
                    sb.append("Macro Recording -- ");
                } else {
                    sb.append("Macro Playback -- ");
                }

                sb.append(pbText);
            } else if (isRecording) {
                sb.append("\n").append("Macro Recording -- ");
            }
        }

        return sb.toString();
    }
}
