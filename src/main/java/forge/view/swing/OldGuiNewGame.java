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

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;

import forge.Constant;
import forge.Singletons;
import forge.error.ErrorViewer;
import forge.gui.ListChooser;
import forge.properties.ForgePreferences.CardSizeType;
import forge.properties.ForgePreferences.StackOffsetType;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;

/**
 * <p>
 * OldGuiNewGame class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public final class OldGuiNewGame {

    // @SuppressWarnings("unused")
    // titledBorder2
    /** Constant <code>smoothLandCheckBox</code>. */
    private static JCheckBox smoothLandCheckBox = new JCheckBox("", false);
    /** Constant <code>devModeCheckBox</code>. */
    private static JCheckBox devModeCheckBox = new JCheckBox("", true);

    /** The upld drft check box. */
    private static JCheckBox upldDrftCheckBox = new JCheckBox("", true);

    /** The foil random check box. */
    private static JCheckBox foilRandomCheckBox = new JCheckBox("", true);
    /** Constant <code>cardOverlay</code>. */
    private static JCheckBoxMenuItem cardOverlay = new JCheckBoxMenuItem(
            ForgeProps.getLocalized(NewConstants.Lang.OldGuiNewGame.MenuBar.Options.CARD_OVERLAY));
    /** Constant <code>cardScale</code>. */
    private static JCheckBoxMenuItem cardScale = new JCheckBoxMenuItem(
            ForgeProps.getLocalized(NewConstants.Lang.OldGuiNewGame.MenuBar.Options.CARD_SCALE));

    /**
     * <p>
     * Constructor for OldGuiNewGame.
     * </p>
     */
    private OldGuiNewGame() {
        throw new AssertionError();
    }

    /**
     * The Class CardSizesAction.
     * 
     * @author dhudson
     */
    public static class CardSizesAction extends AbstractAction {

        private static final long serialVersionUID = -2900235618450319571L;
        private static String[] keys = { "Tiny", "Smaller", "Small", "Medium", "Large", "Huge" };
        private static int[] widths = { 52, 80, 120, 200, 300, 400 };

        /**
         * Instantiates a new card sizes action.
         */
        public CardSizesAction() {
            super(ForgeProps.getLocalized(NewConstants.Lang.OldGuiNewGame.MenuBar.Menu.CARD_SIZES));
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent
         * )
         */
        /**
         * Action performed.
         * 
         * @param e
         *            ActionEvent
         */
        @Override
        public final void actionPerformed(final ActionEvent e) {
            final ListChooser<String> ch = new ListChooser<String>("Choose one", "Choose a new max card size", 0, 1,
                    CardSizesAction.keys);
            if (ch.show()) {
                try {
                    final int index = ch.getSelectedIndex();
                    if (index == -1) {
                        return;
                    }
                    CardSizesAction.set(index);
                } catch (final Exception ex) {
                    ErrorViewer.showError(ex);
                }
            }
        }

        /**
         * Sets the.
         * 
         * @param index
         *            the index
         */
        public static void set(final int index) {
            Singletons.getModel().getPreferences()
                    .setCardSize(CardSizeType.valueOf(CardSizesAction.keys[index].toLowerCase()));
            Constant.Runtime.WIDTH[0] = CardSizesAction.widths[index];
            Constant.Runtime.HEIGHT[0] = (int) Math.round((CardSizesAction.widths[index] * (3.5 / 2.5)));
        }

        /**
         * Sets the.
         * 
         * @param s
         *            the s
         */
        public static void set(final CardSizeType s) {
            Singletons.getModel().getPreferences().setCardSize(s);
            int index = 0;
            for (final String str : CardSizesAction.keys) {
                if (str.toLowerCase().equals(s.toString())) {
                    break;
                }
                index++;
            }
            Constant.Runtime.WIDTH[0] = CardSizesAction.widths[index];
            Constant.Runtime.HEIGHT[0] = (int) Math.round((CardSizesAction.widths[index] * (3.5 / 2.5)));
        }
    }

    /**
     * The Class CardStackAction.
     * 
     * @author dhudson
     */
    public static class CardStackAction extends AbstractAction {

        private static final long serialVersionUID = -3770527681359311455L;
        private static String[] keys = { "3", "4", "5", "6", "7", "8", "9", "10", "11", "12" };
        private static int[] values = { 3, 4, 5, 6, 7, 8, 9, 10, 11, 12 };

        /**
         * Instantiates a new card stack action.
         */
        public CardStackAction() {
            super(ForgeProps.getLocalized(NewConstants.Lang.OldGuiNewGame.MenuBar.Menu.CARD_STACK));
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent
         * )
         */
        /**
         * Action performed.
         * 
         * @param e
         *            ActionEvent
         */
        @Override
        public final void actionPerformed(final ActionEvent e) {

            final ListChooser<String> ch = new ListChooser<String>("Choose one", "Choose the max size of a stack", 0,
                    1, CardStackAction.keys);

            if (ch.show()) {
                try {
                    final int index = ch.getSelectedIndex();
                    if (index == -1) {
                        return;
                    }
                    CardStackAction.set(index);

                } catch (final Exception ex) {
                    ErrorViewer.showError(ex);
                }
            }
        }

        /**
         * Sets the.
         * 
         * @param index
         *            the index
         */
        public static void set(final int index) {
            Singletons.getModel().getPreferences().setMaxStackSize(CardStackAction.values[index]);
            Constant.Runtime.STACK_SIZE[0] = CardStackAction.values[index];
        }

        /**
         * Sets the val.
         * 
         * @param val
         *            the new val
         */
        public static void setVal(final int val) {
            Singletons.getModel().getPreferences().setMaxStackSize(val);
            Constant.Runtime.STACK_SIZE[0] = val;
        }
    }

    /**
     * The Class CardStackOffsetAction.
     * 
     * @author dhudson
     */
    public static class CardStackOffsetAction extends AbstractAction {

        private static final long serialVersionUID = 5021304777748833975L;
        private static String[] keys = { "Tiny", "Small", "Medium", "Large" };
        private static int[] offsets = { 5, 7, 10, 15 };

        /**
         * Instantiates a new card stack offset action.
         */
        public CardStackOffsetAction() {
            super(ForgeProps.getLocalized(NewConstants.Lang.OldGuiNewGame.MenuBar.Menu.CARD_STACK_OFFSET));
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent
         * )
         */
        /**
         * Action performed.
         * 
         * @param e
         *            ActionEvent
         */
        @Override
        public final void actionPerformed(final ActionEvent e) {
            final ListChooser<String> ch = new ListChooser<String>("Choose one", "Choose a stack offset value", 0, 1,
                    CardStackOffsetAction.keys);
            if (ch.show()) {
                try {
                    final int index = ch.getSelectedIndex();
                    if (index == -1) {
                        return;
                    }
                    CardStackOffsetAction.set(index);

                } catch (final Exception ex) {
                    ErrorViewer.showError(ex);
                }
            }
        }

        /**
         * Sets the.
         * 
         * @param index
         *            the index
         */
        public static void set(final int index) {
            Singletons.getModel().getPreferences()
                    .setStackOffset(StackOffsetType.valueOf(CardStackOffsetAction.keys[index].toLowerCase()));
            Constant.Runtime.STACK_OFFSET[0] = CardStackOffsetAction.offsets[index];
        }

        /**
         * Sets the.
         * 
         * @param s
         *            the s
         */
        public static void set(final StackOffsetType s) {
            Singletons.getModel().getPreferences().setStackOffset(s);
            int index = 0;
            for (final String str : CardStackOffsetAction.keys) {
                if (str.toLowerCase().equals(s.toString())) {
                    break;
                }
                index++;
            }
            Constant.Runtime.STACK_OFFSET[0] = CardStackOffsetAction.offsets[index];
        }
    }

    /**
     * Gets the card overlay.
     * 
     * @return the cardOverlay
     */
    public static JCheckBoxMenuItem getCardOverlay() {
        return OldGuiNewGame.cardOverlay;
    }

    /**
     * Sets the card overlay.
     * 
     * @param cardOverlay0
     *            the cardOverlay to set
     */
    public static void setCardOverlay(final JCheckBoxMenuItem cardOverlay0) {
        OldGuiNewGame.cardOverlay = cardOverlay0;
    }

    /**
     * Gets the card scale.
     * 
     * @return the cardScale
     */
    public static JCheckBoxMenuItem getCardScale() {
        return OldGuiNewGame.cardScale;
    }

    /**
     * Sets the card scale.
     * 
     * @param cardScale0
     *            the cardScale to set
     */
    public static void setCardScale(final JCheckBoxMenuItem cardScale0) {
        OldGuiNewGame.cardScale = cardScale0;
    }

    /**
     * Gets the smooth land check box.
     * 
     * @return the smoothLandCheckBox
     */
    static JCheckBox getSmoothLandCheckBox() {
        return OldGuiNewGame.smoothLandCheckBox;
    }

    /**
     * Sets the smooth land check box.
     * 
     * @param smoothLandCheckBox0
     *            the smoothLandCheckBox to set
     */
    static void setSmoothLandCheckBox(final JCheckBox smoothLandCheckBox0) {
        OldGuiNewGame.smoothLandCheckBox = smoothLandCheckBox0;
    }

    /**
     * Gets the dev mode check box.
     * 
     * @return the devModeCheckBox
     */
    public static JCheckBox getDevModeCheckBox() {
        return OldGuiNewGame.devModeCheckBox;
    }

    /**
     * Sets the dev mode check box.
     * 
     * @param devModeCheckBox0
     *            the devModeCheckBox to set
     */
    public static void setDevModeCheckBox(final JCheckBox devModeCheckBox0) {
        OldGuiNewGame.devModeCheckBox = devModeCheckBox0;
    }

    /**
     * Gets the upld drft check box.
     * 
     * @return the upldDrftCheckBox
     */
    public static JCheckBox getUpldDrftCheckBox() {
        return OldGuiNewGame.upldDrftCheckBox;
    }

    /**
     * Sets the upld drft check box.
     * 
     * @param upldDrftCheckBox0
     *            the upldDrftCheckBox to set
     */
    public static void setUpldDrftCheckBox(final JCheckBox upldDrftCheckBox0) {
        OldGuiNewGame.upldDrftCheckBox = upldDrftCheckBox0;
    }

    /**
     * Gets the foil random check box.
     * 
     * @return the foilRandomCheckBox
     */
    public static JCheckBox getFoilRandomCheckBox() {
        return OldGuiNewGame.foilRandomCheckBox;
    }

    /**
     * Sets the foil random check box.
     * 
     * @param foilRandomCheckBox0
     *            the foilRandomCheckBox to set
     */
    public static void setFoilRandomCheckBox(final JCheckBox foilRandomCheckBox0) {
        OldGuiNewGame.foilRandomCheckBox = foilRandomCheckBox0;
    }
}
