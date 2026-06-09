package forge.player;

import forge.game.GameEntityView;
import forge.game.card.Card;
import forge.game.card.CardView;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.PlayerView;
import forge.game.player.actions.ActivateAbilityAction;
import forge.game.player.actions.ConfirmAction;
import forge.game.player.actions.FinishTargetingAction;
import forge.game.player.actions.PassPriorityAction;
import forge.game.player.actions.PayCostAction;
import forge.game.player.actions.PayManaFromPoolAction;
import forge.game.player.actions.PlayerAction;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.gamemodes.match.input.Input;
import forge.gamemodes.match.input.InputAttack;
import forge.gamemodes.match.input.InputConfirm;
import forge.gamemodes.match.input.InputPassPriority;
import forge.gamemodes.match.input.InputPayMana;
import forge.gamemodes.match.input.InputSelectEntitiesFromList;
import forge.gamemodes.match.input.InputSelectTargets;
import forge.util.ITriggerEvent;

import java.util.List;
import java.util.function.Consumer;

class MacroActionReplayer {
    private static final ITriggerEvent REPLAY_TRIGGER_EVENT = new DummyTriggerEvent();

    private final PlayerControllerHuman playerControllerHuman;
    private final Consumer<String> debug;

    MacroActionReplayer(final PlayerControllerHuman playerControllerHuman, final Consumer<String> debug) {
        this.playerControllerHuman = playerControllerHuman;
        this.debug = debug;
    }

    boolean replay(final PlayerAction action) {
        final Input input = playerControllerHuman.getInputProxy().getInput();
        if (action instanceof ActivateAbilityAction activateAbilityAction) {
            return input instanceof InputPassPriority passPriorityInput
                    && activateAbility(passPriorityInput, activateAbilityAction);
        }
        if (action instanceof PassPriorityAction passPriorityAction) {
            return replayPassPriority(input, passPriorityAction);
        }
        if (action instanceof FinishTargetingAction) {
            return selectOkIf(input, input instanceof InputSelectTargets || input instanceof InputAttack);
        }
        if (action instanceof PayManaFromPoolAction payManaFromPoolAction) {
            if (input instanceof InputPayMana manaInput) {
                manaInput.useManaFromPool(payManaFromPoolAction.getSelectedColor());
                return true;
            }
            return false;
        }
        if (action instanceof PayCostAction) {
            return selectOkIf(input, input instanceof InputConfirm);
        }
        if (action instanceof ConfirmAction confirmAction) {
            return replayConfirm(input, confirmAction);
        }
        if (action.getGameEntityView() instanceof CardView cardView) {
            return selectCard(cardView);
        }
        if (action.getGameEntityView() instanceof PlayerView playerView) {
            return selectPlayer(playerView);
        }
        return false;
    }

    private boolean replayPassPriority(final Input input, final PassPriorityAction action) {
        return selectOkIf(input, (input instanceof InputPassPriority && action.canReplay(isStackEmpty(), getCurrentPhase()))
                || (input instanceof InputAttack && action.canReplayDuringAttack(getCurrentPhase())));
    }

    private boolean replayConfirm(final Input input, final ConfirmAction action) {
        if (!action.isConfirmed() && input instanceof InputPayMana manaInput
                && manaInput.canCancelPaymentForMacro()) {
            input.selectButtonCancel();
            return true;
        }
        if (input instanceof InputConfirm confirmInput
                && action.matchesPrompt(confirmInput.getCardViewForMacro(), confirmInput.getMessageForMacro())) {
            if (action.isConfirmed()) {
                input.selectButtonOK();
            } else {
                input.selectButtonCancel();
            }
            return true;
        }
        return false;
    }

    private boolean selectOkIf(final Input input, final boolean canSelect) {
        if (canSelect) {
            input.selectButtonOK();
        }
        return canSelect;
    }

    private PhaseType getCurrentPhase() {
        return playerControllerHuman.getGame().getPhaseHandler().getPhase();
    }

    private boolean isStackEmpty() {
        return playerControllerHuman.getGame().getStack().isEmpty();
    }

    Card findCard(final CardView cardView) {
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

    Card exactRecordedLandManaSource(final CardView recordedCard) {
        final Card card = playerControllerHuman.getCard(recordedCard);
        return isControlledByPlayer(card) && !card.isTapped() && card.isLand() ? card : null;
    }

    boolean isControlledByPlayer(final Card card) {
        return card != null && playerControllerHuman.getPlayer().equals(card.getController());
    }

    boolean selectManaSource(final Card card, final PlayerAction action) {
        if (card == null || card.isTapped() || card.getManaAbilities().isEmpty()
                || !selectCardForMacro(card)) {
            return false;
        }
        debug.accept("using future mana source " + action.describe());
        return true;
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
        final Input input = playerControllerHuman.getInputProxy().getInput();
        if (!(input instanceof InputSelectTargets targetInput)) {
            return false;
        }
        final Player player = playerControllerHuman.getGame().getPlayer(playerView);
        return player != null && targetInput.selectPlayerForMacro(player, REPLAY_TRIGGER_EVENT);
    }

    private boolean selectCard(final CardView cardView) {
        final Input input = playerControllerHuman.getInputProxy().getInput();
        if (input instanceof InputSelectEntitiesFromList<?> selectInput) {
            final Card choice = findListChoice(cardView, selectInput);
            return selectCardForMacro(choice);
        }

        final Card directCard = findCard(cardView);
        if (input instanceof InputSelectTargets targetInput) {
            final Card targetChoice = findTargetChoice(cardView, directCard, targetInput);
            return targetChoice != null && targetInput.selectCardForMacro(targetChoice, REPLAY_TRIGGER_EVENT);
        }

        if (input instanceof InputPassPriority passPriorityInput) {
            if (directCard == null) {
                return false;
            }
            final List<SpellAbility> abilities = directCard.getAllPossibleAbilities(playerControllerHuman.getPlayer(), true);
            if (abilities.size() == 1) {
                return passPriorityInput.selectAbility(abilities.get(0));
            }
            return selectCardForMacro(directCard) && passPriorityInput.getChosenSa() != null;
        }
        if (input instanceof InputAttack attackInput && directCard != null
                && attackInput.isDeclaredAttackerForMacro(directCard)) {
            return false;
        }
        return selectCardForMacro(directCard);
    }

    private boolean selectCardForMacro(final Card card) {
        return card != null && playerControllerHuman.selectCard(card.getView(), null, REPLAY_TRIGGER_EVENT);
    }

    private Card findTargetChoice(final CardView recordedChoice, final Card exactChoice,
                                  final InputSelectTargets targetInput) {
        final Card choice = findCardChoice(recordedChoice, exactChoice, targetInput.getValidCardsForMacro());
        debugNoMatch(choice, "target", recordedChoice, null);
        return choice;
    }

    private Card findListChoice(final CardView recordedChoice, final InputSelectEntitiesFromList<?> selectInput) {
        if (recordedChoice == null) {
            return null;
        }
        final Card exactChoice = playerControllerHuman.getCard(recordedChoice);
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
            debug.accept("no " + choiceType + " match for " + recordedChoice
                    + (choices == null ? "" : " choices=" + choices));
        }
    }

    private boolean isRecordedBattlefieldLandChoice(final CardView recordedChoice,
                                                    final InputSelectEntitiesFromList<?> selectInput) {
        if (exactRecordedLandManaSource(recordedChoice) == null) {
            return false;
        }
        for (final Object validChoice : selectInput.getValidChoices()) {
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

    private boolean isEquivalentCard(final CardView original, final Card candidate) {
        return original.getName().equals(candidate.getView().getName())
                && original.isToken() == candidate.isToken()
                && hasSameController(original, candidate);
    }

    private static class DummyTriggerEvent implements ITriggerEvent {
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
