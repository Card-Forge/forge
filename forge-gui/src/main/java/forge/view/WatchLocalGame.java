/**
 * 
 */
package forge.view;

import forge.game.Game;
import forge.match.input.Input;
import forge.match.input.InputPlaybackControl;
import forge.match.input.InputQueue;
import forge.util.ITriggerEvent;

/**
 * @author lennaert
 *
 */
public class WatchLocalGame extends LocalGameView {

    private final InputQueue inputQueue;
    /**
     * @param game
     * @param inputQueue 
     */
    public WatchLocalGame(final Game game, final InputQueue inputQueue) {
        super(game);
        this.inputQueue = inputQueue;
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
        final Input i = inputQueue.getInput();
        if (i instanceof InputPlaybackControl) {
            i.selectButtonOK();
        }
    }

    @Override
    public void selectButtonCancel() {
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
    public void selectAbility(final SpellAbilityView sa) {
    }

    @Override
    public void alphaStrike() {
    }

    @Override
    public boolean mayShowCard(final CardView c) {
        return true;
    }

    @Override
    public boolean mayShowCardFace(final CardView c) {
        return true;
    }

    // Dev mode functions
    @Override
    public void devTogglePlayManyLands(final boolean b) {
    }
    @Override
    public void devGenerateMana() {
    }
    @Override
    public void devSetupGameState() {
    }
    @Override
    public void devTutorForCard() {
    }
    @Override
    public void devAddCardToHand() {
    }
    @Override
    public void devAddCounterToPermanent() {
    }
    @Override
    public void devTapPermanent() {
    }
    @Override
    public void devUntapPermanent() {
    }
    @Override
    public void devSetPlayerLife() {
    }
    @Override
    public void devWinGame() {
    }
    @Override
    public void devAddCardToBattlefield() {
    }
    @Override
    public void devRiggedPlanerRoll() {
    }
    @Override
    public void devPlaneswalkTo() {
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

}
