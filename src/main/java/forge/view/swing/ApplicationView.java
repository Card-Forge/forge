/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.view.swing;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import net.slightlymagic.braids.util.UtilFunctions;
import net.slightlymagic.braids.util.progress_monitor.BraidsProgressMonitor;
import arcane.ui.util.ManaSymbols;

import com.esotericsoftware.minlog.Log;

import forge.AllZone;
import forge.ComputerAIGeneral;
import forge.ComputerAIInput;
import forge.Constant;
import forge.ImageCache;
import forge.error.ErrorViewer;
import forge.game.GameType;
import forge.model.FModel;
import forge.properties.ForgePreferences;
import forge.view.FView;
import forge.view.GuiTopLevel;
import forge.view.home.SplashFrame;
import forge.view.swing.OldGuiNewGame.CardSizesAction;
import forge.view.swing.OldGuiNewGame.CardStackAction;
import forge.view.swing.OldGuiNewGame.CardStackOffsetAction;
import forge.view.toolbox.FSkin;

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
                    @Override
                    public void run() {
                        ApplicationView.this.splashFrame = new SplashFrame(skin);
                    }
                });

        SwingUtilities.invokeLater(new Runnable() { // NOPMD by Braids on
                                                    // 8/18/11 11:37 PM
                    @Override
                    public void run() {
                        ApplicationView.this.splashFrame.setVisible(true);
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

        if (this.splashFrame == null) {
            result = null;
        } else {
            result = this.splashFrame.getMonitorModel();
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

            OldGuiNewGame.getOldGuiCheckBox().setSelected(preferences.isOldGui());
            OldGuiNewGame.getSmoothLandCheckBox().setSelected(preferences.isStackAiLand());
            OldGuiNewGame.getDevModeCheckBox().setSelected(preferences.isDeveloperMode());
            OldGuiNewGame.getCardOverlay().setSelected(preferences.isCardOverlay());

            // FindBugs doesn't like the next line.
            ImageCache.setScaleLargerThanOriginal(preferences.isScaleLargerThanOriginal());

            OldGuiNewGame.getCardScale().setSelected(preferences.isScaleLargerThanOriginal());
            CardStackOffsetAction.set(preferences.getStackOffset());
            CardStackAction.setVal(preferences.getMaxStackSize());
            CardSizesAction.set(preferences.getCardSize());
            OldGuiNewGame.getUpldDrftCheckBox().setSelected(preferences.isUploadDraftAI());
            OldGuiNewGame.getFoilRandomCheckBox().setSelected(preferences.isRandCFoil());

        } catch (final Exception exn) {
            Log.error("Error loading preferences: " + exn);
        }

        // For the following two blocks, check if user has cancelled
        // SplashFrame.
        // Note: Error thrown sometimes because log file cannot be accessed
        if (!this.splashFrame.getSplashHasBeenClosed()) {
            AllZone.getCardFactory(); // forces preloading of all cards
        }

        if (!this.splashFrame.getSplashHasBeenClosed()) {
            try {
                ManaSymbols.loadImages();

                Constant.Runtime.setGameType(GameType.Constructed);
                SwingUtilities.invokeLater(new Runnable() { // NOPMD by Braids
                                                            // on 8/7/11 1:07
                                                            // PM: this isn't a
                                                            // web app
                            @Override
                            public void run() {
                                AllZone.getInputControl().setComputer(new ComputerAIInput(new ComputerAIGeneral()));

                                // Enable only one of the following two lines.
                                // The second
                                // is useful for debugging.

                                ApplicationView.this.splashFrame.dispose();
                                // splashFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

                                ApplicationView.this.splashFrame = null;

                                if (!Constant.Runtime.OLDGUI[0]) {
                                    GuiTopLevel g = new GuiTopLevel();
                                    g.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                                    AllZone.setDisplay(g);
                                    g.getController().changeState(0);
                                }
                                else {
                                    new OldGuiNewGame();
                                }
                            }
                        });
            } catch (final Exception ex) {
                ErrorViewer.showError(ex);
            }
        } // End if(splashHasBeenClosed)

    } // End ApplicationView()
}
