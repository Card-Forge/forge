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
package forge.control.input;

import java.util.ArrayList;
import java.util.List;

import forge.Card;
import forge.FThreads;
import forge.game.GameState;
import forge.game.GameType;
import forge.game.MatchController;
import forge.game.ai.ComputerUtil;
import forge.game.player.AIPlayer;
import forge.game.player.Player;
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;
import forge.gui.GuiDialog;
import forge.gui.framework.SDisplayUtil;
import forge.gui.match.CMatchUI;
import forge.gui.match.nonsingleton.VField;
import forge.gui.match.views.VMessage;
import forge.util.Lang;
import forge.view.ButtonUtil;
 /**
  * <p>
  * InputMulligan class.
  * </p>
  * 
  * @author Forge
  * @version $Id$
  */
public class InputMulligan extends InputBase {
    /** Constant <code>serialVersionUID=-8112954303001155622L</code>. */
    private static final long serialVersionUID = -8112954303001155622L;
    
    private final MatchController match;
    
    public InputMulligan(MatchController match0, Player humanPlayer) {
        super(humanPlayer);
        match = match0;
    }
    
    /** {@inheritDoc} */
    @Override
    public final void showMessage() {
        ButtonUtil.setButtonText("Keep", "Mulligan");
        ButtonUtil.enableAllFocusOk();

        GameState game = match.getCurrentGame();
        Player startingPlayer = game.getPhaseHandler().getPlayerTurn();

        StringBuilder sb = new StringBuilder();
        if( startingPlayer == player ) {
            sb.append("You are going first.\n");
        } else {
            sb.append(startingPlayer.getName()).append(" is going first. ");
            sb.append("You are going ").append(Lang.getOrdinal(game.getPosition(player, startingPlayer))).append(".\n");
        }

        sb.append("Do you want to keep your hand?");
        showMessage(sb.toString());
    }

    /** {@inheritDoc} */
    @Override
    public final void selectButtonOK() {
        this.end();
    }

    /** {@inheritDoc} */
    @Override
    public final void selectButtonCancel() {
        player.doMulligan();

        if (player.getCardsIn(ZoneType.Hand).isEmpty()) {
            this.end();
        } else {
            ButtonUtil.enableAllFocusOk();
        }
    }

    final void end() {

        // Computer mulligan
        final GameState game = match.getCurrentGame();
        for (Player p : game.getPlayers()) {
            if (!(p instanceof AIPlayer)) {
                continue;
            }
            AIPlayer ai = (AIPlayer) p;
            while (ComputerUtil.wantMulligan(ai)) {
                ai.doMulligan();
            }
        }


        ButtonUtil.reset();
        Player next = game.getPhaseHandler().getPlayerTurn();
        if(game.getType() == GameType.Planechase)
        {
            next.initPlane();
        }
        //Set Field shown to current player.        
        VField nextField = CMatchUI.SINGLETON_INSTANCE.getFieldViewFor(next);
        SDisplayUtil.showTab(nextField);
        
        FThreads.invokeInNewThread( new Runnable() {
            @Override
            public void run() {
                match.afterMulligans();
            }
        });
    }

    @Override
    public void selectCard(Card c0, boolean isMetaDown) {
        Zone z0 = match.getCurrentGame().getZoneOf(c0);
        if (c0.getName().equals("Serum Powder") && z0.is(ZoneType.Hand)) {
            if (GuiDialog.confirm(c0, "Use " + c0.getName() + "'s ability?")) {
                List<Card> hand = new ArrayList<Card>(c0.getController().getCardsIn(ZoneType.Hand));
                for (Card c : hand) {
                    match.getCurrentGame().getAction().exile(c);
                }
                c0.getController().drawCards(hand.size());
            }
        } else {
            SDisplayUtil.remind(VMessage.SINGLETON_INSTANCE);
        }
    }
}
