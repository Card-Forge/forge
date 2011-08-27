package forge.view.swing;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import net.slightlymagic.braids.util.UtilFunctions;
import forge.view.util.ProgressBar_Base;

import com.esotericsoftware.minlog.Log;

import forge.AllZone;
import forge.ComputerAI_General;
import forge.ComputerAI_Input;
import forge.Constant;
import forge.ImageCache;
import forge.error.ErrorViewer;
import forge.model.FModel;
import forge.properties.ForgePreferences;
import forge.view.FView;
import forge.view.swing.OldGuiNewGame.CardSizesAction;
import forge.view.swing.OldGuiNewGame.CardStackAction;
import forge.view.swing.OldGuiNewGame.CardStackOffsetAction;

/**
 * The main view for Forge: a java swing application.
 */
public class ApplicationView implements FView {

    private transient SplashFrame splashFrame;

    /**
     * The splashFrame field is guaranteed to exist when this constructor
     * exits.
     */
    public ApplicationView() {

        // We must use invokeAndWait here to fulfill the constructor's
        // contract.

        UtilFunctions.invokeInEventDispatchThreadAndWait(new Runnable() { // NOPMD by Braids on 8/18/11 11:37 PM
            public void run() {
                splashFrame = new SplashFrame();
            }
        });

        SwingUtilities.invokeLater(new Runnable() { // NOPMD by Braids on 8/18/11 11:37 PM
            public void run() {
                splashFrame.setVisible(true);
            }
        });
    }

    /* (non-Javadoc)
     * @see forge.view.FView#getCardLoadingProgressMonitor()
     */
    @Override
    public final ProgressBar_Base getCardLoadingProgressMonitor() {
        ProgressBar_Base result;

        if (splashFrame == null) {
            result = null;
        }
        else {
            result = splashFrame.getBar();
        }

        return result;
    }

    /* (non-Javadoc)
     * @see forge.view.FView#setModel(forge.model.FModel)
     */
    @Override
    public final void setModel(final FModel model) {
        try {

            final ForgePreferences preferences = model.getPreferences();

            OldGuiNewGame.useLAFFonts.setSelected(preferences.lafFonts);
            // newGuiCheckBox.setSelected(preferences.newGui);
            OldGuiNewGame.smoothLandCheckBox.setSelected(preferences.stackAiLand);
            OldGuiNewGame.devModeCheckBox.setSelected(preferences.developerMode);
            OldGuiNewGame.cardOverlay.setSelected(preferences.cardOverlay);

            // FindBugs doesn't like the next line.
            ImageCache.scaleLargerThanOriginal = preferences.scaleLargerThanOriginal;

            OldGuiNewGame.cardScale.setSelected(preferences.scaleLargerThanOriginal);
            CardStackOffsetAction.set(preferences.stackOffset);
            CardStackAction.setVal(preferences.maxStackSize);
            CardSizesAction.set(preferences.cardSize);
            OldGuiNewGame.upldDrftCheckBox.setSelected(preferences.uploadDraftAI);
            OldGuiNewGame.foilRandomCheckBox.setSelected(preferences.randCFoil);

        } catch (Exception exn) {
            Log.error("Error loading preferences: " + exn);
        }

        OldGuiNewGame.loadDynamicGamedata();

        SwingUtilities.invokeLater(new Runnable() { // NOPMD by Braids on 8/7/11 1:07 PM: this isn't a web app
            public void run() {
                final ForgePreferences finalPreferences = model.getPreferences();

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

        AllZone.getCardFactory(); // forces preloading of all cards
        try {
            Constant.Runtime.GameType[0] = Constant.GameType.Constructed;
            SwingUtilities.invokeLater(new Runnable() { // NOPMD by Braids on 8/7/11 1:07 PM: this isn't a web app
                public void run() {
                    AllZone.setComputer(new ComputerAI_Input(new ComputerAI_General()));

                    getCardLoadingProgressMonitor().dispose();

                    // Enable only one of the following two lines. The second
                    // is useful for debugging.

                    splashFrame.dispose();
                    //splashFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);


                    splashFrame = null;
                    new OldGuiNewGame();
                }
            });
        } catch (Exception ex) {
            ErrorViewer.showError(ex);
        }

    }
}
