/**
 * 
 */
package forge.control;

import java.util.List;

import forge.LobbyPlayer;
import forge.game.Game;
import forge.game.card.CardView;
import forge.game.player.PlayerView;
import forge.game.spellability.SpellAbility;
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

    public boolean canUndoLastAction() {
        return false;
    }

    @Override
    public boolean tryUndoLastAction() {
        return false;
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
    public boolean passPriority() {
        return false;
    }

    @Override
    public boolean passPriorityUntilEndOfTurn() {
        return false;
    }

    @Override
    public boolean useMana(final byte mana) {
        return false;
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
    public void selectAbility(final SpellAbility sa) {
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
        return DevModeCheats.NO_CHEAT;
    }
}
