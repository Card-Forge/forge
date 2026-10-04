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
import forge.game.card.CardView;
import forge.game.player.Player;
import forge.game.player.PlayerView;
import forge.game.player.actions.ActivateAbilityAction;
import forge.game.player.actions.PassPriorityAction;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityView;
import forge.game.spellability.StackItemView;
import forge.gamemodes.match.Answer;
import forge.gamemodes.match.DeclineScope;
import forge.gamemodes.match.PriorityPrompt;
import forge.gamemodes.match.Question;
import forge.gamemodes.match.SuggestionType;
import forge.gamemodes.match.YieldUpdate;
import forge.gamemodes.net.server.HostingServer;
import forge.gamemodes.net.server.HostingServer.AfkTimeout;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.util.collect.FCollectionView;
import forge.model.FModel;
import forge.player.GamePlayerUtil;
import forge.player.PlayerControllerHuman;
import forge.util.Localizer;
import forge.util.ThreadUtil;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    /** Answers resolve against this map because ability ids change between enumerations. */
    private volatile Map<SpellAbilityView, SpellAbility> published;
    private volatile Map<CardView, List<SpellAbilityView>> abilitiesByCard;
    private volatile Question.Priority question;

    public InputPassPriority(final PlayerControllerHuman controller) {
        super(controller);
        refreshAbilities();
    }

    /** Enumerating abilities touches the game, so call this on the game thread or while it is parked on this input. */
    public void refreshAbilities() {
        published = getController().collectPriorityAbilities();
        final Map<CardView, List<SpellAbilityView>> byCard = new LinkedHashMap<>();
        for (final SpellAbilityView view : published.keySet()) {
            byCard.computeIfAbsent(view.getHostCard(), k -> new ArrayList<>()).add(view);
        }
        abilitiesByCard = byCard;
    }

    @Override
    protected void onStop() {
        getController().clearActionableCards();
        getController().getInputProxy().withdrawQuestion();
    }

    @Override
    public void showAndWait() {
        final AfkTimeout timeout = HostingServer.armAfkTimeout(getController(), this);
        try {
            super.showAndWait();
        } finally {
            timeout.cancel();
        }
    }

    /** Asks the priority question. A yield suggestion is the yield layer's, sent just before the question. */
    @Override
    public final void showMessage() {
        chosenSa = null;
        final SuggestionType suggestion = chooseSuggestion();
        if (suggestion != null) {
            getController().getGui().applyYieldUpdate(new YieldUpdate.Suggest(getOwner(), suggestion));
        } else {
            getController().pushActionableCards(false);
        }
        final Game game = getController().getGame();
        final Player player = getController().getPlayer();
        // The phase cannot end while the stack holds something, so no mana is lost by passing then
        final boolean losesMana = game.getStack().isEmpty() && player.getManaPool().willManaBeLostAtEndOfPhase();
        question = new Question.Priority(getController().getInputProxy().nextQuestionId(), getOwner(), abilitiesByCard,
                getController().canUndoLastAction() ? game.getStack().getUndoStackSize() : 0,
                losesMana, losesMana && player.getManaPool().hasBurn());
        getController().getInputProxy().askQuestion(this, question);
    }

    private SuggestionType chooseSuggestion() {
        if (getController().isMacroActive() || isAlreadyYielding()) {
            return null;
        }
        // Suppress one prompt after a yield ends — avoids "yielded → ended → yield again?" loop
        if (getController().getYieldController().getBoolPref(FPref.YIELD_SUPPRESS_AFTER_END)
                && getController().getYieldController().didYieldJustEnd()) {
            return null;
        }
        if (getController().getYieldController().getBoolPref(FPref.YIELD_AUTO_PASS_NO_ACTIONS)) {
            return null;
        }
        // Both scopes NEVER → skip including stack-transition tracking (no decline state to maintain)
        DeclineScope stackScope = getController().getYieldController().getDeclineScope(FPref.YIELD_DECLINE_SCOPE_STACK_YIELD);
        DeclineScope noActionsScope = getController().getYieldController().getDeclineScope(FPref.YIELD_DECLINE_SCOPE_NO_ACTIONS);
        if (stackScope == DeclineScope.NEVER && noActionsScope == DeclineScope.NEVER) {
            return null;
        }

        GameView gvForStack = getGameView();
        boolean stackNonEmpty = gvForStack != null && gvForStack.getStack() != null
                && !gvForStack.getStack().isEmpty();
        getController().getYieldController().onPriorityReceived(stackNonEmpty);

        if (!getController().getYieldController().isSuggestionDeclined(SuggestionType.STACK_YIELD)
                && shouldShowStackYieldPrompt()) {
            return SuggestionType.STACK_YIELD;
        }
        if (!getController().getYieldController().isSuggestionDeclined(SuggestionType.NO_ACTIONS)
                && shouldShowNoActionsPrompt()) {
            return SuggestionType.NO_ACTIONS;
        }
        return null;
    }

    private boolean isAlreadyYielding() {
        return getController().getYieldController().isYieldActive();
    }

    private GameView getGameView() {
        return getController().getGui().getGameView();
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

    private boolean shouldShowNoActionsPrompt() {
        GameView gv = getGameView();
        PlayerView pv = getOwner();
        if (gv == null || pv == null) return false;
        FCollectionView<StackItemView> stack = gv.getStack();
        if (stack != null && !stack.isEmpty()) return false;
        PlayerView currentTurn = gv.getPlayerTurn();
        if (currentTurn != null && currentTurn.equals(pv)) {
            // Always suppress on player's first turn (no lands/mana yet)
            if (gv.getTurn() <= gv.getPlayers().size()) return false;
            if (getController().getYieldController().getBoolPref(FPref.YIELD_SUPPRESS_ON_OWN_TURN)) return false;
        }
        return !checkHasAvailableActions();
    }

    public void answer(final Answer answer) {
        final Question.Priority asked = question;
        if (isFinished() || asked == null || answer.questionId() != asked.id()) {
            return;
        }
        if (answer instanceof Answer.Play play) {
            play(play, asked);
        } else if (answer instanceof Answer.Pass) {
            pass();
        } else if (answer instanceof Answer.Undo) {
            getController().tryUndoLastAction();
        }
    }

    private void play(final Answer.Play play, final Question.Priority asked) {
        final SpellAbility ability = published.get(play.ability());
        if (ability == null || !ability.canPlay(true)) {
            return;
        }
        final List<SpellAbility> chosen = new ArrayList<>();
        chosen.add(ability);
        if (play.alsoOn() != null && ability.isManaAbility()) {
            final String description = ability.getView().getDescription();
            for (final CardView other : play.alsoOn()) {
                for (final SpellAbilityView view : asked.abilities().getOrDefault(other, List.of())) {
                    final SpellAbility sa = published.get(view);
                    if (sa != null && view.getDescription().equals(description) && sa.canPlay(true)) {
                        chosen.add(sa);
                        break;
                    }
                }
            }
        }
        getController().macros().addRememberedAction(new ActivateAbilityAction(ability.getHostCard().getView(), ability.getView().getDescription()));
        chosenSa = chosen;
        stop();
    }

    /** The host passing for the player, as yields do; it warns first when floating mana would be lost. */
    public void passPriority() {
        if (isFinished()) return;
        passPriority(this::pass);
    }

    /** An answer has already had the floating-mana warning from the GUI. */
    private void pass() {
        getController().macros().addRememberedAction(new PassPriorityAction(
                getController().getGame().getStack().isEmpty(),
                getController().getGame().getPhaseHandler().getPhase()));
        stop();
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
                        String message = PriorityPrompt.floatingManaWarning(player.getManaPool().hasBurn());
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
