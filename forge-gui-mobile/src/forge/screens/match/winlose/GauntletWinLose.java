package forge.screens.match.winlose;

import forge.game.GameView;
import forge.gamemodes.gauntlet.GauntletWinLoseController;
import forge.gui.FThreads;
import forge.gui.util.SOptionPane;
import forge.localinstance.assets.FSkinProp;
import forge.util.Localizer;

import java.util.List;

/**
 * The Win/Lose handler for 'gauntlet' type tournament
 * games.
 */
public class GauntletWinLose extends ControlWinLose {
    private final GauntletWinLoseController controller;

    /**
     * Instantiates a new gauntlet win/lose handler.
     * 
     * @param view0 ViewWinLose object
     * @param match
     */
    public GauntletWinLose(final ViewWinLose view0, GameView lastGame) {
        super(view0, lastGame);
        controller = new GauntletWinLoseController(view0, lastGame) {
            @Override
            protected void showOutcome(final boolean isMatchOver, final String message1, final String message2, final FSkinProp icon, final List<String> lstEventNames, final List<String> lstEventRecords, final int len, final int num) {
                if (!isMatchOver) { return; } //don't show progress dialog unless match over

                FThreads.invokeInBackgroundThread(new Runnable() {
                    @Override
                    public void run() {
                        StringBuilder sb = new StringBuilder();
                        if (!lstEventNames.isEmpty()) {
                            for (int i = 0; i < len; i++) {
                                if (i <= num) {
                                    sb.append(i + 1).append(". ").append(lstEventNames.get(i)).append(" (").append(lstEventRecords.get(i)).append(")\n");
                                } else {
                                    sb.append(i + 1).append(". ??????\n");
                                }
                            }
                        }

                        if (message1 != null) {
                            sb.append("\n");
                            sb.append(message1).append("\n\n");
                            sb.append(message2);
                        }
                        else {
                            if (sb.length() > 0) {
                                sb.deleteCharAt(sb.length() - 1); //remove final new line character
                            }
                        }

                        SOptionPane.showMessageDialog(sb.toString(), Localizer.getInstance().getMessage("lblGauntletProgress"), icon);
                    }
                });
            }

            @Override
            protected void saveOptions() {
                GauntletWinLose.this.saveOptions();
            }
        };
    }

    @Override
    public final void showRewards() {
        controller.showOutcome();
    }

    @Override
    public void actionOnContinue() {
        if (!controller.actionOnContinue()) {
            super.actionOnContinue();
        }
    }

    @Override
    public void actionOnQuit() {
        super.actionOnQuit();
        controller.actionOnQuit();
    }
}
