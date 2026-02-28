package forge.net;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import forge.LobbyPlayer;
import forge.ai.GameState;
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
import forge.gamemodes.match.AbstractGuiGame;
import forge.item.PaperCard;
import forge.localinstance.skin.FSkinProp;
import forge.player.PlayerZoneUpdate;
import forge.player.PlayerZoneUpdates;
import forge.trackable.TrackableCollection;
import forge.util.FSerializableFunction;
import forge.util.ITriggerEvent;

/**
 * Headless implementation of AbstractGuiGame for automated network testing.
 * Provides minimal implementations for all abstract/unimplemented methods
 * and tracks client-side state for test assertions.
 */
public class HeadlessNetworkGuiGame extends AbstractGuiGame {

    // Client-side tracking fields for test assertions
    private final AtomicInteger setGameViewCount = new AtomicInteger(0);
    private volatile boolean openViewCalled = false;

    public int getSetGameViewCount() {
        return setGameViewCount.get();
    }

    public boolean isOpenViewCalled() {
        return openViewCalled;
    }

    @Override
    public void setGameView(final GameView gameView) {
        super.setGameView(gameView);
        setGameViewCount.incrementAndGet();
    }

    // ========================================
    // Abstract method from AbstractGuiGame
    // ========================================

    @Override
    protected void updateCurrentPlayer(PlayerView player) {
    }

    // ========================================
    // UI Lifecycle methods
    // ========================================

    @Override
    public void openView(TrackableCollection<PlayerView> myPlayers) {
        openViewCalled = true;
    }

    @Override
    public void showCombat() {
    }

    @Override
    public void finishGame() {
    }

    // ========================================
    // Prompts and Messages
    // ========================================

    @Override
    public void showPromptMessage(PlayerView playerView, String message) {
    }

    @Override
    public void showCardPromptMessage(PlayerView playerView, String message, CardView card) {
    }

    @Override
    public void updateButtons(PlayerView owner, String label1, String label2, boolean enable1, boolean enable2, boolean focus1) {
    }

    @Override
    public void flashIncorrectAction() {
    }

    @Override
    public void alertUser() {
    }

    // ========================================
    // UI Overlays
    // ========================================

    @Override
    public void enableOverlay() {
    }

    @Override
    public void disableOverlay() {
    }

    @Override
    public void showManaPool(PlayerView player) {
    }

    @Override
    public void hideManaPool(PlayerView player) {
    }

    // ========================================
    // Zone/Card Updates
    // ========================================

    @Override
    public Iterable<PlayerZoneUpdate> tempShowZones(PlayerView controller, Iterable<PlayerZoneUpdate> zonesToUpdate) {
        return zonesToUpdate;
    }

    @Override
    public void hideZones(PlayerView controller, Iterable<PlayerZoneUpdate> zonesToUpdate) {
    }

    @Override
    public void updateShards(Iterable<PlayerView> shardsUpdate) {
    }

    // ========================================
    // Game State
    // ========================================

    @Override
    public GameState getGamestate() {
        return null;
    }

    // ========================================
    // Card Selection/Display
    // ========================================

    @Override
    public void setPanelSelection(CardView hostCard) {
    }

    @Override
    public SpellAbilityView getAbilityToPlay(CardView hostCard, List<SpellAbilityView> abilities, ITriggerEvent triggerEvent) {
        return abilities != null && !abilities.isEmpty() ? abilities.get(0) : null;
    }

    @Override
    public Map<CardView, Integer> assignCombatDamage(CardView attacker, List<CardView> blockers, int damage, GameEntityView defender, boolean overrideOrder, boolean maySkip) {
        Map<CardView, Integer> result = new HashMap<>();
        if (blockers != null && !blockers.isEmpty()) {
            result.put(blockers.get(0), damage);
        }
        return result;
    }

    @Override
    public Map<Object, Integer> assignGenericAmount(CardView effectSource, Map<Object, Integer> target, int amount, boolean atLeastOne, String amountLabel) {
        return target;
    }

    // ========================================
    // Dialogs
    // ========================================

    @Override
    public void message(String message, String title) {
    }

    @Override
    public void showErrorDialog(String message, String title) {
        System.err.println("[HeadlessNetworkGuiGame] " + title + ": " + message);
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
    public String showInputDialog(String message, String title, FSkinProp icon, String initialInput, List<String> inputOptions, boolean isNumeric) {
        return initialInput != null ? initialInput : (inputOptions != null && !inputOptions.isEmpty() ? inputOptions.get(0) : "");
    }

    @Override
    public boolean confirm(CardView c, String question, boolean defaultIsYes, List<String> options) {
        return defaultIsYes;
    }

    // ========================================
    // Choices
    // ========================================

    @Override
    public <T> List<T> getChoices(String message, int min, int max, List<T> choices, List<T> selected, FSerializableFunction<T, String> display) {
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
    public <T> List<T> order(String title, String top, int remainingObjectsMin, int remainingObjectsMax, List<T> sourceChoices, List<T> destChoices, CardView referenceCard, boolean sideboardingMode) {
        return sourceChoices != null ? sourceChoices : Collections.emptyList();
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
        return new ArrayList<>(optionList.subList(0, count));
    }

    @Override
    public List<CardView> manipulateCardList(String title, Iterable<CardView> cards, Iterable<CardView> manipulable, boolean toTop, boolean toBottom, boolean toAnywhere) {
        List<CardView> result = new ArrayList<>();
        if (cards != null) {
            for (CardView card : cards) {
                result.add(card);
            }
        }
        return result;
    }

    // ========================================
    // Player/Card Settings
    // ========================================

    @Override
    public void setCard(CardView card) {
    }

    @Override
    public void setPlayerAvatar(LobbyPlayer player, IHasIcon ihi) {
    }

    @Override
    public PlayerZoneUpdates openZones(PlayerView controller, Collection<ZoneType> zones,
            Map<PlayerView, Object> players, boolean backupLastZones) {
        return null;
    }

    @Override
    public void restoreOldZones(PlayerView playerView, PlayerZoneUpdates playerZoneUpdates) {
    }

    @Override
    public boolean isUiSetToSkipPhase(PlayerView playerTurn, PhaseType phase) {
        return false;
    }
}
