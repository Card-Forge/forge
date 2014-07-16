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

import com.google.common.collect.Lists;

import forge.GuiBase;
import forge.LobbyPlayer;
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
import forge.toolbox.FOptionPane;
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

    @Override
    public final void showRewards() {
        final GauntletData gd = FModel.getGauntletData();
        final List<String> lstEventNames = gd.getEventNames();
        final List<Deck> lstDecks = gd.getDecks();
        final List<String> lstEventRecords = gd.getEventRecords();
        final int len = lstDecks.size();
        final int num = gd.getCompleted();
        FSkinImage icon = null;
        String message1 = "";
        String message2 = "";

        // No restarts.
        getView().getBtnRestart().setVisible(false);

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
                    icon = FSkinImage.QUEST_COIN;
                    message1 = "CONGRATULATIONS!";
                    message2 = "You made it through the gauntlet!";

                    getView().getBtnContinue().setVisible(false);
                    getView().getBtnQuit().setText("OK");

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

                    getView().getBtnContinue().setVisible(true);
                    getView().getBtnContinue().setEnabled(true);
                    getView().getBtnQuit().setText("Save and Quit");
                }
            }
            // Lose match case; stop gauntlet.
            else {
                icon = FSkinImage.QUEST_HEART;
                message1 = "DEFEATED!";
                message2 = "You have failed to pass the gauntlet.";

                getView().getBtnContinue().setVisible(false);

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

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) {
            if (i <= num) {
                sb.append((i + 1) + ". " + lstEventNames.get(i)
                        + " (" + lstEventRecords.get(i) + ")\n");
            }
            else {
                sb.append((i + 1) + ". ??????\n");
            }
        }

        sb.append("\n");
        sb.append(message1 + "\n\n");
        sb.append(message2);

        FOptionPane.showMessageDialog(sb.toString(), "Gauntlet Progress", icon);
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
