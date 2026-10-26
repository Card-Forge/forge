package forge.gamemodes.tutorial;

import java.util.List;

import forge.game.card.CardView;
import forge.game.player.PlayerView;
import forge.game.spellability.SpellAbilityView;
import forge.game.spellability.StackItemView;
import forge.gamemodes.match.DrawOfferMessage;
import forge.gamemodes.match.NextGameDecision;
import forge.gamemodes.match.YieldController;
import forge.gamemodes.match.YieldUpdate;
import forge.interfaces.IDevModeCheats;
import forge.interfaces.IGameController;
import forge.interfaces.IMacroSystem;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.player.AutoYieldStore.TriggerDecision;
import forge.util.ITriggerEvent;

/**
 * Sits between the match GUI and the human player's controller while the "How to Play" demo runs.
 *
 * <p>In the demo the {@link TutorialDirector} makes every move itself, so the player's own taps, OK / End Turn, undo
 * and yield changes would only knock it out of step. The GUI reaches the controller through
 * {@code AbstractGuiGame.getGameController()}, so handing it this wrapper (see
 * {@code IGuiGame.setGameController}) is enough to drop that input. The director keeps a reference to the real
 * controller and calls it directly, which is why nothing here needs a "this click is scripted" flag, and why
 * {@code PlayerControllerHuman} needs no tutorial code at all.
 *
 * <p>What is dropped is exactly what the first version of the tutorial blocked inside PlayerControllerHuman: card,
 * player, ability and stack selection, OK, Cancel, mana, alpha strike, undo, and yield changes that would make the
 * game skip ahead. Everything else (conceding, leaving at the end, reading state, preferences) passes through, so
 * the player can still quit the demo.
 */
final class TutorialGameController implements IGameController {
    private final IGameController real;
    private volatile boolean locked;

    TutorialGameController(final IGameController real0, final boolean locked0) {
        real = real0;
        locked = locked0;
    }

    /** While locked, the player's own game input is ignored. Safe to call from any thread. */
    void setLocked(final boolean locked0) {
        locked = locked0;
    }

    /** Whether a yield change would make the game skip ahead on its own, which the demo must not allow. */
    private static boolean startsSkippingAhead(final YieldUpdate update) {
        return update instanceof YieldUpdate.SetMarker
                || update instanceof YieldUpdate.StackYield s && s.active()
                || update instanceof YieldUpdate.SetAutoPassUntilEndOfTurn e && e.active()
                || update instanceof YieldUpdate.SkipPhase p && p.skip();
    }

    // ---------------------------------------------------------------------------------------------------------
    // Player input: dropped while locked
    // ---------------------------------------------------------------------------------------------------------

    @Override
    public void alphaStrike() {
        if (!locked) {
            real.alphaStrike();
        }
    }

    @Override
    public void useMana(final byte color) {
        if (!locked) {
            real.useMana(color);
        }
    }

    @Override
    public void selectButtonOk() {
        if (!locked) {
            real.selectButtonOk();
        }
    }

    @Override
    public void selectButtonCancel() {
        if (!locked) {
            real.selectButtonCancel();
        }
    }

    @Override
    public void selectPlayer(final PlayerView playerView, final ITriggerEvent triggerEvent) {
        if (!locked) {
            real.selectPlayer(playerView, triggerEvent);
        }
    }

    @Override
    public boolean selectCard(final CardView cardView, final List<CardView> otherCardViewsToSelect,
                              final ITriggerEvent triggerEvent) {
        return !locked && real.selectCard(cardView, otherCardViewsToSelect, triggerEvent);
    }

    @Override
    public void selectAbility(final SpellAbilityView sa) {
        if (!locked) {
            real.selectAbility(sa);
        }
    }

    @Override
    public void selectStackItem(final StackItemView item) {
        if (!locked) {
            real.selectStackItem(item);
        }
    }

    @Override
    public void undoLastAction() {
        if (!locked) {
            real.undoLastAction();
        }
    }

    /**
     * The GUI's own yield changes (phase markers, stack yield, auto-pass) arrive here, directly or through the
     * default {@link #sendYieldUpdate}, which is deliberately not overridden so it lands in this method too.
     */
    @Override
    public void applyYieldUpdate(final YieldUpdate update) {
        if (locked && startsSkippingAhead(update)) {
            return;
        }
        real.applyYieldUpdate(update);
    }

    // ---------------------------------------------------------------------------------------------------------
    // Everything else passes straight through
    // ---------------------------------------------------------------------------------------------------------

    @Override
    public boolean mayLookAtAllCards() {
        return real.mayLookAtAllCards();
    }

    @Override
    public boolean canPlayUnlimitedLands() {
        return real.canPlayUnlimitedLands();
    }

    @Override
    public void concede() {
        real.concede();
    }

    @Override
    public void drawOfferAction(final DrawOfferMessage.Action action) {
        real.drawOfferAction(action);
    }

    @Override
    public IDevModeCheats cheat() {
        return real.cheat();
    }

    @Override
    public IMacroSystem macros() {
        return real.macros();
    }

    @Override
    public void nextGameDecision(final NextGameDecision decision) {
        real.nextGameDecision(decision);
    }

    @Override
    public String getActivateDescription(final CardView card) {
        return real.getActivateDescription(card);
    }

    @Override
    public void reorderHand(final CardView card, final int index) {
        real.reorderHand(card, index);
    }

    @Override
    public void requestResync() {
        real.requestResync();
    }

    @Override
    public boolean shouldAutoYield(final String key) {
        return real.shouldAutoYield(key);
    }

    @Override
    public void setShouldAutoYield(final String key, final boolean autoYield, final boolean isAbilityScope) {
        real.setShouldAutoYield(key, autoYield, isAbilityScope);
    }

    @Override
    public void setDisableAutoYields(final boolean disable) {
        real.setDisableAutoYields(disable);
    }

    @Override
    public TriggerDecision getTriggerDecision(final String key) {
        return real.getTriggerDecision(key);
    }

    @Override
    public void setTriggerDecision(final String key, final TriggerDecision decision, final boolean isAbilityScope) {
        real.setTriggerDecision(key, decision, isAbilityScope);
    }

    @Override
    public void setDisableAutoTriggers(final boolean disable) {
        real.setDisableAutoTriggers(disable);
    }

    @Override
    public YieldController getYieldController() {
        return real.getYieldController();
    }

    @Override
    public void setYieldPref(final FPref pref, final String value) {
        real.setYieldPref(pref, value);
    }
}