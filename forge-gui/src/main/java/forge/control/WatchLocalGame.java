/**
 * 
 */
package forge.control;

import forge.LobbyPlayer;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CardView;
import forge.game.player.Player;
import forge.game.player.PlayerView;
import forge.game.spellability.SpellAbility;
import forge.match.input.Input;
import forge.match.input.InputPlaybackControl;
import forge.player.PlayerControllerHuman;
import forge.util.ITriggerEvent;


public class WatchLocalGame extends PlayerControllerHuman {
    public WatchLocalGame(Game game0, Player p, LobbyPlayer lp) {
        super(game0, p, lp);
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
    public void useMana(final byte mana) {
    }

    @Override
    public void selectPlayer(final PlayerView player, final ITriggerEvent triggerEvent) {
    }

    @Override
    public boolean selectCard(final CardView card, final ITriggerEvent triggerEvent) {
        return false;
    }

    @Override
    public void selectAbility(final SpellAbility sa) {
    }

    @Override
    public void alphaStrike() {
    }

    @Override
    public boolean mayShowCard(final Card c) {
        return true;
    }

    @Override
    public boolean mayShowCardFace(final Card c) {
        return true;
    }

    @Override
    public Iterable<String> getAutoYields() {
        return null;
    }
    @Override
    public boolean shouldAutoYield(final String key) {
        return false;
    }
    @Override
    public void setShouldAutoYield(final String key, final boolean autoYield) {
    }
    @Override
    public boolean shouldAlwaysAcceptTrigger(final Integer trigger) {
        return false;
    }
    @Override
    public boolean shouldAlwaysDeclineTrigger(final Integer trigger) {
        return false;
    }
    @Override
    public boolean shouldAlwaysAskTrigger(final Integer trigger) {
        return false;
    }
    @Override
    public void setShouldAlwaysAcceptTrigger(final Integer trigger) {
    }
    @Override
    public void setShouldAlwaysDeclineTrigger(final Integer trigger) {
    }
    @Override
    public void setShouldAlwaysAskTrigger(final Integer trigger) {
    }
    @Override
    public void autoPassCancel() {
    }

    @Override
    public boolean canPlayUnlimitedLands() {
        return false;
    }

    @Override
    public DevModeCheats cheat() {
        return null;
    }
}
