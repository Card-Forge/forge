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

import java.util.ArrayList;
import java.util.List;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.match.MatchUtil;
import forge.player.PlayerControllerHuman;
import forge.util.ITriggerEvent;
import forge.util.Lang;
import forge.util.ThreadUtil;
import forge.util.gui.SGuiDialog;
import forge.view.CardView;
 
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

    public InputConfirmMulligan(final PlayerControllerHuman controller, final Player humanPlayer, final Player startsGame, final boolean commander) {
        super(controller);
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
            ButtonUtil.update(getOwner(), "Keep", "Exile", true, false, true);
            sb.append("Will you keep your hand or choose some cards to exile those and draw one less card?");
        }
        else {
            ButtonUtil.update(getOwner(), "Keep", "Mulligan", true, true, true);
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
        if (isCommander) {
            // Clear the "selected" icon after clicking the done button
            for (final Card c : this.selected) {
                MatchUtil.setUsedToPay(getController().getCardView(c), false);
            }
        }
        stop();
    }

    volatile boolean cardSelectLocked = false;

    @Override
    protected boolean onCardSelected(final Card c0, final ITriggerEvent triggerEvent) { // the only place that would cause troubles - input is supposed only to confirm, not to fire abilities
        boolean fromHand = player.getZone(ZoneType.Hand).contains(c0);
        boolean isSerumPowder = c0.getName().equals("Serum Powder");
        boolean isLegalChoice = fromHand && (isCommander || isSerumPowder);
        if (!isLegalChoice || cardSelectLocked) {
            flashIncorrectAction();
            return false;
        }

        final CardView cView = getController().getCardView(c0);
        if (isSerumPowder && SGuiDialog.confirm(cView, "Use " + cView + "'s ability?")) {
            cardSelectLocked = true;
            ThreadUtil.invokeInGameThread(new Runnable() {
                public void run() {
                    List<Card> hand = new ArrayList<Card>(c0.getController().getCardsIn(ZoneType.Hand));
                    for (Card c : hand) {
                        player.getGame().getAction().exile(c);
                    }
                    c0.getController().drawCards(hand.size());
                    cardSelectLocked = false;
                }
            });
            return true;
        }

        if (isCommander) { // allow to choose cards for partial paris
            if (selected.contains(c0)) {
                MatchUtil.setUsedToPay(getController().getCardView(c0), false);
                selected.remove(c0);
            }
            else {
                MatchUtil.setUsedToPay(getController().getCardView(c0), true);
                selected.add(c0);
            }
            ButtonUtil.update(getOwner(), "Keep", "Exile", true, !selected.isEmpty(), true);
        }
        return true;
    }

    public final boolean isKeepHand() {
        return keepHand;
    }

    public List<Card> getSelectedCards() {
        return selected;
    }
}
