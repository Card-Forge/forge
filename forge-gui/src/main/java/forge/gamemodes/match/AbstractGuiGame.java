package forge.gamemodes.match;

import com.google.common.collect.*;
import forge.game.GameView;
import forge.game.card.Card;
import forge.game.card.CardView;
import forge.game.card.CardView.CardStateView;
import forge.game.event.GameEventSpellAbilityCast;
import forge.game.event.GameEventSpellRemovedFromStack;
import forge.game.player.PlayerView;
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

    // Extended yield mode tracking (experimental feature)
    private final Map<PlayerView, YieldMode> playerYieldMode = Maps.newHashMap();
    private final Map<PlayerView, Integer> yieldStartTurn = Maps.newHashMap(); // Track turn when yield was set
    private final Map<PlayerView, Integer> yieldCombatStartTurn = Maps.newHashMap(); // Track turn when combat yield was set
    private final Map<PlayerView, Boolean> yieldCombatStartedAtOrAfterCombat = Maps.newHashMap(); // Was yield set at/after combat?
    private final Map<PlayerView, Integer> yieldEndStepStartTurn = Maps.newHashMap(); // Track turn when end step yield was set
    private final Map<PlayerView, Boolean> yieldEndStepStartedAtOrAfterEndStep = Maps.newHashMap(); // Was yield set at/after end step?
    private final Map<PlayerView, Boolean> yieldYourTurnStartedDuringOurTurn = Maps.newHashMap(); // Was yield set during our turn?

    // Smart suggestion decline tracking (reset each turn)
    private final Map<PlayerView, Set<String>> declinedSuggestionsThisTurn = Maps.newHashMap();
    private final Map<PlayerView, Integer> declinedSuggestionsTurn = Maps.newHashMap();

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
        // Check legacy auto-pass first
        if (autoPassUntilEndOfTurn.contains(player)) {
            return true;
        }
        // Check experimental yield system
        return shouldAutoYieldForPlayer(player);
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
        PlayerView player = getCurrentPlayer();

        // Check legacy auto-pass first
        if (autoPassUntilEndOfTurn.contains(player)) {
            cancelAwaitNextInput();
            showPromptMessage(player, Localizer.getInstance().getMessage("lblYieldingUntilEndOfTurn"));
            updateButtons(player, false, true, false);
            return;
        }

        // Check experimental yield modes
        YieldMode mode = playerYieldMode.get(player);
        if (mode != null && mode != YieldMode.NONE) {
            cancelAwaitNextInput();
            Localizer loc = Localizer.getInstance();
            String message = switch (mode) {
                case UNTIL_STACK_CLEARS -> loc.getMessage("lblYieldingUntilStackClears");
                case UNTIL_END_OF_TURN -> loc.getMessage("lblYieldingUntilEndOfTurn");
                case UNTIL_YOUR_NEXT_TURN -> loc.getMessage("lblYieldingUntilYourNextTurn");
                case UNTIL_BEFORE_COMBAT -> loc.getMessage("lblYieldingUntilBeforeCombat");
                case UNTIL_END_STEP -> loc.getMessage("lblYieldingUntilEndStep");
                default -> "";
            };
            showPromptMessage(player, message);
            updateButtons(player, false, true, false);
        }
    }

    // Extended yield mode methods (experimental feature)
    @Override
    public final void setYieldMode(PlayerView player, final YieldMode mode) {
        player = TrackableTypes.PlayerViewType.lookup(player); // ensure we use the correct player instance
        if (!isYieldExperimentalEnabled()) {
            // Fall back to legacy behavior for UNTIL_END_OF_TURN
            if (mode == YieldMode.UNTIL_END_OF_TURN) {
                autoPassUntilEndOfTurn.add(player);
                updateAutoPassPrompt();
            }
            return;
        }

        if (mode == YieldMode.NONE) {
            clearYieldMode(player);
            return;
        }

        playerYieldMode.put(player, mode);
        // Track turn number for UNTIL_END_OF_TURN mode
        if (mode == YieldMode.UNTIL_END_OF_TURN && getGameView() != null && getGameView().getGame() != null) {
            yieldStartTurn.put(player, getGameView().getGame().getPhaseHandler().getTurn());
        }
        // Track turn and phase state for UNTIL_BEFORE_COMBAT mode
        if (mode == YieldMode.UNTIL_BEFORE_COMBAT && getGameView() != null && getGameView().getGame() != null) {
            forge.game.phase.PhaseHandler ph = getGameView().getGame().getPhaseHandler();
            yieldCombatStartTurn.put(player, ph.getTurn());
            forge.game.phase.PhaseType phase = ph.getPhase();
            boolean atOrAfterCombat = phase != null &&
                (phase == forge.game.phase.PhaseType.COMBAT_BEGIN || phase.isAfter(forge.game.phase.PhaseType.COMBAT_BEGIN));
            yieldCombatStartedAtOrAfterCombat.put(player, atOrAfterCombat);
        }
        // Track turn and phase state for UNTIL_END_STEP mode
        if (mode == YieldMode.UNTIL_END_STEP && getGameView() != null && getGameView().getGame() != null) {
            forge.game.phase.PhaseHandler ph = getGameView().getGame().getPhaseHandler();
            yieldEndStepStartTurn.put(player, ph.getTurn());
            forge.game.phase.PhaseType phase = ph.getPhase();
            boolean atOrAfterEndStep = phase != null &&
                (phase == forge.game.phase.PhaseType.END_OF_TURN || phase == forge.game.phase.PhaseType.CLEANUP);
            yieldEndStepStartedAtOrAfterEndStep.put(player, atOrAfterEndStep);
        }
        // Track if UNTIL_YOUR_NEXT_TURN was started during our turn
        if (mode == YieldMode.UNTIL_YOUR_NEXT_TURN && getGameView() != null && getGameView().getGame() != null) {
            forge.game.phase.PhaseHandler ph = getGameView().getGame().getPhaseHandler();
            forge.game.player.Player playerObj = getGameView().getGame().getPlayer(player);
            boolean isOurTurn = ph.getPlayerTurn().equals(playerObj);
            yieldYourTurnStartedDuringOurTurn.put(player, isOurTurn);
        }
        updateAutoPassPrompt();
    }

    @Override
    public final void clearYieldMode(PlayerView player) {
        player = TrackableTypes.PlayerViewType.lookup(player); // ensure we use the correct player instance
        playerYieldMode.remove(player);
        yieldStartTurn.remove(player);
        yieldCombatStartTurn.remove(player);
        yieldCombatStartedAtOrAfterCombat.remove(player);
        yieldEndStepStartTurn.remove(player);
        yieldEndStepStartedAtOrAfterEndStep.remove(player);
        yieldYourTurnStartedDuringOurTurn.remove(player);
        autoPassUntilEndOfTurn.remove(player); // Legacy compatibility

        showPromptMessage(player, "");
        updateButtons(player, false, false, false);
        awaitNextInput();
    }

    @Override
    public final YieldMode getYieldMode(PlayerView player) {
        player = TrackableTypes.PlayerViewType.lookup(player); // ensure we use the correct player instance
        // Check legacy auto-pass first
        if (autoPassUntilEndOfTurn.contains(player)) {
            return YieldMode.UNTIL_END_OF_TURN;
        }
        YieldMode mode = playerYieldMode.get(player);
        return mode != null ? mode : YieldMode.NONE;
    }

    @Override
    public final boolean shouldAutoYieldForPlayer(PlayerView player) {
        player = TrackableTypes.PlayerViewType.lookup(player); // ensure we use the correct player instance
        // Check legacy system first
        if (autoPassUntilEndOfTurn.contains(player)) {
            return true;
        }

        if (!isYieldExperimentalEnabled()) {
            return false;
        }

        YieldMode mode = playerYieldMode.get(player);
        if (mode == null || mode == YieldMode.NONE) {
            return false;
        }

        // Check interrupt conditions
        if (shouldInterruptYield(player)) {
            clearYieldMode(player);
            return false;
        }

        if (getGameView() == null || getGameView().getGame() == null) {
            return false;
        }

        forge.game.Game game = getGameView().getGame();
        forge.game.phase.PhaseHandler ph = game.getPhaseHandler();

        return switch (mode) {
            case UNTIL_STACK_CLEARS -> {
                boolean stackEmpty = game.getStack().isEmpty() && !game.getStack().hasSimultaneousStackEntries();
                if (stackEmpty) {
                    clearYieldMode(player);
                    yield false;
                }
                yield true;
            }
            case UNTIL_END_OF_TURN -> {
                // Yield until end of the turn when yield was set - clear when turn number changes
                Integer startTurn = yieldStartTurn.get(player);
                int currentTurn = ph.getTurn();
                if (startTurn == null) {
                    // Turn wasn't tracked when yield was set - track it now
                    yieldStartTurn.put(player, currentTurn);
                    yield true;
                }
                if (currentTurn > startTurn) {
                    clearYieldMode(player);
                    yield false;
                }
                yield true;
            }
            case UNTIL_YOUR_NEXT_TURN -> {
                // Yield until our turn starts
                forge.game.player.Player playerObj = game.getPlayer(player);
                boolean isOurTurn = ph.getPlayerTurn().equals(playerObj);
                Boolean startedDuringOurTurn = yieldYourTurnStartedDuringOurTurn.get(player);

                if (startedDuringOurTurn == null) {
                    // Tracking wasn't set - initialize it now
                    yieldYourTurnStartedDuringOurTurn.put(player, isOurTurn);
                    startedDuringOurTurn = isOurTurn;
                }

                if (isOurTurn) {
                    // If we started during our turn, we need to wait until it's our turn AGAIN
                    // (i.e., we left our turn and came back)
                    // If we started during opponent's turn, stop when we reach our turn
                    if (!Boolean.TRUE.equals(startedDuringOurTurn)) {
                        clearYieldMode(player);
                        yield false;
                    }
                } else {
                    // Not our turn - if we started during our turn, mark that we've left it
                    if (Boolean.TRUE.equals(startedDuringOurTurn)) {
                        // We've left our turn, now waiting for it to come back
                        yieldYourTurnStartedDuringOurTurn.put(player, false);
                    }
                }
                yield true;
            }
            case UNTIL_BEFORE_COMBAT -> {
                forge.game.phase.PhaseType phase = ph.getPhase();
                Integer startTurn = yieldCombatStartTurn.get(player);
                Boolean startedAtOrAfterCombat = yieldCombatStartedAtOrAfterCombat.get(player);
                int currentTurn = ph.getTurn();

                if (startTurn == null) {
                    // Tracking wasn't set - initialize it now
                    yieldCombatStartTurn.put(player, currentTurn);
                    boolean atOrAfterCombat = phase != null &&
                        (phase == forge.game.phase.PhaseType.COMBAT_BEGIN || phase.isAfter(forge.game.phase.PhaseType.COMBAT_BEGIN));
                    yieldCombatStartedAtOrAfterCombat.put(player, atOrAfterCombat);
                    startTurn = currentTurn;
                    startedAtOrAfterCombat = atOrAfterCombat;
                }

                // Check if we should stop: we're at or past combat on a DIFFERENT turn than when we started,
                // OR we're at combat on the SAME turn but we started BEFORE combat
                boolean atOrAfterCombatNow = phase != null &&
                    (phase == forge.game.phase.PhaseType.COMBAT_BEGIN || phase.isAfter(forge.game.phase.PhaseType.COMBAT_BEGIN));

                if (atOrAfterCombatNow) {
                    boolean differentTurn = currentTurn > startTurn;
                    boolean sameTurnButStartedBeforeCombat = (currentTurn == startTurn.intValue()) && !Boolean.TRUE.equals(startedAtOrAfterCombat);

                    if (differentTurn || sameTurnButStartedBeforeCombat) {
                        clearYieldMode(player);
                        yield false;
                    }
                }
                yield true;
            }
            case UNTIL_END_STEP -> {
                forge.game.phase.PhaseType phase = ph.getPhase();
                Integer startTurn = yieldEndStepStartTurn.get(player);
                Boolean startedAtOrAfterEndStep = yieldEndStepStartedAtOrAfterEndStep.get(player);
                int currentTurn = ph.getTurn();

                if (startTurn == null) {
                    // Tracking wasn't set - initialize it now
                    yieldEndStepStartTurn.put(player, currentTurn);
                    boolean atOrAfterEndStep = phase != null &&
                        (phase == forge.game.phase.PhaseType.END_OF_TURN || phase == forge.game.phase.PhaseType.CLEANUP);
                    yieldEndStepStartedAtOrAfterEndStep.put(player, atOrAfterEndStep);
                    startTurn = currentTurn;
                    startedAtOrAfterEndStep = atOrAfterEndStep;
                }

                // Check if we should stop: we're at or past end step on a DIFFERENT turn than when we started,
                // OR we're at end step on the SAME turn but we started BEFORE end step
                boolean atOrAfterEndStepNow = phase != null &&
                    (phase == forge.game.phase.PhaseType.END_OF_TURN || phase == forge.game.phase.PhaseType.CLEANUP);

                if (atOrAfterEndStepNow) {
                    boolean differentTurn = currentTurn > startTurn;
                    boolean sameTurnButStartedBeforeEndStep = (currentTurn == startTurn.intValue()) && !Boolean.TRUE.equals(startedAtOrAfterEndStep);

                    if (differentTurn || sameTurnButStartedBeforeEndStep) {
                        clearYieldMode(player);
                        yield false;
                    }
                }
                yield true;
            }
            default -> false;
        };
    }

    private boolean shouldInterruptYield(final PlayerView player) {
        if (getGameView() == null || getGameView().getGame() == null) {
            return false;
        }

        forge.game.Game game = getGameView().getGame();
        forge.game.player.Player p = game.getPlayer(player);
        if (p == null) {
            return false; // Can't determine player, don't interrupt
        }
        ForgePreferences prefs = FModel.getPreferences();
        forge.game.phase.PhaseType phase = game.getPhaseHandler().getPhase();

        if (prefs.getPrefBoolean(ForgePreferences.FPref.YIELD_INTERRUPT_ON_ATTACKERS)) {
            // Only interrupt if there are creatures attacking THIS player or their planeswalkers/battles
            if (phase == forge.game.phase.PhaseType.COMBAT_DECLARE_ATTACKERS &&
                game.getCombat() != null && isBeingAttacked(game, p)) {
                return true;
            }
        }

        if (prefs.getPrefBoolean(ForgePreferences.FPref.YIELD_INTERRUPT_ON_BLOCKERS)) {
            // Only interrupt if there are creatures attacking THIS player or their planeswalkers/battles
            if (phase == forge.game.phase.PhaseType.COMBAT_DECLARE_BLOCKERS &&
                game.getCombat() != null && isBeingAttacked(game, p)) {
                return true;
            }
        }

        if (prefs.getPrefBoolean(ForgePreferences.FPref.YIELD_INTERRUPT_ON_TARGETING)) {
            for (forge.game.spellability.StackItemView si : getGameView().getStack()) {
                if (targetsPlayerOrPermanents(si, p)) {
                    return true;
                }
            }
        }

        if (prefs.getPrefBoolean(ForgePreferences.FPref.YIELD_INTERRUPT_ON_OPPONENT_SPELL)) {
            if (!game.getStack().isEmpty()) {
                forge.game.spellability.SpellAbility topSa = game.getStack().peekAbility();
                // Exclude triggered abilities - if they target you, the "targeting" setting handles that
                if (topSa != null && !topSa.isTrigger() && !topSa.getActivatingPlayer().equals(p)) {
                    return true;
                }
            }
        }

        if (prefs.getPrefBoolean(ForgePreferences.FPref.YIELD_INTERRUPT_ON_COMBAT)) {
            if (phase == forge.game.phase.PhaseType.COMBAT_BEGIN) {
                YieldMode mode = playerYieldMode.get(player);
                // Don't interrupt UNTIL_END_OF_TURN on our own turn
                if (!(mode == YieldMode.UNTIL_END_OF_TURN && game.getPhaseHandler().getPlayerTurn().equals(p))) {
                    return true;
                }
            }
        }

        if (prefs.getPrefBoolean(ForgePreferences.FPref.YIELD_INTERRUPT_ON_MASS_REMOVAL)) {
            if (hasMassRemovalOnStack(game, p)) {
                return true;
            }
        }

        return false;
    }

    private boolean isBeingAttacked(forge.game.Game game, forge.game.player.Player p) {
        forge.game.combat.Combat combat = game.getCombat();
        if (combat == null) {
            return false;
        }

        // Check if player is being attacked directly
        if (!combat.getAttackersOf(p).isEmpty()) {
            return true;
        }

        // Check if any planeswalkers or battles controlled by the player are being attacked
        for (forge.game.GameEntity defender : combat.getDefenders()) {
            if (defender instanceof forge.game.card.Card) {
                forge.game.card.Card card = (forge.game.card.Card) defender;
                if (card.getController().equals(p) && !combat.getAttackersOf(defender).isEmpty()) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean targetsPlayerOrPermanents(forge.game.spellability.StackItemView si, forge.game.player.Player p) {
        PlayerView pv = p.getView();

        for (PlayerView target : si.getTargetPlayers()) {
            if (target.equals(pv)) return true;
        }

        for (CardView target : si.getTargetCards()) {
            if (target.getController() != null && target.getController().equals(pv)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if there's a mass removal spell on the stack that could affect the player's permanents.
     * Only interrupts if the spell was cast by an opponent AND the player has permanents that match.
     */
    private boolean hasMassRemovalOnStack(forge.game.Game game, forge.game.player.Player p) {
        if (game.getStack().isEmpty()) {
            return false;
        }

        for (forge.game.spellability.SpellAbilityStackInstance si : game.getStack()) {
            forge.game.spellability.SpellAbility sa = si.getSpellAbility();
            if (sa == null) continue;

            // Only interrupt for opponent's spells
            if (sa.getActivatingPlayer() == null || sa.getActivatingPlayer().equals(p)) {
                continue;
            }

            // Check if this is a mass removal spell type
            if (isMassRemovalSpell(sa, game, p)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determine if a spell ability is a mass removal effect that could affect the player.
     */
    private boolean isMassRemovalSpell(forge.game.spellability.SpellAbility sa, forge.game.Game game, forge.game.player.Player p) {
        forge.game.ability.ApiType api = sa.getApi();
        if (api == null) {
            return false;
        }

        // Check the main ability and all sub-abilities (for modal spells like Farewell)
        forge.game.spellability.SpellAbility current = sa;
        while (current != null) {
            if (checkSingleAbilityForMassRemoval(current, game, p)) {
                return true;
            }
            current = current.getSubAbility();
        }

        return false;
    }

    /**
     * Check if a single ability (not including sub-abilities) is mass removal affecting the player.
     */
    private boolean checkSingleAbilityForMassRemoval(forge.game.spellability.SpellAbility sa, forge.game.Game game, forge.game.player.Player p) {
        forge.game.ability.ApiType api = sa.getApi();
        if (api == null) {
            return false;
        }

        String apiName = api.name();

        // DestroyAll - Wrath of God, Day of Judgment, Damnation
        if ("DestroyAll".equals(apiName)) {
            return playerHasMatchingPermanents(sa, game, p, "ValidCards");
        }

        // ChangeZoneAll with Destination=Exile or Graveyard - Farewell, Merciless Eviction
        if ("ChangeZoneAll".equals(apiName)) {
            String destination = sa.getParam("Destination");
            if ("Exile".equals(destination) || "Graveyard".equals(destination)) {
                // Check Origin - only care about Battlefield
                String origin = sa.getParam("Origin");
                if (origin != null && origin.contains("Battlefield")) {
                    return playerHasMatchingPermanents(sa, game, p, "ChangeType");
                }
            }
        }

        // DamageAll - Blasphemous Act, Chain Reaction
        if ("DamageAll".equals(apiName)) {
            return playerHasMatchingPermanents(sa, game, p, "ValidCards");
        }

        // SacrificeAll - All Is Dust, Bane of Progress
        if ("SacrificeAll".equals(apiName)) {
            return playerHasMatchingPermanents(sa, game, p, "ValidCards");
        }

        return false;
    }

    /**
     * Check if the player has any permanents that match the spell's filter parameter.
     */
    private boolean playerHasMatchingPermanents(forge.game.spellability.SpellAbility sa, forge.game.Game game, forge.game.player.Player p, String filterParam) {
        String validFilter = sa.getParam(filterParam);

        // Get all permanents controlled by the player
        forge.game.card.CardCollectionView playerPermanents = p.getCardsIn(forge.game.zone.ZoneType.Battlefield);
        if (playerPermanents.isEmpty()) {
            return false;  // No permanents = no reason to interrupt
        }

        // If no filter specified, assume it affects all permanents
        if (validFilter == null || validFilter.isEmpty()) {
            return true;
        }

        // Check if any of the player's permanents match the filter
        for (forge.game.card.Card card : playerPermanents) {
            try {
                if (card.isValid(validFilter.split(","), sa.getActivatingPlayer(), sa.getHostCard(), sa)) {
                    return true;
                }
            } catch (Exception e) {
                // If validation fails, be conservative and assume it might affect us
                return true;
            }
        }

        return false;
    }

    private boolean isYieldExperimentalEnabled() {
        return FModel.getPreferences().getPrefBoolean(ForgePreferences.FPref.YIELD_EXPERIMENTAL_OPTIONS);
    }

    @Override
    public int getPlayerCount() {
        return getGameView() != null && getGameView().getGame() != null
            ? getGameView().getGame().getPlayers().size()
            : 0;
    }

    @Override
    public void declineSuggestion(PlayerView player, String suggestionType) {
        player = TrackableTypes.PlayerViewType.lookup(player); // ensure we use the correct player instance
        if (getGameView() == null || getGameView().getGame() == null) return;

        int currentTurn = getGameView().getGame().getPhaseHandler().getTurn();
        Integer storedTurn = declinedSuggestionsTurn.get(player);

        // Reset if turn changed
        if (storedTurn == null || storedTurn != currentTurn) {
            declinedSuggestionsThisTurn.put(player, Sets.newHashSet());
            declinedSuggestionsTurn.put(player, currentTurn);
        }

        declinedSuggestionsThisTurn.get(player).add(suggestionType);
    }

    @Override
    public boolean isSuggestionDeclined(PlayerView player, String suggestionType) {
        player = TrackableTypes.PlayerViewType.lookup(player); // ensure we use the correct player instance
        if (getGameView() == null || getGameView().getGame() == null) return false;

        int currentTurn = getGameView().getGame().getPhaseHandler().getTurn();
        Integer storedTurn = declinedSuggestionsTurn.get(player);

        if (storedTurn == null || storedTurn != currentTurn) {
            return false; // Turn changed, reset
        }

        Set<String> declined = declinedSuggestionsThisTurn.get(player);
        return declined != null && declined.contains(suggestionType);
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
}
