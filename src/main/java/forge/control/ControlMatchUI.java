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
package forge.control;

import java.awt.Image;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;

import forge.AllZone;
import forge.Card;
import forge.CardContainer;
import forge.CardList;
import forge.Constant;
import forge.Constant.Zone;
import forge.GuiMultipleBlockers;
import forge.Player;
import forge.Singletons;
import forge.control.match.ControlDetail;
import forge.control.match.ControlDock;
import forge.control.match.ControlField;
import forge.control.match.ControlHand;
import forge.control.match.ControlMessage;
import forge.control.match.ControlPicture;
import forge.control.match.ControlTabber;
import forge.game.GameType;
import forge.properties.ForgePreferences.FPref;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;
import forge.view.ViewMatchUI;
import forge.view.match.ViewField;
import forge.view.toolbox.FLabel;
import forge.view.toolbox.FSkin;

/**
 * <p>
 * ControlMatchUI
 * </p>
 * Top-level controller for matches.
 * 
 */
public class ControlMatchUI implements CardContainer {
    private final ViewMatchUI view;

    /**
     * <p>
     * ControlMatchUI
     * </p>
     * Constructs instance of match UI controller, used as a single point of
     * top-level control for child UIs - in other words, this class controls the
     * controllers. Tasks targeting the view of individual components are found
     * in a separate controller for that component and should not be included
     * here.
     * 
     * This constructor is called after child components have been instantiated.
     * When children are instantiated, they also instantiate their controller.
     * So, this class must be called after everything is already in place.
     * 
     * @param v
     *            &emsp; A ViewMatchUI object
     */
    public ControlMatchUI(final ViewMatchUI v) {
        this.view = v;
    }

    /**
     * Fires up controllers for each component of UI.
     * 
     */
    public void initMatch() {
        ControlMatchUI.this.showCombat("");
        ControlMatchUI.this.showStack();

        // Update avatars
        final String[] indices = Singletons.getModel().getPreferences().getPref(FPref.UI_AVATARS).split(",");
        final Object[] views = Singletons.getView().getViewMatch().getFieldViews().toArray();
        for (int i = 0; i < views.length; i++) {
            final Image img;
            // Update AI quest icon
            if (i > 0 && Constant.Runtime.getGameType() == GameType.Quest) {
                    String filename = ForgeProps.getFile(NewConstants.IMAGE_ICON) + File.separator;

                    if (Constant.Quest.OPP_ICON_NAME[0] != null) {
                        filename += Constant.Quest.OPP_ICON_NAME[0];
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

            ((ViewField) views[i]).getLblAvatar().setIcon(new ImageIcon(img));
            ((FLabel) ((ViewField) views[i]).getLblAvatar()).getResizeTimer().start();
        }

        // Update observers
        AllZone.getHumanPlayer().updateObservers();
        AllZone.getHumanPlayer().getZone(Zone.Hand).updateObservers();
        AllZone.getHumanPlayer().getZone(Zone.Battlefield).updateObservers();

        AllZone.getComputerPlayer().updateObservers();
        AllZone.getComputerPlayer().getZone(Zone.Hand).updateObservers();
        AllZone.getHumanPlayer().getZone(Zone.Battlefield).updateObservers();

        AllZone.getStack().updateObservers();
        AllZone.getInputControl().updateObservers();
        ControlMatchUI.this.getTabberControl().updateObservers();
    }

    /**
     * Resets all phase buttons in all fields to "inactive", so highlight won't
     * be drawn on them. "Enabled" state remains the same.
     */
    // This method is in the top-level controller because it affects ALL fields
    // (not just one).
    public void resetAllPhaseButtons() {
        for (final ControlField c : ControlMatchUI.this.getFieldControls()) {
            c.resetPhaseButtons();
        }
    }

    /** @param s0 &emsp; {@link java.lang.String} */
    public void showMessage(final String s0) {
        getMessageControl().setMessage(s0);
    }

    /** */
    public void showStack() {
        ControlMatchUI.this.getTabberControl().showPnlStack();
    }

    /** @param s0 &emsp; {@link java.lang.String} */
    public void showCombat(final String s0) {
        ControlMatchUI.this.getTabberControl().getView().updateCombat(s0);
    }

    /**
     * Gets the detail controller.
     * 
     * @return ControlDetail
     */
    public ControlDetail getDetailControl() {
        return view.getViewDetail().getControl();
    }

    /**
     * Gets the picture controller.
     * 
     * @return ControlPicture
     */
    public ControlPicture getPictureControl() {
        return view.getViewPicture().getControl();
    }

    /**
     * Gets the tabber controller.
     * 
     * @return ControlTabber
     */
    public ControlTabber getTabberControl() {
        return view.getViewTabber().getControl();
    }

    /**
     * Gets the input controller.
     * 
     * @return ControlMessage
     */
    public ControlMessage getMessageControl() {
        return view.getViewMessage().getControl();
    }

    /**
     * Gets the hand controller.
     * 
     * @return ControlHand
     */
    public ControlHand getHandControl() {
        return view.getViewHand().getControl();
    }

    /**
     * Gets the dock controller.
     * 
     * @return ControlDock
     */
    public ControlDock getDockControl() {
        return view.getDockView().getControl();
    }

    /**
     * Gets the field controllers.
     * 
     * @return List<ControlField>
     */
    public List<ControlField> getFieldControls() {
        final List<ViewField> fields = view.getViewBattlefield().getFields();
        final List<ControlField> controllers = new ArrayList<ControlField>();

        for (final ViewField f : fields) {
            controllers.add(f.getControl());
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
     * @param turn &emsp; {@link forge.Player}
     * @param phase &emsp; {@link java.lang.String}
     * @return boolean
     */
    public final boolean stopAtPhase(final Player turn, final String phase) {
        final List<ControlField> fieldControllers = ControlMatchUI.this.getFieldControls();

        // AI field is at index [0]
        if (turn.isComputer()) {
            if (phase.equals(Constant.Phase.UPKEEP)) {
                return fieldControllers.get(0).getView().getLblUpkeep().getEnabled();
            } else if (phase.equals(Constant.Phase.DRAW)) {
                return fieldControllers.get(0).getView().getLblDraw().getEnabled();
            } else if (phase.equals(Constant.Phase.MAIN1)) {
                return fieldControllers.get(0).getView().getLblMain1().getEnabled();
            } else if (phase.equals(Constant.Phase.COMBAT_BEGIN)) {
                return fieldControllers.get(0).getView().getLblBeginCombat().getEnabled();
            } else if (phase.equals(Constant.Phase.COMBAT_DECLARE_ATTACKERS)) {
                return fieldControllers.get(0).getView().getLblDeclareAttackers().getEnabled();
            } else if (phase.equals(Constant.Phase.COMBAT_DECLARE_BLOCKERS)) {
                return fieldControllers.get(0).getView().getLblDeclareBlockers().getEnabled();
            } else if (phase.equals(Constant.Phase.COMBAT_FIRST_STRIKE_DAMAGE)) {
                return fieldControllers.get(0).getView().getLblFirstStrike().getEnabled();
            } else if (phase.equals(Constant.Phase.COMBAT_DAMAGE)) {
                return fieldControllers.get(0).getView().getLblCombatDamage().getEnabled();
            } else if (phase.equals(Constant.Phase.COMBAT_END)) {
                return fieldControllers.get(0).getView().getLblEndCombat().getEnabled();
            } else if (phase.equals(Constant.Phase.MAIN2)) {
                return fieldControllers.get(0).getView().getLblMain2().getEnabled();
            } else if (phase.equals(Constant.Phase.END_OF_TURN)) {
                return fieldControllers.get(0).getView().getLblEndTurn().getEnabled();
            } else if (phase.equals(Constant.Phase.DRAW)) {
                return fieldControllers.get(0).getView().getLblDraw().getEnabled();
            }
        }
        // Human field is at index [1]
        else {
            if (phase.equals(Constant.Phase.UPKEEP)) {
                return fieldControllers.get(1).getView().getLblUpkeep().getEnabled();
            } else if (phase.equals(Constant.Phase.DRAW)) {
                return fieldControllers.get(1).getView().getLblDraw().getEnabled();
            } else if (phase.equals(Constant.Phase.MAIN1)) {
                return fieldControllers.get(1).getView().getLblMain1().getEnabled();
            } else if (phase.equals(Constant.Phase.COMBAT_BEGIN)) {
                return fieldControllers.get(1).getView().getLblBeginCombat().getEnabled();
            } else if (phase.equals(Constant.Phase.COMBAT_DECLARE_ATTACKERS)) {
                return fieldControllers.get(1).getView().getLblDeclareAttackers().getEnabled();
            } else if (phase.equals(Constant.Phase.COMBAT_DECLARE_BLOCKERS)) {
                return fieldControllers.get(1).getView().getLblDeclareBlockers().getEnabled();
            } else if (phase.equals(Constant.Phase.COMBAT_FIRST_STRIKE_DAMAGE)) {
                return fieldControllers.get(1).getView().getLblFirstStrike().getEnabled();
            } else if (phase.equals(Constant.Phase.COMBAT_DAMAGE)) {
                return fieldControllers.get(1).getView().getLblCombatDamage().getEnabled();
            } else if (phase.equals(Constant.Phase.COMBAT_END)) {
                return fieldControllers.get(1).getView().getLblEndCombat().getEnabled();
            } else if (phase.equals(Constant.Phase.MAIN2)) {
                return fieldControllers.get(1).getView().getLblMain2().getEnabled();
            } else if (phase.equals(Constant.Phase.END_OF_TURN)) {
                return fieldControllers.get(1).getView().getLblEndTurn().getEnabled();
            } else if (phase.equals(Constant.Phase.DRAW)) {
                return fieldControllers.get(1).getView().getLblDraw().getEnabled();
            }
        }
        return true;
    }

    @Override
    public void setCard(final Card c) {
        Singletons.getControl().getControlMatch().getDetailControl().showCard(c);
        Singletons.getControl().getControlMatch().getPictureControl().showCard(c);
    }

    @Override
    public Card getCard() {
        return Singletons.getControl().getControlMatch().getDetailControl().getCurrentCard();
    }
}
