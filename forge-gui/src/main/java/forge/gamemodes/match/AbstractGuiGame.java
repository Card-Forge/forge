package forge.gamemodes.match;

import com.google.common.collect.*;
import forge.game.GameView;
import forge.card.CardStateName;
import forge.game.card.Card;
import forge.game.card.CardView;
import forge.game.card.CardView.CardStateView;
import forge.game.combat.CombatView;
import forge.game.spellability.StackItemView;
import forge.game.event.GameEventSpellAbilityCast;
import forge.game.event.GameEventSpellRemovedFromStack;
import forge.game.player.PlayerView;
import forge.game.zone.ZoneType;
import forge.player.PlayerZoneUpdate;
import forge.gamemodes.net.DeltaPacket;
import forge.gamemodes.net.DeltaPacket.NewObjectData;
import forge.gamemodes.net.FullStatePacket;
import forge.gamemodes.net.NetworkPropertySerializer;
import forge.gamemodes.net.NetworkPropertySerializer.CardStateViewData;
import forge.gamemodes.net.NetworkDebugLogger;
import forge.gamemodes.net.NetworkTrackableDeserializer;
import forge.gui.FThreads;
import forge.gui.GuiBase;
import forge.gui.control.PlaybackSpeed;
import forge.gui.interfaces.IGuiGame;
import forge.gui.interfaces.IMayViewCards;
import forge.interfaces.IGameController;
import forge.localinstance.properties.ForgeConstants;
import forge.localinstance.properties.ForgePreferences;
import forge.localinstance.skin.FSkinProp;
import forge.model.FModel;
import forge.player.PlayerControllerHuman;
import forge.trackable.Tracker;
import forge.trackable.TrackableCollection;
import forge.trackable.TrackableObject;
import forge.trackable.TrackableProperty;
import forge.trackable.TrackableTypes;
import forge.util.FSerializableFunction;
import forge.util.Localizer;
import org.apache.commons.lang3.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.Serializable;
import java.util.*;

public abstract class AbstractGuiGame implements IGuiGame, IMayViewCards {
    private PlayerView currentPlayer = null;
    private IGameController spectator = null;
    private final Map<PlayerView, IGameController> gameControllers = Maps.newHashMap();
    private final Map<PlayerView, IGameController> originalGameControllers = Maps.newHashMap();
    private boolean gamePause = false;
    private boolean gameSpeed = false;
    private PlaybackSpeed playbackSpeed = PlaybackSpeed.NORMAL;
    private String daytime = null;
    private boolean ignoreConcedeChain = false;

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
        NetworkDebugLogger.log("[setGameView] Called with gameView0=%s, existing gameView=%s",
                gameView0 != null ? "non-null" : "null",
                gameView != null ? "non-null" : "null");

        if (gameView == null || gameView0 == null) {
            if (gameView0 != null) {
                // When receiving a deserialized GameView from the network,
                // the tracker field is null (it's transient). We need to:
                // 1. Create a new Tracker
                // 2. Set it on the GameView and all contained objects
                // 3. Call updateObjLookup() to populate the index
                ensureTrackerInitialized(gameView0);
                gameView0.updateObjLookup();

                // Log PlayerView instances after initialization
                if (gameView0.getPlayers() != null) {
                    for (PlayerView pv : gameView0.getPlayers()) {
                        Tracker t = gameView0.getTracker();
                        PlayerView inTracker = t != null ? t.getObj(TrackableTypes.PlayerViewType, pv.getId()) : null;
                        NetworkDebugLogger.debug("[setGameView] Initial setup: Player %d hash=%d, inTracker=%b, sameInstance=%b",
                                pv.getId(), System.identityHashCode(pv),
                                inTracker != null, pv == inTracker);
                    }
                }
            }
            gameView = gameView0;
            return;
        }

        // When updating an existing game view, the incoming gameView0 may not have
        // its tracker initialized (since tracker is transient and null after deserialization).
        // We need to ensure it has a tracker before copyChangedProps tries to use it.
        if (gameView0.getTracker() == null) {
            // Use the existing gameView's tracker for the incoming gameView0
            Tracker existingTracker = gameView.getTracker();
            if (existingTracker != null) {
                java.util.Set<Integer> visited = new java.util.HashSet<>();
                setTrackerRecursively(gameView0, existingTracker, visited);
            }
        }

        //if game view set to another instance without being first cleared,
        //update existing game view object instead of overwriting it
        gameView.copyChangedProps(gameView0);
    }

    /**
     * Ensure the Tracker is initialized on a GameView and all its contained objects.
     * This is necessary after network deserialization because the tracker field is transient.
     */
    private void ensureTrackerInitialized(GameView gameView0) {
        if (gameView0 == null) return;

        // Check if tracker needs to be created
        Tracker tracker = gameView0.getTracker();
        if (tracker == null) {
            tracker = new Tracker();
            gameView0.setTracker(tracker);
        }

        // Recursively set tracker on all TrackableObjects in the GameView's properties
        java.util.Set<Integer> visited = new java.util.HashSet<>();
        setTrackerRecursively(gameView0, tracker, visited);
        NetworkDebugLogger.log("[EnsureTracker] Set tracker on %d unique objects", visited.size());

        // Verify trackers are set on players and their cards
        if (gameView0.getPlayers() != null) {
            for (PlayerView player : gameView0.getPlayers()) {
                boolean playerHasTracker = player.getTracker() != null;
                int cardsWithTracker = 0;
                int cardsWithoutTracker = 0;
                if (player.getHand() != null) {
                    for (CardView card : player.getHand()) {
                        if (card.getTracker() != null) cardsWithTracker++;
                        else cardsWithoutTracker++;
                    }
                }
                if (player.getLibrary() != null) {
                    for (CardView card : player.getLibrary()) {
                        if (card.getTracker() != null) cardsWithTracker++;
                        else cardsWithoutTracker++;
                    }
                }
                NetworkDebugLogger.debug("[EnsureTracker] Player %d: hasTracker=%b, cards with tracker=%d, cards without=%d",
                    player.getId(), playerHasTracker, cardsWithTracker, cardsWithoutTracker);
            }
        }
    }

    /**
     * Recursively set the tracker on a TrackableObject and all objects it references.
     * Uses a visited set to avoid infinite loops from circular references.
     */
    @SuppressWarnings("unchecked")
    private void setTrackerRecursively(TrackableObject obj, Tracker tracker, java.util.Set<Integer> visited) {
        if (obj == null) return;

        // Avoid infinite loops
        int objId = System.identityHashCode(obj);
        if (visited.contains(objId)) return;
        visited.add(objId);

        // Set tracker on this object
        if (obj.getTracker() == null) {
            obj.setTracker(tracker);
        }

        // Get all properties and recursively process any TrackableObjects
        Map<TrackableProperty, Object> props = obj.getProps();
        if (props == null) return;

        for (Map.Entry<TrackableProperty, Object> entry : props.entrySet()) {
            Object value = entry.getValue();
            if (value == null) continue;

            // Handle single TrackableObject
            if (value instanceof TrackableObject) {
                setTrackerRecursively((TrackableObject) value, tracker, visited);
            }
            // Handle collections of TrackableObjects
            else if (value instanceof Iterable) {
                for (Object item : (Iterable<?>) value) {
                    if (item instanceof TrackableObject) {
                        setTrackerRecursively((TrackableObject) item, tracker, visited);
                    }
                }
            }
            // Handle maps that might contain TrackableObjects
            else if (value instanceof Map) {
                for (Object mapValue : ((Map<?, ?>) value).values()) {
                    if (mapValue instanceof TrackableObject) {
                        setTrackerRecursively((TrackableObject) mapValue, tracker, visited);
                    }
                }
            }
        }
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
                autoPassUntilEndOfTurn.remove(player);
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
        } else {
            if (getGameController().mayLookAtAllCards()) {
                return true;
            }
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
                if (cv.isFaceDown()) {
                    return getCurrentPlayer() == null || cv.canFaceDownBeShownToAny(getLocalPlayers());
                }
                return false;
            default:
                return false;
        }
    }

    private final Set<PlayerView> highlightedPlayers = Sets.newHashSet();

    @Override
    public void setHighlighted(final PlayerView pv, final boolean b) {
        final boolean hasChanged = b ? highlightedPlayers.add(pv) : highlightedPlayers.remove(pv);
        if (hasChanged) {
            updateLives(Collections.singleton(pv));
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

    public boolean isGameFast() {
        return gameSpeed;
    }

    public void setgamePause(boolean pause) {
        gamePause = pause;
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
            // check if anyone still needs to confirm
            for (final IGameController c : getOriginalGameControllers()) {
                if (c instanceof PlayerControllerHuman) {
                    if (((PlayerControllerHuman) c).getPlayer().getOutcome() == null) {
                        concedeNeeded = true;
                    }
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

    @Override
    public void updateButtons(final PlayerView owner, final boolean okEnabled, final boolean cancelEnabled, final boolean focusOk) {
        updateButtons(owner, Localizer.getInstance().getMessage("lblOK"), Localizer.getInstance().getMessage("lblCancel"), okEnabled, cancelEnabled, focusOk);
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
        showPromptMessage(playerView, Localizer.getInstance().getMessage("lblWaitingForOpponent"));
        updateButtons(playerView, false, false, false);
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
    }

    @Override
    public final void updateAutoPassPrompt() {
        if (!autoPassUntilEndOfTurn.isEmpty()) {
            //allow user to cancel auto-pass
            cancelAwaitNextInput(); //don't overwrite prompt with awaiting opponent
            showPromptMessage(getCurrentPlayer(), Localizer.getInstance().getMessage("lblYieldingUntilEndOfTurn"));
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
        String abilityKey = key.contains("): ") ? key.substring(key.indexOf("): ") + 3) : key;
        boolean yieldPerAbility = FModel.getPreferences().getPref(ForgePreferences.FPref.UI_AUTO_YIELD_MODE).equals(ForgeConstants.AUTO_YIELD_PER_ABILITY);

        return !getDisableAutoYields() && autoYields.contains(yieldPerAbility ? abilityKey : key);
    }

    @Override
    public final void setShouldAutoYield(final String key, final boolean autoYield) {
        String abilityKey = key.contains("): ") ? key.substring(key.indexOf("): ") + 3) : key;
        boolean yieldPerAbility = FModel.getPreferences().getPref(ForgePreferences.FPref.UI_AUTO_YIELD_MODE).equals(ForgeConstants.AUTO_YIELD_PER_ABILITY);

        if (autoYield) {
            autoYields.add(yieldPerAbility ? abilityKey : key);
        } else {
            autoYields.remove(yieldPerAbility ? abilityKey : key);
        }
    }

    private boolean disableAutoYields;

    public final boolean getDisableAutoYields() {
        return disableAutoYields;
    }

    public final void setDisableAutoYields(final boolean b0) {
        disableAutoYields = b0;
    }

    @Override
    public final void clearAutoYields() {
        autoYields.clear();
        triggersAlwaysAccept.clear();
    }

    // Triggers preliminary choice: ask, decline or play
    private final Map<Integer, Boolean> triggersAlwaysAccept = Maps.newTreeMap();

    @Override
    public final boolean shouldAlwaysAcceptTrigger(final int trigger) {
        return Boolean.TRUE.equals(triggersAlwaysAccept.get(trigger));
    }

    @Override
    public final boolean shouldAlwaysDeclineTrigger(final int trigger) {
        return Boolean.FALSE.equals(triggersAlwaysAccept.get(trigger));
    }

    @Override
    public final void setShouldAlwaysAcceptTrigger(final int trigger) {
        triggersAlwaysAccept.put(trigger, Boolean.TRUE);
    }

    @Override
    public final void setShouldAlwaysDeclineTrigger(final int trigger) {
        triggersAlwaysAccept.put(trigger, Boolean.FALSE);
    }

    @Override
    public final void setShouldAlwaysAskTrigger(final int trigger) {
        triggersAlwaysAccept.remove(trigger);
    }

    // End of Triggers preliminary choice

    // Start of Choice code

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

    // returned Object will never be null

    /**
     * <p>
     * getChoice.
     * </p>
     *
     * @param <T>     a T object.
     * @param message a {@link java.lang.String} object.
     * @param choices a T object.
     * @return a T object.
     */
    @Override
    public <T> T one(final String message, final List<T> choices) {
        return one(message, choices, null);
    }
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
            return getInteger(message, min, max);
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
                prompt = localizer.getMessage("lblEnterNumberBetweenMinAndMax", String.valueOf(min), String.valueOf(max));
            } else {
                prompt = localizer.getMessage("lblEnterNumberGreaterThanOrEqualsToMin", String.valueOf(min));
            }
        } else if (max != Integer.MAX_VALUE) {
            prompt = localizer.getMessage("lblEnterNumberLessThanOrEqualsToMax", String.valueOf(max));
        }

        while (true) {
            final String str = showInputDialog(prompt, message, true);
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
        return many(title, topCaption, min, max, sourceChoices, null, c);
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

    @Override
    public String showInputDialog(final String message, final String title, boolean isNumeric) {
        return showInputDialog(message, title, null, "", null, isNumeric);
    }

    @Override
    public String showInputDialog(final String message, final String title, final FSkinProp icon) {
        return showInputDialog(message, title, icon, "", null, false);
    }

    @Override
    public String showInputDialog(final String message, final String title, final FSkinProp icon, final String initialInput) {
        return showInputDialog(message, title, icon, initialInput, null, false);
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
        return showConfirmDialog(message, title, Localizer.getInstance().getMessage("lblYes"), Localizer.getInstance().getMessage("lblNo"));
    }

    @Override
    public boolean showConfirmDialog(final String message, final String title,
                                     final String yesButtonText, final String noButtonText) {
        return showConfirmDialog(message, title, yesButtonText, noButtonText, true);
    }

    @Override
    public void notifyStackAddition(GameEventSpellAbilityCast event) {
    }

    @Override
    public void notifyStackRemoval(GameEventSpellRemovedFromStack event) {
    }

    @Override
    public void handleLandPlayed(Card land) {
    }

    @Override
    public void afterGameEnd() {
        if (awaitNextInputTimer != null) {
            awaitNextInputTimer.cancel();
            awaitNextInputTimer = null;
        }
        daytime = null;
    }

    public void updateDependencies() {
    }
    // End of Choice code

    // Delta sync and reconnection default implementations

    // Track zone changes during delta application for UI refresh
    private final Map<PlayerView, Set<ZoneType>> pendingZoneUpdates = new HashMap<>();

    @Override
    public void applyDelta(DeltaPacket packet) {
        if (packet == null || gameView == null) {
            return;
        }

        Tracker tracker = gameView.getTracker();
        if (tracker == null) {
            NetworkDebugLogger.error("[DeltaSync] Cannot apply delta: Tracker is null");
            return;
        }

        // Debug: Log what PlayerViews are in the tracker at start
        NetworkDebugLogger.log("[DeltaSync] === START applyDelta seq=%d ===", packet.getSequenceNumber());
        if (gameView.getPlayers() != null) {
            for (PlayerView pv : gameView.getPlayers()) {
                PlayerView inTracker = tracker.getObj(TrackableTypes.PlayerViewType, pv.getId());
                boolean sameInstance = (pv == inTracker);
                NetworkDebugLogger.debug("[DeltaSync] GameView PlayerView ID=%d hash=%d, inTracker=%s trackerHash=%d, sameInstance=%b",
                        pv.getId(), System.identityHashCode(pv),
                        inTracker != null ? "FOUND" : "NULL",
                        inTracker != null ? System.identityHashCode(inTracker) : 0,
                        sameInstance);
            }
        }

        int newObjectCount = 0;
        int appliedCount = 0;
        int skippedCount = 0;

        // Clear pending zone updates before processing
        pendingZoneUpdates.clear();

        // STEP 1: Create new objects first (so deltas can reference them)
        Map<Integer, NewObjectData> newObjects = packet.getNewObjects();
        if (!newObjects.isEmpty()) {
            for (NewObjectData newObj : newObjects.values()) {
                try {
                    createObjectFromData(newObj, tracker);
                    newObjectCount++;
                } catch (Exception e) {
                    NetworkDebugLogger.error("[DeltaSync] Error creating new object " + newObj.getObjectId(), e);
                }
            }
            NetworkDebugLogger.log("[DeltaSync] Created %d new objects", newObjectCount);

            // Verify CardViews are in tracker
            int verifyCount = 0;
            for (NewObjectData newObj : newObjects.values()) {
                if (newObj.getObjectType() == NewObjectData.TYPE_CARD_VIEW) {
                    CardView cv = tracker.getObj(TrackableTypes.CardViewType, newObj.getObjectId());
                    if (cv != null) {
                        verifyCount++;
                    } else {
                        NetworkDebugLogger.warn("[DeltaSync] VERIFY FAILED: CardView %d not in tracker after creation!", newObj.getObjectId());
                    }
                }
            }
            NetworkDebugLogger.debug("[DeltaSync] Verified %d CardViews in tracker", verifyCount);
        }

        // STEP 2: Apply property deltas to existing objects
        for (Map.Entry<Integer, byte[]> entry : packet.getObjectDeltas().entrySet()) {
            int objectId = entry.getKey();
            byte[] deltaBytes = entry.getValue();

            TrackableObject obj = findObjectById(tracker, objectId);
            if (obj != null) {
                try {
                    applyDeltaToObject(obj, deltaBytes, tracker);
                    appliedCount++;
                } catch (Exception e) {
                    NetworkDebugLogger.error("[DeltaSync] Error applying delta to object " + objectId, e);
                    skippedCount++;
                }
            } else {
                // Object not found - this shouldn't happen if new objects were created first
                NetworkDebugLogger.warn("[DeltaSync] Object ID %d NOT FOUND for delta application", objectId);
                skippedCount++;
            }
        }

        // STEP 3: Handle removed objects
        if (!packet.getRemovedObjectIds().isEmpty()) {
            NetworkDebugLogger.debug("[DeltaSync] Packet contains %d removed objects", packet.getRemovedObjectIds().size());
            // Note: Objects are not removed from Tracker - they'll just not be updated anymore
            // and will be garbage collected when no longer referenced by the game state
        }

        // STEP 4: Refresh UI for any changed zones
        if (!pendingZoneUpdates.isEmpty()) {
            List<PlayerZoneUpdate> zoneUpdates = new ArrayList<>();
            for (Map.Entry<PlayerView, Set<ZoneType>> entry : pendingZoneUpdates.entrySet()) {
                PlayerView player = entry.getKey();
                Set<ZoneType> zones = entry.getValue();
                if (!zones.isEmpty()) {
                    PlayerZoneUpdate update = new PlayerZoneUpdate(player, null);
                    for (ZoneType zone : zones) {
                        update.addZone(zone);
                    }
                    zoneUpdates.add(update);
                    NetworkDebugLogger.debug("[DeltaSync] UI refresh: player=%d, zones=%s, hash=%d",
                            player.getId(), zones, System.identityHashCode(player));
                }
            }
            if (!zoneUpdates.isEmpty()) {
                updateZones(zoneUpdates);
            }
            pendingZoneUpdates.clear();
        }

        // Log summary
        if (newObjectCount > 0 || appliedCount > 0 || skippedCount > 0) {
            NetworkDebugLogger.log("[DeltaSync] Summary: %d new objects, %d deltas applied, %d skipped",
                    newObjectCount, appliedCount, skippedCount);
        }

        // Validate checksum if present (every 10 packets)
        if (packet.hasChecksum()) {
            int serverChecksum = packet.getChecksum();
            int clientChecksum = computeStateChecksum(gameView);

            if (serverChecksum != clientChecksum) {
                NetworkDebugLogger.error("[DeltaSync] CHECKSUM MISMATCH! Server=%d, Client=%d at sequence=%d",
                        serverChecksum, clientChecksum, packet.getSequenceNumber());
                NetworkDebugLogger.error("[DeltaSync] State desynchronization detected - requesting full state resync");

                // Automatically request full state resync to recover
                requestFullStateResync();
                return; // Don't send ack for corrupted state
            } else {
                NetworkDebugLogger.log("[DeltaSync] Checksum validated successfully (seq=%d, checksum=%d)",
                        packet.getSequenceNumber(), serverChecksum);
            }
        }

        // Send acknowledgment
        IGameController controller = getGameController();
        if (controller != null) {
            controller.ackSync(packet.getSequenceNumber());
        }
    }

    /**
     * Compute a checksum of the current game state for validation.
     * Uses the same algorithm as the server for consistency.
     */
    private int computeStateChecksum(GameView gameView) {
        int hash = 17;
        hash = 31 * hash + gameView.getId();
        hash = 31 * hash + gameView.getTurn();
        if (gameView.getPhase() != null) {
            hash = 31 * hash + gameView.getPhase().hashCode();
        }
        if (gameView.getPlayers() != null) {
            for (PlayerView player : gameView.getPlayers()) {
                hash = 31 * hash + player.getId();
                hash = 31 * hash + player.getLife();
            }
        }
        return hash;
    }

    /**
     * Request a full state resync from the server to recover from desynchronization.
     * This is called automatically when checksum validation fails.
     */
    private void requestFullStateResync() {
        NetworkDebugLogger.warn("[DeltaSync] Requesting full state resync from server");

        IGameController controller = getGameController();
        if (controller != null) {
            controller.requestResync();
        } else {
            NetworkDebugLogger.error("[DeltaSync] Cannot request resync: No game controller available");
        }
    }

    /**
     * Create a new TrackableObject from NewObjectData and register it in the Tracker.
     */
    private void createObjectFromData(NewObjectData data, Tracker tracker) throws Exception {
        int objectId = data.getObjectId();
        int objectType = data.getObjectType();
        byte[] fullProps = data.getFullProperties();

        // Log what type of object we're trying to create
        String typeName = "Unknown";
        switch (objectType) {
            case NewObjectData.TYPE_CARD_VIEW: typeName = "CardView"; break;
            case NewObjectData.TYPE_PLAYER_VIEW: typeName = "PlayerView"; break;
            case NewObjectData.TYPE_STACK_ITEM_VIEW: typeName = "StackItemView"; break;
            case NewObjectData.TYPE_COMBAT_VIEW: typeName = "CombatView"; break;
            case NewObjectData.TYPE_GAME_VIEW: typeName = "GameView"; break;
        }

        // Check if object already exists (shouldn't happen, but be safe)
        TrackableObject existing = findObjectById(tracker, objectId);
        if (existing != null) {
            NetworkDebugLogger.debug("[DeltaSync] %s ID=%d already exists (hash=%d), applying properties as delta",
                    typeName, objectId, System.identityHashCode(existing));
            applyDeltaToObject(existing, fullProps, tracker);
            return;
        }

        // Log when creating new object (especially important for PlayerView)
        if (objectType == NewObjectData.TYPE_PLAYER_VIEW) {
            NetworkDebugLogger.warn("[DeltaSync] Creating NEW PlayerView ID=%d - this may cause identity mismatch!", objectId);
        }

        // Create the appropriate object type
        TrackableObject obj = null;
        switch (objectType) {
            case NewObjectData.TYPE_CARD_VIEW:
                obj = new CardView(objectId, tracker);
                tracker.putObj(TrackableTypes.CardViewType, objectId, (CardView) obj);
                break;
            case NewObjectData.TYPE_PLAYER_VIEW:
                obj = new PlayerView(objectId, tracker);
                tracker.putObj(TrackableTypes.PlayerViewType, objectId, (PlayerView) obj);
                NetworkDebugLogger.debug("[DeltaSync] Created NEW PlayerView ID=%d hash=%d", objectId, System.identityHashCode(obj));
                break;
            case NewObjectData.TYPE_STACK_ITEM_VIEW:
                obj = new StackItemView(objectId, tracker);
                tracker.putObj(TrackableTypes.StackItemViewType, objectId, (StackItemView) obj);
                break;
            case NewObjectData.TYPE_COMBAT_VIEW:
                // CombatView uses ID -1 (singleton pattern)
                obj = new CombatView(tracker);
                // Register with the actual ID from the data
                tracker.putObj(TrackableTypes.CombatViewType, obj.getId(), (CombatView) obj);
                break;
            case NewObjectData.TYPE_GAME_VIEW:
                // GameView is special - we already have one, update it
                if (gameView != null) {
                    applyDeltaToObject(gameView, fullProps, tracker);
                    return;
                }
                break;
            default:
                NetworkDebugLogger.error("[DeltaSync] Unknown object type: %d", objectType);
                return;
        }

        if (obj != null) {
            // Apply all properties to the new object
            applyDeltaToObject(obj, fullProps, tracker);
            NetworkDebugLogger.debug("[DeltaSync] Created %s ID=%d",
                    obj.getClass().getSimpleName(), obj.getId());
        }
    }

    /**
     * Find a TrackableObject by ID in the Tracker.
     * Searches through known object types (CardView, PlayerView, etc.)
     */
    private TrackableObject findObjectById(Tracker tracker, int objectId) {
        // Try CardView first (most common)
        TrackableObject obj = tracker.getObj(TrackableTypes.CardViewType, objectId);
        if (obj != null) return obj;

        // Try PlayerView
        obj = tracker.getObj(TrackableTypes.PlayerViewType, objectId);
        if (obj != null) return obj;

        // Try StackItemView
        obj = tracker.getObj(TrackableTypes.StackItemViewType, objectId);
        if (obj != null) return obj;

        // Try CombatView
        obj = tracker.getObj(TrackableTypes.CombatViewType, objectId);
        if (obj != null) return obj;

        // Check if it's the GameView itself
        if (gameView != null && gameView.getId() == objectId) {
            return gameView;
        }

        // Debug: Log when object not found
        NetworkDebugLogger.warn("[DeltaSync] Object ID %d NOT FOUND in any lookup", objectId);

        return null;
    }

    /**
     * Apply deserialized delta bytes to a TrackableObject.
     * Reads the compact binary format and updates properties.
     */
    @SuppressWarnings("unchecked")
    private void applyDeltaToObject(TrackableObject obj, byte[] deltaBytes, Tracker tracker) throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream(deltaBytes);
        DataInputStream dis = new DataInputStream(bais);
        NetworkTrackableDeserializer ntd = new NetworkTrackableDeserializer(dis, tracker);

        int propCount = dis.readInt();
        ntd.resetBytesRead(); // Reset counter after the initial propCount read
        Map<TrackableProperty, Object> props = obj.getProps();

        NetworkDebugLogger.debug("[DeltaSync] applyDeltaToObject: objId=%d, objType=%s, deltaBytes=%d, propCount=%d",
                obj.getId(), obj.getClass().getSimpleName(), deltaBytes.length, propCount);

        for (int i = 0; i < propCount; i++) {
            int bytePosBefore = ntd.getBytesRead() + 4; // +4 for the initial propCount
            int ordinal = dis.readInt();

            // Validate ordinal before deserialization
            if (ordinal < 0 || ordinal >= TrackableProperty.values().length) {
                NetworkDebugLogger.error("[DeltaSync] ERROR: Invalid ordinal %d (0x%08X) at byte %d for prop %d/%d in object %d (%s)",
                        ordinal, ordinal, bytePosBefore, i + 1, propCount, obj.getId(), obj.getClass().getSimpleName());
                NetworkDebugLogger.error("[DeltaSync] Valid ordinal range: 0-%d",
                        TrackableProperty.values().length - 1);
                NetworkDebugLogger.hexDump("[DeltaSync] Hex dump of delta bytes:", deltaBytes, bytePosBefore);
                throw new RuntimeException("Invalid TrackableProperty ordinal: " + ordinal);
            }

            TrackableProperty prop = TrackableProperty.deserialize(ordinal);
            Object oldValue = props != null ? props.get(prop) : null;

            try {
                Object value = NetworkPropertySerializer.deserialize(ntd, prop, oldValue);
                // Log what's being set for PlayerView
                if (obj instanceof PlayerView) {
                    String valueDesc = value == null ? "null" :
                        (value instanceof TrackableCollection ? "Collection[" + ((TrackableCollection<?>)value).size() + "]" : value.getClass().getSimpleName());
                    NetworkDebugLogger.debug("[DeltaSync] PlayerView %d: setting %s = %s", obj.getId(), prop, valueDesc);

                    // Track zone changes for UI refresh
                    ZoneType changedZone = getZoneTypeForProperty(prop);
                    if (changedZone != null) {
                        trackZoneChange((PlayerView) obj, changedZone);
                    }
                }
                // Use reflection to call the protected set method
                setPropertyValue(obj, prop, value);
            } catch (Exception e) {
                int bytePosAfter = ntd.getBytesRead() + 4; // +4 for the initial propCount
                NetworkDebugLogger.error("[DeltaSync] Error deserializing property %s (ordinal=%d) at bytes %d-%d: %s",
                        prop, ordinal, bytePosBefore, bytePosAfter, e.getMessage());
                NetworkDebugLogger.hexDump("[DeltaSync] Hex dump of delta bytes:", deltaBytes, bytePosBefore);
                NetworkDebugLogger.error("[DeltaSync] Exception details:", e);
            }
        }
    }

    /**
     * Map a TrackableProperty to its corresponding ZoneType, if any.
     * @param prop the property to check
     * @return the ZoneType if this is a zone property, null otherwise
     */
    private static ZoneType getZoneTypeForProperty(TrackableProperty prop) {
        switch (prop) {
            case Hand: return ZoneType.Hand;
            case Library: return ZoneType.Library;
            case Graveyard: return ZoneType.Graveyard;
            case Battlefield: return ZoneType.Battlefield;
            case Exile: return ZoneType.Exile;
            case Command: return ZoneType.Command;
            case Flashback: return ZoneType.Flashback;
            case Ante: return ZoneType.Ante;
            case Sideboard: return ZoneType.Sideboard;
            default: return null;
        }
    }

    /**
     * Track a zone change for UI refresh after delta application.
     * @param player the player whose zone changed
     * @param zone the zone that changed
     */
    private void trackZoneChange(PlayerView player, ZoneType zone) {
        pendingZoneUpdates.computeIfAbsent(player, k -> EnumSet.noneOf(ZoneType.class)).add(zone);
    }

    /**
     * Set a property value on a TrackableObject.
     * Uses reflection to access the protected set method.
     * Handles CardStateViewData specially by applying properties to existing CardStateView.
     */
    private void setPropertyValue(TrackableObject obj, TrackableProperty prop, Object value) {
        try {
            // Handle CardStateViewData specially - apply to existing CardStateView
            if (value instanceof CardStateViewData && obj instanceof CardView) {
                CardView cardView = (CardView) obj;
                CardStateViewData csvData = (CardStateViewData) value;
                CardStateView csv = null;

                // Get the appropriate CardStateView based on the property
                if (prop == TrackableProperty.CurrentState) {
                    csv = cardView.getCurrentState();
                    if (csv == null) {
                        // CurrentState should have been created by CardView constructor
                        // If it's null, something is wrong - try to create it
                        NetworkDebugLogger.warn("[DeltaSync] CurrentState is null for CardView %d, attempting to create with state=%s",
                                cardView.getId(), csvData.state);
                        csv = createCardStateView(cardView, csvData.state);
                        if (csv != null) {
                            // Set the newly created CardStateView as CurrentState
                            cardView.set(TrackableProperty.CurrentState, csv);
                        }
                    } else if (csv.getState() != csvData.state) {
                        // State mismatch - log warning but continue with property application
                        NetworkDebugLogger.debug("[DeltaSync] CurrentState state mismatch for CardView %d: existing=%s, data=%s (will apply properties anyway)",
                                cardView.getId(), csv.getState(), csvData.state);
                    }
                } else if (prop == TrackableProperty.AlternateState) {
                    csv = cardView.getAlternateState();
                    if (csv == null) {
                        // AlternateState doesn't exist - try to create it
                        NetworkDebugLogger.debug("[DeltaSync] Creating AlternateState for CardView %d with state=%s",
                                cardView.getId(), csvData.state);
                        csv = createCardStateView(cardView, csvData.state);
                        if (csv != null) {
                            // Set the newly created CardStateView as AlternateState
                            cardView.set(TrackableProperty.AlternateState, csv);
                        }
                    }
                } else if (prop == TrackableProperty.LeftSplitState) {
                    csv = cardView.getLeftSplitState();
                    if (csv == null) {
                        csv = createCardStateView(cardView, csvData.state);
                        if (csv != null) {
                            cardView.set(TrackableProperty.LeftSplitState, csv);
                        }
                    }
                } else if (prop == TrackableProperty.RightSplitState) {
                    csv = cardView.getRightSplitState();
                    if (csv == null) {
                        csv = createCardStateView(cardView, csvData.state);
                        if (csv != null) {
                            cardView.set(TrackableProperty.RightSplitState, csv);
                        }
                    }
                }

                if (csv != null) {
                    // Apply all deserialized properties to the CardStateView
                    int appliedCount = 0;
                    for (Map.Entry<TrackableProperty, Object> entry : csvData.properties.entrySet()) {
                        TrackableProperty csvProp = entry.getKey();
                        Object csvValue = entry.getValue();

                        // Recursively handle nested CardStateViewData (shouldn't happen, but be safe)
                        if (csvValue instanceof CardStateViewData) {
                            NetworkDebugLogger.error("[DeltaSync] Nested CardStateViewData not supported for property %s", csvProp);
                            continue;
                        }

                        csv.set(csvProp, csvValue);
                        appliedCount++;
                    }
                    NetworkDebugLogger.debug("[DeltaSync] Applied %d/%d properties to CardStateView (state=%s) of CardView %d",
                            appliedCount, csvData.properties.size(), csvData.state, cardView.getId());
                } else {
                    NetworkDebugLogger.error("[DeltaSync] Failed to get/create CardStateView for property %s on CardView %d",
                            prop, cardView.getId());
                }
                return;
            }

            // Normal property setting (direct call - much faster than reflection)
            obj.set(prop, value);
        } catch (Exception e) {
            NetworkDebugLogger.error("[DeltaSync] Error setting property %s on object %d: %s", prop, obj.getId(), e.getMessage());
            NetworkDebugLogger.error("[DeltaSync] Stack trace:", e);
        }
    }

    /**
     * Create a new CardStateView for a CardView using reflection to access the inner class constructor.
     */
    private CardStateView createCardStateView(CardView cardView, CardStateName state) {
        try {
            // Try to use the createAlternateState method if available
            java.lang.reflect.Method createMethod = CardView.class.getDeclaredMethod(
                    "createAlternateState", CardStateName.class);
            createMethod.setAccessible(true);
            return (CardStateView) createMethod.invoke(cardView, state);
        } catch (NoSuchMethodException e) {
            // Method doesn't exist, try direct constructor approach
            try {
                // CardStateView is an inner class, so we need the outer CardView instance
                Class<?> csvClass = Class.forName("forge.game.card.CardView$CardStateView");
                java.lang.reflect.Constructor<?> constructor = csvClass.getDeclaredConstructor(
                        CardView.class, int.class, CardStateName.class, Tracker.class);
                constructor.setAccessible(true);
                Tracker tracker = cardView.getTracker();
                return (CardStateView) constructor.newInstance(cardView, cardView.getId(), state, tracker);
            } catch (Exception e2) {
                NetworkDebugLogger.error("[DeltaSync] Failed to create CardStateView via constructor: %s", e2.getMessage());
                return null;
            }
        } catch (Exception e) {
            NetworkDebugLogger.error("[DeltaSync] Failed to create CardStateView: %s", e.getMessage());
            return null;
        }
    }

    @Override
    public void fullStateSync(FullStatePacket packet) {
        // Default implementation - apply the full state
        if (packet != null && packet.getGameView() != null) {
            GameView newGameView = packet.getGameView();

            // CRITICAL FIX: If gameView already exists with a tracker, we must NOT replace it
            // because the UI (sortedPlayers, CHand, etc.) holds references to the existing PlayerViews.
            // Replacing gameView would orphan those references and break the UI.
            if (gameView != null && gameView.getTracker() != null) {
                NetworkDebugLogger.log("[FullStateSync] gameView already exists - using copyChangedProps to preserve object identity");

                // Initialize tracker on the incoming gameView so copyChangedProps can work
                if (newGameView.getTracker() == null) {
                    // Use the existing tracker for the new GameView's objects
                    Tracker existingTracker = gameView.getTracker();
                    java.util.Set<Integer> visited = new java.util.HashSet<>();
                    setTrackerRecursively(newGameView, existingTracker, visited);
                }

                // Copy changed properties to existing objects (preserves object identity)
                gameView.copyChangedProps(newGameView);

                NetworkDebugLogger.log("[FullStateSync] Used copyChangedProps - existing PlayerView instances preserved");
                if (gameView.getPlayers() != null) {
                    for (PlayerView player : gameView.getPlayers()) {
                        NetworkDebugLogger.debug("[FullStateSync] Preserved Player %d: hash=%d",
                                player.getId(), System.identityHashCode(player));
                    }
                }
            } else {
                // No existing gameView - initialize fresh (this is the first sync or reconnection)
                NetworkDebugLogger.log("[FullStateSync] No existing gameView - performing fresh initialization");

                ensureTrackerInitialized(newGameView);
                newGameView.updateObjLookup();

                // Debug: Log what's in the tracker after updateObjLookup
                Tracker tracker = newGameView.getTracker();
                if (tracker != null) {
                    int cardCount = 0;
                    int playerCount = 0;
                    // Count objects by iterating through known collections
                    if (newGameView.getPlayers() != null) {
                        for (PlayerView player : newGameView.getPlayers()) {
                            playerCount++;
                            // Count cards in player zones
                            cardCount += countCards(player.getHand());
                            cardCount += countCards(player.getGraveyard());
                            cardCount += countCards(player.getLibrary());
                            cardCount += countCards(player.getExile());
                            cardCount += countCards(player.getBattlefield());
                        }
                    }
                    NetworkDebugLogger.log("[FullStateSync] After updateObjLookup: %d players, ~%d cards found in zones",
                            playerCount, cardCount);

                    // Verify a few objects are actually in the tracker
                    if (newGameView.getPlayers() != null) {
                        for (PlayerView player : newGameView.getPlayers()) {
                            TrackableObject foundPlayer = tracker.getObj(TrackableTypes.PlayerViewType, player.getId());
                            boolean sameInstance = (player == foundPlayer);
                            NetworkDebugLogger.debug("[FullStateSync] Player %d: hash=%d, trackerLookup=%s, trackerHash=%d, sameInstance=%b",
                                    player.getId(),
                                    System.identityHashCode(player),
                                    foundPlayer != null ? "FOUND" : "NOT FOUND",
                                    foundPlayer != null ? System.identityHashCode(foundPlayer) : 0,
                                    sameInstance);

                            // Check a card from hand
                            if (player.getHand() != null) {
                                for (CardView card : player.getHand()) {
                                    TrackableObject foundCard = tracker.getObj(TrackableTypes.CardViewType, card.getId());
                                    NetworkDebugLogger.debug("[FullStateSync] Card %d (from hand): tracker lookup = %s",
                                            card.getId(), foundCard != null ? "FOUND" : "NOT FOUND");
                                    break; // Just check first card
                                }
                            }
                        }
                    }
                }

                // Set the new game view
                gameView = newGameView;
            }

            // Send acknowledgment
            IGameController controller = getGameController();
            if (controller != null) {
                controller.ackSync(packet.getSequenceNumber());
            }
        }
    }

    private int countCards(Iterable<CardView> cards) {
        if (cards == null) return 0;
        int count = 0;
        for (CardView c : cards) {
            if (c != null) count++;
        }
        return count;
    }

    @Override
    public void gamePaused(String message) {
        // Default implementation - show a message and pause
        setgamePause(true);
        if (message != null && !message.isEmpty()) {
            message(message, "Game Paused");
        }
    }

    @Override
    public void gameResumed() {
        // Default implementation - resume the game
        setgamePause(false);
    }

    @Override
    public void reconnectAccepted(FullStatePacket packet) {
        // Default implementation - treat as a full state sync
        // The fullStateSync method will handle both state application and acknowledgment
        fullStateSync(packet);
    }

    @Override
    public void reconnectRejected(String reason) {
        // Default implementation - show an error
        if (reason != null && !reason.isEmpty()) {
            showErrorDialog(reason, "Reconnection Failed");
        }
    }

    @Override
    public void setRememberedActions() {
        // Default implementation - no-op for local games
    }

    @Override
    public void nextRememberedAction() {
        // Default implementation - no-op for local games
    }
}
