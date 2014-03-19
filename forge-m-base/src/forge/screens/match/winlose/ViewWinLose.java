package forge.screens.match.winlose;

import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;

import forge.Forge;
import forge.game.Game;
import forge.game.GameLog;
import forge.game.GameLogEntry;
import forge.game.GameLogEntryType;
import forge.game.GameOutcome;
import forge.game.player.Player;
import forge.model.FModel;
import forge.toolbox.FButton;
import forge.toolbox.FContainer;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FLabel;
import forge.toolbox.FOverlay;
import forge.toolbox.FPanel;
import forge.toolbox.FTextArea;

public class ViewWinLose extends FOverlay {
    private final FButton btnContinue, btnRestart, btnQuit;
    private final FLabel lblTitle, lblLog, lblStats, btnCopyLog;
    private final FTextArea txtLog;
    private final FPanel pnlCustom;
    private final OutcomesPanel pnlOutcomes;
    private final Game game;

    public ViewWinLose(final Game game0) {
        game = game0;
        
        lblTitle = add(new FLabel.Builder().build());
        lblStats = add(new FLabel.Builder().build());
        pnlOutcomes = add(new OutcomesPanel());
        pnlCustom = new FPanel();

        btnContinue = add(new FButton());
        btnRestart = add(new FButton());
        btnQuit = add(new FButton());

        // Control of the win/lose is handled differently for various game
        // modes.
        ControlWinLose control = null;
        switch (game0.getRules().getGameType()) {
        case Quest:
            //control = new QuestWinLose(this, game0);
            break;
        case Draft:
            if (!FModel.getGauntletMini().isGauntletDraft()) {
                break;
            }
        case Sealed:
            control = new LimitedWinLose(this, game0);
            break;
        case Gauntlet:
            control = new GauntletWinLose(this, game0);
            break;
        default: // will catch it after switch
            break;
        }
        if (control == null) {
            control = new ControlWinLose(this, game0);
        }

        btnContinue.setText("Next Game");
        btnContinue.setFontSize(22);
        btnRestart.setText("Start New Match");
        btnRestart.setFontSize(22);
        btnQuit.setText("Quit Match");
        btnQuit.setFontSize(22);
        btnContinue.setEnabled(!game0.getMatch().isMatchOver());

        lblLog = add(new FLabel.Builder().text("Game Log").align(HAlignment.CENTER).fontSize(18).build());
        txtLog = add(new FTextArea(game.getGameLog().getLogText(null).replace("[COMPUTER]", "[AI]")));
        txtLog.setFontSize(14);

        btnCopyLog = add(new FLabel.ButtonBuilder().text("Copy to clipboard").command(new Runnable() {
            @Override
            public void run() {
                Forge.getClipboard().setContents(txtLog.getText());
            }
        }).build());

        if (control.populateCustomPanel()) {
            add(pnlCustom);
        }

        lblTitle.setText(composeTitle(game0.getOutcome()));

        showGameOutcomeSummary();
        showPlayerScores();
    }

    private String composeTitle(GameOutcome outcome) {
        Player winner = outcome.getWinningPlayer();
        int winningTeam = outcome.getWinningTeam();
        if (winner == null) {
            return "It's a draw!";
        } else if (winningTeam != -1) {
            return "Team " + winner.getTeam() + " Won!";
        } else {
            return winner.getName() + " Won!";
        }
    }

    public FButton getBtnContinue() {
        return this.btnContinue;
    }

    public FButton getBtnRestart() {
        return this.btnRestart;
    }

    public FButton getBtnQuit() {
        return this.btnQuit;
    }

    public FPanel getPnlCustom() {
        return this.pnlCustom;
    }

    private void showGameOutcomeSummary() {
        GameLog log = game.getGameLog();
        for (GameLogEntry o : log.getLogEntriesExact(GameLogEntryType.GAME_OUTCOME)) {
            pnlOutcomes.add(new FLabel.Builder().text(o.message).fontSize(14).build());
        }
    }

    private void showPlayerScores() {
        GameLog log = game.getGameLog();
        for (GameLogEntry o : log.getLogEntriesExact(GameLogEntryType.MATCH_RESULTS)) {
            lblStats.setText(removePlayerTypeFromLogMessage(o.message));
        }
    }

    private String removePlayerTypeFromLogMessage(String message) {
        return message.replaceAll("\\[[^\\]]*\\]", "");
    }

    @Override
    protected void doLayout(float width, float height) {
        // TODO Auto-generated method stub
        
    }

    private class OutcomesPanel extends FContainer {
        @Override
        protected void doLayout(float width, float height) {
            float y = 0;
            float lblHeight = 20;
            for (FDisplayObject lbl : getChildren()) {
                lbl.setBounds(0, y, width, lblHeight);
                y += lblHeight;
            }
        }
    }
}
