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

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import net.miginfocom.swing.MigLayout;

import forge.AllZone;
import forge.control.ControlMatchUI;
import forge.control.match.ControlDetail;
import forge.control.match.ControlDock;
import forge.control.match.ControlField;
import forge.control.match.ControlHand;
import forge.control.match.ControlInput;
import forge.control.match.ControlPicture;
import forge.control.match.ControlTabber;
import forge.view.toolbox.FPanel;

/**
 * - Lays out containers and borders for resizeable layout.<br>
 * - Instantiates top-level controller for match UI.<br>
 * - Has access methods for all child controllers<br>
 * - Implements Display interface used in singleton pattern
 * 
 */

@SuppressWarnings("serial")
public class ViewTopLevel extends FPanel {
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
    private double tabberWpct = 0.15;
    private double tabberHpct = 0.55;
    private double battleWpct = 0.68;
    private double battleHpct = 0.73;
    private double pictureHpct = 0.4;
    private double detailHpct = 0.45;

    private static final int BOUNDARY_THICKNESS_PX = 6;

    // Boundary rectangles for all components, and boundary panel objects.
    private Rectangle pictureBounds, detailBounds, battleBounds, handBounds, tabberBounds, dockBounds, inputBounds;

    private BoundaryPanel pnlB1, pnlB2, pnlB3, pnlB4, pnlB5, pnlB6;

    private RegionPanel pnlPicture, pnlDetail, pnlBattlefield, pnlHand, pnlTabber, pnlDock, pnlInput;

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
        this.setBGTexture(new ImageIcon(AllZone.getSkin().getImage("bg.texture")));
        this.setBGImg(new ImageIcon(AllZone.getSkin().getImage("bg.match")));
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
        pnlB2 = new BoundaryPanel();
        pnlB3 = new BoundaryPanel(true);
        pnlB4 = new BoundaryPanel();
        pnlB5 = new BoundaryPanel(true);
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
        hand = new ViewHand(this);
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
     * Panel resizing algorithms. Basically, find the change in % per drag
     * event, then add that to an appropriate parameter. In some cases, also
     * remove the delta from an appropriate parameter.
     * 
     */

    // Formulas here SHOULD NOT BE VERY COMPLICATED at all. If they're
    // complicated, you're doing it wrong. The complicated part should
    // be in calculateBounds().
    private void addDragListeners() {
        pnlB1.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(final MouseEvent e) {
                delta = e.getY() / (double) h;
                tabberHpct += delta;
                repaint();
            }
        });

        pnlB2.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(final MouseEvent e) {
                delta = e.getX() / (double) w;
                tabberWpct += delta;
                battleWpct -= delta;
                repaint();
            }
        });

        pnlB3.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(final MouseEvent e) {
                delta = e.getY() / (double) h;
                battleHpct += delta;
                repaint();
            }
        });

        pnlB4.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(final MouseEvent e) {
                delta = e.getX() / (double) w;
                battleWpct += delta;
                repaint();
            }
        });

        pnlB5.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(final MouseEvent e) {
                delta = e.getY() / (double) h;
                pictureHpct += delta;
                detailHpct -= delta;
                repaint();
            }
        });

        pnlB6.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(final MouseEvent e) {
                delta = e.getY() / (double) h;
                detailHpct += delta;
                repaint();
            }
        });
    }

    /**
     * Put together default layout; most values are dependent on sibling
     * component dimensions. The whole layout can be defined from six
     * parameters.
     * 
     */
    private void calculateBounds() {
        tabberBounds = new Rectangle(b, b, (int) (tabberWpct * w), (int) (tabberHpct * h));

        inputBounds = new Rectangle(tabberBounds.x, tabberBounds.height + 3 * b, tabberBounds.width, h
                - tabberBounds.height - 4 * b);

        battleBounds = new Rectangle(tabberBounds.width + 3 * b, b, (int) (w * battleWpct), (int) (h * battleHpct));

        handBounds = new Rectangle(battleBounds.x, battleBounds.height + 3 * b, battleBounds.width, h
                - battleBounds.height - 4 * b);

        pictureBounds = new Rectangle(battleBounds.x + battleBounds.width + 2 * b, b, w - battleBounds.x
                - battleBounds.width - 3 * b, (int) (h * pictureHpct));

        detailBounds = new Rectangle(pictureBounds.x, pictureBounds.height + 3 * b, pictureBounds.width,
                (int) (h * detailHpct));

        dockBounds = new Rectangle(pictureBounds.x, detailBounds.y + detailBounds.height + 2 * b, pictureBounds.width,
                h - detailBounds.y - detailBounds.height - 3 * b);

        // Apply bounds to regions.
        pnlPicture.setBounds(pictureBounds);
        pnlDetail.setBounds(detailBounds);
        pnlBattlefield.setBounds(battleBounds);
        pnlHand.setBounds(handBounds);
        pnlDock.setBounds(dockBounds);
        pnlInput.setBounds(inputBounds);
        pnlTabber.setBounds(tabberBounds);

        // Apply bounds to boundaries.
        pnlB1.setBounds(new Rectangle(b, tabberBounds.height + b, tabberBounds.width, 2 * b));

        pnlB2.setBounds(new Rectangle(tabberBounds.width + b, b, 2 * b, h - 2 * b));

        pnlB3.setBounds(new Rectangle(battleBounds.x, battleBounds.height + b, battleBounds.width, 2 * b));

        pnlB4.setBounds(new Rectangle(battleBounds.x + battleBounds.width, b, 2 * b, h - 2 * b));

        pnlB5.setBounds(new Rectangle(pictureBounds.x, pictureBounds.height + b, pictureBounds.width, 2 * b));

        pnlB6.setBounds(new Rectangle(pictureBounds.x, detailBounds.y + detailBounds.height, detailBounds.width, 2 * b));

        this.revalidate();
    } // End calculateBounds()

    /** @return String, comma delimited layout parameters. */
    public String getLayoutParams() {
        String s = "";
        s += Double.toString(tabberWpct) + ",";
        s += Double.toString(tabberHpct) + ",";
        s += Double.toString(battleWpct) + ",";
        s += Double.toString(battleHpct) + ",";
        s += Double.toString(pictureHpct) + ",";
        s += Double.toString(detailHpct);

        return s;
    }

    /** 
     * Takes a string of comma-delimited layout parameters and applies to layout.
     * @param s0 &emsp; String
     */
    public void setLayoutParams(String s0) {
        if (s0 == null || s0.isEmpty()) {
            return;
        }

        String[] vals = s0.split(",");

        tabberWpct = Double.parseDouble(vals[0]);
        tabberHpct = Double.parseDouble(vals[1]);
        battleWpct = Double.parseDouble(vals[2]);
        battleHpct = Double.parseDouble(vals[3]);
        pictureHpct = Double.parseDouble(vals[4]);
        detailHpct = Double.parseDouble(vals[5]);
    }

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
            } else {
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
            // setBorder(new LineBorder(Color.green, 1));
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
     *            Graphics object
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
     * @return ControlMatchUI
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
    /**
     * Gets the top level frame.
     * 
     * @return JFrame
     */
    public JFrame getTopLevelFrame() {
        return (JFrame) AllZone.getDisplay();
    }
}
