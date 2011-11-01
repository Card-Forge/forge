package forge.view.swing;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import net.slightlymagic.braids.util.UtilFunctions;
import net.slightlymagic.braids.util.progress_monitor.BraidsProgressMonitor;
import arcane.ui.util.ManaSymbols;

import com.esotericsoftware.minlog.Log;

import forge.AllZone;
import forge.ComputerAI_General;
import forge.ComputerAI_Input;
import forge.Constant;
import forge.ImageCache;
import forge.error.ErrorViewer;
import forge.game.GameType;
import forge.gui.skin.FSkin;
import forge.model.FModel;
import forge.properties.ForgePreferences;
import forge.view.FView;
import forge.view.swing.OldGuiNewGame.CardSizesAction;
import forge.view.swing.OldGuiNewGame.CardStackAction;
import forge.view.swing.OldGuiNewGame.CardStackOffsetAction;

/**
 * The main view for Forge: a java swing application. All view class instances
 * should be accessible from here.
 */
public class ApplicationView implements FView {

    private transient SplashFrame splashFrame;

    /**
     * The splashFrame field is guaranteed to exist when this constructor exits.
     * 
     * @param skin
     *            the skin
     */
    public ApplicationView(final FSkin skin) {

        // We must use invokeAndWait here to fulfill the constructor's
        // contract.

        UtilFunctions.invokeInEventDispatchThreadAndWait(new Runnable() { // NOPMD
                                                                          // by
                                                                          // Braids
                                                                          // on
                                                                          // 8/18/11
                                                                          // 11:37
                                                                          // PM
                    public void run() {
                        splashFrame = new SplashFrame(skin);
                    }
                });

        SwingUtilities.invokeLater(new Runnable() { // NOPMD by Braids on
                                                    // 8/18/11 11:37 PM
                    public void run() {
                        splashFrame.setVisible(true);
                    }
                });

    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.view.FView#getCardLoadingProgressMonitor()
     */
    @Override
    public final BraidsProgressMonitor getCardLoadingProgressMonitor() {
        BraidsProgressMonitor result;

        if (splashFrame == null) {
            result = null;
        } else {
            result = splashFrame.getMonitorModel();
        }

        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.view.FView#setModel(forge.model.FModel)
     */

    @Override
    public final void setModel(final FModel model) {
        try {

            final ForgePreferences preferences = model.getPreferences();

            OldGuiNewGame.useLAFFonts.setSelected(preferences.isLafFonts());
            // newGuiCheckBox.setSelected(preferences.newGui);
            OldGuiNewGame.smoothLandCheckBox.setSelected(preferences.isStackAiLand());
            OldGuiNewGame.devModeCheckBox.setSelected(preferences.isDeveloperMode());
            OldGuiNewGame.cardOverlay.setSelected(preferences.isCardOverlay());

            // FindBugs doesn't like the next line.
            ImageCache.setScaleLargerThanOriginal(preferences.isScaleLargerThanOriginal());

            OldGuiNewGame.cardScale.setSelected(preferences.isScaleLargerThanOriginal());
            CardStackOffsetAction.set(preferences.getStackOffset());
            CardStackAction.setVal(preferences.getMaxStackSize());
            CardSizesAction.set(preferences.getCardSize());
            OldGuiNewGame.upldDrftCheckBox.setSelected(preferences.isUploadDraftAI());
            OldGuiNewGame.foilRandomCheckBox.setSelected(preferences.isRandCFoil());

            AllZone.setSkin(new FSkin(preferences.getSkin()));

        } catch (Exception exn) {
            Log.error("Error loading preferences: " + exn);
        }

        SwingUtilities.invokeLater(new Runnable() { // NOPMD by Braids on 8/7/11
                                                    // 1:07 PM: this isn't a web
                                                    // app
                    public void run() {
                        final ForgePreferences finalPreferences = model.getPreferences();

                        try {
                            if ("".equals(finalPreferences.getLaf())) {
                                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                            } else {
                                UIManager.setLookAndFeel(finalPreferences.getLaf());
                            }
                        } catch (Exception ex) {
                            ErrorViewer.showError(ex);
                        }
                    }
                });

        // For the following two blocks, check if user has cancelled
        // SplashFrame.
        // Note: Error thrown sometimes because log file cannot be accessed
        if (!splashFrame.getSplashHasBeenClosed()) {
            AllZone.getCardFactory(); // forces preloading of all cards
        }

        if (!splashFrame.getSplashHasBeenClosed()) {
            try {
                ManaSymbols.loadImages();

                Constant.Runtime.setGameType(GameType.Constructed);
                SwingUtilities.invokeLater(new Runnable() { // NOPMD by Braids
                                                            // on 8/7/11 1:07
                                                            // PM: this isn't a
                                                            // web app
                            public void run() {
                                AllZone.getInputControl().setComputer(new ComputerAI_Input(new ComputerAI_General()));

                                // Enable only one of the following two lines.
                                // The second
                                // is useful for debugging.

                                splashFrame.dispose();
                                // splashFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

                                splashFrame = null;

                                if (System.getenv("NG2") != null) {
                                    if (System.getenv("NG2").equalsIgnoreCase("true")) {
                                        String[] argz = {};
                                        Gui_HomeScreen.main(argz);
                                    } else {
                                        new OldGuiNewGame();
                                    }
                                } else {
                                    new OldGuiNewGame();
                                }
                            }
                        });
            } catch (Exception ex) {
                ErrorViewer.showError(ex);
            }
        } // End if(splashHasBeenClosed)

    } // End ApplicationView()
}
