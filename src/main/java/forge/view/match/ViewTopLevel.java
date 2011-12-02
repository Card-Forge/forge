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

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import net.miginfocom.swing.MigLayout;

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
import forge.control.ControlMatchUI;
import forge.control.match.ControlDetail;
import forge.control.match.ControlDock;
import forge.control.match.ControlField;
import forge.control.match.ControlHand;
import forge.control.match.ControlInput;
import forge.control.match.ControlPicture;
import forge.control.match.ControlTabber;
import forge.properties.ForgePreferences;
import forge.view.toolbox.FPanel;

/**
 * - Lays out containers and borders for resizeable layout.<br>
 * - Instantiates top-level controller for match UI.<br>
 * - Has access methods for all child controllers<br>
 * - Implements Display interface used in singleton pattern
 * 
 */

@SuppressWarnings("serial")
public class ViewTopLevel extends FPanel implements CardContainer, Display {
    private ViewBattlefield battlefield;
    private ViewDetail detail;
    private ViewDock dock;
    private ViewHand hand;
    private ViewInput input;
    private ViewPicture picture;
    private ViewTabber tabber;

    private ControlMatchUI control;
    private int w, h, b;
    private double delta;

    // Default layout parameters (all in percent!)
    private double dockWpct = 0.15;
    private double dockHpct = 0.2;
    private double tabberHpct = 0.55;
    private double battleWpct = 0.68;
    private double battleHpct = 0.73;
    private double pictureHpct = 0.5;

    private static final int BOUNDARY_THICKNESS_PX = 6;

    // Boundary rectangles for all components, and boundary panel objects.
    private Rectangle pictureBounds, detailBounds, battleBounds,
        handBounds, tabberBounds, dockBounds, inputBounds;

    private BoundaryPanel pnlB1, pnlB2, pnlB3, pnlB4, pnlB5, pnlB6;

    private RegionPanel pnlPicture, pnlDetail, pnlBattlefield,
        pnlHand, pnlTabber, pnlDock, pnlInput;

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
        b = (int) Math.ceil(BOUNDARY_THICKNESS_PX / 2);

        // Declare and add containers and resizers for various regions in layout
        pnlPicture = new RegionPanel();
        pnlDetail = new RegionPanel();
        pnlBattlefield = new RegionPanel();
        pnlHand = new RegionPanel();
        pnlDock = new RegionPanel();
        pnlInput = new RegionPanel();
        pnlTabber = new RegionPanel();

        pnlB1 = new BoundaryPanel(true);
        pnlB2 = new BoundaryPanel(true);
        pnlB3 = new BoundaryPanel();
        pnlB4 = new BoundaryPanel(true);
        pnlB5 = new BoundaryPanel();
        pnlB6 = new BoundaryPanel(true);

        add(pnlPicture);
        add(pnlDetail);
        add(pnlBattlefield);
        add(pnlHand);
        add(pnlDock);
        add(pnlInput);
        add(pnlTabber);

        add(pnlB1);
        add(pnlB2);
        add(pnlB3);
        add(pnlB4);
        add(pnlB5);
        add(pnlB6);

        // Declare and add various view components
        input = new ViewInput();
        hand = new ViewHand();
        dock = new ViewDock();
        battlefield = new ViewBattlefield();
        tabber = new ViewTabber();
        detail = new ViewDetail();
        picture = new ViewPicture();

        String constraints = "w 100%!, h 100%!";
        pnlInput.add(input, constraints);
        pnlHand.add(hand, constraints);
        pnlBattlefield.add(battlefield, constraints);
        pnlDock.add(dock, constraints);
        pnlTabber.add(tabber, constraints);
        pnlDetail.add(detail, constraints);
        pnlPicture.add(picture, constraints);

        // After all components are in place, instantiate controller.
        addDragListeners();
        this.control = new ControlMatchUI(this);
    }

    /**
     * Panel resizing algorithms.  Basically, find the change in % per
     * drag event, then add that to an appropriate parameter.  In some
     * cases, also remove the delta from an appropriate parameter.
     * 
     */

    // Formulas here SHOULD NOT BE VERY COMPLICATED at all.  If they're
    // complicated, you're doing it wrong. The complicated part should
    // be in calculateBounds().
    private void addDragListeners() {
        pnlB1.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(final MouseEvent e) {
                delta = e.getY() / (double) h;
                dockHpct += delta;
                tabberHpct -= delta;
                repaint();
            }
        });

        pnlB2.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(final MouseEvent e) {
                delta = e.getY() / (double) h;
                tabberHpct += delta;
                repaint();
            }
        });

        pnlB3.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(final MouseEvent e) {
                delta = e.getX() / (double) w;
                battleWpct -= delta;
                dockWpct += delta;
                repaint();
            }
        });

        pnlB4.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(final MouseEvent e) {
                delta = e.getY() / (double) h;
                battleHpct += delta;
                repaint();
            }
        });

        pnlB5.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(final MouseEvent e) {
                delta = e.getX() / (double) w;
                battleWpct += delta;
                repaint();
            }
        });

        pnlB6.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(final MouseEvent e) {
                delta = e.getY() / (double) h;
                pictureHpct += delta;
                repaint();
            }
        });
    }

    /**
     * Put together default layout; most values are dependent
     * on sibling component dimensions. The whole layout can be
     * defined from six parameters.
     * 
     */
    private void calculateBounds() {
        dockBounds = new Rectangle(
                b, b,
                (int) (dockWpct * w), (int) (dockHpct * h));

        tabberBounds = new Rectangle(
                dockBounds.x, dockBounds.height + 3 * b,
                dockBounds.width, (int) (tabberHpct * h));

        inputBounds = new Rectangle(
                dockBounds.x, tabberBounds.y + tabberBounds.height + 2 * b,
                dockBounds.width, h - tabberBounds.y - tabberBounds.height - 3 * b);

        battleBounds = new Rectangle(
                dockBounds.width + 3 * b, b,
                (int) (w * battleWpct), (int) (h * battleHpct));

        handBounds = new Rectangle(
                battleBounds.x, battleBounds.height + 3 * b,
                battleBounds.width, h - battleBounds.height - 4 * b);

        pictureBounds = new Rectangle(
                battleBounds.x + battleBounds.width + 2 * b, b,
                w - battleBounds.x - battleBounds.width - 3 * b, (int) (h * pictureHpct));

        detailBounds = new Rectangle(
                pictureBounds.x, pictureBounds.height + 3 * b,
                pictureBounds.width, h - pictureBounds.height - 4 * b);

        // Apply bounds to regions.
        pnlPicture.setBounds(pictureBounds);
        pnlDetail.setBounds(detailBounds);
        pnlBattlefield.setBounds(battleBounds);
        pnlHand.setBounds(handBounds);
        pnlDock.setBounds(dockBounds);
        pnlInput.setBounds(inputBounds);
        pnlTabber.setBounds(tabberBounds);

        // Apply bounds to boundaries.
        pnlB1.setBounds(new Rectangle(
                b, dockBounds.height + b,
                dockBounds.width, 2 * b));

        pnlB2.setBounds(new Rectangle(
                b, dockBounds.height + tabberBounds.height + 3 * b,
                dockBounds.width, 2 * b));

        pnlB3.setBounds(new Rectangle(
                dockBounds.width + b, b,
                2 * b, h - 2 * b));

        pnlB4.setBounds(new Rectangle(
                dockBounds.width + 3 * b, battleBounds.height + b,
                battleBounds.width, 2 * b));

        pnlB5.setBounds(new Rectangle(
                battleBounds.width + dockBounds.width + 3 * b, b,
                2 * b, h - 2 * b));

        pnlB6.setBounds(new Rectangle(
                pictureBounds.x, pictureBounds.height + b,
                pictureBounds.width, 2 * b));

        this.revalidate();
    }   // End calculateBounds()

    /** Consolidates cursor and opacity settings in one place. */
    private class BoundaryPanel extends JPanel {
        public BoundaryPanel() {
            this(false);
        }

        public BoundaryPanel(boolean movesNorthSouth) {
            super();
            // For testing, comment this opaque setter.
            setOpaque(false);

            if (movesNorthSouth) {
                setBackground(Color.red);
                setCursor(new Cursor(Cursor.N_RESIZE_CURSOR));
            }
            else {
                setBackground(Color.blue);
                setCursor(new Cursor(Cursor.E_RESIZE_CURSOR));
            }
        }
    }

    /** Consolidates opacity settings in one place. */
    private class RegionPanel extends JPanel {
        public RegionPanel() {
            super();
            // For testing, comment this opaque setter, uncomment the border.
            //setBorder(new LineBorder(Color.green, 1));
            setOpaque(false);
            setLayout(new MigLayout("insets 0, gap 0"));
        }
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
        h = getHeight();
        w = getWidth();

        calculateBounds();
        super.paintComponent(g);
    }

    // ========== Retrieval functions for easier interaction with children
    // panels.

    /**
     * Retrieves top level controller (actions, observers, etc.) for this UI.
     * 
     * @return {@link java.util.List<MatchPlayer>}
     */
    public ControlMatchUI getController() {
        return this.control;
    }

    /**
     * Gets the detail controller.
     * 
     * @return ControlDetail
     */
    public ControlDetail getDetailController() {
        return ViewTopLevel.this.detail.getController();
    }

    /**
     * Gets the picture controller.
     * 
     * @return ControlPicture
     */
    public ControlPicture getPictureController() {
        return ViewTopLevel.this.picture.getController();
    }

    /**
     * Gets the tabber controller.
     * 
     * @return ControlTabber
     */
    public ControlTabber getTabberController() {
        return ViewTopLevel.this.tabber.getController();
    }

    /**
     * Gets the input controller.
     * 
     * @return ControlInput
     */
    public ControlInput getInputController() {
        return ViewTopLevel.this.input.getController();
    }

    /**
     * Gets the hand controller.
     * 
     * @return ControlHand
     */
    public ControlHand getHandController() {
        return ViewTopLevel.this.hand.getController();
    }

    /**
     * Gets the dock controller.
     * 
     * @return ControlDock
     */
    public ControlDock getDockController() {
        return ViewTopLevel.this.dock.getController();
    }

    /**
     * Gets the field controllers.
     * 
     * @return List<ControlField>
     */
    public List<ControlField> getFieldControllers() {
        final List<ViewField> fields = this.battlefield.getFields();
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
        return ViewTopLevel.this.battlefield.getFields();
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
        return ViewTopLevel.this.input.getTarMessage();
    }

    /**
     * Gets the pnl hand.
     * 
     * @return <b>ViewHand</b> Retrieves player hand panel.
     */
    public ViewHand getPnlHand() {
        return ViewTopLevel.this.hand;
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
                return ViewTopLevel.this.input.getBtnCancel().isEnabled();
            }

            @Override
            public void setSelectable(final boolean b) {
                ViewTopLevel.this.input.getBtnCancel().setEnabled(b);
            }

            @Override
            public String getText() {
                return ViewTopLevel.this.input.getBtnCancel().getText();
            }

            @Override
            public void setText(final String text) {
                ViewTopLevel.this.input.getBtnCancel().setText(text);
            }

            @Override
            public void reset() {
                ViewTopLevel.this.input.getBtnCancel().setText("Cancel");
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
                return ViewTopLevel.this.input.getBtnOK().isEnabled();
            }

            @Override
            public void setSelectable(final boolean b) {
                ViewTopLevel.this.input.getBtnOK().setEnabled(b);
            }

            @Override
            public String getText() {
                return ViewTopLevel.this.input.getBtnOK().getText();
            }

            @Override
            public void setText(final String text) {
                ViewTopLevel.this.input.getBtnOK().setText(text);
            }

            @Override
            public void reset() {
                ViewTopLevel.this.input.getBtnOK().setText("OK");
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
        return this.getDetailController().getCurrentCard();
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
        this.getDetailController().showCard(card);
        this.getPictureController().showCard(card);
    }

    /** {@inheritDoc} */
    @Override
    public final void assignDamage(final Card attacker, final CardList blockers, final int damage) {
        if (damage <= 0) {
            return;
        }

        new GuiMultipleBlockers(attacker, blockers, damage, this);
    }

    /** @return JFrame */
    public JFrame getTopLevelFrame() {
        return (JFrame) ViewTopLevel.this.getParent().getParent().getParent().getParent();
    }
}
