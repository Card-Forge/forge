package forge.view.match;

import java.awt.Graphics;
import java.util.ArrayList;
import java.util.List;

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
import forge.gui.skin.FPanel;
import forge.properties.ForgePreferences;

/**
 * - Lays out battle, sidebar, user areas in locked % vals and repaints
 * as necessary.<br>
 * - Instantiates top-level controller for match UI.<br>
 * - Has access methods for all child controllers<br>
 * - Implements Display interface used in singleton pattern
 *
 */

@SuppressWarnings("serial")
public class ViewTopLevel extends FPanel implements Display {
    private ViewAreaSidebar areaSidebar;
    private ViewAreaBattlefield areaBattle;
    private ViewAreaUser areaUser;

    private int w, h;
    private static final double SIDEBAR_W_PCT = 0.16;
    private static final double USER_H_PCT = 0.27;
    private ControlMatchUI control;

    /**
     * - Lays out battle, sidebar, user areas in locked % vals and repaints
     * as necessary.<br>
     * - Instantiates top-level controller for match UI.<br>
     * - Has access methods for all child controllers<br>
     * - Implements Display interface used in singleton pattern
     * 
     */
    public ViewTopLevel() {
        super();

        // Set properties
        setOpaque(false);
        setBGTexture(AllZone.getSkin().getTexture1());
        setBGImg(AllZone.getSkin().getMatchBG());
        setLayout(null);

        // areaBattle: holds fields for all players in match.
        areaBattle = new ViewAreaBattlefield();
        areaBattle.setBounds(0, 0, getWidth() / 2, getHeight() / 2);
        add(areaBattle);

        // areaSidebar: holds card detail, info tabber.
        areaSidebar = new ViewAreaSidebar();
        areaSidebar.setBounds(0, 0, getWidth() / 2, getHeight() / 2);
        add(areaSidebar);

        // areaUser: holds input, hand, dock.
        areaUser = new ViewAreaUser();
        areaUser.setBounds(0, 0, getWidth() / 2, getHeight() / 2);
        add(areaUser);

        // After all components are in place, instantiate controller.
        control = new ControlMatchUI(this);
    }

    /**
     * The null layout used in MatchFrame has zones split into percentage values
     * to prevent child components pushing around the parent layout. A single
     * instance of BodyPanel holds these zones, and handles the percentage resizing.
     *
     * @param g &emsp; Graphics object
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        w = getWidth();
        h = getHeight();

        // Set % boundaries of layout control layer
        areaBattle.setBounds(0, 0, (int) (w * (1 - SIDEBAR_W_PCT)),
                (int) (h * (1 - USER_H_PCT)));
        areaSidebar.setBounds((int) (w * (1 - SIDEBAR_W_PCT)), 0,
                (int) (w * SIDEBAR_W_PCT), h);
        areaUser.setBounds(0, (int) (h * (1 - USER_H_PCT)),
                (int) (w * (1 - SIDEBAR_W_PCT)), (int) (h * USER_H_PCT));
        areaBattle.validate();
    }


    //========== Retrieval functions for easier interation with children panels.
    /** @return ViewAreaSidebar */
    public ViewAreaSidebar getAreaSidebar() {
        return areaSidebar;
    }

    /** @return ViewAreaBattlefield */
    public ViewAreaBattlefield getAreaBattlefield() {
        return areaBattle;
    }

    /** @return ViewAreaUser */
    public ViewAreaUser getAreaUser() {
        return areaUser;
    }

    /**
     *  Retrieves top level controller (actions, observers, etc.)
     *  for this UI.
     *
     * @return {@link java.util.List<MatchPlayer>}
     */
    public ControlMatchUI getController() {
        return control;
    }

    /** @return ControlCardviewer */
    public ControlCardviewer getCardviewerController() {
        return areaSidebar.getCardviewer().getController();
    }

    /** @return ControlTabber */
    public ControlTabber getTabberController() {
        return areaSidebar.getTabber().getController();
    }

    /** @return ControlInput */
    public ControlInput getInputController() {
        return areaUser.getPnlInput().getController();
    }

    /** @return ControlHand */
    public ControlHand getHandController() {
        return areaUser.getPnlHand().getController();
    }

    /** @return ControlDock */
    public ControlDock getDockController() {
        return areaUser.getPnlDock().getController();
    }

    /** @return List<ControlField> */
    public List<ControlField> getFieldControllers() {
        List<ViewField> fields = areaBattle.getFields();
        List<ControlField> controllers = new ArrayList<ControlField>();

        for (ViewField f : fields) {
            controllers.add(f.getController());
        }

        return controllers;
    }

    /** @return List<ViewField> */
    public List<ViewField> getFieldViews() {
        return areaBattle.getFields();
    }

    //========== Input panel and human hand retrieval functions
    // Also due to be deprecated.  Access should be handled by child component
    // view and/or controller.

    /** @return <b>JTextArea</b> Message area of input panel. */
    public JTextArea getPnlMessage() {
        return areaUser.getPnlInput().getTarMessage();
    }

    /** @return <b>ViewHand</b> Retrieves player hand panel. */
    public ViewHand getPnlHand() {
        return areaUser.getPnlHand();
    }

    //========== The following methods are required by the Display interface.
    // To fit the UI MVC architecture with the previous "mixed nuts" architecture,
    // these methods are temporarily required.  However, since they are a mix of
    // view and control functionalities, they are ALL on the "to-be-deprecated" list.
    // The Display interface is to be reworked, eventually, with a better name
    // and with interfaces for every screen in the entire UI.
    // Doublestrike 23-10-11

    /**
     * Required by Display interface.
     * Due to be deprecated in favor of more semantic getBtnCancel().
     * 
     * @return MyButton
     */
    public MyButton getButtonCancel() {
        MyButton cancel = new MyButton() {
            public void select() {
                getInputController().getInputControl().selectButtonCancel();
            }

            public boolean isSelectable() {
                return areaUser.getPnlInput().getBtnCancel().isEnabled();
            }

            public void setSelectable(final boolean b) {
                areaUser.getPnlInput().getBtnCancel().setEnabled(b);
            }

            public String getText() {
                return areaUser.getPnlInput().getBtnCancel().getText();
            }

            public void setText(final String text) {
                areaUser.getPnlInput().getBtnCancel().setText(text);
            }

            public void reset() {
                areaUser.getPnlInput().getBtnCancel().setText("Cancel");
            }
        };
        return cancel;
    }

    /**
     * Required by Display interface.
     * Due to be deprecated in favor of more semantic getBtnOK().
     * 
     * @return MyButton
     */
    public MyButton getButtonOK() {
        MyButton ok = new MyButton() {
            public void select() {
                getInputController().getInputControl().selectButtonOK();
            }

            public boolean isSelectable() {
                return areaUser.getPnlInput().getBtnOK().isEnabled();
            }

            public void setSelectable(final boolean b) {
                areaUser.getPnlInput().getBtnOK().setEnabled(b);
            }

            public String getText() {
                return areaUser.getPnlInput().getBtnOK().getText();
            }

            public void setText(final String text) {
                areaUser.getPnlInput().getBtnOK().setText(text);
            }

            public void reset() {
                areaUser.getPnlInput().getBtnOK().setText("OK");
            }
        };

        return ok;
    }

    /**
     * Required by Display interface.
     * Due to be deprecated: is now and should be handled by ControlMatchUI.
     * 
     * @param s &emsp; Message string
     */
    public void showMessage(String s) {
        getPnlMessage().setText(s);
    }

    /**
     * Required by Display interface.
     * Due to be deprecated: should be handled by ControlMatchUI.
     * 
     * @param s &emsp; Message string
     */
    public void showCombat(String s) {
        getTabberController().getView().updateCombat(s);
    }

    /**
     * Required by Display interface.
     * Due to be deprecated: should be handled by a control class, and
     * poorly named; "decking" == "milling" in preferences, same terminology
     * should be used throughout project for obvious reasons. Unless "decking"
     * is already the correct terminology, in which case, everything else is
     * poorly named.
     * 
     * @return boolean
     */
    public boolean canLoseByDecking() {
        return Constant.Runtime.MILL[0];
    }

    /**
     * <p>loadPrefs.</p>
     * Required by Display interface.
     * Due to be deprecated: will be handled by ControlMatchUI.
     * 
     *
     * @return boolean.
     */
    public final boolean loadPrefs() {
        ForgePreferences fp = Singletons.getModel().getPreferences();
        List<ViewField> fieldViews = getFieldViews();

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
     * <p>savePrefs.</p>
     * Required by Display interface.
     * Due to be deprecated: will be handled by ControlMatchUI.
     * Also, this functionality is already performed elsewhere in the code base.
     * Furthermore, there's a strong possibility this will need bo be broken
     * down and can't be in one place - e.g. keyboard shortcuts are
     * saved after they're edited.
     * 
     * @return a boolean.
     */
    public final boolean savePrefs() {
        ForgePreferences fp = Singletons.getModel().getPreferences();
        List<ViewField> fieldViews = getFieldViews();

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
     * <p>stopAtPhase.</p>
     * Required by Display interface.
     * Due to be deprecated: should be handled by control class.
     * 
     * @param turn &emsp; Player object...more info needed
     * @param phase &emsp; A string...more info needed
     * @return a boolean.
     */
    public final boolean stopAtPhase(final Player turn, final String phase) {
        List<ControlField> fieldControllers = getFieldControllers();

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
     * Required by display interface.
     * Due to be deprecated: handled by control class.
     *
     * @return a {@link forge.Card} object.
     */
    public final Card getCard() {
        System.err.println("ViewTopLevel > getCard: Something should happen here!");
        new Exception().printStackTrace();
        return null; //new Card(); //detail.getCard();
    }

    /**
     * Required by display interface.
     * Due to be deprecated: already handled by controller class.
     * 
     * @param card &emsp; a card
     */
    public final void setCard(final Card card) {
        System.err.println("ViewTopLevel > getCard: Something should happen here!");
        new Exception().printStackTrace();
    }

    /**
     * Required by Display interface.
     * Assigns damage to multiple blockers.
     * Due to be deprecated: Gui_MultipleBlockers4 says "very hacky"; needs
     * rewriting.
     * 
     * @param attacker &emsp; Card object
     * @param blockers &emsp; Card objects in CardList form
     * @param damage &emsp; int
     */
    public final void assignDamage(final Card attacker, final CardList blockers, final int damage) {
        if (damage <= 0) {
            return;
        }

        //new Gui_MultipleBlockers4(attacker, blockers, damage, this);
    }
}
