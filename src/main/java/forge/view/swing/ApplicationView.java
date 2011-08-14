package forge.view.swing;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

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
    /**
     * Constructor.
     */
    public ApplicationView() { // NOPMD by Braids on 8/7/11 1:14 PM: Damnation if it's here; Damnation if it's not.
        // TODO: insert splash window here
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
}
