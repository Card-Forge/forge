package forge;

import java.awt.BorderLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;

import forge.error.ErrorViewer;
import forge.gui.game.CardPanel;

/**
 * <p>Constructor for Gui_MultipleBlockers4.</p>
 *
 * @author Forge
 * @version $Id$
 */

/**
 * very hacky.
 * 
 */
class GuiMultipleBlockers4 extends JFrame {
    /** Constant <code>serialVersionUID=7622818310877381045L</code>. */
    private static final long serialVersionUID = 7622818310877381045L;

    private int assignDamage;
    private Card att;
    private CardList blockers;
    private CardContainer guiDisplay;

    private BorderLayout borderLayout1 = new BorderLayout();
    private JPanel mainPanel = new JPanel();
    private JScrollPane jScrollPane1 = new JScrollPane();
    private JLabel numberLabel = new JLabel();
    private JPanel jPanel3 = new JPanel();
    private BorderLayout borderLayout3 = new BorderLayout();
    private JPanel creaturePanel = new JPanel();

    /**
     * <p>
     * Constructor for Gui_MultipleBlockers4.
     * </p>
     * 
     * @param attacker
     *            a {@link forge.Card} object.
     * @param creatureList
     *            a {@link forge.CardList} object.
     * @param damage
     *            a int.
     * @param display
     *            a {@link forge.CardContainer} object.
     */
    GuiMultipleBlockers4(final Card attacker, final CardList creatureList,
            final int damage, final CardContainer display) {
        this();
        assignDamage = damage;
        updateDamageLabel(); // update user message about assigning damage
        guiDisplay = display;
        att = attacker;
        blockers = creatureList;

        for (int i = 0; i < creatureList.size(); i++) {
            creaturePanel.add(new CardPanel(creatureList.get(i)));
        }

        if (att.hasKeyword("Trample")) {
            Card player = new Card();
            player.setName("Player");
            player.addIntrinsicKeyword("Shroud");
            player.addIntrinsicKeyword("Indestructible");
            creaturePanel.add(new CardPanel(player));
        }

        JDialog dialog = new JDialog(this, true);
        dialog.setTitle("Multiple Blockers");
        dialog.setContentPane(mainPanel);
        dialog.setSize(470, 310);
        dialog.setVisible(true);
    }

    /**
     * <p>
     * Constructor for Gui_MultipleBlockers4.
     * </p>
     */
    public GuiMultipleBlockers4() {
        try {
            jbInit();
        } catch (Exception ex) {
            ErrorViewer.showError(ex);
        }
        // setSize(470, 280);
        // show();
    }

    /**
     * <p>
     * jbInit.
     * </p>
     * 
     * @throws java.lang.Exception
     *             if any.
     */
    private void jbInit() throws Exception {
        this.getContentPane().setLayout(borderLayout1);
        this.setTitle("Multiple Blockers");
        mainPanel.setLayout(null);
        numberLabel.setHorizontalAlignment(SwingConstants.CENTER);
        numberLabel.setHorizontalTextPosition(SwingConstants.CENTER);
        numberLabel.setText("Assign");
        numberLabel.setBounds(new Rectangle(52, 10, 343, 24));
        jPanel3.setLayout(borderLayout3);
        jPanel3.setBounds(new Rectangle(15, 40, 430, 235));
        creaturePanel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent e) {
                creaturePanelMousePressed(e);
            }
        });
        creaturePanel.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseMoved(final MouseEvent e) {
                creaturePanelMouseMoved(e);
            }
        });
        mainPanel.add(jPanel3, null);
        jPanel3.add(jScrollPane1, BorderLayout.CENTER);
        mainPanel.add(numberLabel, null);
        jScrollPane1.getViewport().add(creaturePanel, null);
        this.getContentPane().add(mainPanel, BorderLayout.CENTER);
    }

    /**
     * <p>
     * okButton_actionPerformed.
     * </p>
     * 
     * @param e
     *            a {@link java.awt.event.ActionEvent} object.
     */
    void okButtonActionPerformed(final ActionEvent e) {
        dispose();
    }

    /**
     * <p>
     * creaturePanel_mousePressed.
     * </p>
     * 
     * @param e
     *            a {@link java.awt.event.MouseEvent} object.
     */
    void creaturePanelMousePressed(final MouseEvent e) {
        Object o = creaturePanel.getComponentAt(e.getPoint());
        if (o instanceof CardPanel) {

            boolean assignedDamage = true;

            CardContainer cardPanel = (CardContainer) o;
            Card c = cardPanel.getCard();
            // c.setAssignedDamage(c.getAssignedDamage() + 1);
            CardList cl = new CardList();
            cl.add(att);

            boolean assignedLethalDamageToAllBlockers = true;
            for (Card crd : blockers) {
                if (crd.getLethalDamage() > 0 && (!att.hasKeyword("Deathtouch") || crd.getTotalAssignedDamage() < 1)) {
                    assignedLethalDamageToAllBlockers = false;
                }
            }

            if (c.getName().equals("Player") && att.hasKeyword("Trample") && assignedLethalDamageToAllBlockers) {
                AllZone.getCombat().addDefendingDamage(1, att);
                c.addAssignedDamage(1, att);
            } else if (!c.getName().equals("Player")) {
                c.addAssignedDamage(1, att);
            } else {
                assignedDamage = false;
            }

            if (assignedDamage) {
                assignDamage--;
                updateDamageLabel();
                if (assignDamage == 0) {
                    dispose();
                }
            }

            if (guiDisplay != null) {
                guiDisplay.setCard(c);
            }
        }
        // reduce damage, show new user message, exit if necessary

    } // creaturePanel_mousePressed()

    /**
     * <p>
     * updateDamageLabel.
     * </p>
     */
    void updateDamageLabel() {
        numberLabel.setText("Assign " + assignDamage + " damage - click on card to assign damage");
    }

    /**
     * <p>
     * creaturePanel_mouseMoved.
     * </p>
     * 
     * @param e
     *            a {@link java.awt.event.MouseEvent} object.
     */
    void creaturePanelMouseMoved(final MouseEvent e) {
        Object o = creaturePanel.getComponentAt(e.getPoint());
        if (o instanceof CardPanel) {
            CardContainer cardPanel = (CardContainer) o;
            Card c = cardPanel.getCard();

            if (guiDisplay != null) {
                guiDisplay.setCard(c);
            }
        }
    }
}
