/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.gamemodes.match.input;

import forge.game.Game;
import forge.game.GameView;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.PlayerView;
import forge.game.player.actions.PassPriorityAction;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.StackItemView;
import forge.gamemodes.match.DeclineScope;
import forge.gamemodes.match.SuggestionType;
import forge.gamemodes.match.YieldController;
import forge.gamemodes.match.YieldUpdate;
import forge.gamemodes.net.server.FServerManager;
import forge.gamemodes.net.server.FServerManager.AfkTimeout;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.util.collect.FCollectionView;
import forge.model.FModel;
import forge.player.GamePlayerUtil;
import forge.player.PlayerControllerHuman;
import forge.util.ITriggerEvent;
import forge.util.Localizer;
import forge.util.ThreadUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * Input_PassPriority class.
 * </p>
 * 
 * @author Forge
 * @version $Id: InputPassPriority.java 24769 2014-02-09 13:56:04Z Hellfish $
 */
public class InputPassPriority extends InputSyncronizedBase {
    /** Constant <code>serialVersionUID=-581477682214137181L</code>. */
    private static final long serialVersionUID = -581477682214137181L;

    private List<SpellAbility> chosenSa;

    private SuggestionType pendingSuggestion = null;
    private String pendingSuggestionMessage = null;

    public InputPassPriority(final PlayerControllerHuman controller) {
        super(controller);
    }

    @Override
    public void showAndWait() {
        final FServerManager server = FServerManager.getInstance();
        final AfkTimeout timeout = server != null
                ? server.armAfkTimeout(getController(), this)
                : AfkTimeout.NOOP;
        try {
            super.showAndWait();
        } finally {
            timeout.cancel();
        }
    }

    /** {@inheritDoc} */
    @Override
    public final void showMessage() {
        if (!isAlreadyYielding()) {
            // Suppress one prompt after a yield ends — avoids "yielded → ended → yield again?" loop
            if (getController().getYieldController().getBoolPref(FPref.YIELD_SUPPRESS_AFTER_END)
                    && getController().getYieldController().didYieldJustEnd()) {
                showNormalPrompt();
                return;
            }

            if (getController().getYieldController().getBoolPref(FPref.YIELD_AUTO_PASS_NO_ACTIONS)) {
                showNormalPrompt();
                return;
            }

            // Both scopes NEVER → skip including stack-transition tracking (no decline state to maintain)
            DeclineScope stackScope = getController().getYieldController().getDeclineScope(FPref.YIELD_DECLINE_SCOPE_STACK_YIELD);
            DeclineScope noActionsScope = getController().getYieldController().getDeclineScope(FPref.YIELD_DECLINE_SCOPE_NO_ACTIONS);
            if (stackScope == DeclineScope.NEVER && noActionsScope == DeclineScope.NEVER) {
                showNormalPrompt();
                return;
            }

            GameView gvForStack = getGameView();
            boolean stackNonEmpty = gvForStack != null && gvForStack.getStack() != null
                    && !gvForStack.getStack().isEmpty();
            getController().getYieldController().onPriorityReceived(stackNonEmpty);

            Localizer loc = Localizer.getInstance();

            if (!getController().getYieldController().isSuggestionDeclined(SuggestionType.STACK_YIELD)
                    && shouldShowStackYieldPrompt()) {
                pendingSuggestion = SuggestionType.STACK_YIELD;
                pendingSuggestionMessage = loc.getMessage("lblCannotRespondToStackYieldPrompt");
                showYieldSuggestionPrompt();
                return;
            }
            if (!getController().getYieldController().isSuggestionDeclined(SuggestionType.NO_ACTIONS)
                    && shouldShowNoActionsPrompt()) {
                pendingSuggestion = SuggestionType.NO_ACTIONS;
                pendingSuggestionMessage = loc.getMessage("lblNoActionsAvailableYieldPrompt");
                showYieldSuggestionPrompt();
                return;
            }
        }

        showNormalPrompt();
    }

    private void showYieldSuggestionPrompt() {
        // State may have flipped between the initial check and now (e.g. async multiplayer click).
        if (isAlreadyYielding()) {
            pendingSuggestion = null;
            pendingSuggestionMessage = null;
            showNormalPrompt();
            return;
        }

        Localizer loc = Localizer.getInstance();
        String fullMessage = pendingSuggestionMessage;
        DeclineScope scope = getController().getYieldController().getDeclineScope(pendingSuggestion.scopePref());
        if (scope == DeclineScope.STACK) {
            fullMessage += "\n" + loc.getMessage("lblYieldSuggestionDeclineHintStack");
        } else if (scope == DeclineScope.TURN) {
            fullMessage += "\n" + loc.getMessage("lblYieldSuggestionDeclineHint");
        }
        showMessage(fullMessage);
        chosenSa = null;
        getController().getGui().updateButtons(getOwner(),
                loc.getMessage("lblAccept"),
                loc.getMessage("lblDecline"),
                true, true, true);
        getController().getGui().alertUser();
    }

    private void showNormalPrompt() {
        pendingSuggestion = null;
        pendingSuggestionMessage = null;

        showMessage(getTurnPhasePriorityMessage(getController().getGame()));
        chosenSa = null;
        Localizer localizer = Localizer.getInstance();
        if (getController().canUndoLastAction()) { //allow undoing with cancel button if can undo last action
            getController().getGui().updateButtons(getOwner(), localizer.getMessage("lblOK"), localizer.getMessage("lblUndo") + " (" + getController().getGame().getStack().getUndoStackSize() + ")", true, true, true);
        }
        else { //otherwise allow ending turn with cancel button
            getController().getGui().updateButtons(getOwner(), localizer.getMessage("lblOK"), localizer.getMessage("lblEndTurn"), true, true, true);
        }

        getController().getGui().alertUser();
    }

    private boolean isAlreadyYielding() {
        return getController().getYieldController().isYieldActive();
    }

    private GameView getGameView() {
        return getController().getGui().getGameView();
    }

    private PlayerView getPlayerView() {
        return getOwner();
    }

    private boolean checkHasAvailableActions() {
        Player player = getController().getPlayer();
        if (player == null) return false;
        // Freshened upstream in chooseSpellAbilityToPlay; don't recompute
        return player.getView().hasAvailableActions();
    }

    private boolean shouldShowStackYieldPrompt() {
        GameView gv = getGameView();
        if (gv == null) return false;
        FCollectionView<StackItemView> stack = gv.getStack();
        if (stack == null || stack.isEmpty()) return false;
        return !checkHasAvailableActions();
    }

    /** Stack non-empty disqualifies; SUPPRESS_ON_OWN_TURN suppresses on own turn (after first round). */
    private boolean isValidSuggestionContext(GameView gv, PlayerView pv) {
        FCollectionView<StackItemView> stack = gv.getStack();
        if (stack != null && !stack.isEmpty()) return false;
        PlayerView currentTurn = gv.getPlayerTurn();
        if (currentTurn != null && currentTurn.equals(pv)) {
            // Always suppress on player's first turn (no lands/mana yet)
            int numPlayers = gv.getPlayers().size();
            if (gv.getTurn() <= numPlayers) return false;
            if (getController().getYieldController().getBoolPref(FPref.YIELD_SUPPRESS_ON_OWN_TURN)) return false;
        }
        return true;
    }

    private boolean shouldShowNoActionsPrompt() {
        GameView gv = getGameView();
        PlayerView pv = getPlayerView();
        if (gv == null || pv == null) return false;
        if (!isValidSuggestionContext(gv, pv)) return false;
        return !checkHasAvailableActions();
    }

    /** {@inheritDoc} */
    @Override
    protected final void onOk() {
        if (pendingSuggestion != null) {
            // Defensive: state may have flipped (e.g. async multiplayer click).
            if (isAlreadyYielding()) {
                pendingSuggestion = null;
                pendingSuggestionMessage = null;
                stop();
                return;
            }
            // APINA flipped on between display and accept — drop the now-redundant accept (host-local only)
            if (!getController().isRemoteClient()
                    && getController().getYieldController().getBoolPref(FPref.YIELD_AUTO_PASS_NO_ACTIONS)) {
                pendingSuggestion = null;
                pendingSuggestionMessage = null;
                stop();
                return;
            }
            SuggestionType accepted = pendingSuggestion;
            pendingSuggestion = null;
            pendingSuggestionMessage = null;

            PlayerView self = getPlayerView();
            YieldController yc = getController().getYieldController();
            if (accepted == SuggestionType.STACK_YIELD) {
                yc.setAutoPassUntilStackEmpty(true);
                if (self != null) getController().getGui().applyYieldUpdate(
                        new YieldUpdate.StackYield(self, true));
            } else if (accepted == SuggestionType.NO_ACTIONS) {
                // UPKEEP because UNTAP has no priority pass — a marker on UNTAP could never fire.
                if (self != null) {
                    boolean atOrPast = YieldController.isPriorityAtOrPastMarker(
                            getGameView(), self, PhaseType.UPKEEP);
                    yc.setMarker(self, PhaseType.UPKEEP, atOrPast);
                    getController().getGui().applyYieldUpdate(
                            new YieldUpdate.SetMarker(self, PhaseType.UPKEEP, atOrPast));
                }
            }
            if (isAlreadyYielding()) {
                stop();
            } else {
                showNormalPrompt();
            }
            return;
        }

        passPriority(() -> {
            getController().macros().addRememberedAction(new PassPriorityAction());
            stop();
        });
    }

    /** {@inheritDoc} */
    @Override
    protected final void onCancel() {
        if (pendingSuggestion != null) {
            getController().getYieldController().declineSuggestion(pendingSuggestion);
            pendingSuggestion = null;
            pendingSuggestionMessage = null;
            showNormalPrompt();
            return;
        }

        if (!getController().tryUndoLastAction()) { //undo if possible
            //otherwise end turn
            passPriority(() -> {
                getController().autoPassUntilEndOfTurn();
                stop();
            });
        }
    }

    @Override
    protected boolean allowAwaitNextInput() {
        return chosenSa == null && !getController().mayAutoPass(); //don't allow awaiting next input if player chose to end the turn or if a spell/ability is chosen
    }

    private void passPriority(final Runnable runnable) {
        if (FModel.getPreferences().getPrefBoolean(FPref.UI_MANA_LOST_PROMPT)) {
            //if gui player has mana floating that will be lost if phase ended right now, prompt before passing priority
            final Game game = getController().getGame();
            if (game.getStack().isEmpty()) { //phase can't end right now if stack isn't empty
                Player player = game.getPhaseHandler().getPriorityPlayer();
                if (player != null && player.getManaPool().willManaBeLostAtEndOfPhase() && player.getLobbyPlayer() == GamePlayerUtil.getGuiPlayer()) {
                    //must invoke in game thread so dialog can be shown on mobile game
                    ThreadUtil.invokeInGameThread(() -> {
                        Localizer localizer = Localizer.getInstance();
                        String message = localizer.getMessage("lblYouHaveManaFloatingInYourManaPoolCouldBeLostIfPassPriority");
                        if (player.getManaPool().hasBurn()) {
                            message += " " + localizer.getMessage("lblYouWillTakeManaBurnDamageEqualAmountFloatingManaLostThisWay");
                        }
                        if (getController().getGui().showConfirmDialog(message, localizer.getMessage("lblManaFloating"), localizer.getMessage("lblOK"), localizer.getMessage("lblCancel"))) {
                            runnable.run();
                        }
                    });
                    return;
                }
            }
        }
        runnable.run(); //just pass priority immediately if no mana floating that would be lost
    }

    public List<SpellAbility> getChosenSa() { return chosenSa; }

    @Override
    protected boolean onCardSelected(final Card card, final List<Card> otherCardsToSelect, final ITriggerEvent triggerEvent) {
        // remove unplayable unless triggerEvent specified, in which case unplayable may be shown as disabled options
        // (so shortcuts are constant regardless of game state)
        List<SpellAbility> abilities = card.getAllPossibleAbilities(getController().getPlayer(), triggerEvent == null); 
        if (abilities.isEmpty()) {
            return false;
        }

        final SpellAbility ability = getController().getAbilityToPlay(card, abilities, triggerEvent);
        if (ability != null) {
            chosenSa = new ArrayList<>();
            chosenSa.add(ability);
            if (otherCardsToSelect != null && ability.isManaAbility()) {
                //if mana ability activated, activate same ability on other cards to select if possible
                String abStr = ability.toUnsuppressedString();
                for (Card c : otherCardsToSelect) {
                    for (SpellAbility ab : c.getAllPossibleAbilities(getController().getPlayer(), true)) {
                        if (ab.toUnsuppressedString().equals(abStr)) {
                            chosenSa.add(ab);
                            break;
                        }
                    }
                }
            }
            stop();
    	}
        return true; //still return true if user cancelled selecting an ability to prevent selecting another card
    }

    @Override
    public String getActivateAction(final Card card) {
        final List<SpellAbility> abilities = card.getAllPossibleAbilities(getController().getPlayer(), true); 
        if (abilities.isEmpty()) {
            return null;
        }
        final SpellAbility sa = abilities.get(0);
        if (sa.isSpell()) {
            return Localizer.getInstance().getMessage("lblCastSpell");
        }
        if (sa.isLandAbility()) {
            return Localizer.getInstance().getMessage("lblPlayLand");
        }
        return Localizer.getInstance().getMessage("lblActivateAbility");
    }

    @Override
    public boolean selectAbility(final SpellAbility ab) {
    	if (ab != null) {
    	    chosenSa = new ArrayList<>();
            chosenSa.add(ab);
            stop();
            return true;
        }
    	return false;
    }
}
