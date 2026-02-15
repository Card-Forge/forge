package forge.gui.interfaces;

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
import forge.gamemodes.match.YieldMode;
import forge.game.player.DelayedReveal;
import forge.game.player.IHasIcon;
import forge.game.player.PlayerView;
import forge.game.spellability.SpellAbilityView;
import forge.game.zone.ZoneType;
import forge.gui.control.PlaybackSpeed;
import forge.interfaces.IGameController;
import forge.item.PaperCard;
import forge.localinstance.skin.FSkinProp;
import forge.player.PlayerZoneUpdate;
import forge.player.PlayerZoneUpdates;
import forge.trackable.TrackableCollection;
import forge.util.FSerializableFunction;
import forge.util.ITriggerEvent;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface IGuiGame {
    void setGameView(GameView gameView);

    GameView getGameView();

    void setOriginalGameController(PlayerView view, IGameController gameController);

    void setGameController(PlayerView player, IGameController gameController);

    void setSpectator(IGameController spectator);

    void openView(TrackableCollection<PlayerView> myPlayers);

    void afterGameEnd();

    void showCombat();

    void showPromptMessage(PlayerView playerView, String message);

    void showCardPromptMessage(PlayerView playerView, String message, CardView card);

    void updateButtons(PlayerView owner, boolean okEnabled, boolean cancelEnabled, boolean focusOk);

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

    void handleLandPlayed(Card land);

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

    void message(String message);

    void message(String message, String title);

    void showErrorDialog(String message);

    void showErrorDialog(String message, String title);

    boolean showConfirmDialog(String message, String title);

    boolean showConfirmDialog(String message, String title, boolean defaultYes);

    boolean showConfirmDialog(String message, String title, String yesButtonText, String noButtonText);

    boolean showConfirmDialog(String message, String title, String yesButtonText, String noButtonText, boolean defaultYes);

    int showOptionDialog(String message, String title, FSkinProp icon, List<String> options, int defaultOption);

    String showInputDialog(String message, String title, boolean isNumeric);

    String showInputDialog(String message, String title, FSkinProp icon);

    String showInputDialog(String message, String title, FSkinProp icon, String initialInput);

    String showInputDialog(String message, String title, FSkinProp icon, String initialInput, List<String> inputOptions, boolean isNumeric);

    boolean confirm(CardView c, String question);

    boolean confirm(CardView c, String question, List<String> options);

    boolean confirm(CardView c, String question, boolean defaultIsYes, List<String> options);

    <T> List<T> getChoices(String message, int min, int max, List<T> choices);

    <T> List<T> getChoices(String message, int min, int max, List<T> choices, List<T> selected, FSerializableFunction<T, String> display);

    // Get Integer in range
    Integer getInteger(String message, int min);

    Integer getInteger(String message, int min, int max);

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
    <T> T one(String message, List<T> choices);
    <T> T one(String message, List<T> choices, FSerializableFunction<T, String> display);

    <T> void reveal(String message, List<T> items);

    <T> List<T> many(String title, String topCaption, int cnt, List<T> sourceChoices, CardView c);

    <T> List<T> many(String title, String topCaption, int min, int max, List<T> sourceChoices, CardView c);

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

    void setHighlighted(PlayerView pv, boolean b);

    void setUsedToPay(CardView card, boolean value);

    void setSelectables(final Iterable<CardView> cards);

    void clearSelectables();

    boolean isSelecting();

    boolean isGamePaused();

    void setgamePause(boolean pause);

    void setGameSpeed(PlaybackSpeed gameSpeed);

    String getDayTime();

    void updateDayTime(String daytime);

    void awaitNextInput();

    void cancelAwaitNextInput();

    boolean isUiSetToSkipPhase(PlayerView playerTurn, PhaseType phase);

    void autoPassUntilEndOfTurn(PlayerView player);

    boolean mayAutoPass(PlayerView player);

    void autoPassCancel(PlayerView player);

    void updateAutoPassPrompt();

    // Extended yield mode methods (experimental feature)
    void setYieldMode(PlayerView player, YieldMode mode);

    /**
     * Update yield mode from remote client without triggering notification.
     * Used by server to receive yield state from network clients.
     */
    void setYieldModeFromRemote(PlayerView player, YieldMode mode);

    /**
     * Sync yield mode from server to client.
     * Used when server clears yield (end condition met) and needs to update client UI.
     */
    void syncYieldMode(PlayerView player, YieldMode mode);

    void clearYieldMode(PlayerView player);

    boolean shouldAutoYieldForPlayer(PlayerView player);

    YieldMode getYieldMode(PlayerView player);

    boolean didYieldJustEnd(PlayerView player);

    // Smart suggestion decline tracking
    void declineSuggestion(PlayerView player, String suggestionType);

    boolean isSuggestionDeclined(PlayerView player, String suggestionType);

    boolean shouldAutoYield(String key);

    void setShouldAutoYield(String key, boolean autoYield);

    boolean shouldAlwaysAcceptTrigger(int trigger);

    boolean shouldAlwaysDeclineTrigger(int trigger);

    void setShouldAlwaysAcceptTrigger(int trigger);

    void setShouldAlwaysDeclineTrigger(int trigger);

    void setShouldAlwaysAskTrigger(int trigger);

    void clearAutoYields();

    void setCurrentPlayer(PlayerView player);

    /**
     * Look up a PlayerView by ID from the current GameView's player list.
     * Used for network play where deserialized PlayerViews have different trackers.
     * @param player the PlayerView to look up (uses its ID for matching)
     * @return the matching PlayerView from GameView, or the input player if not found
     */
    PlayerView lookupPlayerViewById(PlayerView player);

    /** Signal to start a client-side elapsed timer for waiting display. */
    void showWaitingTimer(PlayerView forPlayer, String waitingForPlayerName);

    /** Returns true if this game instance is a network game. */
    boolean isNetGame();
    void setNetGame();
}
