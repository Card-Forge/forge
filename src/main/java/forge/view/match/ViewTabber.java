package forge.view.match;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;

import net.miginfocom.swing.MigLayout;
import forge.AllZone;
import forge.Constant;
import forge.MagicStack;
import forge.Player;
import forge.card.spellability.SpellAbilityStackInstance;
import forge.control.match.ControlTabber;
import forge.gui.MultiLineLabelUI;
import forge.gui.skin.FPanel;
import forge.gui.skin.FSkin;
import forge.view.toolbox.FVerticalTabPanel;

/** 
 * Vertical tab panel for viewing stack, combat, etc.
 * Unfortunately, cannot extend a Swing component, since
 * vertical tabs are generated dynamically in the constructor.
 *
 */
public class ViewTabber {
    private List<JPanel> panelList;
    private HashMap<Player, JLabel[]> detailLabels;

    private ControlTabber control;
    private FSkin skin;

    private FPanel pnlStack, pnlCombat, pnlConsole, pnlPlayers, pnlDev;

    private DevLabel lblMilling, lblHandView, lblLibraryView, lblGenerateMana,
        lblSetupGame, lblTutor, lblCounterPermanent, lblTapPermanent, lblUntapPermanent,
        lblUnlimitedLands, lblHumanLife;

    private FVerticalTabPanel vtpTabber;

    /**
     * Assembles Swing components for tabber area in sidebar.
     */
    public ViewTabber() {
        skin = AllZone.getSkin();

        // Assemble card pic viewer
        panelList = new ArrayList<JPanel>();
        String constraints = "wrap, insets 0, gap 0";

        pnlStack = new FPanel();
        pnlStack.setName("Stack");
        pnlStack.setOpaque(false);
        pnlStack.setLayout(new MigLayout(constraints));
        pnlStack.setToolTipText("View Stack");
        panelList.add(pnlStack);

        pnlCombat = new FPanel();
        pnlCombat.setName("Combat");
        pnlCombat.setOpaque(false);
        pnlCombat.setLayout(new MigLayout(constraints));
        pnlCombat.setToolTipText("View Combat");
        panelList.add(pnlCombat);

        pnlConsole = new FPanel();
        pnlConsole.setName("Log");
        pnlConsole.setOpaque(false);
        pnlConsole.setLayout(new MigLayout(constraints));
        pnlConsole.setToolTipText("View Console");
        panelList.add(pnlConsole);

        pnlPlayers = new FPanel();
        pnlPlayers.setName("Players");
        pnlPlayers.setOpaque(false);
        pnlPlayers.setLayout(new MigLayout(constraints));
        pnlPlayers.setToolTipText("Player List");
        panelList.add(pnlPlayers);

        pnlDev = new FPanel();
        pnlDev.setName("Dev");
        pnlDev.setOpaque(false);
        pnlDev.setLayout(new MigLayout(constraints));
        pnlDev.setToolTipText("Developer Mode");

        if (Constant.Runtime.DEV_MODE[0]) {
            panelList.add(pnlDev);
        }

        // Populate the various panels in the tabber.
        populatePnlDev();
        populatePnlPlayers();
        populatePnlConsole();

        vtpTabber = new FVerticalTabPanel(panelList);
        vtpTabber.getContentPanel().setBorder(new MatteBorder(1, 0, 0, 1, skin.getClrBorders()));

        // After all components are in place, instantiate controller.
        control = new ControlTabber(this);
    }

    /** @return ControlTabber */
    public ControlTabber getController() {
        return control;
    }

    /**
     * Removes and adds JTextAreas to stack panel, which briefly summarize the
     * spell and allow mouseover.
     * 
     */
    public void updateStack() {
        final MagicStack stack = AllZone.getStack();
        final ViewTopLevel t = (ViewTopLevel) AllZone.getDisplay();

        int count = 1;
        JTextArea tar;
        String txt, isOptional;

        pnlStack.removeAll();
        vtpTabber.showTab(0);
        Font font = skin.getFont1().deriveFont(Font.PLAIN, 11);
        Border border = new MatteBorder(0, 0, 1, 0, Color.black);

        for (int i = stack.size() - 1; 0 <= i; i--) {
            isOptional = stack.peekAbility(i).isOptionalTrigger() && stack.peekAbility(i).getSourceCard().getController().isHuman() ? "(OPTIONAL) " : "";
            txt = (count++) + ". " + isOptional + stack.peekInstance(i).getStackDescription();
            tar = new JTextArea(txt);
            tar.setToolTipText(txt);
            tar.setOpaque(false);
            tar.setBorder(border);
            tar.setFont(font);
            tar.setFocusable(false);
            tar.setEditable(false);
            tar.setLineWrap(true);
            tar.setWrapStyleWord(true);

            final SpellAbilityStackInstance spell = stack.peekInstance(i);

            tar.addMouseListener(new MouseAdapter() {
               @Override
               public void mouseEntered(MouseEvent e) {
                   t.getCardviewerController().showCard(spell.getSpellAbility().getSourceCard());
                   System.out.println();
               }
            });

            pnlStack.add(tar, "w 100%!");
        }
    }

    /**
     * Removes and adds JTextAreas to combat panel, which briefly summarize the
     * current combat situation.
     * 
     * @param s &emsp; String message
     */

    // Note: Can (should?) be easily retrofitted to fit stack-style reporting:
    // multiple text areas, with mouseovers highlighting combat cards.  Doublestrike 06-11-11
    public void updateCombat(String s) {
        pnlCombat.removeAll();
        vtpTabber.showTab(1);

        Font font = skin.getFont1().deriveFont(Font.PLAIN, 11);
        Border border = new MatteBorder(1, 0, 0, 0, Color.black);

        JTextArea tar = new JTextArea(s);
        tar.setOpaque(false);
        tar.setBorder(border);
        tar.setFont(font);
        tar.setFocusable(false);
        tar.setLineWrap(true);
        pnlCombat.add(tar, "h 100%!, w 100%!");
    }

    /** Updates labels in the "player" panel, which display non-critical details about
     *  each player in the game.
     *
     *  @param p0 &emsp; Player obj
     */
    public void updatePlayerLabels(Player p0) {
        JLabel[] temp = detailLabels.get(p0);
        temp[0].setText("Life: " + String.valueOf(p0.getLife()));
        temp[1].setText("Max hand: " + String.valueOf(p0.getMaxHandSize()));
        temp[2].setText("Draw per turn: " + String.valueOf(p0.getNumDrawnThisTurn()));
    }

    /** @return FVerticalTabPanel */
    public FVerticalTabPanel getVtpTabber() {
        return vtpTabber;
    }

    /** @return FPanel */
    public FPanel getPnlStack() {
        return pnlStack;
    }

    /** @return FPanel */
    public FPanel getPnlCombat() {
        return pnlCombat;
    }

    /** @return FPanel */
    public FPanel getPnlPlayers() {
        return pnlPlayers;
    }

    /** @return FPanel */
    public FPanel getPnlDev() {
        return pnlDev;
    }

    /** @return DevLabel */
    public DevLabel getLblMilling() {
        return lblMilling;
    }

    /** @return DevLabel */
    public DevLabel getLblHandView() {
        return lblHandView;
    }

    /** @return DevLabel */
    public DevLabel getLblLibraryView() {
        return lblLibraryView;
    }

    /** @return DevLabel */
    public DevLabel getLblGenerateMana() {
        return lblGenerateMana;
    }

    /** @return DevLabel */
    public DevLabel getLblSetupGame() {
        return lblSetupGame;
    }

    /** @return DevLabel */
    public DevLabel getLblTutor() {
        return lblTutor;
    }

    /** @return DevLabel */
    public DevLabel getLblCounterPermanent() {
        return lblCounterPermanent;
    }

    /** @return DevLabel */
    public DevLabel getLblTapPermanent() {
        return lblTapPermanent;
    }

    /** @return DevLabel */
    public DevLabel getLblUntapPermanent() {
        return lblUntapPermanent;
    }

    /** @return DevLabel */
    public DevLabel getLblUnlimitedLands() {
        return lblUnlimitedLands;
    }

    /** @return DevLabel */
    public DevLabel getLblHumanLife() {
        return lblHumanLife;
    }

    /** @return HashMap<Player, JLabel[]> */
    public HashMap<Player, JLabel[]> getDetailLabels() {
        return detailLabels;
    }


    /** Assembles Swing components for "players" panel. */
    private void populatePnlPlayers() {
        List<Player> players = AllZone.getPlayersInGame();
        detailLabels = new HashMap<Player, JLabel[]>();

        for (Player p : players) {
            // Create and store labels detailing various non-critical player info.
            InfoLabel name = new InfoLabel();
            InfoLabel life = new InfoLabel();
            InfoLabel hand = new InfoLabel();
            InfoLabel draw = new InfoLabel();
            detailLabels.put(p, new JLabel[] {life, hand, draw});

            // Set border on bottom label, and larger font on player name
            draw.setBorder(new MatteBorder(0, 0, 1, 0, skin.getClrBorders()));
            name.setText(p.getName());
            name.setFont(skin.getFont1().deriveFont(Font.PLAIN, 14));

            // Add to "players" tab panel
            String constraints = "w 97%!, gapleft 2%, gapbottom 1%";
            pnlPlayers.add(name, constraints);
            pnlPlayers.add(life, constraints);
            pnlPlayers.add(hand, constraints);
            pnlPlayers.add(draw, constraints);
        }
    }

    /** Assembles Swing components for "dev mode" panel. */
    private void populatePnlDev() {
        JPanel viewport = new JPanel();
        viewport.setLayout(new MigLayout("wrap, insets 0"));
        viewport.setOpaque(false);

        JScrollPane jsp = new JScrollPane(viewport, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        jsp.setOpaque(false);
        jsp.getViewport().setOpaque(false);

        pnlDev.add(jsp, "w 100%!, h 100%!");

        lblMilling = new DevLabel("Loss by Milling: Enabled", "Loss by Milling: Disabled");
        lblHandView = new DevLabel("View Any Hand: Enabled", "View Any Hand: Disabled");
        lblLibraryView = new DevLabel("View Any Library: Enabled", "View Any Library: Disabled");
        lblGenerateMana = new DevLabel("Generate Mana");
        lblSetupGame = new DevLabel("Setup Game State");
        lblTutor = new DevLabel("Tutor for card");
        lblCounterPermanent = new DevLabel("Add Counter to Permanent");
        lblTapPermanent = new DevLabel("Tap Permanent");
        lblUntapPermanent = new DevLabel("Untap Permanent");
        lblUnlimitedLands = new DevLabel("Play Unlimited Lands This Turn");
        lblHumanLife = new DevLabel("Set Player Life");

        String constraints = "w 100%!, gap 0 0 5px 0";
        viewport.add(lblMilling, constraints);
        viewport.add(lblHandView, constraints);
        viewport.add(lblLibraryView, constraints);
        viewport.add(lblGenerateMana, constraints);
        viewport.add(lblSetupGame, constraints);
        viewport.add(lblTutor, constraints);
        viewport.add(lblCounterPermanent, constraints);
        viewport.add(lblTapPermanent, constraints);
        viewport.add(lblUntapPermanent, constraints);
        viewport.add(lblUnlimitedLands, constraints);
        viewport.add(lblHumanLife, constraints);
    }

    /** Assembles swing components for "console" panel. */
    private void populatePnlConsole() {
        JLabel prompt = new JLabel("IN > ");
        JTextField input = new JTextField();

        JTextArea log = new JTextArea();
        log.setBackground(new Color(0, 0, 0, 20));
        log.setWrapStyleWord(true);
        log.setLineWrap(true);
        log.setWrapStyleWord(true);
        log.setEditable(false);
        log.setFocusable(false);
        log.setForeground(skin.getClrText());
        log.setFont(skin.getFont1().deriveFont(Font.PLAIN, 12));
        log.setBorder(new MatteBorder(1, 0, 0, 0, skin.getClrBorders()));

        log.setText("Not implemented yet. Input codes entered above. "
                + "Output data recorded below.");

        pnlConsole.setLayout(new MigLayout("insets 0, gap 0, wrap 2"));

        pnlConsole.add(prompt, "w 28%!, h 10%!, gapleft 2%, gaptop 2%, gapbottom 2%");
        pnlConsole.add(input, "w 68%!, gapright 2%, gaptop 2%, gapbottom 2%");
        pnlConsole.add(log, "w 94%!, h 80%!, gapleft 4%, span 2 1");
    }

    /**
    * Labels that act as buttons which control dev mode functions. Labels
    * are used to support multiline text.
    *
    */
   public class DevLabel extends JLabel {
       private static final long serialVersionUID = 7917311680519060700L;

       private Color defaultBG = Color.green;
       private Color hoverBG = skin.getClrHover();
       private boolean enabled;
       private String enabledText, disabledText;
       private int w, h, r, i;  // Width, height, radius, insets (for paintComponent)

       /** 
        * Labels that act as buttons which control dev mode functions. Labels
        * are used (instead of buttons) to support multiline text.
        * 
        * Constructor for DevLabel which doesn't use enabled/disabled states;
        * only single text string required.
        * 
        * @param s0 &emsp; String text/tooltip of label
        */
       public DevLabel(String s0) {
           this(s0, s0);
       }

       /**
        * Labels that act as buttons which control dev mode functions. Labels
        * are used (instead of buttons) to support multiline text.
        * 
        * This constructor for DevLabels empowers an "enable" state that
        * displays them as green (enabled) or red (disabled).
        *
        * @param en0 &emsp; String text/tooltip of label, in "enabled" state
        * @param dis0 &emsp; String text/tooltip of label, in "disabled" state
        */
       public DevLabel(String en0, String dis0) {
           super();
           this.setUI(MultiLineLabelUI.getLabelUI());
           this.setFont(skin.getFont1().deriveFont(Font.PLAIN, 11));
           this.setBorder(new EmptyBorder(5, 5, 5, 5));
           this.enabledText = en0;
           this.disabledText = dis0;
           this.r = 6;  // Radius (for paintComponent)
           this.i = 2;  // Insets (for paintComponent)
           setEnabled(true);

           this.addMouseListener(new MouseAdapter() {
               @Override
               public void mouseEntered(MouseEvent e) {
                   setBackground(hoverBG);
               }

               @Override
               public void mouseExited(MouseEvent e) {
                   setBackground(defaultBG);
               }
           });
       }

       /**
        * Changes enabled state per boolean parameter, automatically updating
        * text string and background color.
        * 
        * @param b &emsp; boolean
        */
       public void setEnabled(boolean b) {
           String s;
           if (b) {
               defaultBG = Color.green;
               s = enabledText;
           }
           else {
               defaultBG = Color.red;
               s = disabledText;
           }
           enabled = b;
           this.setText(s);
           this.setToolTipText(s);
           this.setBackground(defaultBG);
       }

       /** @return boolean */
       public boolean getEnabled() {
           return enabled;
       }

       /**
        * In many cases, a DevLabel state will just be toggling a boolean.
        * This method sets up and evaluates the condition and toggles as appropriate.
        * 
        */
       public void toggleEnabled() {
           if (enabled) {
               setEnabled(false);
           }
           else {
               setEnabled(true);
           }
       }

       @Override
       protected void paintComponent(Graphics g) {
           w = getWidth();
           h = getHeight();
           g.setColor(this.getBackground());
           g.fillRoundRect(i, i, w - 2 * i, h - i, r, r);
           super.paintComponent(g);
       }
   }

   /** A quick JLabel for info in "players" panel, to consolidate styling. */
   @SuppressWarnings("serial")
   private class InfoLabel extends JLabel {
       public InfoLabel() {
           super();
           this.setFont(skin.getFont1().deriveFont(Font.PLAIN, 11));
           this.setForeground(skin.getClrText());
       }
   }
}
