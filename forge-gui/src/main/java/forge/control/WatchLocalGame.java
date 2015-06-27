/**
 *
 */
package forge.control;

import java.util.List;

import forge.LobbyPlayer;
import forge.game.Game;
import forge.game.card.CardView;
import forge.game.player.PlayerView;
import forge.game.spellability.SpellAbilityView;
import forge.interfaces.IDevModeCheats;
import forge.interfaces.IGuiGame;
import forge.match.input.Input;
import forge.match.input.InputPlaybackControl;
import forge.player.PlayerControllerHuman;
import forge.util.ITriggerEvent;

public class WatchLocalGame extends PlayerControllerHuman {
    public WatchLocalGame(final Game game0, final LobbyPlayer lp, final IGuiGame gui) {
        super(game0, null, lp);
        setGui(gui);
    }

    @Override
    public void updateAchievements() {
    }

    @Override
    public boolean canUndoLastAction() {
        return false;
    }

    @Override
    public void undoLastAction() {
    }

    @Override
    public void selectButtonOk() {
        if (inputQueue == null) {
            return;
        }
        final Input i = inputQueue.getInput();
        if (i instanceof InputPlaybackControl) {
            i.selectButtonOK();
        }
    }

    @Override
    public void selectButtonCancel() {
        if (inputQueue == null) {
            return;
        }
        final Input i = inputQueue.getInput();
        if (i instanceof InputPlaybackControl) {
            i.selectButtonCancel();
        }
    }

    @Override
    public void confirm() {
    }

    @Override
    public void passPriority() {
    }

    @Override
    public void passPriorityUntilEndOfTurn() {
    }

    @Override
    public void useMana(final byte mana) {
    }

    @Override
    public void selectPlayer(final PlayerView player,
            final ITriggerEvent triggerEvent) {
    }

    @Override
    public boolean selectCard(final CardView card,
            final List<CardView> otherCardViewsToSelect,
            final ITriggerEvent triggerEvent) {
        return false;
    }

    @Override
    public void selectAbility(final SpellAbilityView sa) {
    }

    @Override
    public void alphaStrike() {
    }

    @Override
    public boolean canPlayUnlimitedLands() {
        return false;
    }

    @Override
    public IDevModeCheats cheat() {
        return IDevModeCheats.NO_CHEAT;
    }

    @Override
    public void awaitNextInput() {
        // Do nothing
    }
    @Override
    public void cancelAwaitNextInput() {
        // Do nothing
    }
}
