package forge.match;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import forge.FThreads;
import forge.assets.FSkinProp;
import forge.card.CardStateName;
import forge.game.GameView;
import forge.game.card.CardView;
import forge.game.card.CardView.CardStateView;
import forge.game.player.PlayerView;
import forge.interfaces.IGameController;
import forge.interfaces.IGuiGame;
import forge.interfaces.IMayViewCards;
import forge.trackable.TrackableTypes;

public abstract class AbstractGuiGame implements IGuiGame, IMayViewCards {
    private PlayerView currentPlayer = null;
    private IGameController spectator = null;
    private final Map<PlayerView, IGameController> gameControllers = Maps.newHashMap();
    private final Map<PlayerView, IGameController> originalGameControllers = Maps.newHashMap();

    public final boolean hasLocalPlayers() {
        return !gameControllers.isEmpty();
    }
    public final Set<PlayerView> getLocalPlayers() {
        return gameControllers.keySet();
    }
    public final int getLocalPlayerCount() {
        return gameControllers.size();
    }
    public final boolean isLocalPlayer(final PlayerView player) {
        return gameControllers.containsKey(player);
    }

    public final PlayerView getCurrentPlayer() {
        return currentPlayer;
    }
    @Override
    public final void setCurrentPlayer(PlayerView player) {
        player = TrackableTypes.PlayerViewType.lookup(player); //ensure we use the correct player

        if (!gameControllers.containsKey(player)) {
            throw new IllegalArgumentException();
        }

        currentPlayer = player;
        updateCurrentPlayer(player);
    }
    protected abstract void updateCurrentPlayer(PlayerView player);

    private GameView gameView = null;
    public final GameView getGameView() {
        return gameView;
    }
    @Override
    public void setGameView(final GameView gameView0) {
        if (gameView == null || gameView0 == null) {
            if (gameView0 != null) {
                //ensure lookup dictionaries are reset before each game
                TrackableTypes.CardViewType.clearLookupDictionary();
                TrackableTypes.PlayerViewType.clearLookupDictionary();
                TrackableTypes.StackItemViewType.clearLookupDictionary();
                gameView0.updateObjLookup();
            }
            gameView = gameView0;
            return;
        }

        //if game view set to another instance without being first cleared,
        //update existing game view object instead of overwriting it
        gameView.copyChangedProps(gameView0);
    }

    public final IGameController getGameController() {
        return getGameController(getCurrentPlayer());
    }
    public final IGameController getGameController(final PlayerView player) {
        if (player == null) {
            return spectator;
        }
        return gameControllers.get(player);
    }

    public final Collection<IGameController> getOriginalGameControllers() {
        return originalGameControllers.values();
    }

    @Override
    public void setOriginalGameController(PlayerView player, final IGameController gameController) {
        if (player == null || gameController == null) {
            throw new IllegalArgumentException();
        }

        player = TrackableTypes.PlayerViewType.lookup(player); //ensure we use the correct player

        final boolean doSetCurrentPlayer = originalGameControllers.isEmpty();
        originalGameControllers.put(player, gameController);
        gameControllers.put(player, gameController);
        if (doSetCurrentPlayer) {
            setCurrentPlayer(player);
        }
    }

    @Override
    public void setGameController(PlayerView player, final IGameController gameController) {
        if (player == null) {
            throw new IllegalArgumentException();
        }

        player = TrackableTypes.PlayerViewType.lookup(player); //ensure we use the correct player

        if (gameController == null) {
            if (originalGameControllers.containsKey(player)) {
                gameControllers.put(player, originalGameControllers.get(player));
            }
            else {
                gameControllers.remove(player);
                autoPassUntilEndOfTurn.remove(player);
                final PlayerView currentPlayer = getCurrentPlayer();
                if (player.equals(currentPlayer)) {
                    // set current player to a value known to be legal
                    setCurrentPlayer(Iterables.getFirst(gameControllers.keySet(), null));
                }
            }
        }
        else {
            gameControllers.put(player, gameController);
        }
    }

    @Override
    public void setSpectator(final IGameController spectator) {
        this.spectator = spectator;
    }

    @Override
    public final void updateSingleCard(final CardView card) {
        updateCards(Collections.singleton(card));
    }

    @Override
    public void refreshCardDetails(final Iterable<CardView> cards) {
        //not needed for base game implementation
    }

    @Override
    public boolean mayView(final CardView c) {
        if (!hasLocalPlayers()) {
            return true; //if not in game, card can be shown
        }
        if (getGameController().mayLookAtAllCards()) {
            return true;
        }
        return c.canBeShownToAny(getLocalPlayers());
    }

    @Override
    public boolean mayFlip(final CardView cv) {
        if (cv == null) { return false; }

        final CardStateView altState = cv.getAlternateState();
        if (altState == null) { return false; }

        switch (altState.getState()) {
            case Original:
                final CardStateView currentState = cv.getCurrentState();
                if (currentState.getState() == CardStateName.FaceDown) {
                    return getCurrentPlayer() == null || cv.canFaceDownBeShownToAny(getLocalPlayers());
                }
                return true; //original can always be shown if not a face down that can't be shown
            case Flipped:
            case Transformed:
            case Meld:
                return true;
            default:
                return false;
        }
    }

    private final Set<PlayerView> highlightedPlayers = Sets.newHashSet();
    @Override
    public void setHighlighted(final PlayerView pv, final boolean b) {
        if (b) {
            highlightedPlayers.add(pv);
        } else {
            highlightedPlayers.remove(pv);
        }
    }

    public boolean isHighlighted(final PlayerView player) {
        return highlightedPlayers.contains(player);
    }

    private final Set<CardView> highlightedCards = Sets.newHashSet();
    // used to highlight cards in UI
    @Override
    public void setUsedToPay(final CardView card, final boolean value) {
        final boolean hasChanged = value ? highlightedCards.add(card) : highlightedCards.remove(card);
        if (hasChanged) { // since we are in UI thread, may redraw the card right now
            updateSingleCard(card);
        }
    }

    public boolean isUsedToPay(final CardView card) {
        return highlightedCards.contains(card);
    }

    /** Concede game, bring up WinLose UI. */
    public boolean concede() {
        if (gameView.isGameOver()) {
            return true;
        }
        if (hasLocalPlayers()) {
            if (showConfirmDialog("This will concede the current game and you will lose.\n\nConcede anyway?", "Concede Game?", "Concede", "Cancel")) {
                for (final IGameController c : getOriginalGameControllers()) {
                    // Concede each player on this Gui (except mind-controlled players)
                    c.concede();
                }
            } else {
                return false;
            }
            if (gameView.isGameOver()) {
                // Don't immediately close, wait for win/lose screen
                return false;
            } else {
                return true;
            }
        }
        else if (spectator == null) {
            return true; //if no local players or spectator, just quit
        }
        else {
            if (showConfirmDialog("This will close this game and you will not be able to resume watching it.\n\nClose anyway?", "Close Game?", "Close", "Cancel")) {
                IGameController controller = spectator;
                spectator = null; //ensure we don't prompt again, including when calling nextGameDecision below
                controller.nextGameDecision(NextGameDecision.QUIT);
            }
            return false; //let logic above handle closing current screen
        }
    }

    public String getConcedeCaption() {
        if (hasLocalPlayers()) {
            return "Concede";
        }
        return "Stop Watching";
    }

    @Override
    public void updateButtons(final PlayerView owner, final boolean okEnabled, final boolean cancelEnabled, final boolean focusOk) {
        updateButtons(owner, "OK", "Cancel", okEnabled, cancelEnabled, focusOk);
    }

    // Auto-yield and other input-related code

    private final Set<PlayerView> autoPassUntilEndOfTurn = Sets.newHashSet();

    /**
     * Automatically pass priority until reaching the Cleanup phase of the
     * current turn.
     */
    @Override
    public final void autoPassUntilEndOfTurn(final PlayerView player) {
        autoPassUntilEndOfTurn.add(player);
        updateAutoPassPrompt();
    }

    @Override
    public final void autoPassCancel(final PlayerView player) {
        if (!autoPassUntilEndOfTurn.remove(player)) {
            return;
        }

        //prevent prompt getting stuck on yielding message while actually waiting for next input opportunity
        final PlayerView playerView = getCurrentPlayer();
        showPromptMessage(playerView, "");
        updateButtons(playerView, false, false, false);
        awaitNextInput();
    }

    @Override
    public final boolean mayAutoPass(final PlayerView player) {
        return autoPassUntilEndOfTurn.contains(player);
    }

    private final Timer awaitNextInputTimer = new Timer();
    private TimerTask awaitNextInputTask;

    @Override
    public final void awaitNextInput() {
        //delay updating prompt to await next input briefly so buttons don't flicker disabled then enabled
        awaitNextInputTask = new TimerTask() {
            @Override
            public void run() {
                FThreads.invokeInEdtLater(new Runnable() {
                    @Override
                    public void run() {
                        synchronized (awaitNextInputTimer) {
                            if (awaitNextInputTask != null) {
                                updatePromptForAwait(getCurrentPlayer());
                                awaitNextInputTask = null;
                            }
                        }
                    }
                });
            }
        };
        awaitNextInputTimer.schedule(awaitNextInputTask, 250);
    }

    protected final void updatePromptForAwait(final PlayerView playerView) {
        showPromptMessage(playerView, "Waiting for opponent...");
        updateButtons(playerView, false, false, false);
    }

    @Override
    public final void cancelAwaitNextInput() {
        synchronized (awaitNextInputTimer) { //ensure task doesn't reset awaitNextInputTask during this block
            if (awaitNextInputTask != null) {
                try {
                    awaitNextInputTask.cancel(); //cancel timer once next input shown if needed
                } catch (final Exception ex) {} //suppress any exception thrown by cancel()
                awaitNextInputTask = null;
            }
        }
    }

    @Override
    public final void updateAutoPassPrompt() {
        if (!autoPassUntilEndOfTurn.isEmpty()) {
            //allow user to cancel auto-pass
            cancelAwaitNextInput(); //don't overwrite prompt with awaiting opponent
            showPromptMessage(getCurrentPlayer(), "Yielding until end of turn.\nYou may cancel this yield to take an action.");
            updateButtons(getCurrentPlayer(), false, true, false);
        }
    }
    // End auto-yield/input code

    // Abilities to auto-yield to
    private final Set<String> autoYields = Sets.newHashSet();
    public final Iterable<String> getAutoYields() {
        return autoYields;
    }
    @Override
    public final boolean shouldAutoYield(final String key) {
        return !getDisableAutoYields() && autoYields.contains(key);
    }
    @Override
    public final void setShouldAutoYield(final String key, final boolean autoYield) {
        if (autoYield) {
            autoYields.add(key);
        }
        else {
            autoYields.remove(key);
        }
    }

    private boolean disableAutoYields;
    public final boolean getDisableAutoYields() {
        return disableAutoYields;
    }
    public final void setDisableAutoYields(final boolean b0) {
        disableAutoYields = b0;
    }

    // Triggers preliminary choice: ask, decline or play
    private final Map<Integer, Boolean> triggersAlwaysAccept = Maps.newTreeMap();

    @Override
    public final boolean shouldAlwaysAcceptTrigger(final int trigger) { return Boolean.TRUE.equals(triggersAlwaysAccept.get(Integer.valueOf(trigger))); }
    @Override
    public final boolean shouldAlwaysDeclineTrigger(final int trigger) { return Boolean.FALSE.equals(triggersAlwaysAccept.get(Integer.valueOf(trigger))); }

    @Override
    public final void setShouldAlwaysAcceptTrigger(final int trigger) { triggersAlwaysAccept.put(Integer.valueOf(trigger), Boolean.TRUE); }
    @Override
    public final void setShouldAlwaysDeclineTrigger(final int trigger) { triggersAlwaysAccept.put(Integer.valueOf(trigger), Boolean.FALSE); }
    @Override
    public final void setShouldAlwaysAskTrigger(final int trigger) { triggersAlwaysAccept.remove(Integer.valueOf(trigger)); }

    // End of Triggers preliminary choice

    // Start of Choice code

    /**
     * Convenience for getChoices(message, 0, 1, choices).
     *
     * @param <T>
     *            is automatically inferred.
     * @param message
     *            a {@link java.lang.String} object.
     * @param choices
     *            a T object.
     * @return null if choices is missing, empty, or if the users' choices are
     *         empty; otherwise, returns the first item in the List returned by
     *         getChoices.
     * @see #getChoices(String, int, int, Object...)
     */
    @Override
    public <T> T oneOrNone(final String message, final List<T> choices) {
        if (choices == null || choices.isEmpty()) {
            return null;
        }
        final List<T> choice = getChoices(message, 0, 1, choices);
        return choice.isEmpty() ? null : choice.get(0);
    }

    // returned Object will never be null
    /**
     * <p>
     * getChoice.
     * </p>
     *
     * @param <T>
     *            a T object.
     * @param message
     *            a {@link java.lang.String} object.
     * @param choices
     *            a T object.
     * @return a T object.
     */
    @Override
    public <T> T one(final String message, final List<T> choices) {
        if (choices == null || choices.isEmpty()) {
            return null;
        }
        if (choices.size() == 1) {
            return Iterables.getFirst(choices, null);
        }

        final List<T> choice = getChoices(message, 1, 1, choices);
        assert choice.size() == 1;
        return choice.get(0);
    }

    // Nothing to choose here. Code uses this to just reveal one or more items
    @Override
    public <T> void reveal(final String message, final List<T> items) {
        getChoices(message, -1, -1, items);
    }

    // Get Integer in range
    @Override
    public Integer getInteger(final String message, final int min) {
        return getInteger(message, min, Integer.MAX_VALUE, false);
    }
    @Override
    public Integer getInteger(final String message, final int min, final int max) {
        return getInteger(message, min, max, false);
    }
    @Override
    public Integer getInteger(final String message, final int min, final int max, final boolean sortDesc) {
        if (max <= min) { return min; } //just return min if max <= min

        //force cutting off after 100 numbers at most
        if (max == Integer.MAX_VALUE) {
            return getInteger(message, min, max, min + 99);
        }
        final int count = max - min + 1;
        if (count > 100) {
            return getInteger(message, min, max, min + 99);
        }

        final Integer[] choices = new Integer[count];
        if (sortDesc) {
            for (int i = 0; i < count; i++) {
                choices[count - i - 1] = Integer.valueOf(i + min);
            }
        } else {
            for (int i = 0; i < count; i++) {
                choices[i] = Integer.valueOf(i + min);
            }
        }
        return oneOrNone(message, ImmutableList.copyOf(choices));
    }

    @Override
    public Integer getInteger(final String message, final int min, final int max, final int cutoff) {
        if (max <= min || cutoff < min) {
            return min; //just return min if max <= min or cutoff < min
        }

        if (cutoff >= max) { //fallback to regular integer prompt if cutoff at or after max
            return getInteger(message, min, max);
        }

        final ImmutableList.Builder<Serializable> choices = ImmutableList.builder();
        for (int i = min; i <= cutoff; i++) {
            choices.add(Integer.valueOf(i));
        }
        choices.add("...");

        final Object choice = oneOrNone(message, choices.build());
        if (choice instanceof Integer || choice == null) {
            return (Integer) choice;
        }

        //if Other option picked, prompt for number input
        String prompt = "Enter a number";
        if (min != Integer.MIN_VALUE) {
            if (max != Integer.MAX_VALUE) {
                prompt += " between " + min + " and " + max;
            } else {
                prompt += " greater than or equal to " + min;
            }
        } else if (max != Integer.MAX_VALUE) {
            prompt += " less than or equal to " + max;
        }
        prompt += ":";

        while (true) {
            final String str = showInputDialog(prompt, message);
            if (str == null) { return null; } // that is 'cancel'

            if (StringUtils.isNumeric(str)) {
                final Integer val = Integer.valueOf(str);
                if (val >= min && val <= max) {
                    return val;
                }
            }
        }
    }

    // returned Object will never be null
    @Override
    public <T> List<T> getChoices(final String message, final int min, final int max, final List<T> choices) {
        return getChoices(message, min, max, choices, null, null);
    }

    @Override
    public <T> List<T> many(final String title, final String topCaption, final int cnt, final List<T> sourceChoices, final CardView c) {
        return many(title, topCaption, cnt, cnt, sourceChoices, c);
    }

    @Override
    public <T> List<T> many(final String title, final String topCaption, final int min, final int max, final List<T> sourceChoices, final CardView c) {
        final int m2 = min >= 0 ? sourceChoices.size() - min : -1;
        final int m1 = max >= 0 ? sourceChoices.size() - max : -1;
        return order(title, topCaption, m1, m2, sourceChoices, null, c, false);
    }

    @Override
    public <T> List<T> order(final String title, final String top, final List<T> sourceChoices, final CardView c) {
        return order(title, top, 0, 0, sourceChoices, null, c, false);
    }

    /**
     * Ask the user to insert an object into a list of other objects. The
     * current implementation requires the user to cancel in order to get the
     * new item to be the first item in the resulting list.
     *
     * @param title the dialog title.
     * @param newItem the object to insert.
     * @param oldItems the list of objects.
     * @return A shallow copy of the list of objects, with newItem inserted.
     */
    @Override
    public <T> List<T> insertInList(final String title, final T newItem, final List<T> oldItems) {
        final T placeAfter = oneOrNone(title, oldItems);
        final int indexAfter = (placeAfter == null ? 0 : oldItems.indexOf(placeAfter) + 1);
        final List<T> result = Lists.newArrayListWithCapacity(oldItems.size() + 1);
        result.addAll(oldItems);
        result.add(indexAfter, newItem);
        return result;
    }

    @Override
    public String showInputDialog(final String message, final String title) {
        return showInputDialog(message, title, null, "", null);
    }

    @Override
    public String showInputDialog(final String message, final String title, final FSkinProp icon) {
        return showInputDialog(message, title, icon, "", null);
    }

    @Override
    public String showInputDialog(final String message, final String title, final FSkinProp icon, final String initialInput) {
        return showInputDialog(message, title, icon, initialInput, null);
    }

    @Override
    public boolean confirm(final CardView c, final String question) {
        return confirm(c, question, true, null);
    }

    @Override
    public boolean confirm(final CardView c, final String question, final List<String> options) {
        return confirm(c, question, true, options);
    }

    @Override
    public void message(final String message) {
        message(message, "Forge");
    }

    @Override
    public void showErrorDialog(final String message) {
        showErrorDialog(message, "Error");
    }

    @Override
    public boolean showConfirmDialog(final String message, final String title) {
        return showConfirmDialog(message, title, true);
    }

    @Override
    public boolean showConfirmDialog(final String message, final String title,
            final boolean defaultYes) {
        return showConfirmDialog(message, title, "Yes", "No");
    }

    @Override
    public boolean showConfirmDialog(final String message, final String title,
            final String yesButtonText, final String noButtonText) {
        return showConfirmDialog(message, title, yesButtonText, noButtonText, true);
    }

    // End of Choice code
}
