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
import forge.AllZone;
import forge.ComputerAIGeneral;
import forge.ComputerAIInput;
import forge.Constant;
import forge.control.FControl;
import forge.game.GameType;
import forge.view.home.SplashFrame;
import forge.view.toolbox.CardFaceSymbols;
import forge.view.toolbox.FProgressBar;
import forge.view.toolbox.FSkin;

/**
 * The main view for Forge: a java swing application. All view class instances
 * should be accessible from here.
 */
public class FView {

    private transient SplashFrame splashFrame;
    private FProgressBar barProgress = null;
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
                try {
                    FView.this.splashFrame = new SplashFrame(FView.this.skin);
                } catch (Exception e) {
                    e.printStackTrace();
                }
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
     * Allows singleton (global) access to a progress bar (which must be set first).
     * 
     * @return a progress monitor having only one phase; may be null
     */
    public final FProgressBar getProgressBar() {
        return this.barProgress;
    }

    /** 
     * Sets a progress bar so it can be accessed via singletons.
     * 
     * @param bar0 &emsp; An FProgressBar object
     */
    public final void setProgressBar(FProgressBar bar0) {
        this.barProgress = bar0;
    }

    /** @return FSkin */
    public FSkin getSkin() {
        return this.skin;
    }

    /** @param skin0 &emsp; FSkin */
    public void setSkin(final FSkin skin0) {
        this.skin = skin0;
    }

    /**
     * Tell the view that the model has been bootstrapped, and its data is ready
     * for initial display.
     */
    public final void initialize() {
        this.setProgressBar(splashFrame.getProgressBar());

        // Preloads all cards (using progress bar).
        AllZone.getCardFactory();

        // Preloads skin components (using progress bar).
        FView.this.skin.loadFontsAndImages();

        // Does not use progress bar, due to be deprecated in favor of skin.
        CardFaceSymbols.loadImages();

        barProgress.setDescription("Creating display components.");

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {

                // TODO there must be a better place for this. ////////////
                Constant.Runtime.setGameType(GameType.Constructed);
                AllZone.getInputControl().setComputer(new ComputerAIInput(new ComputerAIGeneral()));
                /////////////////////////////////////

                final GuiTopLevel g = new GuiTopLevel();
                AllZone.setDisplay(g);
                g.getController().changeState(FControl.HOME_SCREEN);
                g.pack();

                FView.this.splashFrame.dispose();
                FView.this.splashFrame = null;

                barProgress.setDescription("Forge is ready to launch.");
                g.setVisible(true);
            }
        });
    } // End FView()
}
