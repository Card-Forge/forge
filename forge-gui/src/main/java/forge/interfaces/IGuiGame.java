package forge.interfaces;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;

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
import forge.match.MatchButtonType;
import forge.trackable.TrackableCollection;
import forge.util.ITriggerEvent;

public interface IGuiGame {
    void setGameView(GameView gameView);
    void setGameController(PlayerView player, IGameController gameController);
    void openView(TrackableCollection<PlayerView> myPlayers);
    void afterGameEnd();
    void showCombat();
    void showPromptMessage(PlayerView playerView, String message);
    boolean stopAtPhase(PlayerView playerTurn, PhaseType phase);
    IButton getBtnOK(PlayerView playerView);
    IButton getBtnCancel(PlayerView playerView);
    void focusButton(MatchButtonType button);
    void flashIncorrectAction();
    void updatePhase();
    void updateTurn(PlayerView player);
    void updatePlayerControl();
    void enableOverlay();
    void disableOverlay();
    void finishGame();
    Object showManaPool(PlayerView player);
    void hideManaPool(PlayerView player, Object zoneToRestore);
    void updateStack();
    void updateZones(List<Pair<PlayerView, ZoneType>> zonesToUpdate);
    void updateSingleCard(CardView card);
    void updateCards(Iterable<CardView> cards);
    void updateManaPool(Iterable<PlayerView> manaPoolUpdate);
    void updateLives(Iterable<PlayerView> livesUpdate);
    void setPanelSelection(CardView hostCard);
    SpellAbilityView getAbilityToPlay(List<SpellAbilityView> abilities,
            ITriggerEvent triggerEvent);
    Map<CardView, Integer> assignDamage(CardView attacker,
            List<CardView> blockers, int damage, GameEntityView defender,
            boolean overrideOrder);

    void message(String message);
    void message(String message, String title);

    void showErrorDialog(String message);
    void showErrorDialog(String message, String title);

    boolean showConfirmDialog(String message);
    boolean showConfirmDialog(String message, String title);
    boolean showConfirmDialog(String message, String title, boolean defaultYes);
    boolean showConfirmDialog(String message, String title, String yesButtonText, String noButtonText);
    boolean showConfirmDialog(String message, String title, String yesButtonText, String noButtonText, boolean defaultYes);

    int showOptionDialog(String message, String title, FSkinProp icon,
            String[] options, int defaultOption);
    int showCardOptionDialog(CardView card, String message, String title,
            FSkinProp icon, String[] options, int defaultOption);

    String showInputDialog(String message, String title);
    String showInputDialog(String message, String title, FSkinProp icon);
    String showInputDialog(String message, String title, FSkinProp icon,
            String initialInput);
    String showInputDialog(String message, String title, FSkinProp icon,
            String initialInput, String[] inputOptions);

    boolean confirm(CardView c, String question);
    boolean confirm(CardView c, String question, boolean defaultChoice);
    boolean confirm(CardView c, String question, String[] options);
    boolean confirm(CardView c, String question, boolean defaultIsYes, String[] options);

    <T> List<T> getChoices(String message, int min, int max, T[] choices);
    <T> List<T> getChoices(String message, int min, int max,
            Collection<T> choices);
    <T> List<T> getChoices(String message, int min, int max,
            Collection<T> choices, T selected, Function<T, String> display);

    // Get Integer in range
    Integer getInteger(String message);
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
    <T> T oneOrNone(String message, T[] choices);
    <T> T oneOrNone(String message, Collection<T> choices);

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
    <T> T one(String message, T[] choices);
    <T> T one(String message, Collection<T> choices);
    <T> List<T> noneOrMany(String message, Collection<T> choices);

    <T> void reveal(String message, T item);
    <T> void reveal(String message, T[] items);
    <T> void reveal(String message, Collection<T> items);

    <T> List<T> many(String title, String topCaption, int min, int max,
            List<T> sourceChoices);
    <T> List<T> many(String title, String topCaption, int cnt,
            List<T> sourceChoices);
    <T> List<T> many(String title, String topCaption, int cnt,
            List<T> sourceChoices, CardView c);
    <T> List<T> many(String title, String topCaption, int min, int max,
            List<T> sourceChoices, CardView c);

    <T> List<T> order(String title, String top, List<T> sourceChoices);
    <T> List<T> order(String title, String top, List<T> sourceChoices, CardView c);
    <T> List<T> order(String title, String top, int remainingObjectsMin,
            int remainingObjectsMax, List<T> sourceChoices,
            List<T> destChoices, CardView referenceCard,
            boolean sideboardingMode);
    <T> List<T> insertInList(String title, T newItem, List<T> oldItems);

    List<PaperCard> sideboard(CardPool sideboard, CardPool main);
    GameEntityView chooseSingleEntityForEffect(String title,
            Collection<? extends GameEntityView> optionList,
            DelayedReveal delayedReveal, boolean isOptional);
    void setCard(CardView card);
    void setPlayerAvatar(LobbyPlayer player, IHasIcon ihi);
    boolean openZones(Collection<ZoneType> zones, Map<PlayerView, Object> players);
    void restoreOldZones(Map<PlayerView, Object> playersToRestoreZonesFor);
    void updateButtons(PlayerView owner, boolean okEnabled,
            boolean cancelEnabled, boolean focusOk);
    void updateButtons(PlayerView owner, String okLabel, String cancelLabel,
            boolean okEnabled, boolean cancelEnabled, boolean focusOk);
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
    boolean shouldAlwaysAskTrigger(int trigger);
    void setShouldAlwaysAcceptTrigger(int trigger);
    void setShouldAlwaysDeclineTrigger(int trigger);
    void setShouldAlwaysAskTrigger(int trigger);

    void setCurrentPlayer(PlayerView player);
}
