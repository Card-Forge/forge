package forge.gamemodes.match;

import com.google.common.collect.*;
import forge.game.GameEntityView;
import forge.game.GameLog;
import forge.game.GameView;
import forge.game.card.CardView;
import forge.game.card.CardView.CardStateView;
import forge.game.event.GameEvent;
import forge.game.event.GameEventSpellAbilityCast;
import forge.game.event.GameEventSpellRemovedFromStack;
import forge.game.player.PlayerView;
import forge.gamemodes.net.DeltaPacket;
import forge.gui.FThreads;
import forge.gui.GuiBase;
import forge.gui.control.FControlGameEventHandler;
import forge.gui.control.PlaybackSpeed;
import forge.gui.interfaces.IGuiGame;
import forge.gui.interfaces.IMayViewCards;
import forge.interfaces.IGameController;
import forge.player.PlayerControllerHuman;
import forge.player.PlayerZoneUpdate;
import forge.trackable.TrackableCollection;
import forge.trackable.TrackableTypes;
import forge.util.FSerializableFunction;
import forge.util.Localizer;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.*;

public abstract class AbstractGuiGame implements IGuiGame, IMayViewCards {
    private PlayerView currentPlayer = null;
    private IGameController spectator = null;
    private final Map<PlayerView, IGameController> gameControllers = Maps.newHashMap();
    private final Map<PlayerView, IGameController> originalGameControllers = Maps.newHashMap();
    private boolean gamePause = false;
    private PlaybackSpeed playbackSpeed = PlaybackSpeed.NORMAL;
    private String daytime = null;
    private boolean ignoreConcedeChain = false;
    private boolean networkGame = false;

    private java.util.Timer waitingTimer;
    private long waitingStartTime;

    @Override
    public boolean isNetGame() {
        return networkGame;
    }
    @Override
    public void setNetGame() {
        networkGame = true;
    }

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
    public String getDayTime() {
        return daytime;
    }

    @Override
    public void updateDayTime(String daytime) {
        this.daytime = daytime;
    }

    @Override
    public final void setCurrentPlayer(PlayerView player) {
        player = TrackableTypes.PlayerViewType.lookup(player); //ensure we use the correct player

        if (hasLocalPlayers() && !isLocalPlayer(player)) { //add check if gameControllers is not empty
            if(GuiBase.getInterface().isLibgdxPort()){//spectator is registered as localplayer bug on ai vs ai (after .
                if (spectator != null){               //human vs ai game), then it loses "control" when you watch ai vs ai,
                    currentPlayer = null;             //again, and vice versa, This is to prevent throwing error, lose control,
                    updateCurrentPlayer(null);        //workaround fix on mayviewcards below is needed or it will bug the UI..
                    gameControllers.clear();
                    return;
                }
            }

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
            } else {
                gameControllers.remove(player);
                final PlayerView currentPlayer = getCurrentPlayer();
                if (player.equals(currentPlayer)) {
                    // set current player to a value known to be legal
                    setCurrentPlayer(Iterables.getFirst(gameControllers.keySet(), null));
                }
            }
        } else {
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
    public void updateRevealedCards(TrackableCollection<CardView> collection) {
        if (gameView != null) {
            TrackableCollection<CardView> existing = gameView.getRevealedCollection();
            if (existing != null)
                collection.addAll(existing);
            gameView.updateRevealedCards(collection);
        }
    }

    @Override
    public void refreshCardDetails(final Iterable<CardView> cards) {
        //not needed for base game implementation
    }

    @Override
    public void refreshField() {
        //not needed for base game implementation
    }

    @Override
    public boolean mayView(final CardView c) {
        if (!hasLocalPlayers()) {
            return true; //if not in game, card can be shown
        }
        if (GuiBase.getInterface().isLibgdxPort()){
            if (gameView != null && gameView.isGameOver()) {
                return true;
            }
            if (spectator != null) { //workaround fix!! this is needed on above code or it will
                for (Map.Entry<PlayerView, IGameController> e : gameControllers.entrySet()) {
                    if (e.getValue().equals(spectator)) {
                        gameControllers.remove(e.getKey());
                        break;
                    }
                }
                return true;
            }
            try {
                if (getGameController().mayLookAtAllCards()) { // when it bugged here, the game thinks the spectator (null)
                    return true;                               // is the humancontroller here (maybe because there is an existing game thread???)
                }
            } catch (NullPointerException e) {
                return true; // return true so it will work as normal
            }
        } else if (getGameController().mayLookAtAllCards()) {
            return true;
        }
        return c.canBeShownToAny(getLocalPlayers());
    }

    @Override
    public boolean mayFlip(final CardView cv) {
        if (cv == null) {
            return false;
        }

        final CardStateView altState = cv.getAlternateState();
        if (altState == null) {
            return false;
        }

        switch (altState.getState()) {
            case Original:
                if (cv.isFaceDown()) {
                    return getCurrentPlayer() == null || cv.canFaceDownBeShownToAny(getLocalPlayers());
                }
                return true; //original can always be shown if not a face down that can't be shown
            case Flipped:
            case Meld:
            case Backside:
                return true;
            case Secondary:
            case PreparedSpell:
                if (cv.isFaceDown()) {
                    return getCurrentPlayer() == null || cv.canFaceDownBeShownToAny(getLocalPlayers());
                }
                return false;
            default:
                return false;
        }
    }

    private final Set<GameEntityView> highlighted = Sets.newHashSet();

    @Override
    public void setHighlighted(final GameEntityView gv, final boolean b) {
        final boolean hasChanged = b ? highlighted.add(gv) : highlighted.remove(gv);
        if (hasChanged) {
            if (gv instanceof PlayerView pv) {
                updateLives(Collections.singleton(pv));
            }
            if (gv instanceof CardView cv) {
                // since we are in UI thread, may redraw the card right now
                updateSingleCard(cv);
            }
        }
    }

    public boolean isHighlighted(final GameEntityView ge) {
        return highlighted.contains(ge);
    }

    private final Set<CardView> selectableCards = Sets.newHashSet();

    public void setSelectables(final Iterable<CardView> cards) {
        for (CardView cv : cards) {
            selectableCards.add(cv);
        }
    }

    public void clearSelectables() {
        selectableCards.clear();
    }

    public boolean isSelectable(final CardView card) {
        return selectableCards.contains(card);
    }

    public boolean isSelecting() {
        return !selectableCards.isEmpty();
    }

    public boolean isGamePaused() {
        return gamePause;
    }
    public void setGamePause(boolean pause) {
        gamePause = pause;
    }

    public PlaybackSpeed getGameSpeed() {
        return playbackSpeed;
    }
    public void setGameSpeed(PlaybackSpeed speed) {
        playbackSpeed = speed;
    }

    public void pauseMatch() {
        IGameController controller = spectator;
        if (controller != null && !isGamePaused())
            controller.selectButtonOk();
    }

    public void resumeMatch() {
        IGameController controller = spectator;
        if (controller != null && isGamePaused())
            controller.selectButtonOk();
    }

    /**
     * Concede game, bring up WinLose UI.
     */
    public boolean concede() {
        if (gameView.isGameOver()) {
            return true;
        }
        if (hasLocalPlayers()) {
            boolean concedeNeeded = false;
            // check if anyone still needs to concede
            for (final IGameController c : getOriginalGameControllers()) {
                if (c instanceof PlayerControllerHuman pch) {
                    if (pch.getPlayer().getOutcome() == null) {
                        concedeNeeded = true;
                    }
                } else {
                    // Network client — no access to Player outcome, but game
                    // is still in progress (isGameOver checked above)
                    concedeNeeded = true;
                }
            }
            if (concedeNeeded) {
                if (gameView.isMulligan()) { //prevent UI freezing when conceding while the game is waiting for inputs/action
                    showErrorDialog(Localizer.getInstance().getMessage("lblWaitingforActions"));
                    return false;
                }
                if (showConfirmDialog(Localizer.getInstance().getMessage("lblConcedeCurrentGame"), Localizer.getInstance().getMessage("lblConcedeTitle"), Localizer.getInstance().getMessage("lblConcede"), Localizer.getInstance().getMessage("lblCancel"))) {
                    for (final IGameController c : getOriginalGameControllers()) {
                        // Concede each player on this Gui (except mind-controlled players)
                        c.concede();
                    }
                } else {
                    return false;
                }
            } else {
                return !ignoreConcedeChain;
            }
            if (isNetGame()) {
                // Network: concede was sent to server asynchronously.
                // Let the server drive game-end flow — don't send nextGameDecision here.
                return false;
            }
            if (gameView.isGameOver()) {
                // Don't immediately close, wait for win/lose screen
                return false;
            }
            // since the nextGameDecision might come from somewhere else it will try and concede too
            ignoreConcedeChain = true;
            for (PlayerView player : getLocalPlayers()) {
                if (!player.isAI()) {
                    getGameController(player).nextGameDecision(NextGameDecision.QUIT);
                }
            }
            ignoreConcedeChain = false;
            return false;
        } else if (spectator == null) {
            return true; //if no local players or spectator, just quit
        } else {
            if (showConfirmDialog(Localizer.getInstance().getMessage("lblCloseGameSpectator"), Localizer.getInstance().getMessage("lblCloseGame"), Localizer.getInstance().getMessage("lblClose"), Localizer.getInstance().getMessage("lblCancel"))) {
                IGameController controller = spectator;
                spectator = null; //ensure we don't prompt again, including when calling nextGameDecision below
                if (!isGamePaused())
                    controller.selectButtonOk(); //pause
                controller.nextGameDecision(NextGameDecision.QUIT);
            }
            return false; //let logic above handle closing current screen
        }
    }

    public String getConcedeCaption() {
        if (hasLocalPlayers()) {
            return Localizer.getInstance().getMessage("lblConcede");
        }
        return Localizer.getInstance().getMessage("lblStopWatching");
    }

    // Auto-yield and other input-related code

    private Timer awaitNextInputTimer;
    private TimerTask awaitNextInputTask;

    @Override
    public final void awaitNextInput() {
        checkAwaitNextInputTimer();
        //delay updating prompt to await next input briefly so buttons don't flicker disabled then enabled
        awaitNextInputTask = new TimerTask() {
            @Override
            public void run() {
                FThreads.invokeInEdtLater(() -> {
                    checkAwaitNextInputTimer();
                    synchronized (awaitNextInputTimer) {
                        if (awaitNextInputTask != null) {
                            updatePromptForAwait(getCurrentPlayer());
                            if (GuiBase.isNetPlay(AbstractGuiGame.this)) {
                                showWaitingTimer(getCurrentPlayer(), findWaitingForPlayerName(getCurrentPlayer()));
                            }
                            awaitNextInputTask = null;
                        }
                    }
                });
            }
        };
        awaitNextInputTimer.schedule(awaitNextInputTask, 250);
    }

    private void checkAwaitNextInputTimer() {
        if (awaitNextInputTimer == null) {
            String name = "?";
            if (this.currentPlayer != null)
                name = this.currentPlayer.getLobbyPlayerName();
            awaitNextInputTimer = new Timer("awaitNextInputTimer Game:" + this.gameView.getId() + " Player:" + name);
        }
    }

    protected final void updatePromptForAwait(final PlayerView playerView) {
        // Append "Waiting for opponent..." below the yield prompt so the user keeps the
        // cancel-yield UI during opponent turns instead of losing it to the await prompt.
        String waiting = Localizer.getInstance().getMessage("lblWaitingForOpponent");
        String yieldMsg = currentYieldMessage();
        if (yieldMsg != null) {
            cancelAwaitNextInput();
            showPromptMessage(playerView, yieldMsg + "\n\n" + waiting);
            updateButtons(playerView, false, true, false);
        } else {
            showPromptMessage(playerView, waiting);
            updateButtons(playerView, false, false, false);
        }
    }

    private String currentYieldMessage() {
        YieldController yielding = null;
        for (IGameController c : gameControllers.values()) {
            YieldController yc = c.getYieldController();
            if (yc != null && yc.shouldAutoYield()) {
                yielding = yc;
                break;
            }
        }
        if (yielding == null) return null;
        Localizer loc = Localizer.getInstance();
        if (yielding.getAutoPassUntilMarker() != null) {
            YieldMarker m = yielding.getAutoPassUntilMarker();
            return loc.getMessage("lblYieldingUntilPhaseFmt", m.getPhaseOwner().getName(), m.getPhase().nameForUi);
        }
        if (yielding.autoPassUntilStackEmpty()) {
            return loc.getMessage("lblYieldingUntilStackClears");
        }
        if (yielding.autoPassUntilEndOfTurn()) {
            return loc.getMessage("lblYieldingUntilEndOfTurn");
        }
        return null;
    }

    @Override
    public void showWaitingTimer(final PlayerView forPlayer, final String waitingForPlayerName) {
        cancelWaitingTimer();
        if (waitingForPlayerName == null) {
            return;
        }
        this.waitingStartTime = System.currentTimeMillis();
        // Capture timer so stale EDT tick runnables detect cancel/restart and skip
        final java.util.Timer myTimer = new java.util.Timer("waitingTimer");
        waitingTimer = myTimer;
        myTimer.schedule(new java.util.TimerTask() {
            @Override
            public void run() {
                FThreads.invokeInEdtLater(() -> {
                    if (waitingTimer != myTimer) {
                        return; // canceled or replaced before the EDT got to us
                    }
                    updateWaitingDisplay(forPlayer, waitingForPlayerName);
                });
            }
        }, 1000, 1000);
    }

    private void updateWaitingDisplay(final PlayerView forPlayer, final String waitingForPlayerName) {
        long elapsedSec = (System.currentTimeMillis() - waitingStartTime) / 1000;
        if (elapsedSec < 2) {
            return;
        }
        String timeStr;
        if (elapsedSec < 60) {
            timeStr = elapsedSec + "s";
        } else {
            timeStr = String.format("%d:%02d", elapsedSec / 60, elapsedSec % 60);
        }
        String waiting = Localizer.getInstance().getMessage("lblWaitingForPlayer", waitingForPlayerName) + " (" + timeStr + ")";
        String yieldMsg = currentYieldMessage();
        showPromptMessageNoCancel(forPlayer, yieldMsg != null ? yieldMsg + "\n\n" + waiting : waiting);
    }

    protected void cancelWaitingTimer() {
        if (waitingTimer != null) {
            waitingTimer.cancel();
            waitingTimer = null;
        }
    }

    public void showPromptMessageNoCancel(final PlayerView playerView, final String message) {}

    private String findWaitingForPlayerName(final PlayerView forPlayer) {
        if (gameView.getPlayers() != null) {
            for (PlayerView pv : gameView.getPlayers()) {
                if (pv.getHasPriority() && (forPlayer == null || pv.getId() != forPlayer.getId())) {
                    return pv.getName();
                }
            }
        }
        // Fallback to turn player during mulligan/setup
        PlayerView turnPlayer = gameView.getPlayerTurn();
        if (turnPlayer != null && (forPlayer == null || turnPlayer.getId() != forPlayer.getId())) {
            return turnPlayer.getName();
        }
        // Fallback to any non-local player
        if (gameView.getPlayers() != null) {
            for (PlayerView pv : gameView.getPlayers()) {
                if (forPlayer != null && pv.getId() == forPlayer.getId()) {
                    continue;
                }
                if (!isLocalPlayer(pv)) {
                    return pv.getName();
                }
            }
        }
        return null;
    }

    @Override
    public final void cancelAwaitNextInput() {
        if (awaitNextInputTimer == null) {
            return;
        }
        synchronized (awaitNextInputTimer) { //ensure task doesn't reset awaitNextInputTask during this block
            if (awaitNextInputTask != null) {
                try {
                    awaitNextInputTask.cancel(); //cancel timer once next input shown if needed
                } catch (final Exception ex) {
                } //suppress any exception thrown by cancel()
                awaitNextInputTask = null;
            }
        }
        cancelWaitingTimer();
    }

    @Override
    public final void updateAutoPassPrompt() {
        String message = currentYieldMessage();
        if (message == null) return;
        cancelAwaitNextInput();
        showPromptMessage(getCurrentPlayer(), message);
        updateButtons(getCurrentPlayer(), false, true, false);
        if (GuiBase.isNetPlay(this)) {
            showWaitingTimer(getCurrentPlayer(), findWaitingForPlayerName(getCurrentPlayer()));
        }
    }

    @Override
    public void applyYieldUpdate(YieldUpdate update) {
        PlayerView pv = yieldUpdatePlayer(update);
        if (pv == null) return;
        IGameController c = getGameController(pv);
        if (c != null) c.applyYieldUpdate(update);
        refreshYieldUi(pv);
    }

    private static PlayerView yieldUpdatePlayer(YieldUpdate update) {
        if (update instanceof YieldUpdate.ClearMarker u) return u.player();
        if (update instanceof YieldUpdate.StackYield u) return u.player();
        return null;
    }

    // End auto-yield/input code

    /**
     * Convenience for getChoices(message, 0, 1, choices).
     *
     * @param <T>     is automatically inferred.
     * @param message a {@link java.lang.String} object.
     * @param choices a T object.
     * @return null if choices is missing, empty, or if the users' choices are
     * empty; otherwise, returns the first item in the List returned by
     * getChoices.
     * @see #getChoices(String, int, int, List)
     */
    @Override
    public <T> T oneOrNone(final String message, final List<T> choices) {
        if (choices == null || choices.isEmpty()) {
            return null;
        }
        final List<T> choice = getChoices(message, 0, 1, choices);
        return choice.isEmpty() ? null : choice.get(0);
    }

    @Override
    public <T> T one(final String message, final List<T> choices, FSerializableFunction<T, String> display) {
        if (choices == null || choices.isEmpty()) {
            return null;
        }
        if (choices.size() == 1) {
            return Iterables.getFirst(choices, null);
        }

        final List<T> choice = getChoices(message, 1, 1, choices, null, display);
        assert choice.size() == 1;
        return choice.get(0);
    }

    // Nothing to choose here. Code uses this to just reveal one or more items
    @Override
    public <T> void reveal(final String message, final List<T> items) {
        getChoices(message, -1, -1, items);
    }

    @Override
    public Integer getInteger(final String message, final int min, final int max, final boolean sortDesc) {
        if (max <= min) {
            return min;
        } //just return min if max <= min

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
                choices[count - i - 1] = i + min;
            }
        } else {
            for (int i = 0; i < count; i++) {
                choices[i] = i + min;
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
            return getInteger(message, min, max, false);
        }

        final ImmutableList.Builder<Serializable> choices = ImmutableList.builder();
        for (int i = min; i <= cutoff; i++) {
            choices.add(i);
        }
        choices.add(Localizer.getInstance().getMessage("lblOtherInteger"));

        final Object choice = oneOrNone(message, choices.build());
        if (choice instanceof Integer || choice == null) {
            return (Integer) choice;
        }

        //if Other option picked, prompt for number input
        Localizer localizer = Localizer.getInstance();
        String prompt = "";
        if (min != Integer.MIN_VALUE) {
            if (max != Integer.MAX_VALUE) {
                prompt = localizer.getMessage("lblEnterNumberBetweenMinAndMax", min, max);
            } else {
                prompt = localizer.getMessage("lblEnterNumberGreaterThanOrEqualsToMin", min);
            }
        } else if (max != Integer.MAX_VALUE) {
            prompt = localizer.getMessage("lblEnterNumberLessThanOrEqualsToMax", max);
        }

        while (true) {
            final String str = showInputDialog(prompt, message, null, "", null, true);
            if (str == null) {
                return null;
            } // that is 'cancel'

            if (StringUtils.isNumeric(str)) {
                final int val = Integer.parseInt(str);
                if (val >= min && val <= max) {
                    return val;
                }
            }
        }
    }

    @Override
    public <T> List<T> many(String title, String topCaption, int min, int max, List<T> sourceChoices, List<T> destChoices, CardView c) {
        if (max == 1) {
            return getChoices(title, min, max, sourceChoices);
        }
        final int m2 = min >= 0 ? sourceChoices.size() - min : -1;
        final int m1 = max >= 0 ? sourceChoices.size() - max : -1;
        return order(title, topCaption, m1, m2, sourceChoices, destChoices, c, false);
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
     * @param title    the dialog title.
     * @param newItem  the object to insert.
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

    private FControlGameEventHandler localEventHandler;

    @Override
    public void handleGameEvent(GameEvent event) {
        if (localEventHandler == null) {
            localEventHandler = new FControlGameEventHandler(this);
        }
        localEventHandler.receiveGameEvent(event);

        // Feed forwarded events to the local GameLog so remote clients
        // build their own game log (host populates via EventBus instead).
        // gameLog is null for deserialized GameViews until openView calls ensureGameLog().
        GameView gv = getGameView();
        if (gv != null) {
            GameLog gameLog = gv.getGameLog();
            if (gameLog != null) {
                gameLog.getEventVisitor().recieve(event);
            }
        }
    }

    @Override
    public void notifyStackAddition(GameEventSpellAbilityCast event) {
    }

    @Override
    public void notifyStackRemoval(GameEventSpellRemovedFromStack event) {
    }

    @Override
    public void handleLandPlayed(CardView land) {
    }

    @Override
    public void updateStack() { }

    @Override
    public void updatePhase(boolean saveState) { }

    @Override
    public void updateTurn(PlayerView player) { }

    @Override
    public void updatePlayerControl() { }

    @Override
    public void updateZones(Iterable<PlayerZoneUpdate> zonesToUpdate) { }

    @Override
    public void updateCards(Iterable<CardView> cards) { }

    @Override
    public void updateManaPool(Iterable<PlayerView> manaPoolUpdate) { }

    @Override
    public void updateLives(Iterable<PlayerView> livesUpdate) { }

    @Override
    public void afterGameEnd() {
        if (awaitNextInputTimer != null) {
            awaitNextInputTimer.cancel();
            awaitNextInputTimer = null;
        }
        daytime = null;
    }

    @Override
    public void updateDependencies() {
    }

    @Override
    public void applyDelta(DeltaPacket packet) {
        // No-op for local games - network implementation is in NetworkGuiGame
    }

}
