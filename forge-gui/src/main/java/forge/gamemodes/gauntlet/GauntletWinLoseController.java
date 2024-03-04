package forge.gamemodes.gauntlet;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.Lists;

import forge.LobbyPlayer;
import forge.deck.Deck;
import forge.game.GameView;
import forge.game.player.RegisteredPlayer;
import forge.gui.interfaces.IButton;
import forge.gui.interfaces.IWinLoseView;
import forge.localinstance.skin.FSkinProp;
import forge.model.FModel;
import forge.player.GamePlayerUtil;
import forge.util.Localizer;

public abstract class GauntletWinLoseController {
    private final Localizer localizer = Localizer.getInstance();
    private static final String SAVE_AND_QUIT = Localizer.getInstance().getMessage("btnSaveQuit");

    private final IWinLoseView<? extends IButton> view;
    private final GameView lastGame;

    public GauntletWinLoseController(IWinLoseView<? extends IButton> view0, final GameView game0) {
        view = view0;
        lastGame = game0;
    }

    public void showOutcome() {
        final GauntletData gd = FModel.getGauntletData();
        if (gd.getEventNames() == null || gd.getEventRecords() == null) {
            //fix corrupted entry to reset and allow the save to continue instead of crashing due to NPE
            gd.setEventNames(new ArrayList<>());
            gd.setEventRecords(new ArrayList<>());
            gd.reset();
        }
        final List<String> lstEventNames = gd.getEventNames();
        final List<Deck> lstDecks = gd.getDecks();
        final List<String> lstEventRecords = gd.getEventRecords();
        final int len = lstEventNames.size();
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
        LobbyPlayer questPlayer = GamePlayerUtil.getGuiPlayer();

        // In all cases, update stats.
        lstEventRecords.set(gd.getCompleted(), lastGame.getGamesWonBy(questPlayer) + " - "
                + (lastGame.getNumPlayedGamesInMatch() - lastGame.getGamesWonBy(questPlayer) + 1));
        
        boolean isMatchOver = lastGame.isMatchOver();
        if (isMatchOver) {
            gd.setCompleted(gd.getCompleted() + 1);

            // Win match case
            if (lastGame.isMatchWonBy(questPlayer)) {
                // Gauntlet complete: Remove save file
                if (gd.getCompleted() == lstDecks.size()) {
                    icon = FSkinProp.ICO_QUEST_COIN;
                    message1 = localizer.getMessage("lblCongratulations");
                    message2 = localizer.getMessage("lblGauntletTournament");

                    view.getBtnContinue().setVisible(false);
                    view.getBtnQuit().setText(localizer.getMessage("lblOK"));

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

                    view.getBtnContinue().setText(localizer.getMessage("btnNextRound") + " (" + (gd.getCompleted() + 1)
                            + "/" + len + ")");
                    view.getBtnContinue().setVisible(true);
                    view.getBtnContinue().setEnabled(true);
                    view.getBtnQuit().setText(localizer.getMessage("btnSaveQuit"));
                }
            }
            // Lose match case; stop gauntlet.
            else {
                icon = FSkinProp.ICO_QUEST_HEART;
                message1 = localizer.getMessage("lblDefeated");
                message2 = localizer.getMessage("lblFailedGauntlet");

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

        showOutcome(isMatchOver, message1, message2, icon, lstEventNames, lstEventRecords, len, num);
    }

    public final boolean actionOnContinue() {
        if (lastGame.isMatchOver()) {
            // To change the AI deck, we have to create a new match.
            final GauntletData gd = FModel.getGauntletData();
            final RegisteredPlayer human = gd.isCommanderGauntlet()
                    ? RegisteredPlayer.forCommander(gd.getUserDeck()).setPlayer(GamePlayerUtil.getGuiPlayer())
                    : new RegisteredPlayer(gd.getUserDeck()).setPlayer(GamePlayerUtil.getGuiPlayer());
            final Deck aiDeck = gd.getDecks().get(gd.getCompleted());
            final List<RegisteredPlayer> players = Lists.newArrayList();
            players.add(human);
            players.add(gd.isCommanderGauntlet()
                    ? RegisteredPlayer.forCommander(aiDeck).setPlayer(GamePlayerUtil.createAiPlayer())
                    : new RegisteredPlayer(aiDeck).setPlayer(GamePlayerUtil.createAiPlayer()));

            view.hide();
            saveOptions();
            gd.nextRound(players, human);
            return true;
        }
        return false;
    }

    public final void actionOnQuit() {
        if (!SAVE_AND_QUIT.equals(view.getBtnQuit().getText())) {
            // Quitting mid-match abandons the gauntlet.
            FModel.getGauntletData().reset();
        }
    }

    protected abstract void showOutcome(boolean isMatchOver, String message1, String message2, FSkinProp icon, List<String> lstEventNames, List<String> lstEventRecords, int len, int num);
    protected abstract void saveOptions();
}
