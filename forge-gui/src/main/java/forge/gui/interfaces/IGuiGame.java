package forge.gui.interfaces;

import forge.LobbyPlayer;
import forge.ai.GameState;
import forge.deck.CardPool;
import forge.game.GameEntityView;
import forge.game.GameView;
import forge.game.card.CardView;
import forge.game.event.GameEvent;
import forge.game.event.GameEventSpellAbilityCast;
import forge.game.event.GameEventSpellRemovedFromStack;
import forge.game.phase.PhaseType;
import forge.game.player.DelayedReveal;
import forge.game.player.IHasIcon;
import forge.game.player.PlayerView;
import forge.game.spellability.SpellAbilityView;
import forge.game.zone.ZoneType;
import forge.gamemodes.match.YieldUpdate;
import forge.gamemodes.match.input.InputConfirm;
import forge.gamemodes.net.DeltaPacket;
import forge.gui.control.PlaybackSpeed;
import forge.interfaces.IGameController;
import forge.item.PaperCard;
import forge.localinstance.skin.FSkinProp;
import forge.player.PlayerZoneUpdate;
import forge.player.PlayerZoneUpdates;
import forge.trackable.TrackableCollection;
import forge.util.FSerializableFunction;
import forge.util.ITriggerEvent;
import forge.util.Localizer;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface IGuiGame {
    void setGameView(GameView gameView);

    /**
     * Set the game view with a sequence number for delta sync baseline.
     * Local games ignore the sequence number.
     */
    default void setGameView(GameView gameView, long sequenceNumber) {
        setGameView(gameView);
    }
    GameView getGameView();

    void setOriginalGameController(PlayerView view, IGameController gameController);
    void setGameController(PlayerView player, IGameController gameController);

    void setSpectator(IGameController spectator);

    void openView(TrackableCollection<PlayerView> myPlayers);

    void afterGameEnd();

    void showCombat();

    void showPromptMessage(PlayerView playerView, String message);

    void showCardPromptMessage(PlayerView playerView, String message, CardView card);

    default void updateButtons(final PlayerView owner, final boolean okEnabled, final boolean cancelEnabled, final boolean focusOk) {
        updateButtons(owner, Localizer.getInstance().getMessage("lblOK"), Localizer.getInstance().getMessage("lblCancel"), okEnabled, cancelEnabled, focusOk);
    }
    void updateButtons(PlayerView owner, String label1, String label2, boolean enable1, boolean enable2, boolean focus1);

    void flashIncorrectAction();

    void alertUser();

    void updatePhase(boolean saveState);

    void updateTurn(PlayerView player);

    void updatePlayerControl();

    void enableOverlay();
    void disableOverlay();

    void finishGame();

    void showManaPool(PlayerView player);
    void hideManaPool(PlayerView player);

    void updateStack();

    void notifyStackAddition(final GameEventSpellAbilityCast event);
    void notifyStackRemoval(final GameEventSpellRemovedFromStack event);

    void handleLandPlayed(CardView land);

    void handleGameEvent(GameEvent event);
    default void handleGameEvents(List<GameEvent> events) {
        for (GameEvent event : events) {
            handleGameEvent(event);
        }
    }

    Iterable<PlayerZoneUpdate> tempShowZones(PlayerView controller, Iterable<PlayerZoneUpdate> zonesToUpdate);

    void hideZones(PlayerView controller, Iterable<PlayerZoneUpdate> zonesToUpdate);

    void updateZones(Iterable<PlayerZoneUpdate> zonesToUpdate);

    void updateSingleCard(CardView card);

    void updateCards(Iterable<CardView> cards);

    void updateRevealedCards(TrackableCollection<CardView> collection);

    void refreshCardDetails(Iterable<CardView> cards);

    void refreshField();

    GameState getGamestate();

    void updateManaPool(Iterable<PlayerView> manaPoolUpdate);

    void updateLives(Iterable<PlayerView> livesUpdate);
    void updateShards(Iterable<PlayerView> shardsUpdate);

    void updateDependencies();

    void setPanelSelection(CardView hostCard);

    SpellAbilityView getAbilityToPlay(CardView hostCard, List<SpellAbilityView> abilities, ITriggerEvent triggerEvent);

    Map<CardView, Integer> assignCombatDamage(CardView attacker, List<CardView> blockers, int damage, GameEntityView defender, boolean overrideOrder, boolean maySkip);

    // The Object passed should be GameEntityView for most case. Can be Byte for "generate mana of any combination" effect
    Map<Object, Integer> assignGenericAmount(CardView effectSource, Map<Object, Integer> target, int amount, final boolean atLeastOne, final String amountLabel);

    default void message(final String message) {
        message(message, "Forge");
    }
    void message(String message, String title);

    default void showErrorDialog(final String message) {
        showErrorDialog(message, "Error");
    }
    void showErrorDialog(String message, String title);

    default boolean showConfirmDialog(final String message, final String title) {
        return showConfirmDialog(message, title, InputConfirm.defaultOptions.get(0), InputConfirm.defaultOptions.get(1));
    }
    default boolean showConfirmDialog(final String message, final String title, final String yesButtonText, final String noButtonText) {
        return showConfirmDialog(message, title, yesButtonText, noButtonText, true);
    }
    boolean showConfirmDialog(String message, String title, String yesButtonText, String noButtonText, boolean defaultYes);

    int showOptionDialog(String message, String title, FSkinProp icon, List<String> options, int defaultOption);

    String showInputDialog(String message, String title, FSkinProp icon, String initialInput, List<String> inputOptions, boolean isNumeric);

    default boolean confirm(final CardView c, final String question) {
        return confirm(c, question, true, InputConfirm.defaultOptions);
    }
    boolean confirm(CardView c, String question, boolean defaultIsYes, List<String> options);

    // returned Object will never be null
    default <T> List<T> getChoices(final String message, final int min, final int max, final List<T> choices) {
        return getChoices(message, min, max, choices, null, null);
    }
    <T> List<T> getChoices(String message, int min, int max, List<T> choices, List<T> selected, FSerializableFunction<T, String> display);

    // Get Integer in range
    Integer getInteger(String message, int min, int max, boolean sortDesc);
    Integer getInteger(String message, int min, int max, int cutoff);

    /**
     * Convenience for getChoices(message, 0, 1, choices).
     *
     * @param <T>     is automatically inferred.
     * @param message a {@link java.lang.String} object.
     * @param choices a T object.
     * @return null if choices is missing, empty, or if the users' choices are
     * empty; otherwise, returns the first item in the List returned by
     * getChoices.
     */
    <T> T oneOrNone(String message, List<T> choices);

    /**
     * <p>
     * getChoice.
     * </p>
     *
     * @param <T>     a T object.
     * @param message a {@link java.lang.String} object.
     * @param choices a T object.
     * @return One of {@code choices}. Can only be {@code null} if {@code choices} is empty.
     */
    default <T> T one(final String message, final List<T> choices) {
        return one(message, choices, null);
    }
    <T> T one(String message, List<T> choices, FSerializableFunction<T, String> display);

    <T> void reveal(String message, List<T> items);

    default <T> List<T> many(final String title, final String topCaption, final int cnt, final List<T> sourceChoices, final CardView c) {
        return many(title, topCaption, cnt, cnt, sourceChoices, c);
    }
    default <T> List<T> many(final String title, final String topCaption, final int min, final int max, final List<T> sourceChoices, final CardView c) {
        return many(title, topCaption, min, max, sourceChoices, null, c);
    }
    <T> List<T> many(String title, String topCaption, int min, int max, List<T> sourceChoices, List<T> destChoices, CardView c);

    <T> List<T> order(String title, String top, List<T> sourceChoices, CardView c);
    <T> List<T> order(String title, String top, int remainingObjectsMin, int remainingObjectsMax, List<T> sourceChoices, List<T> destChoices, CardView referenceCard, boolean sideboardingMode);

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
    <T> List<T> insertInList(String title, T newItem, List<T> oldItems);

    List<PaperCard> sideboard(CardPool sideboard, CardPool main, String message);

    GameEntityView chooseSingleEntityForEffect(String title, List<? extends GameEntityView> optionList, DelayedReveal delayedReveal, boolean isOptional);

    List<GameEntityView> chooseEntitiesForEffect(String title, List<? extends GameEntityView> optionList, int min, int max, DelayedReveal delayedReveal);

    // show a list of cards and allow some of them to be moved around and return new list
    List<CardView> manipulateCardList(String title, final Iterable<CardView> cards, final Iterable<CardView> manipulable, boolean toTop, boolean toBottom, boolean toAnywhere);

    void setCard(CardView card);

    void setPlayerAvatar(LobbyPlayer player, IHasIcon ihi);

    PlayerZoneUpdates openZones(PlayerView controller, Collection<ZoneType> zones, Map<PlayerView, Object> players, boolean backupLastZones);

    void restoreOldZones(PlayerView playerView, PlayerZoneUpdates playerZoneUpdates);

    void setHighlighted(GameEntityView pv, boolean b);

    void setSelectables(final Iterable<CardView> cards);

    void clearSelectables();

    boolean isSelecting();

    boolean isGamePaused();
    void setGamePause(boolean pause);

    PlaybackSpeed getGameSpeed();
    void setGameSpeed(PlaybackSpeed gameSpeed);

    String getDayTime();
    void updateDayTime(String daytime);

    void awaitNextInput();
    void cancelAwaitNextInput();

    /** Signal to start a client-side elapsed timer for waiting display. */
    void showWaitingTimer(PlayerView forPlayer, String waitingForPlayerName);

    boolean isUiSetToSkipPhase(PlayerView playerTurn, PhaseType phase);

    void updateAutoPassPrompt();

    void setCurrentPlayer(PlayerView player);

    /**
     * Apply a delta update packet to the local game state.
     * @param packet the delta packet containing changes
     */
    void applyDelta(DeltaPacket packet);

    /** Repaint marker chevron / stack-yield UI for the given player. */
    default void refreshYieldUi(PlayerView player) {}

    /** Apply an authoritative yield-state change. {@link forge.gamemodes.match.AbstractGuiGame} routes to the local {@link forge.interfaces.IGameController}; {@link forge.gamemodes.net.server.RemoteClientGuiGame} forwards over the wire. */
    default void applyYieldUpdate(YieldUpdate update) {}

    /** Returns true if this game instance is a network game. */
    boolean isNetGame();
    void setNetGame();
}
