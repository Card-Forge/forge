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
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.player.actions.PassPriorityAction;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.gamemodes.match.YieldMode;
import forge.localinstance.properties.ForgePreferences;
import forge.localinstance.properties.ForgePreferences.FPref;
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

    // Pending yield suggestion state for prompt integration
    private YieldMode pendingSuggestion = null;
    private String pendingSuggestionMessage = null;

    public InputPassPriority(final PlayerControllerHuman controller) {
        super(controller);
    }

    /** {@inheritDoc} */
    @Override
    public final void showMessage() {
        // Check if experimental yield features are enabled and show smart suggestions
        // Only show suggestions if not already yielding
        if (isExperimentalYieldEnabled() && !isAlreadyYielding()) {
            ForgePreferences prefs = FModel.getPreferences();
            Localizer loc = Localizer.getInstance();

            // Suggestion 1: Stack items but can't respond
            if (prefs.getPrefBoolean(FPref.YIELD_SUGGEST_STACK_YIELD) && shouldShowStackYieldPrompt()) {
                pendingSuggestion = YieldMode.UNTIL_STACK_CLEARS;
                pendingSuggestionMessage = loc.getMessage("lblCannotRespondToStackYieldPrompt");
                showYieldSuggestionPrompt();
                return;
            }
            // Suggestion 2: Has cards but no mana
            else if (prefs.getPrefBoolean(FPref.YIELD_SUGGEST_NO_MANA) && shouldShowNoManaPrompt()) {
                pendingSuggestion = getDefaultYieldMode();
                pendingSuggestionMessage = loc.getMessage("lblNoManaAvailableYieldPrompt");
                showYieldSuggestionPrompt();
                return;
            }
            // Suggestion 3: No available actions (empty hand, no abilities)
            else if (prefs.getPrefBoolean(FPref.YIELD_SUGGEST_NO_ACTIONS) && shouldShowNoActionsPrompt()) {
                pendingSuggestion = getDefaultYieldMode();
                pendingSuggestionMessage = loc.getMessage("lblNoActionsAvailableYieldPrompt");
                showYieldSuggestionPrompt();
                return;
            }
        }

        showNormalPrompt();
    }

    private void showYieldSuggestionPrompt() {
        Localizer loc = Localizer.getInstance();
        showMessage(pendingSuggestionMessage);
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
        // If accepting a yield suggestion
        if (pendingSuggestion != null) {
            YieldMode mode = pendingSuggestion;
            pendingSuggestion = null;
            pendingSuggestionMessage = null;
            getController().getGui().setYieldMode(getOwner(), mode);
            stop();
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
        // If declining a yield suggestion, show normal prompt
        if (pendingSuggestion != null) {
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

    // Smart yield suggestion helper methods

    private boolean isExperimentalYieldEnabled() {
        return FModel.getPreferences().getPrefBoolean(FPref.YIELD_EXPERIMENTAL_OPTIONS);
    }

    private YieldMode getDefaultYieldMode() {
        return getController().getGame().getPlayers().size() >= 3
            ? YieldMode.UNTIL_YOUR_NEXT_TURN
            : YieldMode.UNTIL_END_OF_TURN;
    }

    private boolean shouldShowStackYieldPrompt() {
        Game game = getController().getGame();
        Player player = getController().getPlayer();

        if (game.getStack().isEmpty()) {
            return false;
        }

        return !canRespondToStack(game, player);
    }

    private boolean canRespondToStack(Game game, Player player) {
        // Check hand for playable spells (getAllPossibleAbilities already filters by timing)
        for (Card card : player.getCardsIn(ZoneType.Hand)) {
            if (!card.getAllPossibleAbilities(player, true).isEmpty()) {
                return true;
            }
        }

        // Check battlefield for activatable abilities (excluding mana abilities)
        for (Card card : player.getCardsIn(ZoneType.Battlefield)) {
            for (SpellAbility sa : card.getAllPossibleAbilities(player, true)) {
                if (!sa.isManaAbility()) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean shouldShowNoManaPrompt() {
        Game game = getController().getGame();
        Player player = getController().getPlayer();

        if (!game.getStack().isEmpty()) {
            return false;
        }

        if (game.getPhaseHandler().getPlayerTurn().equals(player)) {
            return false;
        }

        if (player.getCardsIn(ZoneType.Hand).isEmpty()) {
            return false;
        }

        return !hasManaAvailable(player);
    }

    private boolean hasManaAvailable(Player player) {
        if (player.getManaPool().totalMana() > 0) {
            return true;
        }

        for (Card card : player.getCardsIn(ZoneType.Battlefield)) {
            if (card.isUntapped()) {
                for (SpellAbility sa : card.getManaAbilities()) {
                    if (sa.canPlay()) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private boolean shouldShowNoActionsPrompt() {
        Player player = getController().getPlayer();
        Game game = getController().getGame();

        if (!game.getStack().isEmpty()) {
            return false;
        }

        if (game.getPhaseHandler().getPlayerTurn().equals(player)) {
            return false;
        }

        return !hasAvailableActions(game, player);
    }

    private boolean hasAvailableActions(Game game, Player player) {
        // Check hand for actually playable spells (filters by timing, mana, etc.)
        for (Card card : player.getCardsIn(ZoneType.Hand)) {
            if (!card.getAllPossibleAbilities(player, true).isEmpty()) {
                return true;
            }
        }

        // Check battlefield for activatable abilities (excluding mana abilities)
        for (Card card : player.getCardsIn(ZoneType.Battlefield)) {
            for (SpellAbility sa : card.getAllPossibleAbilities(player, true)) {
                if (!sa.isManaAbility()) {
                    return true;
                }
            }
        }
        return false;
    }
}
