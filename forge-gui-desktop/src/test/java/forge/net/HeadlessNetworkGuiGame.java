package forge.net;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import forge.LobbyPlayer;
import forge.ai.GameState;
import forge.deck.CardPool;
import forge.game.GameEntityView;
import forge.game.card.CardView;
import forge.game.phase.PhaseType;
import forge.game.player.DelayedReveal;
import forge.game.player.IHasIcon;
import forge.game.player.PlayerView;
import forge.game.spellability.SpellAbilityView;
import forge.game.zone.ZoneType;
import forge.gamemodes.net.NetworkGuiGame;
import forge.item.PaperCard;
import forge.localinstance.skin.FSkinProp;
import forge.player.PlayerZoneUpdate;
import forge.player.PlayerZoneUpdates;
import forge.trackable.TrackableCollection;
import forge.util.FSerializableFunction;
import forge.util.ITriggerEvent;

/**
 * Headless implementation of NetworkGuiGame for automated testing.
 * Extends NetworkGuiGame to get proper delta packet processing,
 * while providing minimal implementations for abstract methods.
 *
 * This allows tests to verify that delta sync works correctly
 * on the client side (deserialization, tracker updates, object creation)
 * without requiring a real display.
 *
 * Most GUI methods are no-ops that return safe defaults.
 */
public class HeadlessNetworkGuiGame extends NetworkGuiGame {

    // ========================================
    // Abstract methods from parent classes
    // ========================================

    @Override
    protected void updateCurrentPlayer(PlayerView player) {
        // No-op for headless testing - no UI to update
    }

    @Override
    public boolean isUiSetToSkipPhase(PlayerView playerTurn, PhaseType phase) {
        // For headless testing, don't skip any phases by default
        return false;
    }

    @Override
    public PlayerZoneUpdates openZones(PlayerView controller, Collection<ZoneType> zones,
            Map<PlayerView, Object> players, boolean backupLastZones) {
        // No-op for headless testing
        return null;
    }

    @Override
    public void restoreOldZones(PlayerView playerView, PlayerZoneUpdates playerZoneUpdates) {
        // No-op for headless testing
    }

    // ========================================
    // UI Lifecycle methods
    // ========================================

    @Override
    public void openView(TrackableCollection<PlayerView> myPlayers) {
        // No-op - no GUI to open
    }

    @Override
    public void showCombat() {
        // No-op
    }

    @Override
    public void finishGame() {
        // No-op
    }

    // ========================================
    // Prompts and Messages
    // ========================================

    @Override
    public void showPromptMessage(PlayerView playerView, String message) {
        // No-op - could log if verbose
    }

    @Override
    public void showCardPromptMessage(PlayerView playerView, String message, CardView card) {
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

    // ========================================
    // Phase/Turn Updates
    // ========================================

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

    // ========================================
    // UI Overlays
    // ========================================

    @Override
    public void enableOverlay() {
        // No-op
    }

    @Override
    public void disableOverlay() {
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

    // ========================================
    // Zone/Card Updates
    // ========================================

    @Override
    public void updateStack() {
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
    public void updateCards(Iterable<CardView> cards) {
        // No-op
    }

    // ========================================
    // Game State
    // ========================================

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

    // ========================================
    // Card Selection/Display
    // ========================================

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

    // ========================================
    // Dialogs
    // ========================================

    @Override
    public void message(String message, String title) {
        // No-op
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
        // No-op
    }

    @Override
    public void setPlayerAvatar(LobbyPlayer player, IHasIcon ihi) {
        // No-op
    }
}
