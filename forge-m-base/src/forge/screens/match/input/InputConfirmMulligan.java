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
package forge.screens.match.input;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.screens.match.FControl;
import forge.toolbox.GuiDialog;
import forge.toolbox.VCardZoom.ZoomController;
import forge.util.Lang;
import forge.util.ThreadUtil;

import java.util.ArrayList;
import java.util.List;
 
/**
  * <p>
  * InputConfirmMulligan class.
  * </p>
  * 
  * @author Forge
  * @version $Id: InputConfirmMulligan.java 24769 2014-02-09 13:56:04Z Hellfish $
  */
public class InputConfirmMulligan extends InputSyncronizedBase {
    /** Constant <code>serialVersionUID=-8112954303001155622L</code>. */
    private static final long serialVersionUID = -8112954303001155622L;

    boolean keepHand = false;
    final boolean isCommander;

    private final List<Card> selected = new ArrayList<Card>();
    private final Player player;
    private final Player startingPlayer;

    public InputConfirmMulligan(Player humanPlayer, Player startsGame, boolean commander) {
        player = humanPlayer;
        isCommander = commander;
        startingPlayer = startsGame;
    }

    /** {@inheritDoc} */
    @Override
    public final void showMessage() {
        Game game = player.getGame();

        StringBuilder sb = new StringBuilder();
        if (startingPlayer == player) {
            sb.append(player).append(", you are going first!\n\n");
        }
        else {
            sb.append(startingPlayer.getName()).append(" is going first.\n");
            sb.append(player).append(", you are going ").append(Lang.getOrdinal(game.getPosition(player, startingPlayer))).append(".\n\n");
        }

        if (isCommander) {
            ButtonUtil.setButtonText("Keep", "Exile");
            ButtonUtil.enableOnlyOk();
            sb.append("Will you keep your hand or choose some cards to exile those and draw one less card?");
        }
        else {
            ButtonUtil.setButtonText("Keep", "Mulligan");
            ButtonUtil.enableAll();
            sb.append("Do you want to keep your hand?");
        }

        showMessage(sb.toString());
    }

    /** {@inheritDoc} */
    @Override
    protected final void onOk() {
        keepHand = true;
        done();
    }

    /** {@inheritDoc} */
    @Override
    protected final void onCancel() {
        keepHand = false;
        done();
    }

    private void done() {
        ButtonUtil.reset();
        if (isCommander) {
            // Clear the "selected" icon after clicking the done button
            for (Card c : this.selected) {
                FControl.setUsedToPay(c, false);
            }
        }
        stop();
    }

    volatile boolean cardSelectLocked = false;

    @Override
    protected void onCardSelected(final Card card, final List<Card> orderedCardOptions) { // the only place that would cause troubles - input is supposed only to confirm, not to fire abilities
        if (cardSelectLocked) { return; }

        FControl.getView().getCardZoom().show(FControl.getView().getPrompt().getMessage(),
                card, orderedCardOptions, new ZoomController<String>() {
            @Override
            public List<String> getOptions(final Card card) {
                List<String> options = new ArrayList<String>();

                if (player.getZone(ZoneType.Hand).contains(card)) {
                    if (card.getName().equals("Serum Powder")) {
                        options.add("Exile all the cards from your hand, then draw that many cards.");
                    }
                    else if (isCommander) {
                        if (selected.contains(card)) {
                            options.add("Select Card");
                        }
                        else {
                            options.add("Unselect Card");
                        }
                    }
                }
                return options;
            }

            @Override
            public boolean selectOption(final Card card, final String option) {
                if (option == "Exile all the cards from your hand, then draw that many cards.") {
                    if (GuiDialog.confirm(card, "This action cannot be undone. Proceed?")) {
                        cardSelectLocked = true;
                        ThreadUtil.invokeInGameThread(new Runnable() {
                            public void run() {
                                List<Card> hand = new ArrayList<Card>(card.getController().getCardsIn(ZoneType.Hand));
                                for (Card c : hand) {
                                    player.getGame().getAction().exile(c);
                                }
                                card.getController().drawCards(hand.size());
                                cardSelectLocked = false;
                            }
                        });
                    }
                    return true;
                }
                if (isCommander) { // allow to choose cards for partial paris
                    if (selected.contains(card)) {
                        FControl.setUsedToPay(card, false);
                        selected.remove(card);
                    }
                    else {
                        FControl.setUsedToPay(card, true);
                        selected.add(card);
                    }
                    if (selected.isEmpty()) {
                        ButtonUtil.enableOnlyOk();
                    }
                    else {
                        ButtonUtil.enableAll();
                    }
                    return false; //keep zoom view open
                }
                return true;
            }
        });

        
    }

    public final boolean isKeepHand() {
        return keepHand;
    }

    public List<Card> getSelectedCards() {
        return selected;
    }
}
