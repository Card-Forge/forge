/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Nate
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
package forge.screens.match.views;

import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JPanel;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.EmptyBorder;

import forge.gui.MultiLineLabelUI;
import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.gui.framework.IVDoc;
import forge.screens.match.controllers.CDev;
import forge.toolbox.FScrollPane;
import forge.toolbox.FSkin;
import forge.toolbox.FSkin.SkinnedLabel;
import forge.util.Localizer;
import net.miginfocom.swing.MigLayout;

/** 
 * Assembles Swing components of players report.
 *
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 */
public class VDev implements IVDoc<CDev>, IDevListener {

    // Fields used with interface IVDoc
    private DragCell parentCell;
    private final DragTab tab = new DragTab(Localizer.getInstance().getMessage("lblDevMode"));

    // Top-level containers
    private final JPanel viewport = new JPanel(new MigLayout("wrap, insets 0, ax center"));
    private final FScrollPane scroller = new FScrollPane(viewport, true,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

    // Dev labels
    private final DevLabel lblUnlimitedLands = new DevLabel(Localizer.getInstance().getMessage("lblUnlimitedLands"));
    private final DevLabel lblViewAll = new DevLabel(Localizer.getInstance().getMessage("lblViewAll"));
    private final DevLabel lblGenerateMana = new DevLabel(Localizer.getInstance().getMessage("lblGenerateMana"));
    private final DevLabel lblSetupGame = new DevLabel(Localizer.getInstance().getMessage("lblSetupGame"));
    private final DevLabel lblDumpGame = new DevLabel(Localizer.getInstance().getMessage("lblDumpGame"));
    private final DevLabel lblTutor = new DevLabel(Localizer.getInstance().getMessage("lblTutor"));
    private final DevLabel lblAddCounterPermanent = new DevLabel(Localizer.getInstance().getMessage("lblAddCounterPermanent"));
    private final DevLabel lblSubCounterPermanent = new DevLabel(Localizer.getInstance().getMessage("lblSubCounterPermanent"));
    private final DevLabel lblTapPermanent = new DevLabel(Localizer.getInstance().getMessage("lblTapPermanent"));
    private final DevLabel lblUntapPermanent = new DevLabel(Localizer.getInstance().getMessage("lblUntapPermanent"));
    private final DevLabel lblSetLife = new DevLabel(Localizer.getInstance().getMessage("lblSetLife"));
    private final DevLabel lblWinGame = new DevLabel(Localizer.getInstance().getMessage("lblWinGame"));
    private final DevLabel lblCardToBattlefield = new DevLabel(Localizer.getInstance().getMessage("lblCardToBattlefield"));
    private final DevLabel lblTokenToBattlefield = new DevLabel(Localizer.getInstance().getMessage("lblTokenToBattlefield"));
    private final DevLabel lblExileFromPlay = new DevLabel(Localizer.getInstance().getMessage("lblExileFromPlay"));
    private final DevLabel lblCardToHand = new DevLabel(Localizer.getInstance().getMessage("lblCardToHand"));
    private final DevLabel lblExileFromHand = new DevLabel(Localizer.getInstance().getMessage("lblExileFromHand"));
    private final DevLabel lblCardToLibrary = new DevLabel(Localizer.getInstance().getMessage("lblCardToLibrary"));
    private final DevLabel lblCardToGraveyard = new DevLabel(Localizer.getInstance().getMessage("lblCardToGraveyard"));
    private final DevLabel lblCardToExile = new DevLabel(Localizer.getInstance().getMessage("lblCardToExile"));
    private final DevLabel lblCastSpell = new DevLabel(Localizer.getInstance().getMessage("lblCastSpellOrPlayLand"));
    private final DevLabel lblRepeatAddCard = new DevLabel(Localizer.getInstance().getMessage("lblRepeatAddCard"));
    private final DevLabel lblRemoveFromGame = new DevLabel(Localizer.getInstance().getMessage("lblRemoveFromGame"));

    private final DevLabel lblRiggedRoll = new DevLabel(Localizer.getInstance().getMessage("lblRiggedRoll"));
    private final DevLabel lblWalkTo = new DevLabel(Localizer.getInstance().getMessage("lblWalkTo"));

    private final DevLabel lblAskAI = new DevLabel(Localizer.getInstance().getMessage("lblAskAI"));
    private final DevLabel lblAskSimulationAI = new DevLabel(Localizer.getInstance().getMessage("lblAskSimulationAI"));


    private final CDev controller;

    //========= Constructor

    public VDev(final CDev controller) {
        this.controller = controller;

        final String halfConstraints = "w 47%!, gap 0 0 4px 0";
        final String halfConstraintsLeft = halfConstraints + ", split 2";
        viewport.setOpaque(false);
        viewport.add(this.lblGenerateMana, halfConstraintsLeft);
        viewport.add(this.lblTutor, halfConstraints);
        viewport.add(this.lblViewAll, halfConstraintsLeft);
        viewport.add(this.lblUnlimitedLands, halfConstraints);
        viewport.add(this.lblCastSpell, halfConstraintsLeft);
        viewport.add(this.lblCardToHand, halfConstraints);
        viewport.add(this.lblCardToLibrary, halfConstraintsLeft);
        viewport.add(this.lblCardToGraveyard, halfConstraints);
        viewport.add(this.lblCardToExile, halfConstraintsLeft);
        viewport.add(this.lblCardToBattlefield, halfConstraints);
        viewport.add(this.lblTokenToBattlefield, halfConstraintsLeft);
        viewport.add(this.lblRemoveFromGame, halfConstraints);
        viewport.add(this.lblRepeatAddCard, halfConstraintsLeft);
        viewport.add(this.lblExileFromHand, halfConstraints);
        viewport.add(this.lblExileFromPlay, halfConstraintsLeft);
        viewport.add(this.lblSetLife, halfConstraints);
        viewport.add(this.lblWinGame, halfConstraintsLeft);
        viewport.add(this.lblAddCounterPermanent, halfConstraints);
        viewport.add(this.lblSubCounterPermanent, halfConstraintsLeft);
        viewport.add(this.lblTapPermanent, halfConstraints);
        viewport.add(this.lblUntapPermanent, halfConstraintsLeft);
        viewport.add(this.lblRiggedRoll, halfConstraints);
        viewport.add(this.lblWalkTo, halfConstraintsLeft);
        viewport.add(this.lblAskAI, halfConstraints);
        viewport.add(this.lblAskSimulationAI, halfConstraintsLeft);
        viewport.add(this.lblSetupGame, halfConstraints);
        viewport.add(this.lblDumpGame, halfConstraintsLeft);
    }

    //========= Overridden methods

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#populate()
     */
    @Override
    public void populate() {
        parentCell.getBody().setLayout(new MigLayout("insets 0, gap 0"));
        parentCell.getBody().add(scroller, "w 100%, h 100%!");
    }

    @Override
    public void update(final boolean playUnlimitedLands, final boolean mayViewAllCards) {
        getLblUnlimitedLands().setToggled(playUnlimitedLands);
        getLblViewAll().setToggled(mayViewAllCards);
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#setParentCell()
     */
    @Override
    public void setParentCell(final DragCell cell0) {
        this.parentCell = cell0;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getParentCell()
     */
    @Override
    public DragCell getParentCell() {
        return this.parentCell;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getDocumentID()
     */
    @Override
    public EDocID getDocumentID() {
        return EDocID.DEV_MODE;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getTabLabel()
     */
    @Override
    public DragTab getTabLabel() {
        return tab;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getLayoutControl()
     */
    @Override
    public CDev getLayoutControl() {
        return controller;
    }

    //========= Retrieval methods

    /** @return {@link forge.screens.match.views.VDev.DevLabel} */
    public DevLabel getLblGenerateMana() {
        return this.lblGenerateMana;
    }

    /** @return {@link forge.screens.match.views.VDev.DevLabel} */
    public DevLabel getLblSetupGame() {
        return this.lblSetupGame;
    }

    /** @return {@link forge.screens.match.views.VDev.DevLabel} */
    public DevLabel getLblDumpGame() {
        return this.lblDumpGame;
    }
    
    /** @return {@link forge.screens.match.views.VDev.DevLabel} */
    public DevLabel getLblTutor() {
        return this.lblTutor;
    }

    /** @return {@link forge.screens.match.views.VDev.DevLabel} */
    public DevLabel getLblCardToHand() {
        return this.lblCardToHand;
    }

    /** @return {@link forge.screens.match.views.VDev.DevLabel} */
    public DevLabel getLblCardToLibrary() {
        return this.lblCardToLibrary;
    }

    /** @return {@link forge.screens.match.views.VDev.DevLabel} */
    public DevLabel getLblCardToGraveyard() {
        return this.lblCardToGraveyard;
    }

    /** @return {@link forge.screens.match.views.VDev.DevLabel} */
    public DevLabel getLblCardToExile() {
        return this.lblCardToExile;
    }

    /** @return {@link forge.screens.match.views.VDev.DevLabel} */
    public DevLabel getLblExileFromHand() {
        return this.lblExileFromHand;
    }

    /** @return {@link forge.screens.match.views.VDev.DevLabel} */
    public final DevLabel getLblCardToBattlefield() {
        return lblCardToBattlefield;
    }

    /** @return {@link forge.screens.match.views.VDev.DevLabel} */
    public final DevLabel getLblTokenToBattlefield() {
        return lblTokenToBattlefield;
    }

    /** @return {@link forge.screens.match.views.VDev.DevLabel} */
    public final DevLabel getLblRepeatAddCard() {
        return lblRepeatAddCard;
    }

    /** @return {@link forge.screens.match.views.VDev.DevLabel} */
    public DevLabel getLblCastSpell() {
        return this.lblCastSpell;
    }

    /** @return {@link forge.screens.match.views.VDev.DevLabel} */
    public DevLabel getLblExileFromPlay() {
        return this.lblExileFromPlay;
    }
    /** @return {@link forge.screens.match.views.VDev.DevLabel} */
    public DevLabel getLblRemoveFromGame() {
        return this.lblRemoveFromGame;
    }

    /** @return {@link forge.screens.match.views.VDev.DevLabel} */
    public DevLabel getLblAddCounterPermanent() {
        return this.lblAddCounterPermanent;
    }

    /** @return {@link forge.screens.match.views.VDev.DevLabel} */
    public DevLabel getLblSubCounterPermanent() {
        return this.lblSubCounterPermanent;
    }

    /** @return {@link forge.screens.match.views.VDev.DevLabel} */
    public DevLabel getLblTapPermanent() {
        return this.lblTapPermanent;
    }

    /** @return {@link forge.screens.match.views.VDev.DevLabel} */
    public DevLabel getLblUntapPermanent() {
        return this.lblUntapPermanent;
    }

    /** @return {@link forge.screens.match.views.VDev.DevLabel} */
    public DevLabel getLblUnlimitedLands() {
        return this.lblUnlimitedLands;
    }

    /** @return {@link forge.screens.match.views.VDev.DevLabel} */
    public DevLabel getLblViewAll() {
        return this.lblViewAll;
    }

    /** @return {@link forge.screens.match.views.VDev.DevLabel} */
    public DevLabel getLblSetLife() {
        return this.lblSetLife;
    }

    /** @return {@link forge.screens.match.views.VDev.DevLabel} */
    public DevLabel getLblWinGame() {
        return this.lblWinGame;
    }

    public DevLabel getLblRiggedRoll() {
        return this.lblRiggedRoll;
    }

    public DevLabel getLblWalkTo() {
        return this.lblWalkTo;
    }

    public DevLabel getLblAskAI() {
        return this.lblAskAI;
    }

    public DevLabel getLblAskSimulationAI() {
        return this.lblAskSimulationAI;
    }

    /**
     * Labels that act as buttons which control dev mode functions. Labels are
     * used to support multiline text.
     */
    public static class DevLabel extends SkinnedLabel {
        private static final long serialVersionUID = 7917311680519060700L;

        private FSkin.SkinColor defaultBG;
        private final FSkin.SkinColor hoverBG = FSkin.getColor(FSkin.Colors.CLR_HOVER);
        private final FSkin.SkinColor pressedBG = FSkin.getColor(FSkin.Colors.CLR_INACTIVE);
        private boolean toggled;

        private final int r, i; // Radius, insets (for paintComponent)

        public DevLabel(final String text0) {
            super();
            this.setText(text0);
            this.setToolTipText(text0);
            this.setUI(MultiLineLabelUI.getLabelUI());
            this.setBorder(new EmptyBorder(5, 5, 5, 5));
            this.r = 6; // Radius (for paintComponent)
            this.i = 2; // Insets (for paintComponent)
            this.setToggled(false);
            this.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));

            this.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(final MouseEvent e) {
                    DevLabel.this.setBackground(DevLabel.this.pressedBG);
                }

                @Override
                public void mouseReleased(final MouseEvent e) {
                    DevLabel.this.setBackground(DevLabel.this.defaultBG);
                }

                @Override
                public void mouseEntered(final MouseEvent e) {
                    DevLabel.this.setBackground(DevLabel.this.hoverBG);
                }

                @Override
                public void mouseExited(final MouseEvent e) {
                    DevLabel.this.setBackground(DevLabel.this.defaultBG);
                }
            });
        }

        public boolean getToggled() {
            return toggled;
        }

        /**
         * Changes enabled state per boolean parameter, automatically updating
         * text string and background color.
         * 
         * @param b
         *            &emsp; boolean
         */
        public void setToggled(final boolean b) {
            if (b) {
                this.defaultBG = FSkin.getColor(FSkin.Colors.CLR_INACTIVE);
            }
            else {
                this.defaultBG = FSkin.getColor(FSkin.Colors.CLR_ACTIVE);
            }
            this.toggled = b;
            this.setBackground(this.defaultBG);
        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.swing.JComponent#paintComponent(java.awt.Graphics)
         */
        @Override
        protected void paintComponent(final Graphics g) {
            int w = this.getWidth();
            int h = this.getHeight();
            g.setColor(this.getBackground());
            g.fillRoundRect(this.i, this.i, w - (2 * this.i), h - this.i, this.r, this.r);
            super.paintComponent(g);
        }
    }
}
