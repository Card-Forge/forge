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

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Toolkit;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.JOptionPane;

import forge.AllZone;
import forge.Card;
import forge.CardContainer;
import forge.CardList;
import forge.Constant;
import forge.Display;
import forge.GuiMultipleBlockers;
import forge.MyButton;
import forge.Player;
import forge.Singletons;
import forge.control.FControl;
import forge.control.match.ControlField;
import forge.properties.ForgePreferences;
import forge.view.match.ViewField;
import forge.view.match.ViewTabber;
import forge.view.toolbox.FOverlay;

/**
 * Parent JFrame for Forge UI.
 * 
 */
@SuppressWarnings("serial")
public class GuiTopLevel extends JFrame implements Display, CardContainer {
    private final JLayeredPane lpnContent;
    private final FControl control;

    /**
     * Parent JFrame for Forge UI.
     */
    public GuiTopLevel() {
        super();
        this.setMinimumSize(new Dimension(800, 600));
        this.setLocationRelativeTo(null);
        this.setExtendedState(this.getExtendedState() | Frame.MAXIMIZED_BOTH);

        this.lpnContent = new JLayeredPane();
        this.lpnContent.setOpaque(true);
        this.setContentPane(this.lpnContent);
        this.addOverlay();
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.setIconImage(Toolkit.getDefaultToolkit().getImage("res/images/symbols-13/favicon.png"));
        this.setTitle("Forge: " + Singletons.getModel().getBuildInfo().getVersion());

        // Init controller
        this.control = new FControl(this);
    }

    /**
     * Adds overlay panel to modal layer. Used when removeAll() has been called
     * on the JLayeredPane parent.
     */
    public void addOverlay() {
        final FOverlay pnlOverlay = new FOverlay();
        AllZone.setOverlay(pnlOverlay);
        pnlOverlay.setOpaque(false);
        pnlOverlay.setVisible(false);
        pnlOverlay.setBounds(0, 0, this.getWidth(), this.getHeight());
        this.lpnContent.add(pnlOverlay, JLayeredPane.MODAL_LAYER);
    }

    /**
     * Gets the controller.
     * 
     * @return FControl
     */
    public FControl getController() {
        return this.control;
    }

    /*
     * ========================================================
     * 
     * WILL BE DEPRECATED SOON WITH DISPLAY INTERFACE UPDATE!!!
     * 
     * ========================================================
     */

    /*
     * (non-Javadoc)
     * 
     * @see forge.Display#showMessage(java.lang.String)
     */
    @Override
    public void showMessage(final String s) {
        control.getMatchController().getView().getPnlMessage().setText(s);
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.Display#getButtonOK()
     */
    @Override
    public MyButton getButtonOK() {
        final MyButton ok = new MyButton() {
            @Override
            public void select() {
                control.getMatchController().getView().getInputController().getInputControl().selectButtonOK();
            }

            @Override
            public boolean isSelectable() {
                return control.getMatchController().getView().getInputController().getView().getBtnOK().isEnabled();
            }

            @Override
            public void setSelectable(final boolean b) {
                control.getMatchController().getView().getInputController().getView().getBtnOK().setEnabled(b);
            }

            @Override
            public String getText() {
                return control.getMatchController().getView().getInputController().getView().getBtnOK().getText();
            }

            @Override
            public void setText(final String text) {
                control.getMatchController().getView().getInputController().getView().getBtnOK().setText(text);
            }

            @Override
            public void reset() {
                control.getMatchController().getView().getInputController().getView().getBtnOK().setText("OK");
            }
        };

        return ok;
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.Display#getButtonCancel()
     */
    @Override
    public MyButton getButtonCancel() {
        final MyButton cancel = new MyButton() {
            @Override
            public void select() {
                control.getMatchController().getView().getInputController().getInputControl().selectButtonCancel();
            }

            @Override
            public boolean isSelectable() {
                return control.getMatchController().getView().getInputController().getView().getBtnCancel().isEnabled();
            }

            @Override
            public void setSelectable(final boolean b) {
                control.getMatchController().getView().getInputController().getView().getBtnCancel().setEnabled(b);
            }

            @Override
            public String getText() {
                return control.getMatchController().getView().getInputController().getView().getBtnCancel().getText();
            }

            @Override
            public void setText(final String text) {
                control.getMatchController().getView().getInputController().getView().getBtnCancel().setText(text);
            }

            @Override
            public void reset() {
                control.getMatchController().getView().getInputController().getView().getBtnCancel().setText("Cancel");
            }
        };
        return cancel;
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.Display#showCombat(java.lang.String)
     */
    @Override
    public void showCombat(final String s0) {
        control.getMatchController().getView().getTabberController().getView().updateCombat(s0);
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.Display#assignDamage(forge.Card, forge.CardList, int)
     */
    @Override
    public void assignDamage(final Card attacker, final CardList blockers, final int damage) {
        if (damage <= 0) {
            return;
        }

        new GuiMultipleBlockers(attacker, blockers, damage, this);
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.Display#stopAtPhase(forge.Player, java.lang.String)
     */
    @Override
    public final boolean stopAtPhase(final Player turn, final String phase) {
        final List<ControlField> fieldControllers = control.getMatchView().getFieldControllers();

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
        final List<ViewField> fieldViews = this.control.getMatchController().getView().getFieldViews();

        Constant.Runtime.MILL[0] = fp.isMillingLossCondition();
        Constant.Runtime.DEV_MODE[0] = fp.isDeveloperMode();
        Constant.Runtime.UPLOAD_DRAFT[0] = fp.isUploadDraftAI();
        Constant.Runtime.RANDOM_FOIL[0] = fp.isRandCFoil();
        Constant.Runtime.UPLOAD_DRAFT[0] = (Constant.Runtime.NET_CONN[0] ? fp.isUploadDraftAI() : false);

        // AI field is at index [0]
        fieldViews.get(0).getLblUpkeep().setEnabled(fp.isShowPhase("phase.ai.upkeep"));
        fieldViews.get(0).getLblDraw().setEnabled(fp.isShowPhase("phase.ai.draw"));
        fieldViews.get(0).getLblMain1().setEnabled(fp.isShowPhase("phase.ai.main1"));
        fieldViews.get(0).getLblBeginCombat().setEnabled(fp.isShowPhase("phase.ai.beginCombat"));
        fieldViews.get(0).getLblDeclareAttackers().setEnabled(fp.isShowPhase("phase.ai.declareAttackers"));
        fieldViews.get(0).getLblDeclareBlockers().setEnabled(fp.isShowPhase("phase.ai.declareBlockers"));
        fieldViews.get(0).getLblFirstStrike().setEnabled(fp.isShowPhase("phase.ai.firstStrike"));
        fieldViews.get(0).getLblCombatDamage().setEnabled(fp.isShowPhase("phase.ai.combatDamage"));
        fieldViews.get(0).getLblEndCombat().setEnabled(fp.isShowPhase("phase.ai.endCombat"));
        fieldViews.get(0).getLblMain2().setEnabled(fp.isShowPhase("phase.ai.main2"));
        fieldViews.get(0).getLblEndTurn().setEnabled(fp.isShowPhase("phase.ai.eot"));
        fieldViews.get(0).getLblCleanup().setEnabled(fp.isShowPhase("phase.ai.cleanup"));

        // Human field is at index [1]
        fieldViews.get(1).getLblUpkeep().setEnabled(fp.isShowPhase("phase.human.upkeep"));
        fieldViews.get(1).getLblDraw().setEnabled(fp.isShowPhase("phase.human.draw"));
        fieldViews.get(1).getLblMain1().setEnabled(fp.isShowPhase("phase.human.main1"));
        fieldViews.get(1).getLblBeginCombat().setEnabled(fp.isShowPhase("phase.human.beginCombat"));
        fieldViews.get(1).getLblDeclareAttackers().setEnabled(fp.isShowPhase("phase.human.declareAttackers"));
        fieldViews.get(1).getLblDeclareBlockers().setEnabled(fp.isShowPhase("phase.human.declareBlockers"));
        fieldViews.get(1).getLblFirstStrike().setEnabled(fp.isShowPhase("phase.human.firstStrike"));
        fieldViews.get(1).getLblCombatDamage().setEnabled(fp.isShowPhase("phase.human.combatDamage"));
        fieldViews.get(1).getLblEndCombat().setEnabled(fp.isShowPhase("phase.human.endCombat"));
        fieldViews.get(1).getLblMain2().setEnabled(fp.isShowPhase("phase.human.main2"));
        fieldViews.get(1).getLblEndTurn().setEnabled(fp.isShowPhase("phase.human.eot"));
        fieldViews.get(1).getLblCleanup().setEnabled(fp.isShowPhase("phase.human.cleanup"));

        this.control.getMatchController().getView().setLayoutParams(fp.getUILayout());
        return true;
    }

    /**
     * <p>
     * savePrefs.
     * </p>
     * Required by Display interface. Due to be deprecated: should be in FModel.
     * Also, this functionality is already performed elsewhere
     * in the code base.
     * 
     * @return a boolean.
     */
    @Override
    public final boolean savePrefs() {
        final ForgePreferences fp = Singletons.getModel().getPreferences();
        final List<ViewField> fieldViews = this.control.getMatchController().getView().getFieldViews();

        // AI field is at index [0]
        fp.setShowPhase("phase.ai.upkeep", fieldViews.get(0).getLblUpkeep().getEnabled());
        fp.setShowPhase("phase.ai.draw", fieldViews.get(0).getLblDraw().getEnabled());
        fp.setShowPhase("phase.ai.main1", fieldViews.get(0).getLblMain1().getEnabled());
        fp.setShowPhase("phase.ai.beginCombat", fieldViews.get(0).getLblBeginCombat().getEnabled());
        fp.setShowPhase("phase.ai.declareAttackers", fieldViews.get(0).getLblDeclareAttackers().getEnabled());
        fp.setShowPhase("phase.ai.declareBlockers", fieldViews.get(0).getLblDeclareBlockers().getEnabled());
        fp.setShowPhase("phase.ai.firstStrike", fieldViews.get(0).getLblFirstStrike().getEnabled());
        fp.setShowPhase("phase.ai.combatDamage", fieldViews.get(0).getLblCombatDamage().getEnabled());
        fp.setShowPhase("phase.ai.endCombat", fieldViews.get(0).getLblEndCombat().getEnabled());
        fp.setShowPhase("phase.ai.main2", fieldViews.get(0).getLblMain2().getEnabled());
        fp.setShowPhase("phase.ai.eot", fieldViews.get(0).getLblEndTurn().getEnabled());
        fp.setShowPhase("phase.ai.cleanup", fieldViews.get(0).getLblCleanup().getEnabled());

        // Human field is at index [1]
        fp.setShowPhase("phase.human.upkeep", fieldViews.get(1).getLblUpkeep().getEnabled());
        fp.setShowPhase("phase.human.draw", fieldViews.get(1).getLblDraw().getEnabled());
        fp.setShowPhase("phase.human.main1", fieldViews.get(1).getLblMain1().getEnabled());
        fp.setShowPhase("phase.human.beginCombat", fieldViews.get(1).getLblBeginCombat().getEnabled());
        fp.setShowPhase("phase.human.declareAttackers", fieldViews.get(1).getLblDeclareAttackers().getEnabled());
        fp.setShowPhase("phase.human.declareBlockers", fieldViews.get(1).getLblDeclareBlockers().getEnabled());
        fp.setShowPhase("phase.human.firstStrike", fieldViews.get(1).getLblFirstStrike().getEnabled());
        fp.setShowPhase("phase.human.combatDamage", fieldViews.get(1).getLblCombatDamage().getEnabled());
        fp.setShowPhase("phase.human.endCombat", fieldViews.get(1).getLblEndCombat().getEnabled());
        fp.setShowPhase("phase.human.main2", fieldViews.get(1).getLblMain2().getEnabled());
        fp.setShowPhase("phase.human.eot", fieldViews.get(1).getLblEndTurn().getEnabled());
        fp.setShowPhase("phase.human.cleanup", fieldViews.get(1).getLblCleanup().getEnabled());

        ViewTabber v = this.control.getMatchController().getView().getTabberController().getView();
        Constant.Runtime.MILL[0] = v.getLblMilling().getEnabled();

        fp.setMillingLossCondition(Constant.Runtime.MILL[0]);
        fp.setUILayout(control.getMatchController().getView().getLayoutParams());
        fp.setUnlimitedLand(v.getLblUnlimitedLands().getEnabled());

        try {
            fp.save();
        } catch (final Exception ex) {
            final int result = JOptionPane.showConfirmDialog(this,
                    "Preferences could not be saved. Continue to close without saving ?", "Confirm Exit",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
            if (result != JOptionPane.OK_OPTION) {
                return false;
            }
        }
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.Display#setCard(forge.Card)
     */
    @Override
    public void setCard(final Card c) {
        this.control.getMatchView().getDetailController().showCard(c);
        this.control.getMatchView().getPictureController().showCard(c);
    }

    /**
     * Required by display interface. Due to be deprecated: handled by control
     * class.
     * 
     * @return a {@link forge.Card} object.
     */
    public final Card getCard() {
        return this.control.getMatchView().getDetailController().getCurrentCard();
    }
}
