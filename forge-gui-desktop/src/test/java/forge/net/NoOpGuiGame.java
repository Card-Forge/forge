package forge.net;

import forge.LobbyPlayer;
import forge.ai.GameState;
import forge.deck.CardPool;
import forge.game.GameEntityView;
import forge.game.GameView;
import forge.game.card.Card;
import forge.game.card.CardView;
import forge.game.event.GameEventSpellAbilityCast;
import forge.game.event.GameEventSpellRemovedFromStack;
import forge.game.phase.PhaseType;
import forge.game.player.DelayedReveal;
import forge.game.player.IHasIcon;
import forge.game.player.PlayerView;
import forge.game.spellability.SpellAbilityView;
import forge.game.zone.ZoneType;
import forge.gamemodes.net.DeltaPacket;
import forge.gamemodes.net.FullStatePacket;
import forge.gui.control.PlaybackSpeed;
import forge.gui.interfaces.IGuiGame;
import forge.interfaces.IGameController;
import forge.item.PaperCard;
import forge.localinstance.skin.FSkinProp;
import forge.player.PlayerZoneUpdate;
import forge.player.PlayerZoneUpdates;
import forge.trackable.TrackableCollection;
import forge.util.FSerializableFunction;
import forge.util.ITriggerEvent;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A no-op implementation of IGuiGame for headless testing.
 * All methods either no-op or return safe defaults.
 * This class is designed for AI-vs-AI games where no real GUI is needed.
 *
 * Part of the Headless Full Game Testing infrastructure.
 */
public class NoOpGuiGame implements IGuiGame {

    private GameView gameView;
    private boolean gamePaused = false;
    private PlaybackSpeed gameSpeed = PlaybackSpeed.NORMAL;
    private String dayTime = null;

    // Controllers - used by setOriginalGameController/setGameController for interface compliance
    // and to support potential future features like controller inspection
    private final Map<PlayerView, IGameController> gameControllers = new HashMap<>();
    private IGameController spectator;

    /**
     * Get the game controller for a player (for testing/debugging).
     * @param player the player view
     * @return the associated controller, or null if not set
     */
    public IGameController getGameController(PlayerView player) {
        return gameControllers.get(player);
    }

    // Auto-yield tracking (for interface compliance)
    private final java.util.Set<String> autoYields = new java.util.HashSet<>();
    private final Map<Integer, Boolean> triggersAlwaysAccept = new HashMap<>();

    @Override
    public void setGameView(GameView gameView) {
        this.gameView = gameView;
    }

    @Override
    public GameView getGameView() {
        return gameView;
    }

    @Override
    public void setOriginalGameController(PlayerView view, IGameController gameController) {
        if (view != null && gameController != null) {
            gameControllers.put(view, gameController);
        }
    }

    @Override
    public void setGameController(PlayerView player, IGameController gameController) {
        if (player != null) {
            if (gameController != null) {
                gameControllers.put(player, gameController);
            } else {
                gameControllers.remove(player);
            }
        }
    }

    @Override
    public void setSpectator(IGameController spectator) {
        this.spectator = spectator;
    }

    @Override
    public void openView(TrackableCollection<PlayerView> myPlayers) {
        // No-op - no GUI to open
    }

    @Override
    public void afterGameEnd() {
        // No-op - no cleanup needed
    }

    @Override
    public void showCombat() {
        // No-op
    }

    @Override
    public void showPromptMessage(PlayerView playerView, String message) {
        // No-op - could log if verbose
    }

    @Override
    public void showCardPromptMessage(PlayerView playerView, String message, CardView card) {
        // No-op
    }

    @Override
    public void updateButtons(PlayerView owner, boolean okEnabled, boolean cancelEnabled, boolean focusOk) {
        // No-op
    }

    @Override
    public void updateButtons(PlayerView owner, String label1, String label2, boolean enable1, boolean enable2, boolean focus1) {
        // No-op
    }

    @Override
    public void flashIncorrectAction() {
        // No-op
    }

    @Override
    public void alertUser() {
        // No-op
    }

    @Override
    public void updatePhase(boolean saveState) {
        // No-op
    }

    @Override
    public void updateTurn(PlayerView player) {
        // No-op
    }

    @Override
    public void updatePlayerControl() {
        // No-op
    }

    @Override
    public void enableOverlay() {
        // No-op
    }

    @Override
    public void disableOverlay() {
        // No-op
    }

    @Override
    public void finishGame() {
        // No-op
    }

    @Override
    public void showManaPool(PlayerView player) {
        // No-op
    }

    @Override
    public void hideManaPool(PlayerView player) {
        // No-op
    }

    @Override
    public void updateStack() {
        // No-op
    }

    @Override
    public void notifyStackAddition(GameEventSpellAbilityCast event) {
        // No-op
    }

    @Override
    public void notifyStackRemoval(GameEventSpellRemovedFromStack event) {
        // No-op
    }

    @Override
    public void handleLandPlayed(Card land) {
        // No-op
    }

    @Override
    public Iterable<PlayerZoneUpdate> tempShowZones(PlayerView controller, Iterable<PlayerZoneUpdate> zonesToUpdate) {
        return zonesToUpdate;
    }

    @Override
    public void hideZones(PlayerView controller, Iterable<PlayerZoneUpdate> zonesToUpdate) {
        // No-op
    }

    @Override
    public void updateZones(Iterable<PlayerZoneUpdate> zonesToUpdate) {
        // No-op
    }

    @Override
    public void updateSingleCard(CardView card) {
        // No-op
    }

    @Override
    public void updateCards(Iterable<CardView> cards) {
        // No-op
    }

    @Override
    public void updateRevealedCards(TrackableCollection<CardView> collection) {
        // No-op
    }

    @Override
    public void refreshCardDetails(Iterable<CardView> cards) {
        // No-op
    }

    @Override
    public void refreshField() {
        // No-op
    }

    @Override
    public GameState getGamestate() {
        return null;
    }

    @Override
    public void updateManaPool(Iterable<PlayerView> manaPoolUpdate) {
        // No-op
    }

    @Override
    public void updateLives(Iterable<PlayerView> livesUpdate) {
        // No-op
    }

    @Override
    public void updateShards(Iterable<PlayerView> shardsUpdate) {
        // No-op
    }

    @Override
    public void updateDependencies() {
        // No-op
    }

    @Override
    public void setPanelSelection(CardView hostCard) {
        // No-op
    }

    @Override
    public SpellAbilityView getAbilityToPlay(CardView hostCard, List<SpellAbilityView> abilities, ITriggerEvent triggerEvent) {
        // Return first ability by default (AI would handle this)
        return abilities != null && !abilities.isEmpty() ? abilities.get(0) : null;
    }

    @Override
    public Map<CardView, Integer> assignCombatDamage(CardView attacker, List<CardView> blockers, int damage, GameEntityView defender, boolean overrideOrder, boolean maySkip) {
        // Return default damage assignment
        Map<CardView, Integer> result = new HashMap<>();
        if (blockers != null && !blockers.isEmpty()) {
            result.put(blockers.get(0), damage);
        }
        return result;
    }

    @Override
    public Map<Object, Integer> assignGenericAmount(CardView effectSource, Map<Object, Integer> target, int amount, boolean atLeastOne, String amountLabel) {
        // Return the input as-is
        return target;
    }

    @Override
    public void message(String message) {
        // No-op - could log if verbose
    }

    @Override
    public void message(String message, String title) {
        // No-op
    }

    @Override
    public void showErrorDialog(String message) {
        System.err.println("[NoOpGuiGame] Error: " + message);
    }

    @Override
    public void showErrorDialog(String message, String title) {
        System.err.println("[NoOpGuiGame] " + title + ": " + message);
    }

    @Override
    public boolean showConfirmDialog(String message, String title) {
        return false; // Default to "No"
    }

    @Override
    public boolean showConfirmDialog(String message, String title, boolean defaultYes) {
        return defaultYes;
    }

    @Override
    public boolean showConfirmDialog(String message, String title, String yesButtonText, String noButtonText) {
        return false;
    }

    @Override
    public boolean showConfirmDialog(String message, String title, String yesButtonText, String noButtonText, boolean defaultYes) {
        return defaultYes;
    }

    @Override
    public int showOptionDialog(String message, String title, FSkinProp icon, List<String> options, int defaultOption) {
        return defaultOption;
    }

    @Override
    public String showInputDialog(String message, String title, boolean isNumeric) {
        return isNumeric ? "0" : "";
    }

    @Override
    public String showInputDialog(String message, String title, FSkinProp icon) {
        return "";
    }

    @Override
    public String showInputDialog(String message, String title, FSkinProp icon, String initialInput) {
        return initialInput;
    }

    @Override
    public String showInputDialog(String message, String title, FSkinProp icon, String initialInput, List<String> inputOptions, boolean isNumeric) {
        return initialInput != null ? initialInput : (inputOptions != null && !inputOptions.isEmpty() ? inputOptions.get(0) : "");
    }

    @Override
    public boolean confirm(CardView c, String question) {
        return false;
    }

    @Override
    public boolean confirm(CardView c, String question, List<String> options) {
        return false;
    }

    @Override
    public boolean confirm(CardView c, String question, boolean defaultIsYes, List<String> options) {
        return defaultIsYes;
    }

    @Override
    public <T> List<T> getChoices(String message, int min, int max, List<T> choices) {
        return getChoices(message, min, max, choices, null, null);
    }

    @Override
    public <T> List<T> getChoices(String message, int min, int max, List<T> choices, List<T> selected, FSerializableFunction<T, String> display) {
        // Return minimum required choices from the start of the list
        if (choices == null || choices.isEmpty()) {
            return Collections.emptyList();
        }
        int actualMin = Math.max(0, min);
        int count = Math.min(actualMin, choices.size());
        if (count == 0 && min <= 0) {
            return Collections.emptyList();
        }
        return choices.subList(0, Math.max(count, 1));
    }

    @Override
    public Integer getInteger(String message, int min) {
        return min;
    }

    @Override
    public Integer getInteger(String message, int min, int max) {
        return min;
    }

    @Override
    public Integer getInteger(String message, int min, int max, boolean sortDesc) {
        return min;
    }

    @Override
    public Integer getInteger(String message, int min, int max, int cutoff) {
        return min;
    }

    @Override
    public <T> T oneOrNone(String message, List<T> choices) {
        return choices != null && !choices.isEmpty() ? choices.get(0) : null;
    }

    @Override
    public <T> T one(String message, List<T> choices) {
        return choices != null && !choices.isEmpty() ? choices.get(0) : null;
    }

    @Override
    public <T> T one(String message, List<T> choices, FSerializableFunction<T, String> display) {
        return choices != null && !choices.isEmpty() ? choices.get(0) : null;
    }

    @Override
    public <T> void reveal(String message, List<T> items) {
        // No-op
    }

    @Override
    public <T> List<T> many(String title, String topCaption, int cnt, List<T> sourceChoices, CardView c) {
        return many(title, topCaption, cnt, cnt, sourceChoices, c);
    }

    @Override
    public <T> List<T> many(String title, String topCaption, int min, int max, List<T> sourceChoices, CardView c) {
        return many(title, topCaption, min, max, sourceChoices, null, c);
    }

    @Override
    public <T> List<T> many(String title, String topCaption, int min, int max, List<T> sourceChoices, List<T> destChoices, CardView c) {
        if (sourceChoices == null || sourceChoices.isEmpty()) {
            return Collections.emptyList();
        }
        int count = Math.min(Math.max(min, 0), sourceChoices.size());
        return sourceChoices.subList(0, count);
    }

    @Override
    public <T> List<T> order(String title, String top, List<T> sourceChoices, CardView c) {
        return sourceChoices != null ? sourceChoices : Collections.emptyList();
    }

    @Override
    public <T> List<T> order(String title, String top, int remainingObjectsMin, int remainingObjectsMax, List<T> sourceChoices, List<T> destChoices, CardView referenceCard, boolean sideboardingMode) {
        return sourceChoices != null ? sourceChoices : Collections.emptyList();
    }

    @Override
    public <T> List<T> insertInList(String title, T newItem, List<T> oldItems) {
        java.util.List<T> result = new java.util.ArrayList<>(oldItems);
        result.add(0, newItem);
        return result;
    }

    @Override
    public List<PaperCard> sideboard(CardPool sideboard, CardPool main, String message) {
        return Collections.emptyList();
    }

    @Override
    public GameEntityView chooseSingleEntityForEffect(String title, List<? extends GameEntityView> optionList, DelayedReveal delayedReveal, boolean isOptional) {
        if (optionList == null || optionList.isEmpty()) {
            return null;
        }
        return isOptional ? null : optionList.get(0);
    }

    @Override
    public List<GameEntityView> chooseEntitiesForEffect(String title, List<? extends GameEntityView> optionList, int min, int max, DelayedReveal delayedReveal) {
        if (optionList == null || optionList.isEmpty()) {
            return Collections.emptyList();
        }
        int count = Math.min(min, optionList.size());
        return new java.util.ArrayList<>(optionList.subList(0, count));
    }

    @Override
    public List<CardView> manipulateCardList(String title, Iterable<CardView> cards, Iterable<CardView> manipulable, boolean toTop, boolean toBottom, boolean toAnywhere) {
        java.util.List<CardView> result = new java.util.ArrayList<>();
        if (cards != null) {
            for (CardView card : cards) {
                result.add(card);
            }
        }
        return result;
    }

    @Override
    public void setCard(CardView card) {
        // No-op
    }

    @Override
    public void setPlayerAvatar(LobbyPlayer player, IHasIcon ihi) {
        // No-op
    }

    @Override
    public PlayerZoneUpdates openZones(PlayerView controller, Collection<ZoneType> zones, Map<PlayerView, Object> players, boolean backupLastZones) {
        return null;
    }

    @Override
    public void restoreOldZones(PlayerView playerView, PlayerZoneUpdates playerZoneUpdates) {
        // No-op
    }

    @Override
    public void setHighlighted(PlayerView pv, boolean b) {
        // No-op
    }

    @Override
    public void setUsedToPay(CardView card, boolean value) {
        // No-op
    }

    @Override
    public void setSelectables(Iterable<CardView> cards) {
        // No-op
    }

    @Override
    public void clearSelectables() {
        // No-op
    }

    @Override
    public boolean isSelecting() {
        return false;
    }

    @Override
    public boolean isGamePaused() {
        return gamePaused;
    }

    @Override
    public void setgamePause(boolean pause) {
        this.gamePaused = pause;
    }

    @Override
    public void setGameSpeed(PlaybackSpeed gameSpeed) {
        this.gameSpeed = gameSpeed;
    }

    @Override
    public String getDayTime() {
        return dayTime;
    }

    @Override
    public void updateDayTime(String daytime) {
        this.dayTime = daytime;
    }

    @Override
    public void awaitNextInput() {
        // No-op
    }

    @Override
    public void cancelAwaitNextInput() {
        // No-op
    }

    @Override
    public boolean isUiSetToSkipPhase(PlayerView playerTurn, PhaseType phase) {
        return false;
    }

    @Override
    public void autoPassUntilEndOfTurn(PlayerView player) {
        // No-op
    }

    @Override
    public boolean mayAutoPass(PlayerView player) {
        return false;
    }

    @Override
    public void autoPassCancel(PlayerView player) {
        // No-op
    }

    @Override
    public void updateAutoPassPrompt() {
        // No-op
    }

    @Override
    public boolean shouldAutoYield(String key) {
        return autoYields.contains(key);
    }

    @Override
    public void setShouldAutoYield(String key, boolean autoYield) {
        if (autoYield) {
            autoYields.add(key);
        } else {
            autoYields.remove(key);
        }
    }

    @Override
    public boolean shouldAlwaysAcceptTrigger(int trigger) {
        return Boolean.TRUE.equals(triggersAlwaysAccept.get(trigger));
    }

    @Override
    public boolean shouldAlwaysDeclineTrigger(int trigger) {
        return Boolean.FALSE.equals(triggersAlwaysAccept.get(trigger));
    }

    @Override
    public void setShouldAlwaysAcceptTrigger(int trigger) {
        triggersAlwaysAccept.put(trigger, Boolean.TRUE);
    }

    @Override
    public void setShouldAlwaysDeclineTrigger(int trigger) {
        triggersAlwaysAccept.put(trigger, Boolean.FALSE);
    }

    @Override
    public void setShouldAlwaysAskTrigger(int trigger) {
        triggersAlwaysAccept.remove(trigger);
    }

    @Override
    public void clearAutoYields() {
        autoYields.clear();
        triggersAlwaysAccept.clear();
    }

    @Override
    public void setCurrentPlayer(PlayerView player) {
        // No-op - no current player tracking needed for headless
    }

    // Delta sync and reconnection methods - all no-op for headless testing

    @Override
    public void applyDelta(DeltaPacket packet) {
        // No-op - delta sync testing happens on server side
    }

    @Override
    public void fullStateSync(FullStatePacket packet) {
        // No-op
    }

    @Override
    public void gamePaused(String message) {
        setgamePause(true);
    }

    @Override
    public void gameResumed() {
        setgamePause(false);
    }

    @Override
    public void reconnectAccepted(FullStatePacket packet) {
        // No-op
    }

    @Override
    public void reconnectRejected(String reason) {
        System.err.println("[NoOpGuiGame] Reconnect rejected: " + reason);
    }

    @Override
    public void setRememberedActions() {
        // No-op
    }

    @Override
    public void nextRememberedAction() {
        // No-op
    }
}
