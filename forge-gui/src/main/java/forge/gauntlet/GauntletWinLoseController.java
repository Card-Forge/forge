package forge.gauntlet;

import java.util.List;

import com.google.common.collect.Lists;

import forge.GuiBase;
import forge.LobbyPlayer;
import forge.assets.FSkinProp;
import forge.deck.Deck;
import forge.game.GameType;
import forge.game.player.RegisteredPlayer;
import forge.interfaces.IButton;
import forge.interfaces.IGuiBase;
import forge.interfaces.IWinLoseView;
import forge.model.FModel;
import forge.view.IGameView;

public abstract class GauntletWinLoseController {
    private final IGameView lastGame;
    private final IWinLoseView<? extends IButton> view;

    public GauntletWinLoseController(IWinLoseView<? extends IButton> view0, final IGameView game0) {
        view = view0;
        lastGame = game0;
    }

    public void showOutcome() {
        final GauntletData gd = FModel.getGauntletData();
        final List<String> lstEventNames = gd.getEventNames();
        final List<Deck> lstDecks = gd.getDecks();
        final List<String> lstEventRecords = gd.getEventRecords();
        final int len = lstDecks.size();
        final int num = gd.getCompleted();
        FSkinProp icon = null;
        String message1 = null;
        String message2 = null;

        // No restarts.
        view.getBtnRestart().setVisible(false);

        // Generic event record.
        lstEventRecords.set(gd.getCompleted(), "Ongoing");

        // Match won't be saved until it is over. This opens up a cheat
        // or failsafe mechanism (depending on your perspective) in which
        // the player can restart Forge to replay a match.
        // Pretty sure this can't be fixed until in-game states can be
        // saved. Doublestrike 07-10-12
        LobbyPlayer questPlayer = GuiBase.getInterface().getQuestPlayer();

        // In all cases, update stats.
        lstEventRecords.set(gd.getCompleted(), lastGame.getGamesWonBy(questPlayer) + " - "
                + (lastGame.getNumPlayedGamesInMatch() - lastGame.getGamesWonBy(questPlayer)));
        
        if (lastGame.isMatchOver()) {
            gd.setCompleted(gd.getCompleted() + 1);

            // Win match case
            if (lastGame.isMatchWonBy(questPlayer)) {
                // Gauntlet complete: Remove save file
                if (gd.getCompleted() == lstDecks.size()) {
                    icon = FSkinProp.ICO_QUEST_COIN;
                    message1 = "CONGRATULATIONS!";
                    message2 = "You made it through the gauntlet!";

                    view.getBtnContinue().setVisible(false);
                    view.getBtnQuit().setText("OK");

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

                    view.getBtnContinue().setVisible(true);
                    view.getBtnContinue().setEnabled(true);
                    view.getBtnQuit().setText("Save and Quit");
                }
            }
            // Lose match case; stop gauntlet.
            else {
                icon = FSkinProp.ICO_QUEST_HEART;
                message1 = "DEFEATED!";
                message2 = "You have failed to pass the gauntlet.";

                view.getBtnContinue().setVisible(false);

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

        showOutcome(message1, message2, icon, lstEventNames, lstEventRecords, len, num);
    }

    public final boolean actionOnContinue() {
        if (lastGame.isMatchOver()) {
            // To change the AI deck, we have to create a new match.
            GauntletData gd = FModel.getGauntletData();
            Deck aiDeck = gd.getDecks().get(gd.getCompleted());
            List<RegisteredPlayer> players = Lists.newArrayList();
            IGuiBase fc = GuiBase.getInterface();
            players.add(new RegisteredPlayer(gd.getUserDeck()).setPlayer(fc.getGuiPlayer()));
            players.add(new RegisteredPlayer(aiDeck).setPlayer(fc.createAiPlayer()));

            view.hide();
            saveOptions();
            GuiBase.getInterface().endCurrentGame();

            GuiBase.getInterface().startMatch(GameType.Gauntlet, players);
            return true;
        }
        return false;
    }

    protected abstract void showOutcome(String message1, String message2, FSkinProp icon, List<String> lstEventNames, List<String> lstEventRecords, int len, int num);
    protected abstract void saveOptions();
}
