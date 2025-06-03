package forge.screens.match.winlose;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.utils.Align;

import forge.Forge;
import forge.assets.FSkinColor;
import forge.assets.FSkinColor.Colors;
import forge.assets.FSkinFont;
import forge.game.GameLogEntry;
import forge.game.GameLogEntryType;
import forge.game.GameView;
import forge.gui.FThreads;
import forge.gui.interfaces.IWinLoseView;
import forge.gui.util.SGuiChoose;
import forge.gui.util.SOptionPane;
import forge.item.PaperCard;
import forge.localinstance.skin.FSkinProp;
import forge.menu.FMagnifyView;
import forge.model.FModel;
import forge.toolbox.FButton;
import forge.toolbox.FContainer;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FLabel;
import forge.toolbox.FOverlay;
import forge.toolbox.FTextArea;
import forge.util.Utils;

public class ViewWinLose extends FOverlay implements IWinLoseView<FButton> {
    private static final float INSETS_FACTOR = 0.025f;
    private static final float GAP_Y_FACTOR = 0.02f;

    private final FButton btnContinue, btnRestart, btnQuit;
    private final FLabel lblTitle, lblLog, lblStats, btnCopyLog, btnShowBattlefield;
    private final FTextArea txtLog;
    private final OutcomesPanel pnlOutcomes;
    private final GameView game;

    public ViewWinLose(final GameView game0) {
        super(FSkinColor.get(Colors.CLR_OVERLAY).alphaColor(0.75f));

        game = game0;

        lblTitle = add(new FLabel.Builder().font(FSkinFont.get(30)).align(Align.center).build());
        lblStats = add(new FLabel.Builder().font(FSkinFont.get(26)).align(Align.center).build());
        pnlOutcomes = add(new OutcomesPanel());

        btnContinue = add(new FButton());
        btnRestart = add(new FButton());
        btnQuit = add(new FButton());

        btnContinue.setText(Forge.getLocalizer().getMessage("btnNextGame"));
        btnContinue.setFont(FSkinFont.get(22));
        btnRestart.setText(Forge.getLocalizer().getMessage("btnStartNewMatch"));
        btnRestart.setFont(btnContinue.getFont());
        btnQuit.setText(Forge.getLocalizer().getMessage("btnQuitMatch"));
        btnQuit.setFont(btnContinue.getFont());
        btnContinue.setEnabled(!game0.isMatchOver());

        lblLog = add(new FLabel.Builder().text(Forge.getLocalizer().getMessage("lblGameLog")).align(Align.center).font(FSkinFont.get(18)).build());
        txtLog = add(new FTextArea(true, StringUtils.join(game.getGameLog().getLogEntries(null), "\r\n").replace("[COMPUTER]", "[AI]")) {
            @Override
            public boolean tap(float x, float y, int count) {
                if (txtLog.getMaxScrollTop() > 0) {
                    FMagnifyView.show(txtLog, txtLog.getText(), txtLog.getTextColor(), ViewWinLose.this.getBackColor(), txtLog.getFont(), true);
                }
                return true;
            }
        });
        txtLog.setFont(FSkinFont.get(12));

        btnCopyLog = add(new FLabel.ButtonBuilder().text(Forge.getLocalizer().getMessage("btnCopyToClipboard")).selectable()
                .command(e -> Forge.getClipboard().setContents(txtLog.getText())).build()
        );

        btnShowBattlefield = add(new FLabel.ButtonBuilder().text(Forge.getLocalizer().getMessage("lblShowBattlefield")).font(FSkinFont.get(12)).selectable()
                .command(e -> hide()).build()
        );
        lblTitle.setText(composeTitle(game0));

        // Control of the win/lose is handled differently for various game
        // modes.
        ControlWinLose control = null;
        switch (game0.getGameType()) {
        case Quest:
            control = new QuestWinLose(this, game0);
            break;
        case QuestDraft:
            control = new QuestDraftWinLose(this, game0);
            break;
        case PlanarConquest:
            control = new ConquestWinLose(this, game0);
            break;
        case Adventure:
        case AdventureEvent:
            control = new AdventureWinLose(this, game0);
            break;
        case Draft:
            if (!FModel.getGauntletMini().isGauntletDraft()) {
                break;
            }
        //$FALL-THROUGH$
        case Sealed:
            control = new LimitedWinLose(this, game0);
            break;
        case Gauntlet:
        case CommanderGauntlet:
        case DeckManager:
            control = new GauntletWinLose(this, game0);
            break;
        default: // will catch it after switch
            break;
        }
        if (control == null) {
            control = new ControlWinLose(this, game0);
        }

        showGameOutcomeSummary();
        showPlayerScores();
        control.showRewards();
    }

    private String composeTitle(final GameView game) {
        final String winner = game.getWinningPlayerName();
        final int winningTeam = game.getWinningTeam();
        if (winner == null) {
            return Forge.getLocalizer().getMessage("lblItsADraw");
        } else if (winningTeam != -1) {
            return Forge.getLocalizer().getMessage("lblTeamWon").replace("%s", String.valueOf(winningTeam));
        } else {
            return Forge.getLocalizer().getMessage("lblWinnerWon").replace("%s", winner);
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

    public FLabel getLabelShowBattlefield() {
        return this.btnShowBattlefield;
    }

    private void showGameOutcomeSummary() {
        for (GameLogEntry o : game.getGameLog().getLogEntriesExact(GameLogEntryType.GAME_OUTCOME)) {
            pnlOutcomes.add(new FLabel.Builder().text(o.message).font(FSkinFont.get(14)).build());
        }
    }

    private void showPlayerScores() {
        for (GameLogEntry o : game.getGameLog().getLogEntriesExact(GameLogEntryType.MATCH_RESULTS)) {
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
        if (Forge.isLandscapeMode()) { //prevent everything being too wide in Landscape mode
            x += width * 0.25f;
        }
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
        if (Forge.isMobileAdventureMode) {
            if (game.getNumGamesInMatch() > 1) {
                btnContinue.setBounds(x, y, w, h);
                y += h + dy;
            }
            else{
                btnContinue.setVisible(false);
            }
            btnQuit.setBounds(x, y, w, h);
            y += h + dy;

            btnRestart.setVisible(false);
        } else {
            if (btnContinue.isEnabled()) {
                btnContinue.setBounds(x, y, w, h);
                y += h + dy;
            } else {
                btnContinue.setBounds(-w, -w, 0, 0);
            }
            if (btnRestart.isEnabled()) {
                btnRestart.setBounds(x, y, w, h);
                y += h + dy;
            } else {
                btnRestart.setBounds(-w, -w, 0, 0);
            }
            if (btnQuit.isEnabled()) {
                btnQuit.setBounds(x, y, w, h);
                y += h + dy;
            } else {
                btnQuit.setBounds(-w, -w, 0, 0);
            }
        }


        h = lblLog.getAutoSizeBounds().height + dy;
        lblLog.setBounds(x, y, w, h);
        y += h;

        h = height / 16;
        float y2 = height - dy - h;
        btnCopyLog.setBounds(width / 4, y2, width / 2, h);
        txtLog.setBounds(x, y, w, y2 - y - dy);
        btnShowBattlefield.setBounds(width / 4,0,width / 2, h);
    }

    private static class OutcomesPanel extends FContainer {
        private static final float LBL_HEIGHT = Utils.scale(20);
 
        @Override
        protected void doLayout(float width, float height) {
            float y = 0;
            for (FDisplayObject lbl : getChildren()) {
                lbl.setBounds(0, y, width, LBL_HEIGHT);
                y += LBL_HEIGHT;
            }
        }
    }

    @Override
    public boolean keyDown(int keyCode) {
        if (keyCode == Keys.ESCAPE || keyCode == Keys.BACK) {
            btnQuit.trigger(); //quit on escape or back
            return true;
        } else if (keyCode == Keys.SPACE || keyCode == Keys.ENTER) {
            if (btnContinue.isEnabled()) {
                btnContinue.trigger();
            } else {
                btnQuit.trigger();
            }
            return true;
        }
        return super.keyDown(keyCode);
    }

    @Override
    public void showRewards(Runnable runnable) {
        //invoke reward logic in background thread so dialogs can be shown
        FThreads.invokeInBackgroundThread(runnable);
    }

    @Override
    public void showCards(String title, List<PaperCard> cards) {
        SGuiChoose.reveal(title, cards);
    }

    @Override
    public void showMessage(String message, String title, FSkinProp icon) {
        SOptionPane.showMessageDialog(message, title, icon);
    }
}
