package forge.screens.match.winlose;

import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;

import forge.Forge;
import forge.assets.FSkinColor;
import forge.assets.FSkinColor.Colors;
import forge.game.Game;
import forge.game.GameLog;
import forge.game.GameLogEntry;
import forge.game.GameLogEntryType;
import forge.game.GameOutcome;
import forge.game.player.Player;
import forge.menu.FMagnifyView;
import forge.model.FModel;
import forge.toolbox.FButton;
import forge.toolbox.FContainer;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FEvent;
import forge.toolbox.FEvent.FEventHandler;
import forge.toolbox.FLabel;
import forge.toolbox.FOverlay;
import forge.toolbox.FPanel;
import forge.toolbox.FTextArea;
import forge.util.Utils;

public class ViewWinLose extends FOverlay {
    private static final float INSETS_FACTOR = 0.025f;
    private static final float GAP_Y_FACTOR = 0.02f;

    private final FButton btnContinue, btnRestart, btnQuit;
    private final FLabel lblTitle, lblLog, lblStats, btnCopyLog;
    private final FTextArea txtLog;
    private final FPanel pnlCustom;
    private final OutcomesPanel pnlOutcomes;
    private final Game game;

    public ViewWinLose(final Game game0) {
        super(FSkinColor.get(Colors.CLR_OVERLAY).alphaColor(0.75f));

        game = game0;
        
        lblTitle = add(new FLabel.Builder().fontSize(30).align(HAlignment.CENTER).build());
        lblStats = add(new FLabel.Builder().fontSize(26).align(HAlignment.CENTER).build());
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
        txtLog = add(new FTextArea(game.getGameLog().getLogText(null).replace("[COMPUTER]", "[AI]")) {
            @Override
            public boolean tap(float x, float y, int count) {
                if (txtLog.getMaxScrollTop() > 0) {
                    FMagnifyView.show(txtLog, txtLog.getText(), FTextArea.FORE_COLOR, ViewWinLose.this.getBackColor(), txtLog.getFont(), 0, txtLog.getWidth());
                }
                return true;
            }
        });
        txtLog.setFontSize(12);

        btnCopyLog = add(new FLabel.ButtonBuilder().text("Copy to clipboard").command(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
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
        float x = width * INSETS_FACTOR;
        float y = x;
        float w = width - 2 * x;
        float dy = height * GAP_Y_FACTOR;

        float h = height / 10;
        lblTitle.setBounds(x, y, w, h);
        y += h + dy;

        h = OutcomesPanel.LBL_HEIGHT * pnlOutcomes.getChildCount();
        pnlOutcomes.setBounds(x, y, w, h);
        y += h + dy;

        h = height / 10;
        lblStats.setBounds(x, y, w, h);
        y += h + dy;

        h = height / 12;
        btnContinue.setBounds(x, y, w, h);
        y += h + dy;
        btnRestart.setBounds(x, y, w, h);
        y += h + dy;
        btnQuit.setBounds(x, y, w, h);
        y += h + dy;

        h = lblLog.getAutoSizeBounds().height + dy;
        lblLog.setBounds(x, y, w, h);
        y += h;

        h = height / 16;
        float y2 = height - dy - h;
        btnCopyLog.setBounds(width / 4, y2, width / 2, h);
        txtLog.setBounds(x, y, w, y2 - y - dy);
    }

    private static class OutcomesPanel extends FContainer {
        private static final float LBL_HEIGHT = Utils.scaleY(20);
 
        @Override
        protected void doLayout(float width, float height) {
            float y = 0;
            for (FDisplayObject lbl : getChildren()) {
                lbl.setBounds(0, y, width, LBL_HEIGHT);
                y += LBL_HEIGHT;
            }
        }
    }
}
