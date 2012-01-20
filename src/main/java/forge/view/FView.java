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
package forge.view;

import javax.swing.SwingUtilities;

import net.slightlymagic.braids.util.UtilFunctions;
import net.slightlymagic.braids.util.progress_monitor.BraidsProgressMonitor;
import forge.AllZone;
import forge.ComputerAIGeneral;
import forge.ComputerAIInput;
import forge.Constant;
import forge.control.FControl;
import forge.error.ErrorViewer;
import forge.game.GameType;
import forge.view.home.SplashFrame;
import forge.view.toolbox.CardFaceSymbols;
import forge.view.toolbox.FSkin;

/**
 * The main view for Forge: a java swing application. All view class instances
 * should be accessible from here.
 */
public class FView {

    private transient SplashFrame splashFrame;
    private FSkin skin;

    /**
     * The splashFrame field is guaranteed to exist when this constructor exits.
     * 
     * @param skin0
     *            the skin
     */
    public FView(final FSkin skin0) {
        this.skin = skin0;

        // We must use invokeAndWait here to fulfill the constructor's
        // contract. NOPMD by Braids on 8/18/11 11:37 PM
        UtilFunctions.invokeInEventDispatchThreadAndWait(new Runnable() {
            @Override
            public void run() {
                FView.this.splashFrame = new SplashFrame(FView.this.skin);
            }
        });

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                FView.this.splashFrame.setVisible(true);
            }
        });
    }

    /**
     * Get the progress monitor for loading all cards at once.
     * 
     * @return a progress monitor having only one phase; may be null
     */
    public final BraidsProgressMonitor getCardLoadingProgressMonitor() {
        BraidsProgressMonitor result;

        if (this.splashFrame == null) {
            result = null;
        } else {
            result = this.splashFrame.getMonitorModel();
        }

        return result;
    }

    /** @return FSkin */
    public FSkin getSkin() {
        return this.skin;
    }

    /**
     * @param skin0
     *            &emsp; FSkin
     */
    public void setSkin(final FSkin skin0) {
        this.skin = skin0;
    }

    /**
     * Tell the view that the model has been bootstrapped, and its data is ready
     * for initial display.
     */
    public final void initialize() {
        // For the following two blocks, check if user has cancelled
        // SplashFrame.
        // Note: Error thrown sometimes because log file cannot be accessed
        if (!this.splashFrame.getSplashHasBeenClosed()) {
            AllZone.getCardFactory(); // forces preloading of all cards
        }

        if (!this.splashFrame.getSplashHasBeenClosed()) {
            try {

                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        AllZone.getInputControl().setComputer(new ComputerAIInput(new ComputerAIGeneral()));
                        FView.this.skin.loadFontAndImages();

                        CardFaceSymbols.loadImages();

                        Constant.Runtime.setGameType(GameType.Constructed);

                        final GuiTopLevel g = new GuiTopLevel();
                        AllZone.setDisplay(g);
                        g.getController().changeState(FControl.HOME_SCREEN);
                        g.pack();
                        g.setVisible(true);

                        FView.this.splashFrame.dispose();
                        // Enable only one of the following two lines.
                        // The second is useful for debugging.
                        FView.this.splashFrame = null;
                        // FView.this.splashFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                    }
                });
            } catch (final Exception ex) {
                ErrorViewer.showError(ex);
            }
        } // End if(splashHasBeenClosed) */
    } // End FView()
}
