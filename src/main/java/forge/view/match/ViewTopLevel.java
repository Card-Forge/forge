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
package forge.view.match;

import java.awt.Graphics;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JTextArea;

import forge.AllZone;
import forge.Card;
import forge.CardList;
import forge.Constant;
import forge.Display;
import forge.MyButton;
import forge.Player;
import forge.Singletons;
import forge.control.ControlMatchUI;
import forge.control.match.ControlCardviewer;
import forge.control.match.ControlDock;
import forge.control.match.ControlField;
import forge.control.match.ControlHand;
import forge.control.match.ControlInput;
import forge.control.match.ControlTabber;
import forge.properties.ForgePreferences;
import forge.view.toolbox.FPanel;

/**
 * - Lays out battle, sidebar, user areas in locked % vals and repaints as
 * necessary.<br>
 * - Instantiates top-level controller for match UI.<br>
 * - Has access methods for all child controllers<br>
 * - Implements Display interface used in singleton pattern
 * 
 */

@SuppressWarnings("serial")
public class ViewTopLevel extends FPanel implements Display {
    private final ViewAreaSidebar areaSidebar;
    private final ViewAreaBattlefield areaBattle;
    private final ViewAreaUser areaUser;

    private int w, h;
    private static final double SIDEBAR_W_PCT = 0.16;
    private static final double USER_H_PCT = 0.27;
    private final ControlMatchUI control;

    /**
     * - Lays out battle, sidebar, user areas in locked % vals and repaints as
     * necessary.<br>
     * - Instantiates top-level controller for match UI.<br>
     * - Has access methods for all child controllers<br>
     * - Implements Display interface used in singleton pattern
     * 
     */
    public ViewTopLevel() {
        super();

        // Set properties
        this.setOpaque(false);
        this.setBGTexture(AllZone.getSkin().getTexture1());
        this.setBGImg(AllZone.getSkin().getMatchBG());
        this.setLayout(null);

        // areaBattle: holds fields for all players in match.
        this.areaBattle = new ViewAreaBattlefield();
        this.areaBattle.setBounds(0, 0, this.getWidth() / 2, this.getHeight() / 2);
        this.add(this.areaBattle);

        // areaSidebar: holds card detail, info tabber.
        this.areaSidebar = new ViewAreaSidebar();
        this.areaSidebar.setBounds(0, 0, this.getWidth() / 2, this.getHeight() / 2);
        this.add(this.areaSidebar);

        // areaUser: holds input, hand, dock.
        this.areaUser = new ViewAreaUser();
        this.areaUser.setBounds(0, 0, this.getWidth() / 2, this.getHeight() / 2);
        this.add(this.areaUser);

        // After all components are in place, instantiate controller.
        this.control = new ControlMatchUI(this);
    }

    /**
     * The null layout used in MatchFrame has zones split into percentage values
     * to prevent child components pushing around the parent layout. A single
     * instance of BodyPanel holds these zones, and handles the percentage
     * resizing.
     * 
     * @param g
     *            &emsp; Graphics object
     */
    @Override
    protected void paintComponent(final Graphics g) {
        super.paintComponent(g);
        this.w = this.getWidth();
        this.h = this.getHeight();

        // Set % boundaries of layout control layer
        this.areaBattle.setBounds(0, 0, (int) (this.w * (1 - ViewTopLevel.SIDEBAR_W_PCT)),
                (int) (this.h * (1 - ViewTopLevel.USER_H_PCT)));
        this.areaSidebar.setBounds((int) (this.w * (1 - ViewTopLevel.SIDEBAR_W_PCT)), 0,
                (int) (this.w * ViewTopLevel.SIDEBAR_W_PCT), this.h);
        this.areaUser.setBounds(0, (int) (this.h * (1 - ViewTopLevel.USER_H_PCT)),
                (int) (this.w * (1 - ViewTopLevel.SIDEBAR_W_PCT)), (int) (this.h * ViewTopLevel.USER_H_PCT));
        this.areaBattle.validate();
    }

    // ========== Retrieval functions for easier interation with children
    // panels.
    /**
     * Gets the area sidebar.
     * 
     * @return ViewAreaSidebar
     */
    public ViewAreaSidebar getAreaSidebar() {
        return this.areaSidebar;
    }

    /**
     * Gets the area battlefield.
     * 
     * @return ViewAreaBattlefield
     */
    public ViewAreaBattlefield getAreaBattlefield() {
        return this.areaBattle;
    }

    /**
     * Gets the area user.
     * 
     * @return ViewAreaUser
     */
    public ViewAreaUser getAreaUser() {
        return this.areaUser;
    }

    /**
     * Retrieves top level controller (actions, observers, etc.) for this UI.
     * 
     * @return {@link java.util.List<MatchPlayer>}
     */
    public ControlMatchUI getController() {
        return this.control;
    }

    /**
     * Gets the cardviewer controller.
     * 
     * @return ControlCardviewer
     */
    public ControlCardviewer getCardviewerController() {
        return this.areaSidebar.getCardviewer().getController();
    }

    /**
     * Gets the tabber controller.
     * 
     * @return ControlTabber
     */
    public ControlTabber getTabberController() {
        return this.areaSidebar.getTabber().getController();
    }

    /**
     * Gets the input controller.
     * 
     * @return ControlInput
     */
    public ControlInput getInputController() {
        return this.areaUser.getPnlInput().getController();
    }

    /**
     * Gets the hand controller.
     * 
     * @return ControlHand
     */
    public ControlHand getHandController() {
        return this.areaUser.getPnlHand().getController();
    }

    /**
     * Gets the dock controller.
     * 
     * @return ControlDock
     */
    public ControlDock getDockController() {
        return this.areaUser.getPnlDock().getController();
    }

    /**
     * Gets the field controllers.
     * 
     * @return List<ControlField>
     */
    public List<ControlField> getFieldControllers() {
        final List<ViewField> fields = this.areaBattle.getFields();
        final List<ControlField> controllers = new ArrayList<ControlField>();

        for (final ViewField f : fields) {
            controllers.add(f.getController());
        }

        return controllers;
    }

    /**
     * Gets the field views.
     * 
     * @return List<ViewField>
     */
    public List<ViewField> getFieldViews() {
        return this.areaBattle.getFields();
    }

    // ========== Input panel and human hand retrieval functions
    // Also due to be deprecated. Access should be handled by child component
    // view and/or controller.

    /**
     * Gets the pnl message.
     * 
     * @return <b>JTextArea</b> Message area of input panel.
     */
    public JTextArea getPnlMessage() {
        return this.areaUser.getPnlInput().getTarMessage();
    }

    /**
     * Gets the pnl hand.
     * 
     * @return <b>ViewHand</b> Retrieves player hand panel.
     */
    public ViewHand getPnlHand() {
        return this.areaUser.getPnlHand();
    }

    // ========== The following methods are required by the Display interface.
    // To fit the UI MVC architecture with the previous "mixed nuts"
    // architecture,
    // these methods are temporarily required. However, since they are a mix of
    // view and control functionalities, they are ALL on the "to-be-deprecated"
    // list.
    // The Display interface is to be reworked, eventually, with a better name
    // and with interfaces for every screen in the entire UI.
    // Doublestrike 23-10-11

    /**
     * Required by Display interface. Due to be deprecated in favor of more
     * semantic getBtnCancel().
     * 
     * @return MyButton
     */
    @Override
    public MyButton getButtonCancel() {
        final MyButton cancel = new MyButton() {
            @Override
            public void select() {
                ViewTopLevel.this.getInputController().getInputControl().selectButtonCancel();
            }

            @Override
            public boolean isSelectable() {
                return ViewTopLevel.this.areaUser.getPnlInput().getBtnCancel().isEnabled();
            }

            @Override
            public void setSelectable(final boolean b) {
                ViewTopLevel.this.areaUser.getPnlInput().getBtnCancel().setEnabled(b);
            }

            @Override
            public String getText() {
                return ViewTopLevel.this.areaUser.getPnlInput().getBtnCancel().getText();
            }

            @Override
            public void setText(final String text) {
                ViewTopLevel.this.areaUser.getPnlInput().getBtnCancel().setText(text);
            }

            @Override
            public void reset() {
                ViewTopLevel.this.areaUser.getPnlInput().getBtnCancel().setText("Cancel");
            }
        };
        return cancel;
    }

    /**
     * Required by Display interface. Due to be deprecated in favor of more
     * semantic getBtnOK().
     * 
     * @return MyButton
     */
    @Override
    public MyButton getButtonOK() {
        final MyButton ok = new MyButton() {
            @Override
            public void select() {
                ViewTopLevel.this.getInputController().getInputControl().selectButtonOK();
            }

            @Override
            public boolean isSelectable() {
                return ViewTopLevel.this.areaUser.getPnlInput().getBtnOK().isEnabled();
            }

            @Override
            public void setSelectable(final boolean b) {
                ViewTopLevel.this.areaUser.getPnlInput().getBtnOK().setEnabled(b);
            }

            @Override
            public String getText() {
                return ViewTopLevel.this.areaUser.getPnlInput().getBtnOK().getText();
            }

            @Override
            public void setText(final String text) {
                ViewTopLevel.this.areaUser.getPnlInput().getBtnOK().setText(text);
            }

            @Override
            public void reset() {
                ViewTopLevel.this.areaUser.getPnlInput().getBtnOK().setText("OK");
            }
        };

        return ok;
    }

    /**
     * Required by Display interface. Due to be deprecated: is now and should be
     * handled by ControlMatchUI.
     * 
     * @param s
     *            &emsp; Message string
     */
    @Override
    public void showMessage(final String s) {
        this.getPnlMessage().setText(s);
    }

    /**
     * Required by Display interface. Due to be deprecated: should be handled by
     * ControlMatchUI.
     * 
     * @param s
     *            &emsp; Message string
     */
    @Override
    public void showCombat(final String s) {
        this.getTabberController().getView().updateCombat(s);
    }

    /**
     * Required by Display interface. Due to be deprecated: should be handled by
     * a control class, and poorly named; "decking" == "milling" in preferences,
     * same terminology should be used throughout project for obvious reasons.
     * Unless "decking" is already the correct terminology, in which case,
     * everything else is poorly named.
     * 
     * @return boolean
     */
    @Override
    public boolean canLoseByDecking() {
        return Constant.Runtime.MILL[0];
    }

    /**
     * <p>
     * loadPrefs.
     * </p>
     * Required by Display interface. Due to be deprecated: will be handled by
     * ControlMatchUI.
     * 
     * 
     * @return boolean.
     */
    @Override
    public final boolean loadPrefs() {
        final ForgePreferences fp = Singletons.getModel().getPreferences();
        final List<ViewField> fieldViews = this.getFieldViews();

        // AI field is at index [0]
        fieldViews.get(0).getLblUpkeep().setEnabled(fp.isbAIUpkeep());
        fieldViews.get(0).getLblDraw().setEnabled(fp.isbAIDraw());
        fieldViews.get(0).getLblEndTurn().setEnabled(fp.isbAIEOT());
        fieldViews.get(0).getLblBeginCombat().setEnabled(fp.isbAIBeginCombat());
        fieldViews.get(0).getLblEndCombat().setEnabled(fp.isbAIEndCombat());

        // Human field is at index [1]
        fieldViews.get(1).getLblUpkeep().setEnabled(fp.isbHumanUpkeep());
        fieldViews.get(1).getLblDraw().setEnabled(fp.isbHumanDraw());
        fieldViews.get(1).getLblEndTurn().setEnabled(fp.isbHumanEOT());
        fieldViews.get(1).getLblBeginCombat().setEnabled(fp.isbHumanBeginCombat());
        fieldViews.get(1).getLblEndCombat().setEnabled(fp.isbHumanEndCombat());

        return true;
    }

    /**
     * <p>
     * savePrefs.
     * </p>
     * Required by Display interface. Due to be deprecated: will be handled by
     * ControlMatchUI. Also, this functionality is already performed elsewhere
     * in the code base. Furthermore, there's a strong possibility this will
     * need bo be broken down and can't be in one place - e.g. keyboard
     * shortcuts are saved after they're edited.
     * 
     * @return a boolean.
     */
    @Override
    public final boolean savePrefs() {
        final ForgePreferences fp = Singletons.getModel().getPreferences();
        final List<ViewField> fieldViews = this.getFieldViews();

        // AI field is at index [0]
        fp.setbAIUpkeep(fieldViews.get(0).getLblUpkeep().getEnabled());
        fp.setbAIDraw(fieldViews.get(0).getLblDraw().getEnabled());
        fp.setbAIEOT(fieldViews.get(0).getLblEndTurn().getEnabled());
        fp.setbAIBeginCombat(fieldViews.get(0).getLblBeginCombat().getEnabled());
        fp.setbAIEndCombat(fieldViews.get(0).getLblEndCombat().getEnabled());

        // Human field is at index [1]
        fp.setbHumanUpkeep(fieldViews.get(1).getLblUpkeep().getEnabled());
        fp.setbHumanDraw(fieldViews.get(1).getLblDraw().getEnabled());
        fp.setbHumanEOT(fieldViews.get(1).getLblEndTurn().getEnabled());
        fp.setbHumanBeginCombat(fieldViews.get(1).getLblBeginCombat().getEnabled());
        fp.setbHumanEndCombat(fieldViews.get(1).getLblEndCombat().getEnabled());

        Constant.Runtime.MILL[0] = this.getTabberController().getView().getLblMilling().getEnabled();
        Constant.Runtime.HANDVIEW[0] = this.getTabberController().getView().getLblHandView().getEnabled();
        Constant.Runtime.LIBRARYVIEW[0] = this.getTabberController().getView().getLblLibraryView().getEnabled();

        fp.setMillingLossCondition(Constant.Runtime.MILL[0]);
        fp.setHandView(Constant.Runtime.HANDVIEW[0]);
        fp.setLibraryView(Constant.Runtime.LIBRARYVIEW[0]);
        return true;
    }

    /**
     * <p>
     * stopAtPhase.
     * </p>
     * Required by Display interface. Due to be deprecated: should be handled by
     * control class.
     * 
     * @param turn
     *            &emsp; Player object...more info needed
     * @param phase
     *            &emsp; A string...more info needed
     * @return a boolean.
     */
    @Override
    public final boolean stopAtPhase(final Player turn, final String phase) {
        final List<ControlField> fieldControllers = this.getFieldControllers();

        // AI field is at index [0]
        if (turn.isComputer()) {
            if (phase.equals(Constant.Phase.END_OF_TURN)) {
                return fieldControllers.get(0).getView().getLblEndTurn().getEnabled();
            } else if (phase.equals(Constant.Phase.UPKEEP)) {
                return fieldControllers.get(0).getView().getLblUpkeep().getEnabled();
            } else if (phase.equals(Constant.Phase.DRAW)) {
                return fieldControllers.get(0).getView().getLblDraw().getEnabled();
            } else if (phase.equals(Constant.Phase.COMBAT_BEGIN)) {
                return fieldControllers.get(0).getView().getLblBeginCombat().getEnabled();
            } else if (phase.equals(Constant.Phase.COMBAT_END)) {
                return fieldControllers.get(0).getView().getLblEndCombat().getEnabled();
            }
        }
        // Human field is at index [1]
        else {
            if (phase.equals(Constant.Phase.END_OF_TURN)) {
                return fieldControllers.get(1).getView().getLblEndTurn().getEnabled();
            } else if (phase.equals(Constant.Phase.UPKEEP)) {
                return fieldControllers.get(1).getView().getLblUpkeep().getEnabled();
            } else if (phase.equals(Constant.Phase.DRAW)) {
                return fieldControllers.get(1).getView().getLblDraw().getEnabled();
            } else if (phase.equals(Constant.Phase.COMBAT_BEGIN)) {
                return fieldControllers.get(1).getView().getLblBeginCombat().getEnabled();
            } else if (phase.equals(Constant.Phase.COMBAT_END)) {
                return fieldControllers.get(1).getView().getLblEndCombat().getEnabled();
            }
        }
        return true;
    }

    /**
     * Required by display interface. Due to be deprecated: handled by control
     * class.
     * 
     * @return a {@link forge.Card} object.
     */
    public final Card getCard() {
        System.err.println("ViewTopLevel > getCard: Something should happen here!");
        new Exception().printStackTrace();
        return null; // new Card(); //detail.getCard();
    }

    /**
     * Required by display interface. Due to be deprecated: already handled by
     * controller class.
     * 
     * @param card
     *            &emsp; a card
     */
    @Override
    public final void setCard(final Card card) {
        this.getCardviewerController().showCard(card);
    }

    /**
     * Required by Display interface. Assigns damage to multiple blockers. Due
     * to be deprecated: Gui_MultipleBlockers4 says "very hacky"; needs
     * rewriting.
     * 
     * @param attacker
     *            &emsp; Card object
     * @param blockers
     *            &emsp; Card objects in CardList form
     * @param damage
     *            &emsp; int
     */
    @Override
    public final void assignDamage(final Card attacker, final CardList blockers, final int damage) {
        if (damage <= 0) {
            return;
        }

        // new Gui_MultipleBlockers4(attacker, blockers, damage, this);
    }

    /** @return JFrame */
    public JFrame getTopLevelFrame() {
        return (JFrame) ViewTopLevel.this.getParent().getParent().getParent().getParent();
    }
}
