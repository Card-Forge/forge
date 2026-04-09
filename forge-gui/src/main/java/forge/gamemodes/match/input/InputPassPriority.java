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

import forge.ai.ComputerUtilMana;
import forge.game.Game;
import forge.game.GameView;
import forge.game.mana.ManaPool;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.player.PlayerView;
import forge.game.player.actions.PassPriorityAction;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.StackItemView;
import forge.gamemodes.match.YieldMode;
import forge.gui.GuiBase;
import forge.localinstance.properties.ForgePreferences;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.player.GamePlayerUtil;
import forge.player.PlayerControllerHuman;
import forge.util.ITriggerEvent;
import forge.util.Localizer;
import forge.util.ThreadUtil;
import forge.util.collect.FCollectionView;

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

    // Pending yield suggestion state for prompt integration
    private YieldMode pendingSuggestion = null;
    private String pendingSuggestionType = null; // "STACK_YIELD", "NO_ACTIONS"
    private String pendingSuggestionMessage = null;

    public InputPassPriority(final PlayerControllerHuman controller) {
        super(controller);
    }

    /** {@inheritDoc} */
    @Override
    public final void showMessage() {
        // Check if experimental yield features are enabled and show smart suggestions
        // Only show suggestions if not already yielding
        // Check if yield just ended and suppression is enabled
        boolean suppressDueToYieldEnd = FModel.getPreferences().getPrefBoolean(FPref.YIELD_SUPPRESS_AFTER_END)
            && getController().didYieldJustEnd();

        if (isExperimentalYieldEnabled() && !isAlreadyYielding() && !suppressDueToYieldEnd) {
            ForgePreferences prefs = FModel.getPreferences();
            Localizer loc = Localizer.getInstance();

            // Track stack transitions for per-stack decline scope
            GameView gvForStack = getGameView();
            boolean stackNonEmpty = gvForStack != null && gvForStack.getStack() != null
                && !gvForStack.getStack().isEmpty();
            getController().onPriorityReceived(stackNonEmpty);

            // Suggestion 1: Stack items but can't respond
            if (shouldShowStackYieldPrompt()
                && !getController().isSuggestionDeclined("STACK_YIELD")) {
                pendingSuggestion = YieldMode.UNTIL_STACK_CLEARS;
                pendingSuggestionType = "STACK_YIELD";
                pendingSuggestionMessage = loc.getMessage("lblCannotRespondToStackYieldPrompt");
                showYieldSuggestionPrompt();
                return;
            }
            // Suggestion 2: No available actions (empty hand, no abilities)
            if (shouldShowNoActionsPrompt()
                && !getController().isSuggestionDeclined("NO_ACTIONS")) {
                pendingSuggestion = getDefaultYieldMode();
                pendingSuggestionType = "NO_ACTIONS";
                pendingSuggestionMessage = loc.getMessage("lblNoActionsAvailableYieldPrompt");
                showYieldSuggestionPrompt();
                return;
            }
        }

        showNormalPrompt();
    }

    private void showYieldSuggestionPrompt() {
        // Double-check yield state right before showing - it may have been set
        // between the initial check and now (e.g., async button click in multiplayer)
        if (isAlreadyYielding()) {
            pendingSuggestion = null;
            pendingSuggestionType = null;
            pendingSuggestionMessage = null;
            showNormalPrompt();
            return;
        }

        Localizer loc = Localizer.getInstance();
        String fullMessage = pendingSuggestionMessage;
        // Append decline hint based on per-type scope setting
        FPref scopePref = "STACK_YIELD".equals(pendingSuggestionType)
            ? FPref.YIELD_DECLINE_SCOPE_STACK_YIELD
            : FPref.YIELD_DECLINE_SCOPE_NO_ACTIONS;
        String scope = FModel.getPreferences().getPref(scopePref);
        if ("stack".equals(scope)) {
            fullMessage += "\n" + loc.getMessage("lblYieldSuggestionDeclineHintStack");
        } else if ("turn".equals(scope)) {
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
        pendingSuggestionType = null;
        pendingSuggestionMessage = null;

        showMessage(getTurnPhasePriorityMessage(getController().getGame()));
        chosenSa = null;
        Localizer localizer = Localizer.getInstance();
        if (getController().canUndoLastAction()) { //allow undoing with cancel button if can undo last action
            getController().getGui().updateButtons(getOwner(), localizer.getMessage("lblOK"), localizer.getMessage("lblUndo"), true, true, true);
        }
        else { //otherwise allow ending turn with cancel button
            getController().getGui().updateButtons(getOwner(), localizer.getMessage("lblOK"), localizer.getMessage("lblEndTurn"), true, true, true);
        }

        getController().getGui().alertUser();
    }

    private boolean isAlreadyYielding() {
        YieldMode currentMode = getController().getGui().getYieldMode(getOwner());
        return currentMode != null && currentMode != YieldMode.NONE;
    }

    /** {@inheritDoc} */
    @Override
    protected final void onOk() {
        // If accepting a yield suggestion (but not if a yield was already set externally)
        if (pendingSuggestion != null) {
            // Check if a yield mode was already set (e.g., by clicking a yield button)
            YieldMode currentMode = getController().getGui().getYieldMode(getOwner());
            if (currentMode != null && currentMode != YieldMode.NONE) {
                // A yield mode is already active - clear suggestion and pass through
                pendingSuggestion = null;
                pendingSuggestionType = null;
                pendingSuggestionMessage = null;
                stop();
                return;
            }
            // CYield.toggleAutoPass enables the pref then calls selectButtonOk to advance
            // the current input. If we reach onOk with a pending suggestion AND the pref
            // is now ON, the user just toggled — the suggestion couldn't have appeared
            // with the pref already on (mayAutoPass would have caught it). Suppress the
            // accidental suggestion accept and just stop the input.
            // Skip for remote proxies: the host's local pref doesn't apply to remote
            // players, who can't toggle it via shortcut anyway, so this guard would
            // produce a false positive on every Accept click from a remote client.
            if (!getController().getGui().isRemoteGuiProxy()
                    && FModel.getPreferences().getPrefBoolean(FPref.YIELD_AUTO_PASS_NO_ACTIONS)) {
                pendingSuggestion = null;
                pendingSuggestionType = null;
                pendingSuggestionMessage = null;
                stop();
                return;
            }

            YieldMode mode = pendingSuggestion;
            pendingSuggestion = null;
            pendingSuggestionType = null;
            pendingSuggestionMessage = null;
            boolean activated = getController().getGui().setYieldMode(getOwner(), mode);
            if (activated) {
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
        // If declining a yield suggestion, track the decline and show normal prompt
        if (pendingSuggestion != null) {
            // Track that this suggestion was declined for this turn
            if (pendingSuggestionType != null) {
                getController().declineSuggestion(pendingSuggestionType);
            }
            pendingSuggestion = null;
            pendingSuggestionType = null;
            pendingSuggestionMessage = null;
            showNormalPrompt();
            return;
        }

        if (!getController().tryUndoLastAction()) { //undo if possible
            //otherwise end turn
            passPriority(() -> {
                if (isExperimentalYieldEnabled()) {
                    // Use experimental yield system with smart interrupts
                    getController().getGui().setYieldMode(getOwner(), YieldMode.UNTIL_END_OF_TURN);
                } else {
                    // Legacy behavior - cancels on any opponent spell
                    getController().autoPassUntilEndOfTurn();
                }
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
            PlayerView pv = getPlayerView();
            if (game != null && pv != null && pv.isLobbyPlayer(GamePlayerUtil.getGuiPlayer())) {
                GameView gv = getGameView();
                FCollectionView<StackItemView> stack = gv != null ? gv.getStack() : null;
                Player livePlayer = game.getPhaseHandler().getPriorityPlayer();
                if ((stack == null || stack.isEmpty()) &&
                    livePlayer != null && livePlayer.getManaPool().willManaBeLostAtEndOfPhase()) {
                    ThreadUtil.invokeInGameThread(() -> {
                        Localizer localizer = Localizer.getInstance();
                        String manaDesc = buildManaDescription(livePlayer.getManaPool());
                        String phaseName = game.getPhaseHandler().getPhase().nameForUi;
                        String message = localizer.getMessage("lblManaFloatingWithAmount", manaDesc, phaseName);
                        if (livePlayer.getManaPool().hasBurn()) {
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

    private static String buildManaDescription(ManaPool pool) {
        StringBuilder sb = new StringBuilder();
        byte[] types = forge.card.mana.ManaAtom.MANATYPES;
        String[] symbols = {"{W}", "{U}", "{B}", "{R}", "{G}", "{C}"};
        for (int i = 0; i < types.length; i++) {
            int amount = pool.getAmountOfColor(types[i]);
            if (amount == 0) continue;
            if (amount <= 3) {
                for (int j = 0; j < amount; j++) sb.append(symbols[i]);
            } else {
                sb.append(symbols[i]).append("x").append(amount);
            }
        }
        return sb.toString();
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

    // Smart yield suggestion helper methods

    private boolean isExperimentalYieldEnabled() {
        // Smart suggestions are desktop-only (mobile GUI doesn't support yield panel)
        if (GuiBase.getInterface().isLibgdxPort()) {
            return false;
        }
        return FModel.getPreferences().getPrefBoolean(FPref.YIELD_EXPERIMENTAL_OPTIONS);
    }

    private GameView getGameView() {
        return getController().getGui().getGameView();
    }

    private PlayerView getPlayerView() {
        return getController().getGui().lookupPlayerViewById(getOwner());
    }

    private YieldMode getDefaultYieldMode() {
        GameView gv = getGameView();
        return gv != null && gv.getPlayers().size() >= 3
            ? YieldMode.UNTIL_YOUR_NEXT_TURN
            : YieldMode.UNTIL_END_OF_TURN;
    }

    private boolean checkHasAvailableActions() {
        Player player = getController().getPlayer();
        if (player == null) return false;
        int manaEstimate = ComputerUtilMana.getAvailableManaEstimate(player);
        player.getView().updateHasAvailableActions(player, manaEstimate);
        return player.getView().hasAvailableActions();
    }

    private boolean shouldShowStackYieldPrompt() {
        GameView gv = getGameView();
        if (gv == null) return false;

        FCollectionView<StackItemView> stack = gv.getStack();
        if (stack == null || stack.isEmpty()) {
            return false;
        }

        return !checkHasAvailableActions();
    }

    /**
     * Check if current game state is valid for showing yield suggestions.
     * Returns false if stack is non-empty or if own-turn suppression applies.
     */
    private boolean isValidSuggestionContext(GameView gv, PlayerView pv) {
        FCollectionView<StackItemView> stack = gv.getStack();
        if (stack != null && !stack.isEmpty()) {
            return false;
        }
        // Check if it's the player's own turn
        PlayerView currentTurn = gv.getPlayerTurn();
        if (currentTurn != null && currentTurn.equals(pv)) {
            // Always suppress on player's first turn (no lands/mana yet)
            // First round = turn number <= player count
            int numPlayers = gv.getPlayers().size();
            if (gv.getTurn() <= numPlayers) {
                return false;
            }
            // Otherwise check the preference
            if (FModel.getPreferences().getPrefBoolean(FPref.YIELD_SUPPRESS_ON_OWN_TURN)) {
                return false;
            }
        }
        return true;
    }

    private boolean shouldShowNoActionsPrompt() {
        GameView gv = getGameView();
        PlayerView pv = getPlayerView();
        if (gv == null || pv == null) return false;

        if (!isValidSuggestionContext(gv, pv)) {
            return false;
        }

        return !checkHasAvailableActions();
    }
}
