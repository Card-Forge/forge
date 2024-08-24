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

import java.util.ArrayList;
import java.util.List;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.player.PlayerController;
import forge.game.player.actions.PassPriorityAction;

import forge.game.spellability.SpellAbility;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.player.GamePlayerUtil;
import forge.player.PlayerControllerHuman;
import forge.util.ITriggerEvent;
import forge.util.Localizer;
import forge.util.ThreadUtil;

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

    public InputPassPriority(final PlayerControllerHuman controller) {
        super(controller);
    }

    /** {@inheritDoc} */
    @Override
    public final void showMessage() {
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

    /** {@inheritDoc} */
    @Override
    protected final void onOk() {
        passPriority(() -> {
            getController().macros().addRememberedAction(new PassPriorityAction());
            stop();
        });
    }

    /** {@inheritDoc} */
    @Override
    protected final void onCancel() {
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
    protected final void onPlayerSelected(Player selected, final ITriggerEvent triggerEvent) {
        PlayerController pc = selected.getController();
        if (pc.isGuiPlayer()) {
           pc.setFullControl(!pc.isFullControl());
        }
    }

    @Override
    protected boolean onCardSelected(final Card card, final List<Card> otherCardsToSelect, final ITriggerEvent triggerEvent) {
        //remove unplayable unless triggerEvent specified, in which case unplayable may be shown as disabled options
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
