package forge.player;

import com.google.common.collect.Lists;
import forge.card.MagicColor;
import forge.game.GameEntity;
import forge.game.GameEntityView;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardView;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.PlayerView;
import forge.game.player.actions.ActivateAbilityAction;
import forge.game.player.actions.ColorChoiceAction;
import forge.game.player.actions.ConfirmAction;
import forge.game.player.actions.FinishTargetingAction;
import forge.game.player.actions.ManaComboAction;
import forge.game.player.actions.ModeChoiceAction;
import forge.game.player.actions.PassPriorityAction;
import forge.game.player.actions.PayCostAction;
import forge.game.player.actions.PayManaFromPoolAction;
import forge.game.player.actions.PlayerAction;
import forge.game.player.actions.SelectCardAction;
import forge.game.player.actions.SelectPlayerAction;
import forge.game.player.actions.ScryAction;
import forge.game.player.actions.StackOrderAction;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.gamemodes.match.input.Input;
import forge.gamemodes.match.input.InputAttack;
import forge.gamemodes.match.input.InputConfirm;
import forge.gamemodes.match.input.InputLockUI;
import forge.gamemodes.match.input.InputPassPriority;
import forge.gamemodes.match.input.InputPayMana;
import forge.gamemodes.match.input.InputSelectEntitiesFromList;
import forge.gamemodes.match.input.InputSelectTargets;
import forge.gui.FThreads;
import forge.interfaces.IMacroSystem;
import forge.util.ITriggerEvent;
import forge.util.Localizer;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.IntSupplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

// Iteration on the current limited macro system. Instead of asking for IDs to click on
// Instead we wrap the input queue in a way that we can record what the player is doing and
// try to play it back as much as possible

public class RecordActionsMacroSystem implements IMacroSystem {
    private static final String DEBUG_PREFIX = "[MacroReplay] ";
    private static final boolean DEBUG = Boolean.parseBoolean(System.getProperty("forge.macro.debug", "false"));
    private static final int NO_ACTION_ACCEPTED = -1;
    private static final int WAIT_FOR_NEXT_INPUT = -2;
    private static final int MAX_REJECTED_ACTION_RETRIES = 40;
    private static final int MAX_WAIT_RETRIES = 400;
    private static final Pattern CARD_ID_PATTERN = Pattern.compile(" \\((\\d+)\\)");
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");

    private final PlayerControllerHuman playerControllerHuman;
    private final Localizer localizer = Localizer.getInstance();
    private final ITriggerEvent replayTriggerEvent = new DummyTriggerEvent();

    private final List<PlayerAction> actions = Lists.newArrayList();
    private final List<PlayerAction> playbackActions = Lists.newArrayList();
    private final List<Runnable> statusListeners = Lists.newArrayList();
    private boolean recording;
    private int repeatIteration;
    private int repeatIterations;
    private int playbackRetries;
    private int activeActionIndex = -1;
    private int pendingStackOrderPriorityPasses;
    private boolean waitingForSelectionAfterStackOrder;
    private ManaComboAction pendingRecordedManaCombo;
    private final List<String> playbackMessages = Lists.newArrayList();

    public RecordActionsMacroSystem(final PlayerControllerHuman playerControllerHuman) {
        this.playerControllerHuman = playerControllerHuman;
    }

    @Override
    public boolean isRecording() { return recording; }

    @Override
    public boolean isReplaying() { return repeatIterations > 0; }

    @Override
    public boolean hasRememberedActions() { return !actions.isEmpty(); }

    @Override
    public List<String> getRememberedActionDescriptions() {
        return actions.stream().map(PlayerAction::describe).collect(Collectors.toList());
    }

    @Override
    public int getActiveActionIndex() {
        return activeActionIndex;
    }

    @Override
    public List<String> getPlaybackMessages() {
        return Lists.newArrayList(playbackMessages);
    }

    @Override
    public void addStatusListener(final Runnable listener) {
        if (listener != null && !statusListeners.contains(listener)) {
            statusListeners.add(listener);
        }
    }

    @Override
    public void removeStatusListener(final Runnable listener) {
        statusListeners.remove(listener);
    }

    private void notifyStatusListeners() {
        for (final Runnable listener : Lists.newArrayList(statusListeners)) {
            listener.run();
        }
    }

    @Override
    public Byte consumeRememberedColorChoice(final List<MagicColor.Color> choices) {
        return consumeRememberedAction(ColorChoiceAction.class,
                action -> hasColorChoice(choices, action.getColor()) ? action.getColor() : null);
    }

    @Override
    public Map<Byte, Integer> consumeRememberedManaCombo(final List<MagicColor.Color> choices, final int manaAmount,
                                                         final boolean different) {
        return consumeRememberedAction(ManaComboAction.class,
                action -> isValidManaCombo(action.getManaCombo(), choices, manaAmount, different)
                        ? new LinkedHashMap<>(action.getManaCombo()) : null);
    }

    private boolean isValidManaCombo(final Map<Byte, Integer> manaCombo, final List<MagicColor.Color> choices,
                                     final int manaAmount, final boolean different) {
        int total = 0;
        for (final Map.Entry<Byte, Integer> entry : manaCombo.entrySet()) {
            final int amount = entry.getValue() == null ? 0 : entry.getValue();
            if (amount < 0 || (different && amount > 1)) {
                return false;
            }
            if (!hasColorChoice(choices, entry.getKey())) {
                return false;
            }
            total += amount;
        }
        return total == manaAmount;
    }

    private boolean hasColorChoice(final List<MagicColor.Color> choices, final byte colorMask) {
        for (final MagicColor.Color choice : choices) {
            if (choice != MagicColor.Color.COLORLESS && choice.getColorMask() == colorMask) {
                return true;
            }
        }
        return false;
    }

    @Override
    public List<String> consumeRememberedModeChoice(final List<String> choices, final int min, final int num,
                                                    final boolean allowRepeat) {
        return consumeRememberedAction(ModeChoiceAction.class,
                action -> isValidModeChoice(action.getModeDescriptions(), choices, min, num, allowRepeat)
                        ? Lists.newArrayList(action.getModeDescriptions()) : null);
    }

    private boolean isValidModeChoice(final List<String> selectedModes, final List<String> choices, final int min,
                                      final int num, final boolean allowRepeat) {
        if (selectedModes.size() < min || selectedModes.size() > num) {
            return false;
        }
        final List<String> available = Lists.newArrayList(choices);
        for (final String selectedMode : selectedModes) {
            if (!available.contains(selectedMode) || (!allowRepeat && !available.remove(selectedMode))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public List<String> consumeRememberedAbilityOrder(final List<String> choices) {
        return consumeRememberedAction(StackOrderAction.class,
                action -> buildRememberedAbilityOrder(action.getAbilityDescriptions(), choices));
    }

    private List<String> buildRememberedAbilityOrder(final List<String> selectedOrder, final List<String> choices) {
        if (selectedOrder.size() != choices.size()) {
            return null;
        }
        final List<String> available = normalizeAbilityDescriptions(choices);
        final List<String> availableChoices = Lists.newArrayList(choices);
        final List<String> orderedChoices = Lists.newArrayListWithCapacity(selectedOrder.size());
        for (final String selected : selectedOrder) {
            final int choiceIndex = available.indexOf(normalizeAbilityDescription(selected));
            if (choiceIndex < 0) {
                return null;
            }
            available.remove(choiceIndex);
            orderedChoices.add(availableChoices.remove(choiceIndex));
        }
        return available.isEmpty() ? orderedChoices : null;
    }

    private List<String> normalizeAbilityDescriptions(final List<String> descriptions) {
        final List<String> normalized = Lists.newArrayListWithCapacity(descriptions.size());
        for (final String description : descriptions) {
            normalized.add(normalizeAbilityDescription(description));
        }
        return normalized;
    }

    private String normalizeAbilityDescription(final String description) {
        if (description == null) {
            return "";
        }
        return WHITESPACE_PATTERN.matcher(CARD_ID_PATTERN.matcher(description).replaceAll(""))
                .replaceAll(" ").trim();
    }

    @Override
    public ImmutablePair<CardCollection, CardCollection> consumeRememberedScry(final CardCollection topN) {
        return consumeRememberedAction(ScryAction.class, action -> buildRememberedScry(topN, action));
    }

    private <A extends PlayerAction, R> R consumeRememberedAction(final Class<A> actionClass,
                                                                  final Function<A, R> replay) {
        if (!isReplaying()) {
            return null;
        }
        final int actionIndex = findNextAction(actionClass);
        if (actionIndex < 0) {
            return null;
        }
        final A action = actionClass.cast(playbackActions.get(actionIndex));
        final R result = replay.apply(action);
        if (result != null) {
            debug("accepted " + action.describe());
            playbackActions.remove(actionIndex);
            playbackRetries = 0;
            pendingStackOrderPriorityPasses = 0;
            waitingForSelectionAfterStackOrder = action instanceof StackOrderAction
                    && findNextAction(SelectCardAction.class, SelectPlayerAction.class) >= 0;
            playerControllerHuman.getInputQueue().updateObservers();
            notifyStatusListeners();
            return result;
        }
        debug("rejected " + action.describe());
        return null;
    }

    private ImmutablePair<CardCollection, CardCollection> buildRememberedScry(final CardCollection topN,
                                                                              final ScryAction action) {
        final CardCollection available = new CardCollection(topN);
        final CardCollection toTop = takeCardsByName(available, action.getTopCardNames());
        final CardCollection toBottom = takeCardsByName(available, action.getBottomCardNames());
        if (toTop == null || toBottom == null || !available.isEmpty()) {
            return null;
        }
        return ImmutablePair.of(toTop.isEmpty() ? null : toTop, toBottom.isEmpty() ? null : toBottom);
    }

    private CardCollection takeCardsByName(final CardCollection available, final List<String> cardNames) {
        final CardCollection result = new CardCollection();
        for (final String cardName : cardNames) {
            Card found = null;
            for (final Card card : available) {
                if (card.getName().equals(cardName)) {
                    found = card;
                    break;
                }
            }
            if (found == null) {
                return null;
            }
            result.add(found);
            available.remove(found);
        }
        return result;
    }

    @Override
    public String playbackText() {
        if (repeatIterations > 0) {
            return repeatIteration + " / " + repeatIterations;
        }
        if (playbackActions.isEmpty()) {
            return null;
        }

        return actions.size() - playbackActions.size() + " / " + actions.size();
    }

    public boolean startRecording() {
        if (recording) {
            return false;
        }

        if (isReplaying()) {
            finishPlayback();
        }
        recording = true;
        activeActionIndex = -1;
        actions.clear();
        playbackActions.clear();
        playbackMessages.clear();
        playerControllerHuman.getInputQueue().updateObservers();
        notifyStatusListeners();

        return true;
    }

    public boolean finishRecording() {
        if (!recording) {
            return false;
        }

        recording = false;
        flushPendingRecordedManaCombo();
        playerControllerHuman.getInputQueue().updateObservers();
        notifyStatusListeners();

        return true;
    }

    @Override
    public void addRememberedAction(PlayerAction action) {
        if (!recording) {
            return;
        }

        if (action instanceof ManaComboAction manaComboAction) {
            pendingRecordedManaCombo = manaComboAction;
            return;
        }

        if (action instanceof ActivateAbilityAction) {
            removeLastCardSelectionFor(action.getGameEntityView(), actions);
            removeLastCardSelectionFor(action.getGameEntityView(), playbackActions);
        }

        if (pendingRecordedManaCombo != null && !(action instanceof PayManaFromPoolAction)) {
            flushPendingRecordedManaCombo();
        }

        if (action instanceof ConfirmAction confirmAction && replaceDuplicateConfirmAction(confirmAction)) {
            return;
        }

        rememberAction(action);
    }

    private boolean replaceDuplicateConfirmAction(final ConfirmAction action) {
        if (actions.isEmpty() || playbackActions.isEmpty()) {
            return false;
        }
        final PlayerAction previous = actions.get(actions.size() - 1);
        if (!(previous instanceof ConfirmAction previousConfirm)
                || previousConfirm.isConfirmed() != action.isConfirmed()) {
            return false;
        }
        actions.set(actions.size() - 1, action);
        playbackActions.set(playbackActions.size() - 1, action);
        return true;
    }

    private void flushPendingRecordedManaCombo() {
        if (pendingRecordedManaCombo == null) {
            return;
        }
        rememberAction(pendingRecordedManaCombo);
        pendingRecordedManaCombo = null;
    }

    private void rememberAction(final PlayerAction action) {
        actions.add(action);
        playbackActions.add(action);
        notifyStatusListeners();
    }

    private void removeLastCardSelectionFor(final GameEntityView view, final List<PlayerAction> actionList) {
        if (view == null || actionList.isEmpty()) {
            return;
        }
        final PlayerAction previous = actionList.get(actionList.size() - 1);
        if (view.equals(previous.getSelectedCardView())) {
            actionList.remove(actionList.size() - 1);
        }
    }

    @Override
    public void setRememberedActions() {
        if (recording) {
            if (finishRecording()) {
                repeatRememberedActions();
            }
        } else {
            startRecording();
        }
    }

    @Override
    public void repeatRememberedActions() {
        final String dialogTitle = localizer.getMessage("lblRepeatActionSequence");
        if (actions.isEmpty()) {
            playerControllerHuman.getGui().message(localizer.getMessage("lblPleaseDefineanActionSequenceFirst"), dialogTitle);
            return;
        }
        if (recording) {
            playerControllerHuman.getGui().message(localizer.getMessage("lblFinishRecordingBeforePlayback"), dialogTitle);
            return;
        }
        if (!playbackActions.isEmpty() && actions.size() != playbackActions.size()) {
            playerControllerHuman.getGui().message(localizer.getMessage("lblRestartMacroBeforeRepeat"), dialogTitle);
            return;
        }

        final String input = playerControllerHuman.getGui().showInputDialog(
                localizer.getMessage("lblHowManyTimesToRepeatSequence"),
                dialogTitle, null, "1", null, true);
        final int loops;
        try {
            loops = Integer.parseInt(input.trim());
        } catch (final Exception ex) {
            return;
        }
        if (loops <= 0) {
            return;
        }

        startPlayback(loops);
    }

    @Override
    public void nextRememberedAction() {
        if (actions.isEmpty()) {
            return;
        }

        if (recording) {
            playerControllerHuman.getGui().message(localizer.getMessage("lblFinishRecordingBeforePlayback"),
                    localizer.getMessage("lblDoNextActioninSequence"));
            return;
        }

        startPlayback(1);
    }

    @Override
    public void cancelPlayback() {
        if (repeatIterations > 0 || !playbackActions.isEmpty()) {
            if (DEBUG) {
                debug("cancelled input=" + describeInput() + " remaining=" + describeActions(playbackActions));
            }
            finishPlayback();
        }
    }

    @Override
    public void cancelCurrentMacro() {
        if (recording) {
            if (DEBUG) {
                debug("cancelled recording actions=" + describeActions(actions));
            }
            recording = false;
            actions.clear();
        }
        pendingRecordedManaCombo = null;
        cancelPlayback();
        playerControllerHuman.getInputQueue().updateObservers();
        notifyStatusListeners();
    }

    private void startPlayback(final int loops) {
        repeatIterations = loops;
        repeatIteration = 1;
        resetPlaybackProgress();
        activeActionIndex = -1;
        playbackMessages.clear();
        restartPlaybackActions();
        if (DEBUG) {
            debug("start loops=" + loops + " actions=" + describeActions(playbackActions));
        }
        playerControllerHuman.getInputQueue().updateObservers();
        notifyStatusListeners();
        FThreads.delayInEDT(50, this::continuePlayback);
    }

    private void continuePlayback() {
        if (recording || repeatIterations <= 0 || playerControllerHuman.getGame().isGameOver()) {
            finishPlayback();
            return;
        }

        if (playbackActions.isEmpty()) {
            final int pendingInputResult = processPendingInputAfterActions();
            if (pendingInputResult == WAIT_FOR_NEXT_INPUT) {
                if (++playbackRetries > MAX_WAIT_RETRIES) {
                    stopPlayback("lblMacroPlaybackStoppedWaitingAfterFinalAction");
                    return;
                }
                FThreads.delayInEDT(50, this::continuePlayback);
                return;
            }
            if (repeatIteration >= repeatIterations) {
                finishPlayback();
                return;
            }
            if (waitForInterIterationStackClear()) {
                if (repeatIterations > 0) {
                    FThreads.delayInEDT(50, this::continuePlayback);
                }
                return;
            }
            repeatIteration++;
            restartPlaybackActions();
            resetPlaybackProgress();
            activeActionIndex = -1;
            playerControllerHuman.getInputQueue().updateObservers();
            notifyStatusListeners();
        }

        setActiveAction(playbackActions.isEmpty() ? null : playbackActions.get(0));
        final int actionIndex = processNextAcceptedAction();
        if (actionIndex >= 0) {
            final PlayerAction acceptedAction = playbackActions.remove(actionIndex);
            playbackRetries = 0;
            pendingStackOrderPriorityPasses = 0;
            if (acceptedAction.clearsPostStackOrderWait()) {
                waitingForSelectionAfterStackOrder = false;
            }
        } else if (actionIndex == WAIT_FOR_NEXT_INPUT) {
            if (++playbackRetries > MAX_WAIT_RETRIES) {
                stopPlayback("lblMacroPlaybackStoppedWaiting");
                return;
            }
        } else if (++playbackRetries > MAX_REJECTED_ACTION_RETRIES) {
            stopPlayback("lblMacroPlaybackStoppedGeneric");
            return;
        }

        FThreads.delayInEDT(50, this::continuePlayback);
    }

    private int processPendingInputAfterActions() {
        final Input input = playerControllerHuman.getInputProxy().getInput();
        if (input instanceof InputLockUI) {
            debug("waiting for pending game action after final recorded action");
            return WAIT_FOR_NEXT_INPUT;
        }
        if (!isStackEmpty() && isPriorityInput(input)) {
            debug("clearing pending stack after final recorded action");
            return waitForInput(input);
        }
        if (input instanceof InputPayMana || input instanceof InputConfirm) {
            debug("handling pending " + describeInput() + " after final recorded action");
            return processNextAcceptedAction();
        }
        if (input instanceof InputSelectEntitiesFromList<?> selectInput) {
            debug("finishing pending list selection after final recorded action");
            return finishListSelectionIfAny(selectInput);
        }
        return NO_ACTION_ACCEPTED;
    }

    private int processNextAcceptedAction() {
        final Input inp = playerControllerHuman.getInputProxy().getInput();
        debugPlaybackState();
        if (inp instanceof InputLockUI) {
            return WAIT_FOR_NEXT_INPUT;
        } else if (inp instanceof InputPassPriority passPriorityInput && passPriorityInput.getChosenSa() != null) {
            return WAIT_FOR_NEXT_INPUT;
        } else if (waitingForSelectionAfterStackOrder && isPriorityInput(inp)) {
            final int nextSelection = findNextAction(SelectCardAction.class, SelectPlayerAction.class);
            if (nextSelection < 0) {
                waitingForSelectionAfterStackOrder = false;
                return NO_ACTION_ACCEPTED;
            }
            if (!isStackEmpty()) {
                return waitForInput(inp, "waiting for post-order selection before "
                        + playbackActions.get(nextSelection).describe());
            }
            waitingForSelectionAfterStackOrder = false;
            debug("stack emptied before post-order selection " + playbackActions.get(nextSelection).describe());
            return NO_ACTION_ACCEPTED;
        } else if (inp instanceof InputAttack) {
            final int passPriority = findNextAction(PassPriorityAction.class);
            final int finishAttack = findNextActionBefore(passPriority, FinishTargetingAction.class);
            final int actionBoundary = finishAttack >= 0 ? finishAttack : passPriority;
            final int playerAction = findNextActionBefore(actionBoundary, SelectPlayerAction.class);
            if (playerAction >= 0) {
                return processActionAt(playerAction);
            }

            final int cardAction = findNextActionBefore(actionBoundary, SelectCardAction.class);
            final int cardResult = processActionAt(cardAction);
            if (cardResult >= 0) {
                return cardResult;
            }
            if (cardAction >= 0) {
                return findPendingPlayerCard(cardAction) == null ? NO_ACTION_ACCEPTED
                        : waitForInput(inp, "waiting for postcombat main before "
                                + playbackActions.get(cardAction).describe());
            }
            if (finishAttack >= 0) {
                return processActionAt(finishAttack);
            }

            final int passResult = processAttackPassPriority(passPriority, inp);
            if (passResult >= 0) {
                return passResult;
            }
        } else if (inp instanceof InputSelectTargets targetInput) {
            for (int i = 0; i < playbackActions.size(); i++) {
                final PlayerAction action = playbackActions.get(i);
                if (action.asPassPriorityAction() != null && noRemainingTargetSelectionsBefore(i)) {
                    return waitForInput(inp);
                }
                if (action instanceof FinishTargetingAction) {
                    if (noRemainingTargetSelectionsBefore(i) && processAction(action)) {
                        return i;
                    }
                    continue;
                }
                if (action.isSelectionAction()) {
                    if (processAction(action)) {
                        return i;
                    }
                    if (targetInput.canFinishTargetingForMacro()) {
                        debug("finished current target prompt before " + action.describe());
                        return waitForInput(targetInput);
                    }
                }
            }
        } else if (inp instanceof InputSelectEntitiesFromList<?> selectInput) {
            return processNextListSelectionAction(selectInput);
        } else if (inp instanceof InputPayMana manaInput) {
            final int manaComboAction = findNextAction(ManaComboAction.class);
            final int paymentAction = findNextManaPaymentAction();
            if (manaComboAction >= 0 && (paymentAction < 0 || manaComboAction < paymentAction)) {
                if (payManaFromPoolBeforeManaCombo(manaInput, manaComboAction)) {
                    return WAIT_FOR_NEXT_INPUT;
                }
                return waitForInput(inp);
            }
            if (paymentAction >= 0 && processAction(playbackActions.get(paymentAction))) {
                return paymentAction;
            }
            final int manaSourceAction = activateLeadingRecordedManaSource();
            if (manaSourceAction >= 0) {
                return manaSourceAction;
            }
            if (manaInput.isActivatingManaAbility()) {
                return WAIT_FOR_NEXT_INPUT;
            }
            if (payAnyManaFromPool(manaInput)) {
                return WAIT_FOR_NEXT_INPUT;
            }
            if (activateFutureRecordedLandManaSource()) {
                return WAIT_FOR_NEXT_INPUT;
            }
            if (hasFutureUsableRecordedLandSelection()) {
                debug("waiting for recorded future land mana source");
                return WAIT_FOR_NEXT_INPUT;
            }
            return waitForInput(inp);
        } else if (inp instanceof InputConfirm confirmInput) {
            final int confirmAction = findNextAction(PayCostAction.class, ConfirmAction.class);
            if (confirmAction >= 0 && processAction(playbackActions.get(confirmAction))) {
                return confirmAction;
            }
            if (canAcceptImplicitTriggerConfirm(confirmInput)) {
                return waitForInput(confirmInput, "accepting trigger before "
                        + playbackActions.get(findNextAction(SelectCardAction.class, SelectPlayerAction.class)).describe());
            }
            return waitForInput(inp);
        } else if (inp instanceof InputPassPriority) {
            final int passPriority = findNextAction(PassPriorityAction.class);
            final int cardAction = findNextActionBefore(passPriority, SelectCardAction.class);
            final int stackOrderAction = findNextActionBefore(cardAction, StackOrderAction.class);
            if (stackOrderAction >= 0 && !isStackEmpty()) {
                if (playerControllerHuman.getGame().getStack().hasSimultaneousStackEntries()) {
                    return WAIT_FOR_NEXT_INPUT;
                }
                if (pendingStackOrderPriorityPasses > 0) {
                    debug("not passing priority again while waiting for simultaneous ability order");
                    return NO_ACTION_ACCEPTED;
                }
                pendingStackOrderPriorityPasses++;
                return waitForInput(inp, "waiting for simultaneous ability order before "
                        + playbackActions.get(stackOrderAction).describe());
            }
            final int activateAbility = findNextActionBefore(passPriority, ActivateAbilityAction.class);
            if (cardAction >= 0 && (activateAbility < 0 || cardAction < activateAbility)) {
                return processActionAt(cardAction, () -> waitForPendingCardAction(inp, cardAction, stackOrderAction >= 0));
            }

            final int activateResult = processActionAt(activateAbility);
            if (activateResult >= 0) {
                return activateResult;
            }

            if (cardAction >= 0) {
                return processActionAt(cardAction, () -> waitForPendingCardAction(inp, cardAction, stackOrderAction >= 0));
            }

            final int playerAction = findNextActionBefore(passPriority, SelectPlayerAction.class);
            if (playerAction >= 0) {
                return processActionAt(playerAction);
            }

            if (passPriority >= 0 && noRemainingTargetSelectionsBefore(passPriority)) {
                return skipOrProcessPassPriority(passPriority);
            }
        }

        for (int i = 0; i < playbackActions.size(); i++) {
            final PlayerAction action = playbackActions.get(i);
            if (inp instanceof InputAttack && action.asPassPriorityAction() != null) {
                return processAttackPassPriority(i, inp);
            }
            if (inp instanceof InputPassPriority && action.asPassPriorityAction() != null
                    && !noRemainingTargetSelectionsBefore(i)) {
                continue;
            }
            if (shouldSkipPassPriorityAction(i)) {
                setActiveAction(action);
                debug("skipped obsolete " + action.describe());
                return i;
            }
            if (!(inp instanceof InputSelectTargets) && action.isTargetSelectionAction()) {
                continue;
            }
            if (processAction(action)) {
                return i;
            }
        }
        return NO_ACTION_ACCEPTED;
    }

    private int processActionAt(final int actionIndex) {
        return processActionAt(actionIndex, () -> NO_ACTION_ACCEPTED);
    }

    private int processActionAt(final int actionIndex, final IntSupplier fallback) {
        if (actionIndex < 0) {
            return NO_ACTION_ACCEPTED;
        }
        return processAction(playbackActions.get(actionIndex)) ? actionIndex : fallback.getAsInt();
    }

    private int skipOrProcessPassPriority(final int actionIndex) {
        if (actionIndex < 0) {
            return NO_ACTION_ACCEPTED;
        }
        if (shouldSkipPassPriorityAction(actionIndex)) {
            debug("skipped obsolete " + playbackActions.get(actionIndex).describe());
            return actionIndex;
        }
        return processActionAt(actionIndex);
    }

    private int processAttackPassPriority(final int actionIndex, final Input input) {
        if (actionIndex < 0) {
            return NO_ACTION_ACCEPTED;
        }
        final PassPriorityAction passPriorityAction = playbackActions.get(actionIndex).asPassPriorityAction();
        if (passPriorityAction == null) {
            return NO_ACTION_ACCEPTED;
        }
        if (passPriorityAction.isStackPassFor(getCurrentPhase())) {
            return waitForInput(input);
        }
        final int result = processActionAt(actionIndex);
        return result >= 0 ? result : waitForInput(input);
    }

    private int processNextListSelectionAction(final InputSelectEntitiesFromList<?> selectInput) {
        for (int i = 0; i < playbackActions.size(); i++) {
            final PlayerAction action = playbackActions.get(i);
            if (action.isSelectionAction()) {
                return processAction(action) ? i : finishListSelectionIfAny(selectInput);
            }
        }
        return finishListSelectionIfAny(selectInput);
    }

    private int finishListSelectionIfAny(final InputSelectEntitiesFromList<?> input) {
        return input.getSelected().isEmpty() ? NO_ACTION_ACCEPTED : waitForInput(input);
    }

    private boolean isPriorityInput(final Input input) {
        return input instanceof InputPassPriority || input instanceof InputAttack;
    }

    private boolean shouldSkipPassPriorityAction(final int actionIndex) {
        final PlayerAction action = playbackActions.get(actionIndex);
        final PassPriorityAction passPriorityAction = action.asPassPriorityAction();
        return passPriorityAction != null
                && (passPriorityAction.isObsoleteWhen(isStackEmpty())
                || passPriorityAction.isStaleFor(getCurrentPhase()))
                || isTrailingMainPhasePassBeforeNextIteration(actionIndex);
    }

    private boolean isTrailingMainPhasePassBeforeNextIteration(final int actionIndex) {
        if (repeatIteration >= repeatIterations || !isStackEmpty()) {
            return false;
        }
        final PlayerAction action = playbackActions.get(actionIndex);
        final PassPriorityAction passPriorityAction = action.asPassPriorityAction();
        if (passPriorityAction == null || !passPriorityAction.isTrailingMainPhasePassCandidate(getCurrentPhase())) {
            return false;
        }
        for (int i = actionIndex + 1; i < playbackActions.size(); i++) {
            final PassPriorityAction remainingPass = playbackActions.get(i).asPassPriorityAction();
            if (remainingPass == null || !remainingPass.wasStackEmpty()) {
                return false;
            }
        }
        return !actions.isEmpty() && actions.get(0).asPassPriorityAction() == null;
    }

    private PhaseType getCurrentPhase() {
        return playerControllerHuman.getGame().getPhaseHandler().getPhase();
    }

    private boolean isStackEmpty() {
        return playerControllerHuman.getGame().getStack().isEmpty();
    }

    private boolean waitForInterIterationStackClear() {
        if (isStackEmpty()) {
            playbackRetries = 0;
            return false;
        }

        final Input input = playerControllerHuman.getInputProxy().getInput();
        if (canStartNextIterationBeforeStackClear(input)) {
            playbackRetries = 0;
            return false;
        }
        if (input instanceof InputPassPriority || input instanceof InputAttack) {
            input.selectButtonOK();
            playbackRetries = 0;
            return true;
        }
        if (++playbackRetries > MAX_WAIT_RETRIES) {
            stopPlayback("lblMacroPlaybackStoppedWaitingBetweenIterations");
            return true;
        }
        return true;
    }

    private boolean canStartNextIterationBeforeStackClear(final Input input) {
        if (actions.isEmpty()) {
            return false;
        }
        final PlayerAction firstAction = actions.get(0);
        return (firstAction.isSelectionAction()
                && (input instanceof InputSelectTargets || input instanceof InputSelectEntitiesFromList<?>))
                || (firstAction instanceof ConfirmAction && input instanceof InputConfirm)
                || (firstAction instanceof PayManaFromPoolAction && input instanceof InputPayMana);
    }

    private int waitForPendingCardAction(final Input input, final int actionIndex, final boolean waitingForRecordedStackOrder) {
        final PlayerAction action = playbackActions.get(actionIndex);
        if (!isStackEmpty()) {
            final String reason = waitingForRecordedStackOrder ? "waiting for stack before "
                    : "waiting for pending stack item before ";
            return waitForInput(input, reason + action.describe());
        }
        if (isWaitingForPostCombatMainAction(actionIndex)) {
            return waitForInput(input, "waiting for postcombat main before " + action.describe());
        }
        if (isWaitingForAttackDeclaration(actionIndex)) {
            return waitForInput(input, "waiting for attack declaration before " + action.describe());
        }
        return NO_ACTION_ACCEPTED;
    }

    private int waitForInput(final Input input, final String debugMessage) {
        debug(debugMessage);
        return waitForInput(input);
    }

    private int waitForInput(final Input input) {
        input.selectButtonOK();
        return WAIT_FOR_NEXT_INPUT;
    }

    private boolean isWaitingForAttackDeclaration(final int actionIndex) {
        final Card card = findPendingPlayerCard(actionIndex);
        if (card == null || !card.isCreature()) {
            return false;
        }
        final PhaseType phase = getCurrentPhase();
        return phase == PhaseType.COMBAT_BEGIN || phase == PhaseType.COMBAT_DECLARE_ATTACKERS
                || playerControllerHuman.getGame().getPhaseHandler().hasExtraPhaseAfter(phase, PhaseType.COMBAT_BEGIN);
    }

    private boolean isWaitingForPostCombatMainAction(final int actionIndex) {
        if (findPendingPlayerCard(actionIndex) == null) {
            return false;
        }
        final PhaseType phase = getCurrentPhase();
        return phase == PhaseType.COMBAT_DECLARE_BLOCKERS
                || phase == PhaseType.COMBAT_FIRST_STRIKE_DAMAGE
                || phase == PhaseType.COMBAT_DAMAGE
                || phase == PhaseType.COMBAT_END;
    }

    private Card findPendingPlayerCard(final int actionIndex) {
        if (actionIndex < 0 || actionIndex >= playbackActions.size()) {
            return null;
        }
        final Card card = findCard(playbackActions.get(actionIndex).getSelectedCardView());
        return isControlledByPlayer(card) ? card : null;
    }

    private int findNextAction(final Class<?>... actionClasses) {
        return findNextActionBefore(playbackActions.size(), actionClasses);
    }

    private int findNextManaPaymentAction() {
        for (int i = 0; i < playbackActions.size(); i++) {
            final PlayerAction action = playbackActions.get(i);
            if (action instanceof PayManaFromPoolAction || action instanceof ConfirmAction) {
                return i;
            }
            if (!(action instanceof ManaComboAction)) {
                return NO_ACTION_ACCEPTED;
            }
        }
        return NO_ACTION_ACCEPTED;
    }

    private boolean payManaFromPoolBeforeManaCombo(final InputPayMana input, final int manaComboAction) {
        if (input.isPaid() || manaComboAction < 0 || manaComboAction >= playbackActions.size()
                || !(playbackActions.get(manaComboAction) instanceof ManaComboAction action)) {
            return false;
        }
        for (final Map.Entry<Byte, Integer> entry : action.getManaCombo().entrySet()) {
            if (entry.getValue() != null && entry.getValue() > 0 && payManaFromPool(input, entry.getKey())) {
                return true;
            }
        }
        for (final byte color : MagicColor.WUBRGC) {
            if (payManaFromPool(input, color)) {
                return true;
            }
        }
        return false;
    }

    private boolean payManaFromPool(final InputPayMana input, final byte color) {
        input.useManaFromPool(color);
        return input.isPaid();
    }

    private boolean payAnyManaFromPool(final InputPayMana input) {
        if (playerControllerHuman.getPlayer().getManaPool().isEmpty()) {
            return false;
        }
        final int poolBefore = playerControllerHuman.getPlayer().getManaPool().totalMana();
        for (final byte color : MagicColor.WUBRGC) {
            input.useManaFromPool(color);
            if (input.isPaid()
                    || playerControllerHuman.getPlayer().getManaPool().totalMana() < poolBefore) {
                return true;
            }
        }
        return false;
    }

    private int activateLeadingRecordedManaSource() {
        final int size = findLeadingManaSourceSearchSize();
        for (int i = 0; i < size; i++) {
            final PlayerAction action = playbackActions.get(i);
            if (activateRecordedManaSource(findCard(action.getSelectedCardView()), action)) {
                return i;
            }
        }
        return NO_ACTION_ACCEPTED;
    }

    private int findLeadingManaSourceSearchSize() {
        for (int i = 0; i < playbackActions.size(); i++) {
            final PlayerAction action = playbackActions.get(i);
            if (action.getSelectedCardView() == null) {
                return i;
            }
        }
        return playbackActions.size();
    }

    private boolean activateFutureRecordedLandManaSource() {
        final int start = findLeadingManaSourceSearchSize();
        for (int i = start; i < playbackActions.size(); i++) {
            final PlayerAction action = playbackActions.get(i);
            if (activateRecordedManaSource(exactRecordedLandManaSource(action.getSelectedCardView()), action)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasFutureUsableRecordedLandSelection() {
        final int start = findLeadingManaSourceSearchSize();
        for (int i = start; i < playbackActions.size(); i++) {
            if (exactRecordedLandManaSource(playbackActions.get(i).getSelectedCardView()) != null) {
                return true;
            }
        }
        return false;
    }

    private Card exactRecordedLandManaSource(final CardView recordedCard) {
        final Card card = playerControllerHuman.getCard(recordedCard);
        return isControlledByPlayer(card) && !card.isTapped() && card.isLand() ? card : null;
    }

    private boolean activateRecordedManaSource(final Card card, final PlayerAction action) {
        if (card == null || card.isTapped() || card.getManaAbilities().isEmpty()
                || !playerControllerHuman.selectCard(card.getView(), null, replayTriggerEvent)) {
            return false;
        }
        debug("using future mana source " + action.describe());
        return true;
    }

    private boolean isControlledByPlayer(final Card card) {
        return card != null && playerControllerHuman.getPlayer().equals(card.getController());
    }

    private int findNextActionBefore(final int endIndex, final Class<?>... actionClasses) {
        final int size = endIndex < 0 ? playbackActions.size() : Math.min(endIndex, playbackActions.size());
        for (int i = 0; i < size; i++) {
            final PlayerAction action = playbackActions.get(i);
            for (final Class<?> actionClass : actionClasses) {
                if (actionClass.isInstance(action)) {
                    return i;
                }
            }
        }
        return NO_ACTION_ACCEPTED;
    }

    private boolean noRemainingTargetSelectionsBefore(final int endIndex) {
        final int size = Math.min(endIndex, playbackActions.size());
        for (int i = 0; i < size; i++) {
            if (playbackActions.get(i).isSelectionAction()) {
                return false;
            }
        }
        return true;
    }

    private void finishPlayback() {
        finishPlayback(true);
    }

    private void finishPlayback(final boolean clearActiveAction) {
        repeatIteration = 0;
        repeatIterations = 0;
        resetPlaybackProgress();
        if (clearActiveAction) {
            activeActionIndex = -1;
        }
        playbackActions.clear();
        playerControllerHuman.getInputQueue().updateObservers();
        notifyStatusListeners();
    }

    private void restartPlaybackActions() {
        playbackActions.clear();
        playbackActions.addAll(actions);
    }

    private void resetPlaybackProgress() {
        playbackRetries = 0;
        pendingStackOrderPriorityPasses = 0;
        waitingForSelectionAfterStackOrder = false;
    }

    private void stopPlayback(final String reasonKey) {
        final String message = localizer.getMessage("lblMacroPlaybackStoppedAt",
                localizer.getMessage(reasonKey), describeActiveAction(), describeInput());
        playbackMessages.add(message);
        if (DEBUG) {
            debug(message + " remaining=" + describeActions(playbackActions));
        }
        finishPlayback(false);
        playerControllerHuman.getGui().message(localizer.getMessage("lblMacroPlaybackStopped"),
                localizer.getMessage("lblRepeatActionSequence"));
    }

    public boolean processAction(PlayerAction action) {
        setActiveAction(action);
        if (DEBUG) {
            debug("try " + action.describe());
        }
        final Input input = playerControllerHuman.getInputProxy().getInput();
        if (action instanceof ActivateAbilityAction activateAbilityAction) {
            if (input instanceof InputPassPriority passPriorityInput) {
                return debugResult(action, activateAbility(passPriorityInput, activateAbilityAction));
            }
        } else if (action instanceof PassPriorityAction passPriorityAction) {
            if (input instanceof InputPassPriority && passPriorityAction.canReplay(isStackEmpty(), getCurrentPhase())) {
                input.selectButtonOK();
                return debugResult(action, true);
            }
            if (input instanceof InputAttack && passPriorityAction.canReplayDuringAttack(getCurrentPhase())) {
                input.selectButtonOK();
                return debugResult(action, true);
            }
        } else if (action instanceof FinishTargetingAction) {
            if (input instanceof InputSelectTargets || input instanceof InputAttack) {
                input.selectButtonOK();
                return debugResult(action, true);
            }
        } else if (action instanceof PayManaFromPoolAction payManaFromPoolAction) {
            if (input instanceof InputPayMana manaInput) {
                manaInput.useManaFromPool(payManaFromPoolAction.getSelectedColor());
                return debugResult(action, true);
            }
        } else if (action instanceof PayCostAction) {
            if (input instanceof InputConfirm) {
                input.selectButtonOK();
                return debugResult(action, true);
            }
        } else if (action instanceof ConfirmAction confirmAction) {
            if (!confirmAction.isConfirmed() && input instanceof InputPayMana manaInput
                    && manaInput.canCancelPaymentForMacro()) {
                input.selectButtonCancel();
                return debugResult(action, true);
            }
            if (input instanceof InputConfirm confirmInput
                    && confirmAction.matchesPrompt(confirmInput.getCardViewForMacro(),
                            confirmInput.getMessageForMacro())) {
                if (confirmAction.isConfirmed()) {
                    input.selectButtonOK();
                } else {
                    input.selectButtonCancel();
                }
                return debugResult(action, true);
            }
        } else if (action.getGameEntityView() instanceof CardView cardView) {
            return debugResult(action, selectCard(cardView));
        } else if (action.getGameEntityView() instanceof PlayerView playerView) {
            return debugResult(action, selectPlayer(playerView));
        }
        return debugResult(action, false);
    }

    private boolean canAcceptImplicitTriggerConfirm(final InputConfirm input) {
        final String message = input.getMessageForMacro();
        if (message == null || !message.startsWith(localizer.getMessage("lblUseTriggeredAbilityOf"))) {
            return false;
        }
        return findNextAction(SelectCardAction.class, SelectPlayerAction.class) >= 0;
    }

    private boolean activateAbility(final InputPassPriority input, final ActivateAbilityAction action) {
        final GameEntityView view = action.getGameEntityView();
        if (!(view instanceof CardView cardView)) {
            return false;
        }

        final Card card = findCard(cardView);
        return card != null && activateAbility(input, action, card);
    }

    private boolean activateAbility(final InputPassPriority input, final ActivateAbilityAction action, final Card card) {
        for (final SpellAbility ability : card.getAllPossibleAbilities(playerControllerHuman.getPlayer(), true)) {
            if (ability.toUnsuppressedString().equals(action.getAbilityDescription())) {
                return input.selectAbility(ability);
            }
        }
        return false;
    }

    private boolean selectPlayer(final PlayerView playerView) {
        final Input inp = playerControllerHuman.getInputProxy().getInput();
        if (!(inp instanceof InputSelectTargets targetInput)) {
            return false;
        }
        final Player player = playerControllerHuman.getGame().getPlayer(playerView);
        return player != null && targetInput.selectPlayerForMacro(player, replayTriggerEvent);
    }

    private boolean selectCard(final CardView cardView) {
        final Input inp = playerControllerHuman.getInputProxy().getInput();
        if (inp instanceof InputSelectEntitiesFromList<?> selectInput) {
            final Card choice = findListChoice(cardView, selectInput);
            return choice != null && playerControllerHuman.selectCard(choice.getView(), null, replayTriggerEvent);
        }

        final Card directCard = findCard(cardView);
        if (inp instanceof InputSelectTargets targetInput) {
            final Card targetChoice = findTargetChoice(cardView, directCard, targetInput);
            return targetChoice != null && targetInput.selectCardForMacro(targetChoice, replayTriggerEvent);
        }

        if (inp instanceof InputPassPriority passPriorityInput) {
            if (directCard == null) {
                return false;
            }
            final List<SpellAbility> abilities = directCard.getAllPossibleAbilities(playerControllerHuman.getPlayer(), true);
            if (abilities.size() == 1) {
                return passPriorityInput.selectAbility(abilities.get(0));
            }
            return playerControllerHuman.selectCard(directCard.getView(), null, replayTriggerEvent)
                    && passPriorityInput.getChosenSa() != null;
        }
        if (inp instanceof InputAttack attackInput && directCard != null
                && attackInput.isDeclaredAttackerForMacro(directCard)) {
            return false;
        }
        if (directCard != null && playerControllerHuman.selectCard(directCard.getView(), null, replayTriggerEvent)) {
            return true;
        }

        return false;
    }

    private Card findTargetChoice(final CardView recordedChoice, final Card exactChoice,
                                  final InputSelectTargets targetInput) {
        final Card choice = findCardChoice(recordedChoice, exactChoice, targetInput.getValidCardsForMacro());
        debugNoMatch(choice, "target", recordedChoice, null);
        return choice;
    }

    private Card findListChoice(final CardView recordedChoice, final InputSelectEntitiesFromList<?> selectInput) {
        final Card exactChoice = playerControllerHuman.getCard(recordedChoice);
        if (recordedChoice == null) {
            return null;
        }
        final Card exactListChoice = findExactCardChoice(exactChoice, selectInput.getValidChoices());
        if (exactListChoice != null) {
            return exactListChoice;
        }
        if (isRecordedBattlefieldLandChoice(recordedChoice, selectInput)) {
            return null;
        }
        final Card choice = findCardChoice(recordedChoice, exactChoice, selectInput.getValidChoices());
        debugNoMatch(choice, "list", recordedChoice, selectInput.getValidChoices());
        return choice;
    }

    private Card findCardChoice(final CardView recordedChoice, final Card exactChoice, final Iterable<?> choices) {
        final Card exactListChoice = findExactCardChoice(exactChoice, choices);
        if (exactListChoice != null) {
            return exactListChoice;
        }
        if (recordedChoice == null) {
            return null;
        }
        final Card equivalent = findCardChoice(recordedChoice, choices, false);
        return equivalent == null ? findCardChoice(recordedChoice, choices, true) : equivalent;
    }

    private Card findExactCardChoice(final Card exactChoice, final Iterable<?> choices) {
        if (exactChoice == null) {
            return null;
        }
        for (final Object choice : choices) {
            if (choice == exactChoice) {
                return exactChoice;
            }
        }
        return null;
    }

    private Card findCardChoice(final CardView recordedChoice, final Iterable<?> choices, final boolean tokenMatch) {
        for (final Object choice : choices) {
            if (choice instanceof Card card
                    && (tokenMatch ? isEquivalentToken(recordedChoice, card) : isEquivalentCard(recordedChoice, card))) {
                return card;
            }
        }
        return null;
    }

    private void debugNoMatch(final Card choice, final String choiceType, final CardView recordedChoice,
                              final Object choices) {
        if (choice == null && recordedChoice != null) {
            debug("no " + choiceType + " match for " + recordedChoice
                    + (choices == null ? "" : " choices=" + choices));
        }
    }

    private boolean isRecordedBattlefieldLandChoice(final CardView recordedChoice,
                                                    final InputSelectEntitiesFromList<?> selectInput) {
        if (exactRecordedLandManaSource(recordedChoice) == null) {
            return false;
        }
        for (final GameEntity validChoice : selectInput.getValidChoices()) {
            if (validChoice instanceof Card card && card.isInZone(ZoneType.Battlefield)) {
                return true;
            }
        }
        return false;
    }

    private boolean isEquivalentToken(final CardView original, final Card candidate) {
        return isTokenLike(original)
                && isTokenLike(candidate.getView())
                && hasSameController(original, candidate)
                && normalizeTokenName(original.getName()).equals(normalizeTokenName(candidate.getView().getName()));
    }

    private boolean isTokenLike(final CardView card) {
        return card.isToken() || card.getName().endsWith(" Token");
    }

    private String normalizeTokenName(final String name) {
        String normalized = name;
        while (normalized.endsWith(" Token")) {
            normalized = normalized.substring(0, normalized.length() - " Token".length());
        }
        return normalized;
    }

    private boolean hasSameController(final CardView original, final Card candidate) {
        return original.getController() == null || original.getController().equals(candidate.getView().getController());
    }

    private Card findCard(final CardView cardView) {
        if (cardView == null) {
            return null;
        }
        final Card directCard = playerControllerHuman.getCard(cardView);
        if (directCard != null) {
            return directCard;
        }
        for (final Card card : playerControllerHuman.getGame().getCardsInGame()) {
            if (isEquivalentCard(cardView, card)) {
                return card;
            }
        }
        return null;
    }

    private boolean isEquivalentCard(final CardView original, final Card candidate) {
        return original.getName().equals(candidate.getView().getName())
                && original.isToken() == candidate.isToken()
                && hasSameController(original, candidate);
    }

    private boolean debugResult(final PlayerAction action, final boolean result) {
        if (DEBUG) {
            debug((result ? "accepted " : "rejected ") + action.describe());
        }
        if (!result && repeatIterations > 0 && playbackRetries == MAX_REJECTED_ACTION_RETRIES) {
            playbackMessages.add(localizer.getMessage("lblMacroCouldNotReplay", action.describe(), describeInput()));
            notifyStatusListeners();
        }
        return result;
    }

    private void setActiveAction(final PlayerAction action) {
        final int index = action == null ? -1 : actions.indexOf(action);
        if (activeActionIndex == index) {
            return;
        }
        activeActionIndex = index;
        notifyStatusListeners();
    }

    private String describeActiveAction() {
        if (activeActionIndex >= 0 && activeActionIndex < actions.size()) {
            return localizer.getMessage("lblMacroStepDescription",
                    activeActionIndex + 1, actions.get(activeActionIndex).describe());
        }
        return localizer.getMessage("lblMacroNextRecordedStep");
    }

    private void debug(final String message) {
        if (DEBUG) {
            System.out.println(DEBUG_PREFIX + message);
        }
    }

    private void debugPlaybackState() {
        if (DEBUG) {
            debug("input=" + describeInput() + " remaining=" + describeActions(playbackActions));
        }
    }

    private String describeInput() {
        final Input inp = playerControllerHuman.getInputProxy().getInput();
        return inp == null ? "none" : inp.getClass().getSimpleName();
    }

    private String describeActions(final List<PlayerAction> actionList) {
        final StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < actionList.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(actionList.get(i).describe());
        }
        sb.append("]");
        return sb.toString();
    }

    private class DummyTriggerEvent implements ITriggerEvent {
        @Override
        public int getButton() {
            return 1; // Emulate left mouse button
        }

        @Override
        public int getX() {
            return 0;
        }

        @Override
        public int getY() {
            return 0;
        }
    }
}
