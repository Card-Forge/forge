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
package forge.gui.match;

import java.awt.Image;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;

import forge.AllZone;
import forge.Card;
import forge.CardList;
import forge.Constant;
import forge.Singletons;
import forge.game.GameType;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.gui.CardContainer;
import forge.gui.GuiMultipleBlockers;
import forge.gui.framework.EDocID;
import forge.gui.framework.SDisplayUtil;
import forge.gui.match.controllers.CDetail;
import forge.gui.match.controllers.CMessage;
import forge.gui.match.controllers.CPicture;
import forge.gui.match.nonsingleton.CField;
import forge.gui.match.nonsingleton.VField;
import forge.gui.toolbox.FLabel;
import forge.gui.toolbox.FSkin;
import forge.properties.ForgePreferences.FPref;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;

/**
 * Constructs instance of match UI controller, used as a single point of
 * top-level control for child UIs. Tasks targeting the view of individual
 * components are found in a separate controller for that component and
 * should not be included here.
 * 
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 */
public enum CMatchUI implements CardContainer {
    /** */
    SINGLETON_INSTANCE;

    /**
     * Fires up controllers for each component of UI.
     * 
     * @param strAvatarIcon &emsp; Filename of non-default avatar icon, if desired.
     * 
     */
    public void initMatch(final String strAvatarIcon) {
        SDisplayUtil.showTab(EDocID.REPORT_LOG.getDoc());

        // Update avatars
        final String[] indices = Singletons.getModel().getPreferences().getPref(FPref.UI_AVATARS).split(",");
        final Object[] views = VMatchUI.SINGLETON_INSTANCE.getFieldViews().toArray();
        for (int i = 0; i < views.length; i++) {
            final Image img;
            // Update AI quest icon
            if (i != 1 && Constant.Runtime.getGameType() == GameType.Quest) {
                    String filename = ForgeProps.getFile(NewConstants.IMAGE_ICON) + File.separator;

                    if (strAvatarIcon != null) {
                        filename += strAvatarIcon;
                        final File f = new File(filename);
                        img = (f.exists()
                                ? new ImageIcon(filename).getImage()
                                : FSkin.getAvatars().get(Integer.parseInt(indices[i])));
                    }
                    else {
                        img = FSkin.getAvatars().get(Integer.parseInt(indices[i]));
                    }
            }
            else {
                img = FSkin.getAvatars().get(Integer.parseInt(indices[i]));
            }

            ((VField) views[i]).getLblAvatar().setIcon(new ImageIcon(img));
            ((FLabel) ((VField) views[i]).getLblAvatar()).getResizeTimer().start();
        }

        // Update observers
        AllZone.getHumanPlayer().updateObservers();
        AllZone.getHumanPlayer().getZone(ZoneType.Hand).updateObservers();
        AllZone.getHumanPlayer().getZone(ZoneType.Battlefield).updateObservers();

        AllZone.getComputerPlayer().updateObservers();
        AllZone.getComputerPlayer().getZone(ZoneType.Hand).updateObservers();
        AllZone.getComputerPlayer().getZone(ZoneType.Battlefield).updateObservers();

        AllZone.getStack().updateObservers();
        AllZone.getInputControl().updateObservers();
        AllZone.getGameLog().updateObservers();
    }

    /**
     * Resets all phase buttons in all fields to "inactive", so highlight won't
     * be drawn on them. "Enabled" state remains the same.
     */
    // This method is in the top-level controller because it affects ALL fields
    // (not just one).
    public void resetAllPhaseButtons() {
        for (final CField c : CMatchUI.this.getFieldControls()) {
            c.resetPhaseButtons();
        }
    }

    /** @param s0 &emsp; {@link java.lang.String} */
    public void showMessage(final String s0) {
        CMessage.SINGLETON_INSTANCE.setMessage(s0);
    }

    /**
     * Gets the field controllers.
     * 
     * @return List<CField>
     */
    public List<CField> getFieldControls() {
        final List<VField> fields = VMatchUI.SINGLETON_INSTANCE.getFieldViews();
        final List<CField> controllers = new ArrayList<CField>();

        for (final VField f : fields) {
            controllers.add((CField) f.getControl());
        }

        return controllers;
    }

    /**
     * Fires up trample dialog.  Very old code, due for refactoring with new UI.
     * Could possibly move to view.
     * 
     * @param attacker &emsp; {@link forge.Card}
     * @param blockers &emsp; {@link forge.CardList}
     * @param damage &emsp; int
     */
    public void assignDamage(final Card attacker, final CardList blockers, final int damage) {
        if (damage <= 0) {
            return;
        }

        new GuiMultipleBlockers(attacker, blockers, damage);
    }

    /**
     * 
     * Checks if game control should stop at a phase, for either
     * a forced programmatic stop, or a user-induced phase toggle.
     * @param turn &emsp; {@link forge.game.player.Player}
     * @param phase &emsp; {@link java.lang.String}
     * @return boolean
     */
    public final boolean stopAtPhase(final Player turn, final PhaseType phase) {
        final List<CField> fieldControllers = CMatchUI.this.getFieldControls();

        // AI field is at index [0]
        int index = turn.isComputer() ? 0 : 1;
        VField vf = fieldControllers.get(index).getView();

        switch (phase) {
            case UPKEEP: return vf.getLblUpkeep().getEnabled();
            case DRAW: return vf.getLblDraw().getEnabled();
            case MAIN1: return vf.getLblMain1().getEnabled();
            case COMBAT_BEGIN: return vf.getLblBeginCombat().getEnabled();
            case COMBAT_DECLARE_ATTACKERS: return vf.getLblDeclareAttackers().getEnabled();
            case COMBAT_DECLARE_BLOCKERS: return vf.getLblDeclareBlockers().getEnabled();
            case COMBAT_FIRST_STRIKE_DAMAGE: return vf.getLblFirstStrike().getEnabled();
            case COMBAT_DAMAGE: return vf.getLblCombatDamage().getEnabled();
            case COMBAT_END: return vf.getLblEndCombat().getEnabled();
            case MAIN2: return vf.getLblMain2().getEnabled();
            case END_OF_TURN: return vf.getLblEndTurn().getEnabled();
            default:
        }

        return true;
    }

    @Override
    public void setCard(final Card c) {
        CDetail.SINGLETON_INSTANCE.showCard(c);
        CPicture.SINGLETON_INSTANCE.showCard(c);
    }

    @Override
    public Card getCard() {
        return CDetail.SINGLETON_INSTANCE.getCurrentCard();
    }
}
