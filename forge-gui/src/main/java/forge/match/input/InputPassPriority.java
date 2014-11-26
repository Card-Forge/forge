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
package forge.match.input;

import java.util.List;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.model.FModel;
import forge.player.GamePlayerUtil;
import forge.player.PlayerControllerHuman;
import forge.properties.ForgePreferences.FPref;
import forge.util.ITriggerEvent;
import forge.util.ThreadUtil;
import forge.util.gui.SOptionPane;

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
    private final Player player;
    
    private SpellAbility chosenSa;
    
    public InputPassPriority(final PlayerControllerHuman controller, final Player human) {
        super(controller);
        player = human;
    }

    /** {@inheritDoc} */
    @Override
    public final void showMessage() {
        showMessage(getTurnPhasePriorityMessage(player.getGame()));
        chosenSa = null;
        if (getController().canUndoLastAction()) { //allow undoing with cancel button if can undo last action
            ButtonUtil.update(getOwner(), "OK", "Undo", true, true, true);
        }
        else { //otherwise allow ending turn with cancel button
            ButtonUtil.update(getOwner(), "OK", "End Turn", true, true, true);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected final void onOk() {
        passPriority(new Runnable() {
            @Override
            public void run() {
                stop();
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    protected final void onCancel() {
        if (!getController().tryUndoLastAction()) { //undo if possible
            //otherwise end turn
            passPriority(new Runnable() {
                @Override
                public void run() {
                    player.getController().autoPassUntilEndOfTurn();
                    stop();
                }
            });
        }
    }

    @Override
    protected boolean allowAwaitNextInput() {
        return chosenSa == null && !player.getController().mayAutoPass(); //don't allow awaiting next input if player chose to end the turn or if a spell/ability is chosen
    }

    private void passPriority(final Runnable runnable) {
        if (FModel.getPreferences().getPrefBoolean(FPref.UI_MANA_LOST_PROMPT)) {
            //if gui player has mana floating that will be lost if phase ended right now, prompt before passing priority
            final Game game = player.getGame();
            if (game.getStack().isEmpty()) { //phase can't end right now if stack isn't empty
                Player player = game.getPhaseHandler().getPriorityPlayer();
                if (player != null && player.getManaPool().willManaBeLostAtEndOfPhase() && player.getLobbyPlayer() == GamePlayerUtil.getGuiPlayer()) {
                    ThreadUtil.invokeInGameThread(new Runnable() { //must invoke in game thread so dialog can be shown on mobile game
                        @Override
                        public void run() {
                            String message = "You have mana floating in your mana pool that could be lost if you pass priority now.";
                            if (FModel.getPreferences().getPrefBoolean(FPref.UI_MANABURN)) {
                                message += " You will take mana burn damage equal to the amount of floating mana lost this way.";
                            }
                            if (SOptionPane.showOptionDialog(message, "Mana Floating", SOptionPane.WARNING_ICON, new String[]{"OK", "Cancel"}) == 0) {
                                runnable.run();
                            }
                        }
                    });
                    return;
                }
            }
        }
        runnable.run(); //just pass priority immediately if no mana floating that would be lost
    }

    public SpellAbility getChosenSa() { return chosenSa; }

    @Override
    protected boolean onCardSelected(final Card card, final List<Card> otherCardsToSelect, final ITriggerEvent triggerEvent) {
        //remove unplayable unless triggerEvent specified, in which case unplayable may be shown as disabled options
    	List<SpellAbility> abilities = card.getAllPossibleAbilities(player, triggerEvent == null); 
    	if (abilities.isEmpty()) {
            flashIncorrectAction();
            return false;
    	}

    	selectAbility(player.getController().getAbilityToPlay(abilities, triggerEvent));
    	return true;
    }
    
    @Override
    public void selectAbility(final SpellAbility ab) {
    	if (ab != null) {
            chosenSa = ab;
            stop();
        }
    }
}
