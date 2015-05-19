package forge.interfaces;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.google.common.base.Function;

import forge.LobbyPlayer;
import forge.assets.FSkinProp;
import forge.deck.CardPool;
import forge.game.GameEntityView;
import forge.game.GameView;
import forge.game.card.CardView;
import forge.game.phase.PhaseType;
import forge.game.player.DelayedReveal;
import forge.game.player.IHasIcon;
import forge.game.player.PlayerView;
import forge.game.spellability.SpellAbilityView;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;
import forge.player.PlayerZoneUpdate;
import forge.trackable.TrackableCollection;
import forge.util.ITriggerEvent;

public interface IGuiGame {
    void setGameView(GameView gameView);
    void setOriginalGameController(PlayerView view, IGameController gameController);
    void setGameController(PlayerView player, IGameController gameController);
    void setSpectator(IGameController spectator);
    void openView(TrackableCollection<PlayerView> myPlayers);
    void afterGameEnd();
    void showCombat();
    void showPromptMessage(PlayerView playerView, String message);
    void updateButtons(PlayerView owner, boolean okEnabled, boolean cancelEnabled, boolean focusOk);
    void updateButtons(PlayerView owner, String label1, String label2, boolean enable1, boolean enable2, boolean focus1);
    void flashIncorrectAction();
    void updatePhase();
    void updateTurn(PlayerView player);
    void updatePlayerControl();
    void enableOverlay();
    void disableOverlay();
    void finishGame();
    void showManaPool(PlayerView player);
    void hideManaPool(PlayerView player);
    void updateStack();
    void updateZones(Iterable<PlayerZoneUpdate> zonesToUpdate);
    void updateSingleCard(CardView card);
    void updateCards(Iterable<CardView> cards);
    void refreshCardDetails(Iterable<CardView> cards);
    void updateManaPool(Iterable<PlayerView> manaPoolUpdate);
    void updateLives(Iterable<PlayerView> livesUpdate);
    void setPanelSelection(CardView hostCard);
    SpellAbilityView getAbilityToPlay(CardView hostCard, List<SpellAbilityView> abilities, ITriggerEvent triggerEvent);
    Map<CardView, Integer> assignDamage(CardView attacker, List<CardView> blockers, int damage, GameEntityView defender, boolean overrideOrder);

    void message(String message);
    void message(String message, String title);

    void showErrorDialog(String message);
    void showErrorDialog(String message, String title);

    boolean showConfirmDialog(String message, String title);
    boolean showConfirmDialog(String message, String title, boolean defaultYes);
    boolean showConfirmDialog(String message, String title, String yesButtonText, String noButtonText);
    boolean showConfirmDialog(String message, String title, String yesButtonText, String noButtonText, boolean defaultYes);

    int showOptionDialog(String message, String title, FSkinProp icon, List<String> options, int defaultOption);

    String showInputDialog(String message, String title);
    String showInputDialog(String message, String title, FSkinProp icon);
    String showInputDialog(String message, String title, FSkinProp icon, String initialInput);
    String showInputDialog(String message, String title, FSkinProp icon, String initialInput, List<String> inputOptions);

    boolean confirm(CardView c, String question);
    boolean confirm(CardView c, String question, List<String> options);
    boolean confirm(CardView c, String question, boolean defaultIsYes, List<String> options);

    <T> List<T> getChoices(String message, int min, int max, List<T> choices);
    <T> List<T> getChoices(String message, int min, int max, List<T> choices, T selected, Function<T, String> display);

    // Get Integer in range
    Integer getInteger(String message, int min);
    Integer getInteger(String message, int min, int max);
    Integer getInteger(String message, int min, int max, boolean sortDesc);
    Integer getInteger(String message, int min, int max, int cutoff);

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
    <T> T oneOrNone(String message, List<T> choices);

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
     * @return One of {@code choices}. Can only be {@code null} if {@code choices} is empty.
     */
    <T> T one(String message, List<T> choices);

    <T> void reveal(String message, List<T> items);

    <T> List<T> many(String title, String topCaption, int cnt, List<T> sourceChoices, CardView c);
    <T> List<T> many(String title, String topCaption, int min, int max, List<T> sourceChoices, CardView c);

    <T> List<T> order(String title, String top, List<T> sourceChoices, CardView c);
    <T> List<T> order(String title, String top, int remainingObjectsMin, int remainingObjectsMax, List<T> sourceChoices, List<T> destChoices, CardView referenceCard, boolean sideboardingMode);

    /**
     * Ask the user to insert an object into a list of other objects. The
     * current implementation requires the user to cancel in order to get the
     * new item to be the first item in the resulting list.
     *
     * @param title
     *            the dialog title.
     * @param newItem
     *            the object to insert.
     * @param oldItems
     *            the list of objects.
     * @return A shallow copy of the list of objects, with newItem inserted.
     */
    <T> List<T> insertInList(String title, T newItem, List<T> oldItems);

    List<PaperCard> sideboard(CardPool sideboard, CardPool main);
    GameEntityView chooseSingleEntityForEffect(String title, List<? extends GameEntityView> optionList, DelayedReveal delayedReveal, boolean isOptional); void setCard(CardView card);
    void setPlayerAvatar(LobbyPlayer player, IHasIcon ihi);
    boolean openZones(Collection<ZoneType> zones, Map<PlayerView, Object> players);
    void restoreOldZones(Map<PlayerView, Object> playersToRestoreZonesFor);
    void setHighlighted(PlayerView pv, boolean b);
    void setUsedToPay(CardView card, boolean value);

    void awaitNextInput();
    void cancelAwaitNextInput();

    boolean isUiSetToSkipPhase(PlayerView playerTurn, PhaseType phase);
    void autoPassUntilEndOfTurn(PlayerView player);
    boolean mayAutoPass(PlayerView player);
    void autoPassCancel(PlayerView player);
    void updateAutoPassPrompt();
    boolean shouldAutoYield(String key);
    void setShouldAutoYield(String key, boolean autoYield);
    boolean shouldAlwaysAcceptTrigger(int trigger);
    boolean shouldAlwaysDeclineTrigger(int trigger);
    void setShouldAlwaysAcceptTrigger(int trigger);
    void setShouldAlwaysDeclineTrigger(int trigger);
    void setShouldAlwaysAskTrigger(int trigger);

    void setCurrentPlayer(PlayerView player);
}
