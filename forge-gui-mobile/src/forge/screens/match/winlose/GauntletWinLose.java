package forge.screens.match.winlose;

/** Forge: Play Magic: the Gathering.
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

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;
import com.google.common.collect.Lists;

import forge.GuiBase;
import forge.LobbyPlayer;
import forge.assets.FSkinColor;
import forge.assets.FSkinColor.Colors;
import forge.assets.FSkinFont;
import forge.assets.FSkinImage;
import forge.deck.Deck;
import forge.game.Game;
import forge.game.GameType;
import forge.game.Match;
import forge.game.player.RegisteredPlayer;
import forge.gauntlet.GauntletData;
import forge.gauntlet.GauntletIO;
import forge.interfaces.IGuiBase;
import forge.model.FModel;
import forge.screens.match.FControl;
import forge.toolbox.FLabel;
import forge.toolbox.FPanel;

import java.util.List;

/**
 * The Win/Lose handler for 'gauntlet' type tournament
 * games.
 */
public class GauntletWinLose extends ControlWinLose {
    /**
     * Instantiates a new gauntlet win/lose handler.
     * 
     * @param view0 ViewWinLose object
     * @param match
     */
    public GauntletWinLose(final ViewWinLose view0, Game lastGame) {
        super(view0, lastGame);
    }

    /**
     * <p>
     * populateCustomPanel.
     * </p>
     * @return true
     */
    @Override
    public final boolean populateCustomPanel() {
        final GauntletData gd = FModel.getGauntletData();
        final List<String> lstEventNames = gd.getEventNames();
        final List<Deck> lstDecks = gd.getDecks();
        final List<String> lstEventRecords = gd.getEventRecords();
        final int len = lstDecks.size();
        final int num = gd.getCompleted();
        FLabel lblGraphic = null;
        FLabel lblMessage1 = null;
        FLabel lblMessage2 = null;

        // No restarts.
        this.getView().getBtnRestart().setVisible(false);

        // Generic event record.
        lstEventRecords.set(gd.getCompleted(), "Ongoing");

        final Match match = lastGame.getMatch();

        // Match won't be saved until it is over. This opens up a cheat
        // or failsafe mechanism (depending on your perspective) in which
        // the player can restart Forge to replay a match.
        // Pretty sure this can't be fixed until in-game states can be
        // saved. Doublestrike 07-10-12
        LobbyPlayer questPlayer = GuiBase.getInterface().getQuestPlayer();

        // In all cases, update stats.
        lstEventRecords.set(gd.getCompleted(), match.getGamesWonBy(questPlayer) + " - "
                + (match.getPlayedGames().size() - match.getGamesWonBy(questPlayer)));
        
        if (match.isMatchOver()) {
            gd.setCompleted(gd.getCompleted() + 1);

            // Win match case
            if (match.isWonBy(questPlayer)) {
                // Gauntlet complete: Remove save file
                if (gd.getCompleted() == lstDecks.size()) {
                    lblGraphic = new FLabel.Builder()
                        .icon(FSkinImage.QUEST_COIN).build();
                    lblMessage1 = new FLabel.Builder().font(FSkinFont.get(24))
                        .text("CONGRATULATIONS!").build();
                    lblMessage2 = new FLabel.Builder().font(FSkinFont.get(18))
                        .text("You made it through the gauntlet!").build();

                    this.getView().getBtnContinue().setVisible(false);
                    this.getView().getBtnQuit().setText("OK");

                    // Remove save file if it's a quickie, or just reset it.
                    if (gd.getName().startsWith(GauntletIO.PREFIX_QUICK)) {
                        GauntletIO.getGauntletFile(gd).delete();
                    }
                    else {
                        gd.reset();
                    }
                }
                // Or, save and move to next game
                else {
                    gd.stamp();
                    GauntletIO.saveGauntlet(gd);

                    this.getView().getBtnContinue().setVisible(true);
                    this.getView().getBtnContinue().setEnabled(true);
                    this.getView().getBtnQuit().setText("Save and Quit");
                }
            }
            // Lose match case; stop gauntlet.
            else {
                lblGraphic = new FLabel.Builder()
                    .icon(FSkinImage.QUEST_HEART).build();
                lblMessage1 = new FLabel.Builder().font(FSkinFont.get(24))
                        .text("DEFEATED!").build();
                lblMessage2 = new FLabel.Builder().font(FSkinFont.get(18))
                        .text("You have failed to pass the gauntlet.").build();

                this.getView().getBtnContinue().setVisible(false);

                // Remove save file if it's a quickie, or just reset it.
                if (gd.getName().startsWith(GauntletIO.PREFIX_QUICK)) {
                    GauntletIO.getGauntletFile(gd).delete();
                }
                else {
                    gd.reset();
                }
            }
        }

        gd.setEventRecords(lstEventRecords);

        // Custom panel display
        final FLabel lblTitle = new FLabel.Builder().text("Gauntlet Progress")
                .align(HAlignment.CENTER).font(FSkinFont.get(18)).build();

        final FPanel pnl = this.getView().getPnlCustom();
        pnl.setBackColor(FSkinColor.get(Colors.CLR_THEME2));
        pnl.add(lblTitle);

        FLabel lblTemp;
        for (int i = 0; i < len; i++) {
            lblTemp = new FLabel.Builder().font(FSkinFont.get(14)).build();

            if (i <= num) {
                lblTemp.setTextColor(FSkinColor.getStandardColor(Color.GREEN).stepColor(20));
                lblTemp.setText((i + 1) + ". " + lstEventNames.get(i)
                        + " (" + lstEventRecords.get(i) + ")");
            }
            else {
                lblTemp.setTextColor(FSkinColor.getStandardColor(Color.RED));
                lblTemp.setText((i + 1) + ". ??????");
            }

            pnl.add(lblTemp);
        }

        if (lblGraphic != null) {
            pnl.add(lblGraphic);
            pnl.add(lblMessage1);
            pnl.add(lblMessage2);
        }

        return true;
    }

    @Override
    public void actionOnContinue() {
        if (lastGame.getMatch().isMatchOver()) {
            // To change the AI deck, we have to create a new match.
            GauntletData gd = FModel.getGauntletData();
            Deck aiDeck = gd.getDecks().get(gd.getCompleted());
            List<RegisteredPlayer> players = Lists.newArrayList();
            IGuiBase fc = GuiBase.getInterface();
            players.add(new RegisteredPlayer(gd.getUserDeck()).setPlayer(fc.getGuiPlayer()));
            players.add(new RegisteredPlayer(aiDeck).setPlayer(fc.createAiPlayer()));
            
            saveOptions();
            FControl.endCurrentGame();

            FControl.startMatch(GameType.Gauntlet, players);
        } else {
            super.actionOnContinue();
        }
    }
}
