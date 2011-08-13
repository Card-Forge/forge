package forge.view.swing;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.esotericsoftware.minlog.Log;

import forge.AllZone;
import forge.ComputerAI_General;
import forge.ComputerAI_Input;
import forge.Constant;
import forge.HttpUtil;
import forge.ImageCache;
import forge.Singletons;
import forge.error.ErrorViewer;
import forge.error.ExceptionHandler;
import forge.model.FModel;
import forge.properties.ForgePreferences;
import forge.view.FView;
import forge.view.swing.OldGuiNewGame.CardSizesAction;
import forge.view.swing.OldGuiNewGame.CardStackAction;
import forge.view.swing.OldGuiNewGame.CardStackOffsetAction;

/**
 * Main class for Forge's swing application view.
 */
public final class Main {

    /**
     * Do not instantiate.
     */
    private Main() {
        // intentionally blank
    }

    /**
     * main method for Forge's swing application view.
     *
     * @param args
     *            an array of {@link java.lang.String} objects.
     */
    public static void main(final String[] args) {
        ExceptionHandler.registerErrorHandling();
        try {
            final FModel model = new FModel(null);
            Singletons.setModel(model);
            final FView view = new ApplicationView();
            Singletons.setView(view);

            ForgePreferences preferences = null;

            try {

                preferences = model.getPreferences();

                OldGuiNewGame.useLAFFonts.setSelected(preferences.lafFonts);
                // newGuiCheckBox.setSelected(preferences.newGui);
                OldGuiNewGame.smoothLandCheckBox.setSelected(preferences.stackAiLand);
                Constant.Runtime.Mill[0] = preferences.millingLossCondition;
                Constant.Runtime.DevMode[0] = preferences.developerMode;
                OldGuiNewGame.devModeCheckBox.setSelected(preferences.developerMode);
                OldGuiNewGame.cardOverlay.setSelected(preferences.cardOverlay);
                ImageCache.scaleLargerThanOriginal = preferences.scaleLargerThanOriginal;
                OldGuiNewGame.cardScale.setSelected(preferences.scaleLargerThanOriginal);
                CardStackOffsetAction.set(preferences.stackOffset);
                CardStackAction.setVal(preferences.maxStackSize);
                CardSizesAction.set(preferences.cardSize);
                Constant.Runtime.UpldDrft[0] = preferences.uploadDraftAI;
                OldGuiNewGame.upldDrftCheckBox.setSelected(preferences.uploadDraftAI);
                Constant.Runtime.RndCFoil[0] = preferences.randCFoil;
                OldGuiNewGame.foilRandomCheckBox.setSelected(preferences.randCFoil);

                final HttpUtil pinger = new HttpUtil();
                if (pinger.getURL("http://cardforge.org/draftAI/ping.php").equals("pong")) {
                    Constant.Runtime.NetConn[0] = true;
                } else {
                    Constant.Runtime.UpldDrft[0] = false;
                }

            } catch (Exception exn) {
                Log.error("Error loading preferences: " + exn);
            }

            OldGuiNewGame.loadDynamicGamedata();

            final ForgePreferences finalPreferences = preferences;

            SwingUtilities.invokeLater(new Runnable() { // NOPMD by Braids on 8/7/11 1:07 PM: this isn't a web app
                public void run() {
                    try {
                        if ("".equals(finalPreferences.laf)) {
                            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                        } else {
                            UIManager.setLookAndFeel(finalPreferences.laf);
                        }
                    } catch (Exception ex) {
                        ErrorViewer.showError(ex);
                    }
                }
            });

            try {
                Constant.Runtime.GameType[0] = Constant.GameType.Constructed;
                SwingUtilities.invokeLater(new Runnable() { // NOPMD by Braids on 8/7/11 1:07 PM: this isn't a web app
                    public void run() {
                        AllZone.setComputer(new ComputerAI_Input(new ComputerAI_General()));
                        new OldGuiNewGame();
                    }
                });
            } catch (Exception ex) {
                ErrorViewer.showError(ex);
            }

        }
        catch (Throwable exn) { // NOPMD by Braids on 8/7/11 1:07 PM: must catch all throwables here.
            ErrorViewer.showError(exn);
        }
    }
}
